# P5 之后工作队列（给 Codex）

> 本文件 = NavCompose P5 通过后的后续工作总清单,按优先级排列。**P5 未结束、未经 Claude review 前不要开工本队列。** 每项独立 commit、不互相混、不混 P5、不并 main;每项完成本地 assembleDebug+lintDebug+smoke 全绿;装机项标注。
> 通用硬约束(每项都适用):不动数据/事件模型与业务逻辑(除非该项明确要求)、不动 FGR/OCR/STT 接线、不动已锁视觉 token、保人工确认链(AI 候选+医疗数据存前必人核、绝不自动入库)、保阶段投影与首登门控、保密钥 Keystore 加密与隐私边界。

---

## Q1（最高）首启医疗免责确认门（用户明确要求）

**目标**：首次打开 App 时弹出医疗免责,用户**显式确认后才能使用**;未确认不得进入任何功能。

**要求**：
1. **时机**：首启、在任何功能/建档之前;早于现有首登建档门(或与之串联,免责在前)。
2. **形态**：全屏阻断式确认门(这是"真确认"——符合"只有确认/破坏性才用阻断 UI"原则,不算回退到弹框堆)。**不可**返回键/点外部/滑动关闭。
3. **显式同意**：必须有明确肯定操作方可继续(如"我已阅读并同意"按钮);建议正文可滚动、按钮在读到内容后可用,降低盲点。提供"查看完整声明"入口(展开或跳完整声明页)。
4. **内容**：取 `DISCLAIMER.md` 中文核心精简版(性质=非医疗器械/未认证、仅供参考、FGR 未校准近似可能显著误差、AI 识别需人工确认、务必线下就医、AS IS 无担保无责任),措辞与 `DISCLAIMER.md` 及 app 内既有提示一致。
5. **持久化(版本化)**：保存"已接受的免责版本号"到本机(如 SharedPreferences `disclaimer_accepted_version`)。常量化版本号;**当声明实质性变更(版本号提升)时,下次启动须重新弹出并重新确认**。该状态属本机设备状态,不进 JSON 导出/家庭同步(与现有边界一致)。
6. **可复阅**：设置页提供"医疗免责声明"入口可随时再查看(并入 P3 设置层级,作为一个 settings 子项/页)。
7. **不诊断/不阻断急救**：纯同意门,不得包含任何诊断性内容。

**范围**：新增首启免责门 composable/screen(NavCompose route,如 `disclaimer`/作为 first-run 流程一环)+ 版本常量 + 持久化读写(复用 `BabyLogSmartConfigStore` 或等价本机存储,不新建加密需求)+ 设置页复阅入口。

**验收**：全新安装首启必出免责门;不同意无法进入;同意后正常进入且重启不再弹;人为提升版本号后重启重新弹;设置页可再查看;assembleDebug+lint+smoke 绿;装机走查一次。

---

## Q2（高）记录可编辑（用户反馈："只能增删不能改"）

**背景**：事件记录只有增/删,无修改;`TimelineRow` 只有 `onDelete`。数据层基本现成:`BabyLogRepository.putEvent()` 按 `event.id` upsert(同 id=原地替换),`BabyLogEvent` 已有 `id`/`updatedAt`/`updatedBy`。缺口仅:`recordXxx` 每次新生成 id 永远是插入;UI 无编辑入口。

**范围(聚焦孕期记录:B超/产检/孕妈指标;baby 一拍即记按既定决策保持只增)**：
1. `TimelineRow` 加"编辑"入口 → 导航到对应 P1 表单页,**预填该事件现值 + 传入 event id,进编辑模式**。
2. 表单页支持可选"既有事件"入参做预填 + 编辑态(P1 已是独立 screen,改造干净)。
3. Service 加"按 id 更新"路径:**保留原 `id`/`createdAt`/`occurredAt`,刷新 `updatedAt`/`updatedBy=local`**,复用已 upsert 的 `putEvent`;不得新生成 id。
4. 编辑同样走表单**显式保存=人工确认**(不静默改);保存后回来源页并像新记录一样高亮该条。
5. OCR/AI 来源字段被编辑后仍走人工确认(本来就走表单保存,天然满足)。

