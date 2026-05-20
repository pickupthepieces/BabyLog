# UI-X 图标系统切换到 Piyo「插图」风（混合策略，给 Codex）

| 项目 | 内容 |
|---|---|
| 作者 | Claude (Opus 4.7) |
| 日期 | 2026-05-20 |
| 触发 | 用户："Codex 能用 gpt-image-2 做 imagegen，现在 Material 图标太普通，切到 Piyo 插图风" |
| 排期 | **G3 LMP 计算器 → G9 复诊汇总 → 本项**（用户拍板）；UI 打磨轮(P5-V+Q4)与 G3/G9 完成后启动 |
| 性质 | v1 视觉收口；非急迫，但是把"普通感"换成"Piyo 卡哇伊辨识度"的最大杠杆 |

## 一、定调（不全切，混合）

| 区域 | 用什么 | 理由 |
|---|---|---|
| **快捷记录磁贴 / 栗子吉祥物 / 空态插画** | **imagegen 生成 Piyo 卡哇伊插图风贴纸** | 品牌/情感区，"普通感"主要来源；尺寸大、对一致性敏感度低、无需动态 tint |
| **BottomNav 4 tab + 设置/返回/编辑/删除/分享/搜索/添加 等功能性图标** | **保持 Material Symbols Rounded** | 全 App 几十处，需统一描边/字重/对齐/可染色；imagegen 不擅长这类小尺寸高一致性图标，会重蹈"贴纸不统一"老坑 |

## 二、imagegen 一致性纪律（硬约束，不守则返工）

1. **同一 prompt 模板批量出图**：所有贴纸用统一模板生成，**禁止分次随手生**；每次新加一个 subject 走同一参数。
2. **固定风格关键词**：`flat kawaii vector sticker, Japanese cute style, rounded shapes, soft pastel palette (coral/cream/blue/green/peach), single subject centered, no outline or thin outline, slight cartoon shading, transparent PNG background, 512×512, pregnancy/family themed`，subject 填具体词。
3. **输出规范**：512×512 透明背景 PNG；色板与 `ChestnutPalette` 主调相符（暖色 + 柔粉/米/淡蓝/淡绿/桃色）；构图居中、留 ~8% 边距。
4. **不在 App 内动态 tint**：贴纸自带颜色；选中态用**微缩放/轻阴影**或外圈光晕，**禁止染色变体**。
5. **一致性 review**：Codex 出完一批先发 Claude 看一致性（风格/尺度/色板），不齐重出。
6. **资产路径**：`android-native/app/src/main/res/drawable-nodpi/sticker_*.png`（与 `chestnut_mascot.png` 同级，命名 `sticker_b_chao` / `sticker_checkup` / 等，用 ASCII 拼音便于工具链）。

## 三、本轮要做的贴纸清单（v1 范围）

**快捷记录磁贴(5 张)**：B 超 / 产检 / 胎动 / 宫缩 / 孕妈指标。subject 提示词例：
- B 超：超声仪 + 可爱胎儿轮廓
- 产检：医生听诊器/夹板 + 心形
- 胎动：圆肚 + 小脚踢动
- 宫缩：圆肚 + 计时器(中性，不含医疗符号)
- 孕妈指标：孕妈剪影 + 小体重秤

**栗子吉祥物 v2(1 张)**：现 splash 的 `chestnut_mascot.png` 风格升级——经典栗子轮廓(圆润棕色上半 + 米色下半)、细白贴纸描边 + 柔和阴影、米色下半保持两色干净不加颗粒/纹理、**透明背景**、**纯物件型禁拟人**。

> **用户决策(2026-05-20 终版):栗子也禁拟人。**
> - 纯物件型美学上更稳更"高级",和"家庭记录工具"的克制气质更搭;拟人卡哇伊偏 toC 母婴商品味。
> - 栗子贴纸**禁用**任何面部(点眼/腮红/嘴/表情)、手脚、拟人轮廓——只靠形状/色板/描边卖萌。
> - 5 张快捷贴纸继续强制纯物件型(同前)。
> - 之前 v2 contact sheet 已通过的栗子版(含简单脸)**需 reroll 为纯物件版**,其余 4 张 + 已 reroll 的 B超/孕妈指标保留不变。

**screening_* / 空态 / 其它**：**v1 不做**(贴纸越多越难保持一致；先把核心 5 + 1 出来跑通)。

