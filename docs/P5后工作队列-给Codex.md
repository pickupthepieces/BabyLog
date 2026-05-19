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

## Q2d（严重/最高）时间线/历史被硬截断 100 条（用户报"业务不行"，Claude 审出）

**缺陷确认**：`ComposeMainActivity:506` `timeline = service.listRecentEvents(100)` —— `state.timeline` 是时间线 Tab、首页摘要、PregnancySummaryPanel、**FGR 成长曲线**等的唯一数据源，被硬截断为"最近 100 条"。家庭长期记录极易破百（孕期产检/B超/胎动/孕妈指标/宫缩 + baby 期日常）。**>100 条后更早记录从时间线及所有读 `state.timeline` 处静默消失**；叠加 Q2c（无详情查看）+ 无搜索 → 旧记录不可达；**FGR 成长曲线只画最近 100 内的 B 超点，历史曲线失真**。非过滤问题（默认 "all" 正常），是数据源截断。

**做法（一笔独立 `fix:`，不混 B超重构/Q2b/perf/Q2c）**：
1. **时间线 Tab 必须可达全部历史**：`TimelineScreen` 数据源改为完整事件集（非 100 上限）。性能上用 LazyColumn 懒加载 / 分页加载（滚到底加载更多）即可，不一次性全载。
2. **FGR/成长曲线、历史趋势**用完整 B超/指标历史，不受 100 限。
3. 首页"最近记录"可仍限 N 条（保持轻量），但**与时间线 Tab/历史数据源解耦**——home recent 用 `listRecentEvents(N)`，timeline/曲线/历史用全量（`listAllEvents()` 或分页 API）。
4. Service/Repository 若无全量/分页接口需补（仅新增读取路径，不改事件/数据模型、不改写入/确认链）。

**MUST 不变**：数据/事件模型、写入、人工确认、删除 ConfirmDialog、阶段投影、FGR 算法均不变；仅修读取截断。**验收**：造 >100 条事件后，时间线 Tab 能滚到最早一条；FGR 曲线含早于最近 100 的 B超点；首页仍轻量；assemble+lint+smoke 绿；装机走查（>100 条后翻到最早记录、曲线历史完整）。

**排期建议**：这是数据可见性严重缺陷，优先级最高——建议 **B超重构(在途) → Q2d(截断) → perf-C → Q2c(详情) → Q2b-2**。最终由用户定。

## Q2e（严重）备份导入非原子，失败可致数据全失（Claude 深审）

**隐患**：`BabyLogService.importBackupJson` 已先校验 format/version/events≠null（坏档先抛，好）；但随后 `restoreAttachmentBlobs` 写盘 + `repository.importData(...)`。导入是带"覆盖本机全部"确认的破坏性操作；若 `repository.importData` 为"先清空再载入"且中途抛 IO/JSON 异常 → **既有数据被清且新数据未载入 = 彻底丢失**。

**做法（一笔独立 `fix:`，不混其它）**：
1. 核查 `repository.importData` 当前是否"先清空后写入"。
2. 改为**事务式**：先在内存完整构建+全量校验新状态（events/profiles/attachments/blobs 全部 parse 成功、attachmentBlobs 与 attachments 对应齐全），**全部通过后再原子替换**；任一步失败则**保持原数据不动**并报错。
3. 附件 blob 落盘失败也须整体回滚（不可留半套文件 + 清空索引）。
4. 不改备份格式/版本/数据模型；仅加固导入事务性与失败回滚。

**验收**：构造①坏 JSON ②版本不符 ③events 缺失 ④attachmentBlob 损坏/缺失 四种坏档导入，均**原数据完好无损**并明确报错；正常备份导入照常成功；assemble+lint+smoke 绿；装机走查（先存数据→导坏档→数据仍在；再导正常档→成功）。

**附：本轮深审其它结论（无需改动）**：阶段切换孕期→出生后无 bug（单 child、Timeline 非阶段过滤、记录不孤儿）；OCR/智能录入误分类有"暂不支持"兜底、不静默乱存。**B2 已知长期限制（非本轮）**：附件 base64 全量入单内存 JSON，照片多时导出/导入易 OOM；未来需流式/分片，记录备查。

## Q2f（高）编辑覆盖缺失：胎动/宫缩(孕期)及 baby 记录不可编辑（Claude 全量审计）

