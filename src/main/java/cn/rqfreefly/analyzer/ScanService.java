package cn.rqfreefly.analyzer;

import cn.rqfreefly.core.Issue;
import cn.rqfreefly.core.ScanConfig;
import cn.rqfreefly.core.ScanResult;
import cn.rqfreefly.core.SortBy;
import cn.rqfreefly.llm.DashScopeClient;
import cn.rqfreefly.llm.LlmReviewService;
import cn.rqfreefly.parser.JavaFileAnalysis;
import cn.rqfreefly.parser.JavaFileDiscoverer;
import cn.rqfreefly.parser.JavaSourceParser;
import cn.rqfreefly.rules.RuleMetadataRegistry;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

/**
 * 扫描服务主流程。
 * 设计上将“发现文件/解析/规则/LLM/排序”串成稳定流水线，
 * 目的是保证同一输入在不同机器和时间执行时结果顺序一致，便于做基线对比。
 */
public final class ScanService {

    private static final Path CACHE_DIR = Paths.get(".poolguard", "cache");
    private static final Path HASH_CACHE_FILE = CACHE_DIR.resolve("file-hash.properties");

    private final JavaFileDiscoverer fileDiscoverer;
    private final JavaSourceParser sourceParser;
    private final P0RuleEngine ruleEngine;
    private final LlmReviewService llmReviewService;

    /**
     * 创建默认扫描服务。
     */
    public ScanService() {
        this(new JavaFileDiscoverer(), new JavaSourceParser(), new P0RuleEngine(new RuleMetadataRegistry()),
                new LlmReviewService(new DashScopeClient()));
    }

    /**
     * 创建可注入依赖的扫描服务。
     *
     * @param fileDiscoverer 文件发现器
     * @param sourceParser 代码解析器
     * @param ruleEngine 规则引擎
     * @param llmReviewService LLM 复判服务
     */
    public ScanService(JavaFileDiscoverer fileDiscoverer,
                       JavaSourceParser sourceParser,
                       P0RuleEngine ruleEngine,
                       LlmReviewService llmReviewService) {
        this.fileDiscoverer = fileDiscoverer;
        this.sourceParser = sourceParser;
        this.ruleEngine = ruleEngine;
        this.llmReviewService = llmReviewService;
    }

