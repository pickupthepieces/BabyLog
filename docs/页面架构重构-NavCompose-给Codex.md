# 页面架构重构：单 Activity 巨弹框 → Navigation-Compose 多页面

| 项目 | 内容 |
|---|---|
| 决策 | 用户拍板：**Navigation-Compose 多页面**（单 Activity + 多真实页面目的地） |
| 病灶 | `ComposeMainActivity.kt` 4331 行 god Activity；14 个 `*Dialog`/15 处 `AlertDialog`；真实页面仅 4 个（home/timeline/library/settings 挤同一 LazyColumn） |
| 目标 | 重表单/设置子页/浏览类升级为带返回栈+AppBar+转场+状态保持的真实页面；弹框只留真确认 |
| 纪律 | **架构轮,分阶段,每阶段独立 commit,不与功能混提交,不并 main** |

## 〇、核心认知（先读，别误解）

**"单 Activity" 不是病灶；"巨石 + 全弹框 + 零页面化" 才是。**

- ❌ 不要拆成很多 Activity。Piyo 实证也只有 2 个 Activity（Initial + Main），现代 Android 正解就是 single-Activity + Navigation。
- ✅ 目标 = 让 `ComposeMainActivity` 变成**薄壳 host**（只托管 NavHost），把 4331 行内容拆成**一堆各自独立、各有返回栈/状态的真实页面**（`ui/screens/*.kt` 一屏一文件）。
- 对照：Piyo = 单 Activity · 多页面 · 薄壳；我们现在 = 单 Activity · 零页面 · 巨石。差距在**有没有内部页面化**，不在 Activity 数量。
- 重构后 MainActivity 应显著变薄；若它仍是几千行 = 没做对。

## 一、弹框去留判定

**保留为弹框（短确认/瞬时）**：`ConfirmDialog`（破坏性确认）、`showInfo`/InfoDialog（瞬时提示）。

**升级为 Nav 页面（带 TopAppBar + 返回）**：
| 现状弹框 | 目标页面 | 备注 |
|---|---|---|
| `UltrasoundDialog`(~470行) | `record/ultrasound` 表单页 | 最严重,字段最多 |
| `PregnancyEventDialog` | `record/pregnancyEvent` 表单页 | |
| `MaternalMetricDialog` | `record/maternalMetric` 表单页 | |
| `BabyCareDialog` | `record/babyCare` 表单页 | |
| `SmartEntryDialog` | `smartEntry` 页 | 同时解决模态套模态:语音/转写/候选内联,确认后跳对应表单页 |
| `QuickActionDialog` | 取消,改**两阶段常驻 quick rail** | 见阶段 5 |
| `ProfileDialog`/`SyncSettingsDialog`/`SmartModelSettingsDialog`/`SpeechSettingsDialog` | `settings` 页 + 各子页 | 设置建层级 |
| `AttachmentListDialog`/`TrashDialog`/资料库详情 | `library`/`library/trash` 列表页 | |
| `AttachmentPreviewDialog` | `attachment/preview` 全屏查看页 | |

## 二、Nav 图

- **底栏 4 个顶层目的地**：`home` / `timeline` / `library` / `settings`（底栏切换这 4 个，保持现有 IA 语义）
- **压栈目的地（带返回）**：`record/{type}`、`smartEntry`、`settings/profile|sync|model|speech`、`library/trash`、`attachment/preview`

## 三、分阶段（每阶段一笔自包含 commit，assembleDebug+lintDebug+smoke 全绿）

- **P0 脚手架**：引入 Navigation-Compose 依赖 + NavHost；把现有 4 个 tab-view 原样搬进 Nav 目的地（底栏照旧切换）。**纯管线,零行为变化**。
- **P1 重表单上页**：Ultrasound/PregnancyEvent/MaternalMetric/BabyCare 四个 AlertDialog → 全屏表单页。**逐字段、校验、人工确认链、阶段投影、FGR/Hadlock 接线一字不漏保留**。每页拆独立文件。
- **P2 SmartEntry 上页**：语音/转写/候选内联到 `smartEntry` 页,确认后导航到 P1 的对应表单页（顺带消灭"FAB→Quick→Smart"模态套娃）。
- **P3 设置层级化**：Settings 页 + Profile/Sync/Model/Speech 子页。
- **P4 浏览类上页**：Library/Attachments/Trash 列表页；AttachmentPreview 全屏查看页。
- **P5 交互收敛**：两阶段都给常驻 quick rail（孕期:B超/产检/胎动/孕妈指标…一拍即记）；语音=显式带标签麦克风控件,禁隐藏长按手势;删首页回潮的竖向渐变(`Brush.verticalGradient` ~line 1472),回扁平。
- **贯穿**：每阶段把迁移部分从 4331 行 god 文件拆到 `ui/screens/*.kt`、`ui/dialogs/*.kt`,减体积、防混合大提交。

