package cn.rqfreefly.llm;

/**
 * LLM 复判结果对象。
 */
public final class LlmReviewResult {
    /** LLM 给出的风险级别。 */
    private final String riskLevel;
    /** LLM 给出的风险判断置信度。 */
    private final double confidence;
    /** LLM 输出的原因说明。 */
    private final String reason;
    /** LLM 输出的修复建议。 */
    private final String fixSuggestion;
    /** 是否建议人工复核该结论。 */
    private final boolean needsHumanReview;

    /**
     * 构造 LLM 复判结果。
     *
     * @param riskLevel 风险级别
     * @param confidence 置信度
     * @param reason 原因
     * @param fixSuggestion 建议
     * @param needsHumanReview 是否需要人工复核
     */
    public LlmReviewResult(String riskLevel, double confidence, String reason, String fixSuggestion, boolean needsHumanReview) {
        this.riskLevel = riskLevel;
        this.confidence = confidence;
        this.reason = reason;
        this.fixSuggestion = fixSuggestion;
        this.needsHumanReview = needsHumanReview;
    }

    /**
     * @return 风险级别
     */
    public String getRiskLevel() {
        return riskLevel;
    }

    /**
     * @return 置信度
     */
    public double getConfidence() {
        return confidence;
    }

    /**
     * @return 原因说明
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
     * @return 是否需要人工复核
     */
    public boolean isNeedsHumanReview() {
        return needsHumanReview;
    }
}
