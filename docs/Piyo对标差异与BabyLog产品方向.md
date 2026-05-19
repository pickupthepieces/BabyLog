# Piyo日志对标差异与 BabyLog 产品方向

## Status

| 项目 | 内容 |
|---|---|
| 文档状态 | Draft |
| 日期 | 2026-05-15 |
| 设备 | 小米手机 `22041216UC`，ADB serial `9XJZS8XKPN4DD6QC` |
| 对标对象 | Piyo日志 `jp.co.sakabou.piyolog` |
| 本项目对象 | BabyLog `app.babylog.nativeapp` |
| 目标 | 将 BabyLog 从孕期本机 MVP 演进为类似 Piyo 的家庭育儿记录工具 |

## 摘要

当前 BabyLog 已具备本机离线记录、孕周首页、B 超资料、时间线、设置、备份和待同步队列等基础能力，但产品重心仍偏向“孕期记录 + 本机数据管理”。Piyo日志是成熟的“出生后育儿日常记录工具”，核心体验围绕高频一键记录、按时间轴回看、统计摘要、成长曲线、家庭共享、提醒、商业化和云服务能力展开。

综合真机观察和包信息，BabyLog 后续如果要开发成类似 Piyo 的工具，优先方向不应是复制视觉风格，而是补齐以下产品闭环：宝宝档案初始化、高频育儿事件记录、24 小时时间轴、日报/摘要、成长曲线、提醒、家庭共享、导入导出、稳定同步和隐私边界。

## 直接证据

| 维度 | BabyLog | Piyo日志 | 说明 |
|---|---|---|---|
| 包名 | `app.babylog.nativeapp` | `jp.co.sakabou.piyolog` | ADB `pm list packages` 确认 |
| 版本 | `versionName=0.1.0`, `versionCode=1` | `versionName=9.1.7`, `versionCode=435` | Piyo 是长期迭代产品，BabyLog 仍是早期版本 |
| SDK | `minSdk=23`, `targetSdk=36` | `minSdk=29`, `targetSdk=35` | BabyLog 面向更低系统版本，Piyo 面向较新 Android |
| 入口 Activity | `.MainActivity` | `.InitialActivity`，注册后进入 `.MainActivity` | Piyo 有完整首次使用流程 |
| 首屏 | 直接显示孕周、预产期、今日记录、最近记录 | 欢迎页、启动、伴侣分享、转移数据、条款、隐私政策 | Piyo 首次使用更强调账号/数据迁移/共享 |
| 主界面 | 首页卡片 + 底部导航 + 中央加号 | 24 小时时间轴 + 底部事件按钮 + 统计/曲线/菜单 | Piyo 更偏高频操作工作台 |
| APK 结构 | 1 个 APK | base + 19 个 split APK | Piyo 含多语言、架构和密度拆分 |
| 权限规模 | 相机、图片读取为主 | 网络、通知、闹钟、蓝牙、定位、录音、Billing、广告 ID、FCM 等 | Piyo 功能边界和外部依赖明显更宽 |
| 日志表现 | 未见崩溃；日志较轻 | 未见崩溃；Firebase、广告、Billing、GMS 服务日志较多 | Piyo 在小米设备上有 `SERVICE_NOT_AVAILABLE` / `SERVICE_DISABLED` 噪声 |

证据文件：

| 文件 | 用途 |
|---|---|
| `diagnostics/app-compare/babylog-screen.png` | BabyLog 首页截图 |
| `diagnostics/app-compare/piyolog-screen.png` | Piyo 欢迎页截图 |
| `diagnostics/app-compare/piyolog-main-screen.png` | Piyo 主界面截图 |
| `diagnostics/app-compare/babylog-dumpsys-package.txt` | BabyLog 包、版本、权限、组件信息 |
| `diagnostics/app-compare/piyolog-dumpsys-package.txt` | Piyo 包、版本、权限、组件信息 |
| `diagnostics/app-compare/piyolog-main-logcat-current.txt` | Piyo 主界面启动后日志 |

## 产品差异分析

### 1. 产品定位

