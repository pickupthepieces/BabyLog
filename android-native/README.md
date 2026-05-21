# BabyLog Android Native

BabyLog Android 原生 MVP，优先验证小米真机上的 APK 方向。当前 UI 使用 Jetpack Compose；业务逻辑、仓库、格式化和附件处理继续复用 Java 逻辑层。

## 当前能力

- 单胎本机模式首页：首登建档、孕期卡 / 出生后日视图、最近记录、趋势卡。
- 当前阶段锁定竖屏使用，不适配横屏，优先保证孕期主线、表单和时间线在手机竖屏下稳定。
- 底部四页：`首页 / 时间线 / 资料 / 设置`，中间麦克风为全局语音 / 智能录入入口；首页底部常驻轻量 quick rail 承担手动快捷记录。
- 孕期快捷记录：B 超、产检、NT、唐筛、无创 DNA、大排畸、OGTT、GBS、NST、胎动、宫缩、孕妈指标；出生后保留喂养、睡眠、尿布、体温、用药等育儿快捷入口。
- 产检结构化记录：常规层（血压、体重、宫高、腹围、胎心率、尿常规、医生结论、下次日期、附件）+ 7 类专项筛查（NT、唐筛、无创 DNA、大排畸、OGTT、GBS、NST），App 只记录报告原文，不计算风险、不判读。
- B 超记录表单：检查日期、孕周、BPD、HC、AC、FL、EFW，以及胎心率、羊水、胎盘、胎位、宫颈管、CRL/NT、脐动脉血流等公共信息。
- 调用系统相机拍 B 超单，使用 `content://` 临时文件接收全尺寸照片，压缩后保存到 App 私有目录。
- 从系统相册/文件选择图片，压缩后保存到 App 私有目录。
- 用户可本机配置 OpenAI-compatible 多模态模型 API，用于主动识别 B 超单和文本结构化候选；确认前不入库。设置页提供 Qwen / OpenAI 预设，API Key 仅本机加密保存，不进备份和同步。
- 语音转文字 STT 独立配置，首版使用 DashScope Paraformer；API Key 与 OCR / 智能解析可以相同，也可以单独填写。
- 全局智能录入已收敛为单一入口：支持手动输入，或在同一入口内按住录音，经云端 Paraformer STT 转成可编辑文本后，再由大模型判类型并打开对应表单预填，确认前不入库。
- B 超单识别 v1 自动候选检查日期、BPD/HC/AC/FL/明确 EFW，以及报告明确写出的胎心率、羊水、胎盘、胎位、胎儿个数、胎动、脐带插入处、宫颈管、CRL/NT、脐动脉血流；孕周不由模型识别或推断，侧脑室/后角宽、鼻骨、四腔心、肢体可及等结构筛查文字仅保留原文或警告供人工核对。
- B 超单识别上传前复用本机图片压缩策略（长边 2048px / JPEG 82），避免原图 base64 直接发送。
- 本机事件仓库：`events / attachments / syncChanges / syncSettings`。
- 同步占位：本机 pending 队列 + 家庭同步协议骨架；真实 PocketBase 推拉仍未接入，后端未配置时标记为可重试失败。
- 资料库：B 超单、检查单、出生证明、疫苗本入口。
- 备份入口：导出/导入 BabyLog JSON，含本机附件图片 base64。
- 胎儿成长曲线：按自有 B 超数据绘制趋势，并提供香港近似参考百分位 / Z-score；当前为未校准近似实现，仅供记录和复诊沟通参考。

## 暂未实现

- 服务端同步真实推送 / 拉取 / 附件跨设备同步。
- 正式可追溯香港 / CUHK FGR 数据源替换；当前仅为香港近似参考。
- 账号登录和家庭共享。
- OpenAI Transcribe 备选通道。

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
