# 家庭同步 E2EE 方案 S3 — 给 Codex

> 衔接：S1 协议骨架（78eca66）→ S1.5 密钥本机加密（1e5cb2d）→ S2 PocketBase 连接检测（48d75e5）→ Pre-S3 收敛（e026c9b）→ **本文 = S3 主线设计**
>
> 范围：把"服务端只见密文"的端到端加密同步从设计落到代码。仅 push，不做 pull/附件/轮换。

## 0. 红线（先看，不要犯）

- 服务端**只见密文**，不见 `entityType` / `eventType` / `occurredAt` / `attachmentIds` / `payload` 内任何字段。
- 服务端**只见 hash**，不见明文家庭密钥（已在 e026c9b 落实，S3 沿用）。
- 不引入第三方账号 / 不联 GMS / 不同步 `smartConfig` / `reminder` / `preVisitQuestion` / `disclaimerConsent`。
- 不动 `BabyLogDomain` 事件模型、不动 FGR、不动 STT、不动免责声明、不动 launcher 视觉、不动 splash。
- `applicationId` / 包名 / GitHub repo / 所有 import / smoke / 任何技术 ID 一切不动。
- 不预设医学建议、不写"异常/严重/危险"等词、不做自动判读。
- E2EE 上线后**所有现存本机记录都不会自动上行**，必须用户在同步页主动按"立即推送"，第一次推送前 UI 显示"将把 N 条本机记录加密上传"二次确认。

## 1. 密钥派生（HKDF-SHA256）

```
familyKey (用户输入字符串)
  → trim()
  → Normalizer.normalize(NFC)        ← 跨设备同一字符串才 byte 一致
  → UTF-8 bytes  = IKM

HKDF-Extract(salt = "babylog-family-v1".getBytes(UTF-8), IKM)
  = PRK (32 字节)

HKDF-Expand(PRK, info, L=32):
  info = "lookup/v1"    → 32 字节 lookupKey   → 用于服务端 familyKeyHash（替代当前裸 SHA-256）
  info = "data/v1"      → 32 字节 dataKey     → AES-256-GCM payload 加密
  info = "index/v1"     → 32 字节 indexKey    → 预留（S4 拉取索引用，S3 不用）
```

### 1.1 实现要求

- 新文件 `BabyLogFamilyKeyDeriver.java`，纯 JVM、无 Android 依赖（smoke 可在 JVM 直接跑）。
- 不引第三方密码库，HKDF 自己用 `javax.crypto.Mac("HmacSHA256")` 实现（约 30 行）。
- 提供静态方法：
  - `byte[] deriveLookupKey(String familyKey)`
  - `byte[] deriveDataKey(String familyKey)`
  - `byte[] deriveIndexKey(String familyKey)`
  - `String lookupHashHex(String familyKey)` ← lookupKey 的 hex，替代 `BabyLogSyncProtocol.hashFamilyKeyForLookup`

### 1.2 与现有 `hashFamilyKeyForLookup` 的兼容

- S3 在 `BabyLogSyncProtocol` 保留 `hashFamilyKeyForLookup`，**内部改为调 `BabyLogFamilyKeyDeriver.lookupHashHex`**。外部签名不变。
- 这意味着现存连接检测/URL filter/header 自动切到新算法。
- PocketBase 服务端 `families.familyKeyHash` 字段语义不变，**值变了** —— 用户重新填一遍家庭密钥即可（或写迁移说明：旧 hash 一次性失效，重新填家庭密钥后会写新 hash）。

### 1.3 Smoke：`BabyLogFamilyKeyDeriverSmokeTest`

- 同 `familyKey` 多次派生必出**同**三套 key（确定性）。
- `"  family-secret  "` 和 `"family-secret"` 必出同 key（trim）。
- 不同 `info` 必出**不同** key。
- 不同 `familyKey` 必出不同 key。
- 三个 key 长度都是 32 字节。
- `lookupHashHex` 长度 64、全 `[0-9a-f]`。
- HKDF 已知答案测试：用 RFC 5869 Test Case 1 验证 HKDF 实现正确（IKM/salt/info 用 RFC 给的，对比期望 OKM）。

## 2. Payload 加密（AES-256-GCM）

### 2.1 加密格式

每条加密记录上行的字段：

