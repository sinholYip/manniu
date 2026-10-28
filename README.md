# man牛

[![Android CI](https://github.com/sinholYip/manniu/actions/workflows/android-ci.yml/badge.svg)](https://github.com/sinholYip/manniu/actions/workflows/android-ci.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)
[![minSdk 28](https://img.shields.io/badge/minSdk-28-3DDC84.svg)](https://developer.android.com/about/versions/pie)
[![Kotlin 2.0](https://img.shields.io/badge/Kotlin-2.0.21-7F52FF.svg)](https://kotlinlang.org)

以 **K 线形态**记录体重、用**热量"成交量"**观察摄入的深色饮食与训练日志应用。

体重每天一笔，取当日首笔、末笔、最高、最低画出 OHLC 蜡烛图；下方对应日期的热量柱状图
就是它的成交量。红涨绿跌的配色沿用 A 股习惯 —— 增重记红，减重记绿。
界面全深色，主色为荧光绿，无网络也能完整使用。

界面设计稿与交互清单见 [`design/`](design/) 目录。

## 功能

**体重**

- 按日聚合为 OHLC K 线，支持 14 天 / 30 天 / 60 天 / 半年 / 至今切换
- 五日移动平均线（MA5），与 K 线同色系
- 点击或长按滑动查看当天收盘体重与摄入热量
- 每天仅一笔记录时，以上一日收盘作为开盘价

**饮食与热量**

- 早、午、练前、晚、宵夜五餐分别记录，热量按碳水×4 + 蛋白质×4 + 脂肪×9 自动计算
- 训练日 / 休息日两套营养目标，五餐比例可分别配置
- 每 100g 自定义食物预设，按实际克数自动换算
- 搜索 Open Food Facts，内置常见中式食物离线参考值
- 首页版块长按可拖动换位，顺序随备份一起迁移

**训练**

- 按具体日期编排训练或休息，支持 1–12 周周期与减载周
- 每天配置动作、组数、次数、重量，并按训练部位显示对应背景图
- 动作个人记录（PR）与部位训练量统计
- 训练计划直接驱动当天的营养目标，无需重复维护

**数据**

- 一键导出完整历史为 JSON，换机或重装后恢复并合并
- 恢复前完整校验，损坏文件不写入任何数据；重复恢复不会重复计入
- 与微信小程序版共用同一份备份格式

## 快速开始

```bash
git clone https://github.com/sinholYip/manniu.git
cd manniu
./gradlew :app:assembleDebug
```

环境要求（前两条都容易踩，请务必确认）：

| 项 | 要求 |
| --- | --- |
| **工程路径** | **必须纯英文**。路径含中文会让单元测试全部报 `ClassNotFoundException`，而编译仍然正常 |
| Gradle JDK | **21**。Gradle 8.11.1 与 AGP 8.9.1 不支持 JDK 25，Android Studio 默认 JBR 会直接构建失败 |
| Android SDK | 36（`compileSdk` / `targetSdk`），设备需 Android 9 / API 28 以上 |

设置 JDK：Android Studio → Settings → Build, Execution, Deployment → Build Tools → Gradle → Gradle JDK。

> 不要把工程放在 OneDrive、坚果云等同步盘目录里，会与 Gradle 争抢文件锁，
> 也会让两端修改互相覆盖。

## 架构

单模块 Compose 应用，源码按职责分为四层，依赖只能自上而下：

```
com.ranke.health
├── MainActivity.kt      应用入口与页面路由
├── model/               数据模型、枚举、派生计算
├── core/                无副作用的热量与宏量换算
├── data/                Room、本地存储、备份、食物数据源
└── ui/
    ├── theme/           颜色、字号、形状等设计令牌
    ├── components/      跨页面复用的弹窗与表单控件
    └── screen/          首页、训练、我的
```

依赖方向 `ui → data → core → model`，`model` 与 `core` 不依赖 Android 框架，
因此可被纯 JVM 单元测试直接覆盖。分层依据、数据流、备份契约，以及有意不引入的抽象
（ViewModel / 依赖注入 / 多模块）都写在 [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md)。

```bash
./gradlew :app:compileDebugKotlin   # 编译
./gradlew :app:testDebugUnitTest    # 单元测试
./gradlew :app:lintDebug            # 静态检查
```

## 安装包发布

APK 体积大，**不进入 Git 仓库**，通过 GitHub Release 分发。

```powershell
.\gradlew.bat assembleRelease   # R8 混淆 + 资源裁剪
.\package-apk.ps1               # 复制为 bao/man-niu-v<versionName>.apk
.\publish-release.ps1           # 以 v<versionName> 建 Release 并上传
```

脚本的 GitHub 凭据优先读环境变量 `GITHUB_TOKEN`，否则从 Git 凭据管理器获取
（需先用 GitHub Desktop 登录过）。三个脚本都可重复执行，同名资产会先删除再上传。

发布前记得在 `app/build.gradle.kts` 里同步递增 `versionCode` 与 `versionName`，
并在 [CHANGELOG.md](CHANGELOG.md) 补一条记录。

## 数据与隐私

应用**不联网上传任何数据**。唯一的网络请求是用户主动搜索食物时访问 Open Food Facts。
体重、饮食、训练与设置全部存放在应用私有目录，没有账号体系，不收集设备标识符，
未接入统计或广告 SDK。导出的备份 JSON 是明文，请自行保管。

## 多设备同步

仓库即同步源，换设备只需 `git clone`。

```bash
git pull --rebase     # 动手前先拉
# 开发……
git add -A && git commit -m "说明" && git push
```

每台设备各自维护、**不纳入仓库**的内容：`local.properties`（SDK 路径）、
`app/build`、`.gradle`、`bao/*.apk`。首次用 Android Studio 打开工程时会自动生成 `local.properties`。

## 参与贡献

欢迎提交 issue 与 PR。开发约定、分层归属、提交信息格式与自检清单见
[CONTRIBUTING.md](CONTRIBUTING.md)；安全问题的报告方式见 [SECURITY.md](SECURITY.md)。

## 许可证

本项目采用 [MIT 许可证](LICENSE)。

- **版权归作者所有**（Copyright © 2026 sinholYip），著作权不因开源而转移。
- 你可以自由使用、复制、修改、合并、发布、分发、再许可和销售本软件，**包括商业用途**，
  无需事先取得同意。
- 唯一义务：在副本或实质部分中保留版权声明与本许可声明。
- 本软件按"现状"提供，不含任何担保，作者不对使用后果承担责任。

> `design/` 下的界面概念图，以及 `app/src/main/res/` 中的图标与训练背景图属于个人视觉素材，
> 二次分发时建议替换为自己的素材或注明来源。
