package cn.rqfreefly.llm;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import cn.rqfreefly.core.Issue;
import cn.rqfreefly.core.RuleId;
import cn.rqfreefly.core.Severity;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 模块: llm
 * 功能: LlmReviewService 并发复判与降级行为测试。
 */
@DisplayName("LLM 复判服务测试")
class LlmReviewServiceTest {

    @Test
    @DisplayName("should_直接返回原列表_when_输入为空")
    void should_returnOriginalList_when_inputIsEmpty() {
        // 模块: llm / 功能: 空输入快速返回
        // Given: 一个可用的复判服务和空 issue 列表。
        LlmReviewService service = new LlmReviewService(new DashScopeClient());
        List<Issue> emptyIssues = Collections.emptyList();

        // When: 执行复判。
        List<Issue> result = service.review(emptyIssues, "qwen-plus", 3);

        // Then: 原列表直接返回。
        assertSame(emptyIssues, result);
        service.close();
    }

    @Test
    @DisplayName("should_回退原始Issue_when_证据序列化失败")
    void should_fallbackToOriginalIssue_when_evidenceSerializationFails() {
        // 模块: llm / 功能: 单任务异常降级
        // Given: 带循环引用证据的 issue（Jackson 序列化会失败）。
        LlmReviewService service = new LlmReviewService(new DashScopeClient());
        Issue cyclicIssue = buildIssueWithCyclicEvidence("A.java", 11);
        List<Issue> issues = Collections.singletonList(cyclicIssue);

        // When: 执行复判。
        List<Issue> result = service.review(issues, "qwen-plus", 2);

        // Then: 返回结果数量不变，且元素回退为原始 issue。
        assertEquals(1, result.size());
        assertSame(cyclicIssue, result.get(0));
        service.close();
    }

    @Test
    @DisplayName("should_保持批次稳定_when_并发提示异常且部分任务失败")
    void should_keepBatchStable_when_concurrencyHintInvalidAndTaskFails() {
        // 模块: llm / 功能: 批处理稳定性与并发提示兜底
        // Given: 多条 issue，其中包含会触发序列化异常的证据。
        LlmReviewService service = new LlmReviewService(new DashScopeClient());
        List<Issue> issues = new ArrayList<Issue>();
        issues.add(buildIssueWithPlainEvidence("B.java", 20));
        issues.add(buildIssueWithCyclicEvidence("C.java", 21));
        issues.add(buildIssueWithPlainEvidence("D.java", 22));

        // When: 使用非法并发提示值执行复判。
        List<Issue> result = service.review(issues, "qwen-plus", -5);

        // Then: 批次结果保持稳定，按原顺序返回且数量一致。
        assertEquals(issues.size(), result.size());
        assertSame(issues.get(1), result.get(1));
        service.close();
    }

    @Test
    @DisplayName("should_忽略后续复判_when_服务已关闭")
    void should_skipFurtherReview_when_serviceClosed() {
        // 模块: llm / 功能: 生命周期关闭保护
        // Given: 已关闭的复判服务和非空 issue 列表。
        LlmReviewService service = new LlmReviewService(new DashScopeClient());
        List<Issue> issues = Collections.singletonList(buildIssueWithPlainEvidence("E.java", 31));
        service.close();

        // When: 再次调用复判。
        List<Issue> result = service.review(issues, "qwen-plus", 4);

        // Then: 直接回传原始列表，且重复 close 不抛异常。
        assertSame(issues, result);
        assertDoesNotThrow(new org.junit.jupiter.api.function.Executable() {
            @Override
            public void execute() {
                service.close();
            }
        });
    }

    private Issue buildIssueWithPlainEvidence(String filePath, int line) {
        Map<String, Object> evidence = new LinkedHashMap<String, Object>();
        evidence.put("class", "DemoService");
        evidence.put("line", Integer.valueOf(line));
        return new Issue(RuleId.PG003, Severity.HIGH, 90, "线程池关闭不完整", "存在关闭路径风险", filePath, line, evidence);
    }

    private Issue buildIssueWithCyclicEvidence(String filePath, int line) {
        Map<String, Object> source = new LinkedHashMap<String, Object>();
        source.put("class", "DemoService");
        source.put("line", Integer.valueOf(line));
        source.put("self", source);
        return new Issue(RuleId.PG003, Severity.HIGH, 90, "线程池关闭不完整", "存在关闭路径风险", filePath, line, source);
    }
}
