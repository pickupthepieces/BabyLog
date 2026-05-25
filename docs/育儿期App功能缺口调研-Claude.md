# 育儿期 App 功能缺口调研（Claude）

| 项目 | 内容 |
|---|---|
| 作者 | Claude（Opus 4.7）|
| 日期 | 2026-05-25（v2 代码层盘点修订） |
| 方法 | 调研主流育儿记录 App + **直接读 BabyLog 代码盘点 baby 期现状**（v2 重点） |
| 性质 | 候选 backlog，**用户挑选后才排期**；非自动进队列 |

> **v2 修订说明**：v1 文档基于训练数据 + 印象列了"BabyLog 现有育儿期能力"，与代码实际严重偏离。本版重读 `BabyLogDomain` / `BabyLogService` / `BabyLogFormatters` / `ComposeMainActivity` / `BabyCareFormScreen` 校准。**关键发现**：4 个 baby 事件类型已经在 Domain 层预留位置等实现，B 系列优先级因此重排。

## 一、定位过滤（先排除非目标）

按 `非目标` / `DISCLAIMER` 不纳入缺口：

- 社区 / 妈妈圈子 / 育儿社交
- 商城 / 母婴电商 / 广告
- 育儿知识资讯 feed
- 早教 / 胎教音乐 / 故事
- AI 育儿问诊
- 第三方账户（微信 / Apple / Google）
- 智能硬件联动（监护器 / 体温贴）
- 宝宝相册社交分享
- 育儿打卡 / 任务 / 游戏化

## 二、BabyLog 现有育儿期能力（v2 · 代码层精确）

### 2.1 已结构化的事件类型（baby 期）

| eventType | payload 字段 | 入口 | 编辑 | 详情 | 现状评估 |
|---|---|---|---|---|---|
| `birth` | 通过 ChildProfile.birthDate 联动 | quick rail | ✓ | ✓ | 完整 |
| `feed` | `feedType` / `amountMl` / `note` | quick rail + 表单 | ✓ | ✓ | **已结构化**，缺 L/R 乳别 + 辅食食材 |
| `breastfeed` | `detail` / `note` | quick rail | ✓ | ✓ | **半结构化**（只 detail 字符串）|
| `bottle` | `detail` / `note` | quick rail | ✓ | ✓ | **半结构化**（只 detail 字符串）|
| `sleep` | `sleepStart` / `sleepEnd` / `sleepPlace` / `note` | quick rail + 表单 | ✓ | ✓ | **已结构化**，缺自动时长计算 + 白觉夜觉 |
| `wake` | `detail` / `note` | quick rail | ✓ | ✓ | 简单标记 |
| `diaper` | `diaperType` / `diaperDetail` / `note` | quick rail + 表单 | ✓ | ✓ | **已结构化**，缺颜色分类 |
| `pee` | `detail` / `note` | quick rail | ✓ | ✓ | 简单标记 |
| `poop` | `detail` / `note` | quick rail | ✓ | ✓ | 简单标记 |
| `temperature` | `temperatureC` / `measureMethod` / `note` | quick rail + 表单 | ✓ | ✓ | **完整** |
| `medication` | `medicationName` / `dosage` / `reason` / `note` | quick rail + 表单 | ✓ | ✓ | **完整** |

### 2.2 已"预留"但未实现的事件类型

**`BabyLogDomain.ALLOWED_EVENT_TYPES` 已注册、`BabyLogFormatters.eventLabel()` 有中文名、`timelineFilterGroup()` 映射到 "baby" 分组、smoke 已校验**，但 **`BabyLogService` 没有 `recordXxxEvent` 入口**、**没有表单**、**没有 quick rail 入口**：

| eventType | 中文标签 | 已就绪 | 缺 |
|---|---|---|---|
| `illness` | "不适" | Domain 白名单 + 标签 + filterGroup baby + smoke | 表单 + payload builder + 入口 |
| `growth` | "成长" | 同上 | 同上（**+ WHO 曲线就是 B4**） |
| `vaccine` | "疫苗" | 同上 | 同上（**+ 国家免疫规划清单就是 B5**） |
| `milestone` | "里程碑" | 同上 | 同上 |

