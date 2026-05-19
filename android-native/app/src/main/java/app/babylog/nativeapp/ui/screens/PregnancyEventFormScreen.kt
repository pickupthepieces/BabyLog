package app.babylog.nativeapp

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue

@Composable
internal fun PregnancyEventFormScreen(
    action: BabyLogService.QuickAction?,
    draft: SmartEntryDraft?,
    isEditing: Boolean = false,
    onBack: () -> Unit,
    onSave: (BabyLogService.PregnancyInput) -> Unit
) {
    if (action == null) {
        MissingRecordRouteScreen("孕期记录", onBack)
        return
    }
    val values = draft?.values.orEmpty()
    var primary by rememberSaveable(action.eventType, draft?.nonce) {
        mutableStateOf(values["primary"] ?: defaultPregnancyPrimary(action.eventType))
    }
    var secondary by rememberSaveable(action.eventType, draft?.nonce) { mutableStateOf(values["secondary"].orEmpty()) }
    var tertiary by rememberSaveable(action.eventType, draft?.nonce) { mutableStateOf(values["tertiary"].orEmpty()) }
    var note by rememberSaveable(action.eventType, draft?.nonce) { mutableStateOf(values["note"].orEmpty()) }
    val labels = pregnancyLabels(action.eventType)

    RecordFormScaffold(
        title = if (isEditing) "编辑${action.label}" else action.label,
        subtitle = "常用字段优先，备注可后补",
        saveText = if (isEditing) "保存修改" else "保存记录",
        onBack = onBack,
        onSave = { onSave(buildPregnancyInput(action.eventType, primary, secondary, tertiary, note)) }
    ) {
        if (action.eventType == "pregnancy_checkup") {
            item { DateInputRow("检查日期", primary, { primary = it }, allowClear = false) }
        } else {
            item { ChestnutTextField(labels.primary, primary, { primary = it }, labels.primaryKeyboard) }
        }
        item { ChestnutTextField(labels.secondary, secondary, { secondary = it }, labels.secondaryKeyboard) }
        if (labels.tertiary != null) {
            item {
                if (action.eventType == "pregnancy_checkup") {
                    ChestnutLongTextField(labels.tertiary, tertiary, { tertiary = it })
                } else {
                    ChestnutTextField(labels.tertiary, tertiary, { tertiary = it }, labels.tertiaryKeyboard)
                }
            }
        }
        if (labels.note != null) {
            item { ChestnutLongTextField(labels.note, note, { note = it }) }
        }
    }
}
