package cn.rqfreefly.rules;

import cn.rqfreefly.core.RuleId;
import cn.rqfreefly.core.Severity;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

/**
 * 固定规则元数据注册表。
 */
public final class RuleMetadataRegistry {

    private final Map<RuleId, RuleMetadata> metadataMap;

    /**
     * 初始化固定规则元数据。
     * 固定注册表比外部动态加载更可控，便于在首期快速迭代规则语义而不引入配置漂移。
     */
    public RuleMetadataRegistry() {
        // 模块 1：初始化可变枚举映射，按规则逐条注册元数据。
        EnumMap<RuleId, RuleMetadata> map = new EnumMap<RuleId, RuleMetadata>(RuleId.class);
        map.put(RuleId.PG001, new RuleMetadata(
                RuleId.PG001,
                Severity.HIGH,
                "高频入口中创建线程池",
                "在控制器或高频请求入口方法内部创建线程池，可能导致资源膨胀。",
                80));
        map.put(RuleId.PG002, new RuleMetadata(
                RuleId.PG002,
                Severity.CRITICAL,
                "循环/递归中重复创建线程池",
                "在线程池创建点位于循环、重试或递归上下文时，可能持续扩容线程与队列。",
                92));
        map.put(RuleId.PG003, new RuleMetadata(
                RuleId.PG003,
                Severity.CRITICAL,
                "线程池关闭不完整",
                "线程池未关闭或关闭路径不完整，存在泄露风险。",
                95));
        map.put(RuleId.PG005, new RuleMetadata(
                RuleId.PG005,
                Severity.HIGH,
                "生命周期不匹配",
                "短生命周期对象持有线程池且缺少明确销毁钩子。",
                78));
        map.put(RuleId.PG006, new RuleMetadata(
                RuleId.PG006,
                Severity.MEDIUM,
                "默认线程工厂不可观测",
                "线程名称不可观测，不利于排障与监控。",
                60));
        map.put(RuleId.PG007, new RuleMetadata(
                RuleId.PG007,
                Severity.HIGH,
                "无界队列资源风险",
                "线程池使用无界队列，可能导致任务积压与内存风险。",
                84));
        map.put(RuleId.PG008, new RuleMetadata(
                RuleId.PG008,
                Severity.HIGH,
                "定时线程池未回收",
                "定时任务未取消且线程池缺少关闭策略，可能导致长期泄露。",
                80));
        map.put(RuleId.PG009, new RuleMetadata(
                RuleId.PG009,
                Severity.HIGH,
                "静态线程池无退出钩子",
                "静态线程池缺少 @PreDestroy/close/shutdownHook 等退出逻辑。",
                83));

        // 模块 2：转换为只读映射，防止运行时被意外修改。
        this.metadataMap = Collections.unmodifiableMap(map);
    }

    /**
     * 按规则 ID 返回元数据。
     *
     * @param ruleId 规则 ID
     * @return 规则元数据
     */
    public RuleMetadata get(RuleId ruleId) {
        // 模块：按规则 ID 查找对应元数据。
        return metadataMap.get(ruleId);
    }
}
