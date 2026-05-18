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

## 验收（Claude review）

| ID | 标准 |
|---|---|
| AC-U1 | 除 1 处品牌栗子外，无吉祥物/贴纸/渐变 |
| AC-U4 | 底部导航及功能图标为统一线性图标，非 PNG 贴纸 |
| AC-U6 | E、F 各一 commit；附 before/after 截图 |
| AC-U7 | 装机回归：功能/阶段投影/OCR/FGR 不因视觉改动回退 |
