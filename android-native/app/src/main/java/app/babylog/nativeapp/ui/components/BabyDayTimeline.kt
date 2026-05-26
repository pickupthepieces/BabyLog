@file:Suppress("FunctionNaming", "InvalidPackageDeclaration", "MagicNumber")

package app.babylog.nativeapp

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val TimelineHourHeight = 48.dp
private val TimelineLeftRailWidth = 54.dp
private val TimelineHeight = TimelineHourHeight * 24

@Composable
internal fun BabyDayTimeline(
    slots: BabyLogBabyDayTimelineSlots.TimelineSlots,
    onEventClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val isEmpty = slots.sleepSegments.isEmpty() && slots.eventPoints.isEmpty()
    Panel {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(TimelineHeight)
                .clip(RoundedCornerShape(16.dp))
                .background(ChestnutPalette.Surface2.copy(alpha = 0.42f))
        ) {
            TimelineGrid()
            TimelineHourLabels()
            slots.sleepSegments.forEach { segment ->
                SleepSegmentView(
                    segment = segment,
                    onClick = { onEventClick(segment.eventId) }
                )
            }
            slots.eventPoints.forEach { point ->
                EventPointView(
                    point = point,
                    onClick = { onEventClick(point.eventId) }
                )
            }
            if (isEmpty) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.Center)
                        .padding(start = TimelineLeftRailWidth + 12.dp, end = 18.dp)
                ) {
                    EmptyPanel("这一天还没有记录")
                }
            }
        }
    }
}

@Composable
private fun TimelineGrid() {
    val lineColor = ChestnutPalette.Border
    val mainLineColor = ChestnutPalette.Primary.copy(alpha = 0.26f)
    Canvas(modifier = Modifier.fillMaxWidth().height(TimelineHeight)) {
        val hourHeightPx = TimelineHourHeight.toPx()
        val railWidthPx = TimelineLeftRailWidth.toPx()
        for (hour in 0..24) {
            val y = hour * hourHeightPx
            val main = hour % 6 == 0
            drawLine(
                color = if (main) mainLineColor else lineColor.copy(alpha = 0.72f),
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = if (main) 1.6f else 1f
            )
        }
        drawLine(
            color = ChestnutPalette.Primary,
            start = Offset(railWidthPx, 0f),
            end = Offset(railWidthPx, size.height),
            strokeWidth = 3f,
            cap = StrokeCap.Round
        )
    }
}

@Composable
private fun TimelineHourLabels() {
    for (hour in 0..24) {
        val main = hour % 6 == 0 || hour == 24
        Text(
            text = if (main) "%02d:00".format(hour.coerceAtMost(24)) else hour.toString(),
            color = if (main) ChestnutPalette.Muted else ChestnutPalette.Text3,
            fontSize = if (main) 11.sp else 10.sp,
            fontWeight = if (main) FontWeight.Bold else FontWeight.SemiBold,
            modifier = Modifier
                .width(TimelineLeftRailWidth - 8.dp)
                .offset(x = 8.dp, y = minuteOffset(hour * 60).coerceAtMost(TimelineHeight - 14.dp))
        )
    }
}

@Composable
private fun SleepSegmentView(
    segment: BabyLogBabyDayTimelineSlots.SleepSegment,
    onClick: () -> Unit
) {
    val top = minuteOffset(segment.startMinuteOfDay)
    val height = minuteOffset(segment.endMinuteOfDay - segment.startMinuteOfDay).coerceAtLeast(28.dp)
    Box(
        modifier = Modifier
            .padding(start = TimelineLeftRailWidth + 12.dp, end = 14.dp)
            .offset(y = top)
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(12.dp))
            .background(ChestnutPalette.Violet.copy(alpha = 0.26f))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = sleepSegmentLabel(segment),
            color = ChestnutPalette.Ink,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun EventPointView(
    point: BabyLogBabyDayTimelineSlots.EventPoint,
    onClick: () -> Unit
) {
    val tone = remember(point.eventType) { timelineTone(point.eventType) }
    Box(
        modifier = Modifier
            .padding(start = TimelineLeftRailWidth + 10.dp, end = 12.dp)
            .offset(y = minuteOffset(point.minuteOfDay).coerceAtMost(TimelineHeight - 40.dp))
            .fillMaxWidth()
            .height(40.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(CircleShape)
                .background(tone)
        )
        Column(modifier = Modifier.padding(start = 22.dp)) {
            Text(
                text = "${minuteLabel(point.minuteOfDay)} · ${BabyLogFormatters.eventLabel(point.eventType)}",
                color = tone,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
            if (point.summaryLabel.isNotBlank()) {
                Spacer(Modifier.height(1.dp))
                Text(
                    text = point.summaryLabel,
                    color = ChestnutPalette.Ink,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

private fun minuteOffset(minuteOfDay: Int): Dp {
    return (minuteOfDay.coerceIn(0, 1440) / 60f * TimelineHourHeight.value).dp
}

private fun minuteLabel(minuteOfDay: Int): String {
    val safe = minuteOfDay.coerceIn(0, 1439)
    return "%02d:%02d".format(safe / 60, safe % 60)
}

private fun sleepSegmentLabel(segment: BabyLogBabyDayTimelineSlots.SleepSegment): String {
    val prefix = when {
        segment.startsBeforeDay -> "上一日延续 · "
        segment.endsAfterDay && !segment.incomplete -> "跨日继续 · "
        segment.incomplete -> "睡眠中 · "
        else -> ""
    }
    return prefix + "${minuteLabel(segment.startMinuteOfDay)}-${if (segment.endMinuteOfDay >= 1440) "24:00" else minuteLabel(segment.endMinuteOfDay)}"
}

private fun timelineTone(eventType: String): Color {
    return when (eventType) {
        "feed", "bottle", "breastfeed" -> ChestnutPalette.Blue
        "diaper", "pee", "poop" -> ChestnutPalette.Yellow
        "temperature", "wake" -> ChestnutPalette.Green
        "medication" -> ChestnutPalette.Peach
        "milestone" -> ChestnutPalette.Rose
        else -> ChestnutPalette.Muted
    }
}