    /**
     * 执行一次扫描任务。
     * 该方法总是先跑规则，再决定是否叠加 LLM 复判。
     * 这样即使外部模型服务不可用，扫描也能稳定返回可用结果。
     *
     * @param config 扫描配置
     * @return 扫描结果
     * @throws IOException 文件读取失败时抛出
     */
    public ScanResult scan(ScanConfig config) throws IOException {
        // 模块 1：初始化计时器，用于分阶段性能日志。
        long start = System.currentTimeMillis();

        // 模块 2：发现待扫描文件（全量或目录下所有 Java 文件）。
        Path rootPath = Paths.get(config.getScanPath());
        List<Path> allJavaFiles = fileDiscoverer.discover(rootPath);

        // 模块 3：根据是否增量模式决定最终扫描范围。
        List<Path> targetFiles = config.isIncrementalMode()
                ? selectIncrementalFiles(config, rootPath, allJavaFiles)
                : allJavaFiles;

        long discoverCost = System.currentTimeMillis() - start;
        logPerf("discover", discoverCost, targetFiles.size(), config.isIncrementalMode() ? "incremental" : "full");

        // 模块 4：准备缓存容器（文件哈希缓存 + 本次 AST 缓存）。
        Properties hashCache = loadHashCache();
        Map<String, JavaFileAnalysis> astCache = new HashMap<String, JavaFileAnalysis>();

        // 模块 5：逐个文件解析，统计失败数量。
        long parseStart = System.currentTimeMillis();
        List<JavaFileAnalysis> analyses = new ArrayList<JavaFileAnalysis>();
        int failedCount = 0;
        for (Path javaFile : targetFiles) {
            String hash = sha256(javaFile);
            // 用内容哈希做键，避免“同一路径新内容”误命中旧解析结果。
            JavaFileAnalysis analysis = astCache.get(hash);
            if (analysis == null) {
                analysis = sourceParser.parse(javaFile);
                astCache.put(hash, analysis);
            }
            analyses.add(analysis);
            hashCache.put(javaFile.toString(), hash);
            if (!analysis.isParseSuccess()) {
                failedCount++;
            }
        }

        // 模块 6：输出解析阶段性能信息。
        long parseCost = System.currentTimeMillis() - parseStart;
        logPerf("parse", parseCost, analyses.size(), "failed=" + failedCount);

        // 模块 7：执行规则检测，得到基础问题列表。
        long ruleStart = System.currentTimeMillis();
        List<Issue> issues = ruleEngine.detect(analyses);
        long ruleCost = System.currentTimeMillis() - ruleStart;
        logPerf("rules", ruleCost, issues.size(), "detected");

        // 模块 8：按需叠加 LLM 复判，增强原因和修复建议。
        if (config.isEnableLlm()) {
            long llmStart = System.currentTimeMillis();
            // LLM 只做增强，不改变“先由规则发现问题”这个主路径。
            LlmReviewService currentLlmReviewService = config.isSkipSslVerification()
                    ? new LlmReviewService(new DashScopeClient(true))
                    : llmReviewService;
            issues = currentLlmReviewService.review(issues, config.getLlmModel(), config.getLlmConcurrency());
            long llmCost = System.currentTimeMillis() - llmStart;
            logPerf("llm", llmCost, issues.size(), "reviewed");
        }

        // 模块 9：统一排序并落盘缓存，保证报告稳定可复现。
        sortIssues(issues, config.getSortBy());
        persistHashCache(hashCache);

        // 模块 10：记录总耗时，并组装最终返回对象。
        long total = System.currentTimeMillis() - start;
        logPerf("total", total, issues.size(), "done");

        return new ScanResult(
                issues,
                targetFiles.size() - failedCount,
                failedCount,
                rootPath.toString(),
                OffsetDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
                config.isIncrementalMode() ? "incremental" : "full",
                config.getLlmModel());
    }

    /**
     * 选择增量扫描文件。
     * 这里采用“一跳文本扩散”（变更方法名 + 被调用文件）来控制扫描时延；
     * 比完整调用图更快，但可能有少量误差，适合日常快速反馈。
     *
     * @param config 扫描配置
     * @param rootPath 项目根路径
     * @param allJavaFiles 全量 Java 文件
     * @return 增量模式下需要扫描的文件
     * @throws IOException 读取文件失败
     */
    private List<Path> selectIncrementalFiles(ScanConfig config, Path rootPath, List<Path> allJavaFiles) throws IOException {
        // 模块 1：读取 changed-files 列表并规范化路径。
        List<String> changedPaths = Files.readAllLines(Paths.get(config.getChangedFilesPath()), StandardCharsets.UTF_8);
        Set<Path> changedFiles = new HashSet<Path>();
        for (String p : changedPaths) {
            String raw = p.trim();
            if ("".equals(raw) || raw.startsWith("#")) {
                continue;
            }
            Path candidate = rootPath.resolve(raw).normalize();
            if (Files.exists(candidate) && candidate.toString().endsWith(".java")) {
                changedFiles.add(candidate);
            } else {
                Path abs = Paths.get(raw);
                if (Files.exists(abs) && abs.toString().endsWith(".java")) {
                    changedFiles.add(abs.normalize());
                }
            }
        }
        if (changedFiles.isEmpty()) {
            return Collections.emptyList();
        }

        // 模块 2：提取变更文件中声明的方法名，作为一跳扩散种子。
        Set<String> changedMethodNames = new HashSet<String>();
        for (Path changedFile : changedFiles) {
            JavaFileAnalysis analysis = sourceParser.parse(changedFile);
            changedMethodNames.addAll(analysis.getDeclaredMethodNames());
        }

        // 模块 3：在全量文件中做轻量文本匹配，找出可能受影响文件。
        Set<Path> impacted = new HashSet<Path>(changedFiles);
        for (Path javaFile : allJavaFiles) {
            if (changedFiles.contains(javaFile)) {
                continue;
            }
            String content = new String(Files.readAllBytes(javaFile), StandardCharsets.UTF_8);
            for (String methodName : changedMethodNames) {
                // 方法名太短时很容易误命中（如 get/set），直接跳过。
                if (methodName.length() <= 2) {
                    continue;
                }
                if (content.contains(methodName + "(")) {
                    impacted.add(javaFile);
                    break;
                }
            }
        }

        // 模块 4：排序后返回，保证同样输入的输出顺序稳定。
        List<Path> sorted = new ArrayList<Path>(impacted);
        Collections.sort(sorted);
        return sorted;
    }

