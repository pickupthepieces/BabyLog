package app.babylog.nativeapp

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.io.File

@Composable
internal fun UltrasoundFormScreen(
    defaultGestationalAge: String,
    expectedDueDate: String,
    draft: SmartEntryDraft?,
    isEditing: Boolean = false,
    photoPath: String?,
    photoName: String?,
    ocrRunning: Boolean,
    ocrCandidate: BabyLogSmartInput.UltrasoundOcrCandidate?,
    onPickPhoto: () -> Unit,
    onCapturePhoto: () -> Unit,
    onRecognizePhoto: () -> Unit,
    onCandidateDismiss: () -> Unit,
    onCandidateApplied: () -> Unit,
    onBack: () -> Unit,
    onSave: (BabyLogService.UltrasoundInput) -> Unit
) {
    val values = draft?.values.orEmpty()
    var examDate by rememberSaveable(draft?.nonce) {
        mutableStateOf(values["examDate"]?.takeIf { BabyLogFormatters.isValidDateInput(it) } ?: BabyLogFormatters.todayDateInput())
    }
    var gestationalAge by rememberSaveable(defaultGestationalAge, draft?.nonce) {
        mutableStateOf(values["gestationalAge"] ?: defaultGestationalAge)
    }
    var gestationalAgeEdited by rememberSaveable(draft?.nonce) {
        mutableStateOf(!values["gestationalAge"].isNullOrBlank())
    }
    var hospital by rememberSaveable(draft?.nonce) { mutableStateOf(values["hospital"].orEmpty()) }
    var reportTime by rememberSaveable(draft?.nonce) { mutableStateOf(values["reportTime"].orEmpty()) }
    var diagnosisText by rememberSaveable(draft?.nonce) { mutableStateOf(values["diagnosisText"].orEmpty()) }
    var bpd by rememberSaveable(draft?.nonce) { mutableStateOf(values["bpdMm"].orEmpty()) }
    var hc by rememberSaveable(draft?.nonce) { mutableStateOf(values["hcMm"].orEmpty()) }
    var ac by rememberSaveable(draft?.nonce) { mutableStateOf(values["acMm"].orEmpty()) }
    var fl by rememberSaveable(draft?.nonce) { mutableStateOf(values["flMm"].orEmpty()) }
    var efw by rememberSaveable(draft?.nonce) { mutableStateOf(values["efwGram"].orEmpty()) }
    var afi by rememberSaveable(draft?.nonce) { mutableStateOf(values["afiCm"].orEmpty()) }
    var deepestPocket by rememberSaveable(draft?.nonce) { mutableStateOf(values["deepestPocketCm"].orEmpty()) }
    var placentaLocation by rememberSaveable(draft?.nonce) { mutableStateOf(values["placentaLocation"].orEmpty()) }
    var placentaGrade by rememberSaveable(draft?.nonce) { mutableStateOf(values["placentaGrade"].orEmpty()) }
    var fetalPresentation by rememberSaveable(draft?.nonce) { mutableStateOf(values["fetalPresentation"].orEmpty()) }
    var fetalHeartRate by rememberSaveable(draft?.nonce) { mutableStateOf(values["fetalHeartRateBpm"].orEmpty()) }
    var fetalCount by rememberSaveable(draft?.nonce) { mutableStateOf(values["fetalCount"].orEmpty()) }
    var fetalMovement by rememberSaveable(draft?.nonce) { mutableStateOf(values["fetalMovement"].orEmpty()) }
    var umbilicalInsertion by rememberSaveable(draft?.nonce) { mutableStateOf(values["umbilicalInsertion"].orEmpty()) }
    var cervicalLength by rememberSaveable(draft?.nonce) { mutableStateOf(values["cervicalLengthMm"].orEmpty()) }
    var crl by rememberSaveable(draft?.nonce) { mutableStateOf(values["crlMm"].orEmpty()) }
    var nt by rememberSaveable(draft?.nonce) { mutableStateOf(values["ntMm"].orEmpty()) }
    var umbilicalSd by rememberSaveable(draft?.nonce) { mutableStateOf(values["umbilicalSd"].orEmpty()) }
    var umbilicalPi by rememberSaveable(draft?.nonce) { mutableStateOf(values["umbilicalPi"].orEmpty()) }
    var umbilicalRi by rememberSaveable(draft?.nonce) { mutableStateOf(values["umbilicalRi"].orEmpty()) }
    var showAdvanced by rememberSaveable(draft?.nonce) {
        mutableStateOf(hasAdvancedUltrasoundDraft(values))
    }
    var saveError by rememberSaveable { mutableStateOf("") }

    val warnings = remember(gestationalAge, bpd, hc, ac, fl, efw) {
        BabyLogFormatters.formatUltrasoundSoftRangeWarnings(
            BabyLogFormatters.parseGestationalAgeDays(gestationalAge),
            BabyLogFormatters.parseOptionalNumber(bpd),
            BabyLogFormatters.parseOptionalNumber(hc),
            BabyLogFormatters.parseOptionalNumber(ac),
            BabyLogFormatters.parseOptionalNumber(fl),
            BabyLogFormatters.parseOptionalNumber(efw)
        )
    }
    val estimatedEfw = remember(bpd, ac, fl, efw) {
        if (efw.trim().isNotEmpty()) {
            null
        } else {
            val parsedBpd = BabyLogFormatters.parseOptionalNumber(bpd)
            val parsedAc = BabyLogFormatters.parseOptionalNumber(ac)
            val parsedFl = BabyLogFormatters.parseOptionalNumber(fl)
            if (parsedBpd != null && parsedAc != null && parsedFl != null) {
                BabyLogFetalGrowthReference.estimateEfwHadlock3Gram(parsedBpd, parsedAc, parsedFl)
            } else {
                null
            }
        }
    }
    LaunchedEffect(examDate, expectedDueDate) {
        if (!gestationalAgeEdited) {
            val inferred = gestationalAgeInputForDate(expectedDueDate, examDate)
            if (inferred.isNotBlank()) {
                gestationalAge = inferred
            }
        }
    }

    RecordFormScaffold(
        title = if (isEditing) "编辑 B 超记录" else "B 超记录",
        subtitle = "先录生长指标，其他医学信息可展开补充",
        saveText = if (isEditing) "保存修改" else "保存生长指标",
        onBack = onBack,
        onSave = {
            val input = BabyLogService.UltrasoundInput(
                examDate,
                gestationalAge,
                hospital,
                reportTime,
                diagnosisText,
                bpd,
                hc,
                ac,
                fl,
                efw,
                afi,
                deepestPocket,
                placentaLocation,
                placentaGrade,
                fetalPresentation,
                fetalHeartRate,
                fetalCount,
                fetalMovement,
                umbilicalInsertion,
                cervicalLength,
                crl,
                nt,
                umbilicalSd,
                umbilicalPi,
                umbilicalRi,
                photoPath,
                photoName
            )
            if (!BabyLogService.hasUltrasoundMinimumContent(input)) {
                saveError = "请先选择 B 超单图片，或填写 BPD/HC/AC/FL/EFW 任一生长指标。"
            } else {
                saveError = ""
                onSave(input)
            }
        }
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("B 超单照片", color = ChestnutPalette.Ink, fontWeight = FontWeight.Bold)
                Text("可拍照/选图识别，或手动填写下方指标", color = ChestnutPalette.Muted, fontSize = 12.sp)
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = onCapturePhoto, border = BorderStroke(1.dp, ChestnutPalette.Primary)) {
                    Text("拍照", color = ChestnutPalette.Primary)
                }
                OutlinedButton(onClick = onPickPhoto, border = BorderStroke(1.dp, ChestnutPalette.Primary)) {
                    Text("选图", color = ChestnutPalette.Primary)
                }
                Button(
                    enabled = photoPath != null && !ocrRunning,
                    onClick = onRecognizePhoto,
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = if (photoPath != null) ChestnutPalette.Primary else ChestnutPalette.Surface2,
                        disabledBackgroundColor = ChestnutPalette.Surface2
                    )
                ) {
                    Text(
                        if (ocrRunning) "识别中..." else if (photoPath == null) "先选图" else "识别",
                        color = if (photoPath != null && !ocrRunning) Color.White else ChestnutPalette.Text3
                    )
                }
            }
        }
        if (photoPath != null) {
            item {
                Text(
                    "已选择：${photoName ?: File(photoPath).name}",
                    color = ChestnutPalette.Primary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
        }
        if (saveError.isNotBlank()) {
            item { InlineWarning(saveError) }
        }
        if (ocrCandidate != null) {
            item {
                UltrasoundOcrCandidateCard(
                    candidate = ocrCandidate,
                    onApply = {
                        ocrCandidate.examDate.value?.takeIf { BabyLogFormatters.isValidDateInput(it) }?.let {
                            examDate = it
                            if (!gestationalAgeEdited) {
                                gestationalAgeInputForDate(expectedDueDate, it).takeIf { value -> value.isNotBlank() }?.let { value ->
                                    gestationalAge = value
                                }
                            }
                        }
                        ocrCandidate.hospital.value?.let { hospital = it; showAdvanced = true }
                        ocrCandidate.reportTime.value?.let { reportTime = it; showAdvanced = true }
                        ocrCandidate.diagnosisText.value?.let { diagnosisText = it; showAdvanced = true }
                        ocrCandidate.bpdMm.value?.let { bpd = BabyLogFormatters.formatNumber(it) }
                        ocrCandidate.hcMm.value?.let { hc = BabyLogFormatters.formatNumber(it) }
                        ocrCandidate.acMm.value?.let { ac = BabyLogFormatters.formatNumber(it) }
                        ocrCandidate.flMm.value?.let { fl = BabyLogFormatters.formatNumber(it) }
                        ocrCandidate.efwGram.value?.let { efw = BabyLogFormatters.formatNumber(it) }
                        ocrCandidate.afiCm.value?.let { afi = BabyLogFormatters.formatNumber(it); showAdvanced = true }
                        ocrCandidate.deepestPocketCm.value?.let { deepestPocket = BabyLogFormatters.formatNumber(it); showAdvanced = true }
                        ocrCandidate.placentaLocation.value?.let { placentaLocation = it; showAdvanced = true }
                        ocrCandidate.placentaGrade.value?.let { placentaGrade = it; showAdvanced = true }
                        ocrCandidate.fetalPresentation.value?.let { fetalPresentation = it; showAdvanced = true }
                        ocrCandidate.fetalHeartRateBpm.value?.let { fetalHeartRate = BabyLogFormatters.formatNumber(it); showAdvanced = true }
                        ocrCandidate.fetalCount.value?.let { fetalCount = it; showAdvanced = true }
                        ocrCandidate.fetalMovement.value?.let { fetalMovement = it; showAdvanced = true }
                        ocrCandidate.umbilicalInsertion.value?.let { umbilicalInsertion = it; showAdvanced = true }
                        ocrCandidate.cervicalLengthMm.value?.let { cervicalLength = BabyLogFormatters.formatNumber(it); showAdvanced = true }
                        ocrCandidate.crlMm.value?.let { crl = BabyLogFormatters.formatNumber(it); showAdvanced = true }
                        ocrCandidate.ntMm.value?.let { nt = BabyLogFormatters.formatNumber(it); showAdvanced = true }
                        ocrCandidate.umbilicalSd.value?.let { umbilicalSd = BabyLogFormatters.formatNumber(it); showAdvanced = true }
                        ocrCandidate.umbilicalPi.value?.let { umbilicalPi = BabyLogFormatters.formatNumber(it); showAdvanced = true }
                        ocrCandidate.umbilicalRi.value?.let { umbilicalRi = BabyLogFormatters.formatNumber(it); showAdvanced = true }
                        onCandidateApplied()
                    },
                    onDismiss = onCandidateDismiss
                )
            }
        }
        item { DateInputRow("检查日期", examDate, { examDate = it }, allowClear = false) }
        item {
            ChestnutTextField(
                "孕周，例如 28+3",
                gestationalAge,
                {
                    gestationalAgeEdited = true
                    gestationalAge = it
                },
                KeyboardType.Text
            )
        }
        item { Text("胎儿生长指标", color = ChestnutPalette.Ink, fontWeight = FontWeight.Bold) }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                UnitInputRow("BPD", bpd, { bpd = it }, "mm", Modifier.weight(1f))
                UnitInputRow("HC", hc, { hc = it }, "mm", Modifier.weight(1f))
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                UnitInputRow("AC", ac, { ac = it }, "mm", Modifier.weight(1f))
                UnitInputRow("FL", fl, { fl = it }, "mm", Modifier.weight(1f))
            }
        }
        item { UnitInputRow("EFW", efw, { efw = it }, "g") }
        if (estimatedEfw != null) {
            item {
                Text(
                    "EFW 留空保存时，会按 Hadlock 3 估算为 ${BabyLogFormatters.formatNumber(estimatedEfw)} g。",
                    color = ChestnutPalette.Muted,
                    fontSize = 12.sp
                )
            }
        }
        item {
            OutlinedButton(
                onClick = { showAdvanced = !showAdvanced },
                border = BorderStroke(1.dp, ChestnutPalette.Border),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    if (showAdvanced) "收起羊水 / 胎盘 / 脐血流" else "填写更多医学信息（可选）",
                    color = ChestnutPalette.Muted
                )
            }
        }
        if (showAdvanced) {
            item { Text("公共信息", color = ChestnutPalette.Ink, fontWeight = FontWeight.Bold) }
            item { ChestnutTextField("医院 / 机构", hospital, { hospital = it }, KeyboardType.Text) }
            item { ChestnutTextField("报告时间", reportTime, { reportTime = it }, KeyboardType.Text) }
            item { ChestnutLongTextField("超声诊断 / 提示", diagnosisText, { diagnosisText = it }, minLines = 2, maxLines = 4) }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    UnitInputRow("胎心率", fetalHeartRate, { fetalHeartRate = it }, "bpm", Modifier.weight(1f))
                    UnitInputRow("CRL", crl, { crl = it }, "mm", Modifier.weight(1f))
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    UnitInputRow("NT", nt, { nt = it }, "mm", Modifier.weight(1f))
                    UnitInputRow("宫颈管", cervicalLength, { cervicalLength = it }, "mm", Modifier.weight(1f))
                }
            }
            item {
                ChoiceChipRow(
                    label = "胎儿个数",
                    selected = fetalCount,
                    options = listOf("单胎" to "单胎", "双胎" to "双胎", "多胎" to "多胎", "未写" to "未写"),
                    onSelect = { fetalCount = it }
                )
            }
            item {
                ChoiceChipRow(
                    label = "胎动",
                    selected = fetalMovement,
                    options = listOf("有" to "有", "可见" to "可见", "无" to "无", "未写" to "未写"),
                    onSelect = { fetalMovement = it }
                )
            }
            item { ChestnutTextField("脐带插入处", umbilicalInsertion, { umbilicalInsertion = it }, KeyboardType.Text) }
            item { Text("羊水 / 胎盘 / 胎位", color = ChestnutPalette.Ink, fontWeight = FontWeight.Bold) }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    UnitInputRow("AFI", afi, { afi = it }, "cm", Modifier.weight(1f))
                    UnitInputRow("最大羊水池", deepestPocket, { deepestPocket = it }, "cm", Modifier.weight(1f))
                }
            }
            item {
                ChoiceChipRow(
                    label = "胎盘位置",
                    selected = placentaLocation,
                    options = listOf("前壁" to "前壁", "后壁" to "后壁", "侧壁" to "侧壁", "低置" to "低置", "前置" to "前置", "其他" to "其他"),
                    onSelect = { placentaLocation = it }
                )
            }
            item {
                ChoiceChipRow(
                    label = "胎盘成熟度",
                    selected = placentaGrade,
                    options = listOf("0级" to "0 级", "I 级" to "I 级", "II 级" to "II 级", "III 级" to "III 级", "未写" to "未写"),
                    onSelect = { placentaGrade = it }
                )
            }
            item {
                ChoiceChipRow(
                    label = "胎位",
                    selected = fetalPresentation,
                    options = listOf("头位" to "头位", "臀位" to "臀位", "横位" to "横位", "不详" to "不详"),
                    onSelect = { fetalPresentation = it }
                )
            }
            item { Text("脐动脉血流", color = ChestnutPalette.Ink, fontWeight = FontWeight.Bold) }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    UnitInputRow("S/D", umbilicalSd, { umbilicalSd = it }, "", Modifier.weight(1f))
                    UnitInputRow("PI", umbilicalPi, { umbilicalPi = it }, "", Modifier.weight(1f))
                }
            }
            item { UnitInputRow("RI", umbilicalRi, { umbilicalRi = it }, "") }
        }
        if (warnings.isNotBlank()) {
            item { InlineWarning(warnings) }
        }
    }
}

