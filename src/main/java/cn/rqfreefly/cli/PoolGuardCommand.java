package cn.rqfreefly.cli;

import picocli.CommandLine.Command;

/**
 * 顶层命令定义。
 */
@Command(
        name = "poolguard",
        mixinStandardHelpOptions = true,
        version = "poolguard 0.1.0",
        description = "Java线程池泄露扫描工具",
        subcommands = {ScanCommand.class}
)
public final class PoolGuardCommand implements Runnable {

    /**
     * {@inheritDoc}
     */
    @Override
    public void run() {
        // 顶层命令本身不执行业务逻辑：
        // 用户只输入 `poolguard` 时，picocli 会自动展示帮助信息。
    }
}
