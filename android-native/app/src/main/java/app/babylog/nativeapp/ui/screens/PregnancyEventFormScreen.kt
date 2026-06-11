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
    voiceState: SmartVoiceUiState,
    onLongTextVoiceStart: LongTextVoiceStart,
    onLongTextVoiceStop: () -> Unit,
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
    var ntMm by rememberSaveable(action.eventType, draft?.nonce) { mutableStateOf(values["ntMm"].orEmpty()) }
    var riskT21 by rememberSaveable(action.eventType, draft?.nonce) { mutableStateOf(values["riskT21"].orEmpty()) }
    var riskT18 by rememberSaveable(action.eventType, draft?.nonce) { mutableStateOf(values["riskT18"].orEmpty()) }
    var riskOntd by rememberSaveable(action.eventType, draft?.nonce) { mutableStateOf(values["riskOntd"].orEmpty()) }
    var riskLevel by rememberSaveable(action.eventType, draft?.nonce) { mutableStateOf(values["riskLevel"].orEmpty()) }
    var t21Result by rememberSaveable(action.eventType, draft?.nonce) { mutableStateOf(values["t21Result"].orEmpty()) }
    var t18Result by rememberSaveable(action.eventType, draft?.nonce) { mutableStateOf(values["t18Result"].orEmpty()) }
    var t13Result by rememberSaveable(action.eventType, draft?.nonce) { mutableStateOf(values["t13Result"].orEmpty()) }
    var sexChromosome by rememberSaveable(action.eventType, draft?.nonce) { mutableStateOf(values["sexChromosome"].orEmpty()) }
    var structureConclusion by rememberSaveable(action.eventType, draft?.nonce) { mutableStateOf(values["structureConclusion"].orEmpty()) }
    var fastingGlucoseMmolL by rememberSaveable(action.eventType, draft?.nonce) { mutableStateOf(values["fastingGlucoseMmolL"].orEmpty()) }
    var oneHourGlucoseMmolL by rememberSaveable(action.eventType, draft?.nonce) { mutableStateOf(values["oneHourGlucoseMmolL"].orEmpty()) }
    var twoHourGlucoseMmolL by rememberSaveable(action.eventType, draft?.nonce) { mutableStateOf(values["twoHourGlucoseMmolL"].orEmpty()) }
    var abnormalFlag by rememberSaveable(action.eventType, draft?.nonce) { mutableStateOf(values["abnormalFlag"].orEmpty()) }
    var gbsResult by rememberSaveable(action.eventType, draft?.nonce) { mutableStateOf(values["gbsResult"].orEmpty()) }
    var nstResult by rememberSaveable(action.eventType, draft?.nonce) { mutableStateOf(values["nstResult"].orEmpty()) }
    var conclusion by rememberSaveable(action.eventType, draft?.nonce) { mutableStateOf(values["conclusion"].orEmpty()) }
    var formError by rememberSaveable(action.eventType, draft?.nonce) { mutableStateOf("") }
    val labels = pregnancyLabels(action.eventType)
    val isCheckup = action.eventType == "pregnancy_checkup"
    val isScreening = BabyLogService.isScreeningEventType(action.eventType)

    LaunchedEffect(isCheckup, isScreening, primary, expectedDueDate) {
        if ((isCheckup || isScreening) && !gestationalAgeEdited) {
            val inferred = gestationalAgeInputForDate(expectedDueDate, primary)
            if (inferred.isNotBlank()) {
                gestationalAge = inferred
            }
        }
    }

    RecordFormScaffold(
        title = if (isEditing) "编辑${action.label}" else action.label,
        subtitle = if (isCheckup || isScreening) "按报告原文记录" else "填写常用字段，详情可后补",
        saveText = if (isEditing) "保存修改" else "保存记录",
        onBack = onBack,
        onSave = {
            val input = if (isCheckup) {
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
            } else if (isScreening) {
                BabyLogService.PregnancyInput.screening(
                    action.eventType,
                    primary,
                    gestationalAge,
                    screeningValues(
                        action.eventType,
                        ntMm,
                        riskT21,
                        riskT18,
                        riskOntd,
                        riskLevel,
                        t21Result,
                        t18Result,
                        t13Result,
                        sexChromosome,
                        structureConclusion,
                        fastingGlucoseMmolL,
                        oneHourGlucoseMmolL,
                        twoHourGlucoseMmolL,
                        abnormalFlag,
                        gbsResult,
                        nstResult,
                        conclusion,
                        attachmentNote
                    ),
                    note,
                    attachmentPath ?: "",
                    attachmentName ?: ""
                )
            } else {
                buildPregnancyInput(action.eventType, primary, secondary, tertiary, note)
            }
            if (!BabyLogService.hasPregnancyMinimumContent(input)) {
                formError = "请至少填写一项记录内容"
            } else {
                formError = ""
                onSave(input)
            }
        }
    ) {
        if (formError.isNotBlank()) {
            item { Text(formError, color = ChestnutPalette.Danger, fontWeight = FontWeight.Bold) }
        }
        if (isCheckup) {
            item { DateInputRow("检查日期", primary, { primary = it }, allowClear = false) }
            item {
                GestationalAgeInputRow(
                    "孕周",
                    gestationalAge,
                    {
                        gestationalAgeEdited = true
                        gestationalAge = it
                    }
                )
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("常规信息", color = ChestnutPalette.Ink, fontWeight = FontWeight.Bold)
                    Text("按报告原文填写日期、孕周、科室与备注。", color = ChestnutPalette.Muted, fontSize = 12.sp)
                }
            }
            item { ChestnutTextField(labels.secondary, secondary, { secondary = it }, labels.secondaryKeyboard) }
            item { ChestnutTextField("科室 / 医生", department, { department = it }, KeyboardType.Text) }
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
            item { ChestnutTextField("尿常规摘要", urineRoutine, { urineRoutine = it }, KeyboardType.Text) }
            item { VoiceLongTextField("高危因素 / 特殊情况", highRiskFactors, { highRiskFactors = it }, voiceState, onLongTextVoiceStart, onLongTextVoiceStop, minLines = 2, maxLines = 4) }
            item { VoiceLongTextField(labels.tertiary ?: "医生结论 / 建议", tertiary, { tertiary = it }, voiceState, onLongTextVoiceStart, onLongTextVoiceStop, minLines = 2, maxLines = 5) }
            item { VoiceLongTextField("处理建议", treatmentAdvice, { treatmentAdvice = it }, voiceState, onLongTextVoiceStart, onLongTextVoiceStop, minLines = 2, maxLines = 5) }
            item { DateInputRow("下次产检日期", nextVisitDate, { nextVisitDate = it }) }
            item { VoiceLongTextField(labels.note ?: "备注", note, { note = it }, voiceState, onLongTextVoiceStart, onLongTextVoiceStop, minLines = 2, maxLines = 4) }
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
        } else if (isScreening) {
            item { DateInputRow("检查日期", primary, { primary = it }, allowClear = false) }
            item {
                GestationalAgeInputRow(
                    "孕周",
                    gestationalAge,
                    {
                        gestationalAgeEdited = true
                        gestationalAge = it
                    }
                )
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(BabyLogFormatters.eventLabel(action.eventType), color = ChestnutPalette.Ink, fontWeight = FontWeight.Bold)
                    Text("分级、阴阳性和标注均按报告原文填写。", color = ChestnutPalette.Muted, fontSize = 12.sp)
                }
            }
            when (action.eventType) {
                "screening_nt" -> {
                    item { UnitInputRow("NT", ntMm, { ntMm = it }, "mm") }
                    item { VoiceLongTextField("结论文本", conclusion, { conclusion = it }, voiceState, onLongTextVoiceStart, onLongTextVoiceStop, minLines = 2, maxLines = 5) }
                }
                "screening_serum" -> {
                    item { ChestnutTextField("21 三体风险值", riskT21, { riskT21 = it }, KeyboardType.Text) }
                    item { ChestnutTextField("18 三体风险值", riskT18, { riskT18 = it }, KeyboardType.Text) }
                    item { ChestnutTextField("开放性神经管风险", riskOntd, { riskOntd = it }, KeyboardType.Text) }
                    item {
                        ChoiceChipRow(
                            label = "报告分级",
                            selected = riskLevel,
                            options = listOf("低危" to "低危", "临界" to "临界", "高危" to "高危", "见报告" to "见报告"),
                            onSelect = { riskLevel = it }
                        )
                    }
                    item { VoiceLongTextField("结论文本", conclusion, { conclusion = it }, voiceState, onLongTextVoiceStart, onLongTextVoiceStop, minLines = 2, maxLines = 5) }
                }
                "screening_nipt" -> {
                    item {
                        ChoiceChipRow(
                            label = "T21",
                            selected = t21Result,
                            options = listOf("低风险" to "低风险", "高风险" to "高风险", "见报告" to "见报告"),
                            onSelect = { t21Result = it }
                        )
                    }
                    item {
                        ChoiceChipRow(
                            label = "T18",
                            selected = t18Result,
                            options = listOf("低风险" to "低风险", "高风险" to "高风险", "见报告" to "见报告"),
                            onSelect = { t18Result = it }
                        )
                    }
                    item {
                        ChoiceChipRow(
                            label = "T13",
                            selected = t13Result,
                            options = listOf("低风险" to "低风险", "高风险" to "高风险", "见报告" to "见报告"),
                            onSelect = { t13Result = it }
                        )
                    }
                    item { ChestnutTextField("性染色体结果", sexChromosome, { sexChromosome = it }, KeyboardType.Text) }
                    item { VoiceLongTextField("结论文本", conclusion, { conclusion = it }, voiceState, onLongTextVoiceStart, onLongTextVoiceStop, minLines = 2, maxLines = 5) }
                }
                "screening_anomaly" -> {
                    item { VoiceLongTextField("结构结论 / 报告描述", structureConclusion, { structureConclusion = it }, voiceState, onLongTextVoiceStart, onLongTextVoiceStop, minLines = 3, maxLines = 8) }
                    item { VoiceLongTextField("结论文本", conclusion, { conclusion = it }, voiceState, onLongTextVoiceStart, onLongTextVoiceStop, minLines = 2, maxLines = 5) }
                }
                "screening_ogtt" -> {
                    item { UnitInputRow("空腹血糖", fastingGlucoseMmolL, { fastingGlucoseMmolL = it }, "mmol/L") }
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            UnitInputRow("1h 血糖", oneHourGlucoseMmolL, { oneHourGlucoseMmolL = it }, "mmol/L", modifier = Modifier.weight(1f))
                            UnitInputRow("2h 血糖", twoHourGlucoseMmolL, { twoHourGlucoseMmolL = it }, "mmol/L", modifier = Modifier.weight(1f))
                        }
                    }
                    item {
                        ChoiceChipRow(
                            label = "报告标注",
                            selected = abnormalFlag,
                            options = listOf("正常" to "正常", "需核对" to "需核对", "见报告" to "见报告"),
                            onSelect = { abnormalFlag = it }
                        )
                    }
                    item { VoiceLongTextField("结论文本", conclusion, { conclusion = it }, voiceState, onLongTextVoiceStart, onLongTextVoiceStop, minLines = 2, maxLines = 5) }
                }
                "screening_gbs" -> {
                    item {
                        ChoiceChipRow(
                            label = "GBS 结果",
                            selected = gbsResult,
                            options = listOf("阴性" to "阴性", "阳性" to "阳性", "见报告" to "见报告"),
                            onSelect = { gbsResult = it }
                        )
                    }
                    item { VoiceLongTextField("结论文本", conclusion, { conclusion = it }, voiceState, onLongTextVoiceStart, onLongTextVoiceStop, minLines = 2, maxLines = 5) }
                }
                "screening_nst" -> {
                    item {
                        ChoiceChipRow(
                            label = "胎心监护结果",
                            selected = nstResult,
                            options = listOf("反应型" to "反应型", "无反应型" to "无反应型", "见报告" to "见报告"),
                            onSelect = { nstResult = it }
                        )
                    }
                    item { VoiceLongTextField("结论文本", conclusion, { conclusion = it }, voiceState, onLongTextVoiceStart, onLongTextVoiceStop, minLines = 2, maxLines = 5) }
                }
            }
            item { VoiceLongTextField(labels.note ?: "备注", note, { note = it }, voiceState, onLongTextVoiceStart, onLongTextVoiceStop, minLines = 2, maxLines = 4) }
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
                                ocrCandidate.values["ntMm"]?.let { ntMm = it }
                                ocrCandidate.values["riskT21"]?.let { riskT21 = it }
                                ocrCandidate.values["riskT18"]?.let { riskT18 = it }
                                ocrCandidate.values["riskOntd"]?.let { riskOntd = it }
                                ocrCandidate.values["riskLevel"]?.let { riskLevel = it }
                                ocrCandidate.values["t21Result"]?.let { t21Result = it }
                                ocrCandidate.values["t18Result"]?.let { t18Result = it }
                                ocrCandidate.values["t13Result"]?.let { t13Result = it }
                                ocrCandidate.values["sexChromosome"]?.let { sexChromosome = it }
                                ocrCandidate.values["structureConclusion"]?.let { structureConclusion = it }
                                ocrCandidate.values["fastingGlucoseMmolL"]?.let { fastingGlucoseMmolL = it }
                                ocrCandidate.values["oneHourGlucoseMmolL"]?.let { oneHourGlucoseMmolL = it }
                                ocrCandidate.values["twoHourGlucoseMmolL"]?.let { twoHourGlucoseMmolL = it }
                                ocrCandidate.values["abnormalFlag"]?.let { abnormalFlag = it }
                                ocrCandidate.values["gbsResult"]?.let { gbsResult = it }
                                ocrCandidate.values["nstResult"]?.let { nstResult = it }
                                ocrCandidate.values["conclusion"]?.let { conclusion = it }
                                ocrCandidate.values["attachmentNote"]?.let { attachmentNote = it }
                                ocrCandidate.values["note"]?.let { note = it }
                                onCandidateApplied()
                            },
                            onDismiss = onCandidateDismiss
                        )
                    }
                    ChestnutTextField("附件备注", attachmentNote, { attachmentNote = it }, KeyboardType.Text)
                }
            }
        } else {
            item {
                when (action.eventType) {
                    "fetal_movement" -> FetalMovementWindowInput(primary, { primary = it })
                    "contraction" -> TimeInputRow(labels.primary, primary, { primary = it })
                    else -> ChestnutTextField(labels.primary, primary, { primary = it }, labels.primaryKeyboard)
                }
            }
            item { ChestnutTextField(labels.secondary, secondary, { secondary = it }, labels.secondaryKeyboard) }
            if (labels.tertiary != null) {
                item { ChestnutTextField(labels.tertiary, tertiary, { tertiary = it }, labels.tertiaryKeyboard) }
            }
            if (labels.note != null) {
                item { VoiceLongTextField(labels.note, note, { note = it }, voiceState, onLongTextVoiceStart, onLongTextVoiceStop) }
            }
        }
    }
}