| 模块 | BabyLog 当前状态 | Piyo 表现 | BabyLog 后续方向 |
|---|---|---|---|
| 孕期记录 | 已有孕周、预产期、B 超、产检资料方向 | Piyo 主流程更偏出生后育儿 | 保留为差异化能力，不应删除 |
| 育儿记录 | 已有喂养、睡眠、尿布、体温、用药等事件模型 | 母乳、配方奶、睡觉、起床、尿尿、便便是主入口 | 将育儿事件提升为主工作流 |
| 时间组织 | 最近记录 + 时间线 | 以一天 24 小时轴为中心 | 增加日视图时间轴，作为高频记录主界面 |
| 资料归档 | B 超单、检查单、出生证明、疫苗本 | 育儿日记、图片、成长数据 | 保留资料库，并与事件记录互通 |
| 同步/共享 | 后端未配置，本机待同步 | 伴侣分享、转移数据、云服务倾向明显 | 先做家庭共享数据模型和同步协议，再做账号 UI |

### 2. 首次使用流程

Piyo 的首次使用流程包含欢迎页、条款隐私确认、宝宝注册、出生前/出生后分支、宝宝姓名等步骤。BabyLog 当前直接进入默认首页，适合开发验证，但不适合长期使用和家庭共享。

BabyLog SHOULD 增加首次使用向导：

| 步骤 | 必填 | 规则 |
|---|---|---|
| 家庭空间 | 是 | 创建默认 `familyId`，本机模式也必须存在 |
| 阶段选择 | 是 | `pregnancy` 或 `born` |
| 预产期/出生日期 | 是 | 孕期用预产期，出生后用出生日期 |
| 宝宝昵称 | 否 | 可后续补充 |
| 多端同步 | 否 | 默认关闭，仅提示可后续配置 |
| 数据导入 | 否 | 支持从 BabyLog JSON 备份恢复 |

### 3. 主界面工作流

Piyo 主界面采用“日期 + 时间轴 + 底部事件按钮”的结构，目标是降低记录成本。BabyLog 当前首页偏 dashboard，适合看状态，但高频记录需要点中央加号再选择。

BabyLog SHOULD 增加两种首页模式：

| 模式 | 目标用户 | 布局 |
|---|---|---|
| 孕期首页 | 怀孕阶段 | 孕周卡、B 超/产检、胎动/宫缩、最近记录、资料入口 |
| 育儿日视图 | 出生后阶段 | 日期、24 小时时间轴、底部快捷事件、摘要入口 |

育儿日视图 MUST 支持快速记录以下 P0 事件：

| 事件 | 必填字段 | 可选字段 |
|---|---|---|
| 母乳 | 开始时间、侧别 | 结束时间、时长、备注 |
| 配方奶/奶瓶 | 时间、容量 ml | 奶粉品牌、备注 |
| 睡觉 | 开始时间 | 结束时间、地点、备注 |
| 起床 | 时间 | 关联睡眠段 |
| 尿尿 | 时间 | 颜色、备注 |
| 便便 | 时间 | 性状、颜色、备注 |
| 体温 | 时间、温度 | 测量方式、备注 |
| 用药 | 时间、药名、剂量 | 原因、备注 |
| 日记 | 时间、文本 | 图片 |

### 4. 统计与摘要

Piyo 主导航包含“摘要”和“成长曲线”。BabyLog 当前有今日记录数量、趋势入口，但摘要能力还不完整。

BabyLog MUST 增加日摘要：

| 指标 | 计算规则 |
|---|---|
| 喂养次数 | 当日母乳、奶瓶、辅食事件总数 |
| 奶量 | 当日奶瓶容量求和 |
| 睡眠时长 | 完整睡眠段求和，未结束段单独标记 |
| 尿布次数 | 尿尿、便便、混合事件统计 |
| 体温异常 | 超过用户配置阈值的记录数 |
| 最近用药 | 最近一次用药时间、药名、剂量 |

BabyLog SHOULD 增加周/月摘要，但不进入第一轮 P0。

### 5. 成长曲线

BabyLog 已有孕期 B 超趋势和资料能力，Piyo 强调出生后成长曲线。后续应统一“孕期胎儿趋势”和“出生后成长曲线”。

| 阶段 | 曲线 | 优先级 |
|---|---|---|
| 孕期 | BPD、HC、AC、FL、EFW | P0，已有模型基础 |
| 出生后 | 体重、身长/身高、头围 | P0，需要补 UI |
| 出生后 | WHO 百分位参考 | P1，需引入标准数据和版本说明 |
| 疫苗/儿保 | 月龄提醒 | P1 |

