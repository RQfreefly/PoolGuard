package cn.rqfreefly.analyzer;

import cn.rqfreefly.core.Issue;
import cn.rqfreefly.core.RuleId;
import cn.rqfreefly.parser.ExecutorCreation;
import cn.rqfreefly.parser.JavaFileAnalysis;
import cn.rqfreefly.parser.MethodFacts;
import cn.rqfreefly.parser.ShutdownCall;
import cn.rqfreefly.rules.RuleMetadata;
import cn.rqfreefly.rules.RuleMetadataRegistry;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 规则引擎：PG001/PG002/PG003/PG005/PG006/PG007/PG008/PG009。
 * 当前策略偏向“高召回优先”，因为首期目标是尽早发现可疑点，再通过 LLM/人工复核降误报。
 */
public final class P0RuleEngine {

    private final RuleMetadataRegistry metadataRegistry;

    /**
     * 构造规则引擎。
     *
     * @param metadataRegistry 规则元数据注册表
     */
    public P0RuleEngine(RuleMetadataRegistry metadataRegistry) {
        this.metadataRegistry = metadataRegistry;
    }

    /**
     * 在文件事实集合上执行规则检测。
     * 实现上采用“单次遍历命中多规则”，因为这比多轮遍历更快，也更容易保证去重一致性。
     *
     * @param analyses 文件分析结果
     * @return 命中的问题列表
     */
    public List<Issue> detect(List<JavaFileAnalysis> analyses) {
        // 模块 1：准备结果集与去重键集合。
        List<Issue> issues = new ArrayList<Issue>();
        Set<String> dedupKeys = new HashSet<String>();

        // 模块 2：按文件遍历解析事实，并构造方法级索引。
        for (JavaFileAnalysis analysis : analyses) {
            Map<String, MethodFacts> factsMap = buildFactsMap(analysis.getMethodFacts());

            // 模块 3：按“线程池创建点”触发各条规则判断。
            for (ExecutorCreation creation : analysis.getCreations()) {
                // PG001：高频入口直接创建线程池。
                if (!creation.isFieldOwner() && creation.isHighFrequencyEntry()) {
                    addIssueIfAbsent(issues, dedupKeys, buildIssue(metadataRegistry.get(RuleId.PG001), creation,
                            evidenceOf(creation, "context", "high_frequency_entry")));
                }

                // PG002：循环或递归中重复创建线程池。
                if (creation.isInLoop() || creation.isInRecursiveMethod()) {
                    String context = creation.isInLoop() ? "loop" : "recursive";
                    addIssueIfAbsent(issues, dedupKeys, buildIssue(metadataRegistry.get(RuleId.PG002), creation,
                            evidenceOf(creation, "context", context)));
                }

                // PG003：关闭缺失或关闭路径不完整。
                if (!isLifecycleManaged(analysis, creation)) {
                    List<ShutdownCall> shutdownCalls = findMatchingShutdownCalls(analysis.getShutdownCalls(), creation);
                    if (shutdownCalls.isEmpty()) {
                        addIssueIfAbsent(issues, dedupKeys, buildIssue(metadataRegistry.get(RuleId.PG003), creation,
                                evidenceOf(creation, "sub_type", "A_NOT_CLOSED")));
                    } else {
                        boolean shutdownInFinally = false;
                        for (ShutdownCall shutdownCall : shutdownCalls) {
                            if (shutdownCall.isInFinally()) {
                                shutdownInFinally = true;
                                break;
                            }
                        }
                        MethodFacts facts = factsMap.get(keyOf(creation.getFilePath(), creation.getClassName(), creation.getMethodName()));
                        boolean riskyPath = facts != null && (facts.getReturnCount() > 1 || facts.isHasCatch());
                        // 没有完整 CFG 时，用“多 return / catch”近似判断路径是否容易漏关线程池。
                        if (!shutdownInFinally && riskyPath) {
                            addIssueIfAbsent(issues, dedupKeys, buildIssue(metadataRegistry.get(RuleId.PG003), creation,
                                    evidenceOf(creation, "sub_type", "B_PATH_INCOMPLETE")));
                        }
                    }
                }

                // PG006：使用默认线程工厂，线程可观测性较弱。
                if (creation.isDefaultThreadFactory()) {
                    addIssueIfAbsent(issues, dedupKeys, buildIssue(metadataRegistry.get(RuleId.PG006), creation,
                            evidenceOf(creation, "thread_factory", "default")));
                }

                // PG007：无界队列可能导致堆积和内存风险。
                if (creation.isUnboundedQueue()) {
                    addIssueIfAbsent(issues, dedupKeys, buildIssue(metadataRegistry.get(RuleId.PG007), creation,
                            evidenceOf(creation, "queue_type", "LinkedBlockingQueue(unbounded)")));
                }

                // PG008：定时任务只 schedule 未 cancel/shutdown。
                if (creation.isScheduledExecutor() && isScheduledTaskNotManaged(analysis, creation)) {
                    addIssueIfAbsent(issues, dedupKeys, buildIssue(metadataRegistry.get(RuleId.PG008), creation,
                            evidenceOf(creation, "scheduled_var", creation.getVariableName())));
                }

                // PG009：静态线程池缺少明确退出回收机制。
                if (creation.isStaticField() && !isLifecycleManaged(analysis, creation)) {
                    addIssueIfAbsent(issues, dedupKeys, buildIssue(metadataRegistry.get(RuleId.PG009), creation,
                            evidenceOf(creation, "lifecycle", "missing_shutdown_hook")));
                }

                // PG005：实例字段持有线程池，但生命周期托管证据不足。
                if (creation.isFieldOwner() && !creation.isStaticField() && !isLifecycleManaged(analysis, creation)
                        && !analysis.getAutoCloseableClasses().contains(creation.getClassName())) {
                    addIssueIfAbsent(issues, dedupKeys, buildIssue(metadataRegistry.get(RuleId.PG005), creation,
                            evidenceOf(creation, "ownership", "short_lifecycle_holder")));
                }
            }
        }

        return issues;
    }

