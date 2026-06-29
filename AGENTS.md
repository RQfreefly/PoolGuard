# Repository Guidelines

## 项目结构与模块组织
- `src/main/java/cn/rqfreefly`：主业务代码，当前入口为 `Main.java`。
- `src/main/resources`：运行时资源与后续配置文件。
- `src/test/java`：测试代码目录，包结构应与 `src/main/java` 对齐。
- `docs/需求文档`、`docs/开发文档`：需求说明与开发文档。
- `pom.xml`：Maven 构建、依赖与编译参数配置。

## 构建、测试与开发命令
- `mvn clean compile`：清理并编译 Java 8 源码。
- `mvn test`：运行全部测试用例。
- `mvn clean package`：打包产物到 `target/`。
- `java -cp target/classes cn.rqfreefly.Main`：本地运行当前入口类。
- `mvn -q clean verify`：提交前推荐执行的完整校验。

## 代码风格与命名规范
- 当前编译目标为 Java 8（`maven.compiler.source/target=8`），新代码需保持兼容。
- 使用 4 空格缩进，文件编码为 UTF-8，保证注释的覆盖率在 10%。
- 包名小写（如 `cn.rqfreefly.*`），类名使用 PascalCase，方法/字段使用 camelCase，常量使用 UPPER_SNAKE_CASE。
- 单一职责优先：避免将复杂逻辑堆在 `Main`，应拆分为独立类。
- 仅在逻辑不直观处添加简短注释，避免冗余描述。
- 遵循 《Code Complete 2》和《阿里巴巴 Java 代码开发规范》的最佳实践。

## 测试规范
- 建议使用 JUnit5，测试类命名以 `Test` 结尾，使用 Given / When / Then（BDD 风格）写测试
- 在每个测试函数中使用注释描述用例，使用 @DisplayName 注解给测试（类，方法）写说明，保证测试覆盖率大于 70%。
- 每次行为变更需覆盖正常路径、边界条件与异常路径。

## 提交与合并请求规范
- 当前仓库暂无历史提交，建议从现在开始采用 Conventional Commits。
- 示例：`feat: 新增阈值校验`、`fix: 修复空传感器数据处理`。
- 每次提交保持单一目标、可独立审阅。
- PR 至少包含：变更说明、影响范围、测试证据（如 `mvn test` 输出）、关联任务或 Issue。
- 仅在涉及可视化或运行结果变化时附截图或日志。