@Composable
private fun VoiceLongTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    voiceState: SmartVoiceUiState,
    onLongTextVoiceStart: LongTextVoiceStart,
    onLongTextVoiceStop: () -> Unit,
    minLines: Int = 3,
    maxLines: Int = 7
) {
    ChestnutLongTextField(
        label = label,
        value = value,
        onValueChange = onValueChange,
        minLines = minLines,
        maxLines = maxLines,
        voiceState = voiceState,
        onVoiceStart = onLongTextVoiceStart,
        onVoiceStop = onLongTextVoiceStop
    )
}

@Suppress("FunctionNaming")
@Composable
private fun FetalMovementWindowInput(
    value: String,
    onValueChange: (String) -> Unit
) {
    val (start, end) = splitFetalMovementWindow(value)
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        TimeInputRow(
            label = "开始时间",
            value = start,
            onValueChange = { onValueChange(joinFetalMovementWindow(it, end)) },
            modifier = Modifier.weight(1f)
        )
        TimeInputRow(
            label = "结束时间",
            value = end,
            onValueChange = { onValueChange(joinFetalMovementWindow(start, it)) },
            modifier = Modifier.weight(1f)
        )
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
        ChestnutTextField("报告类型", reportType, onReportTypeChange, KeyboardType.Text)
        ChestnutTextField("附件备注", attachmentNote, onAttachmentNoteChange, KeyboardType.Text)
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
        shape = RoundedCornerShape(ChestnutRadius.Control),
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
                Text("未识别到可用字段，可手动填写。", color = ChestnutPalette.Muted, fontSize = 13.sp)
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
                    color = ChestnutPalette.Notice,
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
                    Text("填入表单", color = Color.White)
                }
            }
        }
    }
}