## 四、硬约束（MUST / MUST NOT）

- **MUST 保留**：阶段投影（pregnancy/baby/unknown）、首登门控、每个表单字段与校验、**人工确认才保存(绝不自动入库)**、FGR/Hadlock/OCR/STT 接线、密钥 Keystore 加密、音频用后即删、隐私告知文案、医疗安全（不诊断）。
- **MUST 保状态**：页面转屏/返回/进程恢复不丢草稿;返回键语义正确。
- **MUST NOT**：改数据/事件模型与业务逻辑;改已锁定视觉（palette/扁平/Material Rounded 图标/主题色 band/受保护 6 composable）;架构轮与功能轮混进同一 commit;未经用户同意并 main。
- **每阶段**：本地三件套全绿;P1/P2 完成后装机回归（导航、返回栈、转屏、人工确认链、FGR/OCR/STT 全链）。

## 五、Piyo 实证与体验准则（重构时遵循）

**架构实证**：抓 Piyo 运行期,全程仅 2 个 Activity——`InitialActivity`(启动/引导) + `MainActivity`(其余全部)。Piyo 是**单 Activity 内部多页面导航**,不是每块内容一个 Activity。→ 本重构选 Navigation-Compose 被 Piyo 实证背书,方向正确。

**可借鉴（落进各阶段）**：
1. **一拍即记,不漏斗**（P5 重点）：Piyo 首页常驻底部彩色快捷行,点一下直开聚焦记录页,**无 FAB→模态→再模态**。我们两阶段都要常驻 quick rail;语音=显式麦克风控件。
2. **记录页是聚焦页不是堆砌**（P1 重点）：Piyo 记录页时间预填、字段精简、一键存、即时返回时间轴。我们硬约束是逐字段保留——**字段全留,但用页面尺度做分区/分组/可滚 + 顶部 AppBar 返回,让它"喘气";页面化≠把 470 行弹框原样平移成同样拥挤的页**。常用字段在首屏,次要字段折叠/分区。
3. **强反馈闭环**：Piyo 存完即回时间轴并可见新条目。我们每个记录页保存后应导航回来源页并定位/高亮刚存的记录,形成"记了→看见了"闭环。
4. **层级清晰**：菜单→设置→子设置 全屏列表逐级下钻(P3),不是一堆并列弹框。
5. **底栏 + 主题色 band + 4 顶层**:已落地,保持。

**体验准则（每页通用)**：
- 任一核心记录动作 **≤2 次点击**到可输入;**≤2 屏**到确认保存。
- 每个非根页面必须有：标题 AppBar + 明确返回 + 不丢草稿(转屏/进程恢复)。
- 破坏性/不可逆动作才用确认弹框;表单/浏览/设置一律页面。
- 空态一句引导 + 一个明确主操作,不堆"暂无"。
- 保存前永远人工确认,绝不自动入库(医疗安全底线)。

## 六、给 Codex 的话（直接照做）

> 读 `docs/页面架构重构-NavCompose-给Codex.md` 全文（含第五节 Piyo 实证与体验准则）。架构已定 Navigation-Compose 多页面,Piyo 实证为单 Activity 多页面、本方向正确。**严格按 P0→P5 分阶段,每阶段一笔独立 commit（message 标「架构:NavCompose 第N阶段」）,不与任何功能/视觉轮混提交,不并 main。** P0 先做纯脚手架（引 Nav + NavHost + 现有 4 tab 原样搬入,零行为变化）交 Claude review,通过再做 P1。P1 起遵循第五节:页面化≠原样平移,逐字段保留但用页面尺度分区呼吸、常用字段首屏、保存后回来源页并高亮新记录、核心动作≤2 点击/≤2 屏、人工确认绝不自动入库。别动数据/逻辑/已锁视觉/阶段投影/FGR/OCR/STT 接线。每阶段本地 assembleDebug+lintDebug+smoke 全绿;P1/P2 装机回归。把迁移部分从 4331 行 god 文件拆到 `ui/screens/*.kt`、`ui/dialogs/*.kt`。

