# BabyLog Android Native

BabyLog Android 原生 MVP，优先验证小米真机上的 APK 方向。当前 UI 使用 Jetpack Compose；业务逻辑、仓库、格式化和附件处理继续复用 Java 逻辑层。

## 当前能力

- 单胎本机模式首页：首登建档、孕期卡 / 出生后日视图、最近记录、趋势卡。
- 底部四页：`首页 / 时间线 / 资料 / 设置`，中间 `+` 为快捷记录入口。
- 快捷记录：喂养、睡眠、尿布、体温、用药、B 超。
- B 超记录表单：检查日期、孕周、BPD、HC、AC、FL、EFW，以及羊水、胎盘、胎位、脐动脉血流。
- 调用系统相机拍 B 超单，使用 `content://` 临时文件接收全尺寸照片，压缩后保存到 App 私有目录。
- 从系统相册/文件选择图片，压缩后保存到 App 私有目录。
- 用户可本机配置 OpenAI-compatible 多模态模型 API，用于主动识别 B 超单并生成候选字段；确认前不入库。
- 本机事件仓库：`events / attachments / syncChanges / syncSettings`。
- 同步占位：本机 pending 队列，后端未配置时标记为可重试失败。
- 资料库：B 超单、检查单、出生证明、疫苗本入口。
- 备份入口：导出/导入 BabyLog JSON，含本机附件图片 base64。

## 暂未实现

- 服务端同步。
- FGR / 成长曲线计算。
- 账号登录和家庭共享。
- 原生语音识别入口。当前可先使用系统键盘自带语音输入。

## 构建

要求：

- JDK 17
- Android SDK，当前验证使用 `compileSdk 36`
- AndroidX、Jetpack Compose、Kotlin Gradle Plugin 依赖会由 Gradle 下载；不需要安装额外桌面环境。

首次构建需要联网执行一次，让 Gradle 预缓存 Compose / Kotlin / AndroidX 依赖：

```powershell
.\gradlew.bat "-Pandroid.aapt2FromMavenOverride=$env:LOCALAPPDATA\Android\Sdk\build-tools\36.0.0\aapt2.exe" :app:assembleDebug --console=plain
```

离线构建命令：

```powershell
.\gradlew.bat "-Pandroid.aapt2FromMavenOverride=$env:LOCALAPPDATA\Android\Sdk\build-tools\36.0.0\aapt2.exe" clean :app:assembleDebug --offline --console=plain
```

离线构建依赖本机已完成过一次联网构建；否则 Compose / Kotlin 依赖可能尚未在 Gradle cache 中。

APK 输出：

```text
android-native/app/build/outputs/apk/debug/app-debug.apk
```

Release APK 体积预算：阶段一控制在 8 MiB 内；当前 signed release 实测
`app/build/outputs/apk/release/app-release.apk` 为 4,178,567 B（3.98 MiB）。

## Release 签名

Release keystore 不提交仓库。本机打 signed release 时，在 `android-native/signing.properties`
写入以下键，或用同名环境变量提供：

```properties
BABYLOG_RELEASE_STORE_FILE=release.jks
BABYLOG_RELEASE_STORE_PASSWORD=***
BABYLOG_RELEASE_KEY_ALIAS=***
BABYLOG_RELEASE_KEY_PASSWORD=***
```

```powershell
.\gradlew.bat :app:assembleRelease --console=plain
```

CI 使用 `.github/workflows/android-native.yml`。配置
`BABYLOG_RELEASE_KEYSTORE_BASE64`、`BABYLOG_RELEASE_STORE_PASSWORD`、
`BABYLOG_RELEASE_KEY_ALIAS`、`BABYLOG_RELEASE_KEY_PASSWORD` 后会额外构建 signed release；
未配置时只跑 smoke tests 和 debug 构建。

## 本地文件

`local.properties`、`.gradle/`、`build/`、`app/build/` 都不提交。服务器 IP、SDK 绝对路径、调试产物不要进入 Git。
