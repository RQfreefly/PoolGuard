package cn.rqfreefly.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;

/**
 * 模块: cli
 * 功能: ScanCommand 参数校验与输出行为测试。
 */
@DisplayName("Scan 命令测试")
class ScanCommandTest {

    @Test
    @DisplayName("should_返回参数错误码2_when_扫描路径不存在")
    void should_returnParameterErrorCode2_when_scanPathNotExists() {
        // 模块: cli / 功能: 非法 path 参数返回退出码 2
        // Given: 不存在的扫描路径。
        PoolGuardCommand command = new PoolGuardCommand();
        CommandLine commandLine = new CommandLine(command);

        // When: 执行 scan 子命令。
        int exitCode = commandLine.execute("scan", "--path", "not_exists_path", "--format", "json");

        // Then: 返回参数错误退出码 2。
        assertEquals(2, exitCode);
    }

    @Test
    @DisplayName("should_输出帮助并包含scan子命令_when_执行根命令help")
    void should_printHelpContainingScanCommand_when_executeRootHelp() {
        // 模块: cli / 功能: 顶层 help 输出包含 scan 子命令
        // Given: 顶层命令对象与输出捕获流。
        PoolGuardCommand command = new PoolGuardCommand();
        CommandLine commandLine = new CommandLine(command);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        commandLine.setOut(new PrintWriter(out, true));

        // When: 执行 --help。
        int exitCode = commandLine.execute("--help");

        // Then: 成功退出且输出包含 scan 子命令。
        assertEquals(0, exitCode);
        String content = new String(out.toByteArray(), StandardCharsets.UTF_8);
        assertTrue(content.contains("scan"));
    }

    @Test
    @DisplayName("should_写入报告文件_when_指定output参数")
    void should_writeReportToOutputFile_when_outputArgumentProvided() throws Exception {
        // 模块: cli / 功能: --output 参数将报告写入文件
        // Given: 可扫描文件和输出路径。
        Path output = Files.createTempFile("poolguard-report", ".md");
        PoolGuardCommand command = new PoolGuardCommand();
        CommandLine commandLine = new CommandLine(command);

        // When: 以 md 格式输出到文件。
        int exitCode = commandLine.execute(
                "scan",
                "--path", "datasets/threadpool_leak_samples_v1/java/S021_ScheduledAtFixedRateNoStop.java",
                "--format", "md",
                "--output", output.toString()
        );

        // Then: 执行成功且文件存在报告内容。
        assertEquals(0, exitCode);
        String content = new String(Files.readAllBytes(output), StandardCharsets.UTF_8);
        assertTrue(content.contains("PoolGuard 扫描报告"));
    }

    @Test
    @DisplayName("should_返回参数错误码2_when_changedFiles路径不存在")
    void should_returnParameterErrorCode2_when_changedFilesPathNotExists() {
        // 模块: cli / 功能: --changed-files 不存在时返回参数错误
        // Given: 不存在的 changed-files 参数。
        PoolGuardCommand command = new PoolGuardCommand();
        CommandLine commandLine = new CommandLine(command);

        // When: 执行带无效 changed-files 的扫描。
        int exitCode = commandLine.execute(
                "scan",
                "--path", "datasets/threadpool_leak_samples_v1/java",
                "--changed-files", "not_exists_changed_files.txt"
        );

        // Then: 返回参数错误。
        assertEquals(2, exitCode);
    }
}
