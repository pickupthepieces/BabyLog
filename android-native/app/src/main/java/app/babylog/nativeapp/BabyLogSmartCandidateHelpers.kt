package app.babylog.nativeapp

import java.util.Locale

internal fun hasAdvancedUltrasoundDraft(values: Map<String, String>): Boolean {
    val basicKeys = setOf("examDate", "gestationalAge", "bpdMm", "hcMm", "acMm", "flMm", "efwGram")
    return values.any { (key, value) -> key !in basicKeys && value.isNotBlank() }
}

internal fun normalizeGlucoseContext(value: String): String {
    val normalized = value.trim().lowercase(Locale.US)
    return when {
        normalized == "fasting" || value.contains("空腹") -> "fasting"
        normalized == "after_1h" || value.contains("1h") || value.contains("1小时") || value.contains("一小时") -> "after_1h"
        normalized == "after_2h" || value.contains("2h") || value.contains("2小时") || value.contains("两小时") -> "after_2h"
        normalized == "random" || value.contains("随机") -> "random"
        else -> "random"
    }
}

internal fun ultrasoundCandidateRows(candidate: BabyLogSmartInput.UltrasoundOcrCandidate): List<Pair<String, String>> {
    val rows = mutableListOf<Pair<String, String>>()
    addCandidateRow(rows, "检查日期", candidate.examDate.value)
    addCandidateRow(rows, "医院", candidate.hospital.value)
    addCandidateRow(rows, "报告时间", candidate.reportTime.value)
    addCandidateRow(rows, "诊断提示", candidate.diagnosisText.value)
    addCandidateRow(rows, "BPD", formatCandidateNumber(candidate.bpdMm.value, "mm"))
    addCandidateRow(rows, "HC", formatCandidateNumber(candidate.hcMm.value, "mm"))
    addCandidateRow(rows, "AC", formatCandidateNumber(candidate.acMm.value, "mm"))
    addCandidateRow(rows, "FL", formatCandidateNumber(candidate.flMm.value, "mm"))
    addCandidateRow(rows, "EFW", formatCandidateNumber(candidate.efwGram.value, "g"))
    addCandidateRow(rows, "AFI", formatCandidateNumber(candidate.afiCm.value, "cm"))
    addCandidateRow(rows, "最大羊水池", formatCandidateNumber(candidate.deepestPocketCm.value, "cm"))
    addCandidateRow(rows, "胎盘", candidate.placentaLocation.value)
    addCandidateRow(rows, "成熟度", candidate.placentaGrade.value)
    addCandidateRow(rows, "胎位", candidate.fetalPresentation.value)
    addCandidateRow(rows, "胎心率", formatCandidateNumber(candidate.fetalHeartRateBpm.value, "bpm"))
    addCandidateRow(rows, "胎儿个数", candidate.fetalCount.value)
    addCandidateRow(rows, "胎动", candidate.fetalMovement.value)
    addCandidateRow(rows, "脐带插入处", candidate.umbilicalInsertion.value)
    addCandidateRow(rows, "宫颈管", formatCandidateNumber(candidate.cervicalLengthMm.value, "mm"))
    addCandidateRow(rows, "CRL", formatCandidateNumber(candidate.crlMm.value, "mm"))
    addCandidateRow(rows, "NT", formatCandidateNumber(candidate.ntMm.value, "mm"))
    addCandidateRow(rows, "S/D", formatCandidateNumber(candidate.umbilicalSd.value, ""))
    addCandidateRow(rows, "PI", formatCandidateNumber(candidate.umbilicalPi.value, ""))
    addCandidateRow(rows, "RI", formatCandidateNumber(candidate.umbilicalRi.value, ""))
    return rows
}

private fun addCandidateRow(rows: MutableList<Pair<String, String>>, label: String, value: String?) {
    if (!value.isNullOrBlank()) {
        rows.add(label to value)
    }
}

private fun formatCandidateNumber(value: Double?, unit: String): String? {
    if (value == null) {
        return null
    }
    val number = BabyLogFormatters.formatNumber(value)
    return if (unit.isBlank()) number else "$number $unit"
}
