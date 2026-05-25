# 家庭同步 S4 方案 — 给 Codex

> 衔接：S3 push-only E2EE 已落地并真机验证（4e6b688）。本文 = S4 主线设计：**自动推送 + 自动拉取 + LWW 冲突解决 + 防回路**。
>
> 范围：把"好几个人用"的原始诉求真正兑现。新增自动同步触发链 + 拉取编排 + 冲突 UI。

## 0. 红线（先看，不要犯）

- **加密边界不变**：S3 已定的 `encrypted_records` 单密文集合 / HKDF 派生 / AES-GCM seal / AAD 不动；server 仍只见密文。
- **不引入第三方账号 / 不联 GMS / 不同步 `smartConfig` / `reminder` / `preVisitQuestion` / `disclaimerConsent`**。
- 不动 `BabyLogDomain` 事件模型、不动 FGR、不动 STT、不动免责声明、不动 launcher 视觉、不动 splash。
- `applicationId` / 包名 / GitHub repo / 所有 import / smoke / 任何技术 ID 一切不动。
- 不预设医学建议、不写"异常/严重/危险"等词、不做自动判读。
- **远端写入本机不得回路推回 server**：拉取下来的数据写本机时**绝对不能 emit SyncChange**，否则死循环。这是 S4 第一原则。
- **冲突不询问用户**：LWW 静默覆盖 + 顶部信息条提示，**不弹任何对话框打扰**。
- **轮询不打扰电池**：后台 15 分钟（Android Doze 最小允许），前台 2 分钟，未配置同步时**完全不轮询**。

## 1. 设计目标

| # | 目标 | 触发点 |
|---|---|---|
| 1 | 本机写完一条记录，自动加密推送到 server | 任何 `saveEventWith*` / `saveChildProfileWithSync` 成功后 |
| 2 | 其他设备的更改自动出现在本机 | App 进入前台立即拉一次 + 前台每 2 分钟 + 后台每 15 分钟 + 下拉刷新 |
| 3 | 同一条记录被多人改 → LWW 取 `updatedAtClient` 最新 | 拉取时按 entityId 去重 + 比 updatedAt |
| 4 | 家人更新静默生效，但用户能感知到 | 时间线顶部信息条"已同步 N 条家人更新"，可一键 dismiss |
| 5 | 新设备入家庭一次拉全量历史 | `lastSyncedAt = ""` → 全量分页拉取 |

## 2. 数据流总览

```
本机写入                                              远端
─────────                                            ──────
saveEventWith*                                       PocketBase
   │                                                 encrypted_records
   ├─ Repository commit (events + syncChanges)
   │
   └─ enqueue SyncPushWorker (OneTimeWork)
         │
         └─ pushOnce ─────────加密上传───────────────►
                                                     │
                                                     │
                              ◄──────加密拉取─────── pullOnce
                                                     │
   ┌─ pullOnce decrypt ────────────────────────────┘
   │
   ├─ 按 entityId 去重，比 updatedAt
   │
   ├─ remote 新：putFromRemote (不 emit SyncChange ← 关键)
   ├─ remote 旧/等价：忽略
   └─ remote deletedFlag=1：本机 soft delete (不 emit SyncChange)

   ↓
   bump remoteUpdateBannerCount
   ↓
   UI 顶部信息条 "已同步 N 条家人更新"
```

## 3. 自动推送实现

### 3.1 触发点

任何"保存成功"的方法尾巴上加一行 enqueue：

```kotlin
// ComposeMainActivity 内统一的 helper（或下沉到 BabyLogService）
private fun enqueueAutoPush() {
    if (uiState.syncConfig.enabled && syncFamilyKeyConfigured) {
        val request = OneTimeWorkRequestBuilder<BabyLogSyncPushWorker>()
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .build()
        WorkManager.getInstance(this).enqueueUniqueWork(
            "babylog_sync_push",
            ExistingWorkPolicy.REPLACE,
            request
        )
    }
}
```

**调用位置**（每个 record* / update* / saveProfile 等成功保存后）：
- `recordBabyCareEvent` / `updateBabyCareEvent`
- `recordPregnancyEvent` / `updatePregnancyEvent`
- `recordUltrasound` / `updateUltrasound`
- `recordMaternalMetric` / `updateMaternalMetric`
- `recordFetalMovementSession` / `recordContractionSession`
- `softDeleteEventById` / `restoreEventById`
- `saveChildProfile` / `saveFamilyProfile` / `saveFamilyMember`

