@file:Suppress("MagicNumber", "TooManyFunctions")

package app.babylog.nativeapp

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val QUICK_RAIL_STAGGER_MILLIS = 34L
private const val QUICK_RAIL_FADE_MILLIS = 220
private const val QUICK_RAIL_SLIDE_MILLIS = 260
private const val QUICK_RAIL_SCALE_MILLIS = 260
private const val QUICK_RAIL_INITIAL_OFFSET_Y = 18f
private const val QUICK_RAIL_INITIAL_SCALE = 0.92f

@Composable
internal fun PersistentQuickRail(
    actions: List<BabyLogService.QuickAction>,
    onAction: (BabyLogService.QuickAction) -> Unit
) {
    val currentOnAction by rememberUpdatedState(onAction)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(ChestnutPalette.Surface.copy(alpha = 0.96f))
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        actions.forEachIndexed { index, action ->
            quickRailItem(
                action = action,
                index = index,
                onAction = currentOnAction
            )
        }
    }
}

@Composable
private fun quickRailItem(
    action: BabyLogService.QuickAction,
    index: Int,
    onAction: (BabyLogService.QuickAction) -> Unit
) {
    val interactionSource = remember(action.eventType) { MutableInteractionSource() }
    val tone = Color(action.toneColor)
    val motion = rememberQuickRailMotion(
        eventType = action.eventType,
        index = index
    )
    Column(
        modifier = Modifier
            .width(62.dp)
            .graphicsLayer {
                alpha = motion.alpha
                translationY = motion.offsetY
                scaleX = motion.scale
                scaleY = motion.scale
                transformOrigin = TransformOrigin(0.5f, 1f)
            }
            .babyLogPressScale(interactionSource)
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) { onAction(action) }
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(tone.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center
        ) {
            quickRailGlyph(
                eventType = action.eventType,
                tint = tone,
                modifier = Modifier.size(25.dp)
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = action.label,
            color = ChestnutPalette.Muted,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun rememberQuickRailMotion(
    eventType: String,
    index: Int
): QuickRailMotion {
    val entranceAlpha = remember(eventType) { Animatable(0f) }
    val entranceOffset = remember(eventType) { Animatable(QUICK_RAIL_INITIAL_OFFSET_Y) }
    val entranceScale = remember(eventType) { Animatable(QUICK_RAIL_INITIAL_SCALE) }
    LaunchedEffect(eventType) {
        delay(index * QUICK_RAIL_STAGGER_MILLIS)
        launch {
            entranceAlpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = QUICK_RAIL_FADE_MILLIS,
                    easing = FastOutSlowInEasing
                )
            )
        }
        launch {
            entranceOffset.animateTo(
                targetValue = 0f,
                animationSpec = tween(
                    durationMillis = QUICK_RAIL_SLIDE_MILLIS,
                    easing = FastOutSlowInEasing
                )
            )
        }
        launch {
            entranceScale.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = QUICK_RAIL_SCALE_MILLIS,
                    easing = FastOutSlowInEasing
                )
            )
        }
    }
    return QuickRailMotion(
        alpha = entranceAlpha.value,
        offsetY = entranceOffset.value,
        scale = entranceScale.value
    )
}

private data class QuickRailMotion(
    val alpha: Float,
    val offsetY: Float,
    val scale: Float
)

@Composable
private fun quickRailGlyph(
    eventType: String,
    tint: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        quickRailGlyphKind(eventType).draw(this, tint)
    }
}

private fun quickRailGlyphKind(eventType: String): QuickRailGlyphKind {
    return quickRailGlyphKinds[eventType] ?: QuickRailGlyphKind.File
}

