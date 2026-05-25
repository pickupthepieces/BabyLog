# 版本管理与发版流程

| 项目 | 内容 |
|---|---|
| 作者 | Claude（Opus 4.7） |
| 日期 | 2026-05-25 |
| 范围 | BabyLog 版本号规则 + 发版 SOP + CI 自动化说明 |
| 关联 | `.github/workflows/release.yml` / `CHANGELOG.md` / `BabyLogAppUpdateManager` |

## 1. 版本号规则（Semver lite）

```
v MAJOR . MINOR . PATCH
    |       |       |
    |       |       └─ bug fix / housekeeping / 小调整（CMA 拆分 / detekt baseline 更新等）
    |       └───────── feature feat 上线（BB1 / BB5 / S6 / G 系列等业务块）
    └───────────────── 重大不兼容（孕→育阶段切换、同步 schema 不兼容、签名变更等）
```

**versionCode**（Android 内部 monotonic int）：手动递增 +1。不用算法生成，避免 CI 出错。

**versionName**（用户看到的字符串）：跟 git tag 一致（不含 `v` 前缀）。
- tag `v0.2.0` → `versionName '0.2.0'`
- CI 校验两者一致才放过 release

### 1.1 当前基线

| | 值 |
|---|---|
| versionCode | 1 |
| versionName | 0.1.0 |
| 第一个 release tag | （未发，待 v0.1.0 正式打 tag）|

### 1.2 命名约定示例

| 场景 | 推荐 tag | versionCode |
|---|---|---|
| 当前主分支兜底打 tag | `v0.1.0` | 1（保持）|
| 一笔 housekeeping / bug fix | `v0.1.1` | 2 |
| BB1 育儿工作流首轮上线 | `v0.2.0` | 3 |
| BB5 疫苗本上线 | `v0.3.0` | 5（中间几笔可能是 PATCH）|
| 育儿期主线完整可用 | `v1.0.0` | N |
| pre-release（仅测试） | `v0.2.0-beta.1` | （仍递增）|

pre-release tag 名含 `-` 即被 CI 识别为 prerelease，GitHub 上打"Pre-release"标，App 内"检查更新"仍能拉到（不区分）。如果不希望 pre-release 推到家人手机，可以暂不打 pre-release tag，只本地侧手动 sideload 验证。

## 2. 发版 SOP（你日常操作的 5 步）

> 假设当前在 main 分支，所有改动已合 main 且 CI 绿。

### 第 1 步：决定本版 `versionName` + `versionCode`

按 §1 规则。例如要发 BB1 上线：`v0.2.0` + versionCode `3`。

### 第 2 步：本地改 `android-native/app/build.gradle`

```diff
-        versionCode 1
-        versionName '0.1.0'
+        versionCode 3
+        versionName '0.2.0'
```

### 第 3 步：更新 `CHANGELOG.md`

把 `## [Unreleased]` 改成 `## [0.2.0] - 2026-XX-XX`，新加一个空 `## [Unreleased]` 占位在它上面。例如：

```markdown
## [Unreleased]

### Added
- （待填）

---

## [0.2.0] - 2026-06-10

### Added
- BB1 育儿期日摘要 + 时间轴
- 喂养字段补充（母乳 L/R 时长、奶瓶品牌等）

### Changed
- 睡眠时长自动计算

### Fixed
- 跨午夜睡眠归属修正
```

CI 会自动把 `## [0.2.0]` 那节内容作为 GitHub Release 描述和 App 内更新提示文案。

### 第 4 步：commit + tag + push

```bash
git add android-native/app/build.gradle CHANGELOG.md
git commit -m "release: v0.2.0"
git tag v0.2.0
git push origin main
git push origin v0.2.0
```

或一次性 push：

```bash
git push --follow-tags
```

### 第 5 步：等 CI

CI workflow `release.yml` 在 `v*` tag push 时触发：

1. 校验 tag versionName 与 build.gradle 一致
2. 跑 lintDebug + detekt（不重跑 smoke——trust 主线 `android-native.yml` 已跑过；如要保险，先把 tag 对应的 commit push 到 main 跑一次 CI 再打 tag）
3. assembleRelease（signed）
4. 算 SHA-256
5. 生成 `babylog-update.json` manifest
6. 创建 GitHub Release，附 `BabyLog-X.Y.Z.apk` + `babylog-update.json`

CI 跑完约 5-7 分钟。绿就完事。

### 第 6 步（自动）：家人手机自动收到

家人手机上的 BabyLog 设置页"检查更新"会读 `https://github.com/.../releases/latest/download/babylog-update.json`，发现新版本就提示下载安装。

