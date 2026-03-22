package cn.rqfreefly.report;

import static org.junit.jupiter.api.Assertions.assertTrue;

import cn.rqfreefly.core.Issue;
import cn.rqfreefly.core.RuleId;
import cn.rqfreefly.core.ScanResult;
import cn.rqfreefly.core.Severity;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 模块: report
 * 功能: JSON/Markdown 报告渲染测试。
 */
@DisplayName("Report 渲染器测试")
class ReportRendererTest {

    @Test
    @DisplayName("should_包含元信息和问题明细_when_渲染Markdown报告")
    void should_includeMetaAndIssueDetails_when_renderMarkdownReport() {
        // 模块: report / 功能: md 模板包含元信息与问题明细
        // Given: 包含一条问题的扫描结果。
        LinkedHashMap<String, Object> evidence = new LinkedHashMap<String, Object>();
        evidence.put("method", "ping");
        Issue issue = new Issue(
                RuleId.PG001,
                Severity.HIGH,
                85,
                "高频入口中创建线程池",
                "desc",
                "A.java",
                12,
                evidence,
                "原因",
                "建议",
                0.8,
                false
        );
        ScanResult result = new ScanResult(Arrays.asList(issue), 1, 0, ".", "2026-03-15T00:00:00+08:00", "full", "glm-5");

        // When: 渲染 Markdown 报告。
        String markdown = new MdReportRenderer().render(result);

        // Then: 模板关键字段应存在。
        assertTrue(markdown.contains("## 元信息"));
        assertTrue(markdown.contains("[PG001]"));
        assertTrue(markdown.contains("修复建议汇总"));
    }

    @Test
    @DisplayName("should_包含问题统计字段_when_渲染Json报告")
    void should_includeIssueCountFields_when_renderJsonReport() {
        // 模块: report / 功能: json 输出包含计数字段
        // Given: 空问题列表结果。
        ScanResult result = new ScanResult(Collections.<Issue>emptyList(), 2, 0, ".", "t", "full", "glm-5");

        // When: 渲染 JSON 报告。
        String json = new JsonReportRenderer().render(result);

        // Then: 输出包含 issue_count 字段。
        assertTrue(json.contains("\"issue_count\""));
        assertTrue(json.contains("\"severity_count\""));
    }
}
