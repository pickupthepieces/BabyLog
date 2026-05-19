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

#### 附带修正：MetricCard 文字底部被裁（Claude 代码定位，可并入同轮 ui: 提交）

**问题**：用户报"首页很多文字下部被遮挡"。代码定位 = `ui/components/BabyLogComponents.kt` 的 `MetricCard`：`.padding(11.dp).height(76.dp)` 固定高 + `.clip(...)`，可用内容高仅 54dp，而 色条3 + Spacer7 + title(11sp) + value(20sp) + subtitle(10sp) ≈ 64dp+，超出被裁；系统字号 >100% 时更严重。孕期摘要/今日摘要全是 `MetricCard`，故"很多文字"。`TrendCard`（无固定高）不受影响，反证此点。

**修法**：`MetricCard` 的 `.height(76.dp)` → `.heightIn(min = 76.dp)`（内容/字号变大时自适应增高、不裁；行内各格 weight 仍对齐到最高）。不动配色/结构/`subtitle` 的 ellipsis。小改低风险。

**约束/验收**：仅此一处高度约束改动；首页摘要文字（尤其 subtitle）不再被裁、大字号下也不裁；行内宫格高度对齐一致；assembleDebug+lint+smoke 绿；可并入"底部收敛"同一笔 `ui:` 提交（同属首页视觉修正）或紧随其后单提交，二者择一勿混入功能轮。

### P5 收尾修正 II：quick rail 压紧+滚动自动隐藏 ＋ 补做 MetricCard 裁切（用户裁定）

`eec488b` 底部收敛已通过（3 带→2 带，占屏 ~40%→~22%，VoiceEntryRail 删净、BottomNav 纯 4 tab、麦克风磁贴并入 rail）。本节为继续收尾，一笔 `ui:` 提交（两子项同属首页底部视觉/交互，可同提交，勿混功能/Q 队列）：

**A. 🔴 补做 MetricCard 文字裁切（已被静默漏两次，本次硬性必做）**
- `ui/components/BabyLogComponents.kt:78` 仍为 `.height(76.dp)` → 改 `.heightIn(min = 76.dp)`。固定高把摘要宫格底部文字裁掉（大字号更甚），用户最早即报"文字下部被遮挡"。不动配色/结构/ellipsis。**Claude review 会专门查 line 78；scoped 项若决定不做必须显式说明，不得再静默丢弃（perf-B、本项已两次）。**

**B. quick rail 压紧 + 滚动自动隐藏（用户选）**
- **压紧**：rail 变矮——磁贴缩小/单行/弱化文字标签，目标高度降约 30–40%；麦克风磁贴仍居首，5 个动作仍可达（不够宽则横向可滚）。
- **滚动自动隐藏**：绑定首页 LazyColumn 滚动——内容下滑（阅读）时 rail 平滑滑出隐藏；上滑 / 停止 / 列表顶部时滑回。用 nestedScroll/滚动增量 + 动画偏移（如 `AnimatedVisibility`/`animateDpAsState`）。
  - **底栏 BottomNav 不随之隐藏**（全局导航需常驻）；只隐藏 quick rail 这一条。
  - 内容区 bottom contentPadding 跟随 rail 显隐调整，保证任何状态都不永久遮挡、也不跳动。
  - rail 隐藏时麦克风/语音不可触发属正常；上滑回来即可用。仅首页;非首页本就只有底栏。
- 不改动作集/录音逻辑/人工确认链/阶段投影/已锁视觉 token；逻辑只调显隐与尺寸。

**验收**：line 78 已 `heightIn`、摘要文字大字号下不裁；rail 明显变矮；下滑隐藏、上滑/置顶回现、过渡平滑无跳；底栏始终在；一拍即记 + 按住说话在 rail 可见时正常；assembleDebug+lint+smoke 绿；装机回归。一笔 `ui:` 提交不混不并 main。