## 3. CI 校验线（防误发）

`release.yml` 在出 release 之前 MUST 全部过：

1. **Tag 格式正则**：`vMAJOR.MINOR.PATCH` 或 `vMAJOR.MINOR.PATCH-prerelease`，违反直接报错退出
2. **build.gradle versionName 与 tag 一致**：避免你只改了 tag 没改 gradle
3. **lintDebug + detekt 必过**：含 CMA / Service / Repository 行数闸值
4. **签名 secrets 必须存在**：缺 secret 直接报错（不能出未签名 release）
5. **assembleRelease 必须出 APK**：含 ProGuard / R8 / signing 全链路

任何一步不绿，release 不创建，需要你修复后重 push tag（或删 tag 重打）。

**Smoke 不在 release.yml 重跑**——trust 主线 `android-native.yml` 已经跑过。打 tag 前确保对应 commit 在 main 上已合并且 main 上的 CI 跑绿，是发版纪律的一部分。

## 4. 应急：发版后撤回

如果 tag 推上去 release 创建了但发现严重问题：

```bash
# 1. 在 GitHub 网页删 Release（包含 APK + manifest）
# 2. 删 tag
git push --delete origin v0.2.0
git tag -d v0.2.0
# 3. 修问题、重 commit、重 tag、重 push
```

**注意**：删 GitHub Release 后，App 端"检查更新"会回退到上一版 release（GitHub `/releases/latest/download/` 自动指向最新 published release）。如果用户已经下载安装了坏版本，要靠下一个修复版 release 把它覆盖。

## 5. 不在自动化范围

- **本地 build 与 sideload**：你自己 USB 装手机仍然用 `./gradlew assembleDebug` 或 `assembleRelease`，不依赖 release workflow
- **debug 与 release 包覆盖安装**：签名不同会失败，README 已说明
- **修改历史 tag**：tag 一旦 push 不应该改，发现错误就发新 PATCH
- **release notes 翻译 / 润色**：CHANGELOG 怎么写就怎么用，CI 不二次加工

## 6. 常见操作速查

```bash
# 看本地所有 tag
git tag -l

# 看远端所有 tag
git ls-remote --tags origin

# 删本地 tag
git tag -d v0.2.0

# 删远端 tag
git push --delete origin v0.2.0

# 跳过 release CI 重新触发（remove + retag）
git push --delete origin v0.2.0
git tag -d v0.2.0
git tag v0.2.0
git push origin v0.2.0

# 看 release workflow 跑得怎么样
gh run list --workflow=release.yml
gh run view <run-id>
```

## 7. 第一次 release 检查清单（v0.1.0 兜底）

发第一次 release 前一次性确认：

- [ ] GitHub repo 是 public（GitHub Release URL 公开访问），或者你接受家人手机也用 token / 私有访问
- [ ] 4 个 secret 已配：`BABYLOG_RELEASE_KEYSTORE_BASE64` / `BABYLOG_RELEASE_STORE_PASSWORD` / `BABYLOG_RELEASE_KEY_ALIAS` / `BABYLOG_RELEASE_KEY_PASSWORD`
- [ ] CHANGELOG.md 已有 `## [0.1.0]` 章节
- [ ] build.gradle versionCode=1 / versionName='0.1.0' 与下面打的 tag 一致
- [ ] 跑一次：`git tag v0.1.0 && git push origin v0.1.0`
- [ ] 在 GitHub Actions 看 release.yml 跑完
- [ ] 在 GitHub Releases 页确认有 `BabyLog-0.1.0.apk` + `babylog-update.json`
- [ ] App 设置页"检查更新"按钮，看是否能识别（这里需要本机 versionCode < 1 才显示新版本——首次没意义）

第一次 release 主要是验证管道。**实际有意义的检查更新**要等 v0.1.1 / v0.2.0 出来后，家人手机的 App（停在 v0.1.0）能看到。

## 8. 不做（边界明确）

- ❌ **自动 bump versionCode**：手动改 build.gradle 才是发版意图的表达
- ❌ **自动写 CHANGELOG**：从 git commit 生成的 changelog 太碎，人工筛选更有用
- ❌ **每次 push 都 build release**：现有 `android-native.yml` 已经在 secrets 存在时跑 conditional release 用于验证 sign 链路；新 `release.yml` 仅在 tag 推时跑且 publish 到 Releases
- ❌ **Google Play 上架**：BabyLog 是私人项目，不上 Play Store
- ❌ **APK 拆分 / abi-splits**：当前单 APK 满足需求，体积可接受