private fun screeningValues(
    eventType: String,
    ntMm: String,
    riskT21: String,
    riskT18: String,
    riskOntd: String,
    riskLevel: String,
    t21Result: String,
    t18Result: String,
    t13Result: String,
    sexChromosome: String,
    structureConclusion: String,
    fastingGlucoseMmolL: String,
    oneHourGlucoseMmolL: String,
    twoHourGlucoseMmolL: String,
    abnormalFlag: String,
    gbsResult: String,
    nstResult: String,
    conclusion: String,
    attachmentNote: String
): Map<String, String> {
    val values = linkedMapOf<String, String>()
    fun put(key: String, value: String) {
        if (value.isNotBlank()) values[key] = value
    }
    when (eventType) {
        "screening_nt" -> {
            put("ntMm", ntMm)
            put("conclusion", conclusion)
        }
        "screening_serum" -> {
            put("riskT21", riskT21)
            put("riskT18", riskT18)
            put("riskOntd", riskOntd)
            put("riskLevel", riskLevel)
            put("conclusion", conclusion)
        }
        "screening_nipt" -> {
            put("t21Result", t21Result)
            put("t18Result", t18Result)
            put("t13Result", t13Result)
            put("sexChromosome", sexChromosome)
            put("conclusion", conclusion)
        }
        "screening_anomaly" -> {
            put("structureConclusion", structureConclusion)
            put("conclusion", conclusion)
        }
        "screening_ogtt" -> {
            put("fastingGlucoseMmolL", fastingGlucoseMmolL)
            put("oneHourGlucoseMmolL", oneHourGlucoseMmolL)
            put("twoHourGlucoseMmolL", twoHourGlucoseMmolL)
            put("abnormalFlag", abnormalFlag)
            put("conclusion", conclusion)
        }
        "screening_gbs" -> {
            put("gbsResult", gbsResult)
            put("conclusion", conclusion)
        }
        "screening_nst" -> {
            put("nstResult", nstResult)
            put("conclusion", conclusion)
        }
    }
    put("attachmentNote", attachmentNote)
    return values
}

@Suppress("MagicNumber")
private fun splitFetalMovementWindow(value: String): Pair<String, String> {
    val parts = value.split("-", limit = 2).map { it.trim() }
    return if (parts.size == 2) {
        parts[0] to parts[1]
    } else {
        value to ""
    }
}

private fun joinFetalMovementWindow(start: String, end: String): String {
    val cleanStart = start.trim()
    val cleanEnd = end.trim()
    return when {
        cleanStart.isBlank() -> cleanEnd
        cleanEnd.isBlank() -> cleanStart
        else -> "$cleanStart-$cleanEnd"
    }
}
