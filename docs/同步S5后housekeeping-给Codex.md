# 同步 S5 后 housekeeping — 给 Codex

> 衔接：S5 附件同步主线已落地并合 main（b600b62）。本文 = 育儿期主线化启动前的累积小问题清算。
>
> 6 项打包，可拆成 3 笔 commit（推荐）或 2 笔大 commit。每笔后 `:app:assembleDebug` + `:app:lintDebug` + `:app:detekt` + 全量 JVM smoke 必过。
>
> 总工期：一天。

## 0. 红线

- 不动加密协议（HKDF / AES-GCM / AAD 一字不改）
- 不动业务逻辑（不动 record* / update* / 推拉编排 / FGR / STT / Disclaimer）
- 不动 UI 视觉
- 所有改动**外部行为等价**或**修 bug**，不引入新功能
- 留在 `codex/stage-mainline-refactor` 分支

## 1. 六项内容

### H1 · 拆 `BabyLogAttachmentBlobStore`（必做，闸值耗尽）

**问题**：`BabyLogRepository.java` 696 行 / 700 闸值，**只剩 4 行缓冲**。任何往 Repository 加方法必撞顶。

**做法**：把 attachment blob 相关的本机存储路径抽到独立类，参考 `BabyLogSyncSecretStore` 的独立 prefs file 模式：

新建 `BabyLogAttachmentBlobStore.java`：

```java
public final class BabyLogAttachmentBlobStore {
    public static final String PREF_FILE_NAME = "babylog_attachment_blobs";
    // 迁移：从 BabyLogRepository 既有的 attachmentBlobs / attachmentDownloadQueue 字段搬过来

    public boolean putAttachmentBlobFromRemote(String attachmentId, byte[] bytes, String contentHash);
    public boolean hasAttachmentBlob(String attachmentId);
    public String attachmentBlobContentHash(String attachmentId);
    public byte[] findAttachmentBlobBytes(String attachmentId);
    public List<AttachmentDownloadRequest> listAttachmentDownloadQueue();
    public void enqueueAttachmentDownload(...);
    public void removeFromAttachmentDownloadQueue(...);
}
```

**MUST**：
- 现有 `BabyLogRepository` 保留方法签名作 facade，内部 delegate（**外部调用方零改动**）
- prefs file 名独立，**进 backup_rules.xml 和 data_extraction_rules.xml 的排除清单**（attachment blob 是大对象，跨设备应靠 sync 而不是 backup）
- 旧 `attachmentBlobs` key 数据**必须迁移**：第一次构造 BabyLogAttachmentBlobStore 时检测旧 prefs 是否有数据，有则一次性搬过来再清空。这是避免现有用户数据丢失的关键。

**验收**：
- `BabyLogRepository.java` < 600 行（留下面 100 行的缓冲）
- 新建独立 prefs file 且进 backup 排除
- 现有 smoke 全过（包括 attachment 拉取 / 下载 / 回路 smoke）
- 加新 smoke `BabyLogAttachmentBlobStoreSmokeTest`：基本 put/has/find round-trip + 不同 attachmentId 隔离

### H2 · 拆 `BabyLogSyncAttachmentPushHelper`（必做，闸值耗尽）

**问题**：`BabyLogService.java` 2188 行 / 2200 闸值，**只剩 12 行缓冲**。S5 加附件后任何业务方法新增必撞顶。

**做法**：`BabyLogSyncPushOrchestrator` 里现有的附件 multipart 编排（multipart body 拼装 + boundary 处理 + 流量控制）独立到新文件 `BabyLogSyncAttachmentPushHelper.java`，Push orchestrator 调它。

注意：闸值是 Service.java 不是 PushOrchestrator.java，但 Service 的扩展余量不足是因为 Service 里有 attachment 相关 input model / helper（如 `createUltrasoundAttachmentBlob` 路径）。也可以选择把这些往 Push helper 搬。

**两种拆分思路**（Codex 自选）：
- **A**：拆 `BabyLogSyncAttachmentPushHelper`，目标是让 `BabyLogSyncPushOrchestrator` 减重，连带让 Service 里调它的代码简化
- **B**：拆 `BabyLogAttachmentInputBuilder`，把 attachment input model 构造和 blob 读取逻辑从 Service 搬出

**MUST**：
- `BabyLogService.java` < 2000 行（留 200 行缓冲给育儿期主线化）
- 外部调用方零改动（facade 兼容）
- 现有 smoke 全过

### H3 · `dismissRemoteUpdateBanner` 不走全量 reloadData（顺手优化）

**问题**：S4 W2 累积，dismiss banner 只改一个 int 字段，但走全量 `reloadData()` 重读 events / dashboard / reminders / 等。

**做法**：在 `BabyLogService` 加：

```java
public DashboardSnapshot refreshDashboardOnly();
// 只重算 dashboard 字段，不动 timeline / reminders / etc
```

CMA `dismissRemoteUpdateBanner` 改成：

