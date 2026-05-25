# BB1 首轮育儿工作流需求评估（Claude）

| 项目 | 内容 |
|---|---|
| 作者 | Claude（Opus 4.7） |
| 日期 | 2026-05-25 |
| 范围 | B1 喂养字段补充 + B2 睡眠时长 + B3 尿布字段补充 + **B0' 日摘要聚合** |
| 目标 | 让 BabyLog baby 期从"事件流水账"升级为类 Piyo 工作流的第一轮 |
| 关联 | `docs/育儿期App功能缺口调研-Claude.md` v3 / `docs/Piyo对标差异与BabyLog产品方向.md` |

## 0. 红线

- 不动加密协议 / 同步链路（S1-S5 entity-agnostic 自动覆盖新字段）
- 不动 FGR / STT / Disclaimer / launcher / splash / 已锁视觉 token
- 不预设医学建议、不写"异常 / 严重 / 危险"等词
- 不做"是否异常"自动判读（体温摘要只显示最高/最低值，不分类正常/发烧）
- 不破坏现有 baby 事件 payload 兼容（旧记录继续读出）
- 应用打开拉取摘要时**绝不**触发任何 SyncChange（防回路与 S5 一致）
- 留在 `codex/stage-mainline-refactor` 分支
- 每笔后 `:app:assembleDebug` + `:app:lintDebug` + `:app:detekt` + 全量 JVM smoke 必过
- detekt 闸值：Service 余 < 267 行，Repository 余 < 167 行，CMA 余 < 91 行，撞顶按 housekeeping spec §0.5 流程处理

## 1. B1 喂养字段补充

### 1.1 现状

`BabyLogService.buildBabyCarePayload` 中：

```java
"feed":      feedType / amountMl / note
"breastfeed": detail / note
"bottle":     detail / note
```

### 1.2 增加字段（新字段全部 optional，旧记录读取兼容）

| eventType | 新字段 | 类型 | 说明 |
|---|---|---|---|
| `feed` | `breastSide` | string | 取值：`L` / `R` / `BOTH` / `""`（空）。仅当 `feedType` 含母乳语义时有意义；UI 不强制 |
| `feed` | `solidFood` | string | 辅食食材描述（如 "米糊 / 香蕉泥"），仅当 `feedType="辅食"` 类有意义 |
| `breastfeed` | `leftMinutes` | number | 左侧时长 |
| `breastfeed` | `rightMinutes` | number | 右侧时长 |
| `bottle` | `amountMl` | number | 容量。**与 feed.amountMl 平级**用于摘要 |
| `bottle` | `brand` | string | 奶粉品牌（可空）|

**MUST**：
- 现有 `detail` / `note` 字段保留，不要删
- 新字段全部 `putNumberIfNotNull` / `putStringIfNotBlank` 写入，空值不进 payload
- `formatBabyCareSummary` 摘要文本中加新字段（左 12 分钟 / 右 8 分钟 / 120 mL / "美赞臣"）
- `hasBabyCareMinimumContent` 不变（最小内容判定不收紧）

### 1.3 UI 改动

`ComposeMainActivity.babyCareLabels` 现状只支持 `primary / secondary / tertiary / note` 4 字段。**不要扩成 7 字段**——会破坏现有所有 baby 表单布局。改法：

- `breastfeed` label：`primary` 改成"左侧时长（分钟）"、`secondary` 改成"右侧时长（分钟）"、`tertiary` 用作"备注"，**移除原 detail 概念**。`buildBabyCareInput.breastfeed` 把 primary/secondary 解析成 number 填 leftMinutes/rightMinutes。旧 detail 字段在编辑回填时如检测到字符串且无法解析则塞 note 一并显示，避免历史数据丢失。
- `bottle` label：`primary` 改成"奶量 mL"、`secondary` 改成"品牌"、`tertiary` 用作"备注"。同样兼容旧 detail。
- `feed` label：保持 primary（feedType）/ secondary（amountMl）/ tertiary 改为"侧别 L / R / BOTH 或辅食食材"/ note，**只多一项可选输入**，按 feedType 含义自适应。

### 1.4 编辑回填

`draftFromBabyCareEvent` 对 `breastfeed` / `bottle` / `feed`：

