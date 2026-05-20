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

## Splash 二次修正（80189af 后,2026-05-20）

**用户反馈**:logo 显示 OK,但 (1) logo 自带粉底套在 coral splash bg 上视觉打架(粉格子贴 coral);(2) Codex 把白圈和字标 pill 都删了,剩一个孤零零小 logo,**像半成品**——Q4 当初决策是"不要冗余文案、**至多应用名 logo**",字标本来允许,被砍过头。

**做法（一笔小 `ui:` 提交,只动 SplashActivity.kt ± palette token）**:

1. **Splash bg 改为与主 logo 自带 bg 同色**(栗粉/奶茶玫瑰色系)——logo 和 bg 融为一体,不再像贴片:
   - 采源图 `diagnostics/app-icon-update/chestnuticon-4096.png` 角落像素(逐角采样取众数/中位,确定性,不靠肉眼猜)定准 bg hex。
   - 增加 `ChestnutPalette.SplashBg`(或 inline 一个 const 也行)用于 splash bg + `window.statusBarColor` + `window.navigationBarColor` 同步改色。
   - confetti backdrop 保留(对的);其颜色 alpha 可微调到新 bg 上仍可见但不抢戏。
2. **加回克制的应用名字标**:logo 下方居中显示 "BabyLog" 文字,SemiBold 16-18sp,色用与新 bg 协调的 `ChestnutPalette.Ink`(或低调 muted)。**就这一行字,不要副标题/不要 tagline**——符合 Q4 "至多应用名 logo"决策。
3. **logo + 字标整体居中**(建议 logo ≈260-280dp + Spacer ≈18-24dp + 字标),构图有重心感,不再空荡。
4. 保留 1.1s 跳主页;不改任何业务逻辑。

**MUST 不变**:不用 imagegen 重生 logo;不动 launcher mipmap / QuickRail / in-app 贴纸接线 / 其它视觉。assemble+lint+smoke 绿;装机看 splash:bg 与 logo 融为一体、字标克制、整体不再像半成品。Claude 验收看一张 splash 截图即可。

### Splash 终版决策（用户裁定,取代上节"confetti 保留 + 字标"做法）

**用户终版口径**:Splash 极简 = **只一个 logo + 一个应用名,其它全删**——confetti 背景/装饰/任何额外元素都不要。

**取代上节"Splash 二次修正"中的两条**:
- 取消"confetti backdrop 保留"→ **删 `SplashBackdrop` composable 与 `ConfettiMark` data class**;为 backdrop 服务的 import(`Canvas`/`drawCircle`/`drawRoundRect`/`rotate`/`Offset`/`Size`/`CornerRadius`)一并清。
- "字标"保留(就一行 "BabyLog",同前规)。
- "bg 与 logo 自带 bg 同色"保留(同前规,源图采样)。

**最终 splash 内容(以此为准)**:
1. Box(fillMaxSize, background=SplashBg采样色, contentAlignment=Center)
2. Column(居中):
   - `Image(R.drawable.chestnut_main_logo, size≈260-280dp)`
   - `Spacer(18-24dp)`
   - `Text("BabyLog", SemiBold 16-18sp, color=Ink 或低调 muted)`
3. **不要其它任何元素**——没有 confetti、没有副标题、没有 tagline、没有白圈、没有阴影、没有装饰。
4. window status/nav bar color 与 SplashBg 一致;保留 1.1s 跳主页。

**约束不变**:不用 imagegen 重生 logo;不动 launcher mipmap / QuickRail / in-app 贴纸接线 / 其它视觉。assemble+lint+smoke 绿;装机一张 splash 截图给 Claude 验收。

### Splash 终版补丁:中英双名(用户裁定)

**用户终版口径**:显示名 = **"BabyLog · 栗记"**(英主中辅,栗记绑栗子 logo 提供中文识别);applicationId / 包名 / GitHub repo **保持不变**(技术 ID 不动)。

**对终版 splash 的覆盖**:
- 字标从 "BabyLog" 单行 改为 **双行**(更易读、更"成套"):
  - 第一行 "BabyLog" SemiBold ~22sp,色 `ChestnutPalette.Ink`
  - 第二行 "栗记" Normal/Medium ~14sp,色 `ChestnutPalette.Muted`(或低调灰)
  - 两行间 `Spacer(4.dp)` 紧贴
- 或单行 "BabyLog · 栗记"(用 `·` 间隔,SemiBold ~18sp,Ink)——Codex 自选,以视觉重心更稳的为准,**统一就行**。

