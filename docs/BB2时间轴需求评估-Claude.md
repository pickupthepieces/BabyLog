# BB2 · B0 24h 日视图时间轴需求评估（Claude）

| 项目 | 内容 |
|---|---|
| 作者 | Claude（Opus 4.7） |
| 日期 | 2026-05-26 |
| 范围 | B0 24h 日视图时间轴（Piyo 核心交互范式） |
| 目标 | 让 baby 期首页从"日期 + 事件 list"升级为"日期 + 24h 时间轴可视化"，看清娃一天的节律 |
| 关联 | `docs/育儿期App功能缺口调研-Claude.md` v3 / `docs/Piyo对标差异与BabyLog产品方向.md` / BB1 已落地的 B0' 日摘要 |

## 0. 红线（先看）

- 不动加密协议 / 同步链路（B0 仅 UI 改动，零 service 业务改动）
- 不动 FGR / STT / Disclaimer / launcher / splash / 已锁视觉 token
- 不预设医学建议、不写"异常 / 严重 / 危险"等词
- 不做"是否异常"自动判读
- 不破坏现有 baby 事件 payload（仅消费，不改）
- 留在 `codex/stage-mainline-refactor` 分支
- 每笔后 `:app:assembleDebug` + `:app:lintDebug` + `:app:detekt` + 全量 JVM smoke 必过

### 0.1 闸值警示（**🔴 关键**）

| 文件 | 当前 / 闸值 | 余量 | 限制 |
|---|---|---|---|
| ComposeMainActivity.kt | 4251 / 4500 | 249 行 | B0 主要 UI 独立文件，CMA wiring 控制在 +30 行内 |
| **BabyLogService.java** | **2199 / 2200** | **1 行** | **绝不允许在 Service 加任何方法**。如需聚合/分组 helper（如 hourly grouping），**必须**放到独立 `BabyLogBabyDayTimelineGrouper.java` 类似 `BabyLogDailySummaryCalculator` 模式 |
| BabyLogRepository.java | 533 / 700 | 167 行 | OK，但本任务也不该动 |
| BabyLogDomain.java | 921 / 1000 | 79 行 | 本任务不动 |

**Service 余 1 行**是硬约束。任何 Service.java 改动撞顶必撞 detekt 红线。本任务设计上**完全不需要改 Service**——B0 是消费侧 UI，service 端 entity model 已就绪。

## 1. 设计目标

让用户在 baby 期首页**一眼看清娃一天的节律**：

- 喂奶频率（密集 / 稀疏）
- 睡眠段（连续 / 碎片化、白觉 / 夜觉）
- 尿布次数和分布
- 体温 / 用药 / 里程碑等其它事件的时刻标记

vs 当前 list 视图：list 只能看到"按时间倒序排列的事件行"，没有时段密度可视化。

## 2. UI 设计

### 2.1 整体布局

baby 期首页（HomeScreen baby 分支）现状自上而下：

```
[BabyDayCard]                日期切换
[DailyBabySummaryCard]       B0' 日摘要（BB1 已落地）
[BabyDaySummary]             当日 N 条卡（既有）
[SectionHeader "今天记录"]
[TimelineRow] x N            事件 list（既有）
```

B0 改造后：

```
[BabyDayCard]                日期切换（不动）
[DailyBabySummaryCard]       B0' 日摘要（不动）
[BabyDayViewSwitcher]        ← 新增：segmented control "时间轴 / 列表"
[BabyDayTimeline] 或          ← 时间轴视图（B0 新增）
[TimelineRow x N]             或 列表（既有，切换显示）
```

`BabyDaySummary`（当日 N 条卡）作用与 B0' 摘要重叠，**可删**。下面 SectionHeader "今天记录" 也可改为 segmented control。

### 2.2 BabyDayTimeline 视觉规格

```
┌─────────────────────────────────────────────┐
│ [时间轴 ▮] [列表]                            │  ← segmented control
├─────────────────────────────────────────────┤
│  0:00 ┃                                      │  
│       ┃                                      │
│   3   ┃                                      │
│       ┃  ┃▓▓▓ 23:00→05:30 跨日睡眠延续段     │  ← 跨日 sleep 尾段
│   6   ┃                                      │
│       ┃  ●  06:15  喂奶 120 mL               │
│   9   ┃                                      │
│       ┃  ●  09:30  奶瓶 90 mL                │
│  12   ┃  ●  12:00  尿布                     │
│       ┃                                      │
│  15   ┃  ●  14:00  体温 37.4 ℃              │
│       ┃  ●  14:30  用药 布洛芬               │
│  18   ┃  ●  17:00  里程碑：会翻身             │
│       ┃                                      │
│  21   ┃                                      │
│       ┃                                      │
│  24:00┃  ┃▓▓▓ 23:00→次日 当日睡眠起始段     │  ← 跨日 sleep 起始段
└─────────────────────────────────────────────┘
```

