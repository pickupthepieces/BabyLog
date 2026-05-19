package app.babylog.nativeapp

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
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
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.mapSaver
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
    val formState = rememberSaveable(draft?.nonce, defaultGestationalAge, saver = UltrasoundFormState.Saver) {
        UltrasoundFormState.fromValues(values, defaultGestationalAge)
    }
    LaunchedEffect(formState, expectedDueDate) {
        snapshotFlow { formState.examDate }
            .collect { selectedDate ->
                if (!formState.gestationalAgeEdited) {
                    val inferred = gestationalAgeInputForDate(expectedDueDate, selectedDate)
                    if (inferred.isNotBlank()) {
                        formState.gestationalAge = inferred
                    }
                }
            }
    }

    RecordFormScaffold(
        title = if (isEditing) "编辑 B 超记录" else "B 超记录",
        subtitle = "先录生长指标，其他医学信息可展开补充",
        saveText = if (isEditing) "保存修改" else "保存生长指标",
        onBack = onBack,
        onSave = {
            val input = formState.toInput(photoPath, photoName)
            if (!BabyLogService.hasUltrasoundMinimumContent(input)) {
                formState.saveError = "请先选择 B 超单图片，或填写 BPD/HC/AC/FL/EFW 任一生长指标。"
            } else {
                formState.saveError = ""
                onSave(input)
            }
        }
    ) {
        item(key = "photo_header") {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("B 超单照片", color = ChestnutPalette.Ink, fontWeight = FontWeight.Bold)
                Text("可拍照/选图识别，或手动填写下方指标", color = ChestnutPalette.Muted, fontSize = 12.sp)
            }
        }
        item(key = "photo_actions") {
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
            item(key = "photo_selected") {
                Text(
                    "已选择：${photoName ?: File(photoPath).name}",
                    color = ChestnutPalette.Primary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
        }
        if (formState.saveError.isNotBlank()) {
            item(key = "save_error") { InlineWarning(formState.saveError) }
        }
        if (ocrCandidate != null) {
            item(key = "ocr_candidate") {
                UltrasoundOcrCandidateCard(
                    candidate = ocrCandidate,
                    onApply = {
                        ocrCandidate.examDate.value?.takeIf { BabyLogFormatters.isValidDateInput(it) }?.let {
                            formState.examDate = it
                            if (!formState.gestationalAgeEdited) {
                                gestationalAgeInputForDate(expectedDueDate, it).takeIf { value -> value.isNotBlank() }?.let { value ->
                                    formState.gestationalAge = value
                                }
                            }
                        }
                        ocrCandidate.hospital.value?.let { formState.hospital = it; formState.showAdvanced = true }
                        ocrCandidate.reportTime.value?.let { formState.reportTime = it; formState.showAdvanced = true }
                        ocrCandidate.diagnosisText.value?.let { formState.diagnosisText = it; formState.showAdvanced = true }
                        ocrCandidate.bpdMm.value?.let { formState.bpd = BabyLogFormatters.formatNumber(it) }
                        ocrCandidate.hcMm.value?.let { formState.hc = BabyLogFormatters.formatNumber(it) }
                        ocrCandidate.acMm.value?.let { formState.ac = BabyLogFormatters.formatNumber(it) }
                        ocrCandidate.flMm.value?.let { formState.fl = BabyLogFormatters.formatNumber(it) }
                        ocrCandidate.efwGram.value?.let { formState.efw = BabyLogFormatters.formatNumber(it) }
                        ocrCandidate.afiCm.value?.let { formState.afi = BabyLogFormatters.formatNumber(it); formState.showAdvanced = true }
                        ocrCandidate.deepestPocketCm.value?.let { formState.deepestPocket = BabyLogFormatters.formatNumber(it); formState.showAdvanced = true }
                        ocrCandidate.placentaLocation.value?.let { formState.placentaLocation = it; formState.showAdvanced = true }
                        ocrCandidate.placentaGrade.value?.let { formState.placentaGrade = it; formState.showAdvanced = true }
                        ocrCandidate.fetalPresentation.value?.let { formState.fetalPresentation = it; formState.showAdvanced = true }
                        ocrCandidate.fetalHeartRateBpm.value?.let { formState.fetalHeartRate = BabyLogFormatters.formatNumber(it); formState.showAdvanced = true }
                        ocrCandidate.fetalCount.value?.let { formState.fetalCount = it; formState.showAdvanced = true }
                        ocrCandidate.fetalMovement.value?.let { formState.fetalMovement = it; formState.showAdvanced = true }
                        ocrCandidate.umbilicalInsertion.value?.let { formState.umbilicalInsertion = it; formState.showAdvanced = true }
                        ocrCandidate.cervicalLengthMm.value?.let { formState.cervicalLength = BabyLogFormatters.formatNumber(it); formState.showAdvanced = true }
                        ocrCandidate.crlMm.value?.let { formState.crl = BabyLogFormatters.formatNumber(it); formState.showAdvanced = true }
                        ocrCandidate.ntMm.value?.let { formState.nt = BabyLogFormatters.formatNumber(it); formState.showAdvanced = true }
                        ocrCandidate.umbilicalSd.value?.let { formState.umbilicalSd = BabyLogFormatters.formatNumber(it); formState.showAdvanced = true }
                        ocrCandidate.umbilicalPi.value?.let { formState.umbilicalPi = BabyLogFormatters.formatNumber(it); formState.showAdvanced = true }
                        ocrCandidate.umbilicalRi.value?.let { formState.umbilicalRi = BabyLogFormatters.formatNumber(it); formState.showAdvanced = true }
                        onCandidateApplied()
                    },
                    onDismiss = onCandidateDismiss
                )
            }
        }
        item(key = "exam_date") {
            DateInputRow("检查日期", formState.examDate, { formState.examDate = it }, allowClear = false)
        }
        item(key = "gestational_age") {
            ChestnutTextField(
                "孕周，例如 28+3",
                formState.gestationalAge,
                {
                    formState.gestationalAgeEdited = true
                    formState.gestationalAge = it
                },
                KeyboardType.Text
            )
        }
        item(key = "growth_header") { Text("胎儿生长指标", color = ChestnutPalette.Ink, fontWeight = FontWeight.Bold) }
        item(key = "bpd_hc") {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                UltrasoundBpdInput(formState)
                UltrasoundHcInput(formState)
            }
        }
        item(key = "ac_fl") {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                UltrasoundAcInput(formState)
                UltrasoundFlInput(formState)
            }
        }
        item(key = "efw") { UltrasoundEfwInput(formState) }
        item(key = "advanced_toggle") {
            OutlinedButton(
                onClick = { formState.showAdvanced = !formState.showAdvanced },
                border = BorderStroke(1.dp, ChestnutPalette.Border),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    if (formState.showAdvanced) "收起羊水 / 胎盘 / 脐血流" else "填写更多医学信息（可选）",
                    color = ChestnutPalette.Muted
                )
            }
        }
        if (formState.showAdvanced) {
            item(key = "common_header") { Text("公共信息", color = ChestnutPalette.Ink, fontWeight = FontWeight.Bold) }
            item(key = "hospital") { ChestnutTextField("医院 / 机构", formState.hospital, { formState.hospital = it }, KeyboardType.Text) }
            item(key = "report_time") { ChestnutTextField("报告时间", formState.reportTime, { formState.reportTime = it }, KeyboardType.Text) }
            item(key = "diagnosis_text") { ChestnutLongTextField("超声诊断 / 提示", formState.diagnosisText, { formState.diagnosisText = it }, minLines = 2, maxLines = 4) }
            item(key = "fetal_heart_crl") {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    UltrasoundFetalHeartRateInput(formState)
                    UltrasoundCrlInput(formState)
                }
            }
            item(key = "nt_cervical_length") {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    UltrasoundNtInput(formState)
                    UltrasoundCervicalLengthInput(formState)
                }
            }
            item(key = "fetal_count") {
                ChoiceChipRow(
                    label = "胎儿个数",
                    selected = formState.fetalCount,
                    options = listOf("单胎" to "单胎", "双胎" to "双胎", "多胎" to "多胎", "未写" to "未写"),
                    onSelect = { formState.fetalCount = it }
                )
            }
            item(key = "fetal_movement") {
                ChoiceChipRow(
                    label = "胎动",
                    selected = formState.fetalMovement,
                    options = listOf("有" to "有", "可见" to "可见", "无" to "无", "未写" to "未写"),
                    onSelect = { formState.fetalMovement = it }
                )
            }
            item(key = "umbilical_insertion") {
                ChestnutTextField("脐带插入处", formState.umbilicalInsertion, { formState.umbilicalInsertion = it }, KeyboardType.Text)
            }
            item(key = "amniotic_placenta_header") { Text("羊水 / 胎盘 / 胎位", color = ChestnutPalette.Ink, fontWeight = FontWeight.Bold) }
            item(key = "afi_deepest_pocket") {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    UltrasoundAfiInput(formState)
                    UltrasoundDeepestPocketInput(formState)
                }
            }
            item(key = "placenta_location") {
                ChoiceChipRow(
                    label = "胎盘位置",
                    selected = formState.placentaLocation,
                    options = listOf("前壁" to "前壁", "后壁" to "后壁", "侧壁" to "侧壁", "低置" to "低置", "前置" to "前置", "其他" to "其他"),
                    onSelect = { formState.placentaLocation = it }
                )
            }
            item(key = "placenta_grade") {
                ChoiceChipRow(
                    label = "胎盘成熟度",
                    selected = formState.placentaGrade,
                    options = listOf("0级" to "0 级", "I 级" to "I 级", "II 级" to "II 级", "III 级" to "III 级", "未写" to "未写"),
                    onSelect = { formState.placentaGrade = it }
                )
            }
            item(key = "fetal_presentation") {
                ChoiceChipRow(
                    label = "胎位",
                    selected = formState.fetalPresentation,
                    options = listOf("头位" to "头位", "臀位" to "臀位", "横位" to "横位", "不详" to "不详"),
                    onSelect = { formState.fetalPresentation = it }
                )
            }
            item(key = "umbilical_flow_header") { Text("脐动脉血流", color = ChestnutPalette.Ink, fontWeight = FontWeight.Bold) }
            item(key = "umbilical_sd_pi") {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    UltrasoundSdInput(formState)
                    UltrasoundPiInput(formState)
                }
            }
            item(key = "umbilical_ri") { UnitInputRow("RI", formState.umbilicalRi, { formState.umbilicalRi = it }, "") }
        }
        item(key = "soft_warnings") { UltrasoundSoftWarnings(formState) }
    }
}

