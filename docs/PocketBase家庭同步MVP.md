# PocketBase 家庭同步 MVP（S5 E2EE + 附件文件）

## 目标

让同一个家庭内的几台手机共享 BabyLog 记录，同时保持当前产品边界：

- 不做第三方账号登录。
- 不同步模型 API Key、语音 Key、免责声明确认状态、提醒本地状态。
- AI / OCR / STT 只生成候选，用户手动保存后才进入同步队列。
- 首轮只做家庭密钥式轻量同步，不做复杂 RBAC。
- S5 已覆盖本机加密推送、自动拉取、静默 LWW 合并、防回路，以及附件文件加密上传 / 按需下载；密钥轮换和 realtime 后置。

## 核心安全边界

- 服务端只见 `familyKeyHash`、同步游标和密文。
- 服务端不见明文家庭密钥。
- 服务端不见 `entityType` / `eventType` / `occurredAt` / `attachmentIds` / `payload` 内任何字段。
- 服务端不见附件文件明文。附件文件使用独立 `attachmentKey` 加密后写入 PocketBase `file` 字段。
- 所有业务实体统一进入 `encrypted_records`，明文多 collection 已废弃。

## PocketBase 基础端点

- 健康检查：`GET /api/health`。
- 家庭检测：`GET /api/collections/families/records?filter=familyKeyHash="..."`。
- 加密记录上行：`POST /api/collections/encrypted_records/records`。
- 加密记录拉取：`GET /api/collections/encrypted_records/records?filter=familyKeyHash="..."&sort=updatedAtClient&page=...&perPage=...`。
- 附件密文上传：`PATCH /api/collections/encrypted_records/records/{recordId}`，`multipart/form-data`。
- 附件密文下载：`GET /api/files/encrypted_records/{recordId}/{filename}`。

注：`/api/files/...` 的第一段 path 在 PocketBase 文档里官方是 collection ID，但 PocketBase 实际同时接受 collection name（如 `encrypted_records`）。BabyLog 客户端用 name 简化实现。如 PocketBase 未来某版本仅接受 ID，需要先 `GET /api/collections/encrypted_records` 取 `id` 再拼 URL。

## Collection 规划

### families

用于“服务器地址 + 家庭密钥”连接检测。新家庭没有任何记录时，仍可通过管理员预建的 `families` 行确认家庭存在。

字段：

- `name` text，可空，仅显示
- `clientFamilyId` text，App 侧家庭 id，首轮固定 `local-family`
- `familyKeyHash` text，等于 `BabyLogFamilyKeyDeriver.lookupHashHex(familyKey)` 的 64 位 hex
- `status` select：`active` / `rotated` / `disabled`
- `createdByDevice` text，可空

索引：

- `familyKeyHash`
- `clientFamilyId`

连接检测：

1. App 调 `GET /api/health` 确认服务器可达。
2. App 对家庭密钥做 HKDF 派生，取 `lookupHashHex(familyKey)`。
3. App 调 `GET /api/collections/families/records?filter=familyKeyHash="..."`。
4. 返回 `items.length > 0` 才认为家庭可连接。

注意：URL 和 `X-BabyLog-Family-Key` header 都只出现 lookup hash，不传明文家庭密钥。

### encrypted_records

唯一的业务同步集合。原 `family_profiles` / `child_profiles` / `family_members` / `events` / `attachments` 明文集合在 S3 后不再使用。

字段：

| 字段 | 类型 | 索引 | 备注 |
|---|---|---|---|
| `clientId` | text | unique | 客户端生成 UUID v4，无业务语义 |
| `familyKeyHash` | text | yes | 等于 `BabyLogFamilyKeyDeriver.lookupHashHex(familyKey)` |
| `schemaVersion` | number | no | 当前 = `BabyLogDomain.SCHEMA_VERSION` |
| `cipherVersion` | number | no | 当前 = 1 |
| `nonce` | text | no | base64(12 字节 AES-GCM nonce) |
| `ciphertext` | text | no | base64(AES-GCM ciphertext + tag) |
| `updatedAtClient` | text | yes | ISO8601，pull 游标用 |
| `deletedFlag` | number | yes | 0 / 1 |
| `attachmentFile` | file | no | 可空；仅 attachment 实体使用；单文件，建议 maxSize 5MB，accept `application/octet-stream` |
| `attachmentFileVersion` | text | no | 客户端写入，等于本机附件 `contentHash` 或文件 SHA-256，用于跳过重复下载 |

加密前 plaintext 形态：

```json
{
  "entityType": "event",
  "entityId": "evt_xxx",
  "payload": { "...": "原 entity 完整 JSON" },
  "occurredAt": "2026-05-25T09:30:00.000+0800",
  "attachmentIds": ["att_xxx"]
}
```

