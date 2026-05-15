# BabyLog UI 设计书

## Status

| 项目 | 内容 |
|---|---|
| 文档状态 | Draft |
| 日期 | 2026-05-15 |
| 作者 | UI draft |
| 适用范围 | 单胎单宝宝单机版 PWA MVP；iOS Safari / installed PWA 优先 |
| 实现技术栈 | Vite + React + TypeScript |
| 配套原型 | `prototype/` 目录下 HTML/CSS 静态原型，可在手机浏览器直接打开 |
| 阅读顺序 | SHOULD 在读完 `项目立项书.md`、`需求评估表.md` 之后阅读 |

## Design Principles

| 原则 | 说明 |
|---|---|
| 像病历本，不像营销页 | 信息密度高、装饰极少；不用粉色、不用插画式 emoji、不用 marketing 风格 hero |
| 单手可达 | 主操作 MUST 落在手机屏幕下 1/3；顶部仅放上下文信息，不放主操作 |
| 夜间不刺眼 | 默认浅色为低饱和米白偏绿；夜间深色 MUST 用近黑底而非纯黑，文字 MUST 用低饱和米白 |
| 数据可信感 | 数字、单位、孕周等关键字段 MUST 使用等宽数字 (tabular-nums)，对齐稳定 |
| 医疗免责常驻 | 曲线、参考提示页面 MUST 在视觉上常驻一条免责说明，而非首次弹窗 |
| 不堆图标 | 用文字胜过用图标；图标 MUST 配合文字标签出现，禁止纯图标导航 |
| 触控宽容 | 主按钮高度 MUST ≥ 44px；并排按钮间距 MUST ≥ 8px |

## Design Tokens

### Color (Light)

| Token | Value | 用途 |
|---|---|---|
| `--bg` | `#F4F6F5` | 页面背景（米白偏绿） |
| `--surface` | `#FFFFFF` | 卡片、表单 |
| `--surface-2` | `#ECF1EF` | 次级容器、分组背景 |
| `--border` | `#E2E7E5` | 分隔线、卡片描边 |
| `--text-1` | `#142823` | 主要文字 |
| `--text-2` | `#4F5E5A` | 次要文字、说明 |
| `--text-3` | `#889390` | 弱化文字、placeholder |
| `--primary` | `#2F8F82` | 主色（青绿） |
| `--primary-soft` | `#D8ECE9` | 主色淡背景、chip |
| `--primary-press` | `#257267` | 按下态 |
| `--accent` | `#6BA8A0` | 强调色（曲线辅线、tag） |
| `--success` | `#4F8B5A` | 完成、确认 |
| `--warning` | `#C28338` | 配额提醒、未导出警告 |
| `--danger` | `#B0524F` | 不可用、超出范围 |
| `--danger-soft` | `#F4DEDC` | 错误字段背景 |
| `--disclaimer` | `#A57B2A` | 医疗免责条带文字 |
| `--disclaimer-bg` | `#FBF1DA` | 医疗免责条带背景 |

### Color (Dark / Night)

| Token | Value | 用途 |
|---|---|---|
| `--bg` | `#0E1715` | 页面背景 |
| `--surface` | `#16221F` | 卡片 |
| `--surface-2` | `#1E2D2A` | 次级容器 |
| `--border` | `#283934` | 分隔线 |
| `--text-1` | `#E8EFEC` | 主要文字 |
| `--text-2` | `#A8B6B1` | 次要文字 |
| `--text-3` | `#6E7C77` | 弱化文字 |
| `--primary` | `#5BB8AB` | 主色 |
| `--primary-soft` | `#1F3A35` | 主色淡背景 |
| `--primary-press` | `#7CC9BE` | 按下态 |
| `--accent` | `#8FC7BF` | 强调色 |
| `--success` | `#79B58A` | 完成 |
| `--warning` | `#D9A56B` | 警告 |
| `--danger` | `#D88582` | 错误 |
| `--danger-soft` | `#3A2421` | 错误字段背景 |
| `--disclaimer` | `#D6B377` | 免责文字 |
| `--disclaimer-bg` | `#2C2516` | 免责背景 |

