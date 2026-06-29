package cn.rqfreefly.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * DashScope 客户端，固定兼容端点。
 * 该客户端遵循“失败可退化、主流程不中断”的原则，避免外部网络依赖影响扫描可用性。
 */
public final class DashScopeClient {

    private static final String ENDPOINT = "https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions";
    private static final int CONNECT_TIMEOUT_MS = 8000;
    private static final int READ_TIMEOUT_MS = 20000;
    private static final String SYSTEM_PROMPT_RESOURCE = "prompts/llm-review-system-prompt.txt";
    private static final String SYSTEM_PROMPT_FALLBACK = "你是Java线程池泄露分析助手，只返回JSON对象。"
            + "JSON字段必须包含: risk_level, confidence, reason, fix_suggestion, needs_human_review。"
            + "risk_level 仅可为 critical/high/medium/low；confidence 取值 0~1。";

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final String systemPrompt;

    /**
     * 构造客户端并加载可维护的系统提示词模板。
     */
    public DashScopeClient() {
        this.systemPrompt = loadSystemPrompt();
    }

    /**
     * 调用 DashScope 对单条证据进行复判。
     * 无论调用成功还是失败，都会返回结构稳定的对象，
     * 这样上层不需要写大量空值判断，报告渲染也更简单。
     *
     * @param evidenceJson 结构化证据 JSON
     * @param model 模型名
     * @return 复判结果；失败时返回统一 fallback 结果
     */
    public LlmReviewResult review(String evidenceJson, String model) {
        // 模块 1：读取 API Key，缺失则立即降级返回。
        String apiKey = System.getenv("DASHSCOPE_API_KEY");
        if (apiKey == null || "".equals(apiKey.trim())) {
            return fallback("未检测到 DASHSCOPE_API_KEY，回退纯规则结果。");
        }

        // 模块 2：按指数退避策略进行有限次重试。
        int maxRetry = 3;
        long backoffMs = 500L;
        for (int attempt = 1; attempt <= maxRetry; attempt++) {
            try {
                // 子模块 A：构建 HTTP 连接并设置请求头。
                HttpURLConnection connection = (HttpURLConnection) new URL(ENDPOINT).openConnection();
                connection.setRequestMethod("POST");
                connection.setDoOutput(true);
                connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
                connection.setReadTimeout(READ_TIMEOUT_MS);
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setRequestProperty("Authorization", "Bearer " + apiKey);

                // 子模块 B：序列化请求体并发送。
                String requestBody = buildRequestBody(evidenceJson, model);
                OutputStream outputStream = connection.getOutputStream();
                outputStream.write(requestBody.getBytes(StandardCharsets.UTF_8));
                outputStream.flush();
                outputStream.close();

                // 子模块 C：读取响应码与响应体。
                int code = connection.getResponseCode();
                String body = readResponseBody(code >= 200 && code < 300
                        ? connection.getInputStream()
                        : connection.getErrorStream());

                // 子模块 D：按状态码分流处理成功、可重试失败和不可重试失败。
                if (code >= 200 && code < 300) {
                    return parseReviewResult(body);
                }

                if (code == 429 || code >= 500) {
                    // 仅对“可能暂时恢复”的错误重试，参数错误重试只会浪费时间。
                    sleepQuietly(backoffMs);
                    backoffMs = backoffMs * 2;
                    continue;
                }
                return fallback("DashScope 请求失败，HTTP=" + code);
            } catch (Exception ex) {
                // 子模块 E：异常场景同样遵循重试策略，最后一次失败后降级。
                if (attempt == maxRetry) {
                    return fallback("DashScope 调用异常: " + ex.getMessage());
                }
                sleepQuietly(backoffMs);
                backoffMs = backoffMs * 2;
            }
        }

        return fallback("DashScope 多次重试失败，回退纯规则结果。");
    }

