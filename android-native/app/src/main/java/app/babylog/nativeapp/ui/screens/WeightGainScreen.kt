package app.babylog.nativeapp

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.max
import kotlin.math.min

private data class WeightGainPoint(
    val gestationalAgeDays: Int,
    val weightKg: Double,
    val occurredAt: String
)

@Composable
internal fun WeightGainScreen(
    profile: BabyLogDomain.ChildProfile,
    events: List<BabyLogDomain.BabyLogEvent>,
    onBack: () -> Unit,
    onEditProfile: () -> Unit
) {
    val preWeight = profile.prePregnancyWeightKg
    val heightCm = profile.heightCm
    val recommendation = if (preWeight != null && heightCm != null) {
        remember(preWeight, heightCm) {
            BabyLogWeightGainCalculator.recommendation(preWeight, heightCm)
        }
    } else {
        null
    }
    val currentGestationalAge = currentGestationalAgeInput(profile)
    val currentGestationalDays = BabyLogFormatters.parseGestationalAgeDays(currentGestationalAge)
    val points = remember(events, profile.expectedDueDate) {
        extractWeightGainPoints(events, profile.expectedDueDate)
    }
    val latestPoint = points.maxByOrNull { it.occurredAt }
    val currentWeight = latestPoint?.weightKg
    val currentGain = if (preWeight != null && currentWeight != null) {
        BabyLogWeightGainCalculator.cumulativeGainKg(currentWeight, preWeight)
    } else {
        null
    }
    val currentRange = if (recommendation != null && currentGestationalDays != null) {
        BabyLogWeightGainCalculator.recommendedGainRangeKg(recommendation, currentGestationalDays)
    } else {
        null
    }

    SettingsPageScaffold(
        title = "孕期增重曲线",
        subtitle = "IOM 参考区间，便于复诊沟通",
        onBack = onBack
    ) {
        item {
            Text(
                text = "展示公开参考区间，便于复诊时对照体重历史。",
                color = Color(0xFF7C4A21),
                fontSize = 12.sp,
                lineHeight = 18.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFFFEBCB), RoundedCornerShape(ChestnutRadius.Small))
                    .padding(12.dp)
            )
        }

        if (preWeight == null || heightCm == null || recommendation == null) {
            item {
                Panel {
                    SectionHeader("档案待补")
                    Text(
                        "补充孕前体重和身高后，将显示 IOM 参考带与体重历史。",
                        color = ChestnutPalette.Muted,
                        fontSize = 14.sp,
                        lineHeight = 21.sp
                    )
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = onEditProfile,
                        colors = ButtonDefaults.buttonColors(backgroundColor = ChestnutPalette.Primary)
                    ) {
                        Text("去补充档案", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
            return@SettingsPageScaffold
        }

        item {
            Panel {
                SectionHeader("当前参考")
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    MetricCard(
                        title = "当前孕周",
                        value = currentGestationalAge.ifBlank { "待补" },
                        subtitle = "来自档案预产期",
                        tone = ChestnutPalette.Primary,
                        modifier = Modifier.weight(1f)
                    )
                    MetricCard(
                        title = "当前体重",
                        value = currentWeight?.let { "${BabyLogFormatters.formatNumber(it)} kg" } ?: "暂无",
                        subtitle = "来自孕妈指标",
                        tone = ChestnutPalette.Blue,
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    MetricCard(
                        title = "累计增重",
                        value = currentGain?.let { "${BabyLogFormatters.formatNumber(it)} kg" } ?: "暂无",
                        subtitle = "当前体重 - 孕前体重",
                        tone = ChestnutPalette.Peach,
                        modifier = Modifier.weight(1f)
                    )
                    MetricCard(
                        title = "IOM 总区间",
                        value = recommendation.rangeLabel(),
                        subtitle = "${recommendation.categoryLabel} · BMI ${BabyLogFormatters.formatNumber(recommendation.bmi)}",
                        tone = ChestnutPalette.Green,
                        modifier = Modifier.weight(1f)
                    )
                }
                currentRange?.let {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = "当前孕周参考累计增重：${it.label()}。",
                        color = ChestnutPalette.Muted,
                        fontSize = 12.sp,
                        lineHeight = 18.sp
                    )
                }
            }
        }

        item {
            Panel {
                SectionHeader("曲线")
                Spacer(Modifier.height(10.dp))
                if (!BabyLogFormatters.isValidDateInput(profile.expectedDueDate)) {
                    EmptyPanel("补充预产期后显示孕周曲线")
                } else {
                    WeightGainChart(
                        prePregnancyWeightKg = preWeight,
                        recommendation = recommendation,
                        points = points
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(Modifier.fillMaxWidth()) {
                        Text("0 周", color = ChestnutPalette.Text3, fontSize = 11.sp)
                        Spacer(Modifier.weight(1f))
                        Text("40 周", color = ChestnutPalette.Text3, fontSize = 11.sp)
                    }
                    Text(
                        text = if (points.isEmpty()) {
                            "保存孕妈体重后显示曲线点。"
                        } else {
                            "已记录 ${points.size} 个体重点；浅色区域为 IOM 参考带。"
                        },
                        color = ChestnutPalette.Text3,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun WeightGainChart(
    prePregnancyWeightKg: Double,
    recommendation: BabyLogWeightGainCalculator.Recommendation,
    points: List<WeightGainPoint>
) {
    val referenceDays = remember { (0..40).map { it * 7 } }
    val lower = remember(recommendation) {
        referenceDays.map { day ->
            prePregnancyWeightKg + BabyLogWeightGainCalculator.recommendedGainRangeKg(recommendation, day).minKg
        }
    }
    val upper = remember(recommendation) {
        referenceDays.map { day ->
            prePregnancyWeightKg + BabyLogWeightGainCalculator.recommendedGainRangeKg(recommendation, day).maxKg
        }
    }
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(230.dp)
            .background(ChestnutPalette.Bg.copy(alpha = 0.74f), RoundedCornerShape(ChestnutRadius.Control))
            .padding(12.dp)
    ) {
        val left = 34.dp.toPx()
        val top = 18.dp.toPx()
        val right = size.width - 14.dp.toPx()
        val bottom = size.height - 30.dp.toPx()
        val plotWidth = max(1f, right - left)
        val plotHeight = max(1f, bottom - top)
        val minDay = 0
        val maxDay = 40 * 7
        val allValues = lower + upper + points.map { it.weightKg } + listOf(prePregnancyWeightKg)
        val rawMin = allValues.minOrNull() ?: prePregnancyWeightKg
        val rawMax = allValues.maxOrNull() ?: (prePregnancyWeightKg + recommendation.totalGainMaxKg)
        val padding = max(1.0, (rawMax - rawMin) * 0.12)
        val yMin = rawMin - padding
        val yMax = rawMax + padding
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
        drawLine(ChestnutPalette.Border, Offset(left, top), Offset(left, bottom), 1.2.dp.toPx())
        drawLine(ChestnutPalette.Border, Offset(left, bottom), Offset(right, bottom), 1.2.dp.toPx())

        fun toOffset(day: Int, weightKg: Double): Offset {
            val x = left + plotWidth * ((day - minDay).toFloat() / (maxDay - minDay).toFloat())
            val y = bottom - plotHeight * (((weightKg - yMin) / valueSpan).toFloat())
            return Offset(x, y)
        }

        val upperOffsets = referenceDays.zip(upper).map { toOffset(it.first, it.second) }
        val lowerOffsets = referenceDays.zip(lower).map { toOffset(it.first, it.second) }
        val bandPath = Path().apply {
            if (upperOffsets.isNotEmpty()) {
                moveTo(upperOffsets.first().x, upperOffsets.first().y)
                upperOffsets.drop(1).forEach { lineTo(it.x, it.y) }
                lowerOffsets.asReversed().forEach { lineTo(it.x, it.y) }
                close()
            }
        }
        drawPath(bandPath, ChestnutPalette.Primary.copy(alpha = 0.14f))

        fun drawSeries(values: List<Double>, color: Color, width: Float) {
            referenceDays.zip(values)
                .map { toOffset(it.first, it.second) }
                .zipWithNext()
                .forEach { (start, end) ->
                    drawLine(color, start, end, width, cap = StrokeCap.Round)
                }
        }
        drawSeries(lower, ChestnutPalette.Primary.copy(alpha = 0.38f), 1.3.dp.toPx())
        drawSeries(upper, ChestnutPalette.Primary.copy(alpha = 0.38f), 1.3.dp.toPx())

        val visiblePoints = points
            .filter { it.gestationalAgeDays in minDay..maxDay }
            .sortedBy { it.gestationalAgeDays }
        visiblePoints
            .map { toOffset(it.gestationalAgeDays, it.weightKg) }
            .zipWithNext()
            .forEach { (start, end) ->
                drawLine(ChestnutPalette.Blue, start, end, 2.4.dp.toPx(), cap = StrokeCap.Round)
            }
        visiblePoints.forEach { point ->
            val offset = toOffset(point.gestationalAgeDays, point.weightKg)
            drawCircle(ChestnutPalette.Blue, 4.2.dp.toPx(), offset)
        }
    }
}

private fun extractWeightGainPoints(
    events: List<BabyLogDomain.BabyLogEvent>,
    expectedDueDate: String
): List<WeightGainPoint> {
    if (!BabyLogFormatters.isValidDateInput(expectedDueDate)) {
        return emptyList()
    }
    return events.mapNotNull { event ->
        if (event.eventType != "maternal_metric" || !event.payload.has("weightKg")) {
            return@mapNotNull null
        }
        val weight = event.payload.optDouble("weightKg", Double.NaN)
        if (weight.isNaN() || weight.isInfinite() || weight <= 0.0) {
            return@mapNotNull null
        }
        val day = BabyLogFormatters.recordDay(event.occurredAt)
        val gestationalAge = gestationalAgeInputForDate(expectedDueDate, day)
        val gestationalDays = BabyLogFormatters.parseGestationalAgeDays(gestationalAge) ?: return@mapNotNull null
        WeightGainPoint(gestationalDays, weight, event.occurredAt)
    }.sortedBy { it.gestationalAgeDays }
}
