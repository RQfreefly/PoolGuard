package cn.rqfreefly.parser;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.Position;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.NormalAnnotationExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.stmt.ForEachStmt;
import com.github.javaparser.ast.stmt.ForStmt;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.ast.stmt.TryStmt;
import com.github.javaparser.ast.stmt.WhileStmt;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * 基于 JavaParser 的 Java 源码解析器。
 * 解析层只产出“可复用事实”，不直接做规则判定，
 * 这样可以把规则迭代与 AST 提取解耦，减少后续维护成本。
 */
public final class JavaSourceParser {

    private final JavaParser javaParser = new JavaParser();

    /**
     * 解析 Java 文件并提取规则所需事实。
     * 失败时返回 parseSuccess=false 的结果对象，而不是抛异常中断全局扫描。
     * 解析阶段只负责“采集事实”，不负责“下结论”，这样规则变更时不需要频繁改 AST 逻辑。
     *
     * @param filePath Java 文件路径
     * @return 文件分析结果
     */
    public JavaFileAnalysis parse(Path filePath) {
        try {
            // 模块 1：读取源码并执行 AST 解析。
            String source = new String(Files.readAllBytes(filePath), StandardCharsets.UTF_8);
            ParseResult<CompilationUnit> parseResult = javaParser.parse(filePath);
            if (!parseResult.isSuccessful() || !parseResult.getResult().isPresent()) {
                return buildErrorResult(filePath.toString(), parseResult.getProblems().toString());
            }

            // 模块 2：初始化本文件的事实收集容器。
            CompilationUnit compilationUnit = parseResult.getResult().get();
            List<ExecutorCreation> creations = new ArrayList<ExecutorCreation>();
            List<ShutdownCall> shutdownCalls = new ArrayList<ShutdownCall>();
            Set<String> preDestroyShutdownFields = new HashSet<String>();
            Set<String> beanDestroyMethods = new HashSet<String>();
            Set<String> shutdownHookFields = new HashSet<String>();
            List<MethodFacts> methodFacts = new ArrayList<MethodFacts>();
            Set<String> autoCloseableClasses = new HashSet<String>();
            Set<String> scheduledVariables = new HashSet<String>();
            Set<String> cancelVariables = new HashSet<String>();
            Set<String> declaredMethodNames = new HashSet<String>();
            Set<String> calledMethodNames = new HashSet<String>();

            // 模块 3：遍历类声明，提取字段与方法事实。
            for (ClassOrInterfaceDeclaration clazz : compilationUnit.findAll(ClassOrInterfaceDeclaration.class)) {
                String className = clazz.getNameAsString();
                boolean controllerClass = hasAnyAnnotation(clazz.getAnnotations(), "RestController", "Controller");
                if (implementsInterface(clazz, "AutoCloseable", "Closeable")) {
                    autoCloseableClasses.add(className);
                }

                // 子模块 3.1：采集字段初始化中的线程池创建点。
                for (FieldDeclaration fieldDeclaration : clazz.getFields()) {
                    for (VariableDeclarator variableDeclarator : fieldDeclaration.getVariables()) {
                        Optional<Expression> initializer = variableDeclarator.getInitializer();
                        if (!initializer.isPresent()) {
                            continue;
                        }
                        CreationDescriptor descriptor = describeCreation(initializer.get());
                        if (descriptor == null) {
                            continue;
                        }
                        creations.add(new ExecutorCreation(
                                filePath.toString(),
                                className,
                                "<field>",
                                variableDeclarator.getNameAsString(),
                                getLine(variableDeclarator),
                                false,
                                false,
                                false,
                                descriptor.scheduledExecutor,
                                descriptor.defaultThreadFactory,
                                true,
                                fieldDeclaration.isStatic(),
                                descriptor.unboundedQueue));
                    }
                }

                // 子模块 3.2：采集 @Bean(destroyMethod=...) 关闭语义。
                for (MethodDeclaration methodDeclaration : clazz.getMethods()) {
                    declaredMethodNames.add(methodDeclaration.getNameAsString());
                    if (!hasAnyAnnotation(methodDeclaration.getAnnotations(), "Bean")) {
                        continue;
                    }
                    for (AnnotationExpr annotationExpr : methodDeclaration.getAnnotations()) {
                        if (!(annotationExpr instanceof NormalAnnotationExpr)) {
                            continue;
                        }
                        NormalAnnotationExpr normal = (NormalAnnotationExpr) annotationExpr;
                        if (!"Bean".equals(normal.getNameAsString())) {
                            continue;
                        }
                        normal.getPairs().forEach(pair -> {
                            if ("destroyMethod".equals(pair.getNameAsString()) && pair.getValue() instanceof StringLiteralExpr) {
                                StringLiteralExpr literalExpr = (StringLiteralExpr) pair.getValue();
                                if (!"".equals(literalExpr.getValue())) {
                                    beanDestroyMethods.add(methodDeclaration.getNameAsString());
                                }
                            }
                        });
                    }
                }

                // 子模块 3.3：采集方法体中的创建、关闭、调度与取消事实。
                for (MethodDeclaration methodDeclaration : clazz.getMethods()) {
                    String methodName = methodDeclaration.getNameAsString();
                    boolean recursiveMethod = isRecursiveMethod(methodDeclaration);
                    // 控制器/RPC 入口通常调用频率高，这里先打标，后面规则可以直接复用。
                    boolean highFrequencyEntry = controllerClass || hasAnyAnnotation(methodDeclaration.getAnnotations(),
                            "GetMapping", "PostMapping", "PutMapping", "DeleteMapping", "RequestMapping");
                    int returnCount = methodDeclaration.findAll(ReturnStmt.class).size();
                    boolean hasCatch = !methodDeclaration.findAll(com.github.javaparser.ast.stmt.CatchClause.class).isEmpty();
                    methodFacts.add(new MethodFacts(filePath.toString(), className, methodName, returnCount, hasCatch));

                    Map<String, String> futureToScheduler = new HashMap<String, String>();

                    // 3.3.1：变量声明中的创建点与 schedule/future 关联信息。
                    for (VariableDeclarator variableDeclarator : methodDeclaration.findAll(VariableDeclarator.class)) {
                        Optional<Expression> initializer = variableDeclarator.getInitializer();
                        if (!initializer.isPresent()) {
                            continue;
                        }
                        Expression initExpr = initializer.get();
                        CreationDescriptor descriptor = describeCreation(initExpr);
                        if (descriptor != null) {
                            creations.add(new ExecutorCreation(
                                    filePath.toString(),
                                    className,
                                    methodName,
                                    variableDeclarator.getNameAsString(),
                                    getLine(variableDeclarator),
                                    inLoop(variableDeclarator),
                                    recursiveMethod,
                                    highFrequencyEntry,
                                    descriptor.scheduledExecutor,
                                    descriptor.defaultThreadFactory,
                                    false,
                                    false,
                                    descriptor.unboundedQueue));
                        }
                        if (initExpr instanceof MethodCallExpr) {
                            MethodCallExpr mc = (MethodCallExpr) initExpr;
                            if (isScheduleCall(mc) && mc.getScope().isPresent()) {
                                String schedulerVar = extractVariableName(mc.getScope().get());
                                if (schedulerVar != null) {
                                    scheduledVariables.add(schedulerVar);
                                    // cancel 通常发生在 future 上，这里先建映射，后面才能反推到具体 scheduler。
                                    futureToScheduler.put(variableDeclarator.getNameAsString(), schedulerVar);
                                }
                            }
                        }
                    }

                    // 3.3.2：return 表达式中的线程池创建点。
                    for (ReturnStmt returnStmt : methodDeclaration.findAll(ReturnStmt.class)) {
                        if (!returnStmt.getExpression().isPresent()) {
                            continue;
                        }
                        CreationDescriptor descriptor = describeCreation(returnStmt.getExpression().get());
                        if (descriptor == null) {
                            continue;
                        }
                        creations.add(new ExecutorCreation(
                                filePath.toString(),
                                className,
                                methodName,
                                null,
                                getLine(returnStmt),
                                inLoop(returnStmt),
                                recursiveMethod,
                                highFrequencyEntry,
                                descriptor.scheduledExecutor,
                                descriptor.defaultThreadFactory,
                                false,
                                false,
                                descriptor.unboundedQueue));
                    }

                    // 3.3.3：赋值表达式中的线程池创建点。
                    for (AssignExpr assignExpr : methodDeclaration.findAll(AssignExpr.class)) {
                        CreationDescriptor descriptor = describeCreation(assignExpr.getValue());
                        if (descriptor == null) {
                            continue;
                        }
                        String varName = extractVariableName(assignExpr.getTarget());
                        creations.add(new ExecutorCreation(
                                filePath.toString(),
                                className,
                                methodName,
                                varName,
                                getLine(assignExpr),
                                inLoop(assignExpr),
                                recursiveMethod,
                                highFrequencyEntry,
                                descriptor.scheduledExecutor,
                                descriptor.defaultThreadFactory,
                                false,
                                false,
                                descriptor.unboundedQueue));
                    }

                    // 3.3.4：方法调用中的 shutdown/schedule/cancel 证据。
                    for (MethodCallExpr callExpr : methodDeclaration.findAll(MethodCallExpr.class)) {
                        calledMethodNames.add(callExpr.getNameAsString());

                        if (isShutdownLike(callExpr)) {
                            String variableName = callExpr.getScope().isPresent() ? extractVariableName(callExpr.getScope().get()) : null;
                            boolean inFinally = isInFinallyBlock(callExpr);
                            shutdownCalls.add(new ShutdownCall(
                                    filePath.toString(),
                                    className,
                                    methodName,
                                    variableName,
                                    getLine(callExpr),
                                    inFinally));

                            if (hasAnyAnnotation(methodDeclaration.getAnnotations(), "PreDestroy") && variableName != null) {
                                preDestroyShutdownFields.add(variableName);
                            }
                        }

                        if (isScheduleCall(callExpr) && callExpr.getScope().isPresent()) {
                            String schedulerVar = extractVariableName(callExpr.getScope().get());
                            if (schedulerVar != null) {
                                scheduledVariables.add(schedulerVar);
                            }
                        }

                        if (isCancelCall(callExpr) && callExpr.getScope().isPresent()) {
                            String cancelVar = extractVariableName(callExpr.getScope().get());
                            if (cancelVar != null) {
                                if (futureToScheduler.containsKey(cancelVar)) {
                                    // 这个反查的意义是：把 future.cancel 与 scheduler 关联起来，避免 PG008 漏报。
                                    cancelVariables.add(futureToScheduler.get(cancelVar));
                                }
                                cancelVariables.add(cancelVar);
                            }
                        }
                    }
                }
            }

            // 模块 4：文本兜底识别 shutdownHook 场景（补足 AST 局限）。
            if (source.contains("addShutdownHook")) {
                // shutdown hook 可能写成方法引用，纯 AST 变量流不一定能直接连上，这里做一次文本兜底。
                for (ExecutorCreation creation : creations) {
                    if (!creation.isStaticField() || creation.getVariableName() == null) {
                        continue;
                    }
                    String varName = creation.getVariableName();
                    if (source.contains(varName + "::shutdown") || source.contains(varName + ".shutdown(")) {
                        // 有明确 shutdown 痕迹时就记录，后续用于抑制 PG009。
                        shutdownHookFields.add(varName);
                    }
                }
            }

            // 模块 5：组装并返回成功解析结果。
            return new JavaFileAnalysis(filePath.toString(),
                    creations,
                    shutdownCalls,
                    preDestroyShutdownFields,
                    beanDestroyMethods,
                    shutdownHookFields,
                    methodFacts,
                    autoCloseableClasses,
                    scheduledVariables,
                    cancelVariables,
                    declaredMethodNames,
                    calledMethodNames,
                    true,
                    null);
        } catch (Exception ex) {
            // 模块 6：将异常转为失败结果，避免中断整体扫描。
            return buildErrorResult(filePath.toString(), ex.getMessage());
        }
    }