### 6. 同步、共享与隐私

Piyo 的权限、Firebase、FCM、Billing、分享入口表明其产品依赖联网服务。BabyLog 的家庭同步仍显示“后端未配置”；`INTERNET` 权限仅用于用户主动配置的多模态模型识别、语音转文字 STT 和后续自托管同步，不代表接入第三方账号体系。

BabyLog 要做类似 Piyo 的工具，后端能力 SHOULD 按以下顺序建设：

| 阶段 | 能力 | 说明 |
|---|---|---|
| S0 | 本机备份/恢复 | 当前已有基础，必须继续稳定 |
| S1 | 单用户云同步 | 账号登录、设备间同步、冲突处理 |
| S2 | 家庭共享 | 伴侣邀请、角色权限、共同编辑 |
| S3 | 通知提醒 | 疫苗、儿保、用药、睡眠/喂奶计时 |
| S4 | 小范围邀请制 | 给同事家庭使用前必须完成数据隔离验证 |

数据隐私 MUST 作为硬约束：

| 要求 | 规则 |
|---|---|
| 家庭隔离 | 所有云端表必须带 `familyId`，服务端 MUST 校验访问权限 |
| 最小权限 | 不需要广告、定位、蓝牙、录音时 MUST NOT 申请相关权限 |
| 本机可用 | 网络不可用时 MUST 允许继续记录 |
| 数据导出 | 用户 MUST 可以导出完整 JSON 备份 |
| 删除能力 | 用户 MUST 可以删除本机数据；云端阶段 MUST 支持软删除和恢复窗口 |

## BabyLog 目标规格

### Scope

BabyLog 的目标不是复刻 Piyo 的商业化链路，而是实现一个家庭自用优先、可逐步扩展到小范围共享的孕育记录工具。第一目标是覆盖“孕期到 3 岁”的连续记录，第二目标是做到接近 Piyo 的高频育儿记录效率。

### Terms

| 术语 | 含义 |
|---|---|
| Family | 一个家庭数据空间 |
| Child | 一个宝宝档案，阶段可为孕期或出生后 |
| Event | 一条时间线记录，如喂养、睡眠、B 超、体温 |
| Attachment | 图片或文件资料，如 B 超单、疫苗本 |
| Day View | 按一天展示的育儿记录工作台 |
| SyncChange | 本机变更队列，后续用于云同步 |

### Requirements

| ID | 要求 | 优先级 | 验收标准 |
|---|---|---|---|
| ONBOARD-01 | App MUST 支持首次使用向导 | P0 | 新装后可选择孕期/出生后并创建宝宝档案 |
| HOME-01 | App MUST 支持孕期首页 | P0 | 显示孕周、预产期、最近 B 超/产检、最近记录 |
| DAY-01 | App MUST 支持出生后日视图 | P0 | 可按日期查看 24 小时记录 |
| RECORD-01 | App MUST 支持一键记录母乳、奶瓶、睡眠、尿尿、便便 | P0 | 每类事件 2 次点击内可完成最小记录 |
| RECORD-02 | App MUST 支持体温、用药、日记 | P0 | 事件进入统一时间线 |
| SUMMARY-01 | App MUST 支持日摘要 | P0 | 可统计喂养次数、奶量、睡眠、尿布、体温 |
| GROWTH-01 | App MUST 支持出生后体重、身长、头围曲线 | P0 | 至少显示自有趋势线 |
| PREG-01 | App MUST 保留孕期 B 超和产检资料能力 | P0 | 孕期阶段不被育儿日视图覆盖 |
| BACKUP-01 | App MUST 支持 JSON 导出和导入 | P0 | 导出的数据可在新安装 App 恢复 |
| SYNC-01 | App SHOULD 保留同步队列 | P0 | 离线记录不会丢失，后端未配置时给出明确状态 |
| SHARE-01 | App MAY 支持家庭共享 | P2 | 需完成账号、权限、冲突处理后再开放 |

### Recommended Data Model

