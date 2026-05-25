# 家庭同步 S5 方案 — 给 Codex

> 衔接：S3 push-only / S4 自动 push-pull / housekeeping 完成后启动。本文 = S5 主线：**附件文件跨设备同步**。
>
> 范围：把 attachment 的文件本体（照片）真正传到家人手机上。S4 当前只同步 metadata，导致拉到的 attachment record 点开是空白。

## 0. 红线（先看）

- **加密边界不变**：复用 S3/S4 的 `BabyLogFamilyKeyDeriver` / `BabyLogPayloadCipher` / `encrypted_records` schema 加密 metadata 那一面；文件本体**独立 AES-GCM 加密**后再走 PocketBase `file` 字段。
- **server 既不见明文 metadata，也不见明文文件**。
- 不引入第三方账号 / 不联 GMS / 不同步 `smartConfig` / `reminder` / `preVisitQuestion` / `disclaimerConsent`。
- 不动 `BabyLogDomain` 事件模型、不动 FGR、不动 STT、不动免责声明、不动 launcher 视觉。
- `applicationId` / 包名 / GitHub repo / 所有 import / smoke 一切不动。
- 不破坏 S4 的 metadata push/pull 链路（只新增文件上下行，不改 metadata 加密格式）。
- **防回路第一原则继续生效**：远端拉来的文件落到本机 `attachmentBlobs` 时不得 emit 任何 SyncChange。
- 不预设医学建议、不写"异常 / 严重 / 危险"等词。

## 1. 设计目标

| # | 目标 |
|---|---|
| 1 | 本机新增 / 编辑 attachment 时，文件本体自动加密上传到 PocketBase |
| 2 | 拉取 attachment metadata 时如果对应文件未在本机，自动下载并解密落地 |
| 3 | 文件级 LWW：同 attachmentId 多版本只下载最新一版的文件 |
| 4 | 失败重试，不阻塞 metadata 同步 |
| 5 | 流量友好：单次 push 最多上传 3 个附件（避免一次上传几十个 MB） |
| 6 | UI 反馈：附件预览未下载时显示"下载中" + 占位符 |

## 2. 加密方案

### 2.1 文件密钥派生

复用 S3 的 HKDF 工具，新增一个 `attachment/v1` info 派生 attachmentKey：

```
familyKey
  → BabyLogFamilyKeyDeriver
  → HKDF-Expand(info="attachment/v1") → 32 字节 attachmentKey
```

需要在 `BabyLogFamilyKeyDeriver` 加：
- `public static byte[] deriveAttachmentKey(String familyKey)`
- KAT smoke：与现有 `lookupKey` / `dataKey` / `indexKey` 都不同，且确定性

### 2.2 文件加密格式

每个 attachment 文件上传前：

```
nonce = SecureRandom(12 字节)
aad = (attachmentId + "|" + cipherVersion + "|" + familyKeyHash).getBytes(UTF-8)
ciphertext_with_tag = AES-GCM-Encrypt(attachmentKey, nonce, plaintext_bytes, aad)

uploadPayload = [nonce(12 bytes)] + [ciphertext_with_tag]
```

上传到 PocketBase `file` 字段的就是 `uploadPayload`（前 12 字节是 nonce，后面是密文+tag）。

下载时反向：
```
downloaded = bytes from PocketBase file
nonce = downloaded[0:12]
ciphertext_with_tag = downloaded[12:]
aad = (attachmentId + "|" + cipherVersion + "|" + familyKeyHash).getBytes(UTF-8)
plaintext = AES-GCM-Decrypt(attachmentKey, nonce, ciphertext_with_tag, aad)
```

### 2.3 不复用 dataKey

`dataKey` 用于 metadata 加密，频繁使用；`attachmentKey` 用于文件加密，单独派生让密钥用途隔离（NIST SP 800-108 派生分用原则）。两者都从同一 `familyKey` 派生，无新增配置项。

### 2.4 新建工具类

```java
public final class BabyLogAttachmentCipher {
    public static byte[] sealFile(byte[] attachmentKey, byte[] aad, byte[] plaintext)
        throws GeneralSecurityException;
        // 返回 [12 nonce][ciphertext+tag]

    public static byte[] openFile(byte[] attachmentKey, byte[] aad, byte[] sealed)
        throws GeneralSecurityException;
        // 解析 sealed[0:12] 作 nonce，sealed[12:] 作 ciphertext+tag，返回 plaintext
}
```

Smoke：

- `sealFile` → `openFile` round-trip 一致
- 改一字节 sealed → 必失败
- 错 attachmentKey → 必失败
- 改 AAD → 必失败
- 大文件（2MB 随机字节）round-trip 一致

## 3. PocketBase Schema 改动