`entityType`、`eventType`、`occurredAt`、`attachmentIds`、`childId`、`familyId`、`payload` 都不再作为 collection 明文字段存在。服务端不知道哪一行是 attachment；`attachmentFile` 是 optional file，由客户端决定何时填写。

## Collection Rules 建议

首轮仍以私有 VPS + 足够长家庭密钥为边界，PocketBase rules 保持最小开放。客户端需要 `encrypted_records` 开放 list / view；业务更新和软删通过追加密文行表达，服务端 hard delete 始终关闭。

建议：

- `families`：仅允许带 `familyKeyHash` filter 的 list，用于连接检测。
- `encrypted_records`：
  - list / view：`familyKeyHash = @request.headers.x_babylog_family_key`。
  - create：请求 body 的 `familyKeyHash` 必须等于 `X-BabyLog-Family-Key` header。
  - update：S5 需要允许同一 `familyKeyHash` 下对已创建行补写 `attachmentFile` / `attachmentFileVersion`；其它业务更新仍通过追加新密文行表达。
  - delete：关闭。删除记录走新增一条 `deletedFlag=1` 的密文变更，客户端拉取后本地软删。

建议在 PocketBase hook 中额外校验 header hash 与目标行一致。

### S5 后台配置清单

在 `encrypted_records` collection 增加两个 optional 字段：

| 字段 | 类型 | 规则 |
|---|---|---|
| `attachmentFile` | file | 单文件，maxSize 5MB，建议 accept `application/octet-stream` |
| `attachmentFileVersion` | text | 可空 |

List / View rule MUST 保持 `familyKeyHash = @request.headers.x_babylog_family_key`，因为 `/api/files/...` 下载同样依赖记录可见性。Update rule MUST 至少允许同一家庭 hash 下补写 `attachmentFile` / `attachmentFileVersion`。

## S4 拉取与合并

### 推送

本机记录、档案、附件元数据等写入成功后进入 pending sync queue。自动推送 worker 在有网络时把待同步实体封装成 plaintext，再用家庭密钥派生出的 `dataKey` 做 AES-GCM 加密，最后追加到 `encrypted_records`。

服务端只保存：

- `clientId`
- `familyKeyHash`
- `schemaVersion`
- `cipherVersion`
- `nonce`
- `ciphertext`
- `updatedAtClient`
- `deletedFlag`

服务端不保存明文 `entityType`、`eventType`、`occurredAt`、`payload` 或附件 id。

### 拉取

客户端会在以下时机拉取：

- App 前台恢复时立即拉一次。
- 前台运行时每 2 分钟静默拉一次。
- 后台 WorkManager 每 15 分钟、有网络约束下拉一次。
- 用户在首页 / 时间线下拉刷新，或在同步设置页点“立即拉取”。

未配置同步时不启动轮询：`backend.enabled=false`、后端地址为空或本机无家庭密钥时，前台 / 后台拉取都会完全静默停止。

### LWW 与防回路

拉取结果按 `entityType + entityId` 去重，只保留 `updatedAtClient` 最新的一条。写入本机前再和本机 `updatedAt` 比较：

- 远端更新更新于本机：应用远端版本。
- 远端旧于或等于本机：忽略。

冲突不弹窗，统一静默 LWW。应用远端版本时必须走 `BabyLogRepository.putEntityFromRemote(...)`，这条路径不会写 `SyncChange`，因此不会把拉取来的数据再次推送回服务器造成同步回路。

### UI 提示

每轮拉取若应用了远端更新，会累计 `remoteUpdateBannerCount`。首页和时间线顶部显示轻量信息条“已同步 N 条家人更新”，用户可关闭清零。用户若想查看具体变化，自行进入时间线。

同步设置页展示：

- 待同步 / 已推送 / 失败
- 附件待上传数量 / 字节数
- 附件待下载数量
- 上次拉取时间
- 本轮新拉取数量
- 立即推送 / 立即拉取

## S5 附件文件同步

### 密钥与文件格式

附件 metadata 仍走 S3/S4 的 `dataKey` 加密链。附件文件本体 MUST 使用同一家庭密钥派生出的独立 `attachmentKey`：

```text
attachmentKey = HKDF(familyKey, info = "attachment/v1")
```

上传到 PocketBase 的文件内容 MUST 是：

```text
[12-byte nonce][AES-GCM ciphertext + tag]
```

AES-GCM AAD MUST 为：

```text
attachmentId + "|" + cipherVersion + "|" + familyKeyHash
```

### 上传

附件上传分两阶段：