    private JavaFileAnalysis buildErrorResult(String filePath, String error) {
        // 模块：返回“空事实 + 错误信息”的标准失败对象。
        return new JavaFileAnalysis(filePath,
                new ArrayList<ExecutorCreation>(),
                new ArrayList<ShutdownCall>(),
                new HashSet<String>(),
                new HashSet<String>(),
                new HashSet<String>(),
                new ArrayList<MethodFacts>(),
                new HashSet<String>(),
                new HashSet<String>(),
                new HashSet<String>(),
                new HashSet<String>(),
                new HashSet<String>(),
                false,
                error);
    }

    private boolean isRecursiveMethod(MethodDeclaration methodDeclaration) {
        // 模块：判断方法体是否出现“调用自身”的语句。
        String methodName = methodDeclaration.getNameAsString();
        // 递归里创建线程池通常会随深度重复发生，提前打标便于 PG002 直接判断。
        return methodDeclaration.findAll(MethodCallExpr.class, callExpr -> methodName.equals(callExpr.getNameAsString())).size() > 0;
    }

    private boolean hasAnyAnnotation(List<AnnotationExpr> annotations, String... names) {
        // 模块：遍历注解并匹配候选名称集合。
        for (AnnotationExpr annotationExpr : annotations) {
            String annotationName = annotationExpr.getNameAsString();
            for (String name : names) {
                if (name.equals(annotationName) || annotationName.endsWith("." + name)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean implementsInterface(ClassOrInterfaceDeclaration clazz, String... names) {
        // 模块：判断类是否实现目标接口之一。
        for (com.github.javaparser.ast.type.ClassOrInterfaceType type : clazz.getImplementedTypes()) {
            String value = type.getNameAsString();
            for (String name : names) {
                if (name.equals(value) || value.endsWith("." + name)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean inLoop(Node node) {
        // 模块：识别节点是否位于常见循环结构中。
        return node.findAncestor(ForStmt.class).isPresent()
                || node.findAncestor(ForEachStmt.class).isPresent()
                || node.findAncestor(WhileStmt.class).isPresent()
                || node.findAncestor(com.github.javaparser.ast.stmt.DoStmt.class).isPresent();
    }

    private boolean isInFinallyBlock(MethodCallExpr callExpr) {
        // PG003-B 重点看异常路径是否可达关闭点，finally 是最稳定的“全路径覆盖”证据。
        // 模块 1：先找到当前调用所在代码块。
        Optional<com.github.javaparser.ast.stmt.BlockStmt> blockAncestor = callExpr.findAncestor(com.github.javaparser.ast.stmt.BlockStmt.class);
        if (!blockAncestor.isPresent()) {
            return false;
        }
        // 模块 2：再判断该代码块是否正好是某个 try 的 finally 块。
        Optional<TryStmt> tryStmt = blockAncestor.get().findAncestor(TryStmt.class);
        if (!tryStmt.isPresent()) {
            return false;
        }
        return tryStmt.get().getFinallyBlock().isPresent() && tryStmt.get().getFinallyBlock().get() == blockAncestor.get();
    }

    private boolean isShutdownLike(MethodCallExpr callExpr) {
        String name = callExpr.getNameAsString();
        return "shutdown".equals(name) || "shutdownNow".equals(name) || "close".equals(name);
    }

    private boolean isScheduleCall(MethodCallExpr callExpr) {
        String name = callExpr.getNameAsString();
        return "scheduleAtFixedRate".equals(name) || "scheduleWithFixedDelay".equals(name);
    }

    private boolean isCancelCall(MethodCallExpr callExpr) {
        return "cancel".equals(callExpr.getNameAsString());
    }

    private String extractVariableName(Expression expression) {
        // 模块：从不同表达式节点中抽取变量名。
        if (expression instanceof NameExpr) {
            return ((NameExpr) expression).getNameAsString();
        }
        if (expression instanceof FieldAccessExpr) {
            return ((FieldAccessExpr) expression).getNameAsString();
        }
        return null;
    }

    private CreationDescriptor describeCreation(Expression expression) {
        // 模块 1：识别 Executors.newXxx(...) 风格创建。
        if (expression instanceof MethodCallExpr) {
            MethodCallExpr callExpr = (MethodCallExpr) expression;
            if (!callExpr.getScope().isPresent()) {
                return null;
            }
            String scope = callExpr.getScope().get().toString();
            String name = callExpr.getNameAsString();
            if (!"Executors".equals(scope) || !name.startsWith("new")) {
                return null;
            }
            boolean scheduled = name.contains("Scheduled");
            boolean defaultFactory = isDefaultFactoryByExecutors(callExpr);
            return new CreationDescriptor(scheduled, defaultFactory, false);
        }

        // 模块 2：识别 new ThreadPoolExecutor(...) 风格创建。
        if (expression instanceof ObjectCreationExpr) {
            ObjectCreationExpr creationExpr = (ObjectCreationExpr) expression;
            String type = creationExpr.getType().asString();
            if (!type.endsWith("ThreadPoolExecutor")
                    && !type.endsWith("ScheduledThreadPoolExecutor")
                    && !type.endsWith("ForkJoinPool")) {
                return null;
            }
            boolean scheduled = type.endsWith("ScheduledThreadPoolExecutor");
            boolean defaultFactory = isDefaultFactoryByConstructor(creationExpr);
            boolean unboundedQueue = isUnboundedQueue(creationExpr);
            return new CreationDescriptor(scheduled, defaultFactory, unboundedQueue);
        }

        return null;
    }

    private boolean isDefaultFactoryByExecutors(MethodCallExpr callExpr) {
        // 模块：按方法名+参数个数判断是否用了默认线程工厂。
        String methodName = callExpr.getNameAsString();
        int argSize = callExpr.getArguments().size();
        if ("newFixedThreadPool".equals(methodName)) {
            return argSize == 1;
        }
        if ("newSingleThreadExecutor".equals(methodName)) {
            return argSize == 0;
        }
        if ("newCachedThreadPool".equals(methodName)) {
            return argSize == 0;
        }
        if ("newScheduledThreadPool".equals(methodName)) {
            return argSize == 1;
        }
        if ("newSingleThreadScheduledExecutor".equals(methodName)) {
            return argSize == 0;
        }
        if ("newWorkStealingPool".equals(methodName)) {
            return true;
        }
        return true;
    }

    private boolean isDefaultFactoryByConstructor(ObjectCreationExpr creationExpr) {
        // 模块：按构造参数个数判断是否显式配置线程工厂。
        String type = creationExpr.getType().asString();
        int argSize = creationExpr.getArguments().size();
        if (type.endsWith("ScheduledThreadPoolExecutor")) {
            return argSize <= 1;
        }
        if (type.endsWith("ThreadPoolExecutor")) {
            return argSize <= 5;
        }
        return false;
    }

    private boolean isUnboundedQueue(ObjectCreationExpr creationExpr) {
        // 模块 1：筛选 ThreadPoolExecutor 并确保队列参数存在。
        String type = creationExpr.getType().asString();
        if (!type.endsWith("ThreadPoolExecutor") || creationExpr.getArguments().size() < 5) {
            return false;
        }
        // 模块 2：判断队列类型是否为 LinkedBlockingQueue。
        Expression queueExpr = creationExpr.getArgument(4);
        if (!(queueExpr instanceof ObjectCreationExpr)) {
            return false;
        }
        ObjectCreationExpr queueCreation = (ObjectCreationExpr) queueExpr;
        String queueType = queueCreation.getType().asString();
        if (!queueType.endsWith("LinkedBlockingQueue") && !queueType.contains("LinkedBlockingQueue")) {
            return false;
        }
        // 仅在构造参数为空时判定无界，避免把显式容量队列误报为 PG007。
        return queueCreation.getArguments().isEmpty();
    }

    private int getLine(Node node) {
        // 模块：读取节点起始行，缺失时回退到第 1 行。
        Optional<Position> begin = node.getBegin();
        return begin.isPresent() ? begin.get().line : 1;
    }

    private static final class CreationDescriptor {
        private final boolean scheduledExecutor;
        private final boolean defaultThreadFactory;
        private final boolean unboundedQueue;

        private CreationDescriptor(boolean scheduledExecutor, boolean defaultThreadFactory, boolean unboundedQueue) {
            this.scheduledExecutor = scheduledExecutor;
            this.defaultThreadFactory = defaultThreadFactory;
            this.unboundedQueue = unboundedQueue;
        }
    }
}
