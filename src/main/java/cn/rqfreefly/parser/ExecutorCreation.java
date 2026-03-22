package cn.rqfreefly.parser;

/**
 * 线程池创建点。
 */
public final class ExecutorCreation {
    /** 创建点所在文件路径。 */
    private final String filePath;
    /** 创建点所在类名。 */
    private final String className;
    /** 创建点所在方法名。 */
    private final String methodName;
    /** 创建出的线程池变量名。 */
    private final String variableName;
    /** 创建语句所在行号。 */
    private final int line;
    /** 是否位于循环结构中。 */
    private final boolean inLoop;
    /** 是否位于递归方法中。 */
    private final boolean inRecursiveMethod;
    /** 是否处于高频调用入口。 */
    private final boolean highFrequencyEntry;
    /** 是否为定时调度线程池。 */
    private final boolean scheduledExecutor;
    /** 是否使用默认线程工厂。 */
    private final boolean defaultThreadFactory;
    /** 是否由类字段持有该线程池。 */
    private final boolean fieldOwner;
    /** 持有字段是否为 static。 */
    private final boolean staticField;
    /** 是否使用无界队列。 */
    private final boolean unboundedQueue;

    /**
     * 构造线程池创建事实。
     *
     * @param filePath 文件路径
     * @param className 类名
     * @param methodName 方法名
     * @param variableName 变量名
     * @param line 行号
     * @param inLoop 是否位于循环
     * @param inRecursiveMethod 是否位于递归方法
     * @param highFrequencyEntry 是否高频入口
     * @param scheduledExecutor 是否定时线程池
     * @param defaultThreadFactory 是否默认线程工厂
     * @param fieldOwner 是否字段所有者
     * @param staticField 是否静态字段
     * @param unboundedQueue 是否无界队列
     */
    public ExecutorCreation(String filePath,
                            String className,
                            String methodName,
                            String variableName,
                            int line,
                            boolean inLoop,
                            boolean inRecursiveMethod,
                            boolean highFrequencyEntry,
                            boolean scheduledExecutor,
                            boolean defaultThreadFactory,
                            boolean fieldOwner,
                            boolean staticField,
                            boolean unboundedQueue) {
        this.filePath = filePath;
        this.className = className;
        this.methodName = methodName;
        this.variableName = variableName;
        this.line = line;
        this.inLoop = inLoop;
        this.inRecursiveMethod = inRecursiveMethod;
        this.highFrequencyEntry = highFrequencyEntry;
        this.scheduledExecutor = scheduledExecutor;
        this.defaultThreadFactory = defaultThreadFactory;
        this.fieldOwner = fieldOwner;
        this.staticField = staticField;
        this.unboundedQueue = unboundedQueue;
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
     * @return 是否处于循环中
     */
    public boolean isInLoop() {
        return inLoop;
    }

    /**
     * @return 是否处于递归方法中
     */
    public boolean isInRecursiveMethod() {
        return inRecursiveMethod;
    }

    /**
     * @return 是否高频入口
     */
    public boolean isHighFrequencyEntry() {
        return highFrequencyEntry;
    }

    /**
     * @return 是否定时线程池
     */
    public boolean isScheduledExecutor() {
        return scheduledExecutor;
    }

    /**
     * @return 是否默认线程工厂
     */
    public boolean isDefaultThreadFactory() {
        return defaultThreadFactory;
    }

    /**
     * @return 是否字段持有
     */
    public boolean isFieldOwner() {
        return fieldOwner;
    }

    /**
     * @return 是否静态字段
     */
    public boolean isStaticField() {
        return staticField;
    }

    /**
     * @return 是否无界队列
     */
    public boolean isUnboundedQueue() {
        return unboundedQueue;
    }
}