## 七、阶段评审记录（Claude）

### P0 脚手架 — 通过（commit `a33352b`）

**结论**：通过,放行 P1。

**核实**：nav-compose 2.8.5 引入;NavHost+rememberNavController+4 顶层 composable 目的地;底栏切换用标准 `popUpTo(Home){saveState=true}`+`launchSingleTop=true`;4 tab 原样搬入,IA 未变;onCreate 各路径 reset Home;单 commit、零数据/逻辑/视觉改动、未混功能。

**P1 必须处理的遗留**：
1. **双状态收敛**：P0 暂留 `activeTab`(mutableStateOf) 与 navController 双向同步作为过渡桥。P1/P2 必须收敛为**单一事实源(navController 唯一)**,清除 `activeTab`,不得固化。
2. **瘦身兑现**：P0 文件 4331→4380(脚手架未抽屏,符合预期)。**P1 起必须把 4 tab 体抽到 `ui/screens/*.kt`,MainActivity 须开始显著变薄**——置顶第〇节终态硬线持续作为每阶段验收门。
3. 编译/smoke 由 CI assembleDebug 兜底验证(本地沙箱限制无法跑)。

### P1 重表单上页 — 通过（commit `2600e75`）

**结论**：通过,放行 P2。

**核实**：① P0 遗留①已解决——`activeTab` mutableStateOf 删除,改为从 navController currentRoute 派生的 val,单一事实源（`recordReturnRoute` 为"存完回来源页"的合法独立状态,非双源）。② P0 遗留②兑现——CMA 4331→3715,4 tab + 4 表单抽到 `ui/screens/*.kt`,共享 `RecordFormScaffold`。③ 数据/逻辑/OCR/FGR/STT 层零改动（commit 仅 CMA+ui/screens;Ultrasound 仍调 `estimateEfwHadlock3Gram`,接线仅迁移）。④ 人工确认链保留（`onSave` 显式回调、无 autoSave）。⑤ 单 commit、纪律守住。编译/smoke 由 CI 兜底。

**P2+ 持续门**：CMA 仍 3715 行（SmartEntry 等仍内联），瘦身继续;装机字段全等性建议 P2 一并抽查。

### P2 SmartEntry 上页 — 通过（commit `feed1a2`+`36d1c64`）

**结论**：通过,放行 P3。

**核实**：① `SmartEntryDialog` 删除 → `SmartEntryScreen.kt` 全屏页;smartEntry route,短按/长按释放均入同页,模态套娃消灭。② STT/RECORD_AUDIO 权限流/隐私文案/`finally{audioFile.delete()}` 在 CMA 内原样保留,仅迁移 UI 载体。③ 确认链:页内候选 → 显式「打开表单核对」→ P1 表单页 → 手动保存,无 autosave。④ CMA 3715→3628 继续瘦。⑤ `feed1a2`「长按释放结束录音」为独立 UI-only bugfix,未混入 P2、未碰逻辑层——hygiene 正确。编译/smoke 由 CI 兜底。

**跟进项（非阻塞）**：BabyCare 表单因真机档案为孕期,未装机走查字段全等性;P3/后续切档抽查一次。

### P3 设置层级化 — 通过（commit `9d17646`）

**结论**：通过,放行 P4。

**核实**：① 4 设置 AlertDialog(Profile/Sync/Model/Speech)删除 → NavCompose 全屏子页 + 共享 `SettingsPageScaffold`,逐级下钻。② 密钥逻辑仅迁载体——新屏幕为 `onSave:(Config)->Unit` 纯 UI 回调,真实 `smartConfigStore.saveSpeechConfig`(Keystore 加密)仍在 CMA:580,未重写;隐私文案逐字保留。③ 仅 CMA+ui/screens 改动,数据/STT/FGR 接线零动。④ CMA 3628→3312 继续瘦。⑤ 单 commit 干净。编译/smoke 由 CI 兜底。

**产品决策项（非 bug，非 P3 阻塞，待用户拍板）**：Codex 装机发现 baby 快捷"奶瓶"直接入库、不过 BabyCare 表单。经评估此非安全违规——非 AI 候选、非医疗测量、属琐碎自记录,且 Piyo 本身即"一拍即记"、记录可在时间线删改。"人工确认绝不自动入库"铁律针对 AI 候选 + 医疗数据,不含一拍即记琐碎事件。**默认建议保持一拍即记**;是否改走表单确认由用户决定,不作为缺陷修复强推。

### P4 浏览类上页 — 通过（commit `8b59bcc`）