### P5 收尾修正 III（最终决定，取代 II 中"麦克风并入 rail"部分）

用户复议语音入口位置；Claude 评估认同——**语音/智能录入入口回归 BottomNav 正中按钮**，理由：① 底栏正中是拇指热区、最符合主操作人机直觉；② quick rail 将"滚动自动隐藏"，语音若在 rail 内会随滚动消失，而语音/速记需随时可触发，**必须常驻**；底栏永不隐藏 → 放底栏正中最稳；③ 单一入口、零重复（VoiceEntryRail 已删）。

**最终底部形态（收敛终态，本批一笔 `ui:` 提交完成）**：

- **A.（硬性，已被静默漏两次，必做）** `ui/components/BabyLogComponents.kt:78` `.height(76.dp)` → `.heightIn(min = 76.dp)`，修摘要宫格文字底部被裁。Claude review 专查 line 78；scoped 项不做须显式说明，不得再静默丢。
- **C. 语音入口 = BottomNav 正中按钮**：BottomNav 由"纯 4 tab"改为「4 tab + 正中一个语音/智能录入按钮」（视觉可略突出，但不必做成超大 FAB；点按=进智能录入页，长按=按住录音，与现有 `onSmartEntryClick`/录音逻辑对接）。**录音/权限/转写/人工确认/降级逻辑只接线、不改**。
- **B. quick rail = 纯快捷记录 + 压紧 + 滚动自动隐藏**：**移除 rail 内麦克风磁贴**（语音已移至底栏正中）；rail 只剩 B超/产检/胎动/宫缩/孕妈指标；压紧降高约 30–40%；绑首页 LazyColumn，下滑隐藏、上滑/停/置顶滑回；**BottomNav（含正中语音）始终常驻不随之隐藏**；内容 bottom padding 跟随 rail 显隐调整，不永久遮挡不跳动；仅首页。
- 不改动作集/阶段投影/人工确认链/已锁视觉 token；逻辑只调显隐尺寸与语音入口位置接线。

**取代关系**：本节 C+B 取代「P5 收尾修正 II」中"麦克风并入 rail"的做法；II 的 A（MetricCard）并入本节 A 不变。以本节为准。

**验收**：line 78 已 heightIn 且大字号不裁；BottomNav 正中有语音按钮、点按进智能录入、长按录音全链正常、底栏常驻；rail 无麦克风、明显变矮、下滑隐上滑现过渡平滑；一拍即记正常；assembleDebug+lint+smoke 绿；装机回归。一笔 `ui:` 提交不混不并 main。

### P5 收尾修正 III — 通过（commit `41a830d`，终态达成）

**结论**：通过。首页底部 UI 收敛终态达成；放行 Q 队列（Q1 起）。

**核实（git+grep+一次真机截图 `diagnostics/p5-final-home.png`）**：A `BabyLogComponents.kt:79 .heightIn(min=76.dp)`，截图摘要宫格文字不再被裁。B QuickRail 去麦克风（-140 行）、纯快捷 5 项可见且变矮；HomeScreen `rememberLazyListState`+`nestedScroll`+atTop/isScrollInProgress 驱动 rail 滚动显隐。C BottomNav = 4 NavItem + 正中语音（点按 `onSmartEntryClick`、长按 `onVoiceHoldStart/End`），截图见正中大圆麦克风钮，底栏常驻、无 VoiceEntryRail、零重复。仅 4 UI 文件、逻辑/数据/STT 接线零动、单 commit、树干净；assemble+lint+smoke 绿（CI 兜底）+ Codex 装机回归。

**收口**：NavCompose 架构线 + P5 全部收尾（I~III + perf-A/B）通过。后续严格按 `docs/P5后工作队列-给Codex.md`：Q1 首启免责门 → Q2 记录可编辑 → Q2b 产检结构化 → Q4 → Q6（Q3 已完成，Q5 待设备）。

