package cn.rqfreefly.report;

import cn.rqfreefly.core.Issue;
import cn.rqfreefly.core.ScanResult;
import cn.rqfreefly.core.Severity;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 默认 JSON 报告输出。
 */
public final class JsonReportRenderer implements ReportRenderer {

    private final ObjectMapper objectMapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

    /**
     * {@inheritDoc}
     */
    @Override
    public String render(ScanResult result) {
        // 模块 1：组装报告根节点，字段顺序固定便于人工阅读与对比。
        Map<String, Object> root = new LinkedHashMap<String, Object>();
        root.put("project_path", result.getProjectPath());
        root.put("scan_time", result.getScanTime());
        root.put("scan_mode", result.getScanMode());
        root.put("llm_provider", "DashScope");
        root.put("llm_model", result.getLlmModel());
        root.put("parsed_file_count", result.getParsedFileCount());
        root.put("failed_file_count", result.getFailedFileCount());
        root.put("issue_count", result.getIssues().size());
        root.put("severity_count", severityCounter(result));
        root.put("issues", result.getIssues());
        try {
            // 模块 2：输出格式化 JSON，提升可读性。
            return objectMapper.writeValueAsString(root);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("JSON渲染失败", ex);
        }
    }

    private Map<String, Integer> severityCounter(ScanResult result) {
        // 模块 1：初始化各严重级别计数器。
        Map<String, Integer> map = new LinkedHashMap<String, Integer>();
        map.put("critical", 0);
        map.put("high", 0);
        map.put("medium", 0);
        map.put("low", 0);

        // 模块 2：遍历问题列表，按严重级别累加。
        for (Issue issue : result.getIssues()) {
            if (Severity.CRITICAL == issue.getSeverity()) {
                map.put("critical", map.get("critical") + 1);
            } else if (Severity.HIGH == issue.getSeverity()) {
                map.put("high", map.get("high") + 1);
            } else if (Severity.MEDIUM == issue.getSeverity()) {
                map.put("medium", map.get("medium") + 1);
            } else {
                map.put("low", map.get("low") + 1);
            }
        }

        // 模块 3：返回统计结果。
        return map;
    }
}
