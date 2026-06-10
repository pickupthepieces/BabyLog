@file:Suppress("ReturnCount")

package app.babylog.nativeapp

import androidx.compose.ui.graphics.Color
import app.babylog.nativeapp.ui.screens.BabyLogRoutes
import org.json.JSONObject
internal fun isEventVisibleInHome(event: BabyLogDomain.BabyLogEvent, stage: String): Boolean {
    val group = BabyLogFormatters.timelineFilterGroup(event.eventType)
    return when (stage) {
        BabyLogDomain.STAGE_PREGNANCY -> group == "pregnancy" || group == "ultrasound" || group == "checkup"
        BabyLogDomain.STAGE_BABY -> group == "baby" || group == "temperature"
        BabyLogDomain.STAGE_PREGNANCY_ENDED,
        BabyLogDomain.STAGE_PAUSED -> group == "pregnancy" || group == "ultrasound" || group == "checkup"
        else -> false
    }
}

@Suppress("ReturnCount")
internal fun extractDateInput(text: String?): String? {
    if (text.isNullOrBlank()) {
        return null
    }
    val match = Regex("\\d{4}-\\d{2}-\\d{2}").find(text) ?: return null
    val value = match.value
    return if (BabyLogFormatters.isValidDateInput(value)) value else null
}

internal fun eventTone(eventType: String): Color {
    return when (BabyLogFormatters.timelineFilterGroup(eventType)) {
        "pregnancy" -> ChestnutPalette.Accent
        "ultrasound" -> ChestnutPalette.Rose
        "temperature" -> ChestnutPalette.Green
        "checkup" -> ChestnutPalette.Violet
        "baby" -> ChestnutPalette.Peach
        else -> ChestnutPalette.Blue
    }
}

internal fun attachmentCount(attachments: List<BabyLogDomain.AttachmentRecord>): String {
    return if (attachments.isEmpty()) "0 张" else "${attachments.size} 张"
}

internal fun latestEfwValue(events: List<BabyLogDomain.BabyLogEvent>): String {
    val event = events.firstOrNull { it.eventType == "ultrasound" } ?: return "暂无"
    val efw = payloadNumber(event.payload, "efwGram")
    return if (efw == null) "待补" else "${BabyLogFormatters.formatNumber(efw)} g"
}

@Suppress("ReturnCount")
internal fun latestBpdFlValue(events: List<BabyLogDomain.BabyLogEvent>): String {
    val event = events.firstOrNull { it.eventType == "ultrasound" } ?: return "暂无"
    val bpd = payloadNumber(event.payload, "bpdMm")
    val fl = payloadNumber(event.payload, "flMm")
    if (bpd == null && fl == null) {
        return "待补"
    }
    val bpdText = bpd?.let { "BPD ${BabyLogFormatters.formatNumber(it)}" }
    val flText = fl?.let { "FL ${BabyLogFormatters.formatNumber(it)}" }
    return listOfNotNull(bpdText, flText).joinToString(" / ")
}

internal fun latestUltrasoundCaption(events: List<BabyLogDomain.BabyLogEvent>): String {
    val event = events.firstOrNull { it.eventType == "ultrasound" } ?: return "保存 B 超后显示"
    return BabyLogFormatters.formatEventDay(event.occurredAt)
}

internal fun ultrasoundWarningText(event: BabyLogDomain.BabyLogEvent): String {
    return BabyLogFormatters.formatUltrasoundSoftRangeWarnings(
        if (event.payload.has("gestationalAgeDays")) event.payload.optInt("gestationalAgeDays") else null,
        payloadNumber(event.payload, "bpdMm"),
        payloadNumber(event.payload, "hcMm"),
        payloadNumber(event.payload, "acMm"),
        payloadNumber(event.payload, "flMm"),
        payloadNumber(event.payload, "efwGram")
    )
}

private fun payloadNumber(payload: JSONObject, key: String): Double? {
    return if (payload.has(key)) BabyLogFormatters.parseOptionalNumber(payload.optString(key, "")) else null
}

internal fun tabTitle(activeTab: String): String {
    return when (activeTab) {
        BabyLogRoutes.Timeline -> "时间线"
        BabyLogRoutes.Library -> "资料"
        BabyLogRoutes.Settings -> "设置"
        else -> "首页"
    }
}