private val quickRailGlyphKinds = mapOf(
    "ultrasound" to QuickRailGlyphKind.Ultrasound,
    "pregnancy_checkup" to QuickRailGlyphKind.Checkup,
    "screening_nt" to QuickRailGlyphKind.Nt,
    "screening_serum" to QuickRailGlyphKind.Serum,
    "screening_nipt" to QuickRailGlyphKind.Nipt,
    "screening_anomaly" to QuickRailGlyphKind.Anomaly,
    "screening_ogtt" to QuickRailGlyphKind.Ogtt,
    "screening_gbs" to QuickRailGlyphKind.Gbs,
    "screening_nst" to QuickRailGlyphKind.Nst,
    "fetal_movement" to QuickRailGlyphKind.Movement,
    "contraction" to QuickRailGlyphKind.Contraction,
    "maternal_metric" to QuickRailGlyphKind.Metric,
    "growth" to QuickRailGlyphKind.Metric,
    "child_checkup" to QuickRailGlyphKind.Checkup,
    "breastfeed" to QuickRailGlyphKind.Breastfeed,
    "feed" to QuickRailGlyphKind.Breastfeed,
    "bottle" to QuickRailGlyphKind.Bottle,
    "sleep" to QuickRailGlyphKind.Sleep,
    "wake" to QuickRailGlyphKind.Wake,
    "pee" to QuickRailGlyphKind.Pee,
    "diaper" to QuickRailGlyphKind.Pee,
    "poop" to QuickRailGlyphKind.Poop,
    "temperature" to QuickRailGlyphKind.Temperature,
    "medication" to QuickRailGlyphKind.Medication
)

private enum class QuickRailGlyphKind(val draw: DrawScope.(Color) -> Unit) {
    Ultrasound({ color -> drawUltrasoundGlyph(color) }),
    Checkup({ color -> drawCheckupGlyph(color) }),
    Nt({ color -> drawNtGlyph(color) }),
    Serum({ color -> drawSerumGlyph(color) }),
    Nipt({ color -> drawNiptGlyph(color) }),
    Anomaly({ color -> drawAnomalyGlyph(color) }),
    Ogtt({ color -> drawOgttGlyph(color) }),
    Gbs({ color -> drawGbsGlyph(color) }),
    Nst({ color -> drawNstGlyph(color) }),
    Movement({ color -> drawMovementGlyph(color) }),
    Contraction({ color -> drawContractionGlyph(color) }),
    Metric({ color -> drawMetricGlyph(color) }),
    Breastfeed({ color -> drawHeartGlyph(color) }),
    Bottle({ color -> drawBottleGlyph(color) }),
    Sleep({ color -> drawSleepGlyph(color) }),
    Wake({ color -> drawWakeGlyph(color) }),
    Pee({ color -> drawDropGlyph(color) }),
    Poop({ color -> drawPoopGlyph(color) }),
    Temperature({ color -> drawTemperatureGlyph(color) }),
    Medication({ color -> drawPillGlyph(color) }),
    File({ color -> drawFileGlyph(color) })
}

private fun DrawScope.railStroke(widthRatio: Float = 0.085f): Stroke {
    return Stroke(
        width = size.minDimension * widthRatio,
        cap = StrokeCap.Round,
        join = StrokeJoin.Round
    )
}

// B 超：探头扇形（两条边线 + 底弧）
private fun DrawScope.drawUltrasoundGlyph(tint: Color) {
    val stroke = railStroke()
    val w = size.width
    val h = size.height
    drawLine(tint, Offset(w * 0.50f, h * 0.16f), Offset(w * 0.235f, h * 0.62f), strokeWidth = stroke.width, cap = StrokeCap.Round)
    drawLine(tint, Offset(w * 0.50f, h * 0.16f), Offset(w * 0.765f, h * 0.62f), strokeWidth = stroke.width, cap = StrokeCap.Round)
    drawArc(
        color = tint,
        startAngle = 60f,
        sweepAngle = 60f,
        useCenter = false,
        topLeft = Offset(w * 0.50f - w * 0.53f, h * 0.16f - h * 0.53f),
        size = Size(w * 1.06f, h * 1.06f),
        style = stroke
    )
    drawArc(
        color = tint,
        startAngle = 60f,
        sweepAngle = 60f,
        useCenter = false,
        topLeft = Offset(w * 0.50f - w * 0.30f, h * 0.16f - h * 0.30f),
        size = Size(w * 0.60f, h * 0.60f),
        style = stroke
    )
}