**验收**：孕期三类记录可编辑、保存后值更新且 id/createdAt 不变、updatedAt 刷新;时间线反映更新并高亮;删除/新增不受影响;assembleDebug+lint+smoke 绿;装机走查 B超编辑改值。

---

## Q2b（高）产检结构化记录（紧随 Q2，复用其编辑基建）

详见 **[`docs/产检记录需求评估-Claude.md`](产检记录需求评估-Claude.md)**（用户裁定中档：常规层 + 结构化专项；不做孕周智能引导/过期/FGR 深度联动）。

**要点**：① 升级 `pregnancy_checkup` payload 为"常规层"（血压/体重/宫高/腹围/胎心率/尿常规/结论/下次日期/附件）；② 新增 7 类轻量 `screening_*` 结构化事件（NT/唐筛/NIPT/大排畸/OGTT/GBS/NST），数值与分级**均由报告或用户录入，App 绝不计算风险/判读/诊断**；③ 孕周→专项**纯待办清单提醒**，中性措辞，可忽略，不预警不催促；④ 复用 NavCompose 表单页 + Q2 编辑基建 + 附件 + 智能录入分类；不重复建模 B超/FGR/孕妈指标；⑤ 保存全程人工确认。

**拆分**：`feat: 产检常规层结构化` + `feat: 产检专项 screening_* 结构化记录`，各独立提交、不混、装机回归。**排在 Q2 之后**（重度复用其编辑基建与表单页）。

## Q3（中）补 LICENSE（开源就绪）

**目标**：仓库加 `LICENSE`。**推荐 Apache-2.0**(比 MIT 多专利与无商标授权条款,个人项目更稳;自带 AS IS/无责任,与 `DISCLAIMER.md` 互补)。
**范围**：根目录 `LICENSE`(Apache-2.0 全文,版权行为项目作者/年份)+ README 顶部加一行许可证说明/badge。**最终许可证由用户拍板**,默认 Apache-2.0。
**验收**：`LICENSE` 存在;README 标注;不影响构建。

---

## Q4（中）资产与文案小清理（积压非阻塞项打包一笔）

1. Splash "BabyLog" 纯文字 → 至多"应用名 logo"(用户原话"不要文字");可做轻量字标处理,别加多余文案。
2. 删未引用冗余资产 `chestnut_mascot_splash.png`;压缩 `chestnut_mascot.png`(约 272KB)。
3. 麦克风权限被拒后,补一次性 Toast/提示明确指向"系统设置可重新授权"(减少困惑),不改降级逻辑。
**验收**：splash 无多余文字;无未引用大图;权限拒绝提示更清晰;assembleDebug+lint+smoke 绿。

---

## Q5（待装机,非 Codex 代码项）设备回归核对清单

测试机被取走 α 测试,以下待设备回归时核对(责任=装机验证,非本队列代码工作):
1. 真机麦克风按住录音 → Paraformer → 转写回填 → LLM 分类 → 正确表单 → 手动保存 全链。
2. 弱网/断网语音降级提示与手动输入可用。
3. 抽查 `voice-stt` 缓存与 speech 配置确不出现在 App JSON 导出与家庭同步内容里。
4. BabyCare 表单真机字段全等性走查(baby 非焦点,低优先,不阻塞)。
5. Q1 首启免责门、Q2 编辑、首页滚动顺滑(P5 性能修复后)一并装机回归。

---

## Q6（低/收尾,可选）文档与收尾