### 3.1 `encrypted_records` collection 加字段

| 字段 | 类型 | 备注 |
|---|---|---|
| `attachmentFile` | file | 单文件，maxSize 5MB（PocketBase admin 后台配置），可空 |
| `attachmentFileVersion` | text | 客户端写入，等于该附件本机 `contentHash`，便于跳过下载相同内容 |

只有 `entityType=attachment`（在密文里）的 row 会有 `attachmentFile`，其它 entity 类型该字段始终空。

**注意**：PocketBase 的 file 字段类型 server 不知道客户端的"entityType=attachment"（这在密文里），所以 schema 上 `attachmentFile` 是 optional file。客户端逻辑决定何时填充。

### 3.2 Collection Rules 调整

```
encrypted_records:
  Create rule: 同 S4
  List rule:   同 S4（list 时不下载 file，只返回 metadata 字段）
  View rule:   同 S4（View 单条时，PocketBase 默认 file URL 走 /api/files/...，需要 fileToken 鉴权或公开访问）
  Update rule: 关闭（保持 S4 决定）
  Delete rule: 关闭（保持 S4 决定）
```

**重要**：PocketBase file 下载 URL 形如 `/api/files/{collectionId}/{recordId}/{filename}`，默认是受 collection rule 保护的。客户端拿到 record 后用 `httpClient.get(fileUrl, header=X-BabyLog-Family-Key)` 下载，由 List/View rule 校验 familyKeyHash 一致。

## 4. 上传实现

### 4.1 Push orchestrator 扩展

`BabyLogSyncPushOrchestrator.encryptEntityForPush` 当 entityType=attachment 时额外构造文件上传字段。但 PocketBase 上传文件需要 `multipart/form-data`，与现有 JSON POST 不兼容。

**方案**：拆分成两阶段：

1. **阶段 1**：现有 push 链先把 attachment metadata 加密推上（仍 JSON POST 走 `encrypted_records`）
2. **阶段 2**：metadata 推成功后，紧接着对**有本机 blob 的 attachment**额外发一个 multipart PATCH 上传文件：
   ```
   PATCH /api/collections/encrypted_records/records/{recordId}
   Content-Type: multipart/form-data
   X-BabyLog-Family-Key: <hash>
   
   --boundary
   Content-Disposition: form-data; name="attachmentFile"; filename="<random>.bin"
   Content-Type: application/octet-stream
   
   [sealed file bytes]
   --boundary--
   Content-Disposition: form-data; name="attachmentFileVersion"
   
   <contentHash>
   --boundary--
   ```

`recordId` 是阶段 1 PocketBase 返回的 server-side id（不是 clientId）。

### 4.2 流量控制

`BabyLogSyncPushOrchestrator` 加常量：

```java
private static final int MAX_FILE_UPLOADS_PER_RUN = 3;
private static final long MAX_FILE_BYTES_PER_RUN = 10 * 1024 * 1024;  // 10MB
```

按 metadata 推送顺序逐个上传文件，达到任一上限即停止本轮（剩余的 next push 继续）。

未上传的文件保留本地 blob，下次 push 触发时按 attachmentId 检查 server 是否已有该 contentHash 对应的 file → 没有就再传。

### 4.3 SyncChange 状态扩展

复用现有 `status` 字段，新增值：

- `synced` —— metadata 已推 + 文件已上传（或无文件需上传，即非 attachment）
- `metadata_synced_file_pending` —— metadata 已推但文件待传
- `pending` / `failed` —— 现有

`BabyLogSyncPushOrchestrator` 推完 metadata 后：
- 非 attachment：直接 `synced`
- attachment 且文件本轮上传成功：`synced`
- attachment 且本轮没传文件：`metadata_synced_file_pending`

下次 push 优先处理 `pending` / `failed`，再处理 `metadata_synced_file_pending`。

### 4.4 PushSummary 字段扩展

```java
public final int filesUploaded;
public final int filesPending;
public final long bytesUploaded;
```

UI 显示："已加密推送 N 条 / 文件上传 M 个 / X MB"。

## 5. 下载实现

### 5.1 Pull orchestrator 扩展

`BabyLogSyncPullOrchestrator.applyFetchedRecords` 在 putEntityFromRemote 之后：

```java
if (entityType.equals("attachment") && remoteRecord.hasAttachmentFile()) {
    if (!repository.hasAttachmentBlob(entityId)
        || !repository.attachmentBlobContentHash(entityId).equals(remoteRecord.attachmentFileVersion)) {
        enqueueAttachmentDownload(entityId, remoteRecord);
    }
}
```

`enqueueAttachmentDownload` 把 (entityId, recordId, attachmentFileVersion) 写入新增的 `attachmentDownloadQueue` SharedPreferences key。

