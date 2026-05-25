package app.babylog.nativeapp

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
internal fun SyncSettingsScreen(
    config: BabyLogDomain.BackendConfig,
    familyKeyConfigured: Boolean,
    checkingConnection: Boolean,
    connectionMessage: String,
    connectionOk: Boolean?,
    pendingSyncCount: Int,
    syncedSyncCount: Int,
    failedSyncCount: Int,
    pushingSync: Boolean,
    pushMessage: String,
    pullingSync: Boolean,
    pullMessage: String,
    lastPulledAt: String,
    remoteUpdateBannerCount: Int,
    onBack: () -> Unit,
    onCheckConnection: (String, String) -> Unit,
    onPushNow: () -> Unit,
    onPullNow: () -> Unit,
    onSave: (String, String) -> Unit
) {
    var backendBaseUrl by rememberSaveable(config.backendBaseUrl) { mutableStateOf(config.backendBaseUrl) }
    var familyKey by remember { mutableStateOf("") }

    SettingsPageScaffold(
        title = "同步设置",
        subtitle = "配置家庭共享后端地址和家庭密钥",
        onBack = onBack,
        onSave = { onSave(backendBaseUrl, familyKey) }
    ) {
        item {
            ChestnutTextField(
                label = "后端地址，例如 https://api.example.com",
                value = backendBaseUrl,
                onValueChange = { backendBaseUrl = it },
                keyboardType = KeyboardType.Uri
            )
        }
        item {
            ChestnutTextField(
                label = if (familyKeyConfigured) "家庭密钥（留空保留已保存密钥）" else "家庭密钥",
                value = familyKey,
                onValueChange = { familyKey = it },
                keyboardType = KeyboardType.Password,
                visualTransformation = PasswordVisualTransformation(),
                placeholder = if (familyKeyConfigured) "已保存，本机加密保存" else "由家人共享的同一串密钥"
            )
        }
        item {
            Text("留空后端地址会关闭同步。家庭密钥只保存在本机加密存储中，不进入导出、备份或家庭同步。", color = ChestnutPalette.Muted, fontSize = 13.sp)
        }
        item {
            OutlinedButton(
                modifier = Modifier.fillMaxWidth(),
                enabled = !checkingConnection,
                onClick = { onCheckConnection(backendBaseUrl, familyKey) }
            ) {
                Text(if (checkingConnection) "检测中..." else "检测连接", color = ChestnutPalette.Primary)
            }
        }
        if (connectionMessage.isNotBlank()) {
            item {
                Text(
                    connectionMessage,
                    color = if (connectionOk == true) ChestnutPalette.Primary else ChestnutPalette.Muted,
                    fontSize = 13.sp
                )
            }
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    modifier = Modifier.weight(1f),
                    enabled = !pushingSync,
                    onClick = onPushNow
                ) {
                    Text(if (pushingSync) "推送中..." else "立即推送", color = ChestnutPalette.Primary)
                }
                OutlinedButton(
                    modifier = Modifier.weight(1f),
                    enabled = !pullingSync,
                    onClick = onPullNow
                ) {
                    Text(if (pullingSync) "拉取中..." else "立即拉取", color = ChestnutPalette.Primary)
                }
            }
        }
        item {
            Text(
                "待同步：$pendingSyncCount 条\n已推送：$syncedSyncCount 条\n失败：$failedSyncCount 条\n上次拉取：${formatLastPulledAt(lastPulledAt)}\n本轮新拉取：$remoteUpdateBannerCount 条",
                color = ChestnutPalette.Muted,
                fontSize = 13.sp
            )
        }
        if (pullMessage.isNotBlank()) {
            item {
                Text(pullMessage, color = ChestnutPalette.Muted, fontSize = 13.sp)
            }
        }
        if (pushMessage.isNotBlank()) {
            item {
                Text(pushMessage, color = ChestnutPalette.Muted, fontSize = 13.sp)
            }
        }
    }
}

private fun formatLastPulledAt(value: String): String {
    if (value.isBlank()) {
        return "未拉取"
    }
    return "${BabyLogFormatters.formatEventDay(value)} ${BabyLogFormatters.formatEventTime(value)}"
}
