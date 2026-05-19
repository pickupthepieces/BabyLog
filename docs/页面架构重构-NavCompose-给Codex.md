# 页面架构重构：单 Activity 巨弹框 → Navigation-Compose 多页面

| 项目 | 内容 |
|---|---|
| 决策 | 用户拍板：**Navigation-Compose 多页面**（单 Activity + 多真实页面目的地） |
| 病灶 | `ComposeMainActivity.kt` 4331 行 god Activity；14 个 `*Dialog`/15 处 `AlertDialog`；真实页面仅 4 个（home/timeline/library/settings 挤同一 LazyColumn） |
| 目标 | 重表单/设置子页/浏览类升级为带返回栈+AppBar+转场+状态保持的真实页面；弹框只留真确认 |
| 纪律 | **架构轮,分阶段,每阶段独立 commit,不与功能混提交,不并 main** |

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