### 5.2 `BabyLogSyncAttachmentDownloadWorker`

独立的 WorkManager Worker（不混在 PullWorker 里）：

```java
class BabyLogSyncAttachmentDownloadWorker extends Worker {
    public Result doWork() {
        // 1. 加载 attachmentDownloadQueue
        // 2. 加载 backendConfig + familyKey + attachmentKey
        // 3. 逐个 download:
        //    GET /api/files/{collectionId}/{recordId}/{filename}
        //      Header: X-BabyLog-Family-Key: <hash>
        //    解析 sealed[0:12] nonce, sealed[12:] ciphertext+tag
        //    AES-GCM decrypt
        //    repository.putAttachmentBlob(entityId, plaintext)  ← 不 emit SyncChange
        //    从 queue 移除
        // 4. 流量上限：单次 3 个文件 / 10MB
        // 5. 失败 → 留在 queue，下次重试
    }
}
```

触发：
- pull orchestrator 完成后，如果 queue 非空 → enqueue 一次 download worker
- App ON_RESUME 时如果 queue 非空 → enqueue
- 下拉刷新主动触发后也连带触发 download

### 5.3 `BabyLogRepository` 新增

```java
public boolean putAttachmentBlobFromRemote(String attachmentId, byte[] bytes, String contentHash);
    // 写入 attachmentBlobs key 对应的 entry，更新 attachment 的 contentHash
    // 关键：不 emit SyncChange

public boolean hasAttachmentBlob(String attachmentId);
public String attachmentBlobContentHash(String attachmentId);
```

### 5.4 `BabyLogRemoteSyncClient` 新增

```java
public byte[] downloadAttachmentFile(
    String backendBaseUrl,
    String familyKey,
    String recordId,
    String filename
) throws IOException;
    // GET /api/files/{collectionId}/{recordId}/{filename}
    // Header: X-BabyLog-Family-Key: <hash>
    // 返回 raw bytes (sealed = [12 nonce][ciphertext+tag])

public RecordPushResult uploadAttachmentFile(
    String backendBaseUrl,
    String familyKey,
    String recordId,
    byte[] sealedBytes,
    String contentHashVersion
) throws IOException;
    // PATCH multipart/form-data 上传 file 字段
```

multipart 实现自己拼（不引第三方 HTTP 库），约 50 行 boundary 拼装。

## 6. UI 改动

### 6.1 附件预览页

`AttachmentPreviewScreen` 渲染前判断：

```kotlin
if (!repository.hasAttachmentBlob(attachment.id)) {
    // 显示占位符 + "等待下载..." + Indeterminate progress
    AttachmentPlaceholder(state = AttachmentLoadState.Downloading)
} else {
    // 现有渲染逻辑
}
```

`AttachmentListScreen` thumbnail 同理，缺 blob 时显示带"⏳"的占位图。

### 6.2 同步设置页

`SyncSettingsScreen` 新增字段：

- "附件待上传：N 个 / X MB"
- "附件待下载：N 个"
- "立即上传附件"按钮（手动触发）

### 6.3 不动的

- 不改 launcher / splash / 已锁视觉 token
- 不弹任何附件相关对话框（除现有的"将上传 N 条本机记录"二次确认，文案可加"含附件文件"）

## 7. 提交拆分

| # | commit message | 范围 | smoke |
|---|---|---|---|
| S5-a | `feat: 增加附件密钥派生与文件加密` | `BabyLogFamilyKeyDeriver.deriveAttachmentKey` + `BabyLogAttachmentCipher` | 加新 smoke：派生确定性 + 与其它 key 不同；seal/open round-trip / 大文件 / 改字节失败 |
| S5-b | `feat: 增加附件文件上传 API` | `BabyLogRemoteSyncClient.uploadAttachmentFile` (multipart PATCH) | smoke 用 HttpServer 接 PATCH 抓 multipart body 断言含 nonce+ciphertext |
| S5-c | `feat: 增加附件文件下载 API` | `BabyLogRemoteSyncClient.downloadAttachmentFile` (GET file URL) | smoke 用 HttpServer 返回 sealed bytes 断言 download bytes 一致 |
| S5-d | `feat: 附件上传集成到推送编排` | `BabyLogSyncPushOrchestrator` 加流量控制 + SyncChange 状态 `metadata_synced_file_pending` + Repository.findAttachmentBlobBytes | 加 E2E smoke：push 一条 attachment metadata + file → server 收到两次请求（JSON POST + multipart PATCH）+ 第二次 body 含 sealed bytes |
| S5-e | `feat: 附件下载 Worker + Repository 远端写路径` | `BabyLogSyncAttachmentDownloadWorker` + `BabyLogRepository.putAttachmentBlobFromRemote` + pull orchestrator enqueue download | 加 E2E smoke：mock server 返回 attachment record + sealed file → 调 download → repository 含明文 blob + listSyncChanges() == 0（防回路） |
| S5-f | `feat: 附件 UI 占位符 + 同步设置补附件计数` | `AttachmentPreviewScreen` / `AttachmentListScreen` 占位符 + `SyncSettingsScreen` 计数 + ConfirmDialog 文案加"含附件文件" | — |
| S5-g | `docs: 更新 PocketBase MVP 加 S5 附件章节` | `docs/PocketBase家庭同步MVP.md` 加 S5 段落 + schema 改动说明 + Collection Rules + 流量控制 + README 更新 | — |