```json
{
  "clientId":         "<UUID v4>",          // 服务端主键，无语义
  "familyKeyHash":    "<64 hex>",           // 路由 / 鉴权
  "schemaVersion":    1,                    // 明文，便于多版本兼容
  "cipherVersion":    1,                    // 明文，本字段 = 加密格式版本
  "nonce":            "<base64 12 字节>",   // SecureRandom，每次重新生成
  "ciphertext":       "<base64>",           // GCM seal 的输出（含 tag）
  "updatedAtClient":  "<ISO8601>",          // 明文，pull 游标
  "deletedFlag":      0 | 1                 // 明文，软删除标记（不传时间戳）
}
```

明文 plaintext（被加密的内容）：

```json
{
  "entityType":     "event" | "familyProfile" | "childProfile" | "familyMember" | "attachment",
  "entityId":       "<原本机 id，如 evt_xxx>",
  "payload":        { ...原 entity 的完整 JSON... },
  "occurredAt":     "<事件原 occurredAt，仅适用于 event>",
  "attachmentIds":  ["att_xxx", ...]
}
```

### 2.2 AAD（Additional Authenticated Data）

GCM 的 AAD 用 `clientId + "|" + cipherVersion + "|" + familyKeyHash` 的 UTF-8 字节。
目的：服务端就算把 `nonce` 和 `ciphertext` 配对错了行（攻击者重组），AAD 校验也会失败。

### 2.3 实现要求

- 新文件 `BabyLogPayloadCipher.java`：
  - `SealResult seal(byte[] dataKey, byte[] aad, byte[] plaintext)` → 返回 `{nonce, ciphertext}`，nonce 用 `SecureRandom` 12 字节，**绝对不能用 counter 或时间**。
  - `byte[] open(byte[] dataKey, byte[] aad, byte[] nonce, byte[] ciphertext)` → 失败抛 `GeneralSecurityException`。
- Cipher: `AES/GCM/NoPadding`, tag 128 bit。
- 不缓存 Cipher 实例（线程安全坑）。

### 2.4 Smoke：`BabyLogPayloadCipherSmokeTest`

- seal → open → plaintext 一致。
- seal 两次同 plaintext，nonce 必**不同**（断言不等）。
- open 时改一字节 ciphertext → 必抛异常。
- open 时改一字节 nonce → 必抛异常。
- open 时改 AAD → 必抛异常。
- 用错 dataKey → 必抛异常。
- 空 plaintext 也能 seal/open（边界）。

## 3. PocketBase 新 Schema（替换原 5 collection）

### 3.1 改动总则

原 `family_profiles` / `child_profiles` / `family_members` / `events` / `attachments` 5 个明文 collection **全部废弃**，统一为：

#### `encrypted_records`

| 字段 | 类型 | 索引 | 备注 |
|---|---|---|---|
| `clientId` | text | unique | 客户端生成 UUID v4，服务端主键之一 |
| `familyKeyHash` | text | yes | 等于 `BabyLogFamilyKeyDeriver.lookupHashHex(familyKey)` |
| `schemaVersion` | number | no | 当前 = `BabyLogDomain.SCHEMA_VERSION` |
| `cipherVersion` | number | no | 当前 = 1 |
| `nonce` | text | no | base64(12 字节) |
| `ciphertext` | text | no | base64 |
| `updatedAtClient` | text | yes | ISO8601，pull 游标用 |
| `deletedFlag` | number | yes | 0 / 1 |

#### `families`（保留）

只用于连接检测：

| 字段 | 类型 | 索引 | 备注 |
|---|---|---|---|
| `name` | text | no | 可空，仅显示 |
| `clientFamilyId` | text | yes | 固定 `local-family` |
| `familyKeyHash` | text | yes | 同 `encrypted_records.familyKeyHash` |
| `status` | select | no | `active` / `rotated` / `disabled` |
| `createdByDevice` | text | no | 可空 |

> 之所以保留 `families`，是为了"连接检测"能在没有任何 `encrypted_records` 时也能返回"家庭存在"。如果改成查 `encrypted_records` 是否有该 `familyKeyHash`，新家庭首次连接前会一直显示"未找到"。`families` collection 的存在让管理员可以手动 PocketBase 后台预建一行。

### 3.2 字段不再有的（要明确删除）

- `entityType` / `eventType` / `occurredAt` 都进 ciphertext，**collection 外面看不到**。
- `payload` 字段不再明文存在（已在 ciphertext 内）。
- `attachmentIds` 进 ciphertext。
- `childId` / `familyId` 不上行（本机常量 `local-family` / `local-child`，无跨家庭路由需求，已被 `familyKeyHash` 覆盖）。