### P5 收尾修正 IV：quick rail 缩小贴 Piyo（用户反馈偏大，先于 Q1）

对照 Piyo（`diagnostics/app-compare/piyolog-main-screen.png`）：其快捷为轻量小圆图标、无边框盒、更小更密。当前 `PersistentQuickRail`（QuickRail.kt）每格是 76dp 宽 + 1px 边框 + 圆角卡盒，偏大偏重。一笔独立 `ui:` 提交，**先于 Q1**：

| 参数 | 现 | 改为 |
|---|---|---|
| 每格 `.width(76.dp)` | 76 | ~58–60dp（或去固定宽，内容自适应/均分） |
| 图标 tile `.size(34.dp)` | 34 | ~26–28dp |
| `iconSize = 22.dp` | 22 | ~18dp |
| 每格 `.border(1.dp, …)` | 有边框 | **去掉边框**（仅保留轻色 tint 背景；盒子重感主因） |
| 格内 `.padding(vertical = 7.dp)` | 7 | 5dp |
| row `padding(… vertical = 6.dp)` | 6 | 4dp |
| `Arrangement.spacedBy(8.dp)` | 8 | 6dp |
| 标签 `fontSize = 11.sp` | 11 | 10sp |

要求：仅 `QuickRail.kt` 视觉微调；5 项仍全可见（变窄后更易容纳）、整体明显变矮变轻、接近 Piyo 紧凑感；保证可点（tile≈28 + 标签 + padding 总高仍达可点）；不改动作集/逻辑/滚动显隐/底栏/已锁配色 token。一笔 `ui:` 提交不混不并 main；assemble+lint+smoke 绿；装机对比 Piyo 观感。Claude review 用一次真机截图与 Piyo 比对验收。

### P5 收尾修正 IV — 通过（commit `1d036cc`）

**结论**：通过。首页底部 UI 至此为最终终态。

**核实**：值全部按 spec（width 76→58、tile 34→28、icon 22→18、去 border、item pad 7→5、row pad 6→4、spacing 8→6、label 11→10sp）；仅 `QuickRail.kt` 单 commit、树干净、逻辑/滚动显隐/底栏/配色 token 未动。真机截图 `diagnostics/p5-quickrail-iv/home.png` 对比 Piyo：rail 明显变轻、无边框盒、5 项全可见可点、贴近 Piyo 紧凑小图标；语音仍仅底栏正中。assemble+lint+smoke 绿 + Codex 装机回归。

**收口确认**：NavCompose 架构线 + P5 全部收尾（I~IV + perf-A/B）通过，首页底部终态固定，不再迭代。后续严格走 `docs/P5后工作队列-给Codex.md`：Q1 → Q2 → Q2b → Q4 → Q6（Q3 已完成，Q5 待设备）。

### P5 收尾修正 V：quick rail 去双层背景盒，改 Piyo 圆形图标+文字（用户反馈仍重）

根因：当前每格有**两层 tint 背景方块**——外层 `Column .clip(RoundedCornerShape(15)).background(toneColor α0.16)` + 内层 `BabyLogIconTile` 自身 `Box .clip(RoundedCornerShape(16)).background(tileColor)`。Piyo 为「一个圆形图标 + 下方文字」，零背景方块。一笔 `ui:` 提交（QuickRail.kt 一处；**禁止改共享 `BabyLogIconTile`**，其被底栏等复用，改之会连累别处）：

1. 去掉外层 `Column` 的 `.clip(...).background(...)`（那张卡片方块）；Column 仅保留 `.clickable` + `.padding(vertical=4.dp)` + 居中，背景透明。
2. 该格内**不再调用 `BabyLogIconTile`**；内联一个圆形：`Box(Modifier.size(≈40.dp).clip(CircleShape).background(Color(action.toneColor).copy(alpha=0.16f)), contentAlignment=Center){ BabyLogMaterialIcon(icon = quickActionIcon(action.eventType), tint = Color(action.toneColor), modifier = Modifier.size(≈20.dp)) }`。
3. `Spacer(3–4.dp)` + 标签 `fontSize = 10.sp` 不变；每格宽可保持 ~56–58dp 或随内容。

