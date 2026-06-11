@file:Suppress("FunctionNaming", "InvalidPackageDeclaration", "MagicNumber", "TooManyFunctions")

package app.babylog.nativeapp

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val TimelineHourHeight = 48.dp
private val TimelineLeftRailWidth = 54.dp
private val TimelineHeight = TimelineHourHeight * 24
private val TimelineViewportHeight = 420.dp

// 同窗口内（从首条算起 40 分钟）的事件聚成一行胶囊，水平排开、放不下换行，
// 避免同时刻多条记录垂直堆砌偏离时间刻度。
private const val TIMELINE_CLUSTER_WINDOW_MINUTES = 40
private val TimelineChipMaxWidth = 200.dp
private val TimelineClusterBottomInset = 30.dp

@Composable
internal fun BabyDayTimeline(
    slots: BabyLogBabyDayTimelineSlots.TimelineSlots,
    selectedDay: String,
    onEventClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val isEmpty = slots.sleepSegments.isEmpty() && slots.eventPoints.isEmpty()
    val scrollState = rememberTimelineScrollState(slots, selectedDay)
    Panel {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(TimelineViewportHeight)
                .clip(RoundedCornerShape(ChestnutRadius.Control))
                .background(ChestnutPalette.Surface2.copy(alpha = 0.42f))
        ) {
            TimelineScrollableContent(slots, scrollState, onEventClick)
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
private fun rememberTimelineScrollState(
    slots: BabyLogBabyDayTimelineSlots.TimelineSlots,
    selectedDay: String
): ScrollState {
    val initialFocusMinute = remember(slots, selectedDay) {
        BabyLogBabyDayTimelineSlots.initialFocusMinute(slots, selectedDay, BabyLogFormatters.nowIso())
    }
    val density = LocalDensity.current
    val initialScrollPx = remember(initialFocusMinute, density) {
        with(density) {
            val maxScroll = (TimelineHeight - TimelineViewportHeight).roundToPx().coerceAtLeast(0)
            minuteOffset(initialFocusMinute).roundToPx().coerceIn(0, maxScroll)
        }
    }
    val scrollState = rememberScrollState(initial = initialScrollPx)
    LaunchedEffect(initialScrollPx) {
        scrollState.scrollTo(initialScrollPx)
    }
    return scrollState
}

@Composable
private fun TimelineScrollableContent(
    slots: BabyLogBabyDayTimelineSlots.TimelineSlots,
    scrollState: ScrollState,
    onEventClick: (String) -> Unit
) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(TimelineViewportHeight)
            .verticalScroll(scrollState)
    ) {
        Box(Modifier.fillMaxWidth().height(TimelineHeight)) {
            TimelineGrid()
            TimelineHourLabels()
            slots.sleepSegments.forEach { segment ->
                SleepSegmentView(segment, onClick = { onEventClick(segment.eventId) })
            }
            val clusters = remember(slots.eventPoints) {
                clusterEventPoints(slots.eventPoints)
            }
            clusters.forEach { cluster ->
                EventClusterView(cluster, onEventClick)
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
            .clip(RoundedCornerShape(ChestnutRadius.Small))
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

private data class TimelineEventCluster(
    val points: List<BabyLogBabyDayTimelineSlots.EventPoint>,
    val offsetY: Dp
)

private fun clusterEventPoints(
    points: List<BabyLogBabyDayTimelineSlots.EventPoint>
): List<TimelineEventCluster> {
    val clusters = mutableListOf<MutableList<BabyLogBabyDayTimelineSlots.EventPoint>>()
    var clusterStartMinute = Int.MIN_VALUE
    points.sortedBy { it.minuteOfDay }.forEach { point ->
        if (clusters.isEmpty() || point.minuteOfDay - clusterStartMinute >= TIMELINE_CLUSTER_WINDOW_MINUTES) {
            clusters += mutableListOf(point)
            clusterStartMinute = point.minuteOfDay
        } else {
            clusters.last() += point
        }
    }
    return clusters.map { clusterPoints ->
        TimelineEventCluster(
            points = clusterPoints,
            offsetY = minuteOffset(clusterPoints.first().minuteOfDay)
                .coerceAtMost(TimelineHeight - TimelineClusterBottomInset)
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EventClusterView(
    cluster: TimelineEventCluster,
    onEventClick: (String) -> Unit
) {
    FlowRow(
        modifier = Modifier
            .padding(start = TimelineLeftRailWidth + 10.dp, end = 12.dp)
            .offset(y = cluster.offsetY)
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        cluster.points.forEach { point ->
            EventChip(point, onClick = { onEventClick(point.eventId) })
        }
    }
}

@Composable
private fun EventChip(
    point: BabyLogBabyDayTimelineSlots.EventPoint,
    onClick: () -> Unit
) {
    val tone = remember(point.eventType) {
        when (point.eventType) {
            "feed", "bottle", "breastfeed" -> ChestnutPalette.Blue
            "diaper", "pee", "poop" -> ChestnutPalette.Yellow
            "temperature", "wake" -> ChestnutPalette.Green
            "medication" -> ChestnutPalette.Peach
            "milestone" -> ChestnutPalette.Rose
            else -> ChestnutPalette.Muted
        }
    }
    val eventLabel = BabyLogFormatters.eventLabel(point.eventType)
    val summaryLabel = remember(point.summaryLabel, eventLabel) {
        point.summaryLabel
            .removePrefix(eventLabel)
            .trim()
            .trimStart('·', ':', '：', '-', ' ')
            .trim()
            // 占位文案在时间轴上没有信息量，只显示真实摘要。
            .takeIf { it != "待补充详情" }
            .orEmpty()
    }
    Row(
        modifier = Modifier
            .widthIn(max = TimelineChipMaxWidth)
            .clip(RoundedCornerShape(ChestnutRadius.Small))
            .background(ChestnutPalette.Surface.copy(alpha = 0.88f))
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(tone)
        )
        Spacer(Modifier.width(5.dp))
        Text(
            text = buildAnnotatedString {
                withStyle(SpanStyle(color = tone, fontWeight = FontWeight.Bold)) {
                    append("${minuteLabel(point.minuteOfDay)} $eventLabel")
                }
                if (summaryLabel.isNotBlank()) {
                    withStyle(SpanStyle(color = ChestnutPalette.Ink)) {
                        append(" · $summaryLabel")
                    }
                }
            },
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
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
    val endLabel = if (segment.endMinuteOfDay >= 1440) "24:00" else minuteLabel(segment.endMinuteOfDay)
    return prefix + "${minuteLabel(segment.startMinuteOfDay)}-$endLabel"
}