- 新字段存在 → 直接回填新结构
- 只有旧 `detail` 字段 → 把 `detail` 塞到 `secondary`（兼容显示），用户编辑保存后自然升级到新字段
- note 始终塞到 note

### 1.5 Smoke

`BabyLogServiceSmokeTest` 加：

- `recordBabyCareEvent(BabyCareInput.feed(...))` with breastSide → payload 含 `breastSide`
- `recordBabyCareEvent(breastfeed)` with leftMinutes=12 / rightMinutes=8 → payload 含两字段，无 detail
- 旧 detail-only `breastfeed` 记录（直接构造 BabyLogEvent）→ `draftFromBabyCareEvent` 不丢字段（验通过 BabyLogService 静态方法即可，不依赖 Compose）
- `formatBabyCareSummary` 输出含新字段

### 1.6 提交建议

一笔 `feat: 喂养事件字段补充（侧别 / 时长 / 奶量 / 品牌）`。

## 2. B2 睡眠时长自动算

### 2.1 现状

`sleep` payload 已有 `sleepStart` / `sleepEnd` / `sleepPlace` / `note`，但 UI 不显示时长。日摘要 B0' 必须能聚合"今日睡了 X 小时"，前置要求一个 helper 算时长。

### 2.2 不在 payload 增加字段

**MUST NOT** 在 payload 加 `sleepDurationMinutes` 字段。理由：

- 派生字段容易与源字段不一致
- payload 越小越好（同步成本）
- Service 运行时算一次开销极小

### 2.3 新增 Service helper

`BabyLogService` 加：

```java
public static OptionalInt sleepDurationMinutes(BabyLogDomain.BabyLogEvent event);
    // 仅 eventType="sleep" 有效
    // sleepStart 或 sleepEnd 任一空 → 空（表示"未结束段"）
    // sleepEnd < sleepStart → 加一天（跨午夜场景）
    // 返回分钟数
```

跨日策略（睡眠跨过午夜）：
- `sleepStart="2026-05-25T23:00:00.000+0800"` + `sleepEnd="2026-05-26T06:30:00.000+0800"` → 算 450 分钟（7.5 小时）
- 解析用 `BabyLogFormatters.cnFormat` (Asia/Shanghai)
- end < start 时自动 +24h（不假设跨日，仅按时间比较）

### 2.4 UI 改动

`BabyDaySummary` / `TimelineRow` / `RecordDetailScreen` 中 `sleep` 事件显示：
- 已结束：`"23:00–06:30 · 7 小时 30 分"`
- 未结束（仅有 sleepStart）：`"23:00 · 睡眠中…"`

工具方法 `BabyLogFormatters.formatSleepDurationLabel(minutes)` → `"7 小时 30 分"` / `"45 分钟"` / `"3 分钟"`。

### 2.5 Smoke

`BabyLogServiceSmokeTest` 加：

- `sleepDurationMinutes` 正常段
- 跨午夜段（sleepEnd 字典序 < sleepStart）
- 仅 sleepStart 段返回空 OptionalInt
- `formatSleepDurationLabel` 3 个边界

### 2.6 跨日归属

**`sleep` 段在日摘要里按 `sleepStart` 日归属**。即 5/25 23:00–5/26 06:30 的 7.5 小时算 5/25 摘要里。理由：

- "我这天活动总和"语义直观（"我 25 号晚睡了 7.5h"）
- 实现简单，按 `recordDay(sleepStart)` 分组即可
- B0' smoke 必须有跨日 case 验证

### 2.7 提交建议

一笔 `feat: 睡眠事件自动算时长 + UI 显示`。

## 3. B3 尿布字段补充

### 3.1 现状

```java
"diaper": diaperType / diaperDetail / note
"pee":    detail / note
"poop":   detail / note
```

### 3.2 增加字段

| eventType | 新字段 | 类型 | 说明 |
|---|---|---|---|
| `diaper` | `color` | string | 取值：`黄` / `绿` / `黑` / `红` / `白` / `其它` / `""` |
| `diaper` | `consistency` | string | 取值：`成型` / `软便` / `稀` / `水样` / `""` |
| `poop` | `color` | string | 同上（poop 比 pee 更需要颜色信息）|
| `poop` | `consistency` | string | 同上 |