1. `docs/FGR参考曲线集成评估.md` 加"现行决策"头,消除内部分层矛盾(久挂的可选收敛项)。
2. NavCompose 全程结束后,做一次"god 文件终态体检":确认 `ComposeMainActivity` 已显著变薄、屏幕都在 `ui/screens/*.kt`、无残留巨型 when/巨弹框。
3. 复核 README 现状表述(单 Activity 描述等)与重构后实际一致。

---

## 建议执行顺序

先补 P5 遗留 `perf:` FGR remember 化（见架构文档第七节）→ Q1 → Q2 → Q2b → Q4 → Q6;Q3 LICENSE 已由 Claude 完成（跳过）;Q5 等设备回归。Q1/Q2/Q2b 各做完先交 Claude review 再继续下一项。

---

## 用户决策（2026-05-19）：UI 冻结，转业务内容

UI 目前够用不阻塞（用户标尺约 59/100），**暂停一切 UI 微打磨**，把用量投到业务功能。

- **P5 收尾修正 V（quick rail 去双层背景盒/圆形图标）= 后置**，不取消、不现在做；spec 留在架构文档第七节末，待业务完成后的「UI 打磨轮」一并做。
- **UI 打磨停车场（业务后再统一做）**：P5-V 圆形图标；Q4 splash 纯文字→字标 / 删冗余 `chestnut_mascot_splash.png` / 压 272KB 图 / 权限拒绝 Toast；其余视觉细节按需累积。
- Codex **跳过 P5-V，直接进 Q1**；执行顺序更新为：**Q1 → Q2 → Q2b → Q6 →（业务收口后）UI 打磨轮（P5-V + Q4）**。Q3 已完成；Q5 待设备。
- 不阻塞判断依据：MetricCard 文字裁切（可读性缺陷）已于 `41a830d` 修复；底部交互/导航为可用终态；剩余 UI 项均为美观打磨，可安全延后。

---

## 评审记录（Claude）

### Q1 首启医疗免责确认门 — 通过（commit `7266329`）

**结论**：通过，放行 Q2。

**核实（git+grep+diff，未耗真机；Codex 附 6 装机图覆盖场景）**：① scope 仅免责文件+门控接线+备份规则+设置复阅+smoke/CI，QuickRail/FGR/STT/Repository/Domain 零触，单 feat、树干净。② 门控：`MedicalDisclaimerScreen` `BackHandler(enabled=true){}` 拦返回键；onCreate 读 `hasAcceptedCurrentVersion()` 入 uiState 未接受即门控；accept 持久+放行；设置页复阅入口接好。③ 版本化：`BabyLogDisclaimerPolicy.CURRENT_VERSION` 常量，`needsAcceptance`=存≠当前，升版重弹。④ 隔离：`BabyLogDisclaimerStore` 独立 prefs `babylog_disclaimer_state`（非主库/非 smart_config）→ 不入 JSON 导出/家庭同步；`babylog_backup_rules.xml`+`data_extraction_rules.xml`(cloud-backup+device-transfer) 已 exclude → 不随安卓备份/迁移。⑤ 纯同意门无诊断；TDD 红→绿、assemble+lint+smoke 绿、装机六场景齐。

### Q2 记录可编辑 — 通过（commit `85c4122`）

**结论**：通过，放行 Q2b。

**核实（git+grep+diff，未耗真机；Codex 附装机 BPD46→47）**：`createEditedEvent`(BabyLogService:535) 以 `existing.id/occurredAt/createdAt/source/schemaVersion/deletedAt` 重建，`updatedAt=nowIso()`、`updatedBy=UPDATED_BY_LOCAL`，无 `UUID.randomUUID`；`requireEditableEvent`+类型守卫(不存在/已删/类型不匹配抛错)；`putEvent` 同 id upsert=原地替换；编辑经全屏表单显式保存=人工确认，无静默改。scope 仅 Service+编辑接线+4 表单预填入参+时间线编辑入口+smoke；禁区/冻结 UI/Q1/baby 一拍即记零触；单 feat；assemble+lint+smoke 绿。

