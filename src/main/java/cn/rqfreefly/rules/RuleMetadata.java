package cn.rqfreefly.rules;

import cn.rqfreefly.core.RuleId;
import cn.rqfreefly.core.Severity;

/**
 * 规则元数据。
 */
public final class RuleMetadata {
    /** 规则唯一标识。 */
    private final RuleId ruleId;
    /** 规则默认严重级别。 */
    private final Severity severity;
    /** 规则标题。 */
    private final String title;
    /** 规则描述。 */
    private final String description;
    /** 规则默认风险分。 */
    private final int defaultRiskScore;

    /**
     * 构造规则元数据。
     *
     * @param ruleId 规则 ID
     * @param severity 默认严重级别
     * @param title 规则标题
     * @param description 规则描述
     * @param defaultRiskScore 默认风险分
     */
    public RuleMetadata(RuleId ruleId, Severity severity, String title, String description, int defaultRiskScore) {
        this.ruleId = ruleId;
        this.severity = severity;
        this.title = title;
        this.description = description;
        this.defaultRiskScore = defaultRiskScore;
    }

    /**
     * @return 规则 ID
     */
    public RuleId getRuleId() {
        return ruleId;
    }

    /**
     * @return 默认严重级别
     */
    public Severity getSeverity() {
        return severity;
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
     * @return 默认风险分
     */
    public int getDefaultRiskScore() {
        return defaultRiskScore;
    }
}
