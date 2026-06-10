package app.babylog.nativeapp

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Checkbox
import androidx.compose.material.OutlinedButton
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
internal fun SmartModelSettingsScreen(
    config: BabyLogSmartConfigStore.Config?,
    onBack: () -> Unit,
    onSave: (BabyLogSmartConfigStore.Config) -> Unit
) {
    if (config == null) {
        SettingsPageScaffold(
            title = "OCR / 智能解析模型",
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
    var baseUrl by rememberSaveable(config.getBaseUrl()) { mutableStateOf(config.getBaseUrl()) }
    var model by rememberSaveable(config.getModel()) { mutableStateOf(config.getModel()) }
    var textModel by rememberSaveable(config.getTextModel()) { mutableStateOf(config.getTextModel()) }
    var apiKey by remember(config.getApiKey()) { mutableStateOf(config.getApiKey()) }

    SettingsPageScaffold(
        title = "OCR / 智能解析模型",
        subtitle = "用于 OCR 和智能录入",
        onBack = onBack,
        onSave = {
            onSave(
                BabyLogSmartConfigStore.Config(
                    baseUrl.trim(),
                    model.trim(),
                    textModel.trim(),
                    apiKey.trim(),
                    enabled
                )
            )
        }
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = enabled, onCheckedChange = { enabled = it })
                Text("启用 OCR / 智能解析", color = ChestnutPalette.Ink, fontWeight = FontWeight.Bold)
            }
        }
        item {
            Text("服务商预设", color = ChestnutPalette.Muted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                smartModelPresets().forEach { preset ->
                    OutlinedButton(
                        onClick = {
                            enabled = true
                            baseUrl = preset.baseUrl
                            model = preset.model
                            textModel = preset.textModel
                        },
                        border = BorderStroke(1.dp, ChestnutPalette.Border)
                    ) {
                        Text(preset.label, color = ChestnutPalette.Ink, fontSize = 12.sp)
                    }
                }
            }
            Text(
                "预设会填入地址和模型，API Key 需手动填写。",
                color = ChestnutPalette.Muted,
                fontSize = 12.sp
            )
        }
        item {
            ChestnutTextField(
                label = "Base URL",
                value = baseUrl,
                onValueChange = { baseUrl = it },
                keyboardType = KeyboardType.Uri
            )
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
                label = "文本模型（可选）",
                value = textModel,
                onValueChange = { textModel = it },
                keyboardType = KeyboardType.Text,
                placeholder = "留空则与上方模型相同；纯文本任务可填更快的模型如 qwen-plus"
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
                "Key 仅保存在本机。识别或智能录入时，图片和文字才会发送给模型服务商。",
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

private data class SmartModelPreset(
    val label: String,
    val baseUrl: String,
    val model: String,
    val textModel: String = ""
)

private fun smartModelPresets() = listOf(
    SmartModelPreset(
        "使用阿里云千问",
        "https://dashscope.aliyuncs.com/compatible-mode/v1",
        "qwen-vl-max",
        "qwen-plus"
    ),
    SmartModelPreset(
        "Qwen VL Plus",
        "https://dashscope.aliyuncs.com/compatible-mode/v1",
        "qwen3-vl-plus"
    ),
    SmartModelPreset(
        "Qwen Flash",
        "https://dashscope.aliyuncs.com/compatible-mode/v1",
        "qwen3-vl-flash"
    ),
    SmartModelPreset(
        "OpenAI",
        "https://api.openai.com/v1",
        "gpt-4o-mini"
    )
)