#### 2.2.1 布局规则

- **总高度**：24 × 单位高度（建议 48dp/h，整轴 1152dp，外层 LazyColumn item 包住即可滚动）
- **左侧时刻标尺**：每小时一条横线，主标 0/6/12/18/24，次标其它小时
- **右侧事件区**：宽度填满父布局
- **事件钉位置**：`top = (occurredHour + occurredMinute/60) × 单位高度`
- **睡眠段**：从 `sleepStart` 到 `sleepEnd` 的矩形条，宽度比事件点宽（占整列宽度），半透明粉色
- **其它事件**：圆点 + 时间 label + 简短摘要（复用 `BabyLogFormatters.eventSummary` 但截断）

#### 2.2.2 颜色编码（复用 ChestnutPalette）

| eventType | 颜色 token | 形状 |
|---|---|---|
| `sleep` | `ChestnutPalette.VioletArgb`（30% alpha） | 矩形段 |
| `feed` / `bottle` / `breastfeed` | `ChestnutPalette.BlueArgb` | 圆点 |
| `diaper` / `pee` / `poop` | `ChestnutPalette.YellowArgb` | 圆点 |
| `temperature` | `ChestnutPalette.GreenArgb` | 圆点 |
| `medication` | `ChestnutPalette.PeachArgb` | 圆点 |
| `milestone` | `ChestnutPalette.RoseArgb` | 圆点（带星标）|
| `wake` | `ChestnutPalette.GreenArgb` | 圆点（小）|
| 其它 | `ChestnutPalette.Muted` | 圆点 |

不引入新颜色 token，全部复用现有。

#### 2.2.3 跨日 sleep 处理

**与 B0' 摘要按 sleepStart 日归属规则不同**：时间轴是"可视化时段"，需要看到节律。规则：

- 一段 `sleep` 事件，`sleepStart="2026-05-25T23:00:00.000+0800"` + `sleepEnd="2026-05-26T06:30:00.000+0800"`：
  - 在 **5/25 时间轴**上画 23:00 → 24:00 段（标 "跨日继续 →"）
  - 在 **5/26 时间轴**上画 0:00 → 06:30 段（标 "← 上一日跨日"）
- 未结束的 sleep（无 sleepEnd）：从 sleepStart 画到"当前时刻"或"当日 24:00"，标"睡眠中…"

新增 helper（**放独立文件**，**不进 Service.java**）：

`ui/components/BabyDayTimelineSlots.kt` 或 `BabyLogBabyDayTimelineSlots.java`（看 Codex 哪种风格更合适）：

```kotlin
// 把 events × dateInput 转成时间轴上的时段 / 点元素
internal fun computeTimelineSlots(
    events: List<BabyLogDomain.BabyLogEvent>,
    dateInput: String
): TimelineSlots
```

`TimelineSlots` 含：
- `sleepSegments`: List<{startMinuteOfDay, endMinuteOfDay, crossDayMarker?}>
- `eventPoints`: List<{minuteOfDay, eventType, summaryLabel, eventId}>

### 2.3 BabyDayViewSwitcher (segmented control)

简单实现：两个 `OutlinedButton` 水平排列，selected 状态填充背景：

```kotlin
@Composable
internal fun BabyDayViewSwitcher(
    mode: BabyDayViewMode,
    onModeChange: (BabyDayViewMode) -> Unit
)

enum class BabyDayViewMode { Timeline, List }
```

默认 `Timeline`。用户切到 `List` 时显示既有 TimelineRow list。

切换状态用 `rememberSaveable` 保留，跨配置变更不丢。**不**持久化到 SharedPreferences（每次冷启回 Timeline 默认）。

### 2.4 交互

- **点击事件圆点 / 睡眠段** → 跳 `RecordDetailScreen`（复用 BB1 之前已有的 `onOpenDetail`）
- **不**支持 inline 编辑 / 删除（保持时间轴干净，编辑入口走详情页）
- **不**实现"当前时刻"指示线（避免动效干扰）
- **不**实现拖拽创建事件
- **不**实现长按多选

### 2.5 空白日

当日无事件（且无跨日 sleep 延续段）：显示空白时间轴 + 中央 `EmptyPanel("这一天还没有记录")` 半透明覆盖。**不**降级回 list。

## 3. 文件改动