    private boolean isScheduledTaskNotManaged(JavaFileAnalysis analysis, ExecutorCreation creation) {
        // 模块 1：仅针对“确实参与 schedule”的变量继续判断。
        String varName = creation.getVariableName();
        if (varName == null || !analysis.getScheduledVariables().contains(varName)) {
            return false;
        }
        // 模块 2：分别检查 cancel 与 shutdown 两类回收动作。
        boolean hasCancel = analysis.getCancelVariables().contains(varName);
        boolean hasShutdown = !findMatchingShutdownCalls(analysis.getShutdownCalls(), creation).isEmpty();
        // 定时任务至少要做两件事：取消任务 + 关闭线程池，缺一都可能留下后台线程。
        return !hasCancel || !hasShutdown;
    }

    private void addIssueIfAbsent(List<Issue> issues, Set<String> dedupKeys, Issue issue) {
        // 模块 1：构造“规则 + 文件 + 行号”去重键。
        String key = issue.getRuleId() + "|" + issue.getFilePath() + "|" + issue.getLine();
        // 只去掉“同规则同位置”的重复命中，保留同位置不同规则，方便排查完整风险。
        if (dedupKeys.add(key)) {
            issues.add(issue);
        }
    }

    /**
     * 判断线程池是否已经由生命周期托管。
     * 这里只认“明确证据”（例如 @PreDestroy、destroyMethod、shutdownHook），
     * 不做隐式推断，避免把真实风险当成安全场景。
     *
     * @param analysis 文件分析结果
     * @param creation 创建点
     * @return 是否托管
     */
    private boolean isLifecycleManaged(JavaFileAnalysis analysis, ExecutorCreation creation) {
        // 模块 1：处理 @Bean destroyMethod 这类“无变量名创建”的场景。
        if (creation.getVariableName() == null && analysis.getBeanDestroyMethods().contains(creation.getMethodName())) {
            return true;
        }

        // 模块 2：局部匿名创建且无变量名时，无法建立生命周期关联。
        if (!creation.isFieldOwner() && creation.getVariableName() == null) {
            return false;
        }

        // 模块 3：变量名缺失时直接判定为无托管证据。
        String varName = creation.getVariableName();
        if (varName == null) {
            return false;
        }

        // 模块 4：字段线程池只认明确释放证据。
        if (creation.isFieldOwner()) {
            // 字段线程池通常活得更久，所以只有看到明确释放信号才算安全。
            return analysis.getPreDestroyShutdownFields().contains(varName)
                    || analysis.getShutdownHookFields().contains(varName);
        }

        return false;
    }