@Stable
private class UltrasoundFormState(
    examDate: String,
    gestationalAge: String,
    gestationalAgeEdited: Boolean,
    hospital: String,
    reportTime: String,
    diagnosisText: String,
    bpd: String,
    hc: String,
    ac: String,
    fl: String,
    efw: String,
    afi: String,
    deepestPocket: String,
    placentaLocation: String,
    placentaGrade: String,
    fetalPresentation: String,
    fetalHeartRate: String,
    fetalCount: String,
    fetalMovement: String,
    umbilicalInsertion: String,
    cervicalLength: String,
    crl: String,
    nt: String,
    umbilicalSd: String,
    umbilicalPi: String,
    umbilicalRi: String,
    showAdvanced: Boolean,
    saveError: String
) {
    var examDate by mutableStateOf(examDate)
    var gestationalAge by mutableStateOf(gestationalAge)
    var gestationalAgeEdited by mutableStateOf(gestationalAgeEdited)
    var hospital by mutableStateOf(hospital)
    var reportTime by mutableStateOf(reportTime)
    var diagnosisText by mutableStateOf(diagnosisText)
    var bpd by mutableStateOf(bpd)
    var hc by mutableStateOf(hc)
    var ac by mutableStateOf(ac)
    var fl by mutableStateOf(fl)
    var efw by mutableStateOf(efw)
    var afi by mutableStateOf(afi)
    var deepestPocket by mutableStateOf(deepestPocket)
    var placentaLocation by mutableStateOf(placentaLocation)
    var placentaGrade by mutableStateOf(placentaGrade)
    var fetalPresentation by mutableStateOf(fetalPresentation)
    var fetalHeartRate by mutableStateOf(fetalHeartRate)
    var fetalCount by mutableStateOf(fetalCount)
    var fetalMovement by mutableStateOf(fetalMovement)
    var umbilicalInsertion by mutableStateOf(umbilicalInsertion)
    var cervicalLength by mutableStateOf(cervicalLength)
    var crl by mutableStateOf(crl)
    var nt by mutableStateOf(nt)
    var umbilicalSd by mutableStateOf(umbilicalSd)
    var umbilicalPi by mutableStateOf(umbilicalPi)
    var umbilicalRi by mutableStateOf(umbilicalRi)
    var showAdvanced by mutableStateOf(showAdvanced)
    var saveError by mutableStateOf(saveError)

    fun toInput(photoPath: String?, photoName: String?): BabyLogService.UltrasoundInput {
        return BabyLogService.UltrasoundInput(
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
    }

    fun estimatedEfwGram(): Double? {
        if (efw.trim().isNotEmpty()) return null
        val parsedBpd = BabyLogFormatters.parseOptionalNumber(bpd)
        val parsedAc = BabyLogFormatters.parseOptionalNumber(ac)
        val parsedFl = BabyLogFormatters.parseOptionalNumber(fl)
        return if (parsedBpd != null && parsedAc != null && parsedFl != null) {
            BabyLogFetalGrowthReference.estimateEfwHadlock3Gram(parsedBpd, parsedAc, parsedFl)
        } else {
            null
        }
    }

    fun softWarnings(): String {
        return BabyLogFormatters.formatUltrasoundSoftRangeWarnings(
            BabyLogFormatters.parseGestationalAgeDays(gestationalAge),
            BabyLogFormatters.parseOptionalNumber(bpd),
            BabyLogFormatters.parseOptionalNumber(hc),
            BabyLogFormatters.parseOptionalNumber(ac),
            BabyLogFormatters.parseOptionalNumber(fl),
            BabyLogFormatters.parseOptionalNumber(efw)
        )
    }

    companion object {
        val Saver = mapSaver(
            save = { state ->
                mapOf(
                    "examDate" to state.examDate,
                    "gestationalAge" to state.gestationalAge,
                    "gestationalAgeEdited" to state.gestationalAgeEdited,
                    "hospital" to state.hospital,
                    "reportTime" to state.reportTime,
                    "diagnosisText" to state.diagnosisText,
                    "bpd" to state.bpd,
                    "hc" to state.hc,
                    "ac" to state.ac,
                    "fl" to state.fl,
                    "efw" to state.efw,
                    "afi" to state.afi,
                    "deepestPocket" to state.deepestPocket,
                    "placentaLocation" to state.placentaLocation,
                    "placentaGrade" to state.placentaGrade,
                    "fetalPresentation" to state.fetalPresentation,
                    "fetalHeartRate" to state.fetalHeartRate,
                    "fetalCount" to state.fetalCount,
                    "fetalMovement" to state.fetalMovement,
                    "umbilicalInsertion" to state.umbilicalInsertion,
                    "cervicalLength" to state.cervicalLength,
                    "crl" to state.crl,
                    "nt" to state.nt,
                    "umbilicalSd" to state.umbilicalSd,
                    "umbilicalPi" to state.umbilicalPi,
                    "umbilicalRi" to state.umbilicalRi,
                    "showAdvanced" to state.showAdvanced,
                    "saveError" to state.saveError
                )
            },
            restore = { restored ->
                UltrasoundFormState(
                    restored.stringValue("examDate", BabyLogFormatters.todayDateInput()),
                    restored.stringValue("gestationalAge"),
                    restored.booleanValue("gestationalAgeEdited"),
                    restored.stringValue("hospital"),
                    restored.stringValue("reportTime"),
                    restored.stringValue("diagnosisText"),
                    restored.stringValue("bpd"),
                    restored.stringValue("hc"),
                    restored.stringValue("ac"),
                    restored.stringValue("fl"),
                    restored.stringValue("efw"),
                    restored.stringValue("afi"),
                    restored.stringValue("deepestPocket"),
                    restored.stringValue("placentaLocation"),
                    restored.stringValue("placentaGrade"),
                    restored.stringValue("fetalPresentation"),
                    restored.stringValue("fetalHeartRate"),
                    restored.stringValue("fetalCount"),
                    restored.stringValue("fetalMovement"),
                    restored.stringValue("umbilicalInsertion"),
                    restored.stringValue("cervicalLength"),
                    restored.stringValue("crl"),
                    restored.stringValue("nt"),
                    restored.stringValue("umbilicalSd"),
                    restored.stringValue("umbilicalPi"),
                    restored.stringValue("umbilicalRi"),
                    restored.booleanValue("showAdvanced"),
                    restored.stringValue("saveError")
                )
            }
        )

        fun fromValues(values: Map<String, String>, defaultGestationalAge: String): UltrasoundFormState {
            return UltrasoundFormState(
                values["examDate"]?.takeIf { BabyLogFormatters.isValidDateInput(it) } ?: BabyLogFormatters.todayDateInput(),
                values["gestationalAge"] ?: defaultGestationalAge,
                !values["gestationalAge"].isNullOrBlank(),
                values["hospital"].orEmpty(),
                values["reportTime"].orEmpty(),
                values["diagnosisText"].orEmpty(),
                values["bpdMm"].orEmpty(),
                values["hcMm"].orEmpty(),
                values["acMm"].orEmpty(),
                values["flMm"].orEmpty(),
                values["efwGram"].orEmpty(),
                values["afiCm"].orEmpty(),
                values["deepestPocketCm"].orEmpty(),
                values["placentaLocation"].orEmpty(),
                values["placentaGrade"].orEmpty(),
                values["fetalPresentation"].orEmpty(),
                values["fetalHeartRateBpm"].orEmpty(),
                values["fetalCount"].orEmpty(),
                values["fetalMovement"].orEmpty(),
                values["umbilicalInsertion"].orEmpty(),
                values["cervicalLengthMm"].orEmpty(),
                values["crlMm"].orEmpty(),
                values["ntMm"].orEmpty(),
                values["umbilicalSd"].orEmpty(),
                values["umbilicalPi"].orEmpty(),
                values["umbilicalRi"].orEmpty(),
                hasAdvancedUltrasoundDraft(values),
                ""
            )
        }
    }
}

private fun Map<String, Any?>.stringValue(key: String, defaultValue: String = ""): String {
    return this[key] as? String ?: defaultValue
}

private fun Map<String, Any?>.booleanValue(key: String): Boolean {
    return this[key] as? Boolean ?: false
}

@Composable
private fun RowScope.UltrasoundBpdInput(state: UltrasoundFormState) {
    UnitInputRow("BPD", state.bpd, { state.bpd = it }, "mm", Modifier.weight(1f))
}

@Composable
private fun RowScope.UltrasoundHcInput(state: UltrasoundFormState) {
    UnitInputRow("HC", state.hc, { state.hc = it }, "mm", Modifier.weight(1f))
}

@Composable
private fun RowScope.UltrasoundAcInput(state: UltrasoundFormState) {
    UnitInputRow("AC", state.ac, { state.ac = it }, "mm", Modifier.weight(1f))
}

@Composable
private fun RowScope.UltrasoundFlInput(state: UltrasoundFormState) {
    UnitInputRow("FL", state.fl, { state.fl = it }, "mm", Modifier.weight(1f))
}

@Composable
private fun RowScope.UltrasoundFetalHeartRateInput(state: UltrasoundFormState) {
    UnitInputRow("胎心率", state.fetalHeartRate, { state.fetalHeartRate = it }, "bpm", Modifier.weight(1f))
}

@Composable
private fun RowScope.UltrasoundCrlInput(state: UltrasoundFormState) {
    UnitInputRow("CRL", state.crl, { state.crl = it }, "mm", Modifier.weight(1f))
}

@Composable
private fun RowScope.UltrasoundNtInput(state: UltrasoundFormState) {
    UnitInputRow("NT", state.nt, { state.nt = it }, "mm", Modifier.weight(1f))
}

@Composable
private fun RowScope.UltrasoundCervicalLengthInput(state: UltrasoundFormState) {
    UnitInputRow("宫颈管", state.cervicalLength, { state.cervicalLength = it }, "mm", Modifier.weight(1f))
}

@Composable
private fun RowScope.UltrasoundAfiInput(state: UltrasoundFormState) {
    UnitInputRow("AFI", state.afi, { state.afi = it }, "cm", Modifier.weight(1f))
}

@Composable
private fun RowScope.UltrasoundDeepestPocketInput(state: UltrasoundFormState) {
    UnitInputRow("最大羊水池", state.deepestPocket, { state.deepestPocket = it }, "cm", Modifier.weight(1f))
}

@Composable
private fun RowScope.UltrasoundSdInput(state: UltrasoundFormState) {
    UnitInputRow("S/D", state.umbilicalSd, { state.umbilicalSd = it }, "", Modifier.weight(1f))
}

@Composable
private fun RowScope.UltrasoundPiInput(state: UltrasoundFormState) {
    UnitInputRow("PI", state.umbilicalPi, { state.umbilicalPi = it }, "", Modifier.weight(1f))
}

@Composable
private fun UltrasoundEfwInput(state: UltrasoundFormState) {
    val estimatedEfw by remember(state) {
        derivedStateOf { state.estimatedEfwGram() }
    }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        UnitInputRow("EFW", state.efw, { state.efw = it }, "g")
        val estimate = estimatedEfw
        if (estimate != null) {
            Text(
                "EFW 留空保存时，会按 Hadlock 3 估算为 ${BabyLogFormatters.formatNumber(estimate)} g。",
                color = ChestnutPalette.Muted,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun UltrasoundSoftWarnings(state: UltrasoundFormState) {
    val warnings by remember(state) {
        derivedStateOf { state.softWarnings() }
    }
    if (warnings.isNotBlank()) {
        InlineWarning(warnings)
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