要求：仅 `QuickRail.kt`；不动共享组件/动作集/逻辑/滚动显隐/底栏/配色 token；结果为单层柔色圆形图标+文字、无任何方块、贴 Piyo；5 项全可见可点（圆 40 + padding 达可点）。一笔 `ui:` 不混不并 main；assemble+lint+smoke 绿；装机对比 Piyo。Claude 用一次真机截图与 Piyo 比对验收。

### perf-C：广泛滑动卡顿（用户报，Claude 代码定位，一笔独立 perf: 不混 Q2b）

用户反馈"各种界面滑动都卡卡顿顿"。代码定位两系统性源：

**F1（P5 引入回归 · 首页）**：`ui/screens/HomeScreen.kt` `railNestedScroll.onPreScroll` 每个滚动增量即调 `onQuickRailVisibilityChange(false/true)`；`quickRailVisible` 为 `BabyLogApp`/Scaffold 作用域 `rememberSaveable`（CMA ~1514，bottomBar 读取）。滚动中（±6f 抖动）反复翻转 → 整个 Scaffold 子树（含当前 NavHost 页）每帧重组。
- 修：rail 显隐用 `snapshotFlow{ 方向/累积阈值 }.distinctUntilChanged()`，**只在真正 show↔hide 跳变时发一次**；`onPreScroll` 不再每帧直接 set state；滚动过程中 `quickRailVisible` 不被每帧写。

**F2（系统性 · 所有列表页：时间线/首页最近/回收站）**：`ComposeMainActivity.TimelineRow`（~2228）每次重组跑 `BabyLogFormatters.formatEventDay/formatEventTime/eventLabel/eventSummary`（SimpleDateFormat + payload JSON 解析），且行 `Card elevation = 2.dp + border`（逐行阴影）。快滑时 N 行组合 + N 阴影绘制 → fling 卡。
- 修：① `TimelineRow` 派生串提到 `remember(event.id, event.occurredAt, event.updatedAt){ … }` 只算一次；② 行 `Card` `elevation 2.dp → 0`（与 Panel/TrendCard 一致，靠 border/高亮分隔）。

**约束/验收**：一笔 `perf:` 提交，不混 Q2b/冻结 UI、不并 main；不改数据/逻辑/确认链/阶段投影/已锁配色 token（仅 elevation 与计算时机）；assemble+lint+smoke 绿；装机 gfxinfo 首页与时间线 fling 50/90 分位 ≤ ~16ms、无明显掉帧。Claude 用一次真机 gfxinfo/截图验收（perf 关口）。

### perf-D：编辑 B 超表单页卡顿（用户精确定位，独立 perf:，不混 Q2b/perf-C）

用户："主要是编辑 B 超记录这个页面卡"。Claude 代码核查：`UltrasoundFormScreen` 的 Hadlock 估算(line 100 `remember(bpd,ac,fl,efw)`)与 warnings(line 90 `remember`)、`LaunchedEffect` 均已正确 remember/keyed — **非重算问题**。

**真因（结构性 Compose 模式）**：~28 个字段 state 全 hoist 在屏 composable 作用域；`RecordFormScaffold(content = { LazyListScope })` 尾随 lambda 每次重组为新实例。任一 TextField 打字 → 整个 `UltrasoundFormScreen` 重组 → content lambda 重建 → LazyColumn 当前可见的 6–10 个 Material `TextField` 全部重组。该页字段最多最重，故打字/滚动卡最明显（轻表单字段少无感）。

