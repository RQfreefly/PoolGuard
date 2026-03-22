package cn.rqfreefly.report;

import cn.rqfreefly.core.OutputFormat;

/**
 * 报告渲染器工厂。
 */
public final class ReportRendererFactory {

    private ReportRendererFactory() {
    }

    /**
     * 根据输出格式选择渲染器实现。
     *
     * @param format 输出格式
     * @return 渲染器
     */
    public static ReportRenderer create(OutputFormat format) {
        // 模块 1：按目标格式选择对应渲染器实现。
        if (OutputFormat.MD == format) {
            return new MdReportRenderer();
        }
        if (OutputFormat.TEXT == format) {
            return new TextReportRenderer();
        }
        // 模块 2：默认返回 JSON 渲染器。
        return new JsonReportRenderer();
    }
}
