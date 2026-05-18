# UI 精简交接（Claude → Codex）

方向：像 Piyo 的「极简 + 留一点栗子」。Claude 负责 spec/评审/git，Codex 负责重 UI 手术 + 装机截图。

## 已完成（Claude，commit `b780b34`，勿回退）

- WeekCard：删栗子吉祥物图 + 渐变 + 内嵌卡 → 纯白扁平卡；全宽 Column 修词中折行
- Panel / TrendCard：去描边、`elevation = 0`
- EmptyPanel：删 `empty_state_scene` 插画 → 一行 `Text3` 引导
- PregnancySummaryPanel：无孕期数据时 5 宫格「暂无」→ 单句引导

未动 IA / 导航结构 / 阶段投影 / 业务逻辑 / OCR / FGR。

## 待 Codex 做

### E. 底部导航 + 功能图标 → 统一线性图标
- `ComposeMainActivity.kt` 的 `BottomNav`（约 line 2992）：4 个 tab 与常用操作图标，PNG 贴纸切图 → 一套**统一描边宽度**的线性图标（Material `Icon` + material-icons，或一套自带 vector）
- 底栏扁平：去栗子/贴纸装饰，选中态用 `ChestnutPalette.Primary`，未选中 `Muted`

### F. 清扫装饰
- 移除功能 UI 里所有 `piyo`/`chestnut` 贴纸用法（约 48 处 `painterResource(R.drawable.*chestnut*/ *piyo* /...)`）
- 栗子吉祥物全 App **最多保留 1 处**做品牌：启动页 *或* 顶栏小角标，二选一，其余不出现
- 不引外部字体；不动 IA/逻辑

## 约束

- E、F 各自一个独立 UI-only commit，message 标「视觉精简」，不与 OCR/后端混提交
- 期间勿改 WeekCard / Panel / TrendCard / EmptyPanel / PregnancySummaryPanel 的视觉（Claude 已定）
- 装机出 before/after 截图（首页空态 + 有数据 + 一个表单对话框 + 底栏），交 Claude 比对验收

### G. 文案瘦身（Claude 已审，Codex 执行）

原则：删"路线图泄漏 / 内部行为解说 / 过度说明";**保留**破坏性操作确认、医疗免责、密钥隐私说明。
均在 `ComposeMainActivity.kt`,行号以当前 `2ceae47` 为准（改动后会漂移，按文本定位）。

| 行 | 现状 | 处理 |
|---|---|---|
| 1267 | "连接已有家庭后续在设置页填写服务器地址和家庭密钥；首登只保留新建与导入，避免重复建档。" | **整段删** |
| 1383 | "第一轮按选中日期倒序展示记录；24 小时刻度时间轴放到下一阶段。" | **整段删**（路线图） |
| 1981 | "保存后只进入孕期首页和历史时间线，不会出现在出生后日视图。" | **整段删**（内部行为） |
| 1235 | "…数据默认保存在本机。后续接服务器也只做家庭成员身份校验和同步授权，不接第三方账户。" | 删第二句,保留"BabyLog 仅做家庭记录和复诊沟通辅助；数据默认保存在本机。" |
| 1244 | "先选择当前家庭状态，BabyLog 会按孕期或出生后显示不同首页。" | 缩为"选择当前家庭状态" |
| 2042 | "血糖提示只用于提醒复核，不构成诊断；保存后进入孕期首页和历史时间线。" | 缩为"血糖提示仅用于提醒复核，不构成诊断" |
| 2153 | "先拍照/选图识别，再应用候选；也可以直接手动填写下方生长指标。" | 缩为"可拍照/选图识别,或手动填写下方指标" |
| 2452 | "可应用报告明确写出的生长指标、胎心、羊水、胎盘、胎位等字段；孕周不由模型识别或推断，请按报告手动填写。" | 缩为"孕周不由模型识别,请按报告手动填写"（保留安全句） |
| 2553 | "日期可以后补；缺失时只显示补全入口和空态，不使用假孕周、假日龄或假成长曲线。" | 缩为"日期可后补" |
| 2885 | "已删除记录会保留 7 天。恢复后会重新出现在首页、时间线和资料库；超过 7 天会自动永久清理。" | 缩为"删除记录保留 7 天,超期自动永久清理" |
| 2894 | "回收站是空的。误删的记录会先放在这里，7 天内都能恢复。" | 缩为"回收站是空的" |

**勿动（安全/隐私,保持原文）**：L286/300/314/328 确认弹窗、L1801 医疗免责、L2257 Hadlock 估算说明、L2698 密钥隐私说明。
G 可并入 F 的同一 commit（都属去冗余）。

### H. 结构性冗余清除（Claude 已审，Codex 执行）

问题：几乎每个 section 都挂一句**不可点击的灰字 caption**(纯装饰废话),还有每屏重复 chrome。Piyo 不会这样。

**H1. 删 SectionHeader 装饰性 action**（`SectionHeader` 中 `onAction == null` 时 action 只是灰字,无任何功能,纯噪声）：

| 行 | action | 处理 |
|---|---|---|
| 1093 | `"按时间倒序"` | 删 `action=` 参数 |
| 1129 | `"点击查看曲线"` | 删（且它撒谎——根本不可点） |
| 1432 | `"只看孕期"` | 删 |
| 1501 | `"00:00 起算"` | 删 |
| 1394 | `action = selectedDay` | 删（日期 chrome,标题已够） |

