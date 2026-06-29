package cn.rqfreefly.core;

/**
 * 扫描执行配置。
 */
public final class ScanConfig {
    /** 待扫描的目标路径，可为文件或目录。 */
    private final String scanPath;
    /** 报告输出格式。 */
    private final OutputFormat format;
    /** 结果排序维度。 */
    private final SortBy sortBy;
    /** 启用 LLM 时使用的模型名称。 */
    private final String llmModel;
    /** LLM 并发请求数上限。 */
    private final int llmConcurrency;
    /** 是否启用 LLM 复判流程。 */
    private final boolean enableLlm;
    /** 报告输出文件路径，空值表示输出到标准输出。 */
    private final String outputPath;
    /** 增量扫描文件列表路径，空值表示全量扫描。 */
    private final String changedFilesPath;

    /**
     * 构造扫描配置。
     *
     * @param scanPath 扫描路径
     * @param format 输出格式
     * @param sortBy 排序方式
     * @param llmModel LLM 模型
     * @param llmConcurrency LLM 并发
     * @param enableLlm 是否启用 LLM
     * @param outputPath 输出文件路径
     * @param changedFilesPath 增量文件列表路径
     */
    public ScanConfig(String scanPath,
                      OutputFormat format,
                      SortBy sortBy,
                      String llmModel,
                      int llmConcurrency,
                      boolean enableLlm,
                      String outputPath,
                      String changedFilesPath) {
        this.scanPath = scanPath;
        this.format = format;
        this.sortBy = sortBy;
        this.llmModel = llmModel;
        this.llmConcurrency = llmConcurrency;
        this.enableLlm = enableLlm;
        this.outputPath = outputPath;
        this.changedFilesPath = changedFilesPath;
    }

    /**
     * @return 扫描路径
     */
    public String getScanPath() {
        return scanPath;
    }

    /**
     * @return 输出格式
     */
    public OutputFormat getFormat() {
        return format;
    }

    /**
     * @return 排序方式
     */
    public SortBy getSortBy() {
        return sortBy;
    }

    /**
     * @return LLM 模型名
     */
    public String getLlmModel() {
        return llmModel;
    }

    /**
     * @return LLM 并发数
     */
    public int getLlmConcurrency() {
        return llmConcurrency;
    }

    /**
     * @return 是否启用 LLM 复判
     */
    public boolean isEnableLlm() {
        return enableLlm;
    }

    /**
     * @return 输出文件路径，可能为空
     */
    public String getOutputPath() {
        return outputPath;
    }

    /**
     * @return 增量文件列表路径，可能为空
     */
    public String getChangedFilesPath() {
        return changedFilesPath;
    }

    /**
     * @return 是否处于增量扫描模式
     */
    public boolean isIncrementalMode() {
        return changedFilesPath != null && !"".equals(changedFilesPath.trim());
    }
}
