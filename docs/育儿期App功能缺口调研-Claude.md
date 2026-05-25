# 育儿期 App 功能缺口调研（Claude）

| 项目 | 内容 |
|---|---|
| 作者 | Claude（Opus 4.7）|
| 日期 | 2026-05-25（v3 · 叠加 Piyo 对标视角修订） |
| 方法 | 代码层盘点 + **Piyo 对标 doc 2026-05-15** + 主流育儿 App 一般认知 |
| 性质 | 候选 backlog，**用户挑选后才排期** |

> **v3 修订说明**：v2 做了代码层盘点（4 个预留 eventType 发现）但没读 `docs/Piyo对标差异与BabyLog产品方向.md`，**漏了 Piyo 视角下最大的两块体验缺口**：24h 日视图时间轴 + 日摘要聚合。v3 把这两项作为 B0 / B0' 加入并重排顺序。代码层盘点结论延续。

## 一、定位过滤

按 `非目标` / `DISCLAIMER` 不纳入：

- 社区 / 妈妈圈子 / 育儿社交、商城 / 电商 / 广告、育儿知识 feed、早教 / 胎教音乐、AI 育儿问诊、第三方账户、智能硬件、宝宝相册社交分享、育儿打卡游戏化

## 二、BabyLog 现有育儿期能力（v3 · 代码层精确）

### 2.1 已结构化的事件类型

| eventType | payload 字段 | 入口 | 编辑 | 详情 | 现状 |
|---|---|---|---|---|---|
| `birth` | ChildProfile.birthDate 联动 | quick rail | ✓ | ✓ | 完整 |
| `feed` | `feedType` / `amountMl` / `note` | quick rail + 表单 | ✓ | ✓ | 已结构化 |
| `breastfeed` | `detail` / `note` | quick rail | ✓ | ✓ | 半结构化 |
| `bottle` | `detail` / `note` | quick rail | ✓ | ✓ | 半结构化 |
| `sleep` | `sleepStart` / `sleepEnd` / `sleepPlace` / `note` | quick rail + 表单 | ✓ | ✓ | 已结构化但**无自动时长** |
| `wake` | `detail` / `note` | quick rail | ✓ | ✓ | 简单标记 |
| `diaper` | `diaperType` / `diaperDetail` / `note` | quick rail + 表单 | ✓ | ✓ | 已结构化 |
| `pee` | `detail` / `note` | quick rail | ✓ | ✓ | 简单标记 |
| `poop` | `detail` / `note` | quick rail | ✓ | ✓ | 简单标记 |
| `temperature` | `temperatureC` / `measureMethod` / `note` | quick rail + 表单 | ✓ | ✓ | 完整 |
| `medication` | `medicationName` / `dosage` / `reason` / `note` | quick rail + 表单 | ✓ | ✓ | 完整 |

### 2.2 已"预留"但未实现的事件类型

**`BabyLogDomain.ALLOWED_EVENT_TYPES` 已注册、`BabyLogFormatters.eventLabel()` 有中文名、`timelineFilterGroup()` 映射到 "baby" 分组、smoke 已校验**，但 `BabyLogService` 没有 `recordXxxEvent` 入口、没有表单、没有 quick rail：

| eventType | 中文标签 | 已就绪 | 缺 |
|---|---|---|---|
| `illness` | "不适" | Domain 白名单 + 标签 + filterGroup + smoke | 表单 + payload builder + 入口 |
| `growth` | "成长" | 同上 | 同上（+ WHO 曲线就是 B4）|
| `vaccine` | "疫苗" | 同上 | 同上（+ 国家免疫规划清单就是 B5）|
| `milestone` | "里程碑" | 同上 | 同上 |

### 2.3 UI / 基建能力

- `BabyDayCard` 当日选择器（前一天 / 今天 / 后一天）
- `BabyDaySummary` 当日记录 **list**（按时间倒序的事件行）
- stage 切换（孕期↔出生后自动）
- 编辑 / 删除 / 详情查看（Q2 / Q2c / Q2f 全覆盖）
- 智能录入（语音 + LLM 分类）
- 资料库附件入口
- 家庭同步 S1-S5 entity-agnostic
- Q1 免责门 / Q4 提醒中心 / Q2g 搜索

### 2.4 vs Piyo 的核心交互差距

参考 `docs/Piyo对标差异与BabyLog产品方向.md` 2026-05-15：

| 维度 | Piyo | BabyLog 现状 | 差距评估 |
|---|---|---|---|
| 主交互范式 | 日期 + **24h 垂直时间轴** + 底部事件按钮 | 日期 + 事件 list + quick rail | **质变差距**：节律可视化缺失 |
| 摘要 | 主导航有"摘要"页：喂奶 N 次/奶量/睡眠总时长/尿布次数 | `BabyDaySummary` 只 "当日 N 条" | **质变差距**：聚合视图缺失 |
| 成长曲线 | 体重 / 身长 / 头围 含 WHO 参考 | 仅孕期 FGR | 待补（= B4） |
| 疫苗管理 | 疫苗本 + 提醒 | 无 | 待补（= B5）|
| 首次使用向导 | 完整 onboarding | Q1 免责门 + 默认家庭 | 已部分覆盖 |