**含义**：B 系列大部分工程量**不是新建 entity 框架**，而是"在已就绪的 hook 上加业务实现"。复用 NavCompose + Q2 编辑基建 + 同步链路 + smoke 模板。

### 2.3 UI / 基建能力

- `BabyDayCard` 当日选择器（前一天 / 今天 / 后一天）
- `BabyDaySummary` 当日记录列表 + 计数
- stage 切换（`resolveCareStage` 自动按 ChildProfile.birthDate 切孕期→出生后）
- TimelineRow 编辑 / 删除 / 详情查看（Q2 / Q2c / Q2f 已覆盖全部 baby 类型）
- 智能录入（语音 + LLM 分类）—— `babyCareLabels()` 已配 11 种 eventType 标签
- 资料库附件入口（baby 类型可挂照片）
- 家庭同步 S1-S5（全自动覆盖任何新 baby eventType，**无需改同步代码**）
- Q1 免责门 / Q4 提醒中心 / Q2g 搜索 全部覆盖 baby 类型
- detekt 行数闸值 + smoke 网兜 + CI 三 stage

## 三、缺口（v2 · 按代码现状重排）

> 优先级标准：**家庭实际使用频度 × 工程量小** = 高优先
> 工程量评估：依赖 Domain 是否预留 + 是否需新表单 + 是否需新算法（如 WHO 曲线）

### 3.1 高优先（推荐先做的 4-5 项）

| # | 缺口 | 工程量 | 价值 |
|---|---|---|---|
| **B5** | **疫苗本（中国国家免疫规划 + 自费）+ 接种提醒** | 中：`vaccine` 已预留 + `recordVaccineEvent` + payload + 表单 + 国家免规预设清单 + 接 Q4 提醒中心。**不需要新 entity 框架** | **高**：直击家庭刚需，每个孩子打 20+ 针 |
| **B4** | **身高体重头围 + WHO 0-5 岁参考曲线** | 中-大：`growth` 已预留 + payload + 表单 + WHO 数据集成（类比 FGR）+ Compose 曲线绘制（复用 `FetalGrowthChart` 模式） | **高**：每月体检高频；曲线是参考性需 disclaimer |
| **B6** | **体检事件结构化（满月 / 3 / 6 / 9 / 12 / 18 / 24 月+）** | 中：需要新 eventType `baby_checkup`（类比 `pregnancy_checkup`），payload 含身高体重头围 + 医生结论 + 下次时间 + 附件。**与 B4 自动联动**（体检测量值落 `growth` event 或共享字段） | **高**：复诊核心，复用 pregnancy_checkup 模式工程量适中 |
| **B1+B2+B3** | **喂养 / 睡眠 / 尿布字段补充** | 小：在现有 `feed` / `sleep` / `diaper` payload 加字段：母乳 L/R 乳别、辅食食材、自动算睡眠时长、尿布颜色性状。**一笔 fix 可合并** | **高**：日常每天 10+ 次，字段细化复诊价值高 |
| **B8** | **里程碑日记** | 小：`milestone` 已预留 + 简单表单（标题 + 日期 + 备注 + 可选附件）。**纯记录、不预设达标年龄、不预警延迟** | **中-高**：情感价值高，工程量极小 |

### 3.2 中优先（家人用上之后补完）

| # | 缺口 | 工程量 | 价值 |
|---|---|---|---|
| **B7** | **育儿期复诊汇总导出（baby 版 G9）** | 小：复用 G9 现有 Markdown 导出 + LLM 润色 fixed-prompt 链路，加 baby 类型 filter | 中 |
| **B10** | **用药 / 症状日记** | 小：`medication` 已结构化，`illness` 已预留 → 补 `recordIllnessEvent` 表单。同孕期 G5 / G6 模式 | 中 |
| **B9** | **哺乳计时器 / 喂奶间隔提醒** | 中：复用 G2 宫缩计时器代码模式做哺乳 session timer；间隔提醒接 Q4 提醒中心 | 中 |

### 3.3 低优先（看精力 / 看实际反馈）

| # | 缺口 | 工程量 | 价值 |
|---|---|---|---|
| **B11** | **辅食添加记录** | 小：跟 B1 喂养字段补充重合，独立做意义有限 | 低 |
| **B12** | **baby 期长文本字段语音录入** | 极小：复用 G11 模式，给 baby 各表单的 note 字段加按住说话入口 | 低 |

