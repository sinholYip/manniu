# 贡献指南

感谢你愿意改进 man牛。这份文档说明本项目的开发约定，让改动更容易被理解和合并。

## 环境要求

| 项目 | 版本 | 说明 |
| --- | --- | --- |
| JDK | **21** | Gradle 8.11.1 与 AGP 8.9.1 不支持 JDK 25，用 Android Studio 默认 JBR 会直接构建失败 |
| Gradle | 8.11.1 | 由 wrapper 固定，不要手动升级 |
| Android SDK | 36 | `compileSdk` / `targetSdk` |
| 最低运行版本 | API 28（Android 9） | |

把 Gradle JDK 指向 21：Android Studio → Settings → Build, Execution, Deployment → Build Tools → Gradle → Gradle JDK。

## 本地构建

```bash
./gradlew :app:compileDebugKotlin        # 只编译，最快
./gradlew :app:testDebugUnitTest         # 单元测试
./gradlew :app:assembleRelease           # 混淆后的 release 包
./gradlew :app:lintDebug                 # 静态检查
```

首次构建需要联网下载依赖。**不要把工程放在 OneDrive、坚果云等同步盘目录**，会与 Gradle 争抢文件锁。

## 代码风格

- 遵循 [Kotlin 官方编码规范](https://kotlinlang.org/docs/coding-conventions.html)，缩进 4 空格，行宽 120 字符，由 `.editorconfig` 约束。
- **顶层声明默认 `internal`**（同模块可见）。只有真正需要跨模块暴露时才用 `public`。新增文件沿用这一约定。
- Compose 页面优先拆成小的 `@Composable` 私有或 `internal` 组件，单个函数尽量不超过 100 行。
- 不在 UI 层直接读写数据库：页面只调用 `data` 层暴露的方法。

## 分层归属

新增文件请放到对应层，不要堆在根包：

| 包 | 放什么 | 不该放什么 |
| --- | --- | --- |
| `com.ranke.health.model` | 纯数据模型、枚举、派生计算 | 依赖 Android 框架的代码 |
| `com.ranke.health.core` | 无副作用的计算逻辑（热量、宏量换算） | UI、存储 |
| `com.ranke.health.data` | Room 实体与 DAO、本地存储、备份、外部数据源 | Compose 组件 |
| `com.ranke.health.ui.theme` | 颜色、字号、形状等设计令牌 | 业务逻辑 |
| `com.ranke.health.ui.components` | 可复用的弹窗与表单控件 | 具体页面 |
| `com.ranke.health.ui.screen` | 完整页面 | 通用控件 |

依赖方向只能自上而下：`ui → data → core → model`。`model` 与 `core` 不得反向依赖 `ui`。

## 提交信息

采用 [Conventional Commits](https://www.conventionalcommits.org/zh-hans/)：

```
<类型>: <一句话说明>

- 具体改动
- 为什么这样改
```

常用类型：`feat`（新功能）、`fix`（修 bug）、`refactor`（重构）、`docs`（文档）、`test`（测试）、`build`（构建）、`chore`（杂项）。

**一个提交只做一件事。** 重构与功能改动不要混在同一个提交里，方便回溯。

## 提交前自检

- [ ] `./gradlew :app:testDebugUnitTest` 通过。
- [ ] 新增的逻辑有对应单元测试（计算类至少覆盖边界值）。
- [ ] 改动了页面或交互时，同步更新 `design/INTERACTION_MAP.md`。
- [ ] 有用户可见的变化时，在 `CHANGELOG.md` 的「未发布」段落补一条。
- [ ] 抬高了 `versionCode` / `versionName` 时，两者保持同步递增。

## 数据兼容性

备份文件格式（`schemaVersion: 1`）同时被 Android 版和微信小程序版读取。**修改备份结构必须递增 `schemaVersion` 并保留旧版本解析路径**，否则用户的历史数据会在降级安装时丢失。

Room 数据库改动请先在 `app/schemas/` 生成新的 schema 快照，并补充迁移测试。

## 提交 PR

1. 基于 `main` 开分支，命名如 `feat/workout-cycle`、`fix/backup-refresh`。
2. 描述里说明改了什么、为什么改、怎么验证。
3. 涉及界面的改动附截图或录屏。
4. CI 必须通过（编译 + 单元测试 + lint）。

## 报告问题

- 缺陷请用 issue 模板，附上机型、Android 版本、复现步骤。
- 安全相关问题**不要**公开开 issue，见 [SECURITY.md](SECURITY.md)。
