package cn.rqfreefly.core;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 单条风险问题的结构化表示。
 */
public final class Issue {
    /** 命中的规则编号。 */
    private final RuleId ruleId;
    /** 风险严重级别。 */
    private final Severity severity;
    /** 风险评分，通常用于排序与阈值过滤。 */
    private final int riskScore;
    /** 问题标题，用于报告摘要展示。 */
    private final String title;
    /** 问题描述，说明触发背景与现象。 */
    private final String description;
    /** 命中问题的文件路径。 */
    private final String filePath;
    /** 命中问题的代码行号。 */
    private final int line;
    /** 结构化证据，包含命中规则的上下文字段。 */
    private final Map<String, Object> evidence;
    /** 原因说明，可来自规则模板或 LLM 复判。 */
    private final String reason;
    /** 修复建议，可来自规则模板或 LLM 复判。 */
    private final String fixSuggestion;
    /** 置信度，纯规则模式下可能为空。 */
    private final Double confidence;
    /** 是否建议人工复核。 */
    private final boolean needsHumanReview;

    /**
     * 构造基础问题对象。
     * 当未启用 LLM 时会使用默认 reason/fix，保证报告字段结构稳定。
     *
     * @param ruleId 规则标识
     * @param severity 严重级别
     * @param riskScore 风险评分
     * @param title 标题
     * @param description 描述
     * @param filePath 文件路径
     * @param line 行号
     * @param evidence 结构化证据
     */
    public Issue(RuleId ruleId,
                 Severity severity,
                 int riskScore,
                 String title,
                 String description,
                 String filePath,
                 int line,
                 Map<String, Object> evidence) {
        this(ruleId, severity, riskScore, title, description, filePath, line, evidence,
                description, "请将线程池纳入可控生命周期并补齐关闭路径。", null, true);
    }

    /**
     * 构造完整问题对象。
     *
     * @param ruleId 规则标识
     * @param severity 严重级别
     * @param riskScore 风险评分
     * @param title 标题
     * @param description 描述
     * @param filePath 文件路径
     * @param line 行号
     * @param evidence 结构化证据
     * @param reason 原因说明
     * @param fixSuggestion 修复建议
     * @param confidence 置信度
     * @param needsHumanReview 是否需要人工复核
     */
    public Issue(RuleId ruleId,
                 Severity severity,
                 int riskScore,
                 String title,
                 String description,
                 String filePath,
                 int line,
                 Map<String, Object> evidence,
                 String reason,
                 String fixSuggestion,
                 Double confidence,
                 boolean needsHumanReview) {
        this.ruleId = ruleId;
        this.severity = severity;
        this.riskScore = riskScore;
        this.title = title;
        this.description = description;
        this.filePath = filePath;
        this.line = line;
        this.evidence = evidence == null ? Collections.<String, Object>emptyMap()
                : Collections.unmodifiableMap(new LinkedHashMap<String, Object>(evidence));
        this.reason = reason;
        this.fixSuggestion = fixSuggestion;
        this.confidence = confidence;
        this.needsHumanReview = needsHumanReview;
    }

    /**
     * @return 规则标识
     */
    public RuleId getRuleId() {
        return ruleId;
    }

    /**
     * @return 严重级别
     */
    public Severity getSeverity() {
        return severity;
    }

    /**
     * @return 风险评分
     */
    public int getRiskScore() {
        return riskScore;
    }

    /**
     * @return 标题
     */
    public String getTitle() {
        return title;
    }

    /**
     * @return 描述
     */
    public String getDescription() {
        return description;
    }

    /**
     * @return 文件路径
     */
    public String getFilePath() {
        return filePath;
    }

    /**
     * @return 行号
     */
    public int getLine() {
        return line;
    }

    /**
     * @return 结构化证据
     */
    public Map<String, Object> getEvidence() {
        return evidence;
    }

    /**
     * @return 风险原因说明
     */
    public String getReason() {
        return reason;
    }

    /**
     * @return 修复建议
     */
    public String getFixSuggestion() {
        return fixSuggestion;
    }

    /**
     * @return 置信度，可能为空（纯规则模式）
     */
    public Double getConfidence() {
        return confidence;
    }

    /**
     * @return 是否需要人工复核
     */
    public boolean isNeedsHumanReview() {
        return needsHumanReview;
    }

    /**
     * 以不可变方式附加 LLM 复判信息。
     * 使用复制而非原地修改，是为了避免并发复判时共享对象被意外覆盖。
     *
     * @param reason LLM 给出的原因
     * @param fixSuggestion LLM 给出的建议
     * @param confidence LLM 置信度
     * @param needsHumanReview 是否建议人工复核
     * @return 带复判信息的新问题对象
     */
    public Issue withLlmReview(String reason, String fixSuggestion, Double confidence, boolean needsHumanReview) {
        return new Issue(
                this.ruleId,
                this.severity,
                this.riskScore,
                this.title,
                this.description,
                this.filePath,
                this.line,
                this.evidence,
                reason,
                fixSuggestion,
                confidence,
                needsHumanReview
        );
    }
}