```kotlin
private fun dismissRemoteUpdateBanner() {
    runInBackground {
        service.dismissRemoteUpdateBanner()
        val dashboard = service.refreshDashboardOnly()
        runOnUiThread {
            uiState = uiState.copy(dashboard = dashboard)
        }
    }
}
```

**验收**：dismiss 操作 < 50ms 感知；其它字段无副作用。

### H4 · `lastPulledAt` 显示用 `relativeTimeFromNow`（顺手 UX）

**问题**：S4 W4 累积，`SyncSettingsScreen` 当前显示裸 ISO 字符串 `2026-05-25T12:00:00.000+0800`。

**做法**：在 `BabyLogFormatters` 加：

```java
public static String relativeTimeFromNow(String iso) {
    // 5s内 → "刚刚"
    // < 60s → "x 秒前"
    // < 60min → "x 分钟前"
    // < 24h → "x 小时前"
    // < 7d → "x 天前"
    // 其它 → 原 ISO 截到日期
}
```

`SyncSettingsScreen` 调用展示。

**验收**：smoke 加 `BabyLogFormattersSmokeTest` 覆盖 5 个分支。

### H5 · `BabyLogDomain.SyncChange.status` 状态枚举集中文档化（W4 累积）

**问题**：S5 W4，`metadata_synced_file_pending` 是 S5 新增状态，但完整状态集没在 doc 里固化。未来维护者要 grep 代码才能列全。

**做法**：在 `docs/PocketBase家庭同步MVP.md` `## clientId 语义` 之后插一段：

```markdown
## SyncChange 状态机

`BabyLogDomain.SyncChange.status` 字段取值：

| 状态 | 含义 | 进入条件 | 离开条件 |
|---|---|---|---|
| `pending` | 待推送，未尝试或上次成功 | 本机写入后创建 | push 成功 → synced；网络/加密失败 → failed；附件 metadata 已推但文件待传 → metadata_synced_file_pending |
| `failed` | 推送失败 | 网络异常 / 加密失败 / server 错误 | 下次 push 重试，成功 → synced |
| `synced` | 已成功推送到 server | 推送成功 | 永久（除非 entity 再次修改产生新 SyncChange） |
| `metadata_synced_file_pending` | metadata 已推但文件待传（仅 attachment） | attachment metadata 推成功但文件触发流量限制 / 网络失败 | 文件成功上传 → synced；继续失败 → failed |

`pull` orchestrator 仅看 `status != "synced"` 重试。`pushOnce` 排序按 `updatedAtClient` 升序。
```

### H6 · PocketBase `/api/files/...` URL 注脚（W3 累积）

**问题**：S5 W3，文件下载 URL `/api/files/encrypted_records/{recordId}/{filename}` 用 collection name 而不是 collection ID。PocketBase 当前同时接受 name 和 ID，但 doc 没注明。

**做法**：在 `docs/PocketBase家庭同步MVP.md` `## PocketBase 基础端点` 后加一行：

```markdown
注：`/api/files/...` 的第一段 path 在 PocketBase 文档里官方是 collection ID，但 PocketBase 实际同时接受 collection name（如 `encrypted_records`）。BabyLog 客户端用 name 简化实现。如 PocketBase 未来某版本仅接受 ID，需要先 GET `/api/collections/encrypted_records` 取 `id` 再拼 URL。
```

## 2. 提交拆分建议

**方案 A（推荐，3 笔）**：

- **commit 1** `refactor: 拆 BabyLogAttachmentBlobStore` —— H1
- **commit 2** `refactor: 拆 BabyLogSyncAttachmentPushHelper` —— H2
- **commit 3** `chore: 同步 UI 优化与文档补全` —— H3 + H4 + H5 + H6

**方案 B（2 笔）**：

- **commit 1** `refactor: 拆 attachment blob 与 push helper 减闸值压力` —— H1 + H2
- **commit 2** `chore: 同步 UI 优化与文档补全` —— H3 + H4 + H5 + H6

Codex 自选。H1 H2 是分别拆两个文件，并入一笔也无明显坏处。

## 3. 验收清单

- [ ] H1 `BabyLogAttachmentBlobStore` 拆出，旧数据迁移逻辑就位，prefs 进 backup 排除
- [ ] H2 `BabyLogSyncAttachmentPushHelper`（或等价 helper）拆出，Service / PushOrchestrator 减重
- [ ] H1+H2 后：`BabyLogRepository.java` < 600 行 / `BabyLogService.java` < 2000 行
- [ ] H3 dismiss 走 `refreshDashboardOnly()`
- [ ] H4 lastPulledAt 显示用 `relativeTimeFromNow`
- [ ] H5 doc 加 SyncChange 状态机表格
- [ ] H6 doc 加 `/api/files/...` collection ID/name 注脚
- [ ] 每笔 commit 后 `:app:assembleDebug` + `:app:lintDebug` + `:app:detekt` + 全量 JVM smoke 绿
- [ ] 不动加密协议 / 业务逻辑 / UI 视觉
- [ ] 留在 `codex/stage-mainline-refactor` 分支
