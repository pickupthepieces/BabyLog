# BabyLog 重构前逻辑核查

## Status

| 项目 | 内容 |
|---|---|
| 文档状态 | Draft |
| 日期 | 2026-05-15 |
| 范围 | Android 原生 `BabyLogService`、`BabyLogRepository`、`BabyLogFormatters`、相关 `MainActivity` 调用点 |
| 目的 | 在阶段主线重构前锁定 IA 之外的真实逻辑缺陷 |
| 来源 | `重构前逻辑核查-Claude.md` + Codex 代码复核 |

## Scope

本文只记录会造成数据丢失、崩溃、错误医疗/育儿信息展示、导入导出风险、性能退化的逻辑问题。

本文不评审视觉精美度、首页信息架构和阶段混放问题；这些问题归入 `docs/BabyLog阶段主线重构计划.md`。

## Blocking Bugs

以下问题 MUST 并入阶段主线重构，不能作为事后修补。

| ID | 严重度 | 证据 | 影响 | 必须动作 |
|---|---|---|---|---|
| L-1 | Blocker | `BabyLogService.createBackupJson()` 写入空 `familyProfiles`、空 `childProfiles`；`BabyLogRepository` 当前没有 profile store | Phase 1 增加 `ChildProfile.expectedDueDate` / `birthDate` 后，导出再导入会丢档案，阶段退回 `unknown` | 新增 `ChildProfile` 时，Repository、`createBackupJson()`、`importBackupJson()` MUST 同步纳入 profile；backup schema SHOULD 升版并兼容旧备份缺 profile |
| L-2 | Blocker | `BabyLogService.summarizeToday()` 只初始化 `BabyLogDomain.EVENT_TYPES`，随后执行 `counts.get(event.eventType) + 1` | 导入未知事件或重构新增 `breastfeed` / `bottle` / `wake` / `pee` / `poop` 后会 NPE，dashboard 可崩 | 新事件类型、`EVENT_TYPES`、summary、timeline group MUST 原子更新；summary MUST 使用 `getOrDefault` |
| L-3 | Blocker | `BabyLogRepository.importData()` 直接覆盖 `events`、`attachments`、`syncChanges`；`MainActivity.readBackup()` 只提示“导入完成” | 用户导入会静默清空当前本机记录，医疗/育儿记录存在不可逆误删风险 | 导入前 MUST 二次确认“导入会覆盖当前本机全部记录，建议先导出当前数据” |

## High Priority

| ID | 严重度 | 证据 | 影响 | 要求 |
|---|---|---|---|---|
| L-4 | High | Java `MainActivity` 保存路径调用 `service.recordBabyCareEvent()` / `service.recordUltrasound()`；Repository `putJson()` 每次全量 JSON parse + serialize + `apply()` | 记录增长后，每次保存可能卡 UI；进程立即退出可能丢最后一次异步落盘 | 写操作 SHOULD 移入 `ioExecutor`；关键导入/清空/档案写入 SHOULD 使用可确认落盘的策略 |
| L-5 | High | `createAttachmentFile()` 使用 `System.currentTimeMillis() + "-" + safeName` | 同一毫秒且同名调用可能覆盖；当前 restore 传入 `attachmentId + ".jpg"`，已部分规避，但通用 helper 仍不稳 | 文件名 SHOULD 包含 UUID、attachmentId 或自增序号，不能只依赖毫秒 |
| L-6 | High | `MainActivity.latestEfwValue()` / `latestEfwCaption()` 只扫 `dashboard.recentEvents`；无结果返回 `1320 g` / `28+3 周`；孕妈体重卡硬编码 `60.4 kg` | 高频记录超过 recent-20 后，趋势会静默展示假医疗/孕期数据 | 首页趋势 MUST 查询最近一条真实 B 超/体重记录；无数据 MUST 显示空态，MUST NOT 用假数值兜底 |

## Medium Priority

| ID | 严重度 | 证据 | 影响 | 要求 |
|---|---|---|---|---|
| L-7 | Medium | `parseGestationalAgeDays()` 只接受 `\d{1,2}(\+[0-6])?` | 全角 `＋`、`28周3`、`28w3` 会被拒，中文输入体验差 | MUST 先归一化全角符号，SHOULD 接受 `周` / `w` 格式 |
| L-8 | Medium | `createOccurredAtFromDate()` 用检查日期拼接当前时分秒 | 补录历史 B 超排序受当前时间影响；未来日期会长期置顶 | B 超补录 SHOULD 使用明确检查日时间策略，并对未来日期给出确认或限制 |
| L-9 | Medium | `formatEventDay()` 通过日期毫秒除以 `86_400_000` 判断今天/昨天 | 有 DST 时区可能错日；中国时区低风险 | 日期归属 SHOULD 复用 day-bucket helper，不直接用固定毫秒天数 |
| L-10 | Medium | `timelineFilterGroup("birth")` 返回 `pregnancy` | 出生后筛选找不到出生记录 | 阶段筛选 MUST 单独处理 `birth`，出生后历史视图 SHOULD 可见出生事件 |

## Low Priority

| ID | 严重度 | 证据 | 影响 | 要求 |
|---|---|---|---|---|
| L-11 | Low | `recordUltrasound()` 在 `photoPath` 非空但文件不存在时仍创建 `byteSize=0` 附件 | 产生孤儿附件记录，资料库显示无效图片 | 记录附件前 MUST 校验文件存在且可读 |
| L-12 | Low | `clearLocalData()` 只删除 `filesDir/attachments` | `externalFilesDir/Pictures/camera-captures` 中异常残留的拍照临时文件不会清理 | 清空本地数据 SHOULD 同步清理 camera-captures 临时目录 |
| L-13 | Low | `parseOptionalNumber()` 接受 `NaN` / `Infinity`；`formatByteSize()` 用 1024 进制但显示 KB/MB | 可能写入非法 JSON 数值；单位语义不统一 | 数字解析 MUST 拒绝非有限值；文件大小单位 SHOULD 统一为 KiB/MiB 或改为十进制 |

## Refactor Gates

| Gate | 要求 |
|---|---|
| G-1 | Phase 1 MUST 同时解决 L-1、L-2、L-3 |
| G-2 | 引入 `ChildProfile` 的同一 commit MUST 覆盖备份导出、导入恢复和本地清空 |
| G-3 | 引入新育儿事件类型的同一 commit MUST 更新 `EVENT_TYPES`、summary、timeline group、filter 映射 |
| G-4 | 导入备份 UI MUST 先确认覆盖风险，再执行 `importBackupJson()` |
| G-5 | 首页趋势卡 MUST 移除假数值兜底，才能进入阶段首页验收 |

## Implementation Placement

| 问题 | 推荐阶段 | 说明 |
|---|---|---|
| L-1 | Phase 1 | 与最小档案编辑同源，必须一起做 |
| L-2 | Phase 1 / Phase 3 | summary 兜底先做；新事件类型在快捷入口阶段原子更新 |
| L-3 | Phase 1 | 导入覆盖风险与 profile 恢复测试绑定 |
| L-4 | Phase 1 起步，Room 后置 | 先把当前写路径挪后台，Room 迁移仍属于 D 类 |
| L-5 | Phase 1 或独立小修 | 低耦合，可先修 helper |
| L-6 | Phase 2 | 首页分流时一并移除假趋势 |
| L-7 ~ L-13 | 独立小 commit 或随触达模块修 | 不阻断 IA 重构，但应进入 backlog |
