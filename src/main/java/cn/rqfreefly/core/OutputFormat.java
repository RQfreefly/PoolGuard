package cn.rqfreefly.core;

/**
 * 报告输出格式枚举。
 */
public enum OutputFormat {
    /** 结构化 JSON 输出，适合机器处理与平台集成。 */
    JSON,
    /** Markdown 输出，适合人读与评审留痕。 */
    MD,
    /** 纯文本输出，适合终端快速查看。 */
    TEXT
}
