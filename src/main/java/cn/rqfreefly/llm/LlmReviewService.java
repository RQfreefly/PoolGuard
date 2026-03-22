package cn.rqfreefly.llm;

import cn.rqfreefly.core.Issue;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * LLM 复判服务，支持并发与失败降级。
 */
public final class LlmReviewService {

    private final DashScopeClient dashScopeClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 构造复判服务。
     *
     * @param dashScopeClient DashScope 客户端
     */
    public LlmReviewService(DashScopeClient dashScopeClient) {
        this.dashScopeClient = dashScopeClient;
    }

    /**
     * 并发执行 LLM 复判。
     * 复判是增强能力而非主流程依赖，因此任何失败都回退原始问题列表。
     *
     * @param issues 规则命中问题列表
     * @param model 模型名
     * @param concurrency 并发数
     * @return 复判后的问题列表
     */
    public List<Issue> review(List<Issue> issues, String model, int concurrency) {
        if (issues.isEmpty()) {
            return issues;
        }

        // 模块 1：按并发参数创建复判线程池。
        ExecutorService executorService = Executors.newFixedThreadPool(Math.max(1, concurrency));
        try {
            // 模块 2：将每条 Issue 转换为一个并发任务。
            List<Callable<Issue>> tasks = new ArrayList<Callable<Issue>>();
            for (Issue issue : issues) {
                tasks.add(() -> reviewOne(issue, model));
            }

            // 模块 3：批量执行并收集 Future 结果。
            List<Future<Issue>> futures = executorService.invokeAll(tasks);
            List<Issue> reviewed = new ArrayList<Issue>();
            for (Future<Issue> future : futures) {
                try {
                    reviewed.add(future.get());
                } catch (Exception ex) {
                    // 单条失败不影响整体，保持“可用优先”。
                }
            }

            // 模块 4：数量不一致时整体回退，避免部分替换。
            if (reviewed.size() != issues.size()) {
                // 风险控制:
                // 只要结果数量不一致，就整体回退，避免“部分替换”导致行为不确定。
                return issues;
            }
            return reviewed;
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return issues;
        } finally {
            // 模块 5：无论成功失败都及时释放线程池资源。
            executorService.shutdownNow();
        }
    }

    private Issue reviewOne(Issue issue, String model) {
        try {
            // 模块 1：序列化证据并调用大模型复判。
            String evidenceJson = objectMapper.writeValueAsString(issue.getEvidence());
            LlmReviewResult result = dashScopeClient.review(evidenceJson, model);

            // 模块 2：将复判结果映射回新的 Issue 对象。
            return issue.withLlmReview(
                    result.getReason(),
                    result.getFixSuggestion(),
                    result.getConfidence(),
                    result.isNeedsHumanReview());
        } catch (Exception ex) {
            // 模块 3：单条复判异常时回退原始 issue。
            return issue;
        }
    }
}