**MUST**：
- 完全 optional，旧记录读取兼容
- **不做"颜色 = 异常" 等自动判读** —— 黑色 / 红色 / 白色都只是事实记录，不弹警告，不显示"建议就医"等文案
- 仅作复诊沟通辅助

### 3.3 UI 改动

- `diaper` 表单（已结构化，有 babyCareLabels）：`tertiary` 字段改成"颜色 / 性状（可选）"，用户输入自由文本，service 端用 startsWith 简单分类到上述枚举值之一（保留原文）
- `poop` 是 quick rail 类型，现在只有 `detail / note`。建议升级到走 BabyCareFormScreen（仿 diaper 的表单）：primary=性状、secondary=颜色、tertiary=备注。这会让 poop 从 "一拍即记" 变成 "需要表单确认"，**用户体验权衡**：
  - 选 A：保留 poop quick rail 一键，**不**做表单，仅 diaper 表单加字段
  - 选 B：poop 走表单（如 diaper）
- **我建议 A**：日常 quick rail 已经够用，B3 字段补充主要服务 diaper（家长记尿布时本来就会想颜色性状）。poop quick rail 保持简单。

### 3.4 Smoke

- diaper 表单填 color/consistency → payload 含
- 旧 diaper 记录（无新字段）读取不崩
- `formatBabyCareSummary` 输出含新字段

### 3.5 提交建议

一笔 `feat: 尿布事件颜色性状字段补充`。

## 4. B0' 日摘要聚合（核心）

### 4.1 设计目标

让用户在 baby 期首页一眼看到"这一天娃的活动总和"，类 Piyo 摘要页。

### 4.2 新增 Service API

```java
public DailyBabySummary dailyBabySummary(String dateInput);
    // dateInput 形如 "2026-05-25"
    // 返回该日所有 baby 事件的聚合
    // 跨日睡眠按 sleepStart 日归属
```

`DailyBabySummary` POJO：

```java
public final class DailyBabySummary {
    public final String dateInput;             // 查询日期
    public final int feedCount;                // feed + breastfeed + bottle 当日总数
    public final int feedTotalMl;              // bottle.amountMl + feed.amountMl 求和
    public final String feedLastTime;          // 最后一次喂养的 occurredAt ISO（""空）
    public final int sleepTotalMinutes;        // 已结束 sleep 段时长求和
    public final int sleepIncompleteCount;     // 未结束 sleep 段数
    public final int peeCount;
    public final int poopCount;
    public final int diaperCount;
    public final double temperatureMax;        // 当日最高体温（NaN 表示无）
    public final double temperatureMin;        // 当日最低体温
    public final String temperatureLastTime;   // 最后一次体温 occurredAt
    public final String medicationLastName;    // 最后一次 medication.medicationName
    public final String medicationLastTime;
    public final int milestoneCount;           // 当日 milestone 事件数（B8 落地后才有数据）
}
```

**MUST 不计算**：
- `temperatureAbnormalCount` / "异常次数"——**触碰医疗判读边界**，不做
- "睡眠是否充足" / "喂养是否够"——同上
- 只做**事实聚合**，不做**评估**

### 4.3 实现位置

放在 `BabyLogService.dailyBabySummary`，不进 Repository（聚合是业务逻辑不是存储）。从 `repository.listEvents()` 全集过滤当日 baby 事件即可。

性能：单日 baby 事件数 < 100 是常态，listEvents 已无限制（Q2d 修复），全集过滤可接受。如未来事件量大，考虑加 Repository 的索引方法 `listEventsBetween(startIso, endIso)`，但 B0' 不强制做。

### 4.4 UI 改动

新 Composable `DailyBabySummaryCard`：

```
┌────────────────────────────────────────────┐
│ 5 月 25 日 · 今日                            │
│                                              │
│  🍼 喂养  6 次 / 540 mL  · 最后 15:30        │
│  😴 睡眠  7 小时 30 分（含 1 段未结束）       │
│  💧 尿布  尿 4 次 · 便 2 次                  │
│  🌡  体温  36.7 °C（最低）/ 37.4 °C（最高）  │
│  💊 用药  最近：布洛芬 14:00                 │
│  🌟 里程碑 2 个                              │
└────────────────────────────────────────────┘
```