**修（分两步，先便宜后结构）**：
1. 给 `UltrasoundFormScreen` 内每个 `item { … }` 加**稳定 `key`**（如 `item(key="bpd"){}`），让 LazyColumn 跳过未变 item。先做并装机量。
2. 若仍卡：把表单 state 收进 `remember` 的**状态持有者**（如 `class UltrasoundFormState` 持各字段 state），使单字段编辑只重组对应 item，不再整屏重组 + 重建 content lambda。
3. 同型重表单（MaternalMetric/PregnancyEvent）若同样卡，按同法（item key 优先）；轻表单可不动。

**约束/验收**：独立 `perf:` 提交，不混 Q2b/perf-C/冻结 UI、不并 main；**不改字段集/校验/人工确认链/保存逻辑/编辑(Q2)行为/数据**，仅改组合结构与 key；assemble+lint+smoke 绿；装机 gfxinfo 编辑 B 超页打字+滚动 50/90 ≤ ~16ms、无明显掉帧。Claude perf 关口用一次真机 gfxinfo 验收。

#### perf-D 根因升级（Claude 深挖；加 key 不够，需三步）

Codex 反馈定位不到/加 key 未解。Claude 二次核查确诊：

- **真因**：`UltrasoundFormScreen` 单巨型 composable 持 ~28 字段 state；每次按键→整屏重组→传给 `RecordFormScaffold` 的 `content: LazyListScope.()->Unit` 为**新 lambda 实例**，且每个 `item{ UnitInputRow(label, value, { x = it }, unit) }` 的 **onValueChange 内联新 lambda 每帧新建** → LazyColumn 重组、**可见的每个 Material TextField 全部重组**（Material TextField 重组贵；`showAdvanced` 展开后可见 6–10 个）。字段最多故最卡。
- **排除项（别再追）**：`SmartEntryDraft.nonce = System.nanoTime()` 是 `rememberSaveable` 的合法重置 key，编辑态只设一次、稳定，**非卡因**。Hadlock/warnings 已 remember，非卡因。
- **加 item key 是必要但不充分**：有 key 后 LazyColumn 复用槽位，但 item 的 content/onValueChange 仍是新 lambda + 读屏作用域 state → 该 item 仍重组。这就是 Codex 加 key 后没改善的原因。

**根治三步（缺一不可）**：
1. 每 `item` 稳定 `key`（`item(key="bpd")`…）。
2. **回调 stable 化**：onValueChange 不内联 `{x=it}`；改用状态持有者方法引用或 `remember` 的稳定 lambda，使每帧同一实例。
3. **`remember` 的表单状态持有者**（如 `class UltrasoundFormState` 持各字段 `mutableStateOf`），每字段行拆**独立小 composable 只读自身 state**；改 A 字段时 Compose 跳过 B 行，**只重组被编辑那一行**。

三者一起才根治。约束/验收同 perf-D：独立 `perf:`，不改字段集/校验/人工确认链/保存/Q2 编辑行为/数据；assemble+lint+smoke 绿；装机 gfxinfo 编辑 B 超页打字+滚动 50/90 ≤~16ms。MaternalMetric/PregnancyEvent 重表单同法。

#### perf-D 确诊（Claude + 真机 gfxinfo，铁证）

**真机 gfxinfo（编辑 B 超页交互后）**：Janky 48.6%、90th 27ms、**Number Slow UI thread = 188 = 全部卡帧**；GPU 50/90/99 = 3/4/5ms。→ 100% 主线程（组合+measure），非绘制。

**根因（确诊）**：Codex 做 perf-D 时把 `UltrasoundFormScreen` 容器从懒加载 `RecordFormScaffold`(LazyColumn) **换成了非懒 `RecordFormColumnScaffold`(`Column.verticalScroll`，RecordFormScaffold.kt:114-118)**。`Column+verticalScroll` 一次性组合并 measure 全部 ~28 个 Material TextField（verticalScroll 须量全部子项算滚动范围）→ 每帧主线程 20-30ms。其余 3 表单仍用懒 `RecordFormScaffold` 故不卡——正对"只有 B 超编辑页卡"。状态持有者/方法引用/derivedStateOf 子 composable 隔离都对、要保留，但被非懒容器整体抵消。

