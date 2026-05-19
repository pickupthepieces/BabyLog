package app.babylog.nativeapp

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.KeyboardType

@Composable
internal fun BabyCareFormScreen(
    action: BabyLogService.QuickAction?,
    draft: SmartEntryDraft?,
    isEditing: Boolean = false,
    onBack: () -> Unit,
    onSave: (BabyLogService.BabyCareInput) -> Unit
) {
    if (action == null) {
        MissingRecordRouteScreen("宝宝记录", onBack)
        return
    }
    val values = draft?.values.orEmpty()
    var primary by rememberSaveable(action.eventType, draft?.nonce) { mutableStateOf(values["primary"].orEmpty()) }
    var secondary by rememberSaveable(action.eventType, draft?.nonce) { mutableStateOf(values["secondary"].orEmpty()) }
    var tertiary by rememberSaveable(action.eventType, draft?.nonce) { mutableStateOf(values["tertiary"].orEmpty()) }
    var note by rememberSaveable(action.eventType, draft?.nonce) { mutableStateOf(values["note"].orEmpty()) }
    val labels = babyCareLabels(action.eventType)

    RecordFormScaffold(
        title = if (isEditing) "编辑${action.label}" else action.label,
        subtitle = "核对字段后手动保存",
        saveText = if (isEditing) "保存修改" else "保存记录",
        onBack = onBack,
        onSave = { onSave(buildBabyCareInput(action.eventType, primary, secondary, tertiary, note)) }
    ) {
        item { Text("常用信息", color = ChestnutPalette.Ink) }
        item { ChestnutTextField(labels.primary, primary, { primary = it }, labels.primaryKeyboard) }
        item { ChestnutTextField(labels.secondary, secondary, { secondary = it }, labels.secondaryKeyboard) }
        if (labels.tertiary != null) {
            item {
                if (action.eventType == "medication") {
                    ChestnutLongTextField(labels.tertiary, tertiary, { tertiary = it }, minLines = 2, maxLines = 4)
                } else {
                    ChestnutTextField(labels.tertiary, tertiary, { tertiary = it }, KeyboardType.Text)
                }
            }
        }
        if (labels.note != null) {
            item { ChestnutLongTextField(labels.note, note, { note = it }) }
        }
    }
}

@Composable
internal fun MissingRecordRouteScreen(title: String, onBack: () -> Unit) {
    RecordFormScaffold(
        title = title,
        subtitle = "当前没有待编辑表单",
        saveText = "返回",
        onBack = onBack,
        onSave = onBack
    ) {
        item { EmptyPanel("请从快捷记录或智能录入重新打开表单。") }
    }
}
