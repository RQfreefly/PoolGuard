package cn.rqfreefly.parser;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Paths;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 模块: parser
 * 功能: JavaSourceParser 事实提取测试。
 */
@DisplayName("Java 源码解析器测试")
class JavaSourceParserTest {

    private final JavaSourceParser parser = new JavaSourceParser();

    @Test
    @DisplayName("should_提取创建点和关闭点_when_解析安全样本")
    void should_extractCreationAndShutdownFacts_when_parseSafeSample() {
        // 模块: parser / 功能: 提取线程池创建点与关闭点
        // Given: 带 finally shutdown 的样本文件。
        String sample = "datasets/threadpool_leak_samples_v1/java/S007_DefaultFactoryButProperShutdown.java";

        // When: 解析样本 AST 并提取事实。
        JavaFileAnalysis analysis = parser.parse(Paths.get(sample));

        // Then: 存在创建点和关闭点，且解析成功。
        assertTrue(analysis.isParseSuccess());
        assertFalse(analysis.getCreations().isEmpty());
        assertFalse(analysis.getShutdownCalls().isEmpty());
    }

    @Test
    @DisplayName("should_识别PreDestroy和Hook字段_when_解析生命周期样本")
    void should_detectPreDestroyAndHookFields_when_parseLifecycleSamples() {
        // 模块: parser / 功能: 提取 @PreDestroy 与 shutdown hook 生命周期信号
        // Given: 使用 @PreDestroy 和 shutdown hook 管理生命周期的样本。
        String preDestroy = "datasets/threadpool_leak_samples_v1/java/S105_PreDestroyShutdown.java";
        String hook = "datasets/threadpool_leak_samples_v1/java/S121_StaticExecutorShutdownHook.java";

        // When: 解析两类样本。
        JavaFileAnalysis preDestroyAnalysis = parser.parse(Paths.get(preDestroy));
        JavaFileAnalysis hookAnalysis = parser.parse(Paths.get(hook));

        // Then: 能识别预销毁和 hook 变量。
        assertTrue(preDestroyAnalysis.isParseSuccess());
        assertTrue(preDestroyAnalysis.getPreDestroyShutdownFields().contains("executor"));
        assertTrue(hookAnalysis.getShutdownHookFields().contains("EXECUTOR"));
    }
}
