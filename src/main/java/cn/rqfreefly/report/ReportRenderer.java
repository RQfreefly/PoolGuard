package cn.rqfreefly.report;

import cn.rqfreefly.core.ScanResult;

/**
 * 报告渲染器接口。
 */
public interface ReportRenderer {
    /**
     * 将扫描结果渲染为目标格式文本。
     *
     * @param result 扫描结果
     * @return 文本化报告
     */
    String render(ScanResult result);
}