### Typography

| Token | Value | 用途 |
|---|---|---|
| `--font` | `-apple-system, BlinkMacSystemFont, "PingFang SC", "Microsoft YaHei", "Helvetica Neue", Arial, sans-serif` | 系统字体栈，iOS 用苹方 |
| `--fs-12` | `12px` | 辅助标签、单位 |
| `--fs-14` | `14px` | 正文小、列表副文本 |
| `--fs-16` | `16px` | 正文 |
| `--fs-18` | `18px` | 小标题、强调列 |
| `--fs-22` | `22px` | 页面标题 |
| `--fs-28` | `28px` | 首页大数字（孕周/月龄） |
| `--fw-r` | `400` | 正文 |
| `--fw-m` | `500` | 标签、按钮 |
| `--fw-s` | `600` | 标题、数值 |
| `--num` | `font-variant-numeric: tabular-nums` | 所有数值字段 |

### Spacing

| Token | Value |
|---|---|
| `--sp-1` | `4px` |
| `--sp-2` | `8px` |
| `--sp-3` | `12px` |
| `--sp-4` | `16px` |
| `--sp-5` | `20px` |
| `--sp-6` | `24px` |
| `--sp-8` | `32px` |
| `--sp-10` | `40px` |

### Radius / Shadow

| Token | Value | 用途 |
|---|---|---|
| `--r-sm` | `8px` | 输入框、tag |
| `--r-md` | `12px` | 卡片 |
| `--r-lg` | `16px` | 模态、FAB |
| `--r-pill` | `999px` | 胶囊按钮、chip |
| `--shadow-1` | `0 1px 2px rgba(20, 40, 35, 0.06), 0 1px 1px rgba(20, 40, 35, 0.04)` | 卡片轻投影 |
| `--shadow-fab` | `0 4px 12px rgba(47, 143, 130, 0.25)` | FAB 投影 |

### Touch

| Token | Value | 用途 |
|---|---|---|
| `--touch-min` | `44px` | 任何可点元素最小高度 |
| `--touch-fab` | `56px` | 中央快捷记录 FAB |
| `--touch-gap` | `8px` | 并排可点元素最小间距 |

## Layout System

### 屏幕骨架

```text
┌───────────────────────────────────────┐
│ safe-area-inset-top                   │
├───────────────────────────────────────┤
│ Header                                │  ← 上下文信息（孕周/宝宝日龄/页面标题），可消失
├───────────────────────────────────────┤
│                                       │
│ Content (scrollable)                  │
│                                       │
│   - cards / lists / forms             │
│                                       │
├───────────────────────────────────────┤
│ Bottom Nav (4 tabs + center FAB)      │  ← 始终常驻
├───────────────────────────────────────┤
│ safe-area-inset-bottom                │
└───────────────────────────────────────┘
```

- Content 区 MUST `padding-bottom` 至少 `calc(72px + env(safe-area-inset-bottom))`，避免被底部导航遮挡。
- Header MUST 使用 sticky 定位；向下滚动时 SHOULD 收起副标题以节约空间。
- 主操作 FAB MUST 在底部导航中央，悬浮于 nav 上方约 16px，确保单手拇指可达。

### 底部导航

```text
┌─────┬─────┬───────┬─────┬─────┐
│ 首页 │ 时间线│   +   │ 资料 │ 设置 │
└─────┴─────┴───────┴─────┴─────┘
        ↑       ↑      ↑
        Tab    FAB    Tab
```

| 项 | 标签 | 说明 |
|---|---|---|
| 1 | 首页 | 孕周/月龄、今日摘要、快捷入口 |
| 2 | 时间线 | 全部记录倒序，支持阶段/类型筛选 |
| 中 | + (FAB) | 弹出快捷记录抽屉 |
| 3 | 资料 | 附件、B 超单、检查单、出生资料、曲线入口 |
| 4 | 设置 | 档案、导入导出、同步配置、关于 |

> Tab 标签 MUST 与图标同时存在；纯图标 tab 不允许。

### 快捷记录抽屉（FAB 展开）