    private String buildRequestBody(String evidenceJson, String model) throws Exception {
        // 固定低 temperature + json_object，优先稳定解析，而不是追求文案多样性。
        // 模块 1：构建请求根参数。
        Map<String, Object> root = new LinkedHashMap<String, Object>();
        root.put("model", model);
        root.put("temperature", 0.1);
        // 关闭 thinking 是为了把响应时延稳定在可接受区间，适合批量扫描场景。
        root.put("enable_thinking", false);
        root.put("stream", false);
        Map<String, String> responseFormat = new LinkedHashMap<String, String>();
        responseFormat.put("type", "json_object");
        root.put("response_format", responseFormat);

        // 模块 2：构建 system 消息，明确输出约束。
        Map<String, String> systemMsg = new LinkedHashMap<String, String>();
        systemMsg.put("role", "system");
        systemMsg.put("content", systemPrompt);

        // 模块 3：构建 user 消息，注入当前证据。
        Map<String, String> userMsg = new LinkedHashMap<String, String>();
        userMsg.put("role", "user");
        userMsg.put("content", "请基于以下结构化证据 JSON 完成复判并输出结果：\n" + evidenceJson);

        // 模块 4：序列化为 JSON 字符串。
        root.put("messages", java.util.Arrays.asList(systemMsg, userMsg));
        return objectMapper.writeValueAsString(root);
    }

    private LlmReviewResult parseReviewResult(String responseBody) {
        try {
            // 模块 1：解析外层响应结构并定位 content。
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode contentNode = root.path("choices").path(0).path("message").path("content");
            if (contentNode.isMissingNode()) {
                return fallback("DashScope 返回缺少 content 字段。");
            }

            // 模块 2：解析 content 中的 JSON 结果。
            String content = contentNode.asText();
            JsonNode resultNode;
            try {
                resultNode = objectMapper.readTree(content);
            } catch (Exception ex) {
                // 即使要求 json_object，极端场景仍可能返回纯文本，这里兜底避免任务失败。
                return fallback("DashScope 返回非 JSON 内容。");
            }

            // 模块 3：提取业务字段并应用默认值兜底。
            String riskLevel = asTextOrDefault(resultNode, "risk_level", "unknown");
            double confidence = resultNode.path("confidence").asDouble(0.0);
            String reason = asTextOrDefault(resultNode, "reason", "需要人工复核");
            String fixSuggestion = asTextOrDefault(resultNode, "fix_suggestion", "请结合规则证据补齐生命周期管理");
            boolean review = resultNode.path("needs_human_review").asBoolean(true);
            return new LlmReviewResult(riskLevel, confidence, reason, fixSuggestion, review);
        } catch (Exception ex) {
            return fallback("解析 DashScope 返回失败: " + ex.getMessage());
        }
    }

    private String asTextOrDefault(JsonNode node, String key, String defaultValue) {
        // 模块：字段缺失时返回默认值，避免上层空值判断膨胀。
        JsonNode value = node.path(key);
        return value.isMissingNode() ? defaultValue : value.asText(defaultValue);
    }

    private String readResponseBody(InputStream inputStream) throws Exception {
        // 模块 1：空输入流时返回空字符串。
        if (inputStream == null) {
            return "";
        }
        // 模块 2：逐行读取并拼接响应体。
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            sb.append(line);
        }
        return sb.toString();
    }

    private String loadSystemPrompt() {
        InputStream inputStream = null;
        try {
            inputStream = getClass().getClassLoader().getResourceAsStream(SYSTEM_PROMPT_RESOURCE);
            if (inputStream == null) {
                return SYSTEM_PROMPT_FALLBACK;
            }
            String prompt = readResponseBody(inputStream).trim();
            return "".equals(prompt) ? SYSTEM_PROMPT_FALLBACK : prompt;
        } catch (Exception ex) {
            return SYSTEM_PROMPT_FALLBACK;
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (Exception ignored) {
                    // 忽略关闭异常，使用 fallback 保持可用性。
                }
            }
        }
    }

    private void sleepQuietly(long ms) {
        try {
            // 模块：执行退避等待。
            Thread.sleep(ms);
        } catch (InterruptedException ignored) {
            // 恢复中断标记，避免吞掉外层线程池发出的取消信号。
            Thread.currentThread().interrupt();
        }
    }

    private LlmReviewResult fallback(String reason) {
        // fallback 字段保持完整，报告层就不用分“正常结果”和“异常结果”两套分支。
        // 模块：返回结构稳定的降级结果。
        return new LlmReviewResult(
                "unknown",
                0.0,
                reason,
                "请根据规则证据进行人工复核，并优先补齐 shutdown/cancel/生命周期钩子。",
                true);
    }
}