| Entity | Required Fields | Notes |
|---|---|---|
| Family | `id`, `name`, `createdAt`, `updatedAt` | 单机版也创建默认家庭 |
| Child | `id`, `familyId`, `name`, `stage`, `dueDate`, `birthDate`, `createdAt`, `updatedAt` | `stage` 为 `pregnancy` 或 `born` |
| Event | `id`, `familyId`, `childId`, `eventType`, `occurredAt`, `payload`, `attachmentIds`, `createdAt`, `updatedAt`, `deletedAt` | 统一时间线核心表 |
| Attachment | `id`, `familyId`, `childId`, `kind`, `mimeType`, `byteSize`, `localPath`, `remoteUrl`, `createdAt` | 本机和云端共用 |
| SyncChange | `id`, `familyId`, `entityType`, `entityId`, `operation`, `status`, `attemptCount`, `lastError` | 后端阶段继续复用 |

### Event Type Baseline

| eventType | 中文名 | 阶段 | P0 |
|---|---|---|---|
| `pregnancy_checkup` | 产检 | 孕期 | Yes |
| `ultrasound` | B 超 | 孕期 | Yes |
| `fetal_movement` | 胎动 | 孕期 | No |
| `contraction` | 宫缩 | 孕期 | No |
| `birth` | 出生 | 阶段切换 | Yes |
| `breastfeed` | 母乳 | 出生后 | Yes |
| `bottle` | 奶瓶/配方奶 | 出生后 | Yes |
| `sleep` | 睡眠 | 出生后 | Yes |
| `wake` | 起床 | 出生后 | Yes |
| `pee` | 尿尿 | 出生后 | Yes |
| `poop` | 便便 | 出生后 | Yes |
| `temperature` | 体温 | 出生后 | Yes |
| `medication` | 用药 | 出生后 | Yes |
| `growth` | 成长 | 出生后 | Yes |
| `vaccine` | 疫苗 | 出生后 | No |
| `note` | 日记/备注 | 全阶段 | Yes |

## 实施路线

### Phase 1：类 Piyo 记录闭环

| 任务 | 产出 |
|---|---|
| 首次使用向导 | 创建宝宝档案，支持孕期/出生后 |
| 育儿日视图 | 日期切换、24 小时时间轴、底部快捷事件 |
| 高频记录表单 | 母乳、奶瓶、睡眠、尿尿、便便、体温、用药、日记 |
| 日摘要 | 今日/昨日统计 |
| 本地备份兼容 | 新事件类型可导出导入 |

### Phase 2：成长与提醒

| 任务 | 产出 |
|---|---|
| 成长曲线 | 体重、身长、头围自有趋势 |
| 计时器 | 母乳/睡眠计时，支持通知栏或 App 内提醒 |
| 疫苗/儿保 | 可编辑提醒模板 |
| 搜索筛选 | 按类型、日期、关键词查记录 |

### Phase 3：云同步与家庭共享

| 任务 | 产出 |
|---|---|
| 后端最小闭环 | 登录、family、child、event、attachment API |
| 同步协议 | push/pull、冲突处理、软删除 |
| 家庭邀请 | 伴侣加入、角色权限 |
| 隐私验证 | 家庭隔离测试、导出删除能力 |

## 当前证据不足以支撑的结论

| 未证明结论 | 原因 |
|---|---|
| Piyo 的全部功能范围 | 当前只观察了首次使用和主界面，没有系统遍历所有菜单 |
| Piyo 的后端协议 | 未抓包，不能判断 API、加密和同步策略 |
| Piyo 的商业化策略 | 只看到升级、广告、Billing 迹象，不能判断付费规则 |
| BabyLog 的真实性能差异 | 未做启动耗时、内存、帧率和长时间稳定性测试 |

## 问题单摘要

BabyLog 当前是孕期和本机离线记录 MVP，Piyo日志是成熟的出生后育儿日常记录工具。若 BabyLog 目标调整为“类似 Piyo 的工具”，下一阶段应优先补齐宝宝档案初始化、出生后日视图、母乳/奶瓶/睡眠/尿尿/便便等高频记录、日摘要、成长曲线和备份恢复兼容；云同步、家庭共享、提醒和权限隔离作为后续阶段推进。孕期 B 超和产检资料能力应保留，这是 BabyLog 区别于 Piyo 的价值点。
