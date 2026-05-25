@file:Suppress(
    "CyclomaticComplexMethod",
    "FunctionNaming",
    "InvalidPackageDeclaration",
    "LongParameterList",
    "ReturnCount",
    "TooGenericExceptionCaught"
)

package app.babylog.nativeapp

import androidx.compose.runtime.Composable

@Composable
internal fun SyncConfirmDialogs(
    syncConfirmState: SyncConfirmState?,
    syncPushConfirmState: SyncPushConfirmState?,
    onDismissSyncConfirm: () -> Unit,
    onConfirmSyncSettings: (SyncConfirmState) -> Unit,
    onDismissPushConfirm: () -> Unit,
    onConfirmPushSync: () -> Unit
) {
    syncConfirmState?.let { confirm ->
        ConfirmDialog(
            title = "确认启用同步",
            message = "启用后会按你配置的地址和家庭密钥尝试同步。家庭密钥仅保存在本机加密存储中，不会进入导出、备份或家庭同步；当前真实推拉仍在接入中，记录会保留在本机待同步队列中。请确认服务器地址、家庭密钥和医疗数据跨设备风险都已知晓。",
            confirmText = "我已知晓并保存",
            destructive = false,
            onDismiss = onDismissSyncConfirm,
            onConfirm = { onConfirmSyncSettings(confirm) }
        )
    }

    syncPushConfirmState?.let { confirm ->
        ConfirmDialog(
            title = "确认推送",
            message = "将把 ${confirm.pendingCount} 条本机记录加密上传到 ${confirm.backendBaseUrl}，其中可能包含附件文件。家庭密钥仅本机保存，服务器仅看到密文。",
            confirmText = "加密推送",
            destructive = false,
            onDismiss = onDismissPushConfirm,
            onConfirm = onConfirmPushSync
        )
    }
}

internal fun ComposeMainActivity.requestPushSyncNow() {
    if (syncPushRunning) return
    val dashboard = uiState.dashboard
    val pendingCount = dashboard?.pendingSyncCount ?: 0
    if (pendingCount <= 0) {
        showToast("暂无待推送记录")
        return
    }
    val config = uiState.syncConfig
    if (!config.enabled || config.backendBaseUrl.isBlank()) {
        showInfo("同步未配置", "请先在同步设置里填写家庭后端地址和家庭密钥。")
        return
    }
    if ((dashboard?.syncedSyncCount ?: 0) == 0) {
        syncPushConfirmState = SyncPushConfirmState(config.backendBaseUrl, pendingCount)
        return
    }
    pushSyncNow()
}

internal fun ComposeMainActivity.pushSyncNow() {
    if (syncPushRunning) return
    syncPushRunning = true
    syncPushMessage = "正在加密并推送本机记录..."
    runInBackground {
        try {
            val summary = BabyLogSyncPushOrchestrator().pushOnce(
                service,
                repository,
                syncSecretStore,
                repository.loadSyncSettings(),
                remoteSyncClient
            )
            runOnUiThread {
                syncPushRunning = false
                val noPushWork = summary.pushed == 0 &&
                    summary.failed == 0 &&
                    summary.filesUploaded == 0 &&
                    summary.filesPending == 0
                syncPushMessage = if (noPushWork) {
                    "上次推送：刚刚，已是最新；没有待推送记录或附件"
                } else {
                    "上次推送：刚刚，成功 ${summary.pushed}、失败 ${summary.failed}；文件上传 ${summary.filesUploaded} 个 / ${BabyLogFormatters.formatByteSize(summary.bytesUploaded)}"
                }
                if (summary.filesPending > 0) {
                    syncPushMessage += "；附件待重试 ${summary.filesPending} 个"
                }
                if (summary.failed > 0 && summary.lastError.isNotBlank()) {
                    syncPushMessage += "；失败原因：${formatSyncError(summary.lastError)}"
                }
                showToast(
                    if (noPushWork) {
                        "已是最新"
                    } else if (summary.failed == 0) {
                        "已加密推送 ${summary.pushed} 条，文件 ${summary.filesUploaded} 个"
                    } else {
                        "推送完成，失败 ${summary.failed} 条"
                    }
                )
            }
            reloadData()
        } catch (error: Exception) {
            runOnUiThread {
                syncPushRunning = false
                syncPushMessage = "上次推送失败：${error.message ?: "网络不可用"}"
                showInfo("推送失败", error.message ?: "无法推送本机记录")
            }
        }
    }
}

internal fun ComposeMainActivity.requestPullSyncNow() {
    pullSyncNow(silent = false)
}

internal fun ComposeMainActivity.shouldAutoPullSync(): Boolean {
    val config = repository.loadSyncSettings()
    return config.enabled && config.backendBaseUrl.isNotEmpty() && syncFamilyKeyConfigured
}

internal fun ComposeMainActivity.pullSyncNow(silent: Boolean) {
    if (syncPullRunning) return
    if (!shouldAutoPullSync()) {
        if (!silent) {
            showInfo("同步未配置", "请先在同步设置里填写家庭后端地址和家庭密钥。")
        }
        return
    }
    syncPullRunning = true
    if (!silent) {
        syncPullMessage = "正在拉取家人更新..."
    }
    runInBackground {
        try {
            val summary = BabyLogSyncPullOrchestrator().pullOnce(
                repository,
                syncSecretStore,
                repository.loadSyncSettings(),
                remoteSyncClient
            )
            runOnUiThread {
                syncPullRunning = false
                if (!silent) {
                    syncPullMessage = if (summary.lastError.isBlank()) {
                        "上次拉取：刚刚，新增 ${summary.applied}、忽略 ${summary.skipped}"
                    } else {
                        "上次拉取失败：${formatSyncError(summary.lastError)}"
                    }
                }
            }
            if (summary.lastError.isBlank()) {
                BabyLogSyncAttachmentDownloadWorker.enqueueIfNeeded(this)
                reloadData()
            }
            if (!silent) {
                if (summary.lastError.isBlank()) {
                    showToast(if (summary.applied > 0) "已同步 ${summary.applied} 条家人更新" else "已是最新")
                } else {
                    showToast("同步失败：${formatSyncError(summary.lastError)}")
                }
            }
        } catch (error: Exception) {
            runOnUiThread {
                syncPullRunning = false
                if (!silent) {
                    syncPullMessage = "上次拉取失败：${error.message ?: "网络不可用"}"
                }
            }
            if (!silent) {
                showToast("同步失败：${error.message ?: "网络不可用"}")
            }
        }
    }
}

internal fun ComposeMainActivity.dismissRemoteUpdateBanner() {
    runInBackground {
        service.dismissRemoteUpdateBanner()
        val dashboard = service.refreshDashboardOnly()
        runOnUiThread { uiState = uiState.copy(dashboard = dashboard) }
    }
}

private fun formatSyncError(code: String?): String {
    return when (code) {
        "BACKEND_NOT_CONFIGURED" -> "后端未配置"
        "BACKEND_UNREACHABLE" -> "后端暂不可达"
        "FAMILY_KEY_MISSING" -> "家庭密钥未配置"
        "FAMILY_KEY_LOAD_FAILED" -> "家庭密钥读取失败"
        "ENTITY_NOT_FOUND" -> "本机记录不存在"
        "ENCRYPT_FAILED" -> "加密失败"
        "PUSH_FAILED" -> "网络推送失败"
        "PULL_FAILED" -> "网络拉取失败"
        "STATUS_UPDATE_FAILED" -> "状态更新失败"
        else -> code ?: "未知错误"
    }
}
