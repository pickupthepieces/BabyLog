@file:Suppress(
    "FunctionNaming",
    "InvalidPackageDeclaration",
    "TooGenericExceptionCaught"
)

package app.babylog.nativeapp

import androidx.compose.runtime.Composable
import java.io.File

@Composable
internal fun AppUpdateConfirmDialog(
    update: BabyLogAppUpdateManager.UpdateInfo?,
    onDismiss: () -> Unit,
    onConfirm: (BabyLogAppUpdateManager.UpdateInfo) -> Unit
) {
    update ?: return
    ConfirmDialog(
        title = "发现新版本 ${update.versionName}",
        message = "当前版本 ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})，可更新到 ${update.versionName} (${update.versionCode})。\n\n${update.notes.ifBlank { "本次更新未填写说明。" }}\n\n下载后会打开系统安装器，需要你手动确认安装。",
        confirmText = "下载更新",
        destructive = false,
        onDismiss = onDismiss,
        onConfirm = { onConfirm(update) }
    )
}

internal fun ComposeMainActivity.checkAppUpdate() {
    if (appUpdateRunning) return
    appUpdateRunning = true
    appUpdateStatus = "正在检查更新..."
    runInBackground {
        try {
            val update = BabyLogAppUpdateManager.fetchLatest(
                BabyLogAppUpdateManager.DEFAULT_MANIFEST_URL,
                BuildConfig.VERSION_CODE
            )
            runOnUiThread {
                appUpdateRunning = false
                if (update == null) {
                    appUpdateStatus = "当前已是最新版本"
                    showToast("已是最新版本")
                } else {
                    appUpdateStatus = "发现新版本 ${update.versionName}"
                    appUpdateCandidate = update
                }
            }
        } catch (error: Exception) {
            runOnUiThread {
                appUpdateRunning = false
                appUpdateStatus = "检查失败：${error.message ?: "网络不可用"}"
                showInfo("检查更新失败", error.message ?: "无法读取更新信息")
            }
        }
    }
}

internal fun ComposeMainActivity.downloadAndInstallAppUpdate(update: BabyLogAppUpdateManager.UpdateInfo) {
    appUpdateRunning = true
    appUpdateStatus = "正在下载 ${update.versionName}..."
    runInBackground {
        try {
            val target = File(File(filesDir, "app-updates"), "栗记-${update.versionCode}.apk")
            val apk = BabyLogAppUpdateManager.downloadApk(update, target)
            runOnUiThread {
                appUpdateRunning = false
                appUpdateStatus = "下载完成，等待安装"
                BabyLogApkInstaller.install(this, apk, ::showInfo)
            }
        } catch (error: Exception) {
            runOnUiThread {
                appUpdateRunning = false
                appUpdateStatus = "下载失败：${error.message ?: "网络不可用"}"
                showInfo("下载更新失败", error.message ?: "无法下载或校验更新包")
            }
        }
    }
}
