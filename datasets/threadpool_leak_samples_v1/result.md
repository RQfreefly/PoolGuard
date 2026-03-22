# PoolGuard 扫描报告

## 元信息
- 项目路径: ./datasets/threadpool_leak_samples_v1/test
- 扫描时间: 2026-03-15T18:16:36.992+08:00
- 扫描模式: full
- LLM 提供方: DashScope
- LLM 模型: glm-5

## 扫描摘要
- 扫描文件数: 3
- 问题总数: 7
- critical: 4
- high: 0
- medium: 3
- low: 0

## 问题明细（按严重程度排序）
### critical
#### [PG003] 线程池关闭不完整
- 风险分值: 95
- 位置: ./datasets/threadpool_leak_samples_v1/test/S001_MethodLocalCreateNoClose.java:8 (handleRequest)
- 原因: 在方法内部创建了线程池变量 'executor' 但未关闭。线程池是重量级资源，方法结束后不会自动回收，会导致线程资源耗尽（OOM）或线程泄露。
- 建议: 确保线程池在不再使用时被正确关闭。建议使用 try-with-resources 语法（如果线程池实现了AutoCloseable，如ThreadPoolExecutor）或在 finally 代码块中显式调用 shutdown() 方法。

#### [PG003] 线程池关闭不完整
- 风险分值: 95
- 位置: ./datasets/threadpool_leak_samples_v1/test/S002_PerCallFactoryNoClose.java:13 (createExecutor)
- 原因: 代码在方法 'createExecutor' 中创建了线程池（基于类名 'S002_PerCallFactoryNoClose' 和子类型 'A_NOT_CLOSED' 推断），但未将其关闭或返回给调用者管理。这会导致每次调用该方法时都创建一个新的线程池且无法回收，造成严重的线程和内存资源泄露。
- 建议: 1. 如果该方法用于创建线程池，建议将返回值类型修改为 ExecutorService，由调用方在使用完毕后显式调用 shutdown() 或 shutdownNow()。 2. 如果该方法内部使用完线程池，必须在 finally 代码块中确保线程池被关闭。 3. 建议检查是否应该使用共享的全局线程池，而非每次新建。

#### [PG002] 循环/递归中重复创建线程池
- 风险分值: 92
- 位置: ./datasets/threadpool_leak_samples_v1/test/S003_CreateInLoop.java:9 (batchProcess)
- 原因: 在循环上下文中创建线程池对象（变量 'executor'）。如果该线程池未在循环内部关闭，将导致严重的线程泄漏和资源耗尽；如果在循环内频繁创建并关闭，则会丧失线程池复用的性能优势并增加系统开销。
- 建议: 将线程池的初始化移至循环外部（作为类成员变量或方法局部变量），确保全局复用同一个线程池实例。如果必须在循环内创建，请确保在 try-finally 块中正确调用 shutdown() 方法进行关闭。

#### [PG003] 线程池关闭不完整
- 风险分值: 95
- 位置: ./datasets/threadpool_leak_samples_v1/test/S003_CreateInLoop.java:9 (batchProcess)
- 原因: 在 'batchProcess' 方法的循环中创建线程池（变量 'executor'），且子类型 'A_NOT_CLOSED' 表明线程池未被正确关闭。这会导致严重的线程泄漏和资源耗尽，随着循环次数增加，将创建大量非守护线程，最终导致系统内存溢出或线程资源耗尽。
- 建议: 1. 将线程池的创建移至循环外部，作为类成员变量或通过依赖注入管理，确保全局共享一个实例。
2. 如果必须在方法内部创建，务必在 try-catch-finally 块中调用 executor.shutdown() 或 shutdownNow() 确保资源释放。
3. 建议使用 ThreadPoolExecutor 自定义线程池并设置合理的队列大小与拒绝策略，避免无界队列导致的内存问题。

### medium
#### [PG006] 默认线程工厂不可观测
- 风险分值: 60
- 位置: ./datasets/threadpool_leak_samples_v1/test/S001_MethodLocalCreateNoClose.java:8 (handleRequest)
- 原因: 线程池实例 'executor' 在方法 'handleRequest' 内部创建且未显式关闭。该类名 'S001_MethodLocalCreateNoClose' 指示了典型的线程池泄露模式。作为局部变量，线程池的生命周期未受管理，方法调用结束后线程池引用丢失但工作线程可能仍在运行，导致线程资源泄露（Thread Leak）和潜在的内存泄露。
- 建议: 1. 提升作用域：将线程池声明为类的成员变量（static或instance），应用生命周期通常为单例，避免频繁创建销毁。
2. 注册关闭钩子：如果必须局部创建，需使用 try-finally 块包裹，并在 finally 中调用 executor.shutdown() 或 executor.shutdownNow() 确保资源释放。
3. 替换方案：如果是为了执行异步任务，建议使用共享的全局线程池，而非每次请求新建一个。

#### [PG006] 默认线程工厂不可观测
- 风险分值: 60
- 位置: ./datasets/threadpool_leak_samples_v1/test/S002_PerCallFactoryNoClose.java:13 (createExecutor)
- 原因: 方法 'createExecutor' 在第13行创建线程池时使用了默认线程工厂，且根据类名 'S002_PerCallFactoryNoClose' 推断，该线程池实例可能是方法内部局部变量或由工厂每次调用新建。若该线程池未显式关闭（shutdown）或未被上层持有以管理生命周期，将导致线程资源无法释放，造成线程泄漏。
- 建议: 1. 确保线程池实例在不再使用时调用 shutdown() 或 shutdownNow() 方法关闭。
2. 如果该线程池用于执行短期任务，建议将其声明为静态成员变量或单例，避免频繁创建销毁。
3. 如果必须每次创建，请使用 try-finally 块确保资源关闭，或使用 ExecutorService 的包装类实现 AutoCloseable 接口。

#### [PG006] 默认线程工厂不可观测
- 风险分值: 60
- 位置: ./datasets/threadpool_leak_samples_v1/test/S003_CreateInLoop.java:9 (batchProcess)
- 原因: 在循环结构（batchProcess方法第9行）中创建线程池（executor），且未显式设置thread_factory，使用了默认非守护线程工厂。若循环次数较多，将导致大量线程池对象被创建且无法被垃圾回收（GC），每个线程池都会维持存活线程，极易耗尽系统资源或导致OOM。
- 建议: 将线程池的初始化移至循环外部（作为类成员变量或方法局部变量），确保整个批处理过程复用同一个线程池。同时，建议自定义ThreadFactory将线程设置为守护线程（Daemon），并在服务关闭时调用shutdown()方法优雅关闭线程池。

## 修复建议汇总
1. 统一通过 try/finally 或 @PreDestroy 补齐线程池关闭路径。
2. 为线程池配置可观测线程名（自定义 ThreadFactory）。
