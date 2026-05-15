# BabyLog Android Native

BabyLog Android 原生 MVP，优先验证小米真机上的 APK 方向。当前实现刻意只使用 Android SDK 和 Java，避免引入 Compose、AppCompat、CameraX 等额外依赖，方便在网络不稳定时离线构建。

## 当前能力

- 单胎本机模式首页。
- B 超记录表单：检查日期、孕周、BPD、HC、AC、FL、EFW、备注。
- 调用系统相机拍 B 超单。
- 使用 `SharedPreferences` 保存本机 JSON 记录。
- 照片保存到 App 私有目录，首页可显示记录摘要和图片。

## 暂未实现

- 服务端同步。
- OCR 识别 B 超单。
- FGR / 成长曲线计算。
- 账号登录和家庭共享。
- 原生语音识别入口。当前可先使用系统键盘自带语音输入。

## 构建

要求：

- JDK 17
- Android SDK，当前验证使用 `compileSdk 36`

离线构建命令：

```powershell
.\gradlew.bat "-Pandroid.aapt2FromMavenOverride=$env:LOCALAPPDATA\Android\Sdk\build-tools\36.0.0\aapt2.exe" clean :app:assembleDebug --offline --console=plain
```

APK 输出：

```text
android-native/app/build/outputs/apk/debug/app-debug.apk
```

## 本地文件

`local.properties`、`.gradle/`、`build/`、`app/build/` 都不提交。服务器 IP、SDK 绝对路径、调试产物不要进入 Git。