**结论**：通过,放行 P5（末轮）。

**核实**：① `AttachmentListDialog`/`AttachmentPreviewDialog`/`TrashDialog` 删除 → `AttachmentListScreen`/`AttachmentPreviewScreen`/`TrashScreen` 全屏页。② 仅 CMA+ui/screens 改动,回收站7天保留/清理/附件读取/FileProvider 逻辑层零动。③ 破坏性删除仍走 `ConfirmDialog`(CMA:391 "移入回收站"全文确认保留);TrashScreen 仅恢复,无新增未确认永久删除。④ CMA 3312→3140。⑤ 单 commit 干净。编译/smoke 由 CI 兜底。

**进度**：CMA 4331→3140;P0–P4 全通过。

### 用户决策（2026-05-19）

1. **baby 快捷"奶瓶"等一拍即记 = 保持原样**（Piyo 风,按设计如此,不改走表单确认）。该决策项关闭,不单开轮次,P5 及后续不要动 baby 快捷入库行为。
2. **当前焦点 = 孕期**；出生后档保留但非重点。**P5 quick rail 重心放孕期常驻 rail**;baby 侧维持现状,不过度投入。P2 遗留"BabyCare 真机字段全等性走查"**降级**为低优先（baby 非焦点,逻辑已保留即可,不阻塞）。

### P5 追加：首页滚动卡顿性能修复（并入 P5，用户决定）

装机发现首页上下滑卡顿。Claude 代码定位两根因,**并入 P5 一笔一起做**（不单开）：

1. **根因A（主因）**：`BabyLogScreenColumn`(CMA:1717,所有列表页公共壳) 的 `LazyColumn.background(Brush.verticalGradient(...))` ——渐变写在 composable 体内未 remember,且铺在滚动容器背景=每帧全屏 overdraw 重绘,所有页滚动皆卡,首页最重。**修：删该 verticalGradient,改纯色 `ChestnutPalette.Bg`**（与 P5「删首页回潮渐变回扁平」同一动作）。今后禁止把未 remember 的 Brush 直接作滚动容器背景。
2. **根因B**：`FetalGrowthChart.kt` ~180,`p10/p50/p90Values = referenceWeeks.map{ referenceValue(...) }` 共 27周×3=81 次 BCCG/LMS 超越函数,位于 `Canvas{}` DrawScope 内→每次绘制/重组重算。**修：移出 DrawScope,提到 `remember(metric.key){ 计算 p10/p50/p90 + min/max 缩放 }` 只算一次复用**（曲线仅依赖 metric.key,与滚动无关；数据点 points 已 remember,照此对齐）。
3. 通用：首页重派生值一律 `remember` 按输入 key 缓存;composable 体内勿 new Brush/对象。

验收补充：装机首页上下滑应顺滑（无明显掉帧）;FGR 面板在屏滚动不卡。

### P5 交互收敛+性能 — 通过（commit `45ee224`），但根因B遗留

**结论**：架构/交互/性能A 通过;**根因B 未做,须立即补一笔 `perf:` 独立提交**（先于 P5后队列 Q1）。

**核实通过**：① 旧 FAB/`QuickActionDialog`/隐藏长按手势清除;`PersistentQuickRail` 两阶段常驻 + 显式"按住说话/文字录入"语音条(修 P1 可发现性);底栏中央语音仅显式打开。② 根因A：全屏 `Brush.verticalGradient` 已删,恢复扁平(性能+视觉双兑现);残留 1855 `linearGradient` 仅小卡片背景,非滚动容器,无碍。③ CMA 3140→2921;仅 CMA+QuickRail,逻辑/数据零改;单 commit 干净;装机回归通过。④ NavCompose P0–P5 闭环,CMA 4331→2921。

**遗留（必须补，记过程问题）**：根因B（`FetalGrowthChart.kt` p10/p50/p90 仍在 `FetalGrowthCanvas` 的 `Canvas{}` DrawScope 内每帧重算）**未做**——该项在文档「P5 追加」已明确并入 P5,Codex 只做 A、漏 B 且未声明。**要求：单独一笔 `perf: FGR 参考曲线移出 DrawScope 提到 remember(metric.key)`，先于 Q 队列执行。** 流程纪律：并入范围的项若不做须显式说明,不得静默丢弃。

### perf-B FGR remember 化 — 通过（commit `afbca2d`）

**结论**：通过。**NavCompose P0–P5 + 性能根因 A/B 全部闭环。**

