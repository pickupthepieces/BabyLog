# G2 宫缩计时器 需求评估（Claude）

| 项目 | 内容 |
|---|---|
| 作者 | Claude (Opus 4.7) |
| 日期 | 2026-05-20 |
| 来源 | `docs/孕期App功能缺口调研-Claude.md` G2（高优先候选） |
| 性质 | 真计时器，临产高频刚需；仅记录与展示数值，不预警、不诊断、不催就医 |

## 一、背景与定位

现有 `contraction` 事件类型只是"快捷记录一笔",缺真正的**计时器**:开始/停止、本次间隔、持续时长、频率历史、规律性。临产期家庭普遍需要这个工具——几乎所有孕期 App 都有。

## 二、范围与非范围

**范围（v1）**:
- 专用计时器全屏页:大启动按钮;启动后进入"宫缩会话",可记录多次宫缩开始/结束;实时显示**当前间隔、当前持续、平均频率**;结束会话存入时间线。
- 数据模型升级 `contraction` payload:加 `startIso/endIso/durationSec/sessionId/intervalFromPrevSec`。**单次宫缩仍是一个 event**;同一会话用 `sessionId` 聚合。
- 时间线/详情(Q2c)显示会话聚合(同 sessionId 行成簇)或单次。
- 编辑(Q2f)支持改 start/end 数值。

**非范围（v1）**:
- 不做"你应该去医院"或任何阈值预警/推送(医疗安全红线)。
- 不做后台计时(App 退到后台允许计时丢失;首版以前台为准)。
- 不与产检/产前征兆数据联动(纯独立工具)。

## 三、数据模型

升级 `contraction` payload 增加可选字段:
- `startIso`(string, ISO8601):本次宫缩开始时刻
- `endIso`(string, ISO8601):本次宫缩结束时刻(若用户只点了一次"开始"未点"结束",`endIso` 留空,`durationSec` 为 0)
- `durationSec`(number):本次持续秒数
- `sessionId`(string UUID):同一组临产计时的标识
- `intervalFromPrevSec`(number, 可选):本次开始距上次开始的秒数(用户开启同一会话内第 2 次起填)

**向后兼容**:旧 `contraction` 浅记录(只 `note`)字段缺省即可,时间线/编辑仍 work。

## 四、计时器逻辑(纯数值,无判读)

- "开始一次宫缩" → 记 `startIso = now`,UI 显示"宫缩进行中…"+ 倒计读秒
- "结束本次" → 记 `endIso = now`、`durationSec = end - start`;若是会话内第 N 次(N≥2),记 `intervalFromPrevSec = thisStart - prevStart`
- "本会话统计"(纯展示):次数、平均持续、平均间隔、最短/最长间隔
- "结束会话" → 全部宫缩事件入库(显式确认按钮触发);未确认前是临时态,不入库
- App 退到后台/进程被杀:会话当前未存的数据丢失(首版接受,提示明显)

## 五、UI

新增 `record/contraction-session` 全屏页(`NavCompose composable`):
- 顶部 AppBar + 中性免责行:"宫缩计时器仅作记录与参考,具体判断请以医生意见为准"
- 大按钮 "开始一次宫缩 / 结束本次"(切换态)
- 进行中显示当前持续秒数,实时刷新
- 本会话列表:每一次宫缩 [开始时间 持续X秒 距上次Y分Z秒]
- 底部 "结束会话并保存" 显式按钮(人工确认才入库;复用项目铁律)
- 退出页面前提示 "未保存数据将丢失,继续退出?"

入口:孕期 quick rail 的"宫缩"磁贴点击 → 跳本页(替代现有快速记录路径);快速记录仍在 quick action dialog 内保留(不替换、不删除——一拍即记的快速路径与计时器路径并存,用户选)。

## 六、医疗安全(硬约束)

- App **不**给任何"是否临产"/"应否就医"判读;**不**做阈值预警(如"间隔<5分钟提示")。
- 顶部固定免责行,与 `DISCLAIMER.md` 风格一致。
- 仅展示数值与统计;统计仅"平均/最短/最长",**不**附"接近临产"等暗示。
- 人工确认才入库(与项目铁律一致)。

## 七、复用与衔接

- 复用现有 `contraction` 事件类型 + `timelineFilterGroup → "pregnancy"`(已映射,首页/时间线/Q2c/Q2f 自动覆盖);新字段是可选 payload,不破坏既有渲染。
- 时间线 `eventSummary` 对带 sessionId 的可显示聚合行(同 session 合并显示,可选;v1 可不聚合,每次宫缩单独一行,通过 sessionId 加小标注即可)。
- Q2c 详情显示 start/end/duration/interval 字段。
- Q2f 编辑支持改 start/end 时间。
- G9 复诊汇总导出按 session 聚合呈现该会话宫缩列表与统计。

## 八、验收

- 启动会话 → 记 N 次宫缩 → 结束会话:N 个 event 入库,每个 payload 含 start/end/duration/sessionId,第 2 起含 intervalFromPrev。
- 时间线显示这些 event;详情(Q2c)显示完整字段;编辑(Q2f)可改 start/end。
- G9 导出含本次会话(按 sessionId 聚合显示)。
- App 不出现任何"是否临产/应否就医"判读文案;免责行固定。
- 未结束会话前不入库;退出前确认;assemble+lint+smoke 绿;装机走查完整会话+编辑+详情+导出。

## 九、不做(v2)

- 后台计时/通知;阈值提示(医疗安全主动拒绝)。
- 自动判定"规律宫缩"等模型判读;留给医生。
