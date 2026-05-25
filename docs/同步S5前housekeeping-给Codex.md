# 同步 S5 前 housekeeping — 给 Codex

> 衔接：S4 已落地（8a7b3d1，已合 main）。本文 = S5 附件同步主线启动前的累积小问题清算。
>
> 5 项打包，可拆成 2 笔 commit（推荐）或 1 笔大 commit（如果都在一个上下文）。每笔后 `:app:assembleDebug` + `:app:lintDebug` + 全量 JVM smoke 必过。
>
> 总工期：半天。

## 0. 红线

- 不动加密协议（HKDF / AES-GCM / AAD 一字不改）
- 不动业务逻辑（不动 record* / update* / FGR / STT / Disclaimer）
- 不动 UI 视觉
- 所有改动**外部行为等价**或**修 bug**，不引入新功能
- 留在 `codex/stage-mainline-refactor` 分支

## 1. 五项内容

### H1 · Base64 切 JDK 标准（高优先）

**问题**：`BabyLogSyncBase64.java` 71 行手搓 RFC 4648，因为 JVM smoke 跑不动 `android.util.Base64`。当前实现无已知答案测试（KAT），与 PocketBase server-side 标准 base64 兼容性靠 round-trip 自洽不可靠。

**做法**：
1. 在 `android-native/app/build.gradle` 启用 core library desugaring：
   ```gradle
   android {
       compileOptions {
           coreLibraryDesugaringEnabled true
           sourceCompatibility JavaVersion.VERSION_1_8
           targetCompatibility JavaVersion.VERSION_1_8
       }
   }
   dependencies {
       coreLibraryDesugaring 'com.android.tools:desugar_jdk_libs:2.0.4'  // 或最新
   }
   ```
2. 全局替换 `BabyLogSyncBase64.encode(x)` → `java.util.Base64.getEncoder().encodeToString(x)`
3. 全局替换 `BabyLogSyncBase64.decode(x)` → `java.util.Base64.getDecoder().decode(x)`
4. 删除 `BabyLogSyncBase64.java`
5. 调整相关 smoke import

**验收**：现有 `BabyLogSyncPushOrchestratorSmokeTest` + `BabyLogSyncPullOrchestratorSmokeTest` 全过；assembleDebug + lintDebug 绿（desugaring 启用后 APK 大小可能 +50KB，可接受）。

---

### H2 · Pull cursor 解密失败不推进（高优先）

**问题**：`BabyLogSyncPullOrchestrator.applyFetchedRecords` 第 84-86 行在解密前就更新 `maxCursor`，意味着如果家庭密钥被误改一字 → 整批解密失败 → cursor 仍推到最新 → 改回密钥后这批数据永远不会重拉。

**做法**：把 cursor 推进的判定改为"只对成功解密的 row 计入 maxCursor"：

```java
for (BabyLogRemoteSyncClient.EncryptedRecord record : fetched) {
    if (record == null) continue;
    RemoteVersion version;
    try {
        version = decryptRecord(familyKey, record);
    } catch (Exception error) {
        skipped += 1;
        continue;  // ← 不更新 maxCursor
    }
    if (record.updatedAtClient.compareTo(maxCursor) > 0) {
        maxCursor = record.updatedAtClient;  // ← 移到这里
    }
    // ... dedupe 逻辑
}
```

**Smoke 加固**：在 `BabyLogSyncPullOrchestratorSmokeTest` 加一条 case：

- 5 行 record，故意把 `familyKey` 传错给 orchestrator
- 期望：`summary.totalFetched == 5`、`summary.applied == 0`、`summary.skipped == 5`
- **关键断言**：`repository.loadSyncLastPulledAt() == ""`（cursor 没被推进）

---

### H3 · 文档补 `clientId` 语义与 row 膨胀说明（高优先，S5 前置）

**问题**：S3/S4 实现里 `clientId = SyncChange.id`，意味着同一 entity 多次修改 → N 行 `encrypted_records`，server 不做自动 LWW upsert。S5 附件实现者如果不懂这一层，会以为"一个 attachment 对应 server 一行"，写错下载逻辑。

**做法**：在 `docs/PocketBase家庭同步MVP.md` 现有"S4 拉取与合并"章节后插入一段：

