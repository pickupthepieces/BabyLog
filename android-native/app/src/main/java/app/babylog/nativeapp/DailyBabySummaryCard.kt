package app.babylog.nativeapp

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private const val TILE_BG_ALPHA = 0.10f
private const val TILE_VALUE_SLIDE_FRACTION = 3

@Composable
@Suppress("FunctionNaming")
internal fun DailyBabySummaryCard(summary: BabyLogDailyBabySummary) {
    val tiles = dailyBabySummaryTiles(summary)
    if (tiles.isEmpty()) return
    Panel {
        val day = BabyLogFormatters.formatEventDay("${summary.dateInput}T00:00:00.000+0800").ifBlank { "日" }
        SectionHeader("${day}摘要")
        Spacer(Modifier.height(10.dp))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            tiles.chunked(2).forEach { rowTiles ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Max),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    rowTiles.forEach { tile ->
                        DailySummaryTileCell(tile, Modifier.weight(1f))
                    }
                    if (rowTiles.size == 1) {
                        Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
@Suppress("FunctionNaming")
private fun DailySummaryTileCell(tile: DailySummaryTile, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(ChestnutRadius.Control))
            .background(tile.tone.copy(alpha = TILE_BG_ALPHA))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(tile.label, color = tile.tone, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        AnimatedContent(
            targetState = tile.value,
            transitionSpec = {
                (fadeIn() + slideInVertically { it / TILE_VALUE_SLIDE_FRACTION }) togetherWith fadeOut()
            },
            label = "dailySummaryTileValue"
        ) { value ->
            Text(
                value,
                color = ChestnutPalette.Ink,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (tile.detail.isNotBlank()) {
            Text(
                tile.detail,
                color = ChestnutPalette.Muted,
                fontSize = 11.sp,
                lineHeight = 15.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

private data class DailySummaryTile(
    val label: String,
    val value: String,
    val detail: String,
    val tone: Color
)

private fun dailyBabySummaryTiles(summary: BabyLogDailyBabySummary): List<DailySummaryTile> {
    return listOfNotNull(
        feedSummaryTile(summary),
        sleepSummaryTile(summary),
        diaperSummaryTile(summary),
        temperatureSummaryTile(summary),
        growthSummaryTile(summary),
        medicationSummaryTile(summary),
        summary.milestoneCount.takeIf { it > 0 }?.let {
            DailySummaryTile("里程碑", "$it 个", "", ChestnutPalette.Primary)
        }
    )
}

private fun feedSummaryTile(summary: BabyLogDailyBabySummary): DailySummaryTile? {
    if (summary.feedCount <= 0) return null
    fun kindCount(label: String, count: Int): String? {
        return count.takeIf { it > 0 }?.let { "$label $it" }
    }

    val breakdown = listOfNotNull(
        kindCount("母乳", summary.feedBreastCount),
        kindCount("奶瓶", summary.feedBottleCount),
        kindCount("辅食", summary.feedSolidCount)
    ).joinToString(" · ")
    val details = mutableListOf<String>()
    if (breakdown.isNotBlank()) details += breakdown
    if (summary.feedTotalMl > 0) details += "总 ${summary.feedTotalMl} mL"
    val feedTime = BabyLogFormatters.formatEventTime(summary.feedLastTime)
        .takeIf { it != "--:--" }
        ?.let { " $it" }
        .orEmpty()
    if (summary.feedLastType.isNotBlank()) {
        val latestAmount = if (summary.feedLastAmountMl > 0) " ${summary.feedLastAmountMl} mL" else ""
        details += "最近 ${summary.feedLastType}$latestAmount$feedTime"
    }
    return DailySummaryTile("喂养", "${summary.feedCount} 次", details.joinToString(" · "), ChestnutPalette.Rose)
}

private fun sleepSummaryTile(summary: BabyLogDailyBabySummary): DailySummaryTile? {
    if (summary.sleepTotalMinutes <= 0 && summary.sleepIncompleteCount <= 0) return null
    val value = if (summary.sleepTotalMinutes > 0) {
        BabyLogFormatters.formatSleepDurationLabel(summary.sleepTotalMinutes)
    } else {
        "进行中"
    }
    val details = mutableListOf<String>()
    if (summary.sleepLongestMinutes > 0) {
        details += "最长 ${BabyLogFormatters.formatSleepDurationLabel(summary.sleepLongestMinutes)}"
    }
    if (summary.sleepIncompleteCount > 0) {
        details += "含 ${summary.sleepIncompleteCount} 段未结束"
    }
    if (summary.sleepLastTime.isNotBlank()) {
        details += "最后 ${BabyLogFormatters.formatEventTime(summary.sleepLastTime)}"
    }
    return DailySummaryTile("睡眠", value, details.joinToString(" · "), ChestnutPalette.Violet)
}

private fun diaperSummaryTile(summary: BabyLogDailyBabySummary): DailySummaryTile? {
    val counts = mutableListOf<String>()
    if (summary.peeCount > 0) counts += "尿 ${summary.peeCount}"
    if (summary.poopCount > 0) counts += "便 ${summary.poopCount}"
    if (counts.isEmpty() && summary.diaperCount > 0) counts += "${summary.diaperCount} 次"
    if (counts.isEmpty()) return null
    val detail = if (summary.diaperLastKind.isNotBlank()) {
        val diaperTime = BabyLogFormatters.formatEventTime(summary.diaperLastTime)
            .takeIf { it != "--:--" }
            ?.let { " $it" }
            .orEmpty()
        "最后 ${summary.diaperLastKind}$diaperTime"
    } else {
        ""
    }
    return DailySummaryTile("尿布", counts.joinToString(" · "), detail, ChestnutPalette.Blue)
}

private fun temperatureSummaryTile(summary: BabyLogDailyBabySummary): DailySummaryTile? {
    if (summary.temperatureMin.isNaN() || summary.temperatureMax.isNaN()) return null
    val value = "${BabyLogFormatters.formatNumber(summary.temperatureMax)} ℃"
    val details = mutableListOf<String>()
    if (summary.temperatureMin != summary.temperatureMax) {
        details += "最低 ${BabyLogFormatters.formatNumber(summary.temperatureMin)} ℃"
    }
    summaryTimeDetail(summary.temperatureLastTime)?.let { details += it }
    return DailySummaryTile("体温", value, details.joinToString(" · "), ChestnutPalette.Peach)
}

private fun medicationSummaryTile(summary: BabyLogDailyBabySummary): DailySummaryTile? {
    if (summary.medicationLastName.isBlank() && summary.medicationLastTime.isBlank()) return null
    val name = summary.medicationLastName.ifBlank { "已记录" }
    return DailySummaryTile(
        "用药",
        name,
        summaryTimeDetail(summary.medicationLastTime).orEmpty(),
        ChestnutPalette.Accent
    )
}

private fun growthSummaryTile(summary: BabyLogDailyBabySummary): DailySummaryTile? {
    val parts = mutableListOf<String>()
    if (!summary.growthWeightKg.isNaN()) {
        parts += "${BabyLogFormatters.formatNumber(summary.growthWeightKg)} kg"
    }
    if (!summary.growthHeightCm.isNaN()) {
        parts += "身长 ${BabyLogFormatters.formatNumber(summary.growthHeightCm)} cm"
    }
    if (!summary.growthHeadCircumferenceCm.isNaN()) {
        parts += "头围 ${BabyLogFormatters.formatNumber(summary.growthHeadCircumferenceCm)} cm"
    }
    if (parts.isEmpty()) return null
    return DailySummaryTile(
        "成长",
        parts.first(),
        parts.drop(1).joinToString(" · "),
        ChestnutPalette.Green
    )
}

private fun summaryTimeDetail(iso: String): String? {
    if (iso.isBlank()) return null
    val time = BabyLogFormatters.formatEventTime(iso)
    return if (time == "--:--") BabyLogFormatters.relativeTimeFromNow(iso) else "最后 $time"
}
