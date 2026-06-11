@file:Suppress("LongMethod", "CyclomaticComplexMethod", "SpreadOperator", "ReturnCount", "MagicNumber")

package app.babylog.nativeapp

import org.json.JSONObject

internal fun smartFormFields(vararg values: Pair<String, String?>): Map<String, String> {
    return linkedMapOf(*values.filter { !it.second.isNullOrBlank() }
        .map { it.first to it.second.orEmpty() }
        .toTypedArray())
}

internal fun draftFromBabyCareEvent(event: BabyLogDomain.BabyLogEvent): SmartEntryDraft {
    return SmartEntryDraft(
        values = BabyLogService.babyCareDraftFields(event.eventType, event.payload) + smartFormFields(
            "occurredDate" to BabyLogFormatters.recordDay(event.occurredAt).takeIf { BabyLogFormatters.isValidDateInput(it) },
            "occurredTime" to BabyLogFormatters.formatEventTime(event.occurredAt).takeUnless { it == "--:--" }
        )
    )
}

internal fun draftFromPregnancyEvent(event: BabyLogDomain.BabyLogEvent): SmartEntryDraft {
    val payload = event.payload
    if (event.eventType == "fetal_movement") {
        return SmartEntryDraft(
            values = smartFormFields(
                "primary" to payload.optString("movementWindow").ifBlank {
                    sessionWindowDraft(payload.optString("startedAt"), payload.optString("endedAt"))
                },
                "secondary" to payloadNumberText(payload, "movementCount"),
                "note" to payload.optString("note")
            )
        )
    }
    if (event.eventType == "contraction") {
        return SmartEntryDraft(
            values = smartFormFields(
                "primary" to payload.optString("contractionStart").ifBlank { BabyLogFormatters.formatEventTime(event.occurredAt).takeUnless { it == "--:--" } },
                "secondary" to payloadNumberText(payload, "intervalMinutes").ifBlank { intervalMinutesDraft(payload) },
                "tertiary" to payloadNumberText(payload, "durationSeconds").ifBlank { payloadNumberText(payload, "durationSec") },
                "note" to payload.optString("note")
            )
        )
    }
    if (BabyLogService.isScreeningEventType(event.eventType)) {
        return SmartEntryDraft(
            values = smartFormFields(
                "primary" to payload.optString("screeningDate").ifBlank { BabyLogFormatters.recordDay(event.occurredAt) },
                "gestationalAge" to gestationalAgeDraftValue(payload),
                "ntMm" to payloadNumberText(payload, "ntMm"),
                "riskT21" to payload.optString("riskT21"),
                "riskT18" to payload.optString("riskT18"),
                "riskOntd" to payload.optString("riskOntd"),
                "riskLevel" to payload.optString("riskLevel"),
                "t21Result" to payload.optString("t21Result"),
                "t18Result" to payload.optString("t18Result"),
                "t13Result" to payload.optString("t13Result"),
                "sexChromosome" to payload.optString("sexChromosome"),
                "structureConclusion" to payload.optString("structureConclusion"),
                "fastingGlucoseMmolL" to payloadNumberText(payload, "fastingGlucoseMmolL"),
                "oneHourGlucoseMmolL" to payloadNumberText(payload, "oneHourGlucoseMmolL"),
                "twoHourGlucoseMmolL" to payloadNumberText(payload, "twoHourGlucoseMmolL"),
                "abnormalFlag" to payload.optString("abnormalFlag"),
                "gbsResult" to payload.optString("gbsResult"),
                "nstResult" to payload.optString("nstResult"),
                "conclusion" to payload.optString("conclusion"),
                "attachmentNote" to payload.optString("attachmentNote"),
                "note" to payload.optString("note")
            )
        )
    }
    return SmartEntryDraft(
        values = smartFormFields(
            "primary" to payload.optString("checkupDate").ifBlank { BabyLogFormatters.recordDay(event.occurredAt) },
            "gestationalAge" to gestationalAgeDraftValue(payload),
            "secondary" to payload.optString("provider"),
            "department" to payload.optString("department"),
            "systolicBp" to payloadNumberText(payload, "systolicBp"),
            "diastolicBp" to payloadNumberText(payload, "diastolicBp"),
            "weightKg" to payloadNumberText(payload, "weightKg"),
            "fundalHeightCm" to payloadNumberText(payload, "fundalHeightCm"),
            "abdominalCircumferenceCm" to payloadNumberText(payload, "abdominalCircumferenceCm"),
            "fetalHeartRateBpm" to payloadNumberText(payload, "fetalHeartRateBpm"),
            "fetalPresentation" to payload.optString("fetalPresentation"),
            "edema" to payload.optString("edema"),
            "urineRoutine" to payload.optString("urineRoutine"),
            "urineProtein" to payload.optString("urineProtein"),
            "hemoglobinGL" to payloadNumberText(payload, "hemoglobinGL"),
            "highRiskFactors" to payload.optString("highRiskFactors"),
            "tertiary" to payload.optString("doctorConclusion").ifBlank { payload.optString("finding") },
            "treatmentAdvice" to payload.optString("treatmentAdvice"),
            "nextVisitDate" to payload.optString("nextVisitDate").ifBlank { extractDateInput(payload.optString("nextVisitNote")) ?: "" },
            "reportType" to payload.optString("reportType"),
            "attachmentNote" to payload.optString("attachmentNote"),
            "note" to payload.optString("note").ifBlank {
                payload.optString("nextVisitNote").takeUnless { BabyLogFormatters.isValidDateInput(it) }.orEmpty()
            }
        )
    )
}

