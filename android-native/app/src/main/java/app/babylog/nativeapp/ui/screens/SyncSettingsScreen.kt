package app.babylog.nativeapp

import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.sp

@Composable
internal fun SyncSettingsScreen(
    config: BabyLogDomain.BackendConfig,
    onBack: () -> Unit,
    onSave: (String) -> Unit
) {
    var backendBaseUrl by remember(config.backendBaseUrl) { mutableStateOf(config.backendBaseUrl) }

    SettingsPageScaffold(
        title = "同步设置",
        subtitle = "配置家庭共享后端地址",
        onBack = onBack,
        onSave = { onSave(backendBaseUrl) }
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
            Text("留空会关闭后端同步，本机记录不受影响。", color = ChestnutPalette.Muted, fontSize = 13.sp)
        }
    }
}