### Q2b-1 产检常规层结构化 — 通过（commit `154f02d`）

**结论**：通过，放行 Q2b 第二笔 `screening_*`。

**核实（git+grep+diff，未耗真机；Codex 附装机+摘要证据）**：scope 仅 Service+CMA+PregnancyEventFormScreen+smoke，未触 screening_/QuickRail/FGR/STT/Disclaimer/perf/冻结UI，单 feat。payload 经 putNumberIfNotNull/putStringIfNotBlank 存结构化常规层（systolic/diastolic/fundalHeight/胎心率/urineRoutine/nextVisitDate），可选字段→旧浅记录向后兼容；nextVisitNote 保留；`if(pregnancy_checkup)` 门控不影响共用表单其它类型。`updatePregnancyEvent`→`createEditedEvent`（保 id/createdAt/occurredAt、刷 updatedAt、无新 UUID）；编辑预填+显式人工保存。产检块只存测量值，无风险/判读/诊断/预警（339 glucose warning 属既有 maternal_metric 非新增）；不重复建模（存本次访视自身值）；附件走既有 document_image 压缩链路；smoke 覆盖 checkupStructured+摘要。

### UI 打磨停车场追加（2026-05-19）：去页面转场动画

用户反馈 NavCompose 页面切换的淡入淡出"很奇怪，不如不要"。

- **改法**：`NavHost`/各 `composable` 的 `enterTransition/exitTransition/popEnterTransition/popExitTransition` 设 `None` → 瞬切，无淡入淡出。
- **加载指示按需**：不给每页套 spinner；仅确有异步加载的页，页面中心放一个 `CircularProgressIndicator`；无加载则什么都不显（当前基本无真异步加载页）。
- **排期**：并入业务收口后的「UI 打磨轮」（与 P5-V、Q4 一起）；体量极小，届时顺手做。一笔 `ui:` 提交，不混业务/不并 main。

## Q2c（高）记录详情查看页（用户报缺陷：记录只能编辑/删除，不能查看）

**缺陷确认**：`BabyLogRoutes` 无 detail/view 路由；`TimelineRow` 仅 `onEditEvent`/`onDeleteEvent`，无点击查看。存的 B 超/产检/孕妈指标等记录**无只读查看页**，要看数据只能进编辑表单——医疗相关记录的核心缺失。

**做法（一笔独立 `feat:`，不混 B超重构/Q2b-2/perf）**：
1. 新增 `ui/screens/RecordDetailScreen.kt` + Nav 路由 `record/detail`（传 eventId 或 event）。
2. `TimelineRow` 加点击 → 导航到详情页（保留/移交 编辑、删除）。
3. 详情页**只读**渲染该事件类型**全部已存字段**：B 超核心+高级、产检常规层、孕妈指标、胎动、宫缩、baby 各类型；复用 `BabyLogFormatters`/`eventSummary` + 结构化字段渲染；附件可查看（跳 `AttachmentPreview`）。长内容用 LazyColumn。
4. 详情页内提供 **编辑**（→ 现有 Q2 编辑表单）+ **删除**（→ 现有 `ConfirmDialog`，逻辑不变）。
5. NavCompose 规范：AppBar + 返回；不丢状态。

**MUST 不变**：数据/事件模型零改；删除仍走 `ConfirmDialog` 人工确认；所有事件类型可看；复用既有格式化；不动 FGR/OCR/STT/已锁视觉/阶段投影。**验收**：任一记录点进只读详情，全字段正确呈现（B超高级字段也在）、附件可看；详情内可编辑可删除；assemble+lint+smoke 绿；装机走查 B超/产检/孕妈指标 查看。

**排期建议**：`record/detail` 是更基础的能力（能看自己记录）——建议 **B超重构(在途) → perf-C → Q2c 记录详情 → Q2b-2 产检专项**。最终顺序由用户定。
