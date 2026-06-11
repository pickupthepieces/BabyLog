package app.babylog.nativeapp

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
@Suppress("FunctionNaming")
internal fun DailyBabySummaryCard(summary: BabyLogDailyBabySummary) {
    val rows = dailyBabySummaryRows(summary)
    if (rows.isEmpty()) return
    Panel {
        val day = BabyLogFormatters.formatEventDay("${summary.dateInput}T00:00:00.000+0800").ifBlank { "日" }
        SectionHeader("${day}摘要")
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            rows.forEach { row ->
                Text(
                    "${row.label}  ${row.value}",
                    color = ChestnutPalette.Ink,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

private data class DailyBabySummaryRow(
    val label: String,
    val value: String
)

private fun dailyBabySummaryRows(summary: BabyLogDailyBabySummary): List<DailyBabySummaryRow> {
    return listOfNotNull(
        feedSummaryRow(summary),
        sleepSummaryRow(summary),
        diaperSummaryRow(summary),
        temperatureSummaryRow(summary),
        medicationSummaryRow(summary),
        growthSummaryRow(summary),
        summary.milestoneCount.takeIf { it > 0 }?.let { DailyBabySummaryRow("里程碑", countLabel(it)) }
    )
}

private fun feedSummaryRow(summary: BabyLogDailyBabySummary): DailyBabySummaryRow? {
    if (summary.feedCount <= 0) return null
    val amount = if (summary.feedTotalMl > 0) " / ${summary.feedTotalMl} mL" else ""
    val feedTime = BabyLogFormatters.formatEventTime(summary.feedLastTime).takeIf { it != "--:--" }?.let { " $it" }.orEmpty()
    val latest = if (summary.feedLastType.isBlank()) "" else " · 最近 ${summary.feedLastType}$feedTime"
    return DailyBabySummaryRow("喂养", "${countLabel(summary.feedCount)}$amount$latest")
}

private fun sleepSummaryRow(summary: BabyLogDailyBabySummary): DailyBabySummaryRow? {
    if (summary.sleepTotalMinutes <= 0 && summary.sleepIncompleteCount <= 0) return null
    val parts = mutableListOf<String>()
    if (summary.sleepTotalMinutes > 0) {
        parts += BabyLogFormatters.formatSleepDurationLabel(summary.sleepTotalMinutes)
    }
    if (summary.sleepLongestMinutes > 0) {
        parts += "最长 ${BabyLogFormatters.formatSleepDurationLabel(summary.sleepLongestMinutes)}"
    }
    if (summary.sleepIncompleteCount > 0) {
        parts += "含 ${countLabel(summary.sleepIncompleteCount)} 段未结束"
    }
    if (summary.sleepLastTime.isNotBlank()) {
        parts += "最后 ${BabyLogFormatters.formatEventTime(summary.sleepLastTime)}"
    }
    return DailyBabySummaryRow("睡眠", parts.joinToString(" · "))
}

private fun diaperSummaryRow(summary: BabyLogDailyBabySummary): DailyBabySummaryRow? {
    val parts = mutableListOf<String>()
    if (summary.peeCount > 0) parts += "尿 ${countLabel(summary.peeCount)}"
    if (summary.poopCount > 0) parts += "便 ${countLabel(summary.poopCount)}"
    if (summary.diaperCount > 0 && parts.isEmpty()) parts += "尿布 ${countLabel(summary.diaperCount)}"
    if (summary.diaperLastKind.isNotBlank()) {
        val diaperTime = BabyLogFormatters.formatEventTime(summary.diaperLastTime)
            .takeIf { it != "--:--" }
            ?.let { " $it" }
            .orEmpty()
        parts += "最后 ${summary.diaperLastKind}$diaperTime"
    }
    return parts.takeIf { it.isNotEmpty() }?.let { DailyBabySummaryRow("尿布", it.joinToString(" · ")) }
}

private fun temperatureSummaryRow(summary: BabyLogDailyBabySummary): DailyBabySummaryRow? {
    if (summary.temperatureMin.isNaN() || summary.temperatureMax.isNaN()) return null
    val temperature = if (summary.temperatureMin == summary.temperatureMax) {
        "${BabyLogFormatters.formatNumber(summary.temperatureMax)} ℃"
    } else {
        "最低 ${BabyLogFormatters.formatNumber(summary.temperatureMin)} ℃ / 最高 ${BabyLogFormatters.formatNumber(summary.temperatureMax)} ℃"
    }
    return DailyBabySummaryRow("体温", temperature + summaryTimeSuffix(summary.temperatureLastTime))
}

private fun medicationSummaryRow(summary: BabyLogDailyBabySummary): DailyBabySummaryRow? {
    if (summary.medicationLastName.isBlank() && summary.medicationLastTime.isBlank()) return null
    val name = summary.medicationLastName.ifBlank { "最近一次" }
    return DailyBabySummaryRow("用药", "$name${summaryTimeSuffix(summary.medicationLastTime)}")
}

private fun growthSummaryRow(summary: BabyLogDailyBabySummary): DailyBabySummaryRow? {
    val parts = mutableListOf<String>()
    if (!summary.growthWeightKg.isNaN()) {
        parts += "体重 ${BabyLogFormatters.formatNumber(summary.growthWeightKg)} kg"
    }
    if (!summary.growthHeightCm.isNaN()) {
        parts += "身长 ${BabyLogFormatters.formatNumber(summary.growthHeightCm)} cm"
    }
    if (!summary.growthHeadCircumferenceCm.isNaN()) {
        parts += "头围 ${BabyLogFormatters.formatNumber(summary.growthHeadCircumferenceCm)} cm"
    }
    return parts.takeIf { it.isNotEmpty() }?.let {
        DailyBabySummaryRow("成长", it.joinToString(" · ") + summaryTimeSuffix(summary.growthLastTime))
    }
}

private fun summaryTimeSuffix(iso: String): String {
    if (iso.isBlank()) return ""
    val time = BabyLogFormatters.formatEventTime(iso)
    val relative = BabyLogFormatters.relativeTimeFromNow(iso)
    return if (time == "--:--") " · $relative" else " · 最后 $time · $relative"
}

private fun countLabel(count: Int): String {
    return if (count == 1) "1" else "$count 次"
}
