# BabyLog

**👶 我还在妈妈肚子里，但我的故事，他们已经开始一笔一笔记下来了。**

[![Android Native](https://github.com/pickupthepieces/BabyLog/actions/workflows/android-native.yml/badge.svg)](https://github.com/pickupthepieces/BabyLog/actions/workflows/android-native.yml)
[![License: Apache 2.0](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](LICENSE)
[![Platform](https://img.shields.io/badge/platform-Android-3DDC84.svg)](android-native/)
[![Stage](https://img.shields.io/badge/scope-孕期主线-E8896B.svg)](#我的成长路线)

> 我是这个 App 要记录的那个小家伙。
> 爸爸妈妈、爷爷奶奶、外公外婆，必要时还有月嫂，会从备孕、孕期一路记到我慢慢长大。
> 他们说：**我的事，要留在自己手机里；和医疗有关的，只记录、不替我下结论。**

---

> ## ⚠️ 医疗免责声明 / Medical Disclaimer
>
> （这一段他们叮嘱我，必须一字一句、严肃地写清楚——）
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

## 关于我，和这个 App 不一样的地方

别的记录工具，要么把我的事存到别人的服务器上，要么把"参考范围"说得像医生的结论。
他们给我做的这个反过来——

- **我的事在我家**　不用注册、不用联网也能完整用，不靠任何后端。
- **就我一个**　数据结构虽然给多宝宝留了位置，但界面只认真服务我们一家这一个清楚的场景。
- **该是孕期就是孕期，出生了就是出生后**　同一条时间线，会自己切换该给他们看的样子。
- **机器只是帮忙认字**　B 超单识别、语音转写、智能解析都只是先猜一版，**得他们亲手确认了才算数**。

## 他们为我记录些什么

### 还没出生的时候（现在的主线，已经能好好用了）

- **首页就能看到我**：第几周了、离预产期还有多少天、最近一次产检和 B 超、有没有要复核的
- **产检 / 胎动 / 宫缩**一拍就记；我踢妈妈的时候，有个胎动计数器（开始 / 数 / 计时 / 到 10 次提醒）
- **B 超的各种数字**（BPD / HC / AC / FL / EFW…）能录、超出常用范围会轻轻提示、单子能拍照存起来
- **B 超单拍一下就识别**：接他们自己配的多模态模型，只生成候选，要他们核对
- **对着手机说一句就行**：按住说话 → 独立配置的 Paraformer 转成可改的文字 → 自动打开该填的表单
- **我的成长曲线**：按孕周把我自己的 B 超趋势画出来
- **也照顾妈妈**：体重 / 血压 / 血糖（分空腹·餐后，妊娠糖尿病有软范围提示）

### 不管哪个阶段

- 一条统一时间线，按阶段自动切换；记录能本地导出 / 导入（覆盖前会再问一次）
- 暗色模式、单位小徽标、数字对齐、删错了有二次确认

### 等我出生以后（先搭了骨架，育儿期是后面的主线）

- 育儿日视图骨架：按天看，喂奶 / 睡觉 / 尿布快捷记一笔

## 我的成长路线

| 阶段 | 内容 | 状态 |
|---|---|---|
| **现在** | Android 原生 · **孕期主线** | 基本可用，持续打磨 |
| **进行中** | 单 Activity → Navigation-Compose 多页面架构重构 | P0–P5 已完成，收尾中 |
| **下一步** | **育儿期**（出生后 0–3 岁）功能补全 | 骨架已在，待主线化 |
| **之后** | **PWA**（跨端访问）、家庭密钥同步 | 接口与数据模型预留 |

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
- B 超识别 / 文本结构化只用用户本机配置的多模态 / LLM API；语音转文字使用独立 STT 配置。
  两类 API Key 均**仅本机加密保存，不进备份 / 同步 / 设备迁移 / 日志**。
- 任何模型识别结果只生成候选，**必须人工确认后才入库**。

## 非目标

- 不做公开 SaaS / 多家庭运营 / 商业化（自家 + 至多月嫂的小范围使用）
- 不做第三方账户（Google / Apple / 微信）；同步只做家庭密钥校验
- 不做医疗诊断；曲线与范围仅作记录和参考提示（完整免责见 [DISCLAIMER.md](DISCLAIMER.md)）

## 许可证

以 **Apache License 2.0** 授权（含 "AS IS" 无担保与责任限制条款），详见 [LICENSE](LICENSE)，
并须与 [DISCLAIMER.md](DISCLAIMER.md) 一并阅读。当前仓库为私有，待完善后再考虑开源。

---

> 等我长大了，翻到这些记录，会知道——
> 从我还是一个数字、一段心跳开始，他们就很用心地，把我记着了。