    /**
     * 在关闭调用集合中查找与创建点对应的关闭证据。
     * 优先变量名匹配；变量名不可得时再退回“同方法”匹配。
     *
     * @param shutdownCalls 关闭调用
     * @param creation 创建点
     * @return 匹配到的关闭调用
     */
    private List<ShutdownCall> findMatchingShutdownCalls(List<ShutdownCall> shutdownCalls, ExecutorCreation creation) {
        // 模块 1：遍历候选关闭调用并做同文件/同类过滤。
        List<ShutdownCall> matched = new ArrayList<ShutdownCall>();
        for (ShutdownCall shutdownCall : shutdownCalls) {
            if (!creation.getFilePath().equals(shutdownCall.getFilePath())) {
                continue;
            }
            if (!creation.getClassName().equals(shutdownCall.getClassName())) {
                continue;
            }
            String createVar = creation.getVariableName();
            String shutdownVar = shutdownCall.getVariableName();

            // 模块 2：变量名可用时，优先走精确变量匹配。
            if (createVar != null && createVar.equals(shutdownVar)) {
                matched.add(shutdownCall);
                continue;
            }

            // 模块 3：变量名缺失时，退化为同方法匹配。
            if (createVar == null && creation.getMethodName().equals(shutdownCall.getMethodName())) {
                matched.add(shutdownCall);
            }
        }
        return matched;
    }

    private Map<String, MethodFacts> buildFactsMap(List<MethodFacts> methodFactsList) {
        // 模块：将列表转为哈希索引，提升后续按方法查询效率。
        Map<String, MethodFacts> map = new HashMap<String, MethodFacts>();
        for (MethodFacts methodFacts : methodFactsList) {
            map.put(keyOf(methodFacts.getFilePath(), methodFacts.getClassName(), methodFacts.getMethodName()), methodFacts);
        }
        return map;
    }

    private String keyOf(String filePath, String className, String methodName) {
        // 使用稳定拼接键，避免引入额外对象作为 map key。
        return filePath + "|" + className + "|" + methodName;
    }

    private Issue buildIssue(RuleMetadata metadata, ExecutorCreation creation, Map<String, Object> evidence) {
        // 规则元数据 + 创建点事实 -> 统一 Issue 对象。
        return new Issue(
                metadata.getRuleId(),
                metadata.getSeverity(),
                metadata.getDefaultRiskScore(),
                metadata.getTitle(),
                metadata.getDescription(),
                creation.getFilePath(),
                creation.getLine(),
                evidence
        );
    }

    private Map<String, Object> evidenceOf(ExecutorCreation creation, String extraKey, Object extraValue) {
        // 证据字段使用固定键，便于后续 JSON 报告和 LLM 提示词复用。
        Map<String, Object> evidence = new LinkedHashMap<String, Object>();
        evidence.put("class", creation.getClassName());
        evidence.put("method", creation.getMethodName());
        evidence.put("line", creation.getLine());
        evidence.put("variable", creation.getVariableName());
        evidence.put(extraKey, extraValue);
        return evidence;
    }
}
