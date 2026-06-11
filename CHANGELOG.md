# Changelog

BabyLog 发版变更日志。每次 release tag 触发 CI 时，从这里提取对应版本的条目作为 GitHub Release 描述和 App 内更新提示。

格式：`## [versionName]` 一节对应一个 release，节内自由 Markdown。CI 用 `## [versionName]` 行作为切分锚。

---

## [Unreleased]

> 下次发版前在这里累积条目。打 tag 之前把 `[Unreleased]` 改成 `[X.Y.Z]`，再加新的空 `## [Unreleased]` 占位。

### Added
- （待填）

### Changed
- （待填）

### Fixed
- （待填）

---

## [0.1.1] - 2026-06-11

### Changed
- 栗记主界面、录入表单、设置页与智能录入继续收敛为 Chestnut 风格。
- 记录摘要改为运行时根据结构化字段生成，减少 payload 冗余并兼容旧数据。
- Smoke 测试基建去重，DailyBabySummary 模型合并。

### Fixed
- 适配系统底部三键导航栏，避免底部保存按钮、免责声明确认按钮和智能录入按钮贴底或被遮挡。

---

## [0.1.0] - 2026-05-25

首个公开版本兜底基线条目。

### 包含主线
- 孕期记录：B超 / 产检（常规层 + 7 类 screening_*）/ 孕妈指标 / 胎动 / 宫缩 / FGR 曲线 / DueDate 计算器 / 提醒中心
- 育儿期骨架：feed / sleep / diaper / temperature / medication 等 11 个事件类型可记录可编辑
- AI 识别：B 超 OCR / 产检 OCR / 智能录入语音转写
- 家庭同步 S1-S5：HKDF + AES-GCM E2EE 推拉、附件文件加密上下行、24h 自动拉取、防回路写入
- 系统：首启免责门、本地备份导入导出、记录搜索、回收站、PocketBase 连接检测
- 工程：detekt 行数闸值、文件级拆分、smoke 覆盖核心链路
- App 自动更新：半自动 APK 升级（设置页"检查更新"→ 下载 → SHA-256 校验 → 系统安装器）