## 三、缺口（v3 · Piyo + 代码盘点交叉）

### 3.1 最高优先（Piyo P0 体验缺口）

| # | 缺口 | 工程量 | 价值 | 备注 |
|---|---|---|---|---|
| **B0** | **24h 日视图时间轴** | 中：新 Composable `BabyDayTimelineView`，按时段渲染事件气泡 / 段（睡眠段、喂奶点、尿布点）。可与现有 list 视图共存或切换 | **极高** | Piyo 核心体验，**没它就不是"育儿日工具"** |
| **B0'** | **日摘要聚合视图** | 中：service 加 `dailyBabySummary(date)` 返回 {喂养次数 / 奶量 mL / 睡眠总时长 / 尿尿便便次数 / 最近体温用药}。UI 顶部摘要卡 | **极高** | Piyo P0，家长晚上回看每天的"数字 KPI" |

### 3.2 高优先（家庭刚需 + 工程量适中）

| # | 缺口 | 工程量 | 价值 |
|---|---|---|---|
| **B1+B2+B3** | **喂养 / 睡眠 / 尿布字段补充** | 小：在现有 feed / sleep / diaper payload 加字段。**B2 sleepEnd 自动算时长是 B0' 摘要的前置** | 高（每天 10+ 次）|
| **B5** | **疫苗本（国家免疫规划 + 自费）+ 接种提醒** | 中：`vaccine` 已预留 + 表单 + 预设清单 + 接 Q4 提醒中心 | **高**：每孩子打 20+ 针，刚需 |
| **B8** | **里程碑日记** | 小：`milestone` 已预留 + 简单表单 | 中-高：情感价值高，工程量极小 |

### 3.3 中优先（家人用熟后补完）

| # | 缺口 | 工程量 | 价值 |
|---|---|---|---|
| **B4** | **身高体重头围 + WHO 0-5 岁参考曲线** | 中-大：`growth` 已预留 + 表单 + WHO 数据 + Compose 曲线（复用 FGR）| 高，每月体检高频 |
| **B6** | **体检事件结构化（满月/3/6/9/12/18/24 月）** | 中：新 `baby_checkup` eventType（类比 pregnancy_checkup）+ 与 B4 联动 | 高，复诊核心 |
| **B10** | **用药 / 症状日记** | 小：`medication` 已结构化、`illness` 已预留 | 中 |
| **B7** | **育儿期复诊汇总导出（baby 版 G9）** | 小：复用 G9 链路加 baby filter | 中 |

### 3.4 低优先

| # | 缺口 | 工程量 | 价值 |
|---|---|---|---|
| **B9** | **哺乳计时器 / 喂奶间隔提醒** | 中：复用 G2 宫缩计时器模式 | 中（如果做 B0' 摘要 + B5 提醒可能不需要单独计时器） |
| **B11** | **辅食添加记录** | 小：跟 B1 字段补充重合 | 低 |
| **B12** | **baby 期长文本字段语音录入** | 极小：复用 G11 模式 | 低 |

### 3.5 排除（不做）

- 宝宝相册社交分享、智能监护器联动、育儿打卡游戏化、AI 育儿建议、辅食食谱推荐、社区社交、电商、广告、第三方账户、母婴知识 feed

## 四、医疗安全注记

凡涉及"推荐范围 / 区间 / 达标"（B4 WHO 百分位、B5 疫苗时间窗、B6 体检值、B11 辅食过敏观察）一律比照 FGR：

- **仅参考**，不诊断、不预警、不催促
- 数值由**用户 / 医院报告**录入，App 绝不计算风险或判读
- 文案与 `DISCLAIMER.md` 一致
- 保人工确认链
- 疫苗 / 体检提醒措辞中性可忽略，**不医疗结论、不催促**

里程碑（B8）**不预设达标年龄**，避免引发家长焦虑。

`vaccine` 即使预设国家免疫规划清单，**只展示日期窗 + 是否已打**，**不判断**"晚打 X 天有 Y 风险"，**不催促**。

## 五、推荐执行顺序（v3 · Piyo 视角）

