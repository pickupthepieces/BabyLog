@file:Suppress("InvalidPackageDeclaration", "MagicNumber")

package app.babylog.nativeapp

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

private const val RECORD_SHEET_HALF_FRACTION = 0.58f
private const val RECORD_SHEET_MOTION_MILLIS = 260

@Composable
internal fun recordEntrySheet(
    state: RecordEntrySheetState,
    onExpandedChange: (Boolean) -> Unit,
    onDismiss: () -> Unit,
    onDismissAnimationEnd: () -> Unit,
    content: @Composable (String) -> Unit
) {
    val route = state.route
    if (route == null) return
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val dragThresholdPx = with(density) { 54.dp.toPx() }
        var dragOffsetPx by remember(route) { mutableStateOf(0f) }
        val sheetHeight by animateDpAsState(
            targetValue = when {
                !state.visible -> 0.dp
                state.expanded -> maxHeight
                else -> maxHeight * RECORD_SHEET_HALF_FRACTION
            },
            animationSpec = tween(durationMillis = RECORD_SHEET_MOTION_MILLIS, easing = FastOutSlowInEasing),
            label = "recordEntrySheetHeight"
        )
        LaunchedEffect(state.expanded, route) {
            dragOffsetPx = 0f
        }
        LaunchedEffect(state.visible, route) {
            if (!state.visible) {
                delay(RECORD_SHEET_MOTION_MILLIS.toLong())
                onDismissAnimationEnd()
            }
        }
        recordSheetScrim(
            visible = state.visible,
            expanded = state.expanded,
            onDismiss = onDismiss,
            modifier = Modifier.matchParentSize()
        )
        recordSheetPanel(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(sheetHeight),
            motion = RecordSheetMotion(
                expanded = state.expanded,
                dragOffsetPx = dragOffsetPx,
                dragThresholdPx = dragThresholdPx
            ),
            actions = RecordSheetGestureActions(
                onExpandedChange = onExpandedChange,
                onDismiss = onDismiss,
                onDragOffsetChange = { dragOffsetPx += it },
                onDragEnd = {
                    when {
                        dragOffsetPx < -dragThresholdPx -> onExpandedChange(true)
                        dragOffsetPx > dragThresholdPx && state.expanded -> onExpandedChange(false)
                        dragOffsetPx > dragThresholdPx -> onDismiss()
                    }
                    dragOffsetPx = 0f
                }
            )
        ) {
            content(route)
        }
    }
}

internal data class RecordEntrySheetState(
    val route: String?,
    val visible: Boolean,
    val expanded: Boolean
)

@Composable
private fun recordSheetScrim(
    visible: Boolean,
    expanded: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrimAlpha by animateFloatAsState(
        targetValue = if (!visible) 0f else if (expanded) 0.20f else 0.10f,
        animationSpec = tween(durationMillis = RECORD_SHEET_MOTION_MILLIS, easing = FastOutSlowInEasing),
        label = "recordSheetScrimAlpha"
    )
    Box(
        modifier = modifier
            .background(Color.Black.copy(alpha = scrimAlpha))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                enabled = visible,
                onClick = onDismiss
            )
    )
}

@Composable
private fun recordSheetPanel(
    modifier: Modifier,
    motion: RecordSheetMotion,
    actions: RecordSheetGestureActions,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = modifier
            .navigationBarsPadding()
            .imePadding()
            .graphicsLayer {
                translationY = motion.dragOffsetPx.coerceIn(-motion.dragThresholdPx, motion.dragThresholdPx)
            },
        color = ChestnutPalette.Bg,
        shape = RoundedCornerShape(topStart = ChestnutRadius.Sheet, topEnd = ChestnutRadius.Sheet),
        elevation = 14.dp
    ) {
        Column(modifier = recordSheetContentModifier(motion.expanded)) {
            recordSheetHandle(
                expanded = motion.expanded,
                onExpandedChange = actions.onExpandedChange,
                onDragOffsetChange = actions.onDragOffsetChange,
                onDragEnd = actions.onDragEnd
            )
            Box(modifier = Modifier.weight(1f)) {
                content()
            }
        }
    }
}

private data class RecordSheetMotion(
    val expanded: Boolean,
    val dragOffsetPx: Float,
    val dragThresholdPx: Float
)

private data class RecordSheetGestureActions(
    val onExpandedChange: (Boolean) -> Unit,
    val onDismiss: () -> Unit,
    val onDragOffsetChange: (Float) -> Unit,
    val onDragEnd: () -> Unit
)

private fun recordSheetContentModifier(expanded: Boolean): Modifier {
    return if (expanded) {
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
    } else {
        Modifier.fillMaxSize()
    }
}

@Composable
private fun recordSheetHandle(
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onDragOffsetChange: (Float) -> Unit,
    onDragEnd: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(32.dp)
            .pointerInput(expanded) {
                detectVerticalDragGestures(
                    onVerticalDrag = { _, dragAmount -> onDragOffsetChange(dragAmount) },
                    onDragEnd = onDragEnd
                )
            }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = { onExpandedChange(!expanded) }
            ),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(width = 42.dp, height = 5.dp)
                .clip(RoundedCornerShape(ChestnutRadius.Sheet))
                .background(ChestnutPalette.Border.copy(alpha = 0.95f))
        )
    }
}