更干净的做法：**在 BabyLogService 的所有 saveEventWith* helper 出口统一调** `onSuccessfulWrite()`，里面执行 enqueue。一处加，全链路覆盖。

### 3.2 `BabyLogSyncPushWorker`

```kotlin
class BabyLogSyncPushWorker(ctx: Context, params: WorkerParameters)
    : Worker(ctx, params) {

    override fun doWork(): Result {
        val repository = BabyLogRepository(applicationContext)
        val service = BabyLogService(applicationContext, repository)
        val secretStore = BabyLogSyncSecretStore(applicationContext)
        val backend = repository.loadSyncSettings()
        if (!backend.enabled || backend.backendBaseUrl.isBlank()) return Result.success()

        val summary = BabyLogSyncPushOrchestrator().pushOnce(
            service, repository, secretStore, backend, BabyLogRemoteSyncClient()
        )
        return if (summary.failed > 0 && summary.lastError == "PUSH_FAILED")
            Result.retry()  // WorkManager 自动指数退避
        else
            Result.success()
    }
}
```

- 单次 push 失败（网络问题）→ `Result.retry()` → WorkManager 指数退避（10s, 20s, 40s ...）
- 加密失败 / 配置缺失 → `Result.success()`（不要无限重试这类错误）
- 用 `enqueueUniqueWork` + `REPLACE` 策略：连续保存 3 条记录只跑 1 次 push（合并）

### 3.3 关闭手动 "立即推送" 按钮？

**保留**。理由：
- 用户能感知"刚保存的会被推上去"，按钮提供心理可控感
- 后台 WorkManager 在 Android 厂商杀进程的设备上可能被 kill，手动按钮兜底
- 实测后用户觉得多余再删

按钮文案不变。点击仍走现有 `pushSyncNow()` 路径。

---

## 4. 自动拉取实现

### 4.1 触发点

| 时机 | 实现 | 频率 |
|---|---|---|
| App 进入前台 | `LifecycleEventObserver.ON_RESUME` | 立即一次 |
| 前台时定时 | `LaunchedEffect(Unit) { while(true) { delay(120_000); pull(silent=true) } }` | 每 2 分钟 |
| 后台 | `PeriodicWorkRequest<BabyLogSyncPullWorker>(15, MINUTES)` + `NetworkType.CONNECTED` | 每 15 分钟（Doze 最小） |
| 下拉刷新 | TimelineScreen / HomeScreen 加 PullToRefresh，触发 pull(silent=false) | 用户手动 |

**注意**：
- 前台轮询协程必须在 Activity `onPause` 时取消，避免后台仍在跑
- `LaunchedEffect(syncFamilyKeyConfigured && uiState.syncConfig.enabled)` —— key 变化时重新启动协程，未配置时不启
- WorkManager 注册在 Application.onCreate（新建 `BabyLogApplication` 或复用现有）
- 后台 PeriodicWork 用 `enqueueUniquePeriodicWork` + `KEEP` 策略，避免重复

### 4.2 `BabyLogSyncPullOrchestrator`

```java
public final class BabyLogSyncPullOrchestrator {
    private static final int PER_PAGE = 200;
    private static final int MAX_PAGES_PER_RUN = 10;  // 一次最多 2000 行

    public PullSummary pullOnce(
        BabyLogRepository repository,
        BabyLogSyncSecretStore secretStore,
        BabyLogDomain.BackendConfig backendConfig,
        BabyLogRemoteSyncClient remoteClient
    ) {
        // 1. 加载游标
        // 2. 循环分页 GET，直到该页 < PER_PAGE 或到 MAX_PAGES_PER_RUN
        // 3. 累积所有 EncryptedRecord
        // 4. 解密每条 → plaintext (entityType, entityId, payload, ...)
        // 5. 按 (entityType, entityId) 分组，每组取 updatedAtClient 最新一行
        // 6. 对每个最终版本：
        //    - 查本机现有 entity
        //    - 远端 updatedAt > 本机 → putFromRemote(applied++)
        //    - 远端 updatedAt <= 本机 → skip(skipped++)
        //    - deletedFlag=1 → 软删本机(applied++)
        // 7. 推进 lastSyncedAt = max(updatedAtClient in batch)
        // 8. 返回 PullSummary
    }
}

PullSummary:
    int totalFetched       // server 拉了多少行（含重复 entityId）
    int applied            // 实际写入本机的 entity 数
    int skipped            // 等价/远端旧的
    String lastError       // 错误码
    String newCursor       // 新游标
```

