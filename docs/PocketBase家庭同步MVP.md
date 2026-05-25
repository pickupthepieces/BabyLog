# PocketBase 家庭同步 MVP（S4 E2EE Push / Pull）

## 目标

让同一个家庭内的几台手机共享 BabyLog 记录，同时保持当前产品边界：

- 不做第三方账号登录。
- 不同步模型 API Key、语音 Key、免责声明确认状态、提醒本地状态。
- AI / OCR / STT 只生成候选，用户手动保存后才进入同步队列。
- 首轮只做家庭密钥式轻量同步，不做复杂 RBAC。
- S4 已覆盖本机加密推送、自动拉取、静默 LWW 合并和防回路；附件文件上传、密钥轮换和 realtime 后置。

## 核心安全边界

- 服务端只见 `familyKeyHash`、同步游标和密文。
- 服务端不见明文家庭密钥。
- 服务端不见 `entityType` / `eventType` / `occurredAt` / `attachmentIds` / `payload` 内任何字段。
- 所有业务实体统一进入 `encrypted_records`，明文多 collection 已废弃。

## PocketBase 基础端点

- 健康检查：`GET /api/health`。
- 家庭检测：`GET /api/collections/families/records?filter=familyKeyHash="..."`。
- 加密记录上行：`POST /api/collections/encrypted_records/records`。
- 加密记录拉取：`GET /api/collections/encrypted_records/records?filter=familyKeyHash="..."&sort=updatedAtClient&page=...&perPage=...`。

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
| `updatedAtClient` | text | yes | ISO8601，S4 pull 游标用 |
| `deletedFlag` | number | yes | 0 / 1 |

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

`entityType`、`eventType`、`occurredAt`、`attachmentIds`、`childId`、`familyId`、`payload` 都不再作为 collection 明文字段存在。

## Collection Rules 建议

首轮仍以私有 VPS + 足够长家庭密钥为边界，PocketBase rules 保持最小开放。S4 需要 `encrypted_records` 开放 list / view；客户端只通过追加密文行表达更新和软删，服务端 hard delete 始终关闭。

建议：

- `families`：仅允许带 `familyKeyHash` filter 的 list，用于连接检测。
- `encrypted_records`：
  - list / view：`familyKeyHash = @request.headers.x_babylog_family_key`。
  - create：请求 body 的 `familyKeyHash` 必须等于 `X-BabyLog-Family-Key` header。
  - update：如需开放，仅允许同一 `familyKeyHash` 下改 `deletedFlag` / `nonce` / `ciphertext` / `updatedAtClient` 等密文字段；当前 App 正常同步不依赖 update。
  - delete：关闭。删除记录走新增一条 `deletedFlag=1` 的密文变更，客户端拉取后本地软删。

建议在 PocketBase hook 中额外校验 header hash 与目标行一致。

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

S4 客户端会在以下时机拉取：

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
- 上次拉取时间
- 本轮新拉取数量
- 立即推送 / 立即拉取

## clientId 语义与 row 膨胀

`clientId` = 本机 `SyncChange.id`（UUID v4，无业务语义），不是 entity 自己的 id。

含义：

- 同一 entity 多次改 = N 个 SyncChange = N 行 `encrypted_records`（解密后 entityId 相同）
- Server **不做**自动 LWW upsert，row 数 = 同步活动量，不是 entity 数量
- 拉取时客户端必须按解密后 `(entityType, entityId)` 分组 + `updatedAtClient` 取最新（已实现于 `BabyLogSyncPullOrchestrator`）
- 长期使用 server row 可能是 entity 数的 5-10 倍

元数据泄漏面：攻击者拿 server DB 能看到“发生过 N 次同步事件”，但看不到 entity 类型 / 时间分布（这些都在密文里）。可接受。

S5+ 含义：附件文件如果走 PocketBase `file` 字段挂在同一 `encrypted_records` 行，每次 attachment metadata 改一次都会重传文件。S5 实现必须按 `(entityType=attachment, entityId)` 去重，**只上传最新版本对应的文件**，避免文件级膨胀。

## App 侧现状

已完成：

- 本机加密保存家庭密钥。
- Android backup / device-transfer 排除 `babylog_sync_secrets.xml`。
- 家庭同步协议白名单。
- PocketBase health + family lookup 连接检测。
- 家庭密钥派生已切到 HKDF lookup hash；旧裸 SHA-256 hash 失效，需要用户重新输入家庭密钥，让服务端 `families.familyKeyHash` 使用新值。

S3 / S4 已完成：

- S3：加密封装本机 pending sync changes 并推送到 `encrypted_records`。
- S4：自动拉取、前台轮询、后台 worker、下拉刷新、静默 LWW 合并、防回路写入路径和同步信息条。

仍未完成：

- 附件文件上传 / 下载。
- 家庭密钥轮换。
- PocketBase realtime。