```markdown
## clientId 语义与 row 膨胀

`clientId` = 本机 `SyncChange.id`（UUID v4，无业务语义），不是 entity 自己的 id。
含义：

- 同一 entity 多次改 = N 个 SyncChange = N 行 `encrypted_records`（解密后 entityId 相同）
- Server **不做**自动 LWW upsert，row 数 = 同步活动量，不是 entity 数量
- 拉取时客户端必须按解密后 `(entityType, entityId)` 分组 + `updatedAtClient` 取最新（已实现于 `BabyLogSyncPullOrchestrator`）
- 长期使用 server row 可能是 entity 数的 5-10 倍

**元数据泄漏面**：攻击者拿 server DB 能看到"发生过 N 次同步事件"，但看不到 entity 类型 / 时间分布（这些都在密文里）。可接受。

**S5+ 含义**：附件文件如果走 PocketBase `file` 字段挂在同一 `encrypted_records` 行，每次 attachment metadata 改一次都会重传文件。S5 实现必须按 `(entityType=attachment, entityId)` 去重，**只上传最新版本对应的文件**，避免文件级膨胀。
```

---

### H4 · 删除 `BabyLogService.runSyncNow` 死代码（低优先，顺手）

**问题**：CMA 已切到 `requestPushSyncNow` 走 `BabyLogSyncPushOrchestrator`，旧 `runSyncNow` 方法残留。

**做法**：
1. `grep -rn "runSyncNow" android-native/app/src` 确认 0 引用
2. 删除 `BabyLogService.runSyncNow` 及相关 `SyncResult` 类（如果是它专用）
3. 删除对应 smoke 测试（如果有）

**验收**：smoke + assembleDebug 绿。

---

### H5 · detekt 行数封顶 + baseline（防 god-file 反弹）

**问题**：CMA 当前 4471 行（P5 重构后 2921 → 反弹 53%），无机制约束未来不再涨。S5 又会加 push/pull 文件路径 + UI 进度提示，CMA 会继续涨。

**做法**：
1. 引入 detekt Gradle plugin（在 `android-native/app/build.gradle`）：
   ```gradle
   plugins {
       id 'io.gitlab.arturbosch.detekt' version '1.23.x'
   }
   detekt {
       toolVersion = '1.23.x'
       config.setFrom("$projectDir/detekt.yml")
       baseline = file("$projectDir/detekt-baseline.xml")
       buildUponDefaultConfig = true
   }
   ```
2. 创建 `android-native/app/detekt.yml`，**只开两个规则**：
   - `style.MaxLineLength`：禁用（避免无关 noise）
   - 自定义 `LongFileRule`：
     - `ComposeMainActivity.kt` ≤ 4500 行
     - `BabyLogService.java` ≤ 2200 行
     - `BabyLogDomain.java` ≤ 1000 行
     - `BabyLogRepository.java` ≤ 700 行（已涨到 ~600，留缓冲）
   - 实现方式：用 detekt 的 `LongMethod` / `LargeClass` 规则改 threshold，或自己写一条简易 custom rule
3. 首次跑 `./gradlew :app:detektBaseline` 生成 `detekt-baseline.xml` 锁住现有违规
4. 串到 GitHub Actions `lintDebug` 之后：
   ```yaml
   - name: Detekt
     run: cd android-native && ./gradlew :app:detekt --console=plain
   ```

**验收**：CI 首次跑 detekt 绿（baseline 已锁）；故意往 CMA 加 100 行测试 → CI 红；删掉测试代码 → CI 复绿。

---

## 2. 提交拆分建议

**方案 A（推荐，2 笔）**：

- **commit 1** `chore: 同步 S5 前清理 (Base64/cursor/doc/死代码)` —— H1 + H2 + H3 + H4
- **commit 2** `chore: detekt 行数封顶 + baseline` —— H5

**方案 B（1 笔）**：5 项打包成 `chore: 同步 S5 前 housekeeping`。

不影响功能，按你方便选。

## 3. 验收清单

- [ ] H1 Base64 切 JDK 标准，desugaring 启用，自实现删除
- [ ] H2 pull cursor 解密失败不推进，smoke 验证
- [ ] H3 文档补 clientId 语义段落
- [ ] H4 `runSyncNow` 死代码删除
- [ ] H5 detekt 集成，baseline 锁现有违规，CI 串接
- [ ] 每笔 commit 后 `:app:assembleDebug` + `:app:lintDebug` + 全量 JVM smoke 绿
- [ ] 不动加密协议 / 业务逻辑 / UI 视觉
- [ ] 留在 `codex/stage-mainline-refactor` 分支
