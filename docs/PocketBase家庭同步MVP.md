# PocketBase 家庭同步 MVP

## 目标

让同一个家庭内的几台手机共享 BabyLog 记录，同时保持当前产品边界：

- 不做第三方账号登录。
- 不同步模型 API Key、语音 Key、免责声明确认状态、提醒本地状态。
- AI / OCR / STT 只生成候选，用户手动保存后才进入同步队列。
- 首轮只做家庭密钥式轻量同步，不做复杂 RBAC。

## PocketBase 基础端点

- 健康检查：`GET /api/health`。
- 记录列表：`GET /api/collections/{collection}/records?filter=...`。
- 后续增删改：沿用 PocketBase records API。

## Collection 规划

### families

用于“服务器地址 + 家庭密钥”连接检测。

字段：

- `name` text
- `clientFamilyId` text，App 侧家庭 id，首轮固定 `local-family`
- `familyKeyHash` text，客户端对家庭密钥做 SHA-256 后的 64 位 hex
- `status` select：`active` / `rotated` / `disabled`
- `createdByDevice` text，可空

索引：

- `familyKeyHash`
- `clientFamilyId`

首轮连接检测：

1. App 调 `GET /api/health` 确认服务器可达。
2. App 计算 `sha256(trim(familyKey))`。
3. App 调 `GET /api/collections/families/records?filter=familyKeyHash="..."`。
4. 返回 `items.length > 0` 才认为家庭可连接。

注意：URL 和请求 header 都只出现 `sha256(trim(familyKey))` 的 64 位 hex，不传明文家庭密钥；`X-BabyLog-Family-Key` header 仅作为后续 PocketBase hook / middleware 的 hash 凭据。

### family_profiles

- `clientId` text
- `familyId` text
- `payload` json
- `updatedAtClient` text
- `schemaVersion` number
- `deletedAt` text，可空

### child_profiles

- `clientId` text
- `familyId` text
- `childId` text
- `payload` json
- `updatedAtClient` text
- `schemaVersion` number
- `deletedAt` text，可空

### family_members

- `clientId` text
- `familyId` text
- `memberId` text
- `role` select：`manager` / `family` / `caregiver`
- `payload` json
- `updatedAtClient` text
- `schemaVersion` number
- `deletedAt` text，可空

### events

- `clientId` text
- `familyId` text
- `childId` text
- `eventType` text
- `occurredAt` text
- `payload` json
- `attachmentIds` json
- `updatedAtClient` text
- `schemaVersion` number
- `deletedAt` text，可空

### attachments

首轮先同步 metadata，文件上传放下一阶段。

- `clientId` text
- `familyId` text
- `childId` text
- `kind` text
- `originalName` text
- `mimeType` text
- `byteSize` number
- `contentHash` text，可空
- `remoteFile` file，可空，S5 再启用
- `payload` json
- `updatedAtClient` text
- `schemaVersion` number
- `deletedAt` text，可空

### sync_changes

可选调试集合，首轮 App 仍以本机 pending 队列为准。

- `changeId` text
- `familyId` text
- `childId` text
- `entityType` text
- `entityId` text
- `operation` text
- `status` text
- `lastError` text，可空
- `updatedAtClient` text
- `schemaVersion` number

## 权限策略

首轮不做账号登录，安全边界是“服务器地址 + 足够长的家庭密钥 + 私有 VPS”。PocketBase collection rules 保持最小开放，配合家庭密钥 hash 过滤。

建议：

- `families` 仅允许带 `familyKeyHash` filter 的 list，用于连接检测。
- 其它集合在真实推拉接入前先关闭 public create/update/delete。
- 正式推拉前再加 PocketBase hook：校验 `X-BabyLog-Family-Key` header 中的 64 位 hash 是否匹配目标 family。

## App 侧现状

已完成：

- 本机加密保存家庭密钥。
- Android backup / device-transfer 排除 `babylog_sync_secrets.xml`。
- 家庭同步协议白名单。
- PocketBase health + family lookup 连接检测。

未完成：

- 推送 pending sync changes。
- 拉取合并远端记录。
- 附件文件上传 / 下载。
- 家庭密钥轮换。
