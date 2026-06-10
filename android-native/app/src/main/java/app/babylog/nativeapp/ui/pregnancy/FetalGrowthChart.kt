package app.babylog.nativeapp

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.json.JSONObject
import kotlin.math.max
import kotlin.math.min

private data class FetalGrowthMetric(
    val key: String,
    val label: String,
    val unit: String,
    val tone: Color
)

private data class FetalGrowthPoint(
    val gestationalAgeDays: Int,
    val value: Double,
    val event: BabyLogDomain.BabyLogEvent
)

private data class FetalGrowthReferenceSeries(
    val weeks: List<Int>,
    val p10Values: List<Double>,
    val p50Values: List<Double>,
    val p90Values: List<Double>
)

private val fetalGrowthMetrics = listOf(
    FetalGrowthMetric("efwGram", "EFW", "g", ChestnutPalette.Rose),
    FetalGrowthMetric("bpdMm", "BPD", "mm", ChestnutPalette.Green),
    FetalGrowthMetric("hcMm", "HC", "mm", ChestnutPalette.Blue),
    FetalGrowthMetric("acMm", "AC", "mm", ChestnutPalette.Peach),
    FetalGrowthMetric("flMm", "FL", "mm", ChestnutPalette.Violet)
)

@Composable
fun FetalGrowthPanel(events: List<BabyLogDomain.BabyLogEvent>) {
    var selectedKey by rememberSaveable { mutableStateOf("efwGram") }
    val selectedMetric = fetalGrowthMetrics.firstOrNull { it.key == selectedKey } ?: fetalGrowthMetrics.first()
    val points = remember(events, selectedMetric.key) {
        extractFetalGrowthPoints(events, selectedMetric)
    }
    val latest = points.maxByOrNull { it.gestationalAgeDays }
    val latestReference = latest?.let {
        BabyLogFetalGrowthReference.evaluate(BabyLogFetalGrowthReference.DEFAULT_STANDARD, selectedMetric.key, it.gestationalAgeDays, it.value)
    }

    Panel {
        SectionHeader(title = "胎儿成长曲线")
        Spacer(Modifier.height(10.dp))
        Text(
            text = BabyLogFetalGrowthReference.approximationNotice(),
            color = ChestnutPalette.Muted,
            fontSize = 12.sp,
            lineHeight = 18.sp,
            modifier = Modifier
                .fillMaxWidth()
                .background(ChestnutPalette.Surface2, RoundedCornerShape(ChestnutRadius.Small))
                .padding(12.dp)
        )
        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            fetalGrowthMetrics.forEach { metric ->
                val active = metric.key == selectedMetric.key
                OutlinedButton(
                    onClick = { selectedKey = metric.key },
                    border = BorderStroke(1.dp, if (active) selectedMetric.tone else ChestnutPalette.Border),
                    colors = ButtonDefaults.outlinedButtonColors(
                        backgroundColor = if (active) selectedMetric.tone.copy(alpha = 0.12f) else ChestnutPalette.Surface,
                        contentColor = ChestnutPalette.Ink
                    )
                ) {
                    Text(metric.label, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        if (points.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(174.dp)
                    .background(ChestnutPalette.Bg.copy(alpha = 0.74f), RoundedCornerShape(ChestnutRadius.Control))
                    .padding(16.dp)
            ) {
                Text(
                    text = "还没有可画的 ${selectedMetric.label} 数据。保存带孕周和指标的 B 超后，这里会按孕周连线显示。",
                    color = ChestnutPalette.Muted,
                    fontSize = 13.sp,
                    lineHeight = 20.sp
                )
            }
        } else {
            FetalGrowthCanvas(points, selectedMetric)
            Row(Modifier.fillMaxWidth()) {
                Text(
                    text = "14 周",
                    color = ChestnutPalette.Text3,
                    fontSize = 11.sp
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = "40 周",
                    color = ChestnutPalette.Text3,
                    fontSize = 11.sp
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = latest?.let {
                    "${BabyLogFormatters.formatGestationalAge(it.gestationalAgeDays)} · ${selectedMetric.label} ${BabyLogFormatters.formatNumber(it.value)} ${selectedMetric.unit}"
                } ?: "",
                color = ChestnutPalette.Ink,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )
            Text(
                text = latestReference?.let {
                    "${it.standardLabel} · ${BabyLogFetalGrowthReference.formatResult(it)} · 未校准 P10/P50/P90"
                } ?: "参考曲线需要有效孕周和 ${selectedMetric.label} 数值。",
                color = ChestnutPalette.Muted,
                fontSize = 12.sp
            )
            Text(
                text = if (points.size == 1) "已有 1 次 B 超指标；当前百分位和 Z-score 是未校准近似值。" else "共 ${points.size} 个数据点；当前百分位和 Z-score 是未校准近似值。",
                color = ChestnutPalette.Text3,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun FetalGrowthCanvas(points: List<FetalGrowthPoint>, metric: FetalGrowthMetric) {
    val referenceSeries = remember(metric.key) {
        val referenceWeeks = (14..40).map { it * 7 }
        FetalGrowthReferenceSeries(
            weeks = referenceWeeks,
            p10Values = referenceWeeks.map {
                BabyLogFetalGrowthReference.referenceValue(BabyLogFetalGrowthReference.DEFAULT_STANDARD, metric.key, it, -1.2815515655446004)
            },
            p50Values = referenceWeeks.map {
                BabyLogFetalGrowthReference.referenceValue(BabyLogFetalGrowthReference.DEFAULT_STANDARD, metric.key, it, 0.0)
            },
            p90Values = referenceWeeks.map {
                BabyLogFetalGrowthReference.referenceValue(BabyLogFetalGrowthReference.DEFAULT_STANDARD, metric.key, it, 1.2815515655446004)
            }
        )
    }

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(210.dp)
            .background(ChestnutPalette.Bg.copy(alpha = 0.74f), RoundedCornerShape(ChestnutRadius.Control))
            .padding(12.dp)
    ) {
        val left = 26.dp.toPx()
        val top = 18.dp.toPx()
        val right = size.width - 14.dp.toPx()
        val bottom = size.height - 28.dp.toPx()
        val plotWidth = max(1f, right - left)
        val plotHeight = max(1f, bottom - top)

        val minWeek = 14 * 7
        val maxWeek = 40 * 7
        val allValues = (points.map { it.value } + referenceSeries.p10Values + referenceSeries.p90Values)
            .filter { !it.isNaN() && !it.isInfinite() }
        val minValue = allValues.minOrNull() ?: points.minOf { it.value }
        val maxValue = allValues.maxOrNull() ?: points.maxOf { it.value }
        val weekSpan = max(7, maxWeek - minWeek)
        val valuePadding = max(1.0, (maxValue - minValue) * 0.10)
        val yMin = minValue - valuePadding
        val yMax = maxValue + valuePadding
        val valueSpan = max(1.0, yMax - yMin)

        repeat(4) { index ->
            val y = top + plotHeight * index / 3f
            drawLine(
                color = ChestnutPalette.Border.copy(alpha = 0.55f),
                start = Offset(left, y),
                end = Offset(right, y),
                strokeWidth = 1.dp.toPx()
            )
        }
        drawLine(
            color = ChestnutPalette.Border,
            start = Offset(left, top),
            end = Offset(left, bottom),
            strokeWidth = 1.2.dp.toPx()
        )
        drawLine(
            color = ChestnutPalette.Border,
            start = Offset(left, bottom),
            end = Offset(right, bottom),
            strokeWidth = 1.2.dp.toPx()
        )

        fun toOffset(gestationalAgeDays: Int, value: Double): Offset {
            val xRatio = (gestationalAgeDays - minWeek).toFloat() / weekSpan.toFloat()
            val yRatio = ((value - yMin) / valueSpan).toFloat()
            return Offset(
                x = left + plotWidth * xRatio,
                y = bottom - plotHeight * yRatio
            )
        }

        fun drawReference(values: List<Double>, color: Color, strokeWidth: Float) {
            referenceSeries.weeks.zip(values)
                .filter { !it.second.isNaN() && !it.second.isInfinite() }
                .map { toOffset(it.first, it.second) }
                .zipWithNext()
                .forEach { (start, end) ->
                    drawLine(
                        color = color,
                        start = start,
                        end = end,
                        strokeWidth = strokeWidth,
                        cap = StrokeCap.Round
                    )
                }
        }

        drawReference(referenceSeries.p10Values, ChestnutPalette.Blue.copy(alpha = 0.48f), 1.4.dp.toPx())
        drawReference(referenceSeries.p50Values, ChestnutPalette.Green.copy(alpha = 0.58f), 1.8.dp.toPx())
        drawReference(referenceSeries.p90Values, ChestnutPalette.Peach.copy(alpha = 0.48f), 1.4.dp.toPx())

        val offsets = points.map { toOffset(it.gestationalAgeDays, it.value) }
        offsets.zipWithNext().forEach { (start, end) ->
            drawLine(
                color = metric.tone,
                start = start,
                end = end,
                strokeWidth = 3.dp.toPx(),
                cap = StrokeCap.Round
            )
        }
        offsets.forEach { point ->
            drawCircle(
                color = ChestnutPalette.Surface,
                radius = 6.dp.toPx(),
                center = point
            )
            drawCircle(
                color = metric.tone,
                radius = 4.dp.toPx(),
                center = point
            )
        }
    }
}

private fun extractFetalGrowthPoints(
    events: List<BabyLogDomain.BabyLogEvent>,
    metric: FetalGrowthMetric
): List<FetalGrowthPoint> {
    return events
        .asSequence()
        .filter { it.eventType == "ultrasound" }
        .mapNotNull { event ->
            val gestationalAgeDays = if (event.payload.has("gestationalAgeDays")) {
                event.payload.optInt("gestationalAgeDays")
            } else {
                null
            }
            val value = payloadNumber(event.payload, metric.key)
            if (gestationalAgeDays == null || value == null) {
                null
            } else {
                FetalGrowthPoint(gestationalAgeDays, value, event)
            }
        }
        .sortedBy { it.gestationalAgeDays }
        .toList()
}

private fun payloadNumber(payload: JSONObject, key: String): Double? {
    return if (payload.has(key)) BabyLogFormatters.parseOptionalNumber(payload.optString(key, "")) else null
}
