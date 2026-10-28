# man牛微信小程序 MVP

这是与 Android 版并行维护的原生微信小程序工程。目前包含：首页、饮食记录、体重记录、本地持久化，以及 Android `schemaVersion: 1` JSON 备份的导入与导出。

## 在微信开发者工具中运行

1. 安装并打开微信开发者工具。
2. 选择“导入项目”，目录选择仓库根目录 `Manniu`。
3. 首次体验可使用测试号；正式开发时，把根目录 `project.config.json` 的 `appid` 替换为实际小程序 AppID。
4. 工具会根据 `miniprogramRoot` 加载 `miniprogram/`。

## 数据说明

- 当前数据存储在 `wx.setStorageSync`，尚未接入云数据库。
- “恢复数据”通过微信聊天文件选择 Android 导出的 `man牛健康数据.json`。
- 体重按 `time|kg`、饮食按 `time|name|kcal` 去重合并。
- 正式上线前还需增加隐私保护指引、数据删除入口、云同步和服务类目配置。
