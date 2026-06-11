@file:Suppress("FunctionNaming")

package app.babylog.nativeapp

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
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
    val valueLabel = value?.let { "${BabyLogFormatters.formatNumber(it)} ${metric.unit}" } ?: "暂无"
    val deltaLabel = delta?.let {
        val sign = if (it >= 0.0) "+" else ""
        "较上次 $sign${BabyLogFormatters.formatNumber(it)} ${metric.unit}"
    } ?: "保存 2 次后显示变化"
    val countLabel = if (count > 0) "已记录 $count 点" else "录入成长后显示"
    TrendCard(
        title = metric.title,
        value = valueLabel,
        subtitle = if (count > 1) "$countLabel · $deltaLabel" else countLabel,
        tone = metric.tone,
        modifier = modifier
    )
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
        .filter { it.eventType == "growth" && it.deletedAt == null }
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

private fun metricDelta(points: List<BabyGrowthPoint>, selector: (BabyGrowthPoint) -> Double?): Double? {
    val values = points.mapNotNull(selector)
    return if (values.size >= 2) values.last() - values[values.size - 2] else null
}