@Composable
private fun InlineWarning(text: String) {
    Text(
        text,
        color = Color(0xFF7C4A21),
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFFFEBCB))
            .padding(12.dp)
    )
}

@Composable
private fun UltrasoundOcrCandidateCard(
    candidate: BabyLogSmartInput.UltrasoundOcrCandidate,
    onApply: () -> Unit,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(ChestnutPalette.PrimarySoft)
            .border(1.dp, ChestnutPalette.Primary.copy(alpha = 0.45f), RoundedCornerShape(14.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("识别候选（生长指标 + 公共信息）", color = ChestnutPalette.Ink, fontWeight = FontWeight.Bold)
        Text("孕周由预产期和检查日期自动推算，也可手动改", color = ChestnutPalette.Muted, fontSize = 12.sp)
        val rows = ultrasoundCandidateRows(candidate)
        if (rows.isEmpty()) {
            Text("模型没有返回可用字段，请手动录入。", color = ChestnutPalette.Muted, fontSize = 13.sp)
        } else {
            rows.take(14).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(row.first, color = ChestnutPalette.Muted, fontSize = 12.sp, modifier = Modifier.width(90.dp))
                    Text(row.second, color = ChestnutPalette.Ink, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
            if (rows.size > 14) {
                Text("另有 ${rows.size - 14} 项候选，应用后进入表单核对。", color = ChestnutPalette.Muted, fontSize = 12.sp)
            }
        }
        if (candidate.warnings.isNotEmpty()) {
            Text("需核对：" + candidate.warnings.joinToString("；"), color = Color(0xFF7C4A21), fontSize = 12.sp)
        }
        if (!candidate.rawText.isNullOrBlank()) {
            Text("识别原文片段", color = ChestnutPalette.Muted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text(
                candidate.rawText.take(220),
                color = ChestnutPalette.Muted,
                fontSize = 12.sp,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(onClick = onDismiss, border = BorderStroke(1.dp, ChestnutPalette.Border)) {
                Text("忽略", color = ChestnutPalette.Muted)
            }
            Button(
                enabled = rows.isNotEmpty(),
                onClick = onApply,
                colors = ButtonDefaults.buttonColors(backgroundColor = ChestnutPalette.Primary)
            ) {
                Text("应用到表单", color = Color.White)
            }
        }
    }
}