**同笔提交一起改**:
- `AndroidManifest.xml` 的 `android:label` (或 `strings.xml` 的 `app_name`) → "BabyLog · 栗记"(launcher 桌面图标下文字也变)。
- 任何用户可见处沿用同样双名(README/DISCLAIMER 标题区适度补副标,可后续 docs 跟进,不强求本笔)。

**MUST 不变**:applicationId `app.babylog.nativeapp`、Kotlin/Java 包名、GitHub repo 路径、所有 import、smoke 测试等技术 ID 全部不动——本笔纯**显示名**变更。assemble+lint+smoke 绿,装机看 splash + 桌面 launcher label 都显双名。

### 命名最终布局校准（取代上节"manifest label 改为 BabyLog · 栗记"）

**用户裁定:双名只出现于 splash 一次,其余地方各保单名,避免到处中英文双显的视觉噪声。**

| 位置 | 显示 |
|---|---|
| Splash 字标(品牌介绍场景) | **双名**:BabyLog(SemiBold 22sp Ink) + 栗记(Medium 14sp Muted) |
| **Android Launcher 桌面 label**(`android:label` / `strings.xml app_name`) | **单名 "栗记"**(2 字最紧凑,避免被 launcher 空间截断) |
| App 内 TopBrandBand 顶栏标题 | **单名 "BabyLog"**(保持现状,不动) |
| README H1 / 主要文档 | H1 可加副标"栗记"一次,正文沿用 BabyLog |

**对前节"manifest label 同步改为 BabyLog · 栗记"的取代**:
- `AndroidManifest.xml` 的 `android:label`(或 `strings.xml` 的 `app_name`)= **"栗记"**(只 2 字,非"BabyLog · 栗记")。
- splash 字标 仍按前节双名实现(BabyLog 主 + 栗记 辅)。
- App 内 TopBrandBand 字符串保持现有"BabyLog",**本笔不动**。

**MUST 不变**:applicationId `app.babylog.nativeapp` / 包名 / GitHub repo / import / smoke 等技术 ID 一切不动。Codex 把 splash 双名 + launcher label 改"栗记" + 删 backdrop + bg 同色 一起做成一笔 ui:。

### Splash 字标改用 imagegen 设计字标（用户裁定）

**用户决策:gpt-image-2 中文出图已可用,两个字标都做成设计款 PNG。** 取代之前 splash 用 Compose `Text` 渲染字标的方案;launcher label 仍是字符串"栗记"不变(Android 平台限制,桌面 label 只能是 strings.xml unicode)。

**做法**:

副线 imagegen 生成 2 张独立透明 wordmark PNG → staging `diagnostics/ui-x-stickers/`:

| 文件 | 用途 | 尺寸建议 |
|---|---|---|
| `wordmark_babylog.png` | splash 主字标 "BabyLog" | ~1024×360,横向 wordmark 宽高比 |
| `wordmark_lijji.png` | splash 辅字标 "栗记" | ~512×280,稍小,与 BabyLog 风格协调 |

**统一 prompt 模板**(与之前贴纸批次同源风格,但目标是 wordmark 非物件):
```
Flat kawaii vector wordmark logotype, Japanese cute style with rounded letterforms,
soft pastel palette (warm ink color on transparent background, optional subtle coral accent),
single line of letters/characters, generous letter-spacing, no outline or very thin outline,
slight cartoon flatness, transparent PNG background, friendly and warm pregnancy/family brand feel.
Word: "<BabyLog | 栗记>".
```

**约束**:
- 两张同 prompt 模板批量出,**风格必须成套**(同字重感、同色温、同圆度感、同 letterform 处理)。
- 透明背景 PNG,字形清晰锐利;**中文必须字形正确**——任一字形错(如不是"栗记"两字)就 reroll。
- 颜色用 Ink 偏暖深棕(与 splash bg 栗粉协调对比);**不带 tint 不带 shadow box**。
- 出完两张 + 更新 `_contact_sheet.png` 一起提交副线 staging,发 Claude 看一致性 + 字形正确。

**主线接线**(副线 PNG 通过 review 后做一笔 ui:):
- 复制两张 wordmark PNG 到 `res/drawable-nodpi/`。
- `SplashActivity.kt` 字标区从 `Text("BabyLog"…) + Text("栗记"…)` 改为 `Image(painterResource(R.drawable.wordmark_babylog), 适当宽度) + Spacer(8-12dp) + Image(painterResource(R.drawable.wordmark_lijji), 较小宽度)`。
- launcher `android:label` / `strings.xml app_name` = `栗记`(Unicode 字符串,**保持**,不改)。
- 不动技术 ID/包名/repo/import。assemble+lint+smoke 绿。