| 文件 | 改动类型 | 行数估计 |
|---|---|---|
| `ui/components/BabyDayTimeline.kt` | 新建 | 200-280 行 |
| `ui/components/BabyDayTimelineSlots.kt` 或 `.java` | 新建（slot 计算 helper） | 80-120 行 |
| `ui/components/BabyDayViewSwitcher.kt` | 新建 | 40-60 行 |
| `ui/screens/HomeScreen.kt` | 修改（接入 switcher + 切换显示） | +30 行 |
| `smoke-tests/BabyLogBabyDayTimelineSlotsSmokeTest.java` | 新建 | 80-120 行 |
| `BabyLogService.java` | **零改动** | 0 |
| `ComposeMainActivity.kt` | **零改动**（HomeScreen 内部 state） | 0 |

总体新增 ~500 行 / 修改 ~30 行。CMA / Service 不动。

## 4. 提交拆分

| # | commit message | 范围 |
|---|---|---|
| 1 | `feat: 增加时间轴 slot 计算 helper` | BabyDayTimelineSlots + smoke |
| 2 | `feat: 增加 BabyDayTimeline composable` | 时间轴绘制 UI（无 wiring）|
| 3 | `feat: 首页 baby 日视图增加时间轴 / 列表切换` | switcher + HomeScreen 接入 |

3 笔，4-7 天工期。

## 5. Smoke 验收

`BabyLogBabyDayTimelineSlotsSmokeTest`：

| Case | 输入 | 期望 |
|---|---|---|
| 1 | 空事件 | sleepSegments=[], eventPoints=[] |
| 2 | 单 feed 06:15 | 1 个 eventPoint at minute=375 |
| 3 | 完整 sleep 22:00→05:30（跨日）当日 5/25 | sleepSegments=[{1320→1440, crossDay=true}]，eventPoints=[] |
| 4 | 同上 sleep，当日 5/26 | sleepSegments=[{0→330, crossDayPrev=true}] |
| 5 | 未结束 sleep（无 sleepEnd） | sleepSegments=[{1320→1440, incomplete=true}]，注：是否补到 24:00 视实现决定 |
| 6 | 多事件混合 | 按 minuteOfDay 升序排列正确 |
| 7 | 当日有 ultrasound（非 baby 事件） | 不出现在 timeline（过滤 baby 分组）|

7 case 全过。UI 视图本身无 smoke（Compose UI test 不在本任务范围）。

## 6. 不在本轮范围

- B5 疫苗本 / B4 WHO 曲线 / B6 体检 / 其它 B 系列
- 时间轴上 inline 编辑 / 拖拽 / 多选
- "现在时刻"指示线动效
- 周 / 月视图
- 缩放（pinch zoom 改 hour 高度）
- 持久化 viewMode 偏好（每次冷启回 Timeline 默认）
- 跨午夜 sleep 在两日时间轴各画一段是设计意图，但**这与 B0' 摘要按 sleepStart 日归属规则不同**，**这是预期不一致**（时间轴 = 可视化时段；摘要 = 当日活动总和）。doc 里在 spec §2.2.3 已说明，**Codex 实现时不要试图让两者对齐**

## 7. 验收清单

- [ ] 3 笔 commit 各自独立，conventional commit message
- [ ] `BabyLogBabyDayTimelineSlots` 7 case smoke 全过
- [ ] 时间轴 UI 在 baby 期首页正确渲染
- [ ] segmented control "时间轴 / 列表" 切换工作
- [ ] 跨日 sleep 在两日时间轴正确显示尾 / 起始段
- [ ] 未结束 sleep 显示"睡眠中…" 标记
- [ ] 事件圆点点击跳 RecordDetailScreen
- [ ] 空白日显示 EmptyPanel
- [ ] **BabyLogService.java 零改动**
- [ ] **ComposeMainActivity.kt 改动 ≤ 30 行**（应为 0，但留缓冲）
- [ ] CMA / Service / Repository / Domain 闸值均未撞顶
- [ ] 每笔 commit 后 assembleDebug + lintDebug + detekt + JVM smoke 绿
- [ ] 留在 `codex/stage-mainline-refactor`

## 8. 设计决策汇总

| 项 | 选择 | 理由 |
|---|---|---|
| 列表 vs 时间轴 | 切换共存（segmented control）| 列表已成熟覆盖编辑/删除，保留备选 |
| 默认模式 | Timeline | Piyo 核心体验，给用户最直接的差异化感受 |
| 跨日 sleep 显示 | 两日各画一段 | 节律可视化要求 |
| 摘要规则一致性 | 不强求 B0' 和 B0 跨日规则一致 | 二者目的不同（活动总和 vs 时段可视化）|
| 时间轴上编辑 | 不做 | 简化交互，跳详情页 |
| "当前时刻"指示线 | 不做 | 避免动效干扰，用户能自己看时钟 |
| 持久化模式 | 不持久化 | 每天回 Timeline 默认，避免用户忘记切回 |
| Service 改动 | **零** | 闸值约束 + B0 是纯消费侧 |
