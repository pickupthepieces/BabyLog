# BabyLog

BabyLog 是一个家庭自用的孕育记录 App，目标覆盖怀孕全阶段到宝宝 3 岁。当前阶段一优先做 Android 原生 APK，先在小米真机上跑通；PWA 路径在 `app/` 暂搁置作为参考实现。本地优先，后端、同步和 OCR 都先保留接口，不阻塞本地使用。

## 当前范围

- **阶段一**：Android 原生 APK（`android-native/`，Java + Android SDK，无第三方依赖）。
- **阶段二**：再评估 PWA / iOS 走向；`app/` (Vite + React + TS) 仍维护数据模型与字段定义作为参考。
- 当前 App 仅支持单胎/单宝宝 UI；数据模型保留 `familyId` / `childId`，为后续扩展预留。
- 本地优先，原生用 `SharedPreferences` JSON（计划迁 Room）；PWA 用 IndexedDB。
- 后端技术栈未决，倾向 PocketBase，FastAPI 仍为候选。
- 医疗内容只做记录、趋势和参考提示，不做诊断。

## 项目结构

| 路径 | 说明 |
|---|---|
| `android-native/` | **阶段一主线**：Java + Android SDK 原生 APK；纯净栈、离线可构建 |
| `android-native/app/src/main/java/app/babylog/nativeapp/` | Activity、Domain、Repository、Service、Formatters |
| `android-native/smoke-tests/` | 离线可跑的纯 JVM smoke test |
| `app/` | 参考实现：Vite + React + TypeScript PWA（阶段二再评估） |
| `app/src/domain/` | 数据模型、孕周/年龄计算、B 超字段定义（双端共享 schema） |
| `app/src/storage/` | IndexedDB repository、备份、附件、同步队列 |
| `app/src/adapters/` | 后端、OCR、浏览器存储适配层 |
| `app/src/services/` | UI 可复用的应用服务层 |
| `deploy/` | Nginx 配置和 Windows 部署脚本（PWA 路径） |
| `prototype/` | 静态 HTML 原型（设计参考） |
| `*.md` | 立项、需求、UI、Android 补充、部署文档 |

## Android 原生构建（阶段一）

要求：JDK 17、Android SDK（验证使用 `compileSdk 36`）。

```powershell
cd android-native
.\gradlew.bat "-Pandroid.aapt2FromMavenOverride=$env:LOCALAPPDATA\Android\Sdk\build-tools\36.0.0\aapt2.exe" clean :app:assembleDebug --offline --console=plain
```

APK 输出：`android-native/app/build/outputs/apk/debug/app-debug.apk`。

详见 `android-native/README.md`。

## PWA 本地开发（参考）

```powershell
cd app
npm install
npm run dev
```

```powershell
cd app
npm test
npm run build
```

PWA 最近一次验证：`9 files / 40 tests passed`，`npm run build` passed。

## 部署

PWA 服务器部署说明：

- `Vultr部署说明.md`
- `腾讯云部署说明.md`
- `iOS部署说明.md`
- `deploy/nginx-babylog.conf`
- `deploy/deploy-pwa.ps1`

Android APK 分发：待补 release signing、keystore 流程和 GitHub Actions 构建。

服务器 IP、域名、SSH 用户、Android keystore 等敏感参数不写入仓库。