底部弹出 sheet，6 个高频入口排成 2 行 3 列；MUST 包含：
- 喂养 / 睡眠 / 尿布 / 体温 / 用药 / B 超
- 每个入口为 ≥ 64×64 的方形卡片，含图形 + 标签
- 顶部一条横拉手柄；点 sheet 外区域或下拉关闭

## Component Inventory

### 基础组件

| 组件 | 必备状态 | 说明 |
|---|---|---|
| `Button` | default / press / disabled / danger | 高度 44/52，主按钮填充色；次按钮描边；不允许小按钮承担主操作 |
| `IconButton` | default / press | 仅次级用途，必须配合上下文文字 |
| `Card` | default / pressable | 默认描边 + 微投影；pressable 卡片整张可点 |
| `ListRow` | default / press / destructive | 高度 ≥ 56；左侧标签、右侧值或箭头 |
| `Field` | idle / focus / error | 表单输入；error 时字段背景 `--danger-soft`，下方红字说明 |
| `UnitInput` | — | 数值 + 单位徽标合体；单位通过 props 固定，不允许字段间漂移 |
| `Chip` | default / selected | 胶囊形态；用于阶段筛选、类型筛选 |
| `Tag` | default / warning / danger | 静态标签，不可点 |
| `Modal/Sheet` | bottom-sheet / center-modal | 仅底部 sheet 用于快捷记录；center modal 用于二次确认 |
| `Toast` | info / success / warning / danger | 顶部出现，3 秒自动消失；不打断操作 |
| `Disclaimer` | inline / pinned | 曲线、参考提示页 MUST 常驻 pinned 形态 |
| `EmptyState` | — | 空时间线、空附件库等；MUST 包含一句"做什么"指引 |

### 复合组件

| 组件 | 说明 |
|---|---|
| `WeekBadge` | 大数字 + "孕周"/"月龄"标签；首页主信息 |
| `QuickRecordGrid` | 6 格快捷记录入口 |
| `TimelineItem` | 时间 + 类型徽标 + 摘要 + 可选附件缩略；左侧时间线轴 |
| `TrendCard` | 标题 + 最新值 + 迷你折线；点进入详细曲线页 |
| `UltrasoundFieldGroup` | 按 `UltrasoundExam Fields` 表渲染字段；含单位、软范围校验 |
| `AttachmentTile` | 附件缩略 + 文件名 + 大小；长按弹出删除/导出 |
| `BackupStatusCard` | "距上次导出 N 天" + 立即导出按钮；阈值超过 MUST 用 `--warning` |
| `StorageQuotaCard` | 已用/总配额条 + 大于 70% 黄色、大于 90% 红色 |
| `SyncConfigCard` | base URL / 地域 / 启用开关；启用前 MUST 弹"跨境医疗数据"确认 |

## Screen Specs

### S-01 首页 Home

```text
┌─────────────────────────────────────┐
│  孕 28⁺³ 周                          │  ← WeekBadge (--fs-28)
│  距预产期 82 天                       │
├─────────────────────────────────────┤
│ 今日摘要                              │
│  喂养 6 次 ・ 睡眠 14h ・ 尿布 8 次     │  ← 出生后呈现；孕期阶段展示"最近产检"
├─────────────────────────────────────┤
│ ┌────┬────┬────┐                   │
│ │喂养│睡眠│尿布│                   │
│ ├────┼────┼────┤   QuickRecordGrid │
│ │体温│用药│B超│                   │
│ └────┴────┴────┘                   │
├─────────────────────────────────────┤
│ 最近记录                              │
│  ・ 16:20 喂养 母乳 120 ml           │  ← 最多 5 条；点进入时间线
│  ・ 14:00 睡眠 已结束 1h30          │
│  ・ 09:30 B 超 28⁺³周               │
│  ─────── 全部记录 →                  │
├─────────────────────────────────────┤
│ 趋势                                  │
│ ┌─────────────┐ ┌─────────────┐    │
│ │ 胎儿 EFW    │ │ 体重         │    │  ← TrendCard ×2，点进入曲线页
│ │  1320 g    │ │  —           │    │
│ │  ╱╱╱╲      │ │              │    │
│ └─────────────┘ └─────────────┘    │
└─────────────────────────────────────┘
```