**精确修（高置信，独立 `perf:` 或并入 5254097 的修正提交）**：
1. `UltrasoundFormScreen` **改回懒 `RecordFormScaffold`(LazyColumn)**；每字段/分区 `item(key="bpd"){}`…（保留 state holder + 方法引用回调 + `UltrasoundEfwInput`/`UltrasoundSoftWarnings` 子 composable）。
2. 评估 `RecordFormColumnScaffold` 是否还有别处用；本页不再用它。若无人用可后续清理（非本笔必须）。
3. 同理：任何重表单都不得用 `Column+verticalScroll`，统一走 LazyColumn 版。

**验收**：编辑 B 超页 gfxinfo 重测——Slow UI thread 大幅下降、90th ≤ ~16ms、Janky 显著下降；字段集/校验/人工确认链/Q2 编辑/数据不变；assemble+lint+smoke 绿。Claude 复测一次真机 gfxinfo 验收。

#### perf-D 修正：高刷(120/144Hz)适配 + 验收标准更正（用户要求适配高刷）

测试机实为 **144Hz**（`dumpsys display` refreshRate≈144），帧预算 **6.94ms**（120Hz=8.33ms），非 60Hz 的 16.67ms。故先前"90th ≤ ~16ms"验收**作废**。

**澄清（别追不存在的开关）**：Android 本就按屏幕刷新率渲染。"高刷适配"实质 = ① App 不得把刷新率钉死 60；② 每帧主线程工作量压进高刷预算内。对本重表单，杠杆仍是 perf-D 确诊的修复（懒加载容器 + 状态持有者），不是某设置项。

**本轮要做**：
1. **保留高置信修复**：撤回为追数值做的激进实验；`UltrasoundFormScreen` 改回懒 `RecordFormScaffold`(LazyColumn) + 分组 `item(key=…)` + 已做的 state holder/方法引用/derivedStateOf 子 composable。
2. **不钉 60Hz**：排查 App 是否有 `preferredRefreshRate`/`preferredDisplayModeId`/`Surface.setFrameRate`/窗口属性把帧率限 60；若有则去除，让系统用原生 120/144；无则说明确认。
3. **验收按原生刷新率衡量**（不再固定 16ms）：测试机 144Hz 下，编辑 B 超页打字+滚动 **Janky% 显著下降**、`Number Slow UI thread` 大降；百分位对 144Hz 预算（~6.9ms）尽量靠拢，**至少不再是 48% jank/90th 27ms 量级**；若 Material TextField 在 144Hz 仍超预算，记录数据、评估是否需更轻输入控件（不在本笔强求，先 LazyColumn 复测）。

**约束**：独立 `perf:`；字段集/校验/人工确认链/Q2 编辑/数据不变；assemble+lint+smoke 绿。Claude 复测真机 gfxinfo（按 144Hz 解读：看 Janky% 与 Slow UI thread 降幅，不用 16ms 这把尺）。

#### perf-D 终局：B 超录入「照片/OCR 优先 + 极少核心字段」重构（用户拍板，取代单纯 perf-D 实验）

调研结论：28 字段是临床 EMR 形态，与"家庭自用、非医疗器械"定位错位；消费级孕期 App 普遍是"拍报告 + 少量核心指标 + 医生结论"，不让孕妇手敲 28 个数。**此重构同时根解 UX + 性能(可见输入骤减→LazyColumn measure 轻→144Hz 不卡) + 定位一致**。一笔独立提交（`refactor:` 或 `feat:`，非纯 perf；不混 Q2b/screening_*、不并 main）。