## 四、接线（QuickRail.kt 替换）

- 现 `PersistentQuickRail` 每项渲染靠 `BabyLogIconTile(icon=quickActionIcon(eventType), tint=toneColor, tileColor=…)` 或 P5-V 改后的圆形 box+Material icon。
- 切贴纸：在 `quickActionIcon` 旁加 `quickActionSticker(eventType): Painter?`（基于 `painterResource(R.drawable.sticker_…)`）；rail 渲染优先用贴纸；无贴纸则 fallback 到 Material icon(过渡期/未覆盖类型)。
- 贴纸渲染直接 `Image(painter, contentDescription, modifier=Modifier.size(40-44.dp))`——**无外层圆形背景、无 tint**(P5-V 圆形 box 在贴纸下可去掉，让贴纸自身的形状/色彩说话)。
- BottomNav / 设置/编辑/删除/分享/搜索/添加 / 详情页等仍用 `BabyLogIconTile` + Material icon，**不动**。
- `chestnut_mascot.png` 升级版替换 splash 引用；不动 splash 其它结构。

## 五、医疗安全 / 文案

- 贴纸是装饰，不承载诊断信息，不替代提示文案；与 `DISCLAIMER.md` 无关，但保持"工具/记录"克制气质，不要画成"医生卡通拍胸脯安心"那种暗示诊断的画面。
- subject 提示词避免：医生头像/听诊器贴在肚子上的特写/任何看着像"App 在诊疗"的构图。

## 六、不做（v2 或后续）

- screening_* 7 类贴纸(若 G3/G9 后还想要再加，按同一 prompt 模板补)
- 空态插图(empty state)、里程碑插图
- 多套主题色/动态主题色(用户单一主题足够)

## 七、验收

- 5 张快捷贴纸 + 栗子升级 (v2 主版) 一致性高(同人物比例/同线宽/同色板)
- QuickRail 渲染替换为贴纸,无 tint 染色,选中态不靠染色
- BottomNav/其它功能图标 Material Rounded 未动
- splash 用升级版栗子
- 不动数据/事件模型/FGR/确认链/已锁视觉 token(配色除外:贴纸自带不染色)
- assemble+lint+smoke 绿
- 装机走查首页 quick rail 与 splash;Claude 一致性 review 通过

## 八、给 Codex 的话(到时直接转发)

> 读 `docs/UI-X图标系统切Piyo插图风-给Codex.md` 全文。本轮:用 gpt-image-2 (imagegen) 生成 5 张快捷记录贴纸 + 栗子吉祥物 v2 主版，全部同一 prompt 模板批量出图(参数见第二节),透明背景 PNG ≥512px,无 tint;QuickRail.kt 渲染从 Material icon 切到贴纸(无外圈染色);BottomNav 与功能性图标保持 Material 不动。出完先发 Claude 看一致性再装机回归。一笔 `feat:` 提交(可拆"resources" + "wire up"两笔,各独立、不混 G3/G9、不并 main),assemble+lint+smoke 绿。

## 副线 staging 资产批次 — 已通过一致性 review（2026-05-20）

**已就绪资产**（`diagnostics/ui-x-stickers/`，6 张 512×512 RGBA 透明 PNG + contact sheet）：
- `sticker_b_chao.png` / `sticker_checkup.png` / `sticker_fetal_movement.png` / `sticker_contraction.png` / `sticker_maternal_metric.png` / `chestnut_mascot_v2.png`

**审计过程**：v1 contact sheet(4488caa) → B超含底座栗子角色 + 孕妈指标含完整人物 → reroll 这 2 张(2544635) → v2 contact sheet 通过。整套风格一致(白厚边/柔阴影/暖珊瑚色板/统一线宽)。

**主线 UI-X 接线时**(G9 之后启动)：
- 把 6 张 PNG 从 `diagnostics/ui-x-stickers/` 复制到 `android-native/app/src/main/res/drawable-nodpi/`。
- `QuickRail.kt` 渲染 5 张快捷贴纸替换 Material icon，无 tint，无外圈染色。
- `splash` 用 `chestnut_mascot_v2.png` 替换现 `chestnut_mascot.png`(或并存,splash 切引用即可)。
- BottomNav / 设置/编辑/删除等功能性图标保持 Material Rounded **不动**。
- 应用启动图标 `mipmap-*/ic_launcher*` 已于 `7d10c9b` 由用户授意一并更新,主线 UI-X 不必再动该部分,只做 in-app 贴纸接线。