### 3.4 已排除（不做）

- 宝宝相册社交分享、智能监护器联动、育儿打卡游戏化、AI 育儿建议、辅食食谱推荐、社区社交、电商、广告、第三方账户、母婴知识 feed

## 四、医疗安全注记（所有候选通用）

凡涉及"推荐范围 / 区间 / 达标"（B4 WHO 百分位、B5 疫苗时间窗、B6 体检值正常范围、B11 辅食过敏观察）一律比照 FGR：

- **仅参考**，不诊断、不预警、不催促
- 数值由**用户 / 医院报告**录入，App 绝不计算风险或判读
- 文案与 `DISCLAIMER.md` 一致
- 保人工确认链（save 之前必须用户点保存）
- 疫苗 / 体检提醒措辞中性可忽略，**不医疗结论、不催促**

里程碑（B8）**不预设达标年龄**：不写"翻身正常应在 3 月"这种判断性文案，只让用户记录何时观察到，避免引发家长焦虑。

`vaccine` 即使是国家免疫规划清单，也**只展示日期窗 + 是否已打**，**不判断**"晚打 X 天有 Y 风险"，**不催促**。措辞参考 Q1 免责门 + Q4 提醒中心既有模板。

## 五、推荐顺序

按"工程量小 × 价值高"打分，**推荐执行顺序**：

```
B1+B2+B3 字段补充 (小, 1-2 笔合并 fix)
    ↓
B8 里程碑 (小, 1 笔 feat)
    ↓
B5 疫苗本 + 接种提醒 (中, 2-3 笔 feat)
    ↓
B4 身高体重头围 + WHO 曲线 (中-大, 3-4 笔 feat)
    ↓
B6 体检结构化 + 与 B4 联动 (中, 2-3 笔 feat)
    ↓
B10 用药 / 症状日记 (小-中, 1-2 笔 feat)
    ↓
B7 复诊汇总 baby 版 (小, 1 笔 feat)
    ↓
[按需] B9 哺乳计时器 / B11 辅食 / B12 长文本语音
```

**总评估**：B 系列 7 项 + 字段补充 1 笔 = **约 12-15 笔 commit**，育儿期主线化全工期约 3-5 周。

## 六、与现有同步链路的关系

S1-S5 同步链路是 **entity-agnostic** 设计：

1. `BabyLogDomain.createEvent` 已支持所有 eventType（包括预留的 illness/growth/vaccine/milestone）
2. `BabyLogFormatters.timelineFilterGroup` 已映射所有预留 type 到 `"baby"` 组
3. `isEventVisibleInHome(STAGE_BABY)` 已覆盖 `baby` 分组
4. push / pull / 附件链路 **全自动覆盖**新 type，无需改同步代码

这意味着育儿期主线化工程量**主要在 UI + 表单 + payload builder + 校验 + 文档**，同步层零改动。

每项 B 实现的标准 commit 模板：

```
1. BabyLogService: add recordXxxEvent + updateXxxEvent + buildXxxPayload + 
                   formatXxxSummary + hasXxxMinimumContent + XxxInput
2. BabyLogDomain: 已在 ALLOWED_EVENT_TYPES（除 B6 新增 baby_checkup）
3. BabyLogFormatters: 已在 timelineFilterGroup（除 B6 新增）+ eventLabel
4. CMA: quick rail 入口 + 表单 wiring + 编辑回填 + isEditableBabyRecord
5. ui/screens/XxxFormScreen.kt: 新表单
6. smoke: BabyLogServiceSmokeTest 加 case
```

## 七、Sources

- 代码盘点（本次主要数据源）：`BabyLogDomain.java` `ALLOWED_EVENT_TYPES` / `BabyLogFormatters.eventLabel timelineFilterGroup` / `BabyLogService.buildBabyCarePayload recordBabyCareEvent` / `ComposeMainActivity.babyCareLabels babyEntries isEditableBabyRecord`
- [WHO Child Growth Standards 0-5 years](https://www.who.int/tools/child-growth-standards)
- [国家免疫规划 0-6 岁疫苗时间表 · 国家卫健委](http://www.nhc.gov.cn/)
- [亲宝宝 / 宝宝树（出生后）功能盘点 · 人人都是产品经理](https://www.woshipm.com/)
