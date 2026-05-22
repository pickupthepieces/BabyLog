package app.babylog.nativeapp

import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.sp

@Composable
internal fun SyncSettingsScreen(
    config: BabyLogDomain.BackendConfig,
    familyKeyConfigured: Boolean,
    onBack: () -> Unit,
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
    }
}