// 产检：写字板 + 对勾
private fun DrawScope.drawCheckupGlyph(tint: Color) {
    val stroke = railStroke()
    val w = size.width
    val h = size.height
    drawRoundRect(
        color = tint,
        topLeft = Offset(w * 0.26f, h * 0.14f),
        size = Size(w * 0.48f, h * 0.72f),
        cornerRadius = CornerRadius(w * 0.10f, h * 0.10f),
        style = stroke
    )
    drawLine(tint, Offset(w * 0.38f, h * 0.50f), Offset(w * 0.47f, h * 0.60f), strokeWidth = stroke.width, cap = StrokeCap.Round)
    drawLine(tint, Offset(w * 0.47f, h * 0.60f), Offset(w * 0.64f, h * 0.40f), strokeWidth = stroke.width, cap = StrokeCap.Round)
}

// NT：颈部弧线 + 卡尺测量线
private fun DrawScope.drawNtGlyph(tint: Color) {
    val stroke = railStroke()
    val w = size.width
    val h = size.height
    val curve = Path().apply {
        moveTo(w * 0.24f, h * 0.40f)
        cubicTo(w * 0.41f, h * 0.20f, w * 0.59f, h * 0.20f, w * 0.76f, h * 0.40f)
    }
    drawPath(curve, tint, style = stroke)
    drawLine(tint, Offset(w * 0.28f, h * 0.68f), Offset(w * 0.72f, h * 0.68f), strokeWidth = stroke.width, cap = StrokeCap.Round)
    drawLine(tint, Offset(w * 0.28f, h * 0.58f), Offset(w * 0.28f, h * 0.78f), strokeWidth = stroke.width, cap = StrokeCap.Round)
    drawLine(tint, Offset(w * 0.72f, h * 0.58f), Offset(w * 0.72f, h * 0.78f), strokeWidth = stroke.width, cap = StrokeCap.Round)
}

// 唐筛：试管 + 液面线
private fun DrawScope.drawSerumGlyph(tint: Color) {
    val stroke = railStroke()
    val w = size.width
    val h = size.height
    val tube = Path().apply {
        moveTo(w * 0.38f, h * 0.14f)
        lineTo(w * 0.38f, h * 0.64f)
        cubicTo(w * 0.38f, h * 0.86f, w * 0.62f, h * 0.86f, w * 0.62f, h * 0.64f)
        lineTo(w * 0.62f, h * 0.14f)
    }
    drawPath(tube, tint, style = stroke)
    drawLine(tint, Offset(w * 0.38f, h * 0.52f), Offset(w * 0.62f, h * 0.52f), strokeWidth = stroke.width, cap = StrokeCap.Round)
}

// 无创 DNA：两条交叉螺旋线
private fun DrawScope.drawNiptGlyph(tint: Color) {
    val stroke = railStroke()
    val w = size.width
    val h = size.height
    val left = Path().apply {
        moveTo(w * 0.36f, h * 0.14f)
        cubicTo(w * 0.74f, h * 0.34f, w * 0.26f, h * 0.62f, w * 0.64f, h * 0.86f)
    }
    val right = Path().apply {
        moveTo(w * 0.64f, h * 0.14f)
        cubicTo(w * 0.26f, h * 0.34f, w * 0.74f, h * 0.62f, w * 0.36f, h * 0.86f)
    }
    drawPath(left, tint, style = stroke)
    drawPath(right, tint, style = stroke)
}

