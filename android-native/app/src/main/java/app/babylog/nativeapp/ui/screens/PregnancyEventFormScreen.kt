package app.babylog.nativeapp

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.io.File

@Composable
internal fun PregnancyEventFormScreen(
    action: BabyLogService.QuickAction?,
    draft: SmartEntryDraft?,
    isEditing: Boolean = false,
    attachmentPath: String?,
    attachmentName: String?,
    onPickAttachment: () -> Unit,
    onCaptureAttachment: () -> Unit,
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
    var department by rememberSaveable(action.eventType, draft?.nonce) { mutableStateOf(values["department"].orEmpty()) }
    var systolicBp by rememberSaveable(action.eventType, draft?.nonce) { mutableStateOf(values["systolicBp"].orEmpty()) }
    var diastolicBp by rememberSaveable(action.eventType, draft?.nonce) { mutableStateOf(values["diastolicBp"].orEmpty()) }
    var weightKg by rememberSaveable(action.eventType, draft?.nonce) { mutableStateOf(values["weightKg"].orEmpty()) }
    var fundalHeightCm by rememberSaveable(action.eventType, draft?.nonce) { mutableStateOf(values["fundalHeightCm"].orEmpty()) }
    var abdominalCircumferenceCm by rememberSaveable(action.eventType, draft?.nonce) { mutableStateOf(values["abdominalCircumferenceCm"].orEmpty()) }
    var fetalHeartRateBpm by rememberSaveable(action.eventType, draft?.nonce) { mutableStateOf(values["fetalHeartRateBpm"].orEmpty()) }
    var urineRoutine by rememberSaveable(action.eventType, draft?.nonce) { mutableStateOf(values["urineRoutine"].orEmpty()) }
    var tertiary by rememberSaveable(action.eventType, draft?.nonce) { mutableStateOf(values["tertiary"].orEmpty()) }
    var nextVisitDate by rememberSaveable(action.eventType, draft?.nonce) { mutableStateOf(values["nextVisitDate"].orEmpty()) }
    var note by rememberSaveable(action.eventType, draft?.nonce) { mutableStateOf(values["note"].orEmpty()) }
    val labels = pregnancyLabels(action.eventType)
    val isCheckup = action.eventType == "pregnancy_checkup"

    RecordFormScaffold(
        title = if (isEditing) "编辑${action.label}" else action.label,
        subtitle = if (isCheckup) "常规层结构化；报告结论请人工核对" else "常用字段优先，备注可后补",
        saveText = if (isEditing) "保存修改" else "保存记录",
        onBack = onBack,
        onSave = {
            if (isCheckup) {
                onSave(
                    BabyLogService.PregnancyInput.checkupStructured(
                        primary,
                        secondary,
                        department,
                        systolicBp,
                        diastolicBp,
                        weightKg,
                        fundalHeightCm,
                        abdominalCircumferenceCm,
                        fetalHeartRateBpm,
                        urineRoutine,
                        tertiary,
                        nextVisitDate,
                        note,
                        attachmentPath ?: "",
                        attachmentName ?: ""
                    )
                )
            } else {
                onSave(buildPregnancyInput(action.eventType, primary, secondary, tertiary, note))
            }
        }
    ) {
        if (isCheckup) {
            item { DateInputRow("检查日期", primary, { primary = it }, allowClear = false) }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("常规信息", color = ChestnutPalette.Ink, fontWeight = FontWeight.Bold)
                    Text("只记录报告或现场告知内容，不做风险判读", color = ChestnutPalette.Muted, fontSize = 12.sp)
                }
            }
            item { ChestnutTextField(labels.secondary, secondary, { secondary = it }, labels.secondaryKeyboard) }
            item { ChestnutTextField("科室 / 医生，可空", department, { department = it }, KeyboardType.Text) }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    UnitInputRow("收缩压", systolicBp, { systolicBp = it }, "mmHg", modifier = Modifier.weight(1f))
                    UnitInputRow("舒张压", diastolicBp, { diastolicBp = it }, "mmHg", modifier = Modifier.weight(1f))
                }
            }
            item { UnitInputRow("体重", weightKg, { weightKg = it }, "kg") }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    UnitInputRow("宫高", fundalHeightCm, { fundalHeightCm = it }, "cm", modifier = Modifier.weight(1f))
                    UnitInputRow("腹围", abdominalCircumferenceCm, { abdominalCircumferenceCm = it }, "cm", modifier = Modifier.weight(1f))
                }
            }
            item { UnitInputRow("胎心率", fetalHeartRateBpm, { fetalHeartRateBpm = it }, "bpm") }
            item {
                ChoiceChipRow(
                    label = "尿常规",
                    selected = urineRoutine,
                    options = listOf(
                        "正常" to "正常",
                        "+" to "+",
                        "++" to "++",
                        "+++" to "+++",
                        "见报告" to "见报告"
                    ),
                    onSelect = { urineRoutine = it }
                )
            }
            item { ChestnutTextField("尿常规补充，可空", urineRoutine, { urineRoutine = it }, KeyboardType.Text) }
            item { ChestnutLongTextField(labels.tertiary ?: "医生结论 / 建议", tertiary, { tertiary = it }) }
            item { DateInputRow("下次产检日期，可空", nextVisitDate, { nextVisitDate = it }) }
            item { ChestnutLongTextField(labels.note ?: "备注", note, { note = it }, minLines = 2, maxLines = 4) }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("检查单附件", color = ChestnutPalette.Ink, fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(onClick = onCaptureAttachment) { Text("拍照", color = ChestnutPalette.Primary) }
                        OutlinedButton(onClick = onPickAttachment) { Text("选图", color = ChestnutPalette.Primary) }
                    }
                    if (!attachmentPath.isNullOrBlank()) {
                        Text(
                            "已选择：${attachmentName ?: File(attachmentPath).name}",
                            color = ChestnutPalette.Primary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        } else {
            item { ChestnutTextField(labels.primary, primary, { primary = it }, labels.primaryKeyboard) }
            item { ChestnutTextField(labels.secondary, secondary, { secondary = it }, labels.secondaryKeyboard) }
            if (labels.tertiary != null) {
                item { ChestnutTextField(labels.tertiary, tertiary, { tertiary = it }, labels.tertiaryKeyboard) }
            }
            if (labels.note != null) {
                item { ChestnutLongTextField(labels.note, note, { note = it }) }
            }
        }
    }
}
