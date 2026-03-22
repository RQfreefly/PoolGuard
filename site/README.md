# PoolGuard Web 页面（GitHub Pages）

此目录用于承载 PoolGuard 的 H5 宣传页。

## 本地预览

在项目根目录执行：

```bash
python3 -m http.server 8080 --directory site
```

打开 `http://localhost:8080`。

## 发布到 GitHub Pages

1. 推送代码到 GitHub 仓库。
2. 进入仓库 `Settings -> Pages`。
3. `Build and deployment` 选择：
- Source: `Deploy from a branch`
- Branch: `main`（或你的默认分支）
- Folder: `/site`
4. 保存后等待 Pages 构建完成。

发布地址通常为：

```text
https://<你的用户名>.github.io/<仓库名>/
```