**缺陷**：`isEditablePregnancyRecord` 仅 `ultrasound/pregnancy_checkup/maternal_metric`。**胎动(fetal_movement)、宫缩(contraction) 同属孕期主线却不可编辑**，全部 baby 记录(feed/sleep/diaper…)也不可编辑——只能删。叠加 Q2c(无查看)→ 这些记录**写一次即不可改、错了只能删了重录**，与"记录应可编辑"的用户预期与 Q2 既有基建不一致。

**做法（一笔 `feat:`，复用 Q2 编辑基建）**：把胎动/宫缩纳入可编辑(走对应表单页 + `createEditedEvent` 保 id/createdAt/occurredAt 刷 updatedAt，与 Q2 同模式)；baby 记录可编辑同法(baby 非焦点，可与孕期同基建一并打开或单列，按实现成本定，但**至少胎动/宫缩孕期两类必须可编辑**)。保存仍显式人工确认。验收：胎动/宫缩记录可编辑改值、id/createdAt 不变；不影响删除/新增。

## Q2g（中高/缺失功能）无记录搜索：长期历史不可检索（Claude 全量审计）

**缺陷**：全库无任何搜索/检索(grep 确认)。Q2d 修复后历史完整，但**定位某条旧记录只能滚动时间线**，长期使用(几百上千条)实际不可用。

**做法（一笔 `feat:`）**：时间线/资料库加按 关键词/日期/类型 的检索(本地过滤，复用现有 timeline 数据与分类)；不改数据模型。验收：能按关键字/类型/日期范围定位历史记录；性能 OK(配合 Q2d 全量/分页)。

---

## 全量逻辑/UX 审计结论（Claude，2026-05-19）

**逻辑/数据完整性 严重缺陷（已立项）**：Q2d 时间线/FGR 100 截断、Q2e 备份导入非原子可致数据全失、Q2c 无只读详情、Q2f 编辑覆盖缺失(胎动/宫缩等)、Q2g 无搜索。
**审计判定无 bug（无需改）**：阶段切换孕期→出生后(单 child、Timeline 非阶段过滤、记录不孤儿)；OCR/智能录入误分类有"暂不支持"兜底不静默乱存；编辑回填 round-trip 完整(Q2b-1 结构化字段不丢)；首启免责门(Q1)门控正确；quick 一拍即记(用户既定保留)；stage=unknown 有"固定未知"标签非死角。
**已知长期限制（非本轮，记录备查）**：B2 备份附件 base64 全量入单内存 JSON，照片多时导出/导入易 OOM，未来需流式/分片；FGR 为未校准近似(已 DISCLAIMER 声明)；旧版本备份直接拒绝无迁移路径(schema 升级时需补)。
**产品级缺口（候选 backlog，用户挑）**：见 `docs/孕期App功能缺口调研-Claude.md` G1-G7+G9。

**推荐总执行顺序**：B超重构(已过) → Q2d → Q2e → perf-C → Q2c → Q2f → Q2g → Q2b-2 → Q6 →（之后）UI 打磨轮 + 用户选定的 G 系列。

## Q2e 更正（Claude 深读 `BabyLogRepository.importData` 后自我纠正，降级）

**原判断高估、更正**：`importData`(Repository:232-248) 是**单个 SharedPreferences.Editor 批量 commit()**，对该批改动原子；`importBackupJson` 顺序=校验 format/version→`restoreAttachmentBlobs` 写盘(失败即抛、此时未调 importData、旧数据完好)→最后一次原子 commit。**不存在"清空后中途崩=数据全失"**。原 Q2e"严重/非原子"**作废，降为中等**。

**真实问题（精确化）**：① 仅浅校验(format/version/events 数组存在)，**无逐条 sanity** → 结构合法但内容垃圾的备份会被原子覆盖掉好数据；② **无导入前自动快照/撤销**，覆盖不可逆。

**更正后做法（中等，一笔 `fix:`）**：导入前先自动落一份一次性本机快照(如 `pre-import-<ts>.json` 或现有导出复用)；对 events/attachments 做逐条 sanity(必需字段/类型)，全过才 commit 覆盖；提供"撤销上次导入"恢复该快照。**不需要改 importData 的写入机制(已等效原子)**。验收：垃圾内容备份导入被拒、原数据无损；正常导入成功且可一键撤销回导入前。

> 自我说明：此更正源于"先 grep 后未读实现"的快扫，经用户要求深读后坐实并纠偏——记录以儆，后续审计须读实现体再定级。
