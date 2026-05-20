package app.babylog.nativeapp

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.Alignment
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
    voiceState: SmartVoiceUiState,
    onLongTextVoiceStart: LongTextVoiceStart,
    onLongTextVoiceStop: () -> Unit,
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
        subtitle = "拍照识别优先，手填只保留核心生长指标",
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
                Text("拍 B 超单识别", color = ChestnutPalette.Ink, fontWeight = FontWeight.Bold)
                Text("先拍照或选图，识别候选后再由你核对保存；手填只是兜底。", color = ChestnutPalette.Muted, fontSize = 12.sp)
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
                        val candidateAdvancedSection = advancedSectionForCandidate(ocrCandidate)
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
                        ocrCandidate.diagnosisText.value?.let { formState.diagnosisText = it }
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
                        if (candidateAdvancedSection.isNotBlank()) {
                            formState.showAdvanced = true
                            formState.advancedSection = candidateAdvancedSection
                        }
                        onCandidateApplied()
                    },
                    onDismiss = onCandidateDismiss
                )
            }
        }
        item(key = "exam_date") { UltrasoundExamDateInput(formState) }
        item(key = "gestational_age") { UltrasoundGestationalAgeInput(formState) }
        item(key = "growth_header") { Text("胎儿生长指标", color = ChestnutPalette.Ink, fontWeight = FontWeight.Bold) }
        item(key = "growth_bpd_hc") {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                UltrasoundBpdInput(formState)
                UltrasoundHcInput(formState)
            }
        }
        item(key = "growth_ac_fl") {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                UltrasoundAcInput(formState)
                UltrasoundFlInput(formState)
            }
        }
        item(key = "efw") { UltrasoundEfwInput(formState) }
        item(key = "diagnosis_text_core") { UltrasoundDiagnosisInput(formState, voiceState, onLongTextVoiceStart, onLongTextVoiceStop) }
        item(key = "soft_warnings") { UltrasoundSoftWarnings(formState) }
        item(key = "advanced_toggle") {
            OutlinedButton(
                onClick = {
                    formState.showAdvanced = !formState.showAdvanced
                    if (formState.showAdvanced && formState.advancedSection.isBlank()) {
                        formState.advancedSection = "report"
                    }
                },
                border = BorderStroke(1.dp, ChestnutPalette.Border),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    if (formState.showAdvanced) "收起更多医学指标" else "更多医学指标 ▾",
                    color = ChestnutPalette.Muted
                )
            }
        }
        if (formState.showAdvanced) {
            item(key = "advanced_tabs") {
                UltrasoundAdvancedSectionTabs(formState.advancedSection, formState.updateAdvancedSection)
            }
            when (formState.advancedSection) {
                "report" -> {
                    item(key = "report_header") { Text("报告信息", color = ChestnutPalette.Ink, fontWeight = FontWeight.Bold) }
                    item(key = "hospital") { UltrasoundHospitalInput(formState) }
                    item(key = "report_time") { UltrasoundReportTimeInput(formState) }
                }
                "fetal" -> {
                    item(key = "fetal_header") { Text("胎儿附加信息", color = ChestnutPalette.Ink, fontWeight = FontWeight.Bold) }
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
                    item(key = "fetal_count") { UltrasoundFetalCountInput(formState) }
                    item(key = "fetal_movement") { UltrasoundFetalMovementInput(formState) }
                    item(key = "umbilical_insertion") { UltrasoundUmbilicalInsertionInput(formState) }
                }
                "amniotic" -> {
                    item(key = "amniotic_header") { Text("羊水 / 胎盘 / 胎位", color = ChestnutPalette.Ink, fontWeight = FontWeight.Bold) }
                    item(key = "afi_deepest_pocket") {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            UltrasoundAfiInput(formState)
                            UltrasoundDeepestPocketInput(formState)
                        }
                    }
                    item(key = "placenta_location") { UltrasoundPlacentaLocationInput(formState) }
                    item(key = "placenta_grade") { UltrasoundPlacentaGradeInput(formState) }
                    item(key = "fetal_presentation") { UltrasoundFetalPresentationInput(formState) }
                }
                "flow" -> {
                    item(key = "flow_header") { Text("脐动脉血流", color = ChestnutPalette.Ink, fontWeight = FontWeight.Bold) }
                    item(key = "sd_pi") {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            UltrasoundSdInput(formState)
                            UltrasoundPiInput(formState)
                        }
                    }
                    item(key = "ri") { UltrasoundRiInput(formState) }
                }
                else -> {
                    item(key = "advanced_hint") {
                        Text(
                            "按报告内容选择一组填写；未填写的项目保存时会自动留空。",
                            color = ChestnutPalette.Muted,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
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
    advancedSection: String,
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
    var advancedSection by mutableStateOf(advancedSection)
    var saveError by mutableStateOf(saveError)

    val updateExamDate: (String) -> Unit = { this.examDate = it }
    val updateGestationalAge: (String) -> Unit = {
        this.gestationalAgeEdited = true
        this.gestationalAge = it
    }
    val updateHospital: (String) -> Unit = { this.hospital = it }
    val updateReportTime: (String) -> Unit = { this.reportTime = it }
    val updateDiagnosisText: (String) -> Unit = { this.diagnosisText = it }
    val updateBpd: (String) -> Unit = { this.bpd = it }
    val updateHc: (String) -> Unit = { this.hc = it }
    val updateAc: (String) -> Unit = { this.ac = it }
    val updateFl: (String) -> Unit = { this.fl = it }
    val updateEfw: (String) -> Unit = { this.efw = it }
    val updateAfi: (String) -> Unit = { this.afi = it }
    val updateDeepestPocket: (String) -> Unit = { this.deepestPocket = it }
    val updatePlacentaLocation: (String) -> Unit = { this.placentaLocation = it }
    val updatePlacentaGrade: (String) -> Unit = { this.placentaGrade = it }
    val updateFetalPresentation: (String) -> Unit = { this.fetalPresentation = it }
    val updateFetalHeartRate: (String) -> Unit = { this.fetalHeartRate = it }
    val updateFetalCount: (String) -> Unit = { this.fetalCount = it }
    val updateFetalMovement: (String) -> Unit = { this.fetalMovement = it }
    val updateUmbilicalInsertion: (String) -> Unit = { this.umbilicalInsertion = it }
    val updateCervicalLength: (String) -> Unit = { this.cervicalLength = it }
    val updateCrl: (String) -> Unit = { this.crl = it }
    val updateNt: (String) -> Unit = { this.nt = it }
    val updateUmbilicalSd: (String) -> Unit = { this.umbilicalSd = it }
    val updateUmbilicalPi: (String) -> Unit = { this.umbilicalPi = it }
    val updateUmbilicalRi: (String) -> Unit = { this.umbilicalRi = it }
    val updateAdvancedSection: (String) -> Unit = { this.advancedSection = it }

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
                    "advancedSection" to state.advancedSection,
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
                    restored.stringValue("advancedSection"),
                    restored.stringValue("saveError")
                )
            }
        )

        fun fromValues(values: Map<String, String>, defaultGestationalAge: String): UltrasoundFormState {
            val advancedSection = advancedUltrasoundDraftSection(values)
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
                advancedSection.isNotBlank(),
                advancedSection,
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

private fun advancedUltrasoundDraftSection(values: Map<String, String>): String {
    return when {
        hasAnyDraftValue(values, "afiCm", "deepestPocketCm", "placentaLocation", "placentaGrade", "fetalPresentation") -> "amniotic"
        hasAnyDraftValue(
            values,
            "fetalHeartRateBpm",
            "fetalCount",
            "fetalMovement",
            "umbilicalInsertion",
            "cervicalLengthMm",
            "crlMm",
            "ntMm"
        ) -> "fetal"
        hasAnyDraftValue(values, "umbilicalSd", "umbilicalPi", "umbilicalRi") -> "flow"
        hasAnyDraftValue(values, "hospital", "reportTime") -> "report"
        else -> ""
    }
}

private fun hasAnyDraftValue(values: Map<String, String>, vararg keys: String): Boolean {
    return keys.any { key -> !values[key].isNullOrBlank() }
}

private fun advancedSectionForCandidate(candidate: BabyLogSmartInput.UltrasoundOcrCandidate): String {
    return when {
        candidate.afiCm.value != null ||
            candidate.deepestPocketCm.value != null ||
            !candidate.placentaLocation.value.isNullOrBlank() ||
            !candidate.placentaGrade.value.isNullOrBlank() ||
            !candidate.fetalPresentation.value.isNullOrBlank() -> "amniotic"
        candidate.fetalHeartRateBpm.value != null ||
            !candidate.fetalCount.value.isNullOrBlank() ||
            !candidate.fetalMovement.value.isNullOrBlank() ||
            !candidate.umbilicalInsertion.value.isNullOrBlank() ||
            candidate.cervicalLengthMm.value != null ||
            candidate.crlMm.value != null ||
            candidate.ntMm.value != null -> "fetal"
        candidate.umbilicalSd.value != null ||
            candidate.umbilicalPi.value != null ||
            candidate.umbilicalRi.value != null -> "flow"
        !candidate.hospital.value.isNullOrBlank() || !candidate.reportTime.value.isNullOrBlank() -> "report"
        else -> ""
    }
}

@Composable
private fun UltrasoundExamDateInput(state: UltrasoundFormState) {
    DateInputRow("检查日期", state.examDate, state.updateExamDate, allowClear = false)
}

@Composable
private fun UltrasoundGestationalAgeInput(state: UltrasoundFormState) {
    ChestnutTextField(
        "孕周，例如 28+3",
        state.gestationalAge,
        state.updateGestationalAge,
        KeyboardType.Text
    )
}

@Composable
private fun UltrasoundHospitalInput(state: UltrasoundFormState) {
    ChestnutTextField("医院 / 机构", state.hospital, state.updateHospital, KeyboardType.Text)
}

@Composable
private fun UltrasoundReportTimeInput(state: UltrasoundFormState) {
    ChestnutTextField("报告时间", state.reportTime, state.updateReportTime, KeyboardType.Text)
}

@Composable
private fun UltrasoundDiagnosisInput(
    state: UltrasoundFormState,
    voiceState: SmartVoiceUiState,
    onLongTextVoiceStart: LongTextVoiceStart,
    onLongTextVoiceStop: () -> Unit
) {
    ChestnutLongTextField(
        "医生结论 / 提示",
        state.diagnosisText,
        state.updateDiagnosisText,
        minLines = 2,
        maxLines = 4,
        voiceState = voiceState,
        onVoiceStart = onLongTextVoiceStart,
        onVoiceStop = onLongTextVoiceStop
    )
}

@Composable
private fun RowScope.UltrasoundBpdInput(state: UltrasoundFormState) {
    UnitInputRow("BPD", state.bpd, state.updateBpd, "mm", Modifier.weight(1f))
}

@Composable
private fun RowScope.UltrasoundHcInput(state: UltrasoundFormState) {
    UnitInputRow("HC", state.hc, state.updateHc, "mm", Modifier.weight(1f))
}

@Composable
private fun RowScope.UltrasoundAcInput(state: UltrasoundFormState) {
    UnitInputRow("AC", state.ac, state.updateAc, "mm", Modifier.weight(1f))
}

@Composable
private fun RowScope.UltrasoundFlInput(state: UltrasoundFormState) {
    UnitInputRow("FL", state.fl, state.updateFl, "mm", Modifier.weight(1f))
}

@Composable
private fun RowScope.UltrasoundFetalHeartRateInput(state: UltrasoundFormState) {
    UnitInputRow("胎心率", state.fetalHeartRate, state.updateFetalHeartRate, "bpm", Modifier.weight(1f))
}

@Composable
private fun RowScope.UltrasoundCrlInput(state: UltrasoundFormState) {
    UnitInputRow("CRL", state.crl, state.updateCrl, "mm", Modifier.weight(1f))
}

@Composable
private fun RowScope.UltrasoundNtInput(state: UltrasoundFormState) {
    UnitInputRow("NT", state.nt, state.updateNt, "mm", Modifier.weight(1f))
}

@Composable
private fun RowScope.UltrasoundCervicalLengthInput(state: UltrasoundFormState) {
    UnitInputRow("宫颈管", state.cervicalLength, state.updateCervicalLength, "mm", Modifier.weight(1f))
}

@Composable
private fun RowScope.UltrasoundAfiInput(state: UltrasoundFormState) {
    UnitInputRow("AFI", state.afi, state.updateAfi, "cm", Modifier.weight(1f))
}

@Composable
private fun RowScope.UltrasoundDeepestPocketInput(state: UltrasoundFormState) {
    UnitInputRow("最大羊水池", state.deepestPocket, state.updateDeepestPocket, "cm", Modifier.weight(1f))
}

@Composable
private fun RowScope.UltrasoundSdInput(state: UltrasoundFormState) {
    UnitInputRow("S/D", state.umbilicalSd, state.updateUmbilicalSd, "", Modifier.weight(1f))
}

@Composable
private fun RowScope.UltrasoundPiInput(state: UltrasoundFormState) {
    UnitInputRow("PI", state.umbilicalPi, state.updateUmbilicalPi, "", Modifier.weight(1f))
}

@Composable
private fun UltrasoundRiInput(state: UltrasoundFormState) {
    UnitInputRow("RI", state.umbilicalRi, state.updateUmbilicalRi, "")
}

@Composable
private fun UltrasoundFetalCountInput(state: UltrasoundFormState) {
    ChoiceChipRow(
        label = "胎儿个数",
        selected = state.fetalCount,
        options = listOf("单胎" to "单胎", "双胎" to "双胎", "多胎" to "多胎", "未写" to "未写"),
        onSelect = state.updateFetalCount
    )
}

@Composable
private fun UltrasoundFetalMovementInput(state: UltrasoundFormState) {
    ChoiceChipRow(
        label = "胎动",
        selected = state.fetalMovement,
        options = listOf("有" to "有", "可见" to "可见", "无" to "无", "未写" to "未写"),
        onSelect = state.updateFetalMovement
    )
}

@Composable
private fun UltrasoundUmbilicalInsertionInput(state: UltrasoundFormState) {
    ChestnutTextField("脐带插入处", state.umbilicalInsertion, state.updateUmbilicalInsertion, KeyboardType.Text)
}

@Composable
private fun UltrasoundPlacentaLocationInput(state: UltrasoundFormState) {
    ChoiceChipRow(
        label = "胎盘位置",
        selected = state.placentaLocation,
        options = listOf("前壁" to "前壁", "后壁" to "后壁", "侧壁" to "侧壁", "低置" to "低置", "前置" to "前置", "其他" to "其他"),
        onSelect = state.updatePlacentaLocation
    )
}

@Composable
private fun UltrasoundPlacentaGradeInput(state: UltrasoundFormState) {
    ChoiceChipRow(
        label = "胎盘成熟度",
        selected = state.placentaGrade,
        options = listOf("0级" to "0 级", "I 级" to "I 级", "II 级" to "II 级", "III 级" to "III 级", "未写" to "未写"),
        onSelect = state.updatePlacentaGrade
    )
}

@Composable
private fun UltrasoundFetalPresentationInput(state: UltrasoundFormState) {
    ChoiceChipRow(
        label = "胎位",
        selected = state.fetalPresentation,
        options = listOf("头位" to "头位", "臀位" to "臀位", "横位" to "横位", "不详" to "不详"),
        onSelect = state.updateFetalPresentation
    )
}

@Composable
private fun UltrasoundAdvancedSectionTabs(
    selected: String,
    onSelect: (String) -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(
            "report" to "报告",
            "fetal" to "胎儿",
            "amniotic" to "羊水胎盘",
            "flow" to "脐血流"
        ).forEach { (key, label) ->
            val active = selected == key
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (active) ChestnutPalette.Primary else ChestnutPalette.Surface)
                    .border(
                        1.dp,
                        if (active) ChestnutPalette.Primary else ChestnutPalette.Border,
                        RoundedCornerShape(12.dp)
                    )
                    .clickable { onSelect(key) }
                    .padding(horizontal = 6.dp, vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    label,
                    color = if (active) Color.White else ChestnutPalette.Ink,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun UltrasoundEfwInput(state: UltrasoundFormState) {
    val estimatedEfw by remember(state) {
        derivedStateOf { state.estimatedEfwGram() }
    }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        UnitInputRow("EFW", state.efw, state.updateEfw, "g")
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
