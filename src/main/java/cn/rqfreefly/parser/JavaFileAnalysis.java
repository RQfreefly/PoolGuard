package cn.rqfreefly.parser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * 单文件分析结果。
 */
public final class JavaFileAnalysis {
    /** 被分析的 Java 文件路径。 */
    private final String filePath;
    /** 文件中识别到的线程池创建点集合。 */
    private final List<ExecutorCreation> creations;
    /** 文件中识别到的关闭调用点集合。 */
    private final List<ShutdownCall> shutdownCalls;
    /** 在 @PreDestroy 中执行关闭的字段名集合。 */
    private final List<String> preDestroyShutdownFields;
    /** 由框架管理的 Bean 销毁方法名集合。 */
    private final List<String> beanDestroyMethods;
    /** 在 shutdown hook 中关闭的字段名集合。 */
    private final List<String> shutdownHookFields;
    /** 文件中的方法级事实集合。 */
    private final List<MethodFacts> methodFacts;
    /** 实现 AutoCloseable 的类名集合。 */
    private final List<String> autoCloseableClasses;
    /** 涉及调度任务的变量名集合。 */
    private final List<String> scheduledVariables;
    /** 被 cancel 调用过的变量名集合。 */
    private final List<String> cancelVariables;
    /** 当前文件声明的方法名集合。 */
    private final List<String> declaredMethodNames;
    /** 当前文件调用过的方法名集合。 */
    private final List<String> calledMethodNames;
    /** 当前文件解析是否成功。 */
    private final boolean parseSuccess;
    /** 当前文件解析失败时的错误信息。 */
    private final String parseError;

    /**
     * 构造单文件分析结果。
     *
     * @param filePath 文件路径
     * @param creations 创建点
     * @param shutdownCalls 关闭点
     * @param preDestroyShutdownFields 预销毁关闭字段
     * @param beanDestroyMethods Bean 销毁方法
     * @param shutdownHookFields shutdown hook 关闭字段
     * @param methodFacts 方法事实
     * @param autoCloseableClasses AutoCloseable 类名集合
     * @param scheduledVariables 定时调度变量
     * @param cancelVariables cancel 变量
     * @param declaredMethodNames 声明的方法名
     * @param calledMethodNames 调用的方法名
     * @param parseSuccess 是否解析成功
     * @param parseError 解析错误
     */
    public JavaFileAnalysis(String filePath,
                            List<ExecutorCreation> creations,
                            List<ShutdownCall> shutdownCalls,
                            Set<String> preDestroyShutdownFields,
                            Set<String> beanDestroyMethods,
                            Set<String> shutdownHookFields,
                            List<MethodFacts> methodFacts,
                            Set<String> autoCloseableClasses,
                            Set<String> scheduledVariables,
                            Set<String> cancelVariables,
                            Set<String> declaredMethodNames,
                            Set<String> calledMethodNames,
                            boolean parseSuccess,
                            String parseError) {
        this.filePath = filePath;
        this.creations = Collections.unmodifiableList(new ArrayList<ExecutorCreation>(creations));
        this.shutdownCalls = Collections.unmodifiableList(new ArrayList<ShutdownCall>(shutdownCalls));
        this.preDestroyShutdownFields = Collections.unmodifiableList(new ArrayList<String>(preDestroyShutdownFields));
        this.beanDestroyMethods = Collections.unmodifiableList(new ArrayList<String>(beanDestroyMethods));
        this.shutdownHookFields = Collections.unmodifiableList(new ArrayList<String>(shutdownHookFields));
        this.methodFacts = Collections.unmodifiableList(new ArrayList<MethodFacts>(methodFacts));
        this.autoCloseableClasses = Collections.unmodifiableList(new ArrayList<String>(autoCloseableClasses));
        this.scheduledVariables = Collections.unmodifiableList(new ArrayList<String>(scheduledVariables));
        this.cancelVariables = Collections.unmodifiableList(new ArrayList<String>(cancelVariables));
        this.declaredMethodNames = Collections.unmodifiableList(new ArrayList<String>(declaredMethodNames));
        this.calledMethodNames = Collections.unmodifiableList(new ArrayList<String>(calledMethodNames));
        this.parseSuccess = parseSuccess;
        this.parseError = parseError;
    }

    /**
     * @return 文件路径
     */
    public String getFilePath() {
        return filePath;
    }

    /**
     * @return 创建点集合
     */
    public List<ExecutorCreation> getCreations() {
        return creations;
    }

    /**
     * @return 关闭点集合
     */
    public List<ShutdownCall> getShutdownCalls() {
        return shutdownCalls;
    }

    /**
     * @return @PreDestroy 关闭字段集合
     */
    public List<String> getPreDestroyShutdownFields() {
        return preDestroyShutdownFields;
    }

    /**
     * @return Bean 销毁方法集合
     */
    public List<String> getBeanDestroyMethods() {
        return beanDestroyMethods;
    }

    /**
     * @return shutdown hook 关闭字段集合
     */
    public List<String> getShutdownHookFields() {
        return shutdownHookFields;
    }

    /**
     * @return 方法事实集合
     */
    public List<MethodFacts> getMethodFacts() {
        return methodFacts;
    }

    /**
     * @return 实现 AutoCloseable 的类名集合
     */
    public List<String> getAutoCloseableClasses() {
        return autoCloseableClasses;
    }

    /**
     * @return 调度变量集合
     */
    public List<String> getScheduledVariables() {
        return scheduledVariables;
    }

    /**
     * @return cancel 变量集合
     */
    public List<String> getCancelVariables() {
        return cancelVariables;
    }

    /**
     * @return 当前文件声明的方法名集合
     */
    public List<String> getDeclaredMethodNames() {
        return declaredMethodNames;
    }

    /**
     * @return 当前文件调用的方法名集合
     */
    public List<String> getCalledMethodNames() {
        return calledMethodNames;
    }

    /**
     * @return 是否解析成功
     */
    public boolean isParseSuccess() {
        return parseSuccess;
    }

    /**
     * @return 解析错误信息，成功时为 null
     */
    public String getParseError() {
        return parseError;
    }
}