### 3.3 PocketBase Collection Rules（推荐）

- `families`：list rule = `familyKeyHash = @request.query.filter ...`（保留为最小开放，仅允许带 hash filter 的 list）。
- `encrypted_records`：
  - list / view：`familyKeyHash = @request.headers.x_babylog_family_key`（PocketBase hook 校验 header 与目标行一致）。
  - create：同上 + payload `familyKeyHash` 必须等于 header。
  - update / delete：S3 阶段先关闭，S4 拉取/合并阶段再开。
- 文档化在新 `docs/PocketBase家庭同步MVP.md`，并标"S3 push only"。

## 4. 客户端 push 实现

### 4.1 `BabyLogRemoteSyncClient` 扩展

新增方法：

```java
public PushResult pushPendingChanges(
    String backendBaseUrl,
    String familyKey,
    List<EncryptedRecord> encrypted
) throws IOException
```

`EncryptedRecord` 就是 §2.1 字段的 POJO。

- 对每条记录 `POST /api/collections/encrypted_records/records`。
- 失败按记录粒度计入 `PushResult.failed`，不整体回滚（每条 syncChange 独立 retry）。
- 已存在（409 / 唯一约束冲突）按 upsert 处理：再发 `PATCH /api/collections/encrypted_records/records/<id>`（id 用 PocketBase 返回的 id，或 list by `clientId` 拿到 id）。
- 全部走 `https://`（沿用 normalizeBackendBaseUrl 不强制，但 S3 之后准备加 toggle，本笔不做）。

### 4.2 `BabyLogSyncPushOrchestrator`（新建，纯 Java/Android）

```java
public final class BabyLogSyncPushOrchestrator {
    public PushSummary pushOnce(
        BabyLogService service,
        BabyLogRepository repository,
        BabyLogSyncSecretStore secretStore,
        BabyLogDomain.BackendConfig backendConfig,
        BabyLogRemoteSyncClient remoteClient
    );
}
```

流程：

1. `repository.listSyncChanges()`，过滤 `status != "synced"` 的项。
2. 按 `entityType` 取对应 entity（event / attachment / childProfile / familyProfile / familyMember）。
3. 构造 plaintext JSON（§2.1）→ `BabyLogPayloadCipher.seal(dataKey, aad, plaintextBytes)`。
4. 拼 `EncryptedRecord`，加入批次。
5. `remoteClient.pushPendingChanges` 上传。
6. 按返回逐条更新 `syncChange.status`：成功 → `"synced"`；失败 → `"failed"` + lastError。
7. 返回 `PushSummary{ pushed, failed, total }`。

**约束**：

- 这个类**不依赖 Compose / Activity**，方便 smoke 用 mock HttpServer 端到端跑。
- 不在主线程，调用方负责放 `runInBackground`。
- 单次最多推 200 条（防 IOException 时积压成几千行）。

### 4.3 `syncChange.status` 状态机

现状是 `"pending" / "failed"`。新增 `"synced"`。

- 历史已有数据迁移：把所有 `"failed"` 视同 `"pending"`，重启后第一次推送会自动重试。无需脚本。
- `listSyncChanges()` 排序按 `updatedAtClient` 升序，保证旧的先推。

## 5. UI 改动（最小）

### 5.1 同步设置页 `SyncSettingsScreen` 新增

在"检测连接"按钮下方加：

```
[ 立即推送本机记录 ]      ← OutlinedButton，相同视觉风格

   待同步：12 条
   上次推送：3 分钟前，成功 11、失败 1
   失败原因：HTTP 409（已存在）
```

- 第一次按按钮：弹 `ConfirmDialog`，"将把 N 条本机记录加密上传到 <服务器地址>。家庭密钥仅本机保存，服务器仅看到密文。"
- 推送过程：按钮文案 "推送中..." + 禁用。
- 完成：刷新 summary 文本。

### 5.2 文案红线

- 不出现"医疗记录" / "病史"等敏感词，统一叫"本机记录"。
- 不写"安全" / "加密保证"等绝对化承诺，写"家庭密钥仅本机保存"客观描述。
- 不写"备份到云端"暗示用户云端兜底，写"同步到家庭后端"。

## 6. 不做（明确边界）