| 区域 | 行为 |
|---|---|
| WeekBadge | 孕期：显示孕周和距预产期；出生后：显示日龄/月龄 |
| 今日摘要 | 日界默认 00:00，可在设置改 04:00（CORE-13） |
| QuickRecordGrid | 6 个高频入口；点击弹出对应表单 sheet |
| 最近记录 | 与时间线共享样式，限 5 条 |
| 趋势 | 阶段相关：孕期出 EFW/BPD/HC；出生后出体重/身高/头围 |

### S-02 时间线 Timeline

```text
┌─────────────────────────────────────┐
│ 时间线                       筛选 ⌄  │  ← Header
├─────────────────────────────────────┤
│ [ 全部 ] [ 孕期 ] [ 育儿 ]           │  ← 阶段 Chip
│ [ 喂养 ] [ 睡眠 ] [ 尿布 ] ...       │  ← 类型 Chip（横向滑动）
├─────────────────────────────────────┤
│ 今天                                 │
│ │                                   │
│ ●  16:20  喂养 母乳 120 ml           │
│ │   备注: 吃后吐了一点                │
│ ●  14:00  睡眠 1h30                  │
│ │                                   │
│ ●  09:30  B 超 28⁺³周                │
│ │   EFW 1320 g · BPD 71 mm  📎       │
│                                     │
│ 5 月 14 日                           │
│ ●  21:00  体温 37.2 ℃                │
│ │                                   │
└─────────────────────────────────────┘
```

| 区域 | 行为 |
|---|---|
| 阶段/类型 Chip | 多选；选中状态用 `--primary-soft` 背景 |
| 时间线条目 | 左侧细线 + 圆点轴；点条目进入详情；附件徽标 `📎`（用图形非 emoji） |
| 日期分组 | sticky 日期标题；今天 / 昨天 / 具体日期 |
| 空态 | "还没有记录。点 + 开始" |

### S-03 B 超录入 Ultrasound

```text
┌─────────────────────────────────────┐
│ ← 新建 B 超                          │  ← Header 含返回
├─────────────────────────────────────┤
│ ┌─────────────────────────────────┐ │
│ │     +  拍照 / 选图              │ │  ← 点击调用 input[capture]
│ │                                 │ │
│ └─────────────────────────────────┘ │
│                                     │
│ ★ 仅供记录和参考，不能替代医生判断    │  ← Disclaimer pinned
│                                     │
│ 检查日期       2026-05-15           │
│ 孕周           28 周 + 3 天          │
│                                     │
│ BPD            [   71  ] mm          │  ← UnitInput
│ HC             [  265  ] mm          │
│ AC             [  240  ] mm          │
│ FL             [   54  ] mm          │
│ EFW            [ 1320  ] g           │
│ AFI            [  140  ] mm          │
│                                     │
│ 胎盘位置        前壁                  │
│ 胎盘成熟度       I                   │
│ 胎位            头位                  │
│                                     │
│ 备注（医生口头建议等）                │
│ ┌─────────────────────────────────┐ │
│ │                                 │ │
│ └─────────────────────────────────┘ │
│                                     │
├─────────────────────────────────────┤
│              [ 保存到时间线 ]         │  ← 主按钮粘底
└─────────────────────────────────────┘
```

| 区域 | 行为 |
|---|---|
| 照片区 | 支持多张；保存前 MUST 客户端压缩（最长边 2048，JPEG q=0.8） |
| Disclaimer | 常驻条带，MUST 不可关闭 |
| 字段表 | 直接对应 `UltrasoundExam Fields` 表；单位徽标固定不可改；超出软范围 MUST 字段变 `--danger-soft` + 提示，但允许保存（医生可能给特殊值） |
| 备注 | 多行；自动保留为草稿 |
| 保存按钮 | 粘底；安全区适配；保存后回到时间线并 toast |

### S-04 成长曲线 Growth

