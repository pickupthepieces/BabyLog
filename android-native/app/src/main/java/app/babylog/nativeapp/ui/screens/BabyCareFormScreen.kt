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
    voiceState: SmartVoiceUiState,
    onLongTextVoiceStart: LongTextVoiceStart,
    onLongTextVoiceStop: () -> Unit,
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
    var checkupInstitution by rememberSaveable(action.eventType, draft?.nonce) {
        mutableStateOf(values["checkupInstitution"].orEmpty())
    }
    var checkupConclusion by rememberSaveable(action.eventType, draft?.nonce) {
        mutableStateOf(values["checkupConclusion"].orEmpty())
    }
    var nextCheckupDate by rememberSaveable(action.eventType, draft?.nonce) {
        mutableStateOf(values["nextCheckupDate"].orEmpty())
    }
    var occurredDate by rememberSaveable(action.eventType, draft?.nonce) {
        mutableStateOf(values["occurredDate"].orEmpty())
    }
    var occurredTime by rememberSaveable(action.eventType, draft?.nonce) {
        mutableStateOf(values["occurredTime"].orEmpty().ifBlank { currentBabyCareTimeInput() })
    }
    var formError by rememberSaveable(action.eventType, draft?.nonce) { mutableStateOf("") }
    val labels = babyCareLabels(action.eventType)

    RecordFormScaffold(
        title = if (isEditing) "编辑${action.label}" else action.label,
        subtitle = "核对字段后手动保存",
        saveText = if (isEditing) "保存修改" else "保存记录",
        onBack = onBack,
        onSave = {
            val input = if (action.eventType == "child_checkup") {
                BabyLogService.BabyCareInput.childCheckup(
                    primary,
                    secondary,
                    tertiary,
                    checkupInstitution,
                    checkupConclusion,
                    nextCheckupDate,
                    note
                ).withOccurredTime(occurredTime).withOccurredDate(occurredDate)
            } else {
                buildBabyCareInput(action.eventType, primary, secondary, tertiary, note, occurredTime, occurredDate)
            }
            if (!BabyLogService.hasBabyCareMinimumContent(input)) {
                formError = "请至少填写一项记录内容"
            } else {
                formError = ""
                onSave(input)
            }
        }
    ) {
        item { Text("常用信息", color = ChestnutPalette.Ink) }
        if (formError.isNotBlank()) {
            item { Text(formError, color = ChestnutPalette.Danger) }
        }
        item {
            DateInputRow(
                label = "发生日期（可选）",
                value = occurredDate,
                onValueChange = { occurredDate = it },
                allowClear = true
            )
        }
        item {
            TimeInputRow(
                label = "发生时刻",
                value = occurredTime,
                onValueChange = { occurredTime = it },
                allowClear = false
            )
        }
        item {
            BabyCareConfiguredField(
                label = labels.primary,
                value = primary,
                onValueChange = { primary = it },
                keyboardType = labels.primaryKeyboard,
                options = labels.primaryOptions,
                allowCustom = labels.primaryAllowCustom,
                isTime = labels.primaryIsTime
            )
        }
        item {
            BabyCareConfiguredField(
                label = labels.secondary,
                value = secondary,
                onValueChange = { secondary = it },
                keyboardType = labels.secondaryKeyboard,
                options = labels.secondaryOptions,
                allowCustom = labels.secondaryAllowCustom,
                isTime = labels.secondaryIsTime
            )
        }
        if (labels.tertiary != null) {
            item {
                if (action.eventType == "medication") {
                    ChestnutLongTextField(
                        labels.tertiary,
                        tertiary,
                        { tertiary = it },
                        minLines = 2,
                        maxLines = 4,
                        voiceState = voiceState,
                        onVoiceStart = onLongTextVoiceStart,
                        onVoiceStop = onLongTextVoiceStop
                    )
                } else {
                    BabyCareConfiguredField(
                        label = labels.tertiary,
                        value = tertiary,
                        onValueChange = { tertiary = it },
                        keyboardType = labels.tertiaryKeyboard,
                        options = labels.tertiaryOptions,
                        allowCustom = labels.tertiaryAllowCustom,
                        isTime = false
                    )
                }
            }
        }
        if (action.eventType == "child_checkup") {
            item {
                ChestnutTextField("机构 / 社区", checkupInstitution, { checkupInstitution = it }, KeyboardType.Text)
            }
            item {
                ChestnutLongTextField(
                    "儿保记录 / 医生备注",
                    checkupConclusion,
                    { checkupConclusion = it },
                    minLines = 2,
                    maxLines = 4,
                    voiceState = voiceState,
                    onVoiceStart = onLongTextVoiceStart,
                    onVoiceStop = onLongTextVoiceStop
                )
            }
            item {
                DateInputRow(
                    label = "下次儿保日期",
                    value = nextCheckupDate,
                    onValueChange = { nextCheckupDate = it },
                    allowClear = true
                )
            }
        }
        if (labels.note != null) {
            item {
                ChestnutLongTextField(
                    labels.note,
                    note,
                    { note = it },
                    voiceState = voiceState,
                    onVoiceStart = onLongTextVoiceStart,
                    onVoiceStop = onLongTextVoiceStop
                )
            }
        }
    }
}

private fun currentBabyCareTimeInput(): String {
    return BabyLogFormatters.formatEventTime(BabyLogFormatters.nowIso()).takeUnless { it == "--:--" }.orEmpty()
}

@Suppress("FunctionNaming", "LongParameterList")
@Composable
private fun BabyCareConfiguredField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    keyboardType: KeyboardType,
    options: List<Option>? = null,
    allowCustom: Boolean = false,
    isTime: Boolean = false
) {
    when {
        options != null -> ChoiceChipsRow(
            label = label,
            options = options,
            value = value,
            onValueChange = onValueChange,
            allowCustom = allowCustom
        )
        isTime -> TimeInputRow(label, value, onValueChange)
        else -> ChestnutTextField(label, value, onValueChange, keyboardType)
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
