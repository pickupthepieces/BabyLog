# 语音录入 v1 + UI（I 轮）Claude 终审

| 项目 | 内容 |
|---|---|
| 审核人 | Claude (Opus 4.7) |
| 日期 | 2026-05-19 |
| 范围 | `97a0e0a..63bdd59`（语音 v1 全链 + I 轮 UI + 文档收敛） |
| 结论 | **通过**（仅余非阻塞小项 + 待装机验证项） |
| 工作区 | 干净;提交按 feat/ui/test/docs 规范拆分,未混 |

## 一、逐条核验（均以代码为证）

| 要求 | 结论 | 证据 |
|---|---|---|
| 单一全局入口（非每菜单分散） | ✅ | 唯一 `SmartEntryDialog`/`showSmartEntryDialog`;语音入口居中于底栏 |
| App 内录音（非系统键盘语音） | ✅ | `BabyLogPcmVoiceRecorder` 录至 `cacheDir/voice-stt/*.pcm`;按住说话 |
| 云端专业 STT | ✅ | `BabyLogParaformerSpeechClient` → 官方 `wss://dashscope.aliyuncs.com/api-ws/v1/inference/` |
| 转写文本可编辑再识别 | ✅ | "语音已转文字，请核对后再识别"——LLM 前有人工检查点 |
| LLM 仅生成候选、不自动入库 | ✅ | `openSmartEntryCandidate` 打开表单草稿;"仍需你在表单里手动保存" |
| 人工确认才保存 | ✅ | 路由到对应表单后由用户手动保存;分类不支持有兜底（line 1003） |
| RECORD_AUDIO 运行时权限流 | ✅ | `checkSelfPermission`→`audioPermissionLauncher.launch`;Manifest 已声明 |
| 权限拒绝优雅降级 | ✅ | "麦克风权限被拒绝，可继续手动输入文本" |
| 离线优雅降级（语音=可选增强） | ✅ | "当前网络不可用，语音作为增强功能已降级;可以直接手动输入" |
| 音频临时、用后即删、不入备份/同步/日志 | ✅ | `finally { audioFile.delete() }`;cancel 删除;录音仅落 cacheDir |
| STT 密钥 Keystore 加密、与视觉密钥分离 | ✅ | `PREF_SPEECH_API_KEY_CIPHER_TEXT`+IV，AndroidKeyStore 加解密 |
| 隐私告知含"语音"项 | ✅ | "按住说话会把本次语音发给你配置的语音识别服务商…只有你主动按住说话时,本次语音才会发送" |
| 旧评估文档收敛单源 | ✅ | `语音录入需求评估-Claude.md` 已标"旧系统键盘 STT 结论已废弃" |
| 受保护 composable 未动 | ✅ | WeekCard/PregnancySummaryPanel/Panel/EmptyPanel/SectionHeader/TrendCard 不在 diff |
| I 轮:Material Rounded 图标、非贴纸/非 imagegen | ✅ | `LineIcon` 全 `Icons.Rounded.*`;`BabyLogIconTile` 统一 |
| I 轮:主题色顶栏/底栏 band | ✅ | BottomNav `backgroundColor=Primary` 白图标;Splash 主题色满屏 |
| Splash:Compose 画 confetti、单一栗子、不生成 | ✅ | `SplashBackdrop` Canvas 绘制;栗子仅 splash 一处 |

## 二、非阻塞小项（建议改，不卡放行）

1. **Splash 文字**:"BabyLog" 为纯文字药丸。用户原话"不要文字、最多应用名 logo"——勉强算应用名 logo,可保留,但它是文本非设计 logo,留作后续美化项。
2. **冗余资产**:`chestnut_mascot_splash.png`(7.5KB)未被引用;splash 实际用 `chestnut_mascot.png`(90KB→272KB)。建议删冗余文件 + 压缩 272KB 大图。
3. **权限拒绝提示**:已有文案降级,建议补一次性 Toast 明确指向"设置可重新授权",减少困惑（次要 UX）。

## 三、待装机验证（代码已就绪，需真机确认；测试机被取走 α，延后）

1. 真机麦克风按住录音 → Paraformer 往返 → 转写文本回填 → LLM 分类 → 打开正确表单 → 手动保存 全链路。
2. 权限首次申请/拒绝后的实际交互与降级体验。
3. 弱网/断网下语音降级提示与手动输入可用性。
4. 确认 `voice-stt` cache 与 speech 偏好确实不出现在 App JSON 导出与家庭同步内容里（代码层为独立加密存储 + 导出仅 events/profile,逻辑上成立,装机抽查一次更稳）。

## 四、裁定

语音录入 v1 架构与四条护栏**全部正确落地**,医疗安全底线（确认前不落库、不诊断）与隐私纪律（音频即删、密钥加密、出境显式告知）守住;I 轮 UI 命中既定方向（扁平 + 主题色 band + Material Rounded + 栗子吉祥物）。**通过,可继续。** 二、三项为后续打磨与装机回归,不阻塞当前进度。
