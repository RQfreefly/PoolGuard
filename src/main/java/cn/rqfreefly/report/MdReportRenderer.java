package cn.rqfreefly.report;

import cn.rqfreefly.core.Issue;
import cn.rqfreefly.core.ScanResult;
import cn.rqfreefly.core.Severity;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * 固定模板 md-template-v1。
 */
public final class MdReportRenderer implements ReportRenderer {

    /**
     * {@inheritDoc}
     */
    @Override
    public String render(ScanResult result) {
        // 模块 1：先按严重级别分组，后续章节可直接复用。
        Map<Severity, List<Issue>> grouped = groupBySeverity(result.getIssues());

        // 模块 2：输出 Markdown 头部与元信息。
        StringBuilder sb = new StringBuilder();
        sb.append("# PoolGuard 扫描报告\n\n");
        sb.append("## 元信息\n");
        sb.append("- 项目路径: ").append(result.getProjectPath()).append("\n");
        sb.append("- 扫描时间: ").append(result.getScanTime()).append("\n");
        sb.append("- 扫描模式: ").append(result.getScanMode()).append("\n");
        sb.append("- LLM 提供方: DashScope\n");
        sb.append("- LLM 模型: ").append(result.getLlmModel()).append("\n\n");

        // 模块 3：输出扫描摘要和按级别统计。
        sb.append("## 扫描摘要\n");
        sb.append("- 扫描文件数: ").append(result.getParsedFileCount()).append("\n");
        sb.append("- 问题总数: ").append(result.getIssues().size()).append("\n");
        sb.append("- critical: ").append(grouped.get(Severity.CRITICAL).size()).append("\n");
        sb.append("- high: ").append(grouped.get(Severity.HIGH).size()).append("\n");
        sb.append("- medium: ").append(grouped.get(Severity.MEDIUM).size()).append("\n");
        sb.append("- low: ").append(grouped.get(Severity.LOW).size()).append("\n\n");

        // 模块 4：按严重级别输出问题明细。
        sb.append("## 问题明细（按严重程度排序）\n");
        for (Severity severity : new Severity[]{Severity.CRITICAL, Severity.HIGH, Severity.MEDIUM, Severity.LOW}) {
            if (grouped.get(severity).isEmpty()) {
                continue;
            }
            sb.append("### ").append(severity.name().toLowerCase()).append("\n");
            for (Issue issue : grouped.get(severity)) {
                sb.append("#### [").append(issue.getRuleId()).append("] ").append(issue.getTitle()).append("\n");
                sb.append("- 风险分值: ").append(issue.getRiskScore()).append("\n");
                sb.append("- 位置: ").append(issue.getFilePath()).append(":").append(issue.getLine())
                        .append(" (").append(asMethod(issue)).append(")\n");
                sb.append("- 原因: ").append(issue.getReason()).append("\n");
                sb.append("- 建议: ").append(issue.getFixSuggestion()).append("\n\n");
            }
        }

        // 模块 5：输出聚合修复建议，方便一次性治理。
        sb.append("## 修复建议汇总\n");
        List<String> summaries = buildSummary(result.getIssues());
        for (int i = 0; i < summaries.size(); i++) {
            sb.append(i + 1).append(". ").append(summaries.get(i)).append("\n");
        }
        return sb.toString();
    }

    private String asMethod(Issue issue) {
        // 证据中无 method 字段时返回 unknown，避免 NPE。
        Object method = issue.getEvidence().get("method");
        return method == null ? "unknown" : String.valueOf(method);
    }

    private List<String> buildSummary(List<Issue> issues) {
        // 模块 1：先扫描是否命中关键规则。
        List<String> summary = new ArrayList<String>();
        boolean hasPg003 = false;
        boolean hasPg006 = false;
        boolean hasPg008 = false;
        for (Issue issue : issues) {
            if (issue.getRuleId().name().equals("PG003")) {
                hasPg003 = true;
            }
            if (issue.getRuleId().name().equals("PG006")) {
                hasPg006 = true;
            }
            if (issue.getRuleId().name().equals("PG008")) {
                hasPg008 = true;
            }
        }

        // 模块 2：根据命中规则输出对应治理建议。
        if (hasPg003) {
            summary.add("统一通过 try/finally 或 @PreDestroy 补齐线程池关闭路径。");
        }
        if (hasPg008) {
            summary.add("定时任务需同时实现 ScheduledFuture.cancel 与 executor.shutdown。");
        }
        if (hasPg006) {
            summary.add("为线程池配置可观测线程名（自定义 ThreadFactory）。");
        }
        if (summary.isEmpty()) {
            summary.add("本次扫描未发现问题，保持现有生命周期管理策略。");
        }

        // 模块 3：返回最终摘要列表。
        return summary;
    }

    private Map<Severity, List<Issue>> groupBySeverity(List<Issue> issues) {
        // 模块 1：初始化每个严重级别的桶。
        Map<Severity, List<Issue>> grouped = new EnumMap<Severity, List<Issue>>(Severity.class);
        grouped.put(Severity.CRITICAL, new ArrayList<Issue>());
        grouped.put(Severity.HIGH, new ArrayList<Issue>());
        grouped.put(Severity.MEDIUM, new ArrayList<Issue>());
        grouped.put(Severity.LOW, new ArrayList<Issue>());

        // 模块 2：将每条问题放入对应桶。
        for (Issue issue : issues) {
            grouped.get(issue.getSeverity()).add(issue);
        }

        // 模块 3：返回分组结果。
        return grouped;
    }
}
