# BabyLog

家庭自用的孕育记录 App，覆盖怀孕全阶段到宝宝 3 岁。本地优先，单胎单宝宝，医疗内容只做记录、趋势与参考提示，不做诊断。

## 当前状态

| 项 | 内容 |
|---|---|
| 主线 | **Android 原生 APK**（`android-native/`） |
| UI 技术 | **Jetpack Compose**（单一 UI；早期 Java 手写 View 已废弃删除） |
| 逻辑层 | Java（Domain / Service / Repository / Formatters / FileProvider / ImageUtils），Compose 复用 |
| 数据 | 本机 `SharedPreferences` JSON（后续视情况迁 Room） |
| 同步 | VPS 已就绪；按 `familyId` 的 push/pull 在重构后续阶段做，未配置时纯本机可用 |
| 后端 | PocketBase（已定） |
| 范围 | 单胎/单宝宝；多胎仅数据模型 1:N 预留，UI 不实现 |

阶段二（PWA / iOS）暂搁置，详见下方"参考与历史"。

## 仓库结构

### 主线（在用）

| 路径 | 说明 |
|---|---|
| `android-native/` | Android 原生 APK 工程；`ComposeMainActivity.kt` 为唯一 Activity |
| `android-native/app/src/main/java/app/babylog/nativeapp/` | Compose UI + Java 逻辑层（Domain / Service / Repository / Formatters / FileProvider / ImageUtils） |
| `android-native/smoke-tests/` | 离线可跑的纯 JVM smoke test（Domain / Service / Formatters / ImageUtils / SmartInput / SmartVision） |
| `android-native/README.md` | 构建、签名、离线缓存说明 |
| `.github/workflows/android-native.yml` | CI：JVM smoke test + `assembleDebug` + 条件 signed release |

### 产品与设计文档

| 路径 | 说明 |
|---|---|
| `项目立项书.md` | 产品定位、范围、数据模型、里程碑、非目标 |
| `需求评估表.md` | 需求优先级、MVP 裁剪、风险 |
| `UI设计书.md` | UI 方向、design tokens、组件、屏幕规格 |
| `docs/BabyLog阶段主线重构计划.md` | 阶段投影（孕期 / 出生后）重构计划与验收标准 |
| `docs/BabyLog重构前逻辑核查.md` | 重构前锁定的逻辑缺陷与门禁（L-1~L-13） |
| `docs/Piyo对标差异与BabyLog产品方向.md` | 对标 Piyo日志的产品方向分析 |

### 参考与历史（不在主线迭代）

| 路径 | 说明 |
|---|---|
| `app/` | Vite + React + TS 的 PWA 实现；阶段二再评估，当前仅作数据模型 / 字段定义 / 业务逻辑的参考，UI 部分已被 Android 主线取代 |
| `prototype/` | 早期静态 HTML 原型，设计已演进，仅留作设计史 |
| `deploy/`、`Vultr部署说明.md`、`腾讯云部署说明.md`、`iOS部署说明.md` | **PWA / 阶段二** 的服务器部署资料，与当前 Android 主线无关 |

> 仓库内不写入服务器 IP、域名、SSH 用户、Android keystore 等敏感参数；部署时通过脚本参数或 CI Secrets 注入。

## 构建 Android（主线）

要求：JDK 17、Android SDK（`compileSdk 36`）。首次构建需联网，让 Gradle 缓存 Compose / Kotlin / AndroidX 依赖；之后可离线构建。

```powershell
cd android-native
.\gradlew.bat "-Pandroid.aapt2FromMavenOverride=$env:LOCALAPPDATA\Android\Sdk\build-tools\36.0.0\aapt2.exe" :app:assembleDebug --console=plain
```

APK 输出：`android-native/app/build/outputs/apk/debug/app-debug.apk`。

Release 签名走本机 `android-native/signing.properties` 或 CI Secrets 注入；CI 配置 `BABYLOG_RELEASE_KEYSTORE_BASE64` 等 Secret 后构建 signed release。详见 `android-native/README.md`。

## 非目标

- 不做公开 SaaS / 多家庭运营 / 商业化（自家 + 至多月嫂的小范围使用）
- 不做第三方账户（Google / Apple / 微信）；同步只做家庭密钥校验
- 不做医疗诊断；曲线和范围只作记录与参考提示
- 阶段一不做医疗诊断、不做完整云同步、不迁 Room；B 超单识别仅走用户自带多模态模型 Key，并必须人工确认后入库
