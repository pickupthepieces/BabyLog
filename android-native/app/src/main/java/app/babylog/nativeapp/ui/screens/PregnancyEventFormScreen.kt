package app.babylog.nativeapp

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.io.File

@Composable
internal fun PregnancyEventFormScreen(
    action: BabyLogService.QuickAction?,
    draft: SmartEntryDraft?,
    isEditing: Boolean = false,
    expectedDueDate: String,
    attachmentPath: String?,
    attachmentName: String?,
    ocrRunning: Boolean,
    ocrCandidate: BabyLogSmartTextClient.SmartFillCandidate?,
    onPickAttachment: () -> Unit,
    onCaptureAttachment: () -> Unit,
    onRecognizeAttachment: () -> Unit,
    onCandidateDismiss: () -> Unit,
    onCandidateApplied: () -> Unit,
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
    var gestationalAge by rememberSaveable(action.eventType, draft?.nonce) {
        mutableStateOf(values["gestationalAge"].orEmpty())
    }
    var gestationalAgeEdited by rememberSaveable(action.eventType, draft?.nonce) {
        mutableStateOf(!values["gestationalAge"].isNullOrBlank())
    }
    var secondary by rememberSaveable(action.eventType, draft?.nonce) { mutableStateOf(values["secondary"].orEmpty()) }
    var department by rememberSaveable(action.eventType, draft?.nonce) { mutableStateOf(values["department"].orEmpty()) }
    var systolicBp by rememberSaveable(action.eventType, draft?.nonce) { mutableStateOf(values["systolicBp"].orEmpty()) }
    var diastolicBp by rememberSaveable(action.eventType, draft?.nonce) { mutableStateOf(values["diastolicBp"].orEmpty()) }
    var weightKg by rememberSaveable(action.eventType, draft?.nonce) { mutableStateOf(values["weightKg"].orEmpty()) }
    var fundalHeightCm by rememberSaveable(action.eventType, draft?.nonce) { mutableStateOf(values["fundalHeightCm"].orEmpty()) }
    var abdominalCircumferenceCm by rememberSaveable(action.eventType, draft?.nonce) { mutableStateOf(values["abdominalCircumferenceCm"].orEmpty()) }
    var fetalHeartRateBpm by rememberSaveable(action.eventType, draft?.nonce) { mutableStateOf(values["fetalHeartRateBpm"].orEmpty()) }
    var fetalPresentation by rememberSaveable(action.eventType, draft?.nonce) { mutableStateOf(values["fetalPresentation"].orEmpty()) }
    var edema by rememberSaveable(action.eventType, draft?.nonce) { mutableStateOf(values["edema"].orEmpty()) }
    var urineRoutine by rememberSaveable(action.eventType, draft?.nonce) { mutableStateOf(values["urineRoutine"].orEmpty()) }
    var urineProtein by rememberSaveable(action.eventType, draft?.nonce) { mutableStateOf(values["urineProtein"].orEmpty()) }
    var hemoglobinGL by rememberSaveable(action.eventType, draft?.nonce) { mutableStateOf(values["hemoglobinGL"].orEmpty()) }
    var highRiskFactors by rememberSaveable(action.eventType, draft?.nonce) { mutableStateOf(values["highRiskFactors"].orEmpty()) }
    var tertiary by rememberSaveable(action.eventType, draft?.nonce) { mutableStateOf(values["tertiary"].orEmpty()) }
    var treatmentAdvice by rememberSaveable(action.eventType, draft?.nonce) { mutableStateOf(values["treatmentAdvice"].orEmpty()) }
    var nextVisitDate by rememberSaveable(action.eventType, draft?.nonce) { mutableStateOf(values["nextVisitDate"].orEmpty()) }
    var reportType by rememberSaveable(action.eventType, draft?.nonce) { mutableStateOf(values["reportType"].orEmpty()) }
    var attachmentNote by rememberSaveable(action.eventType, draft?.nonce) { mutableStateOf(values["attachmentNote"].orEmpty()) }
    var note by rememberSaveable(action.eventType, draft?.nonce) { mutableStateOf(values["note"].orEmpty()) }
    val labels = pregnancyLabels(action.eventType)
    val isCheckup = action.eventType == "pregnancy_checkup"

    LaunchedEffect(isCheckup, primary, expectedDueDate) {
        if (isCheckup && !gestationalAgeEdited) {
            val inferred = gestationalAgeInputForDate(expectedDueDate, primary)
            if (inferred.isNotBlank()) {
                gestationalAge = inferred
            }
        }
    }

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
                        gestationalAge,
                        secondary,
                        department,
                        systolicBp,
                        diastolicBp,
                        weightKg,
                        fundalHeightCm,
                        abdominalCircumferenceCm,
                        fetalHeartRateBpm,
                        fetalPresentation,
                        edema,
                        urineRoutine,
                        urineProtein,
                        hemoglobinGL,
                        highRiskFactors,
                        tertiary,
                        treatmentAdvice,
                        nextVisitDate,
                        reportType,
                        attachmentNote,
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
                ChestnutTextField(
                    "孕周，例如 22+5（可自动推算，也可改）",
                    gestationalAge,
                    {
                        gestationalAgeEdited = true
                        gestationalAge = it
                    },
                    KeyboardType.Text
                )
            }
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
            item { UnitInputRow("血红蛋白 Hb", hemoglobinGL, { hemoglobinGL = it }, "g/L") }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    UnitInputRow("宫高", fundalHeightCm, { fundalHeightCm = it }, "cm", modifier = Modifier.weight(1f))
                    UnitInputRow("腹围", abdominalCircumferenceCm, { abdominalCircumferenceCm = it }, "cm", modifier = Modifier.weight(1f))
                }
            }
            item { UnitInputRow("胎心率", fetalHeartRateBpm, { fetalHeartRateBpm = it }, "bpm") }
            item {
                ChoiceChipRow(
                    label = "胎位",
                    selected = fetalPresentation,
                    options = listOf(
                        "头位" to "头位",
                        "臀位" to "臀位",
                        "横位" to "横位",
                        "未记录" to "未记录"
                    ),
                    onSelect = { fetalPresentation = it }
                )
            }
            item {
                ChoiceChipRow(
                    label = "水肿",
                    selected = edema,
                    options = listOf(
                        "无" to "无",
                        "轻" to "轻",
                        "中" to "中",
                        "重" to "重",
                        "未记录" to "未记录"
                    ),
                    onSelect = { edema = it }
                )
            }
            item {
                ChoiceChipRow(
                    label = "尿蛋白",
                    selected = urineProtein,
                    options = listOf(
                        "阴性" to "阴性",
                        "±" to "±",
                        "+" to "+",
                        "++" to "++",
                        "+++" to "+++",
                        "见报告" to "见报告"
                    ),
                    onSelect = { urineProtein = it }
                )
            }
            item { ChestnutTextField("尿常规摘要，可空", urineRoutine, { urineRoutine = it }, KeyboardType.Text) }
            item { ChestnutLongTextField("高危因素 / 特殊情况，可空", highRiskFactors, { highRiskFactors = it }, minLines = 2, maxLines = 4) }
            item { ChestnutLongTextField(labels.tertiary ?: "医生结论 / 建议", tertiary, { tertiary = it }, minLines = 2, maxLines = 5) }
            item { ChestnutLongTextField("处理及建议，可空", treatmentAdvice, { treatmentAdvice = it }, minLines = 2, maxLines = 5) }
            item { DateInputRow("下次产检日期，可空", nextVisitDate, { nextVisitDate = it }) }
            item { ChestnutLongTextField(labels.note ?: "备注", note, { note = it }, minLines = 2, maxLines = 4) }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("检查单附件", color = ChestnutPalette.Ink, fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(onClick = onCaptureAttachment) { Text("拍照", color = ChestnutPalette.Primary) }
                        OutlinedButton(onClick = onPickAttachment) { Text("选图", color = ChestnutPalette.Primary) }
                        Button(
                            enabled = !attachmentPath.isNullOrBlank() && !ocrRunning,
                            onClick = onRecognizeAttachment,
                            colors = ButtonDefaults.buttonColors(
                                backgroundColor = if (!attachmentPath.isNullOrBlank()) ChestnutPalette.Primary else ChestnutPalette.Surface2,
                                disabledBackgroundColor = ChestnutPalette.Surface2
                            )
                        ) {
                            Text(
                                if (ocrRunning) "识别中..." else if (attachmentPath.isNullOrBlank()) "先选图" else "识别",
                                color = if (!attachmentPath.isNullOrBlank() && !ocrRunning) Color.White else ChestnutPalette.Text3
                            )
                        }
                    }
                    if (!attachmentPath.isNullOrBlank()) {
                        Text(
                            "已选择：${attachmentName ?: File(attachmentPath).name}",
                            color = ChestnutPalette.Primary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    if (ocrCandidate != null) {
                        CheckupOcrCandidateCard(
                            candidate = ocrCandidate,
                            onApply = {
                                ocrCandidate.values["primary"]?.takeIf { BabyLogFormatters.isValidDateInput(it) }?.let {
                                    primary = it
                                    if (!gestationalAgeEdited) {
                                        gestationalAgeInputForDate(expectedDueDate, it).takeIf { value -> value.isNotBlank() }?.let { value ->
                                            gestationalAge = value
                                        }
                                    }
                                }
                                ocrCandidate.values["gestationalAge"]?.let {
                                    gestationalAgeEdited = true
                                    gestationalAge = it
                                }
                                ocrCandidate.values["secondary"]?.let { secondary = it }
                                ocrCandidate.values["department"]?.let { department = it }
                                ocrCandidate.values["systolicBp"]?.let { systolicBp = it }
                                ocrCandidate.values["diastolicBp"]?.let { diastolicBp = it }
                                ocrCandidate.values["weightKg"]?.let { weightKg = it }
                                ocrCandidate.values["fundalHeightCm"]?.let { fundalHeightCm = it }
                                ocrCandidate.values["abdominalCircumferenceCm"]?.let { abdominalCircumferenceCm = it }
                                ocrCandidate.values["fetalHeartRateBpm"]?.let { fetalHeartRateBpm = it }
                                ocrCandidate.values["fetalPresentation"]?.let { fetalPresentation = it }
                                ocrCandidate.values["edema"]?.let { edema = it }
                                ocrCandidate.values["urineRoutine"]?.let { urineRoutine = it }
                                ocrCandidate.values["urineProtein"]?.let { urineProtein = it }
                                ocrCandidate.values["hemoglobinGL"]?.let { hemoglobinGL = it }
                                ocrCandidate.values["highRiskFactors"]?.let { highRiskFactors = it }
                                ocrCandidate.values["tertiary"]?.let { tertiary = it }
                                ocrCandidate.values["treatmentAdvice"]?.let { treatmentAdvice = it }
                                ocrCandidate.values["nextVisitDate"]?.takeIf { BabyLogFormatters.isValidDateInput(it) }?.let { nextVisitDate = it }
                                ocrCandidate.values["reportType"]?.let { reportType = it }
                                ocrCandidate.values["attachmentNote"]?.let { attachmentNote = it }
                                ocrCandidate.values["note"]?.let { note = it }
                                onCandidateApplied()
                            },
                            onDismiss = onCandidateDismiss
                        )
                    }
                    CheckupAttachmentFields(
                        reportType = reportType,
                        onReportTypeChange = { reportType = it },
                        attachmentNote = attachmentNote,
                        onAttachmentNoteChange = { attachmentNote = it }
                    )
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

@Composable
private fun CheckupAttachmentFields(
    reportType: String,
    onReportTypeChange: (String) -> Unit,
    attachmentNote: String,
    onAttachmentNoteChange: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ChestnutTextField("报告类型，可空", reportType, onReportTypeChange, KeyboardType.Text)
        ChestnutTextField("附件备注，可空", attachmentNote, onAttachmentNoteChange, KeyboardType.Text)
    }
}

@Composable
private fun CheckupOcrCandidateCard(
    candidate: BabyLogSmartTextClient.SmartFillCandidate,
    onApply: () -> Unit,
    onDismiss: () -> Unit
) {
    val rows = candidate.values.entries
        .filter { it.value.isNotBlank() }
        .take(10)
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = ChestnutPalette.PrimarySoft,
        border = BorderStroke(1.dp, ChestnutPalette.Primary.copy(alpha = 0.36f)),
        elevation = 0.dp
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("识别候选", color = ChestnutPalette.Ink, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            if (rows.isEmpty()) {
                Text("模型没有返回可用字段，可以保留附件后手动填写。", color = ChestnutPalette.Muted, fontSize = 13.sp)
            } else {
                rows.forEach { entry ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(entry.key, color = ChestnutPalette.Muted, fontSize = 12.sp, modifier = Modifier.weight(0.42f))
                        Text(
                            entry.value,
                            color = ChestnutPalette.Ink,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(0.58f)
                        )
                    }
                }
            }
            if (candidate.warnings.isNotEmpty()) {
                Text(
                    "需核对：" + candidate.warnings.joinToString("；"),
                    color = Color(0xFF7C4A21),
                    fontSize = 12.sp
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                TextButton(onClick = onDismiss) {
                    Text("不用候选", color = ChestnutPalette.Muted)
                }
                Button(
                    enabled = rows.isNotEmpty(),
                    onClick = onApply,
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = ChestnutPalette.Primary,
                        disabledBackgroundColor = ChestnutPalette.Surface2
                    )
                ) {
                    Text("应用到表单", color = Color.White)
                }
            }
        }
    }
}