```
B1+B2+B3 字段补充（含 sleepEnd 时长自动算）  ← B0' 摘要的前置
   ↓
B0' 日摘要聚合视图                            ← 让现有数据立刻有 Piyo 体验
   ↓
B0 24h 日视图时间轴                           ← Piyo 核心交互范式
   ↓
B5 疫苗本 + 接种提醒                          ← 家庭刚需 entity
   ↓
B8 里程碑                                     ← 顺手做（小）
   ↓
B4 身高体重 + WHO 曲线                        ← 工程量最大的一项
   ↓
B6 体检结构化 + B4 联动                       ← B4 之上的复用
   ↓
B10 用药症状 / B7 复诊导出                    ← 收尾
   ↓
[按需] B9 / B11 / B12
```

### 顺序逻辑

1. **B1+B2+B3 先做**：是 B0' 的前置（sleepEnd 不算时长则摘要里"今日睡眠 X 小时"无意义）。本身也是日常高频体验提升。
2. **B0' 摘要紧接**：让现有 11 个 baby 事件类型立刻产生聚合价值，**乘数效应最大**。比新增任何 entity 都更让用户感知到 BabyLog 是"育儿工具"而不是"事件流水账"。
3. **B0 时间轴第三**：和 B0' 配合形成 Piyo 主交互闭环。先 B0' 后 B0 是因为 B0' 服务端聚合逻辑可以独立验，B0 是 UI 重活更适合在 B0' 数据基础上接。
4. **B5 第四**：开启疫苗本 entity，家庭刚需。前三项做完后用户已经习惯 baby 期工作流，导入新 entity 阻力小。
5. **B8 第五**：milestone 工程量极小，顺手做。
6. **B4 第六**：WHO 曲线是工程量最大的一项（数据集成 + Compose 曲线 + 参考带绘制），等 BabyLog baby 期已成熟后再啃。
7. **B6 紧接 B4**：复用 B4 的身高体重输入。
8. **B10 / B7 收尾**。

### 工期估算

| 项 | commit 数 | 工期估算 |
|---|---|---|
| B1+B2+B3 字段补充 | 1-2 | 1-2 天 |
| B0' 日摘要 | 2-3 | 3-4 天 |
| B0 24h 时间轴 | 3-4 | 4-7 天（含 UI 重活）|
| B5 疫苗本 | 3-4 | 4-7 天（含预设清单）|
| B8 里程碑 | 1 | 1-2 天 |
| B4 WHO 曲线 | 3-4 | 5-7 天（含数据集成）|
| B6 体检 | 2-3 | 3-5 天 |
| B10 用药症状 | 1-2 | 2-3 天 |
| B7 复诊导出 | 1 | 1-2 天 |
| **合计** | **17-24 笔 commit** | **4-6 周** |

## 六、与现有同步链路的关系

S1-S5 entity-agnostic 设计意味着：

1. `BabyLogDomain.createEvent` 已支持所有 eventType（包括 4 个预留 type）
2. `BabyLogFormatters.timelineFilterGroup` 已映射所有预留 type 到 `"baby"` 组
3. `isEventVisibleInHome(STAGE_BABY)` 已覆盖 `baby` 分组
4. push / pull / 附件链路全自动覆盖新 type，**同步层零改动**

每项 B 标准 commit 模板：

```
1. BabyLogService: recordXxxEvent / updateXxxEvent / buildXxxPayload / 
                   formatXxxSummary / hasXxxMinimumContent / XxxInput
2. BabyLogDomain: 已预留（除 B6 需加 baby_checkup）
3. BabyLogFormatters: 已映射（除 B6 需加）+ eventLabel
4. CMA: quick rail 入口 + 表单 wiring + 编辑回填 + isEditableBabyRecord
5. ui/screens/XxxFormScreen.kt: 新表单
6. smoke: BabyLogServiceSmokeTest 加 case
```

B0 / B0' 不是新 entity，是新 UI / Service 聚合：

```
B0' 摘要：
   1. BabyLogService: dailyBabySummary(date) 返回 DailySummary
   2. ui/components: DailySummaryCard
   3. smoke: 各 entity 聚合断言

B0 时间轴：
   1. ui/screens: BabyDayTimelineView 新 Composable
   2. CMA: 接入 baby 期 home，可与现有 list 切换或并存
   3. （无 smoke，UI 视图层）
```

## 七、Sources

- **代码盘点（v3 主要数据源）**：`BabyLogDomain.ALLOWED_EVENT_TYPES` / `BabyLogFormatters.eventLabel timelineFilterGroup` / `BabyLogService.buildBabyCarePayload recordBabyCareEvent recordQuickEvent` / `ComposeMainActivity.babyCareLabels babyEntries isEditableBabyRecord` / `BabyCareFormScreen`
- **Piyo 对标文档**：`docs/Piyo对标差异与BabyLog产品方向.md` 2026-05-15（v3 新增交叉数据源）
- [WHO Child Growth Standards 0-5 years](https://www.who.int/tools/child-growth-standards)
- [国家免疫规划 0-6 岁疫苗时间表 · 国家卫健委](http://www.nhc.gov.cn/)