// 大排畸：放大镜
private fun DrawScope.drawAnomalyGlyph(tint: Color) {
    val stroke = railStroke()
    val w = size.width
    val h = size.height
    drawCircle(tint, radius = w * 0.23f, center = Offset(w * 0.43f, h * 0.41f), style = stroke)
    drawLine(tint, Offset(w * 0.60f, h * 0.58f), Offset(w * 0.80f, h * 0.78f), strokeWidth = stroke.width, cap = StrokeCap.Round)
}

// 糖耐：杯子 + 吸管
private fun DrawScope.drawOgttGlyph(tint: Color) {
    val stroke = railStroke()
    val w = size.width
    val h = size.height
    val cup = Path().apply {
        moveTo(w * 0.26f, h * 0.34f)
        lineTo(w * 0.33f, h * 0.82f)
        lineTo(w * 0.67f, h * 0.82f)
        lineTo(w * 0.74f, h * 0.34f)
        close()
    }
    drawPath(cup, tint, style = stroke)
    drawLine(tint, Offset(w * 0.70f, h * 0.12f), Offset(w * 0.56f, h * 0.42f), strokeWidth = stroke.width, cap = StrokeCap.Round)
}

// GBS：棉签（斜杆 + 圆头）
private fun DrawScope.drawGbsGlyph(tint: Color) {
    val stroke = railStroke()
    val w = size.width
    val h = size.height
    drawLine(tint, Offset(w * 0.28f, h * 0.80f), Offset(w * 0.62f, h * 0.38f), strokeWidth = stroke.width, cap = StrokeCap.Round)
    drawCircle(tint, radius = w * 0.12f, center = Offset(w * 0.68f, h * 0.30f), style = stroke)
}

// 胎心监护：单条心电折线
private fun DrawScope.drawNstGlyph(tint: Color) {
    val stroke = railStroke()
    val w = size.width
    val h = size.height
    val wave = Path().apply {
        moveTo(w * 0.12f, h * 0.52f)
        lineTo(w * 0.34f, h * 0.52f)
        lineTo(w * 0.44f, h * 0.28f)
        lineTo(w * 0.56f, h * 0.74f)
        lineTo(w * 0.66f, h * 0.52f)
        lineTo(w * 0.88f, h * 0.52f)
    }
    drawPath(wave, tint, style = stroke)
}

// 胎动：圆点 + 两道扩散弧
private fun DrawScope.drawMovementGlyph(tint: Color) {
    val stroke = railStroke()
    val w = size.width
    val h = size.height
    drawCircle(tint, radius = w * 0.08f, center = Offset(w * 0.32f, h * 0.50f))
    drawArc(
        color = tint,
        startAngle = -52f,
        sweepAngle = 104f,
        useCenter = false,
        topLeft = Offset(w * 0.32f - w * 0.22f, h * 0.50f - h * 0.22f),
        size = Size(w * 0.44f, h * 0.44f),
        style = stroke
    )
    drawArc(
        color = tint,
        startAngle = -52f,
        sweepAngle = 104f,
        useCenter = false,
        topLeft = Offset(w * 0.32f - w * 0.38f, h * 0.50f - h * 0.38f),
        size = Size(w * 0.76f, h * 0.76f),
        style = stroke
    )
}

// 宫缩：秒表（表盘 + 单针 + 顶钮）
private fun DrawScope.drawContractionGlyph(tint: Color) {
    val stroke = railStroke()
    val w = size.width
    val h = size.height
    drawCircle(tint, radius = w * 0.30f, center = Offset(w * 0.50f, h * 0.58f), style = stroke)
    drawLine(tint, Offset(w * 0.50f, h * 0.58f), Offset(w * 0.50f, h * 0.38f), strokeWidth = stroke.width, cap = StrokeCap.Round)
    drawLine(tint, Offset(w * 0.50f, h * 0.12f), Offset(w * 0.50f, h * 0.22f), strokeWidth = stroke.width, cap = StrokeCap.Round)
}