**核实**：新增 `FetalGrowthReferenceSeries`;p10/p50/p90 计算移入 `remember(metric.key){…}`,位于 `Canvas(` 之前;DrawScope 仅读缓存,不再每帧重算 BCCG。单一 `perf:` commit、仅 FetalGrowthChart.kt、未混、树干净。装机 gfxinfo 50/90=8ms/16ms（≤16.67ms 帧预算,顺滑）;assembleDebug+lint+smoke 绿(CI 兜底)。

**收口**：架构重构线结束。后续转入 `docs/P5后工作队列-给Codex.md`：perf-B 已清 → Q1 首启免责门 → Q2 记录可编辑 → Q2b 产检结构化 → Q4 → Q6（Q3 已完成,Q5 等设备）。

### P5 收尾修正：底部收敛——语音并入 quick rail（用户裁定，先于 Q1）

**问题**：P5 后底部在首页堆三带（`PersistentQuickRail` + `VoiceEntryRail` + `BottomNav`，≈200dp+ 永久占屏），且 `VoiceEntryRail.onTextEntry` 与 `BottomNav(onSmartEntryClick)` 语音入口**重复**；`VoiceEntryRail` 还在非首页 tab 也常驻。P5 交互收敛用力过猛、反增带与重复。

**修法（用户选：语音并进 quick rail）**——一笔独立 `ui:` 提交，**先于 Q1**：
1. **删除独立 `VoiceEntryRail` 整条带**（移除 `ComposeMainActivity` bottomBar Column 内其调用 1481–1486 及 `QuickRail.kt` 中该 composable）。
2. **在 `PersistentQuickRail` 内加一个显式麦克风磁贴**："按住说话"（hold → `onSmartVoiceHoldStart/End`；tap → `onSmartEntryClick` 文字/智能录入）。视觉用 Primary 区分、置于 rail 首位，保证可发现；**把原 `VoiceEntryRail` 的录音态反馈（识别中/松开结束等 `smartVoiceState` 提示）移入该磁贴**，不丢状态可见性。
3. **`BottomNav` 去掉语音/智能录入入口**（移除其 `onSmartEntryClick` 中央按钮/参数），底栏回归纯 4 tab（Home/Timeline/Library/Settings），勿破坏导航。
4. quick rail（含麦克风磁贴）**仅首页**（沿用现有 `activeTab==Home` 门控）；非首页仅 `BottomNav`。
5. **结果**：首页底部 3 带→2 带（quick rail 含麦克风 + 底栏）；其他 tab 仅底栏；语音零重复且可发现；baby 阶段经同一 `PersistentQuickRail` 一致获得麦克风磁贴。
6. 内容区 bottom contentPadding 随新（更矮）chrome 高度更新，确保不被遮挡。

**硬约束**：只迁移语音入口的 UI 载体；按住录音/权限流/转写/人工确认链/降级逻辑（CMA 内 `startSmartVoiceRecording` 等）**逻辑不动**;不动数据/FGR/STT 接线/阶段投影/已锁视觉 token;一笔 `ui:` commit不混不并 main;assembleDebug+lint+smoke 绿;装机：首页 quick rail 含麦克风、按住转写全链、点击进文字录入、底栏纯 4 tab、非首页无语音带、可视区变大。

**验收**：3 带→2 带；语音入口唯一且可发现；按住说话全链不回退；非首页无语音带；导航 4 tab 正常。

#### 截图实测补强（`diagnostics/now-home-bottom.png`，Claude 装机核对）

实测比预估更重，spec 强化如下（仍属上条同一笔 `ui:` 修正）：
- 底部 chrome 实占**约 40% 屏幕**；孕期摘要被压到半屏且宫格截断；quick rail 第 5 项（孕妈指标）已被挤出不可见 → 横向需可滚或自适应，5 项都要可达。
- 语音入口实为**三重**：① `VoiceEntryRail`「按住说话」带 ② 同带「文字录入」按钮 ③ **`BottomNav` 中央一个 FAB 式凸起大麦克风圆钮**。三者紧邻堆叠。
- 明确：第 3 步"BottomNav 去语音入口" = **移除该中央大麦克风 FAB / 中央动作槽位，底栏回归标准等分 4 tab，无凸起中央位**。合并后语音仅剩 quick rail 内麦克风磁贴一处。
- 收敛后自检：底部 chrome 占屏显著下降、孕期摘要不再被截断、quick rail 5 项可达、语音仅 1 处入口。