### 4.3 `lastSyncedAt` 游标

- 存到 `BabyLogRepository` 的 sync prefs（**不进 backup / device-transfer**，新设备应能首次全量拉）
- 字段：`sync_last_pulled_at`（ISO8601 字符串）
- 初值：`""` → 拉全量（`filter=familyKeyHash="..."`，无 updatedAt 限制）
- 每次 pullOnce 成功后：`max(updatedAtClient in pulled batch)` → 持久化

### 4.4 `BabyLogRemoteSyncClient` 扩展

```java
public PullResult pullEncryptedRecords(
    String backendBaseUrl,
    String familyKey,
    String sinceCursor,   // "" = 拉全量
    int page,
    int perPage
) throws IOException;
```

实现：
```
GET /api/collections/encrypted_records/records
   ?filter=familyKeyHash="<hash>" && updatedAtClient > "<cursor>"
   &sort=updatedAtClient
   &page=<page>
   &perPage=<perPage>
```

`sinceCursor = ""` 时省略 `updatedAtClient` 条件。

返回：
```java
class PullResult {
    List<EncryptedRecord> records;  // 复用 S3 EncryptedRecord 类型
    int page;
    int totalPages;
    int totalItems;
    String lastError;
}
```

---

## 5. 冲突解决（LWW）

### 5.1 比较规则

```java
// 在 PullOrchestrator 内
RemoteVersion remote = decryptedPlaintext;
Optional<LocalEntity> local = findByEntityId(remote.entityId);

if (local.isPresent()) {
    if (remote.updatedAt.compareTo(local.get().updatedAt) > 0) {
        // 远端新 → 覆盖
        writeFromRemote(remote);
        applied++;
        bannerCount++;
    } else {
        // 远端旧/等价 → 忽略
        skipped++;
    }
} else {
    // 本机没有 → 直接写入（家人新增）
    writeFromRemote(remote);
    applied++;
    bannerCount++;
}
```

### 5.2 编辑 vs 删除冲突

`remote.deletedFlag=1` 视为"远端这条记录处于删除状态"：

- 远端 updatedAt > 本机 updatedAt → 软删本机（应用远端的"删除决定"）
- 远端 updatedAt <= 本机 → 忽略（本机的编辑赢）

**两种结果都不弹窗，都计入 `bannerCount`，让用户在顶部信息条看到。**

### 5.3 不引入 CRDT / 三向合并

家庭场景两人同时编辑同一条 B 超的概率接近 0。LWW 的"输的一方丢字段"代价远低于 CRDT 的实现复杂度。**S4 不做合并算法**。

如果实测真发生频繁冲突（用户反馈），后续可加"冲突历史记录"功能（把输的版本存进 trash 类似的位置）。S4 不预防性做。

---

## 6. 防回路（CRITICAL）

### 6.1 问题

拉取下来的数据写本机时如果走 `saveEventWith*` → 会再 emit `SyncChange` → 下次 push 又被推回 server → 服务端 row 翻倍膨胀 → 拉取又拉回来 → 死循环。

### 6.2 解决：新建一组 `*FromRemote` 写入路径

`BabyLogRepository` 新增：

```java
public boolean putEntityFromRemote(
    String entityType,
    JSONObject entityJson,
    boolean softDelete
) throws JSONException
```

内部按 entityType 分发：
- `event` → 直接覆盖 events 数组
- `attachment` → 直接覆盖 attachments 数组
- `childProfile` → 直接写入 child_profile 字段
- `familyProfile` / `familyMember` → 同理

**关键**：这个方法**不调用 `putSyncChange`**。完全不感知 syncChanges 表。

`BabyLogSyncPullOrchestrator` 拉到的所有数据都走这条路径，**绝不**走 `saveEventWith*`。

### 6.3 Smoke 必加断言