```text
┌─────────────────────────────────────┐
│ 曲线                  胎儿 ⌄ 体重 ⌄  │
├─────────────────────────────────────┤
│ ★ 仅供记录和参考，不能替代医生判断    │
├─────────────────────────────────────┤
│                                     │
│    g                                 │
│  2000│             ●                │
│  1500│       ●                       │
│  1000│   ●                          │
│   500│●                              │
│      └──┬──┬──┬──┬──┬──┬──         │
│         24 26 28 30 32 34  周         │
│                                     │
│ 当前 EFW 1320 g （28⁺³ 周）           │
│                                     │
│ 历史记录                              │
│  28⁺³ 1320 g                         │
│  26⁺²  920 g                         │
│  24⁺¹  680 g                         │
└─────────────────────────────────────┘
```

| 区域 | 行为 |
|---|---|
| 切换器 | 阶段（胎儿/出生后）+ 指标（EFW/BPD/HC 或 体重/身高/头围） |
| Disclaimer | 常驻 |
| 曲线 | MVP 只画用户数据点+连线；P3/P10/P50/P90 参考带为 P1，预留容器 |
| 历史 | 倒序点列表，可点编辑 |

### S-05 设置 Settings

```text
┌─────────────────────────────────────┐
│ 设置                                 │
├─────────────────────────────────────┤
│ 档案                                 │
│  ▸ 宝宝/妊娠档案                     │
│  ▸ 预产期 / 出生日                   │
├─────────────────────────────────────┤
│ 数据                                 │
│  💾 距上次导出 12 天 ・ [立即导出]    │  ← BackupStatusCard，>7 天黄色提示
│  📊 已用 312 MB / 约 1 GB            │  ← StorageQuotaCard
│  ▸ 从备份导入                        │
│  ▸ 清空本地数据                      │
├─────────────────────────────────────┤
│ 显示                                 │
│  ▸ 日界  自然日 00:00                │
│  ▸ 主题  跟随系统                    │
├─────────────────────────────────────┤
│ 同步（未配置）                        │
│  状态: 未配置                        │
│  地址: —                            │
│  地域: —                            │
│  [ 配置后端 ]                        │  ← 点击进入 SyncConfigCard
├─────────────────────────────────────┤
│ 关于                                 │
│  版本 0.1.0 ・ MVP                   │
│  ▸ 医疗免责声明                      │
│  ▸ 隐私说明                          │
└─────────────────────────────────────┘
```

## Interaction Patterns

| 模式 | 要求 |
|---|---|
| 草稿保留 | 任何表单切走 MUST 自动保留草稿到 IndexedDB；下次进入 MUST 恢复并标记"草稿" |
| 单位强制 | UnitInput 单位 MUST 由 schema 决定，不允许字段间漂移；NFR-06 |
| 软范围越界 | 越界字段红框提示但允许保存；MUST 在保存确认 modal 中复述越界字段 |
| 删除二次确认 | 任何记录删除 MUST 弹 center modal 二次确认；可撤销 5 秒（toast） |
| 网络感（其实没有） | 所有保存 MUST 立刻成功且本地落地；MUST NOT 显示"上传中"等误导词 |
| 同步配置启用 | 用户点"启用同步" MUST 弹"跨境医疗数据"声明 modal，单独勾选确认 |
| 安装引导 | iOS Safari 非 standalone 状态 MUST 在首页底部出"添加到主屏幕"轻提示，可关闭一次保留 7 天 |
| 持久化申请 | 创建档案后 MUST 调用 `navigator.storage.persist()`，结果以 toast + 设置页徽标显示 |
| 导出提醒 | 距上次导出 ≥ 7 天 MUST 在设置首屏黄色提示；≥ 30 天 MUST 在首页顶部出红条 |
| 夜间模式 | MUST 跟随系统 prefers-color-scheme；MAY 提供强制开关 |

## iOS-First Constraints

