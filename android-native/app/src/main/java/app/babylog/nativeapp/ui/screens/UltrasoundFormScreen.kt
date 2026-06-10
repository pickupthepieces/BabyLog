package app.babylog.nativeapp

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.mapSaver
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

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
        subtitle = "拍照识别或手动记录核心指标",
        saveText = if (isEditing) "保存修改" else "保存生长指标",
        onBack = onBack,
        onSave = {
            val input = formState.toInput(photoPath, photoName)
            if (!BabyLogService.hasUltrasoundMinimumContent(input)) {
                formState.saveError = "请选择 B 超单图片，或填写任一生长指标。"
            } else {
                formState.saveError = ""
                onSave(input)
            }
        }
    ) {
        ultrasoundPhotoSection(
            photoPath = photoPath,
            photoName = photoName,
            ocrRunning = ocrRunning,
            ocrCandidate = ocrCandidate,
            saveError = formState.saveError,
            onPickPhoto = onPickPhoto,
            onCapturePhoto = onCapturePhoto,
            onRecognizePhoto = onRecognizePhoto,
            onCandidateDismiss = onCandidateDismiss,
            onCandidateApply = { candidate ->
                        val candidateAdvancedSection = advancedSectionForCandidate(candidate)
                        candidate.examDate.value?.takeIf { BabyLogFormatters.isValidDateInput(it) }?.let {
                            formState.examDate = it
                            if (!formState.gestationalAgeEdited) {
                                gestationalAgeInputForDate(expectedDueDate, it).takeIf { value -> value.isNotBlank() }?.let { value ->
                                    formState.gestationalAge = value
                                }
                            }
                        }
                        candidate.hospital.value?.let { formState.hospital = it; formState.showAdvanced = true }
                        candidate.reportTime.value?.let { formState.reportTime = it; formState.showAdvanced = true }
                        candidate.diagnosisText.value?.let { formState.diagnosisText = it }
                        candidate.bpdMm.value?.let { formState.bpd = BabyLogFormatters.formatNumber(it) }
                        candidate.hcMm.value?.let { formState.hc = BabyLogFormatters.formatNumber(it) }
                        candidate.acMm.value?.let { formState.ac = BabyLogFormatters.formatNumber(it) }
                        candidate.flMm.value?.let { formState.fl = BabyLogFormatters.formatNumber(it) }
                        candidate.efwGram.value?.let { formState.efw = BabyLogFormatters.formatNumber(it) }
                        candidate.afiCm.value?.let { formState.afi = BabyLogFormatters.formatNumber(it); formState.showAdvanced = true }
                        candidate.deepestPocketCm.value?.let { formState.deepestPocket = BabyLogFormatters.formatNumber(it); formState.showAdvanced = true }
                        candidate.placentaLocation.value?.let { formState.placentaLocation = it; formState.showAdvanced = true }
                        candidate.placentaGrade.value?.let { formState.placentaGrade = it; formState.showAdvanced = true }
                        candidate.fetalPresentation.value?.let { formState.fetalPresentation = it; formState.showAdvanced = true }
                        candidate.fetalHeartRateBpm.value?.let { formState.fetalHeartRate = BabyLogFormatters.formatNumber(it); formState.showAdvanced = true }
                        candidate.fetalCount.value?.let { formState.fetalCount = it; formState.showAdvanced = true }
                        candidate.fetalMovement.value?.let { formState.fetalMovement = it; formState.showAdvanced = true }
                        candidate.umbilicalInsertion.value?.let { formState.umbilicalInsertion = it; formState.showAdvanced = true }
                        candidate.cervicalLengthMm.value?.let { formState.cervicalLength = BabyLogFormatters.formatNumber(it); formState.showAdvanced = true }
                        candidate.crlMm.value?.let { formState.crl = BabyLogFormatters.formatNumber(it); formState.showAdvanced = true }
                        candidate.ntMm.value?.let { formState.nt = BabyLogFormatters.formatNumber(it); formState.showAdvanced = true }
                        candidate.umbilicalSd.value?.let { formState.umbilicalSd = BabyLogFormatters.formatNumber(it); formState.showAdvanced = true }
                        candidate.umbilicalPi.value?.let { formState.umbilicalPi = BabyLogFormatters.formatNumber(it); formState.showAdvanced = true }
                        candidate.umbilicalRi.value?.let { formState.umbilicalRi = BabyLogFormatters.formatNumber(it); formState.showAdvanced = true }
                        if (candidateAdvancedSection.isNotBlank()) {
                            formState.showAdvanced = true
                            formState.advancedSection = candidateAdvancedSection
                        }
                        onCandidateApplied()
            }
        )
        item(key = "exam_date") { UltrasoundExamDateInput(formState) }
        item(key = "gestational_age") { UltrasoundGestationalAgeInput(formState) }
        ultrasoundGrowthSection(formState, voiceState, onLongTextVoiceStart, onLongTextVoiceStop)
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
                    if (formState.showAdvanced) "收起详细指标" else "补充详细指标 ▾",
                    color = ChestnutPalette.Muted
                )
            }
        }
        if (formState.showAdvanced) {
            ultrasoundClinicalSection(formState)
        }
    }
}

@Stable
internal class UltrasoundFormState(
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