```
push 1 条 → server 收到 1 条
pull 一次 → 本机 entity 被远端覆盖
pull 完后 listSyncChanges() 该 entity 必须 0 条新 SyncChange
push 一次 → server 仍是原 1 条，不增长
```

如果回路防不住，这条 smoke 会失败。

---

## 7. UI 改动

### 7.1 顶部信息条

新增 composable `ChestnutSyncBanner`：

```
┌─────────────────────────────────────────────────────┐
│  🔄 已同步 3 条家人更新                            ✕  │
└─────────────────────────────────────────────────────┘
```

放在：
- `HomeScreen` 顶部（TopBrandBand 下方）
- `TimelineScreen` 顶部

显示条件：`dashboard.remoteUpdateBannerCount > 0`。

点击 ✕ 调 `service.dismissRemoteUpdateBanner()` → 清零计数。

**不点击不展开任何详情**（不阻塞、不打扰）。用户想知道改了什么 → 自己去时间线看。

### 7.2 PullToRefresh

`HomeScreen` + `TimelineScreen` + `RecordDetailScreen` 加下拉刷新：

```kotlin
val pullState = rememberPullToRefreshState()
val coroutineScope = rememberCoroutineScope()
val isRefreshing by remember { mutableStateOf(false) }

PullToRefreshBox(
    isRefreshing = isRefreshing,
    onRefresh = {
        coroutineScope.launch {
            isRefreshing = true
            onPullSyncNow()  // → pullSyncNow(silent=false)
            isRefreshing = false
        }
    }
) {
    // 原内容
}
```

`pullSyncNow(silent=false)` 完成后弹 Toast："已同步 N 条家人更新"或"已是最新"。
`pullSyncNow(silent=true)`（自动触发）不弹任何 Toast。

### 7.3 同步设置页计数补充

`SyncSettingsScreen` 已有 "待同步 / 已推送 / 失败"，再加：
- "上次拉取：2 分钟前"
- "本轮新拉取：5 条"
- 已存的"立即推送"旁边加"立即拉取"按钮

### 7.4 不动的

- 不动 launcher 视觉 / splash / 已锁视觉 token
- 不弹任何冲突对话框
- 不在记录详情页加"被家人覆盖过"标记（信息条已足够）

---

## 8. PocketBase Collection Rules 调整（管理员后台操作）

S3 期间 `encrypted_records` 只开了 create。S4 需要开 list / view：

```
encrypted_records:
  List rule:   @request.headers.x_babylog_family_key != "" 
               && familyKeyHash = @request.headers.x_babylog_family_key
  View rule:   同上
  Create rule: 同上（保持 S3）
  Update rule: 同上 + 限制只能改 deletedFlag / nonce / ciphertext / updatedAtClient
  Delete rule: 关闭（软删走 deletedFlag，永不硬删）
```

文档化在 `docs/PocketBase家庭同步MVP.md`，更新 S4 章节。

---

## 9. 提交拆分（每个独立 commit）

| # | commit message | 范围 | smoke |
|---|---|---|---|
| S4-a | `feat: 增加同步拉取客户端 API` | `BabyLogRemoteSyncClient.pullEncryptedRecords` + `PullResult` 类型 | `BabyLogRemoteSyncClient` smoke 加 pull mock |
| S4-b | `feat: 增加同步拉取编排` | `BabyLogSyncPullOrchestrator` + `BabyLogRepository.putEntityFromRemote` + sync prefs 加 `lastSyncedAt` + `remoteUpdateBannerCount` | `BabyLogSyncPullOrchestratorSmokeTest` 端到端：mock server 返回 5 行→断言 dedupe + LWW + 防回路 + bannerCount |
| S4-c | `feat: 自动推送 Worker` | `BabyLogSyncPushWorker` + `BabyLogService.onSuccessfulWrite()` hook + 所有 `record*` 出口接线 | 不加 worker smoke（WorkManager 难 JVM 测），断言 hook 覆盖度 |
| S4-d | `feat: 自动拉取 Worker + 前台轮询` | `BabyLogSyncPullWorker` + Application.onCreate 注册 PeriodicWork + CMA `LaunchedEffect` 前台 2min 轮询 + ON_RESUME 立即拉一次 | 不加 worker smoke |
| S4-e | `feat: 同步信息条 + 下拉刷新` | `ChestnutSyncBanner` composable + HomeScreen/TimelineScreen 接入 + PullToRefresh + dismiss 入口 + Service `dismissRemoteUpdateBanner` | — |
| S4-f | `docs: 更新 PocketBase MVP 加 S4 拉取章节` | `docs/PocketBase家庭同步MVP.md` 加 S4 段落 + collection rules 调整说明 + README 更新 | — |