每行**只显示非空数据**：
- 0 / NaN / "" → 整行不显示
- 数字为 1 时不显示 "1 次"，简化为 "1"
- 时间显示绝对时刻 + 相对时间（如 "15:30 · 2 小时前"）—— 复用 `BabyLogFormatters.relativeTimeFromNow`

放在 `HomeScreen` 的 baby 期分支：`BabyDayCard` 之下、`BabyDaySummary` 之上。`BabyDaySummary`（list 视图）保留，两者共存——摘要是聚合，list 是时间线明细。

### 4.5 不做的

- **不弹任何对话框 / 不做对比 / 不做趋势提示**："今日比昨日多 N mL"这种比较都不做
- **不做"建议"**：不显示"娃今天奶量偏少"等文案
- **不做手动汇总按钮**：dailyBabySummary 是首页 dashboard 数据，跟随 selectedBabyDay 切换实时算

### 4.6 Smoke

`BabyLogServiceSmokeTest` 加 `dailyBabySummary` 多 case：

1. **零事件**：返回所有 count=0、totalMinutes=0、temperatureMax/Min=NaN、各种 lastTime=""
2. **多 feed/bottle**：feedCount + feedTotalMl 求和正确
3. **跨日 sleep**：5/25 23:00–5/26 06:30 → 5/25 摘要 sleepTotalMinutes=450、5/26 摘要不包括
4. **未结束 sleep**：仅 sleepStart 无 sleepEnd → incompleteCount=1，totalMinutes 不计入
5. **多 temperature**：max / min 正确
6. **混合事件**：上述全有的一天，断言每项独立正确
7. **过滤非 baby 事件**：当日有孕期 ultrasound 不污染 baby 摘要

### 4.7 提交建议

按 service / UI 分两笔：
- `feat: 服务层日摘要聚合 (DailyBabySummary)`
- `feat: 首页日摘要卡片`

## 5. 提交拆分（推荐顺序）

| 序 | commit message | 内容 | 工期 |
|---|---|---|---|
| 1 | `feat: 喂养事件字段补充（侧别 / 时长 / 奶量 / 品牌）` | §1 全部 | 0.5 天 |
| 2 | `feat: 睡眠事件自动算时长 + UI 显示` | §2 全部 | 0.5 天 |
| 3 | `feat: 尿布事件颜色性状字段补充` | §3 全部 | 0.5 天 |
| 4 | `feat: 服务层日摘要聚合 (DailyBabySummary)` | §4.2 / 4.3 / 4.6 service | 1 天 |
| 5 | `feat: 首页日摘要卡片` | §4.4 UI | 1 天 |

5 笔，3-4 天工期。

## 6. 验收清单（总）

- [ ] B1 喂养：feed.breastSide / feed.solidFood / breastfeed.leftMinutes/rightMinutes / bottle.amountMl/brand 全部入 payload，UI 表单接入，编辑回填兼容旧 detail，smoke 覆盖
- [ ] B2 睡眠：`sleepDurationMinutes` helper 跨午夜正确，UI 显示 "X 小时 Y 分" / "睡眠中…"，smoke 覆盖跨日和未结束
- [ ] B3 尿布：diaper.color/consistency 字段入 payload，UI 表单 tertiary 自由文本，旧记录读取兼容，smoke 覆盖
- [ ] B0' 摘要：`dailyBabySummary(dateInput)` 返回完整 POJO，跨日 sleep 按 sleepStart 归属，**不做任何医疗判读**，smoke 覆盖 7 个 case
- [ ] B0' UI：`DailyBabySummaryCard` 显示 6 大类，0/NaN/空行不显示，时间显示绝对+相对
- [ ] 不动加密协议 / FGR / STT / Disclaimer / launcher / splash
- [ ] 每笔后 assembleDebug + lintDebug + detekt + JVM smoke 绿
- [ ] detekt 闸值未撞顶（CMA/Service/Repository 都有缓冲）
- [ ] 留在 `codex/stage-mainline-refactor`

## 7. 不在本轮范围

- B0 24h 时间轴（下一份评估）
- B5 疫苗本 / B4 WHO 曲线 / B6 体检 / 其它 B 系列
- 任何同步链路改动
- 任何 UI 视觉 token / launcher / splash 改动