## 主 Logo / Launcher Icon 最终决策（2026-05-20）

**用户拍板：BabyLog App 主 logo / Android launcher icon 使用 `diagnostics/app-icon-update/chestnuticon.jpg` 这张栗子图。**

资产口径：
- Canonical 视觉源图：`diagnostics/app-icon-update/chestnuticon.jpg`（1254×1254）。
- 高清导出版：`diagnostics/app-icon-update/chestnuticon-4096.png`（4096×4096，确定性放大，不经 imagegen 重绘）。
- 兼容备用导出版：`diagnostics/app-icon-update/chestnuticon-4096.jpg`。
- 当前 Android launcher mipmap 资源已由该图生成，详见 `diagnostics/app-icon-update/CLAUDE_REVIEW.md`。

设计定稿特征：
- 单主体栗子，非拟人化，无眼睛、嘴、腮红、手脚或配饰。
- 日系卡哇伊扁平贴纸风；圆润 Q 感；暖栗棕主体 + 奶油色底部斑块 + 细白贴纸边 + 轻微贴纸阴影。
- 背景为温柔栗粉/奶茶玫瑰色系，整体用于 app 主图标时保持当前构图与比例。

后续约束：
- **不要再用 imagegen 重新生成主 logo 来替换当前版本**，除非用户明确要求重新设计；imagegen 容易改变主体轮廓、居中和背景色。
- 如需不同尺寸，优先从上述源图/4096 PNG 做确定性 resize/crop。
- UI-X 后续只处理 in-app 贴纸和快捷入口图标，不应顺手替换 `mipmap-*/ic_launcher*`。

## Splash 改用主 Logo（2026-05-20 后续）

**用户决策**：Splash 也要体现主 logo,与桌面 launcher icon 一致,品牌印象统一。当前 splash 用的是 in-app 贴纸版 `chestnut_mascot_v2`,需切到 canonical 主 logo。

**做法（一笔小 `ui:` 提交）**：
1. 从 canonical 源做**确定性 resize**生成 `android-native/app/src/main/res/drawable-nodpi/chestnut_main_logo.png`:
   - 源:`diagnostics/app-icon-update/chestnuticon-4096.png`(优先)或 `chestnuticon.jpg`
   - 目标尺寸:**~1024×1024 PNG**(splash 显示 ~238dp 够用;不必塞 3.7MB 高清进 APK)
   - 工具:任意确定性 image-resize(ffmpeg/PIL/imagemagick/Android Studio Image Asset 都行),**禁止用 imagegen 重生**——保主体轮廓、居中、背景色完整保留。
   - 目标文件大小 ≤500KB(若超,提示并说明)。
2. `SplashActivity.kt`:
   - line 123 `painterResource(R.drawable.chestnut_mascot_v2)` → `painterResource(R.drawable.chestnut_main_logo)`
   - **移除外层白圈 backdrop**(line 89-94 `Box(size=274dp).clip(CircleShape).background(Color.White α0.14)`)**和内层白 Surface 圆**(line 95-108 `Surface(shape=CircleShape, color=White α0.96, size=238dp)`)——主 logo 自带 sticker 框与背景,再套白圈视觉冗余。
   - 直接 `Image(painter=chestnut_main_logo, size≈238dp)` 居中在 coral primary splash bg 上,就是放大版桌面图标观感。
3. **`chestnut_mascot_v2.png` 用途排查**:
   - 全局 grep `R.drawable.chestnut_mascot_v2` / `chestnut_mascot_v2` 找其它引用。
   - 若仅 splash 在用 → 顺手删 res/ 里的 `chestnut_mascot_v2.png` 收小 APK(staging `diagnostics/ui-x-stickers/chestnut_mascot_v2.png` 保留作历史)。
   - 若别处还在用(如空态/资料库) → 保留不动,只切 splash。

**约束**:仅 `SplashActivity.kt` + `res/drawable-nodpi/chestnut_main_logo.png`(新增)± `chestnut_mascot_v2.png`(条件删除);不动 launcher icon mipmap(`7d10c9b` 已就绪)、不动 QuickRail/in-app 贴纸接线、不动其它视觉。assemble+lint+smoke 绿;装机看 splash 显主 logo + 跳主页正常。