- ❌ **附件文件上传**：S3 只同步 attachment metadata（已含在 ciphertext 内），文件本体留 S4。`encrypted_records` 里 attachment 类的 plaintext.payload 包含 `byteSize / mimeType / contentHash`，**不包含 base64 文件**。
- ❌ **服务端 → 客户端拉取**：S4。
- ❌ **冲突合并**：S4 拉取阶段才有冲突，S3 push only 无冲突。
- ❌ **密钥轮换**：S5+，会涉及全量数据重加密。
- ❌ **多家庭/家庭成员粒度权限**：永远不做，单家庭单密钥假设。
- ❌ **离线队列持久化优化**：现有 `syncChanges` SharedPreferences 已够。
- ❌ **PocketBase realtime / WebSocket**：S6+。

## 7. 提交拆分（每个独立 commit）

| # | commit message | 范围 | smoke |
|---|---|---|---|
| S3-a | `feat: 增加家庭同步密钥派生` | `BabyLogFamilyKeyDeriver` + Protocol 内部切换 lookup | `BabyLogFamilyKeyDeriverSmokeTest` + RFC 5869 KAT |
| S3-b | `feat: 增加同步载荷加密` | `BabyLogPayloadCipher` | `BabyLogPayloadCipherSmokeTest` |
| S3-c | `docs: 重写家庭同步 schema 为单密文集合` | `docs/PocketBase家庭同步MVP.md` 替换为新 schema | — |
| S3-d | `feat: 增加同步推送编排` | `BabyLogSyncPushOrchestrator` + `BabyLogRemoteSyncClient.pushPendingChanges` | `BabyLogSyncPushOrchestratorSmokeTest`（用 loopback HttpServer 模拟 PocketBase，端到端验证服务端拿到的只是密文） |
| S3-e | `feat: 同步设置页增加立即推送` | `SyncSettingsScreen` + `ComposeMainActivity` 接线 + 二次确认 | 复用 a/b/d smoke |

每个 commit 自身可 `:app:assembleDebug` + `:app:lintDebug` + 全量 JVM smoke 过。

## 8. 端到端 smoke（S3-d 核心）

`BabyLogSyncPushOrchestratorSmokeTest`：

1. 起 loopback `HttpServer` 模拟 PocketBase（参考 `BabyLogRemoteSyncClientSmokeTest.assertHeaderUsesFamilyKeyHashOnly` 套路）。
2. server handler 把每个 POST 收到的 body 存进内存数组。
3. 客户端推一条事件 plaintext = `{"entityType":"event", "entityId":"evt_xxx", "payload":{"eventType":"ultrasound","note":"sensitive content X"}}`。
4. 断言 server 拿到的 body **不包含** 字符串 `"ultrasound"`、`"sensitive content X"`、`"evt_xxx"`、`"event"`、`"familyProfile"`（防止 plaintext key 泄漏）。
5. 断言 server 拿到的 body 包含 `"ciphertext"`, `"nonce"`, `"familyKeyHash"`。
6. 用同 familyKey 派生 dataKey + AAD 解出 ciphertext，断言 plaintext 还原。

这条 smoke 跑通 = "服务端只见密文"端到端成立。**S3 验收的核心证据。**

## 9. 验收清单

- [ ] 5 笔 commit 各自独立、消息符合 Conventional Commit。
- [ ] `:app:assembleDebug` + `:app:lintDebug` 在每笔 commit 后都过。
- [ ] 全量 JVM smoke 在每笔 commit 后都过。
- [ ] `BabyLogSyncPushOrchestratorSmokeTest` 端到端 smoke 验证服务端 body 不含任何 plaintext 关键词。
- [ ] HKDF 实现通过 RFC 5869 Test Case 1 KAT。
- [ ] UI 首次推送有"将上传 N 条本机记录"二次确认。
- [ ] 同步设置页显示待同步 / 已推 / 失败计数。
- [ ] `docs/PocketBase家庭同步MVP.md` 已替换为新单密文 schema，旧 5 collection schema 全部移除。
- [ ] README 同步段落更新："已完成 push only E2EE，拉取/附件待 S4"。
- [ ] 不动任何禁区文件（DISCLAIMER / FGR / STT / launcher icon / splash）。

---

**S3 推完之后下一笔（S4 预告，不在本文范围）**：拉取 + 增量合并 + LWW 冲突解决 + 软删除同步。
