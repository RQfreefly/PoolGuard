package cn.rqfreefly.parser;

/**
 * 关闭调用点。
 */
public final class ShutdownCall {
    /** 调用点所在文件路径。 */
    private final String filePath;
    /** 调用点所在类名。 */
    private final String className;
    /** 调用点所在方法名。 */
    private final String methodName;
    /** 调用关闭方法的变量名。 */
    private final String variableName;
    /** 调用语句所在行号。 */
    private final int line;
    /** 是否位于 finally 块内。 */
    private final boolean inFinally;

    /**
     * 构造关闭调用事实。
     *
     * @param filePath 文件路径
     * @param className 类名
     * @param methodName 方法名
     * @param variableName 变量名
     * @param line 行号
     * @param inFinally 是否 finally 块内
     */
    public ShutdownCall(String filePath,
                        String className,
                        String methodName,
                        String variableName,
                        int line,
                        boolean inFinally) {
        this.filePath = filePath;
        this.className = className;
        this.methodName = methodName;
        this.variableName = variableName;
        this.line = line;
        this.inFinally = inFinally;
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
     * @return 变量名
     */
    public String getVariableName() {
        return variableName;
    }

    /**
     * @return 行号
     */
    public int getLine() {
        return line;
    }

    /**
     * @return 是否在 finally 块中
     */
    public boolean isInFinally() {
        return inFinally;
    }
}