private fun sessionWindowDraft(startedAt: String, endedAt: String): String {
    val start = BabyLogFormatters.formatEventTime(startedAt)
    val end = BabyLogFormatters.formatEventTime(endedAt)
    return when {
        start != "--:--" && end != "--:--" -> "$start-$end"
        start != "--:--" -> start
        end != "--:--" -> end
        else -> ""
    }
}

internal fun draftFromMaternalMetricEvent(event: BabyLogDomain.BabyLogEvent): SmartEntryDraft {
    val payload = event.payload
    return SmartEntryDraft(
        values = smartFormFields(
            "weightKg" to payloadNumberText(payload, "weightKg"),
            "systolicBp" to payloadNumberText(payload, "systolicBp"),
            "diastolicBp" to payloadNumberText(payload, "diastolicBp"),
            "glucoseMmolL" to payloadNumberText(payload, "glucoseMmolL"),
            "glucoseContext" to payload.optString("glucoseContext"),
            "note" to payload.optString("note")
        )
    )
}

internal fun draftFromUltrasoundEvent(event: BabyLogDomain.BabyLogEvent): SmartEntryDraft {
    val payload = event.payload
    return SmartEntryDraft(
        values = smartFormFields(
            "examDate" to payload.optString("examDate").ifBlank { BabyLogFormatters.recordDay(event.occurredAt) },
            "gestationalAge" to gestationalAgeDraftValue(payload),
            "hospital" to payload.optString("hospital"),
            "reportTime" to payload.optString("reportTime"),
            "diagnosisText" to payload.optString("diagnosisText"),
            "bpdMm" to payloadNumberText(payload, "bpdMm"),
            "hcMm" to payloadNumberText(payload, "hcMm"),
            "acMm" to payloadNumberText(payload, "acMm"),
            "flMm" to payloadNumberText(payload, "flMm"),
            "efwGram" to payloadNumberText(payload, "efwGram"),
            "afiCm" to payloadNumberText(payload, "afiCm"),
            "deepestPocketCm" to payloadNumberText(payload, "deepestPocketCm"),
            "placentaLocation" to payload.optString("placentaLocation"),
            "placentaGrade" to payload.optString("placentaGrade"),
            "fetalPresentation" to payload.optString("fetalPresentation"),
            "fetalHeartRateBpm" to payloadNumberText(payload, "fetalHeartRateBpm"),
            "fetalCount" to payload.optString("fetalCount"),
            "fetalMovement" to payload.optString("fetalMovement"),
            "umbilicalInsertion" to payload.optString("umbilicalInsertion"),
            "cervicalLengthMm" to payloadNumberText(payload, "cervicalLengthMm"),
            "crlMm" to payloadNumberText(payload, "crlMm"),
            "ntMm" to payloadNumberText(payload, "ntMm"),
            "umbilicalSd" to payloadNumberText(payload, "umbilicalSd"),
            "umbilicalPi" to payloadNumberText(payload, "umbilicalPi"),
            "umbilicalRi" to payloadNumberText(payload, "umbilicalRi")
        )
    )
}

private fun payloadNumberText(payload: JSONObject, key: String): String {
    if (!payload.has(key) || payload.isNull(key)) {
        return ""
    }
    return BabyLogFormatters.formatNumber(payload.optDouble(key))
}

private fun intervalMinutesDraft(payload: JSONObject): String {
    if (!payload.has("intervalFromPrevSec") || payload.isNull("intervalFromPrevSec")) {
        return ""
    }
    val seconds = payload.optDouble("intervalFromPrevSec")
    if (seconds <= 0.0) {
        return ""
    }
    return BabyLogFormatters.formatNumber(seconds / 60.0)
}

private fun gestationalAgeDraftValue(payload: JSONObject): String {
    if (!payload.has("gestationalAgeDays") || payload.isNull("gestationalAgeDays")) {
        return ""
    }
    return BabyLogFormatters.formatGestationalAge(payload.optInt("gestationalAgeDays")).removeSuffix(" 周")
}