    private Properties loadHashCache() {
        // 模块 1：初始化空缓存对象。
        Properties properties = new Properties();
        try {
            // 模块 2：仅当缓存文件存在时读取。
            if (Files.exists(HASH_CACHE_FILE)) {
                java.io.InputStream in = Files.newInputStream(HASH_CACHE_FILE);
                properties.load(in);
                in.close();
            }
        } catch (Exception ex) {
            // 缓存损坏时直接忽略，保证主流程可继续执行。
        }
        // 模块 3：返回可用缓存（可能为空）。
        return properties;
    }

    private void persistHashCache(Properties properties) {
        try {
            // 模块 1：确保缓存目录存在。
            Files.createDirectories(CACHE_DIR);
            // 模块 2：将最新哈希写回本地缓存文件。
            java.io.OutputStream out = Files.newOutputStream(HASH_CACHE_FILE);
            properties.store(out, "PoolGuard file hash cache");
            out.close();
        } catch (Exception ex) {
            System.err.println("[poolguard][warn] 写入缓存失败: " + ex.getMessage());
        }
    }

    private String sha256(Path filePath) {
        try {
            // 模块 1：读取文件内容并计算 SHA-256 摘要。
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = Files.readAllBytes(filePath);
            byte[] hash = digest.digest(bytes);

            // 模块 2：把二进制摘要转成十六进制字符串。
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception ex) {
            // 哈希失败时退回路径键，宁可少用缓存，也不要中断扫描。
            return filePath.toString();
        }
    }

    private void sortIssues(List<Issue> issues, SortBy sortBy) {
        // 模块 1：根据指定维度构造排序器。
        Comparator<Issue> comparator;
        if (SortBy.SCORE == sortBy) {
            comparator = Comparator.comparingInt(Issue::getRiskScore).reversed()
                    .thenComparing(Issue::getFilePath)
                    .thenComparingInt(Issue::getLine);
        } else if (SortBy.PATH == sortBy) {
            comparator = Comparator.comparing(Issue::getFilePath)
                    .thenComparingInt(Issue::getLine)
                    .thenComparing((Issue issue) -> issue.getSeverity().getOrder(), Comparator.reverseOrder());
        } else {
            comparator = Comparator.comparing((Issue issue) -> issue.getSeverity().getOrder(), Comparator.reverseOrder())
                    .thenComparing(Issue::getFilePath)
                    .thenComparingInt(Issue::getLine);
        }
        // 模块 2：原地排序，供后续报告稳定输出。
        issues.sort(comparator);
    }

    private void logPerf(String stage, long costMs, int count, String extra) {
        // 统一打印结构化性能日志，方便后续采集和分析。
        System.err.println("[poolguard][perf] stage=" + stage + " cost_ms=" + costMs + " count=" + count + " " + extra);
    }
}
