package cn.rqfreefly.analyzer;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import cn.rqfreefly.core.Issue;
import cn.rqfreefly.core.OutputFormat;
import cn.rqfreefly.core.RuleId;
import cn.rqfreefly.core.ScanConfig;
import cn.rqfreefly.core.ScanResult;
import cn.rqfreefly.core.SortBy;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 模块: analyzer
 * 功能: ScanService 端到端规则集成测试。
 */
@DisplayName("扫描服务集成测试")
class ScanServiceIntegrationTest {

    private final ScanService scanService = new ScanService();

    @Test
    @DisplayName("should_命中核心规则_when_扫描泄露样本集")
    void should_hitCoreRules_when_scanLeakSamples() throws Exception {
        // 模块: analyzer / 功能: 全量扫描命中核心规则（PG001/PG002/PG003/PG009）
        // Given: 包含多种泄露模式的数据集目录。
        ScanConfig config = new ScanConfig("datasets/threadpool_leak_samples_v1/java", OutputFormat.JSON, SortBy.SEVERITY,
                "glm-5", 3, false, null, null);

        // When: 执行全目录扫描。
        ScanResult result = scanService.scan(config);

        // Then: 至少命中 PG001/PG002/PG003/PG009 规则。
        assertTrue(hasRule(result.getIssues(), RuleId.PG001));
        assertTrue(hasRule(result.getIssues(), RuleId.PG002));
        assertTrue(hasRule(result.getIssues(), RuleId.PG003));
        assertTrue(hasRule(result.getIssues(), RuleId.PG009));
    }

    @Test
    @DisplayName("should_抑制PG003_when_生命周期已托管")
    void should_suppressPg003_when_lifecycleIsManaged() throws Exception {
        // 模块: analyzer / 功能: 生命周期托管场景抑制 PG003
        // Given: 仅扫描带 @PreDestroy 的安全样本。
        ScanConfig config = new ScanConfig("datasets/threadpool_leak_samples_v1/java/S105_PreDestroyShutdown.java",
                OutputFormat.JSON,
                SortBy.SEVERITY,
                "glm-5",
                3,
                false,
                null,
                null);

        // When: 执行单文件扫描。
        ScanResult result = scanService.scan(config);

        // Then: 不应出现 PG003 关闭缺失告警。
        assertFalse(hasRule(result.getIssues(), RuleId.PG003));
    }

    @Test
    @DisplayName("should_不误报PG006_when_使用自定义线程工厂")
    void should_notFlagPg006_when_useCustomThreadFactory() throws Exception {
        // 模块: analyzer / 功能: 自定义线程工厂不误报 PG006
        // Given: 使用自定义 ThreadFactory 并 finally 关闭的样本。
        ScanConfig config = new ScanConfig("datasets/threadpool_leak_samples_v1/java/S104_CustomThreadFactoryAndClose.java",
                OutputFormat.JSON,
                SortBy.SEVERITY,
                "glm-5",
                3,
                false,
                null,
                null);

        // When: 扫描该文件。
        ScanResult result = scanService.scan(config);

        // Then: 不应误报 PG006 默认线程工厂问题。
        assertFalse(hasRule(result.getIssues(), RuleId.PG006));
    }

    @Test
    @DisplayName("should_识别PG007和PG008_when_扫描对应风险样本")
    void should_detectPg007AndPg008_when_scanRiskSamples() throws Exception {
        // 模块: analyzer / 功能: 识别无界队列(PG007)与定时池未回收(PG008)
        // Given: 一个无界队列样本和一个定时线程池未关闭样本。
        ScanConfig queueConfig = new ScanConfig("datasets/threadpool_leak_samples_v1/java/S020_UnboundedQueueThreadPool.java",
                OutputFormat.JSON,
                SortBy.SEVERITY,
                "glm-5",
                3,
                false,
                null,
                null);
        ScanConfig scheduleConfig = new ScanConfig("datasets/threadpool_leak_samples_v1/java/S021_ScheduledAtFixedRateNoStop.java",
                OutputFormat.JSON,
                SortBy.SEVERITY,
                "glm-5",
                3,
                false,
                null,
                null);

        // When: 分别执行扫描。
        ScanResult queueResult = scanService.scan(queueConfig);
        ScanResult scheduleResult = scanService.scan(scheduleConfig);

        // Then: 分别命中 PG007 与 PG008。
        assertTrue(hasRule(queueResult.getIssues(), RuleId.PG007));
        assertTrue(hasRule(scheduleResult.getIssues(), RuleId.PG008));
    }

    private boolean hasRule(List<Issue> issues, RuleId ruleId) {
        for (Issue issue : issues) {
            if (ruleId == issue.getRuleId()) {
                return true;
            }
        }
        return false;
    }
}
