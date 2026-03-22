package cn.rqfreefly.parser;

/**
 * 方法级事实，用于路径不完整判断。
 */
public final class MethodFacts {
    /** 方法所在文件路径。 */
    private final String filePath;
    /** 方法所在类名。 */
    private final String className;
    /** 方法名称。 */
    private final String methodName;
    /** 方法内 return 语句数量。 */
    private final int returnCount;
    /** 方法中是否包含 catch 分支。 */
    private final boolean hasCatch;

    /**
     * 构造方法事实。
     *
     * @param filePath 文件路径
     * @param className 类名
     * @param methodName 方法名
     * @param returnCount return 语句数量
     * @param hasCatch 是否存在 catch
     */
    public MethodFacts(String filePath, String className, String methodName, int returnCount, boolean hasCatch) {
        this.filePath = filePath;
        this.className = className;
        this.methodName = methodName;
        this.returnCount = returnCount;
        this.hasCatch = hasCatch;
    }

    /**
     * @return 文件路径
     */
    public String getFilePath() {
        return filePath;
    }

    /**
     * @return 类名
     */
    public String getClassName() {
        return className;
    }

    /**
     * @return 方法名
     */
    public String getMethodName() {
        return methodName;
    }

    /**
     * @return return 语句数量
     */
    public int getReturnCount() {
        return returnCount;
    }

    /**
     * @return 是否包含 catch 分支
     */
    public boolean isHasCatch() {
        return hasCatch;
    }
}