1. 先按普通 entity 将 attachment metadata 加密后 `POST /api/collections/encrypted_records/records`。
2. 拿到 PocketBase 返回的 server-side `id` 后，再 `PATCH /api/collections/encrypted_records/records/{id}` 上传 `attachmentFile` 和 `attachmentFileVersion`。

客户端 MUST 限流：单次 push 最多上传 3 个附件文件，总明文字节数最多 10 MiB。文件上传失败时，metadata 可先保持已同步，`SyncChange.status` 保留为 `metadata_synced_file_pending`，下次 push 继续重试。

### 拉取与下载

拉取 `encrypted_records` 时，客户端解密 metadata 后如果发现：

- `entityType = attachment`
- 远端行带 `attachmentFile`
- 本机没有对应 blob，或本机 `contentHash` 与 `attachmentFileVersion` 不一致

则 MUST 把 `(attachmentId, remoteRecordId, filename, attachmentFileVersion)` 写入本机下载队列。

下载 worker MUST：

1. GET `/api/files/encrypted_records/{recordId}/{filename}`。
2. 使用 `attachmentKey` + AAD 解密文件。
3. 调 `BabyLogRepository.putAttachmentBlobFromRemote(...)` 写入本机。
4. 成功后移除下载队列。

远端附件落地 MUST NOT 写 `SyncChange`，否则会形成拉取→写入→再推送的同步回路。

### UI 行为

- 附件列表中，缺少本机 blob 的条目显示“等待下载”占位。
- 附件预览页中，缺少本机 blob 时显示下载中占位符，不显示破图。
- 同步设置页展示“附件待上传：N 个 / X”和“附件待下载：N 个”。
- 用户点“立即推送”或“立即上传附件”时走同一 push 编排。

## clientId 语义与 row 膨胀

`clientId` = 本机 `SyncChange.id`（UUID v4，无业务语义），不是 entity 自己的 id。

含义：

- 同一 entity 多次改 = N 个 SyncChange = N 行 `encrypted_records`（解密后 entityId 相同）
- Server **不做**自动 LWW upsert，row 数 = 同步活动量，不是 entity 数量
- 拉取时客户端必须按解密后 `(entityType, entityId)` 分组 + `updatedAtClient` 取最新（已实现于 `BabyLogSyncPullOrchestrator`）
- 长期使用 server row 可能是 entity 数的 5-10 倍

元数据泄漏面：攻击者拿 server DB 能看到“发生过 N 次同步事件”，但看不到 entity 类型 / 时间分布（这些都在密文里）。可接受。

S5 含义：附件文件走 PocketBase `file` 字段挂在同一 `encrypted_records` 行。客户端按 `(entityType=attachment, entityId)` 去重，只下载最新版本对应的文件；上传侧每次 attachment metadata 有待同步时，会尝试补传该版本文件。

## SyncChange 状态机

`BabyLogDomain.SyncChange.status` 字段取值：

| 状态 | 含义 | 进入条件 | 离开条件 |
|---|---|---|---|
| `pending` | 待推送，未尝试或上次成功 | 本机写入后创建 | push 成功 → synced；网络/加密失败 → failed；附件 metadata 已推但文件待传 → metadata_synced_file_pending |
| `failed` | 推送失败 | 网络异常 / 加密失败 / server 错误 | 下次 push 重试，成功 → synced |
| `synced` | 已成功推送到 server | 推送成功 | 永久（除非 entity 再次修改产生新 SyncChange） |
| `metadata_synced_file_pending` | metadata 已推但文件待传（仅 attachment） | attachment metadata 推成功但文件触发流量限制 / 网络失败 | 文件成功上传 → synced；继续失败 → failed |

`pull` orchestrator 仅看 `status != "synced"` 重试。`pushOnce` 排序按 `updatedAtClient` 升序。

## App 侧现状

已完成：

- 本机加密保存家庭密钥。
- Android backup / device-transfer 排除 `babylog_sync_secrets.xml`。
- 家庭同步协议白名单。
- PocketBase health + family lookup 连接检测。
- 家庭密钥派生已切到 HKDF lookup hash；旧裸 SHA-256 hash 失效，需要用户重新输入家庭密钥，让服务端 `families.familyKeyHash` 使用新值。

S3 / S4 / S5 已完成：

- S3：加密封装本机 pending sync changes 并推送到 `encrypted_records`。
- S4：自动拉取、前台轮询、后台 worker、下拉刷新、静默 LWW 合并、防回路写入路径和同步信息条。
- S5：附件文件独立加密上传、按需下载、下载落地不产生 SyncChange、同步设置附件计数、缺本机 blob 的附件占位。

仍未完成：

- 家庭密钥轮换。
- PocketBase realtime。
