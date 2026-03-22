package cn.rqfreefly.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 一次扫描任务的聚合结果。
 */
public final class ScanResult {
    /** 命中的风险问题列表。 */
    private final List<Issue> issues;
    /** 解析成功的文件数量。 */
    private final int parsedFileCount;
    /** 解析失败的文件数量。 */
    private final int failedFileCount;
    /** 扫描根路径。 */
    private final String projectPath;
    /** 扫描时间戳（ISO-8601 字符串）。 */
    private final String scanTime;
    /** 扫描模式，如 full 或 incremental。 */
    private final String scanMode;
    /** 本次扫描使用的 LLM 模型名。 */
    private final String llmModel;

    /**
     * 构造扫描结果。
     *
     * @param issues 命中的问题列表
     * @param parsedFileCount 解析成功文件数
     * @param failedFileCount 解析失败文件数
     * @param projectPath 项目路径
     * @param scanTime 扫描时间
     * @param scanMode 扫描模式
     * @param llmModel LLM 模型名
     */
    public ScanResult(List<Issue> issues,
                      int parsedFileCount,
                      int failedFileCount,
                      String projectPath,
                      String scanTime,
                      String scanMode,
                      String llmModel) {
        this.issues = Collections.unmodifiableList(new ArrayList<Issue>(issues));
        this.parsedFileCount = parsedFileCount;
        this.failedFileCount = failedFileCount;
        this.projectPath = projectPath;
        this.scanTime = scanTime;
        this.scanMode = scanMode;
        this.llmModel = llmModel;
    }

    /**
     * @return 问题列表
     */
    public List<Issue> getIssues() {
        return issues;
    }

    /**
     * @return 解析成功文件数
     */
    public int getParsedFileCount() {
        return parsedFileCount;
    }

    /**
     * @return 解析失败文件数
     */
    public int getFailedFileCount() {
        return failedFileCount;
    }

    /**
     * @return 项目路径
     */
    public String getProjectPath() {
        return projectPath;
    }

    /**
     * @return 扫描时间（ISO-8601）
     */
    public String getScanTime() {
        return scanTime;
    }

    /**
     * @return 扫描模式（full/incremental）
     */
    public String getScanMode() {
        return scanMode;
    }

    /**
     * @return 使用的 LLM 模型名
     */
    public String getLlmModel() {
        return llmModel;
    }
}