// 孕妈指标：仪表盘（半圆弧 + 指针）
private fun DrawScope.drawMetricGlyph(tint: Color) {
    val stroke = railStroke()
    val w = size.width
    val h = size.height
    drawArc(
        color = tint,
        startAngle = 180f,
        sweepAngle = 180f,
        useCenter = false,
        topLeft = Offset(w * 0.16f, h * 0.30f),
        size = Size(w * 0.68f, h * 0.68f),
        style = stroke
    )
    drawLine(tint, Offset(w * 0.50f, h * 0.64f), Offset(w * 0.67f, h * 0.42f), strokeWidth = stroke.width, cap = StrokeCap.Round)
}

private fun DrawScope.drawHeartGlyph(tint: Color) {
    val stroke = railStroke(0.085f)
    val w = size.width
    val h = size.height
    val path = Path().apply {
        moveTo(w * 0.50f, h * 0.76f)
        cubicTo(w * 0.20f, h * 0.58f, w * 0.22f, h * 0.30f, w * 0.39f, h * 0.33f)
        cubicTo(w * 0.47f, h * 0.34f, w * 0.50f, h * 0.44f, w * 0.50f, h * 0.44f)
        cubicTo(w * 0.50f, h * 0.44f, w * 0.53f, h * 0.34f, w * 0.61f, h * 0.33f)
        cubicTo(w * 0.78f, h * 0.30f, w * 0.80f, h * 0.58f, w * 0.50f, h * 0.76f)
    }
    drawPath(path, color = tint, style = stroke)
}

private fun DrawScope.drawBottleGlyph(tint: Color) {
    val stroke = railStroke()
    val w = size.width
    val h = size.height
    drawRoundRect(tint, topLeft = Offset(w * 0.34f, h * 0.20f), size = Size(w * 0.32f, h * 0.12f), cornerRadius = CornerRadius(w * 0.04f), style = stroke)
    drawRoundRect(tint, topLeft = Offset(w * 0.28f, h * 0.32f), size = Size(w * 0.44f, h * 0.52f), cornerRadius = CornerRadius(w * 0.12f), style = stroke)
    drawLine(tint, Offset(w * 0.37f, h * 0.54f), Offset(w * 0.63f, h * 0.54f), strokeWidth = stroke.width, cap = StrokeCap.Round)
}

private fun DrawScope.drawSleepGlyph(tint: Color) {
    val stroke = railStroke(0.085f)
    val w = size.width
    val h = size.height
    drawArc(tint, startAngle = 76f, sweepAngle = 255f, useCenter = false, topLeft = Offset(w * 0.24f, h * 0.16f), size = Size(w * 0.50f, h * 0.68f), style = stroke)
    drawLine(tint, Offset(w * 0.66f, h * 0.21f), Offset(w * 0.82f, h * 0.21f), strokeWidth = stroke.width, cap = StrokeCap.Round)
    drawLine(tint, Offset(w * 0.82f, h * 0.21f), Offset(w * 0.66f, h * 0.36f), strokeWidth = stroke.width, cap = StrokeCap.Round)
    drawLine(tint, Offset(w * 0.66f, h * 0.36f), Offset(w * 0.82f, h * 0.36f), strokeWidth = stroke.width, cap = StrokeCap.Round)
}

private fun DrawScope.drawWakeGlyph(tint: Color) {
    val stroke = railStroke()
    val w = size.width
    val h = size.height
    drawCircle(tint, radius = w * 0.18f, center = Offset(w * 0.50f, h * 0.50f), style = stroke)
    listOf(
        Offset(0.50f, 0.12f) to Offset(0.50f, 0.24f),
        Offset(0.50f, 0.76f) to Offset(0.50f, 0.88f),
        Offset(0.12f, 0.50f) to Offset(0.24f, 0.50f),
        Offset(0.76f, 0.50f) to Offset(0.88f, 0.50f),
        Offset(0.24f, 0.24f) to Offset(0.32f, 0.32f),
        Offset(0.76f, 0.24f) to Offset(0.68f, 0.32f),
        Offset(0.24f, 0.76f) to Offset(0.32f, 0.68f),
        Offset(0.76f, 0.76f) to Offset(0.68f, 0.68f)
    ).forEach { (start, end) ->
        drawLine(tint, Offset(w * start.x, h * start.y), Offset(w * end.x, h * end.y), strokeWidth = stroke.width, cap = StrokeCap.Round)
    }
}

