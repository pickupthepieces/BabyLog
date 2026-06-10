package app.babylog.nativeapp

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Checkbox
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Switch
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
internal fun SpeechSettingsScreen(
    config: BabyLogSmartConfigStore.SpeechConfig?,
    onBack: () -> Unit,
    onSave: (BabyLogSmartConfigStore.SpeechConfig) -> Unit
) {
    if (config == null) {
        SettingsPageScaffold(
            title = "语音转文字",
            subtitle = "配置读取中",
            onBack = onBack
        ) {
            item {
                Text("正在读取本机配置。", color = ChestnutPalette.Muted)
            }
        }
        return
    }

    var enabled by rememberSaveable(config.isEnabled()) { mutableStateOf(config.isEnabled()) }
    var model by rememberSaveable(config.getModel()) {
        mutableStateOf(config.getModel().ifBlank { BabyLogSpeechToTextProtocol.DEFAULT_MODEL })
    }
    var apiKey by remember(config.getApiKey()) { mutableStateOf(config.getApiKey()) }
    var inverseTextNormalizationEnabled by rememberSaveable(config.isInverseTextNormalizationEnabled()) {
        mutableStateOf(config.isInverseTextNormalizationEnabled())
    }

    SettingsPageScaffold(
        title = "语音转文字",
        subtitle = "按住说话时转写",
        onBack = onBack,
        onSave = {
            onSave(
                BabyLogSmartConfigStore.SpeechConfig(
                    apiKey.trim(),
                    model.trim(),
                    enabled,
                    inverseTextNormalizationEnabled
                )
            )
        }
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = enabled, onCheckedChange = { enabled = it })
                Text("启用语音转文字", color = ChestnutPalette.Ink, fontWeight = FontWeight.Bold)
            }
        }
        item {
            OutlinedButton(
                onClick = {
                    enabled = true
                    model = BabyLogSpeechToTextProtocol.DEFAULT_MODEL
                },
                border = BorderStroke(1.dp, ChestnutPalette.Border)
            ) {
                Text("DashScope Paraformer", color = ChestnutPalette.Ink, fontSize = 12.sp)
            }
            Text(
                "使用 DashScope Paraformer；API Key 可与智能解析共用。",
                color = ChestnutPalette.Muted,
                fontSize = 12.sp
            )
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("数字转写为阿拉伯数字", color = ChestnutPalette.Ink, fontWeight = FontWeight.Bold)
                    Text(
                        "实验性。关闭后保留中文数字原文，由智能识别负责转换。",
                        color = ChestnutPalette.Muted,
                        fontSize = 12.sp
                    )
                }
                Switch(
                    checked = inverseTextNormalizationEnabled,
                    onCheckedChange = { inverseTextNormalizationEnabled = it }
                )
            }
        }
        item {
            ChestnutTextField(
                label = "模型",
                value = model,
                onValueChange = { model = it },
                keyboardType = KeyboardType.Text
            )
        }
        item {
            ChestnutTextField(
                label = "API Key",
                value = apiKey,
                onValueChange = { apiKey = it },
                keyboardType = KeyboardType.Password,
                visualTransformation = PasswordVisualTransformation()
            )
        }
        item {
            Text(
                "Key 仅保存在本机。只有按住说话时，本次语音才会发送给识别服务商。",
                color = Color(0xFF7C4A21),
                fontSize = 13.sp,
                modifier = Modifier
                    .clip(RoundedCornerShape(ChestnutRadius.Small))
                    .background(Color(0xFFFFEBCB))
                    .border(1.dp, Color(0xFFFFD89C), RoundedCornerShape(ChestnutRadius.Small))
                    .padding(12.dp)
            )
        }
    }
}
