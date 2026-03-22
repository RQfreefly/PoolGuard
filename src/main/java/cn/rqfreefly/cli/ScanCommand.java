package cn.rqfreefly.cli;

import cn.rqfreefly.analyzer.ScanService;
import cn.rqfreefly.core.OutputFormat;
import cn.rqfreefly.core.ScanConfig;
import cn.rqfreefly.core.ScanResult;
import cn.rqfreefly.core.SortBy;
import cn.rqfreefly.report.ReportRenderer;
import cn.rqfreefly.report.ReportRendererFactory;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.Callable;
import picocli.CommandLine.Command;
import picocli.CommandLine.ITypeConverter;
import picocli.CommandLine.Option;

/**
 * `scan` 子命令入口。
 */
@Command(
        name = "scan",
        mixinStandardHelpOptions = true,
        description = "扫描 Java 项目并输出线程池泄露风险"
)
public final class ScanCommand implements Callable<Integer> {

    @Option(names = "--path", required = true, description = "待扫描路径（文件或目录）")
    private String path;

    @Option(names = "--format", defaultValue = "json", description = "输出格式: json|md|text",
            converter = OutputFormatConverter.class)
    private OutputFormat format;

    @Option(names = "--sort", defaultValue = "severity", description = "排序方式: severity|score|path",
            converter = SortByConverter.class)
    private SortBy sortBy;

    @Option(names = "--output", description = "输出文件路径")
    private String output;

    @Option(names = "--changed-files", description = "增量扫描文件列表路径")
    private String changedFiles;

    @Option(names = "--llm-model", defaultValue = "glm-5", description = "LLM 模型名称")
    private String llmModel;

    @Option(names = "--llm-concurrency", defaultValue = "3", description = "LLM 并发数")
    private int llmConcurrency;

    @Option(names = "--enable-llm", defaultValue = "false", description = "是否启用 LLM 复判")
    private boolean enableLlm;

    @Option(names = "--skip-ssl-verification", defaultValue = "false", description = "是否跳过 LLM HTTPS SSL 校验（仅建议测试环境）")
    private boolean skipSslVerification;

    private final ScanService scanService;

    /**
     * 创建默认扫描命令。
     */
    public ScanCommand() {
        this(new ScanService());
    }

    ScanCommand(ScanService scanService) {
        this.scanService = scanService;
    }

    /**
     * 执行扫描命令。
     * 返回退出码而不是抛异常，是为了让脚本环境可直接根据返回值做流程编排。
     *
     * @return 退出码
     */
    @Override
    public Integer call() {
        try {
            // 模块 1：先做参数校验，尽量在真正扫描前快速失败。
            validateArgs();

            // 模块 2：把命令行参数组装为领域配置对象，避免后续方法参数过多。
            ScanConfig config = new ScanConfig(path, format, sortBy, llmModel, llmConcurrency, enableLlm,
                    skipSslVerification, output, changedFiles);

            // 模块 3：执行扫描主流程并拿到结构化结果。
            ScanResult result = scanService.scan(config);

            // 模块 4：根据输出格式选择渲染器，统一生成文本报告。
            ReportRenderer renderer = ReportRendererFactory.create(format);
            String content = renderer.render(result);

            // 模块 5：根据是否指定输出路径，决定写文件还是直接打印。
            if (output != null && !"".equals(output.trim())) {
                Path outputPath = Paths.get(output);
                // 输出目录不存在时先自动创建，提升 CLI 易用性。
                if (outputPath.getParent() != null) {
                    Files.createDirectories(outputPath.getParent());
                }
                // 固定 UTF-8，避免不同系统默认编码导致乱码。
                Files.write(outputPath, content.getBytes(StandardCharsets.UTF_8));
                System.out.println("报告已写入: " + outputPath.toAbsolutePath());
            } else {
                // 未指定 --output 时直接输出到标准输出，便于管道处理。
                System.out.println(content);
            }
            return 0;
        } catch (IllegalArgumentException ex) {
            // 参数类异常统一返回 2，与入口参数错误语义保持一致。
            System.err.println(ex.getMessage());
            return 2;
        } catch (Exception ex) {
            // 非预期异常统一返回 3，便于调用方区分错误类型。
            System.err.println("扫描异常: " + ex.getMessage());
            return 3;
        }
    }

    private void validateArgs() {
        // 模块 1：并发参数必须为正数，避免线程池初始化失败。
        if (llmConcurrency <= 0) {
            throw new IllegalArgumentException("--llm-concurrency 必须大于 0");
        }

        // 模块 2：扫描路径必须存在，避免无意义的后续流程。
        if (!Files.exists(Paths.get(path))) {
            throw new IllegalArgumentException("扫描路径不存在: " + path);
        }

        // 模块 3：增量文件列表如果给了，也必须存在。
        if (changedFiles != null && !"".equals(changedFiles.trim()) && !Files.exists(Paths.get(changedFiles))) {
            throw new IllegalArgumentException("--changed-files 文件不存在: " + changedFiles);
        }
    }

    static final class OutputFormatConverter implements ITypeConverter<OutputFormat> {
        /**
         * {@inheritDoc}
         */
        @Override
        public OutputFormat convert(String value) {
            // 允许用户输入大小写混合值，如 Json / JSON / json。
            return OutputFormat.valueOf(value.trim().toUpperCase());
        }
    }

    static final class SortByConverter implements ITypeConverter<SortBy> {
        /**
         * {@inheritDoc}
         */
        @Override
        public SortBy convert(String value) {
            // 统一转成枚举常量格式，避免命令行大小写差异造成解析失败。
            return SortBy.valueOf(value.trim().toUpperCase());
        }
    }
}
