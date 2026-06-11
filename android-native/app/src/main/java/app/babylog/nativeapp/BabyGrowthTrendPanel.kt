@file:Suppress("FunctionNaming", "MagicNumber")

package app.babylog.nativeapp

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.json.JSONObject

@Composable
internal fun BabyGrowthTrendPanel(events: List<BabyLogDomain.BabyLogEvent>) {
    val points = remember(events) { babyGrowthPoints(events) }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            GrowthTrendCard(
                metric = GrowthTrendMetric("宝宝体重", "kg", ChestnutPalette.Rose) { it.weightKg },
                points = points,
                modifier = Modifier.weight(1f)
            )
            GrowthTrendCard(
                metric = GrowthTrendMetric("宝宝身长", "cm", ChestnutPalette.Green) { it.heightCm },
                points = points,
                modifier = Modifier.weight(1f)
            )
        }
        GrowthTrendCard(
            metric = GrowthTrendMetric("宝宝头围", "cm", ChestnutPalette.Blue) { it.headCircumferenceCm },
            points = points,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun GrowthTrendCard(
    metric: GrowthTrendMetric,
    points: List<BabyGrowthPoint>,
    modifier: Modifier = Modifier
) {
    val selector = metric.selector
    val value = latestMetric(points, selector)
    val delta = metricDelta(points, selector)
    val count = metricCount(points, selector)
    val values = metricValues(points, selector)
    val valueLabel = value?.let { "${BabyLogFormatters.formatNumber(it)} ${metric.unit}" } ?: "暂无"
    val deltaLabel = delta?.let {
        val sign = if (it >= 0.0) "+" else ""
        "较上次 $sign${BabyLogFormatters.formatNumber(it)} ${metric.unit}"
    } ?: "保存 2 次后显示变化"
    val countLabel = if (count > 0) "已记录 $count 点" else "录入成长后显示"
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(ChestnutRadius.Card),
        backgroundColor = ChestnutPalette.Surface,
        border = BorderStroke(1.dp, ChestnutPalette.Border.copy(alpha = 0.28f)),
        elevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .heightIn(min = 132.dp)
        ) {
            Box(
                modifier = Modifier
                    .width(34.dp)
                    .height(3.dp)
                    .background(metric.tone)
            )
            Spacer(Modifier.height(10.dp))
            Text(
                metric.title,
                color = ChestnutPalette.Muted,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                valueLabel,
                color = ChestnutPalette.Ink,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                if (count > 1) "$countLabel · $deltaLabel" else countLabel,
                color = ChestnutPalette.Muted,
                fontSize = 12.sp
            )
            Spacer(Modifier.height(10.dp))
            GrowthSparkline(values, metric.tone)
        }
    }
}

@Composable
private fun GrowthSparkline(values: List<Double>, tone: Color) {
    Canvas(modifier = Modifier.fillMaxWidth().height(34.dp)) {
        val baseline = size.height * 0.72f
        drawLine(
            color = ChestnutPalette.Border.copy(alpha = 0.55f),
            start = Offset(0f, baseline),
            end = Offset(size.width, baseline),
            strokeWidth = 1.dp.toPx()
        )
        if (values.size < 2) {
            drawCircle(
                color = tone.copy(alpha = 0.55f),
                radius = 3.5.dp.toPx(),
                center = Offset(size.width * 0.5f, baseline)
            )
            return@Canvas
        }
        val min = values.minOrNull() ?: return@Canvas
        val max = values.maxOrNull() ?: return@Canvas
        val range = (max - min).takeIf { it > 0.0001 } ?: 1.0
        val path = Path()
        values.forEachIndexed { index, value ->
            val x = if (values.size == 1) {
                size.width * 0.5f
            } else {
                size.width * index / values.lastIndex
            }
            val normalized = ((value - min) / range).toFloat()
            val y = size.height - normalized * (size.height * 0.78f) - size.height * 0.11f
            if (index == 0) {
                path.moveTo(x, y)
            } else {
                path.lineTo(x, y)
            }
        }
        drawPath(
            path = path,
            color = tone,
            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
        )
    }
}

private data class GrowthTrendMetric(
    val title: String,
    val unit: String,
    val tone: Color,
    val selector: (BabyGrowthPoint) -> Double?
)

private data class BabyGrowthPoint(
    val occurredAt: String,
    val weightKg: Double?,
    val heightCm: Double?,
    val headCircumferenceCm: Double?
)

private fun babyGrowthPoints(events: List<BabyLogDomain.BabyLogEvent>): List<BabyGrowthPoint> {
    return events
        .asSequence()
        .filter { (it.eventType == "growth" || it.eventType == "child_checkup") && it.deletedAt == null }
        .mapNotNull { event ->
            val payload = event.payload ?: return@mapNotNull null
            val point = BabyGrowthPoint(
                event.occurredAt,
                payloadMetric(payload, "weightKg"),
                payloadMetric(payload, "heightCm"),
                payloadMetric(payload, "headCircumferenceCm")
            )
            if (point.weightKg == null && point.heightCm == null && point.headCircumferenceCm == null) null else point
        }
        .sortedBy { BabyLogFormatters.parseIsoMillis(it.occurredAt) }
        .toList()
}

private fun payloadMetric(payload: JSONObject, key: String): Double? {
    if (!payload.has(key)) return null
    val value = payload.optDouble(key, Double.NaN)
    return if (value.isNaN() || value.isInfinite()) null else value
}

private fun latestMetric(points: List<BabyGrowthPoint>, selector: (BabyGrowthPoint) -> Double?): Double? {
    for (point in points.asReversed()) {
        selector(point)?.let { return it }
    }
    return null
}

private fun metricCount(points: List<BabyGrowthPoint>, selector: (BabyGrowthPoint) -> Double?): Int {
    return points.count { selector(it) != null }
}

private fun metricValues(points: List<BabyGrowthPoint>, selector: (BabyGrowthPoint) -> Double?): List<Double> {
    return points.mapNotNull(selector)
}

private fun metricDelta(points: List<BabyGrowthPoint>, selector: (BabyGrowthPoint) -> Double?): Double? {
    val values = points.mapNotNull(selector)
    return if (values.size >= 2) values.last() - values[values.size - 2] else null
}