每笔 commit 自身可 `:app:assembleDebug` + `:app:lintDebug` + 全量 JVM smoke 过。

## 8. PocketBase 后台改动（用户做）

1. `encrypted_records` collection 加字段：
   - `attachmentFile` (file, single, maxSize 5MB, accept `application/octet-stream`)
   - `attachmentFileVersion` (text, optional)
2. List / View rule 保持 S4（`familyKeyHash = @request.headers.x_babylog_family_key`）—— file URL 受同 rule 保护
3. 测试：用 PocketBase 管理后台手动给一行 record 上传一个文件，确认返回的 `file URL` 加 header 能被客户端 GET

## 9. 端到端 smoke（S5-d / S5-e 核心）

### S5-d push E2E

```java
// 起 HttpServer:
// /api/collections/encrypted_records/records  POST → 返回 {id: "remote_1"}
// /api/collections/encrypted_records/records/remote_1  PATCH (multipart) → 抓 body 存 capturedFileBytes
// 调 pushOnce with 1 attachment + plaintext blob bytes
// 断言:
//   server 收到 2 个请求（POST + PATCH）
//   PATCH body 包含 multipart boundary + name="attachmentFile"
//   提取 multipart 中的文件字节，前 12 是 nonce，后面是 ciphertext+tag
//   用 attachmentKey + AAD 解密，断言等于原始 blob
//   listSyncChanges() 中该 attachment 状态 == "synced"
```

### S5-e pull + download E2E

```java
// 起 HttpServer:
// /api/collections/encrypted_records/records  GET → 返回 1 行 attachment record 含 attachmentFile 字段（URL 指向 mock）
// /api/files/<collectionId>/<recordId>/<filename>  GET → 返回 sealed bytes
// 调 pullOnce
// 断言:
//   metadata 落本机 (entity exists)
//   attachmentDownloadQueue 含该 entityId
//   listSyncChanges() == 0  ← 防回路
// 调 download worker
// 断言:
//   repository.hasAttachmentBlob(entityId) == true
//   解密后的 blob bytes 等于原始 plaintext
//   listSyncChanges() == 0  ← 防回路再确认
//   attachmentDownloadQueue 已清
```

## 10. 不做（明确边界）

- ❌ **流式上传 / 下载**：文件全部读内存。最大 5MB × 3 并发 = 15MB 内存峰值可接受。
- ❌ **下载进度条**（只显示"下载中"占位符，不显示百分比）
- ❌ **后台预下载所有附件**（仅按需 / 显示时触发）
- ❌ **附件压缩 / 转码**：上传前不再压缩（本机存的就是已压缩 2048px JPEG 82）
- ❌ **CDN / 多区域**：单 PocketBase 实例
- ❌ **密钥轮换**（S6+）
- ❌ **PocketBase Realtime**（S7+）

## 11. 验收清单

- [ ] 7 笔 commit 各自独立、消息符合 Conventional Commit
- [ ] `:app:assembleDebug` + `:app:lintDebug` + 全量 JVM smoke 在每笔后都过
- [ ] S5-d push E2E smoke：multipart body 含密文，可解回原文件
- [ ] S5-e pull + download E2E smoke：下载落地 + 防回路 listSyncChanges() == 0
- [ ] 单次 push 上限 3 文件 / 10MB（用例验证）
- [ ] 上传 / 下载失败留 queue 等下次重试
- [ ] 附件预览未下载时显示占位符
- [ ] PocketBase 文档已更新 S5 附件章节 + schema 改动
- [ ] 不动加密 metadata 链 / FGR / STT / Disclaimer / launcher / splash
- [ ] PocketBase 后台改动指引已在 doc

## 12. S5 之后

S6 候选（按需）：
- **家庭密钥轮换**：全量数据重加密 + 服务端配合 + 设备协调
- **PocketBase Realtime**：WebSocket 替代轮询（如果用户嫌 2 分钟太慢）
- **附件级 LWW 冲突**：当前同 attachmentId 后传赢，可考虑保留旧版本到 trash
