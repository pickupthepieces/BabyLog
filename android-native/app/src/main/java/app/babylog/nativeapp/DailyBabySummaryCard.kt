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
internal fun DailyBabySummaryCard(summary: BabyLogService.DailyBabySummary) {
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

private fun dailyBabySummaryRows(summary: BabyLogService.DailyBabySummary): List<DailyBabySummaryRow> {
    return listOfNotNull(
        feedSummaryRow(summary),
        sleepSummaryRow(summary),
        diaperSummaryRow(summary),
        temperatureSummaryRow(summary),
        medicationSummaryRow(summary),
        milestoneSummaryRow(summary)
    )
}

private fun feedSummaryRow(summary: BabyLogService.DailyBabySummary): DailyBabySummaryRow? {
    if (summary.feedCount <= 0) return null
    val amount = if (summary.feedTotalMl > 0) " / ${summary.feedTotalMl} mL" else ""
    return DailyBabySummaryRow("喂养", "${countLabel(summary.feedCount)}$amount${summaryTimeSuffix(summary.feedLastTime)}")
}

private fun sleepSummaryRow(summary: BabyLogService.DailyBabySummary): DailyBabySummaryRow? {
    if (summary.sleepTotalMinutes <= 0 && summary.sleepIncompleteCount <= 0) return null
    val parts = mutableListOf<String>()
    if (summary.sleepTotalMinutes > 0) {
        parts += BabyLogFormatters.formatSleepDurationLabel(summary.sleepTotalMinutes)
    }
    if (summary.sleepIncompleteCount > 0) {
        parts += "含 ${countLabel(summary.sleepIncompleteCount)} 段未结束"
    }
    return DailyBabySummaryRow("睡眠", parts.joinToString(" · "))
}

private fun diaperSummaryRow(summary: BabyLogService.DailyBabySummary): DailyBabySummaryRow? {
    val parts = mutableListOf<String>()
    if (summary.peeCount > 0) parts += "尿 ${countLabel(summary.peeCount)}"
    if (summary.poopCount > 0) parts += "便 ${countLabel(summary.poopCount)}"
    if (summary.diaperCount > 0 && parts.isEmpty()) parts += "尿布 ${countLabel(summary.diaperCount)}"
    return parts.takeIf { it.isNotEmpty() }?.let { DailyBabySummaryRow("尿布", it.joinToString(" · ")) }
}

private fun temperatureSummaryRow(summary: BabyLogService.DailyBabySummary): DailyBabySummaryRow? {
    if (summary.temperatureMin.isNaN() || summary.temperatureMax.isNaN()) return null
    val temperature = if (summary.temperatureMin == summary.temperatureMax) {
        "${BabyLogFormatters.formatNumber(summary.temperatureMax)} ℃"
    } else {
        "最低 ${BabyLogFormatters.formatNumber(summary.temperatureMin)} ℃ / 最高 ${BabyLogFormatters.formatNumber(summary.temperatureMax)} ℃"
    }
    return DailyBabySummaryRow("体温", temperature + summaryTimeSuffix(summary.temperatureLastTime))
}

private fun medicationSummaryRow(summary: BabyLogService.DailyBabySummary): DailyBabySummaryRow? {
    if (summary.medicationLastName.isBlank() && summary.medicationLastTime.isBlank()) return null
    val name = summary.medicationLastName.ifBlank { "最近一次" }
    return DailyBabySummaryRow("用药", "$name${summaryTimeSuffix(summary.medicationLastTime)}")
}

private fun milestoneSummaryRow(summary: BabyLogService.DailyBabySummary): DailyBabySummaryRow? {
    return if (summary.milestoneCount > 0) DailyBabySummaryRow("里程碑", countLabel(summary.milestoneCount)) else null
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