private fun DrawScope.drawDropGlyph(tint: Color) {
    val stroke = railStroke(0.085f)
    val w = size.width
    val h = size.height
    val path = Path().apply {
        moveTo(w * 0.50f, h * 0.14f)
        cubicTo(w * 0.28f, h * 0.40f, w * 0.22f, h * 0.54f, w * 0.28f, h * 0.70f)
        cubicTo(w * 0.36f, h * 0.88f, w * 0.64f, h * 0.88f, w * 0.72f, h * 0.70f)
        cubicTo(w * 0.78f, h * 0.54f, w * 0.72f, h * 0.40f, w * 0.50f, h * 0.14f)
    }
    drawPath(path, tint, style = stroke)
}

private fun DrawScope.drawPoopGlyph(tint: Color) {
    val stroke = railStroke(0.085f)
    val w = size.width
    val h = size.height
    drawArc(tint, startAngle = 195f, sweepAngle = 270f, useCenter = false, topLeft = Offset(w * 0.24f, h * 0.18f), size = Size(w * 0.52f, h * 0.36f), style = stroke)
    drawArc(tint, startAngle = 190f, sweepAngle = 230f, useCenter = false, topLeft = Offset(w * 0.16f, h * 0.36f), size = Size(w * 0.68f, h * 0.34f), style = stroke)
    drawArc(tint, startAngle = 180f, sweepAngle = 180f, useCenter = false, topLeft = Offset(w * 0.10f, h * 0.56f), size = Size(w * 0.80f, h * 0.30f), style = stroke)
}

private fun DrawScope.drawTemperatureGlyph(tint: Color) {
    val stroke = railStroke()
    val w = size.width
    val h = size.height
    drawRoundRect(tint, topLeft = Offset(w * 0.43f, h * 0.14f), size = Size(w * 0.14f, h * 0.48f), cornerRadius = CornerRadius(w * 0.07f), style = stroke)
    drawCircle(tint, radius = w * 0.15f, center = Offset(w * 0.50f, h * 0.72f), style = stroke)
    drawLine(tint, Offset(w * 0.50f, h * 0.32f), Offset(w * 0.50f, h * 0.72f), strokeWidth = stroke.width, cap = StrokeCap.Round)
}

private fun DrawScope.drawPillGlyph(tint: Color) {
    val stroke = railStroke()
    val w = size.width
    val h = size.height
    drawRoundRect(tint, topLeft = Offset(w * 0.16f, h * 0.34f), size = Size(w * 0.68f, h * 0.32f), cornerRadius = CornerRadius(w * 0.18f), style = stroke)
    drawLine(tint, Offset(w * 0.50f, h * 0.36f), Offset(w * 0.50f, h * 0.64f), strokeWidth = stroke.width, cap = StrokeCap.Round)
}

private fun DrawScope.drawFileGlyph(tint: Color) {
    val stroke = railStroke()
    val w = size.width
    val h = size.height
    drawRoundRect(tint, topLeft = Offset(w * 0.24f, h * 0.14f), size = Size(w * 0.52f, h * 0.72f), cornerRadius = CornerRadius(w * 0.08f), style = stroke)
    drawLine(tint, Offset(w * 0.36f, h * 0.48f), Offset(w * 0.64f, h * 0.48f), strokeWidth = stroke.width, cap = StrokeCap.Round)
    drawLine(tint, Offset(w * 0.36f, h * 0.62f), Offset(w * 0.58f, h * 0.62f), strokeWidth = stroke.width, cap = StrokeCap.Round)
}
