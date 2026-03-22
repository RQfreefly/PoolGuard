package cn.rqfreefly.report;

import cn.rqfreefly.core.Issue;
import cn.rqfreefly.core.ScanResult;

/**
 * 纯文本报告。
 */
public final class TextReportRenderer implements ReportRenderer {

    /**
     * {@inheritDoc}
     */
    @Override
    public String render(ScanResult result) {
        // 模块 1：输出报告头和整体元信息。
        StringBuilder sb = new StringBuilder();
        sb.append("PoolGuard Scan Result\n");
        sb.append("project=").append(result.getProjectPath())
                .append(" mode=").append(result.getScanMode())
                .append(" llm=").append(result.getLlmModel())
                .append("\n");
        sb.append("parsed=").append(result.getParsedFileCount())
                .append(", failed=").append(result.getFailedFileCount())
                .append(", issues=").append(result.getIssues().size())
                .append("\n");

        // 模块 2：逐条输出问题摘要，便于终端快速查看。
        for (Issue issue : result.getIssues()) {
            sb.append(issue.getSeverity())
                    .append(" ")
                    .append(issue.getRuleId())
                    .append(" ")
                    .append(issue.getFilePath())
                    .append(":")
                    .append(issue.getLine())
                    .append(" ")
                    .append(issue.getReason())
                    .append("\n");
        }

        // 模块 3：返回纯文本内容。
        return sb.toString();
    }
}
