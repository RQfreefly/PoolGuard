package cn.rqfreefly.llm;

import cn.rqfreefly.core.Issue;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * LLM 复判服务，支持并发与失败降级。
 */
public final class LlmReviewService implements AutoCloseable {

    /** 线程池最小并发。 */
    private static final int MIN_CONCURRENCY = 1;
    /** 线程池最大并发，覆盖常见本地与 CI 环境，避免过度探测。 */
    private static final int MAX_CONCURRENCY = 32;
    /** 未传有效并发时使用的默认并发。 */
    private static final int DEFAULT_CONCURRENCY = 4;
    /** 复判任务队列容量。 */
    private static final int QUEUE_CAPACITY = 128;
    /** 单批复判超时时长（秒）。 */
    private static final int BATCH_TIMEOUT_SECONDS = 60;

    /** DashScope 调用客户端。 */
    private final DashScopeClient dashScopeClient;
    /** JSON 序列化器，用于构造 LLM 入参。 */
    private final ObjectMapper objectMapper = new ObjectMapper();
    /** 服务关闭标记，防止关闭后继续提交任务。 */
    private final AtomicBoolean closed = new AtomicBoolean(false);

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
     * @param concurrency 并发提示值
     * @return 复判后的问题列表
     */
    public List<Issue> review(List<Issue> issues, String model, int concurrency) {
        if (issues.isEmpty() || closed.get()) {
            return issues;
        }

        // 模块 1：按调用方提示生成本批固定并发。
        int parallelism = resolveParallelism(concurrency);
        ExecutorService executorService = new ThreadPoolExecutor(
                parallelism,
                parallelism,
                0L,
                TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<Runnable>(QUEUE_CAPACITY),
                new ThreadPoolExecutor.CallerRunsPolicy());
        try {
            // 模块 2：将每条 Issue 转换为一个并发任务。
            List<Callable<Issue>> tasks = new ArrayList<Callable<Issue>>();
            for (Issue issue : issues) {
                tasks.add(() -> reviewOne(issue, model));
            }

            // 模块 3：按批次超时执行并收集 Future 结果。
            List<Future<Issue>> futures = executorService.invokeAll(tasks, BATCH_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            List<Issue> reviewed = new ArrayList<Issue>();
            for (Future<Issue> future : futures) {
                if (future.isCancelled()) {
                    continue;
                }
                try {
                    reviewed.add(future.get());
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    return issues;
                } catch (ExecutionException ex) {
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
        } catch (RuntimeException ex) {
            return issues;
        } finally {
            executorService.shutdownNow();
        }
    }

    /**
     * 释放线程池资源。
     */
    @Override
    public void close() {
        closed.set(true);
    }

    /**
     * 并发只受调用方提示和固定上限影响。
     */
    private int resolveParallelism(int concurrencyHint) {
        int requestedConcurrency = concurrencyHint > 0 ? concurrencyHint : DEFAULT_CONCURRENCY;
        return clamp(requestedConcurrency, MIN_CONCURRENCY, MAX_CONCURRENCY);
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
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
