# BabyLog

[![Android Native](https://github.com/pickupthepieces/BabyLog/actions/workflows/android-native.yml/badge.svg)](https://github.com/pickupthepieces/BabyLog/actions/workflows/android-native.yml)
[![License: Apache 2.0](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](LICENSE)
[![Platform](https://img.shields.io/badge/platform-Android-3DDC84.svg)](android-native/)
[![Stage](https://img.shields.io/badge/scope-孕期主线-E8896B.svg)](#路线图)

> 一个家庭自己用的小本子：从备孕、孕期，到宝宝慢慢长大，一路记下来。
> 数据就放在自己手机里，不用联网也能用；和医疗有关的，只帮忙记，不替你下结论。

---

> ## ⚠️ 医疗免责声明 / Medical Disclaimer
>
> BabyLog 仅为个人/家庭自用的**记录与参考工具**，**不是医疗器械**，未经任何监管或认证机构注册审批，
> **不提供医疗建议、诊断、治疗或临床决策**，使用不构成医患关系。应用内一切数值、范围提示、百分位、
> Z-score 与曲线**仅供参考**；**胎儿生长参考为"未校准近似实现"，未接入完整临床标准，可能存在显著误差**；
> OCR / 语音 / 大模型识别可能出错，均为需人工确认的候选。任何健康相关判断**请务必咨询有资质的医疗专业人员**；
> 出现异常或紧急情况**请立即线下就医**。本项目按 **"AS IS"** 提供，**无任何担保、不承担任何责任、风险自负**。
>
> _Not a medical device. No medical advice or diagnosis. Reference only. Always consult a qualified
> professional; seek in-person care in emergencies. Provided "AS IS" with no warranty or liability._
> 完整条款见 **[DISCLAIMER.md](DISCLAIMER.md)**。

---

<sub>以下为工程信息（设计原则 / 功能 / 技术栈 / 构建）。</sub>

## 设计原则

- **本地优先**：无需账号、无需联网即可完整使用，不依赖后端。
- **单胎单宝宝**：数据模型预留 1:N，界面只服务一个家庭的单一场景。
- **阶段投影**：同一条时间线，按"孕期 / 出生后"自动切换首页与录入。
- **人在回路**：B 超 OCR、语音转写、智能解析只生成候选，**人工确认后才入库**。
- **不做判断**：医疗相关内容只呈现与记录，并在每处标注其参考性质与不确定性。

## 功能

### 孕期（当前主线，已基本可用）

- 孕期首页：孕周、距预产期天数、孕期摘要（最近产检 / 最近 B 超 / 待复核）
- 产检结构化记录：常规层（血压 / 体重 / 宫高 / 腹围 / 胎心率 / 尿常规 / 结论 / 下次日期 / 附件）
  + 专项（NT / 唐筛 / 无创 DNA / 大排畸 / OGTT / GBS / NST），只记录报告原文，不计算风险
- 胎动 / 宫缩快捷记录；胎动会话计数器（开始 / 计数 / 计时 / 目标提示）
- B 超指标录入（BPD / HC / AC / FL / EFW…）+ 软范围提示 + 拍照归档
- B 超单识别：接入用户自带 OpenAI-compatible 多模态模型，仅生成候选字段
- 全局语音录入：按住说话 → 独立配置的 Paraformer STT 转可编辑文本 → 智能解析打开对应表单
- 胎儿成长曲线：按孕周绘制自有 B 超趋势 + 香港近似参考百分位 / Z-score（未校准，仅供记录）
- 孕妈指标：体重 / 血压 / 血糖（含空腹·餐后情境与妊娠糖尿病软范围提示）

### 通用

- 统一时间线 + 阶段投影；本地备份导出 / 导入（含覆盖确认）
- 暗色模式、单位徽标、等宽数字、防误删二次确认

### 出生后（骨架，育儿期主线见路线图）

- 育儿日视图骨架：按日期查看，喂养 / 睡眠 / 尿布等快捷记录

## 路线图

| 阶段 | 内容 | 状态 |
|---|---|---|
| 现在 | Android 原生 · 孕期主线 | 基本可用，持续打磨 |
| 进行中 | 单 Activity → Navigation-Compose 多页面架构重构 | P0–P5 已完成，收尾中 |
| 下一步 | 育儿期（出生后 0–3 岁）功能补全 | 骨架已在，待主线化 |
| 之后 | PWA（跨端访问）、家庭密钥同步 | 接口与数据模型预留 |

## 技术栈

| 层 | 选型 |
|---|---|
| 平台 | Android 原生 APK（`android-native/`，`compileSdk 36`） |
| UI | Jetpack Compose（单一 UI 技术）+ Navigation-Compose 多页面 |
| 逻辑 | Java：Domain / Service / Repository / Formatters / FileProvider / ImageUtils（Compose 复用） |
| 存储 | 本机 `SharedPreferences` JSON（后续视情况迁 Room） |
| 识别 | 用户自带 OpenAI-compatible 多模态 / LLM API；语音用独立 STT 配置；Key 仅本机加密 |
| 同步 | PocketBase（已定，后置）；按 `familyId` push/pull 为后续阶段，未配置时纯本机可用 |
| CI | GitHub Actions：JVM smoke test + `assembleDebug` + 条件 signed release |

## 仓库结构

```
android-native/        Android 主线工程（Compose UI + Java 逻辑层）
  app/.../ui/screens/  Navigation-Compose 页面
  app/.../ui/          components / pregnancy / theme 分包
  smoke-tests/         可离线运行的纯 JVM smoke test
docs/                  产品方向、重构计划、阶段评审与工作队列
DISCLAIMER.md          完整医疗免责声明
app/  prototype/  deploy/   早期 PWA / 原型 / 部署资料（参考与历史，不在主线迭代）
```

> 仓库内不写入服务器 IP、域名、SSH 用户、Android keystore 等敏感参数；部署参数通过脚本参数或 CI Secrets 注入。

## 构建

要求 JDK 17、Android SDK（`compileSdk 36`）。首次构建需联网缓存 Compose / Kotlin / AndroidX 依赖，之后可离线。

```powershell
cd android-native
.\gradlew.bat "-Pandroid.aapt2FromMavenOverride=$env:LOCALAPPDATA\Android\Sdk\build-tools\36.0.0\aapt2.exe" :app:assembleDebug --console=plain
```

产物：`android-native/app/build/outputs/apk/debug/app-debug.apk`。
Release 签名走本机 `android-native/signing.properties` 或 CI Secrets，详见 [`android-native/README.md`](android-native/README.md)。

## 隐私与边界

- 数据本机优先，离线完整可用，不依赖服务器。
- B 超识别 / 文本结构化只用用户本机配置的多模态 / LLM API；语音转文字使用独立 STT 配置；
  两类 API Key 均仅本机加密保存，不进备份 / 同步 / 设备迁移 / 日志。
- 任何模型识别结果只生成候选，必须人工确认后才入库。

## 非目标

- 不做公开 SaaS / 多家庭运营 / 商业化（自家 + 至多月嫂的小范围使用）
- 不做第三方账户（Google / Apple / 微信）；同步只做家庭密钥校验
- 不做医疗诊断；曲线与范围仅作记录和参考提示（完整免责见 [DISCLAIMER.md](DISCLAIMER.md)）

## 许可证

以 **Apache License 2.0** 授权（含 "AS IS" 无担保与责任限制条款），详见 [LICENSE](LICENSE)，
并须与 [DISCLAIMER.md](DISCLAIMER.md) 一并阅读。当前仓库为私有，待完善后再考虑开源。
