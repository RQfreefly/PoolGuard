package cn.rqfreefly.parser;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Java 文件发现器。
 */
public final class JavaFileDiscoverer {

    /**
     * 发现目标路径下可扫描的 Java 文件。
     * 返回稳定排序结果，是为了保证后续扫描输出顺序可复现。
     *
     * @param rootPath 目录或单文件路径
     * @return Java 文件路径列表
     * @throws IOException 文件遍历失败时抛出
     */
    public List<Path> discover(Path rootPath) throws IOException {
        // 模块 1：路径不存在时直接返回空列表。
        if (!Files.exists(rootPath)) {
            return Collections.emptyList();
        }

        // 模块 2：如果输入本身就是 Java 文件，直接返回单元素结果。
        if (Files.isRegularFile(rootPath) && rootPath.toString().endsWith(".java")) {
            List<Path> single = new ArrayList<Path>();
            single.add(rootPath);
            return single;
        }

        // 模块 3：递归遍历目录，筛选出 Java 文件。
        List<Path> files = new ArrayList<Path>();
        Files.walk(rootPath)
                .filter(Files::isRegularFile)
                .filter(path -> path.toString().endsWith(".java"))
                .forEach(files::add);

        // 模块 4：排序后返回，保证扫描顺序稳定。
        Collections.sort(files);
        return files;
    }
}