| 项 | 要求 |
|---|---|
| Viewport | `<meta name="viewport" content="width=device-width, initial-scale=1, viewport-fit=cover">` |
| Safe area | 顶/底 MUST 使用 `env(safe-area-inset-*)`；底部导航 MUST 在 home indicator 之上 |
| Standalone | `apple-mobile-web-app-capable=yes`；`apple-mobile-web-app-status-bar-style=black-translucent` |
| Theme color | MUST 设置 `<meta name="theme-color">`，浅色 `#F4F6F5`、深色 `#0E1715` |
| Touch | 禁用 `-webkit-tap-highlight-color`（设为 transparent）；自定义 press 态 |
| 输入键盘 | 数值字段 MUST 用 `inputmode="decimal"` 弹出数字键盘；日期用 `<input type="date">` 但提供 native picker fallback |
| 相机调用 | B 超照片 MUST 用 `<input type="file" accept="image/*" capture="environment">` |
| 滚动 | iOS 惯性滚动默认 OK；MUST NOT 用第三方滚动库覆盖原生行为 |
| 字体 | MUST 用系统字体栈，不自带 web font；保证离线和加载性能 |
| 长按菜单 | MUST 禁用图片默认长按菜单（`-webkit-touch-callout: none`）以便实现自定义附件操作 |

## Prototype Index

`prototype/` 目录下的静态 HTML 实现了上面 5 个屏幕。可以：

- 在电脑浏览器打开 `prototype/index.html`，缩窄到 ~390 px 宽预览。
- 用电脑起一个静态服务器（如 `npx serve prototype`），手机连同一 WiFi 用 `http://<电脑 IP>:3000` 打开。
- 或把整个 `prototype/` 拷到手机 iCloud / 微信收藏，用 iOS Safari 打开 `index.html`。

| 文件 | 对应屏幕 |
|---|---|
| `prototype/index.html` | 屏幕索引，列出所有页面 |
| `prototype/styles.css` | 共享 design tokens + 组件样式 |
| `prototype/home.html` | S-01 首页 |
| `prototype/timeline.html` | S-02 时间线 |
| `prototype/ultrasound.html` | S-03 B 超录入 |
| `prototype/growth.html` | S-04 成长曲线（含静态 SVG） |
| `prototype/settings.html` | S-05 设置 |

> 原型 MUST NOT 接任何数据；所有内容为示意；按钮无真实跳转目标时落回 index。

## Implementation Notes For React + Vite

| 关注点 | 建议 |
|---|---|
| 项目脚手架 | `npm create vite@latest babylog -- --template react-ts` |
| PWA 插件 | `vite-plugin-pwa`；MUST 自定义 manifest（中文名、theme_color、apple-touch-icon） |
| 路由 | React Router v6；MUST 使用 hash 路由或 base path 兼容 iOS standalone 刷新 |
| 状态管理 | MVP 用 Zustand 即可，避免 Redux 重量级；本地仓库层独立模块 |
| IndexedDB | 推荐 `idb` 库（薄封装），MUST 自己写 Repository 抽象，不要让 UI 直接调 IDB |
| 图表 | Chart.js 4（gzip ~50KB+）；MUST 用 `react-chartjs-2` 适配；坚守 ≤ 300KB gzip 预算 |
| 表单 | 用 React Hook Form + Zod 校验；UltrasoundExam 字段单独 schema 文件 |
| 主题 | CSS 变量切换 + `prefers-color-scheme`；不引入 CSS-in-JS |
| 测试 | Vitest + Playwright；P0 流程在 iOS Safari + Android Chrome 各跑一次 |
| 体积预算 | 首屏 JS gzip ≤ 200KB；图表懒加载 |

## Open Questions

| ID | 问题 | 默认处理 |
|---|---|---|
| UI-Q-01 | 是否需要"快速记录"在锁屏 widget / 通知 | 不在 MVP；PWA 暂不支持 |
| UI-Q-02 | 首页趋势卡是 2 个固定还是用户可选 | MVP 固定，按阶段切换内容 |
| UI-Q-03 | 是否需要"宝宝头像"展示位 | 不在 MVP；过度装饰，留到 P2 |
| UI-Q-04 | 是否在 standalone 时显示自定义状态栏色块 | iOS 由 `theme-color` 控制；不再额外做 |
| UI-Q-05 | 多语言 | 暂仅中文；i18n 框架不在 MVP |
