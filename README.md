# BabyLog

> 家庭自用的孕育记录 App，覆盖怀孕全阶段到宝宝 3 岁。本地优先，单胎单宝宝，医疗内容只做记录、趋势与参考提示，不做诊断。

[![Android Native](https://github.com/pickupthepieces/BabyLog/actions/workflows/android-native.yml/badge.svg)](https://github.com/pickupthepieces/BabyLog/actions/workflows/android-native.yml)

BabyLog 给一个真实家庭（父母、祖辈，必要时月嫂）记录从备孕、孕期到宝宝 3 岁的连续过程。设计上优先离线可用、数据本机可控、隐私边界清晰，不追求公开商业化。

---

## 功能

**孕期（当前主线，已基本可用）**

- 孕期首页：孕周、距预产期天数、孕期摘要（最近产检 / 最近 B 超 / 待复核）
- 产检 / 胎动 / 宫缩快捷记录
- B 超指标录入（BPD / HC / AC / FL / EFW…）+ 软范围提示 + 拍照归档
- B 超单多模态识别：用户自带 OpenAI-compatible 多模态模型，识别仅生成候选字段，人工确认后入库
- 胎儿成长曲线：按孕周绘制自有 B 超趋势（不叠加参考曲线）
- 胎动会话计数器：开始 / 计数 / 计时 / 目标提示 / 保存
- 孕妈指标：体重、血压、血糖（含空腹 / 餐后情境与妊娠糖尿病软范围提示）

**出生后**

- 育儿日视图骨架：按日期查看，喂养 / 睡眠 / 尿布等快捷记录

**通用**

- 统一时间线 + 阶段投影（孕期 / 出生后界面按当前阶段切换）
- 本地备份导出 / 导入（含覆盖确认）
- 暗色模式、单位徽标、等宽数字、防误删二次确认

## 当前状态

| 项 | 内容 |
|---|---|
| 主线 | **Android 原生 APK**（`android-native/`） |
| UI 技术 | **Jetpack Compose**（单一 UI；早期 Java 手写 View 已废弃删除） |
| 逻辑层 | Java（Domain / Service / Repository / Formatters / FileProvider / ImageUtils），Compose 复用 |
| 数据 | 本机 `SharedPreferences` JSON（后续视情况迁 Room） |
| 同步 | VPS 已就绪；按 `familyId` 的 push/pull 为后续阶段，未配置时纯本机可用 |
| 后端 | PocketBase（已定） |
| 范围 | 单胎 / 单宝宝；多胎仅数据模型 1:N 预留，UI 不实现 |
| 阶段二 | PWA / iOS 暂搁置（见「参考与历史」） |

## 仓库结构

### 主线（在用）

| 路径 | 说明 |
|---|---|
| `android-native/` | Android 原生 APK 工程；`ComposeMainActivity.kt` 为唯一 Activity |
| `android-native/app/src/main/java/app/babylog/nativeapp/` | Compose UI + Java 逻辑层 |
| `android-native/app/src/main/java/app/babylog/nativeapp/ui/` | Compose UI 分包：`components` / `pregnancy` / `theme` |
| `android-native/smoke-tests/` | 离线可跑的纯 JVM smoke test |
| `android-native/README.md` | 构建、签名、离线缓存说明 |
| `.github/workflows/android-native.yml` | CI：JVM smoke test + `assembleDebug` + 条件 signed release |

### 产品与设计文档

| 路径 | 说明 |
|---|---|
| `项目立项书.md` | 产品定位、范围、数据模型、里程碑、非目标 |
| `需求评估表.md` | 需求优先级、MVP 裁剪、风险 |
| `UI设计书.md` | UI 方向、design tokens、组件、屏幕规格 |
| `docs/BabyLog阶段主线重构计划.md` | 阶段投影重构计划与验收标准 |
| `docs/BabyLog重构前逻辑核查.md` | 重构前锁定的逻辑缺陷与门禁（L-1~L-13） |
| `docs/Piyo对标差异与BabyLog产品方向.md` | 对标 Piyo日志的产品方向分析 |

### 参考与历史（不在主线迭代）

| 路径 | 说明 |
|---|---|
| `app/` | Vite + React + TS 的 PWA 实现；当前仅作数据模型 / 字段定义 / 业务逻辑参考，UI 已被 Android 主线取代 |
| `prototype/` | 早期静态 HTML 原型，仅留作设计史 |
| `deploy/`、`Vultr部署说明.md`、`腾讯云部署说明.md`、`iOS部署说明.md` | PWA / 阶段二 服务器部署资料，与当前 Android 主线无关 |

> 仓库内不写入服务器 IP、域名、SSH 用户、Android keystore 等敏感参数；部署参数通过脚本参数或 CI Secrets 注入。

## 构建（Android 主线）

要求：JDK 17、Android SDK（`compileSdk 36`）。首次构建需联网，让 Gradle 缓存 Compose / Kotlin / AndroidX 依赖；之后可离线构建。

```powershell
cd android-native
.\gradlew.bat "-Pandroid.aapt2FromMavenOverride=$env:LOCALAPPDATA\Android\Sdk\build-tools\36.0.0\aapt2.exe" :app:assembleDebug --console=plain
```

产物：`android-native/app/build/outputs/apk/debug/app-debug.apk`

Release 签名走本机 `android-native/signing.properties` 或 CI Secrets 注入；配置 `BABYLOG_RELEASE_KEYSTORE_BASE64` 等 Secret 后 CI 构建 signed release。详见 `android-native/README.md`。

## 隐私与边界

- 数据本机优先；离线完整可用，不依赖服务器
- B 超单识别只用用户本机配置的多模态模型 API，API Key 仅本机加密保存，不进备份 / 同步 / 设备迁移
- 任何模型识别结果只生成候选，必须人工确认后才入库

## 非目标

- 不做公开 SaaS / 多家庭运营 / 商业化（自家 + 至多月嫂的小范围使用）
- 不做第三方账户（Google / Apple / 微信）；同步只做家庭密钥校验
- 不做医疗诊断；曲线与范围仅作记录和参考提示
- 阶段一不做完整云同步、不迁 Room（接口与字段预留，能力后置）
