# PocketBase 家庭同步 MVP（S3 E2EE Push Only）

## 目标

让同一个家庭内的几台手机共享 BabyLog 记录，同时保持当前产品边界：

- 不做第三方账号登录。
- 不同步模型 API Key、语音 Key、免责声明确认状态、提醒本地状态。
- AI / OCR / STT 只生成候选，用户手动保存后才进入同步队列。
- 首轮只做家庭密钥式轻量同步，不做复杂 RBAC。
- S3 只做本机到家庭后端的加密推送，不做拉取、附件文件上传、密钥轮换或 realtime。

## 核心安全边界

- 服务端只见 `familyKeyHash`、同步游标和密文。
- 服务端不见明文家庭密钥。
- 服务端不见 `entityType` / `eventType` / `occurredAt` / `attachmentIds` / `payload` 内任何字段。
- 所有业务实体统一进入 `encrypted_records`，明文多 collection 已废弃。

## PocketBase 基础端点

- 健康检查：`GET /api/health`。
- 家庭检测：`GET /api/collections/families/records?filter=familyKeyHash="..."`。
- 加密记录上行：`POST /api/collections/encrypted_records/records`。

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

首轮仍以私有 VPS + 足够长家庭密钥为边界，PocketBase rules 保持最小开放。

建议：

- `families`：仅允许带 `familyKeyHash` filter 的 list，用于连接检测。
- `encrypted_records`：
  - list / view：`familyKeyHash = @request.headers.x_babylog_family_key`。
  - create：请求 body 的 `familyKeyHash` 必须等于 `X-BabyLog-Family-Key` header。
  - update / delete：S3 push only 暂不开放，S4 拉取 / 合并阶段再补。

建议在 PocketBase hook 中额外校验 header hash 与目标行一致。

## App 侧现状

已完成：

- 本机加密保存家庭密钥。
- Android backup / device-transfer 排除 `babylog_sync_secrets.xml`。
- 家庭同步协议白名单。
- PocketBase health + family lookup 连接检测。
- 家庭密钥派生已切到 HKDF lookup hash；旧裸 SHA-256 hash 失效，需要用户重新输入家庭密钥，让服务端 `families.familyKeyHash` 使用新值。

S3 进行中：

- 加密封装本机 pending sync changes。
- 推送到 `encrypted_records`。
- 设置页显示待推送 / 已推送 / 失败计数，并提供用户主动触发的“立即推送”。

未完成：

- 服务端拉取与合并。
- 附件文件上传 / 下载。
- 家庭密钥轮换。
- PocketBase realtime。
