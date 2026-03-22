package cn.rqfreefly.core;

/**
 * 风险结果排序维度。
 */
public enum SortBy {
    /** 按严重级别排序（通常从高到低）。 */
    SEVERITY,
    /** 按风险分值排序（通常从高到低）。 */
    SCORE,
    /** 按文件路径字典序排序，便于按目录审查。 */
    PATH
}