> **保留**所有带 `onAction` 的(真功能)：1111 全部记录、1710/1717/1724/1731 编辑、1739/1754 设置、1746 同步、1762 导出、1769 导入、1781/1788 查看、1795 清空。
> 建议给 `SectionHeader` 加约束：`action` 仅在有 `onAction` 时才允许传,从源头杜绝装饰 caption。

**H2. 删每屏重复 chrome**：
- L1190 Header 副标题 `"$nickname · ${stageLabel} · 本机模式"` → 去掉 `· 本机模式`（首登免责已说明本机,不必每屏复读）
- L1215 `"离线可用"` 灰底药丸 → 整块删（同上,冗余 chrome）

**H3. EmptyPanel 文案去尾巴**：
- L1097 `"这一天还没有记录。底部按钮可以快速补一条。"` → `"这一天还没有记录"`
- L1119 `"还没有记录，点下方 + 开始。"` → `"还没有记录"`

**H4. QuickActionDialog 冗余 hint**（L1884 显示 `action.hint`）：出生后档的 `"一键记录"`/`"开始睡"`/`"醒来"` 与 label 同义,删这些 hint 行或留空(L872-877 的 hint 改空串,渲染处 hint 为空时不显示该行)；孕期档 hint(`"指标 / 照片 / 识别"`等)有信息量,保留。

H1–H4 可与 F/G 并入同一"去冗余"commit。

### I. 视觉风格统一与素材策略（下一轮，权威）

**背景**：对照真 Piyo 实图（`diagnostics/app-compare/`）结论——我们的扁平/干净/线性图标方向没错（Piyo 自己就有「简单」图标主题），缺的不是颜色（Piyo 主题色可选，珊瑚=我们默认即可），**缺的是 Piyo 的视觉辨识结构**。本轮统一到「日系卡哇伊扁平 / Piyo 简单主题」,吉祥物用**栗子**。

**风格定义**：日系卡哇伊扁平（Kawaii flat illustration）—— 圆润造型、点状眼+腮红、无/细描边、柔和卡通阴影、pastel 主题色、圆角 pill、大留白。

**I1. 图标系统**（对齐 Piyo「简单」主题）
- 全 App 功能图标（底栏 4 tab + 快捷记录 + section/操作图标）统一为 **Material Symbols Rounded、filled**,单色染 `ChestnutPalette.Primary`（未选中 `Muted`）,放柔和圆角 tile（圆角 ≥14dp,底 `tone.copy(alpha≈0.16)` 或 `Surface2`）
- 一套统一描边/字重/圆角端点,禁止混风格、禁止 PNG 贴纸切图
- **禁止 imagegen 生成图标**（栅格不一致、不可动态 tint、小尺寸糊,会重蹈 PNG 贴纸坑）

**I2. 主题色顶栏 band**（补 Piyo 辨识度,关键）
- 顶部 app bar 区 + 底部导航区用 `ChestnutPalette.Primary` 实色 band;内容区维持现扁平奶白干净,**别动内容卡**
- 主题色统一走 `ChestnutPalette.Primary` 单 token,便于将来主题色可选

**I3. 吉祥物 = 栗子**
- **用 imagegen 生成**,风格=日系卡哇伊扁平;**透明背景 PNG、≥512px、生 3 版供挑**
- 全 App **只 1 处**:splash 或空态二选一（splash 现有 `chestnut_mascot` 可重绘替换）,其余不出现
- confetti:**Compose 直接绘制几何色块,不生成**,克制,仅空态/里程碑

**I4. 约束（硬性）**
- 不动 Claude 已定结构与逻辑:`WeekCard / Panel / TrendCard / EmptyPanel / SectionHeader / PregnancySummaryPanel`（仅允许图标/顶栏 band 的视觉替换）
- 不动 IA / 导航 / 阶段投影 / 业务逻辑 / OCR / FGR;不引外部字体
- 一个独立 UI-only commit,message「视觉精简:统一卡哇伊扁平图标+主题色顶栏+栗子吉祥物」,不与后端混提交
- 本地 assembleDebug + lintDebug + smoke 全过
- 装机出 before/after:**空态首页 + 有数据首页 + 快捷弹窗 + 底栏**,交 Claude 对 Piyo 比对

#### 给 Codex 的话（直接照做）

> 读 `docs/UI精修交接-给Codex.md` 的 **I 节**,本轮做 I1–I3,合一个 UI-only commit。
> 重点:① 图标全换 Material Symbols Rounded filled、染 `Primary`、统一圆角 tile,**别用 imagegen 生图标**;② 顶栏+底栏加 `Primary` 主题色 band,内容区不动;③ 栗子吉祥物用 imagegen 生 3 版(日系卡哇伊扁平、透明底、≥512px)挑一版,只放 splash 或空态 1 处;confetti 用 Compose 画。
> 硬约束见 I4:别动 WeekCard/Panel/TrendCard/EmptyPanel/SectionHeader/PregnancySummaryPanel 的结构与逻辑,别动 IA/OCR/FGR,本地三件套(assembleDebug/lintDebug/smoke)全过,装机出 before/after 四图交 Claude 比对。

## 验收（Claude review）

| ID | 标准 |
|---|---|
| AC-U1 | 除 1 处品牌栗子外，无吉祥物/贴纸/渐变 |
| AC-U4 | 底部导航及功能图标为统一线性图标，非 PNG 贴纸 |
| AC-U6 | E、F 各一 commit；附 before/after 截图 |
| AC-U7 | 装机回归：功能/阶段投影/OCR/FGR 不因视觉改动回退 |
