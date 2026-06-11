@file:Suppress("MagicNumber", "ReturnCount")

package app.babylog.nativeapp

import java.util.Locale

internal fun currentCareStage(profile: BabyLogDomain.ChildProfile): String {
    return BabyLogFormatters.resolveCareStage(profile, BabyLogFormatters.todayDateInput())
}

internal fun currentGestationalAgeInput(profile: BabyLogDomain.ChildProfile): String {
    if (!BabyLogFormatters.isValidDateInput(profile.expectedDueDate)) {
        return ""
    }
    return gestationalAgeInputForDate(profile.expectedDueDate, BabyLogFormatters.todayDateInput())
}

internal fun gestationalAgeInputForDate(expectedDueDate: String, examDate: String): String {
    if (!BabyLogFormatters.isValidDateInput(expectedDueDate) || !BabyLogFormatters.isValidDateInput(examDate)) {
        return ""
    }
    val daysToDue = daysBetween(examDate, expectedDueDate)
    val gestationalDays = (280 - daysToDue).coerceIn(0, 280)
    return BabyLogFormatters.formatGestationalAge(gestationalDays).removeSuffix(" 周")
}

internal fun stageLabel(stage: String): String {
    return when (stage) {
        BabyLogDomain.STAGE_PREGNANCY -> "孕期"
        BabyLogDomain.STAGE_BABY -> "出生后"
        BabyLogDomain.STAGE_PREGNANCY_ENDED -> "妊娠结束"
        BabyLogDomain.STAGE_PAUSED -> "暂停"
        else -> "待补档案"
    }
}

internal fun normalizeSexInput(value: String): String {
    val normalized = value.trim().lowercase(Locale.US)
    return when (normalized) {
        "女", "female", "girl" -> "female"
        "男", "male", "boy" -> "male"
        else -> "unknown"
    }
}

internal fun normalizeStageInput(value: String): String {
    val normalized = value.trim().lowercase(Locale.US)
    return when (normalized) {
        "孕期", "孕期中", BabyLogDomain.STAGE_PREGNANCY -> BabyLogDomain.STAGE_PREGNANCY
        "出生后", "育儿", BabyLogDomain.STAGE_BABY -> BabyLogDomain.STAGE_BABY
        "妊娠结束", BabyLogDomain.STAGE_PREGNANCY_ENDED -> BabyLogDomain.STAGE_PREGNANCY_ENDED
        "暂停", BabyLogDomain.STAGE_PAUSED -> BabyLogDomain.STAGE_PAUSED
        "未知", BabyLogDomain.STAGE_UNKNOWN -> BabyLogDomain.STAGE_UNKNOWN
        else -> BabyLogDomain.STAGE_AUTO
    }
}

internal fun parsePositiveProfileNumber(value: String): OptionalProfileNumber? {
    if (value.trim().isEmpty()) {
        return OptionalProfileNumber(null)
    }
    val parsed = BabyLogFormatters.parseOptionalNumber(value)
    if (parsed == null || parsed <= 0.0) {
        return null
    }
    return OptionalProfileNumber(parsed)
}

internal fun isValidBabyLogReminderTimeInput(value: String): Boolean {
    if (!value.matches(Regex("\\d{2}:\\d{2}"))) {
        return false
    }
    val hour = value.substring(0, 2).toIntOrNull() ?: return false
    val minute = value.substring(3, 5).toIntOrNull() ?: return false
    return hour in 0..23 && minute in 0..59
}

internal fun daysBetween(fromDate: String, toDate: String): Int {
    return BabyLogFormatters.daysBetweenDateInputs(fromDate, toDate)
}
