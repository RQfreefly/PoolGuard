package cn.rqfreefly.core;

/**
 * 风险严重级别。
 */
public enum Severity {
    /** 可导致系统稳定性或可用性显著受损，需优先处理。 */
    CRITICAL(4),
    /** 高风险问题，建议在近期迭代内完成修复。 */
    HIGH(3),
    /** 中风险问题，建议纳入常规治理计划。 */
    MEDIUM(2),
    /** 低风险问题，建议在重构或例行优化时处理。 */
    LOW(1);

    /** 用于排序的稳定权重，数值越大表示越严重。 */
    private final int order;

    Severity(int order) {
        this.order = order;
    }

    /**
     * 返回稳定排序权重。
     * 之所以维护显式数值而不是依赖枚举声明顺序，是为了避免未来插入新级别时破坏排序语义。
     *
     * @return 严重级别排序值，越大越严重
     */
    public int getOrder() {
        return order;
    }
}