**做法**：
1. **主路径=拍 B 超单**：照片/OCR 入口前置、显著（复用现有 OCR：拍/选图→候选→人工确认）；手敲为 fallback。
2. **默认只露核心 ≈8 项**：检查日期、孕周、BPD、HC、AC、FL、EFW、医生结论/提示（FGR/曲线所需 + 家庭真正关心）。
3. **其余全部默认折叠进「更多医学指标 ▾」**：AFI/最大羊水池/胎盘位置·分级/胎位/脐血流 S·D·PI·RI/宫颈管/CRL/NT/胎心率/胎数/胎动/脐带插入处/报告时间/医院·机构/超声诊断长文本 —— 字段都保留可填可存，仅默认不可见。
4. **容器用懒 `RecordFormScaffold`(LazyColumn)**（保留 perf-D 的容器修复 + Codex 已做的 state holder/方法引用/derivedStateOf 子 composable，复用别推翻）。
5. **编辑态(Q2)**：若既有记录已填了高级字段，进入编辑须**自动展开**高级区，不得静默隐藏已有数据。

**取代关系**：本节取代"单纯把 Ultrasound 改回 LazyColumn 就收工"的 perf-D 做法——容器仍要懒，但**根解是默认字段精简**；做完本重构 perf-D 即闭。

**MUST 不变**：所有字段仍存在/可填/可存、FGR/Hadlock 接线、人工确认保存、Q2 编辑、Q2b 产检独立、OCR 候选流、校验、数据/事件模型不变。仅表单信息架构与默认可见性 + 照片/OCR 前置。

**验收**：默认表单 ≈8 核心输入 + 照片/OCR 显著；高级区默认折叠、编辑有数据时自动展开；字段/校验/确认链/数据零改；容器 LazyColumn；真机 144Hz gfxinfo：Janky% 低、`Slow UI thread` 大降（性能随之解决）；assemble+lint+smoke 绿；装机走查（拍图+OCR 新建、手填核心、展开高级、编辑含高级数据的旧记录）。Claude 复测真机 gfxinfo（144Hz 解读）。同型 MaternalMetric/PregnancyEvent 若同样重可后续同法，本轮聚焦 B 超。

##### 数据完整性硬保证（回应"折叠后数据还能全面体现吗"）

折叠=默认可见性，**非删字段**。本重构必须保证数据不打折：
1. **存储/事件模型零改**：28 字段全留 payload，可填可存；时间线摘要/成长曲线/FGR 读 payload，与表单可见性无关，**不回归**。
2. **OCR 字段映射不得收窄**：OCR 仍须能识别并预填**全部字段（含高级：AFI/胎盘/脐血流/CRL/NT…）**；识别到高级字段有值时**自动展开高级区**（与编辑态同逻辑），用户确认后入库。禁止借重构缩小 OCR 可识别集。
3. **报告照片=全保真兜底**：照片附件始终保留，即便用户不逐项转录，原始报告也在。
4. **编辑态**：既有记录含高级数据→进入编辑自动展开，不静默隐藏。
**验收追加**：用一张含高级指标（AFI/胎盘/脐血流等）的 B 超单走 OCR，确认高级字段被识别、自动展开、可确认入库；既有含高级数据记录编辑时高级区自动展开且值不丢。

#### perf-D 终局 / B超照片优先重构 — 通过（commit `b931e50`）

**结论**：通过。perf-D 与 B超重构闭环。

**核实（git+grep+Codex gfxinfo，未重跑真机）**：单文件 `UltrasoundFormScreen.kt`、单 `refactor:`、树干净；数据模型/Service/FGR/OCR-client 未动。gfxinfo(144Hz)：Slow UI thread 188→30、Janky 48.6%→11.7%、GPU 2-4ms——非懒 Column 根因解决（回 LazyColumn + 默认字段精简）。数据完整性：OCR 候选映射全字段且识别到高级值 `showAdvanced=true` 自动展开；`fromValues/toInput` round-trip、编辑自动展开高级；字段/模型零改——符合数据完整性硬保证。90th 23ms 残留少量重帧（OCR应用/展开时），可接受非阻塞。