每个 commit 自身可 `:app:assembleDebug` + `:app:lintDebug` + 全量 JVM smoke 过。

## 10. 端到端 smoke（S4-b 核心）

`BabyLogSyncPullOrchestratorSmokeTest`：

1. 起 loopback HttpServer mock `/api/collections/encrypted_records/records`。
2. 用 S3 工具加密 5 条 plaintext，伪装成 server 响应：
   - 行 A：entityId=`evt_1`, updatedAt=`2026-05-25T09:00`, payload note="A 版"
   - 行 B：entityId=`evt_1`, updatedAt=`2026-05-25T10:00`, payload note="B 版"（更新覆盖 A）
   - 行 C：entityId=`evt_2`, updatedAt=`2026-05-25T11:00`, deletedFlag=1
   - 行 D：entityId=`evt_3`, updatedAt=`2026-05-25T12:00`, payload 完整（新事件）
   - 行 E：entityId=`evt_4`, updatedAt=`2026-05-25T08:00`（**本机已有 updatedAt=10:00 的 evt_4**，应被忽略）
3. 本机预置 evt_4 (updatedAt=`2026-05-25T10:00`, payload note="本机版")。
4. 调 `pullOnce`。
5. 断言：
   - `summary.totalFetched == 5`
   - `summary.applied == 3` (B 覆盖 A → 1，C 软删 → 1，D 新增 → 1)
   - `summary.skipped == 2` (A 被 B 去重 → 1，E 远端旧 → 1)
   - 本机 evt_1.payload.note == "B 版"
   - 本机 evt_2 已软删
   - 本机 evt_3 存在
   - 本机 evt_4.payload.note == "本机版"（未被覆盖）
   - `dashboard.remoteUpdateBannerCount == 3`
   - `lastSyncedAt == "2026-05-25T12:00"`（最大值）
   - **`listSyncChanges()` 全部 5 条 entityId 均无新 SyncChange**（防回路核心断言）
6. 再调一次 pullOnce（相同 server 响应）：
   - 断言 `summary.totalFetched == 0`（因为 cursor 已过）

## 11. 验收清单

- [ ] 6 笔 commit 各自独立、消息符合 Conventional Commit。
- [ ] `:app:assembleDebug` + `:app:lintDebug` 在每笔 commit 后都过。
- [ ] 全量 JVM smoke 在每笔 commit 后都过。
- [ ] `BabyLogSyncPullOrchestratorSmokeTest` 端到端 smoke 验证去重 + LWW + 防回路 + bannerCount。
- [ ] 本机写入触发自动 push（手动验证：保存一条记录 → 1 分钟内 PocketBase 后台看到新行）。
- [ ] 前台 2 分钟自动拉取（手动验证：A 手机推一条，B 手机前台 2 分钟内出现）。
- [ ] 后台 15 分钟自动拉取（手动验证：B 手机回到前台后能看到 A 手机推送的更新）。
- [ ] 下拉刷新立即触发 pull。
- [ ] 信息条"已同步 N 条家人更新"正确显示和 dismiss。
- [ ] 新设备入家庭一次拉到全量历史（lastSyncedAt 空 → 分页拉完）。
- [ ] 不动任何禁区文件（DISCLAIMER / FGR / STT / launcher icon / splash）。
- [ ] PocketBase 文档已更新 S4 拉取章节 + collection rules。

---

## S4 之后

S5 候选（按需启动）：
- **附件文件上传** (S5)：当前只同步 attachment metadata，文件本体（base64）下一阶段做。需要 PocketBase `file` 字段 + AES-GCM 加密文件后上传。
- **家庭密钥轮换** (S6)：涉及全量数据重加密 + 服务端配合。
- **PocketBase Realtime** (S7)：如果用户反馈"轮询太慢"才考虑，否则不上 WebSocket。
