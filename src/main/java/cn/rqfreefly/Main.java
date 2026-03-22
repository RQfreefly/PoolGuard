package cn.rqfreefly;

import cn.rqfreefly.cli.PoolGuardCommand;
import picocli.CommandLine;
import picocli.CommandLine.ParameterException;

/**
 * CLI 主入口，统一处理退出码。
 */
public final class Main {

    private Main() {
    }

    /**
     * CLI 程序入口。
     * 在入口层统一设置参数错误处理器，是为了让所有子命令保持一致的退出码语义。
     *
     * @param args 命令行参数
     */
    public static void main(String[] args) {
        // 1) 初始化顶层命令对象，后续所有子命令都从这里分发。
        CommandLine commandLine = new CommandLine(new PoolGuardCommand());
        // 2) 允许枚举参数大小写不敏感，降低命令行使用门槛。
        commandLine.setCaseInsensitiveEnumValuesAllowed(true);
        // 3) 统一参数错误处理逻辑，确保错误退出码一致。
        commandLine.setParameterExceptionHandler(new ParameterErrorHandler());
        // 4) 执行命令并拿到退出码。
        int exitCode = commandLine.execute(args);
        // 5) 显式退出，便于 CI/脚本准确感知执行结果。
        System.exit(exitCode);
    }

    private static final class ParameterErrorHandler implements CommandLine.IParameterExceptionHandler {
        /**
         * {@inheritDoc}
         */
        @Override
        public int handleParseException(ParameterException ex, String[] args) {
            // 1) 将具体错误原因输出到标准错误，便于用户第一时间定位。
            ex.getCommandLine().getErr().println(ex.getMessage());
            // 2) 紧跟帮助信息，告诉用户正确参数格式。
            ex.getCommandLine().usage(ex.getCommandLine().getErr());
            // 3) 返回约定的参数错误退出码。
            return 2;
        }
    }
}
