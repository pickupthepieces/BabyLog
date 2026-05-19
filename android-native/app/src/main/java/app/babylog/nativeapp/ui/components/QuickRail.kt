package app.babylog.nativeapp

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitLongPressOrCancellation
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
internal fun PersistentQuickRail(
    actions: List<BabyLogService.QuickAction>,
    voiceState: SmartVoiceUiState,
    onVoiceTap: () -> Unit,
    onVoiceHoldStart: () -> Unit,
    onVoiceHoldEnd: () -> Unit,
    onAction: (BabyLogService.QuickAction) -> Unit
) {
    val currentOnAction by rememberUpdatedState(onAction)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(ChestnutPalette.Surface)
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        VoiceQuickTile(
            voiceState = voiceState,
            onVoiceTap = onVoiceTap,
            onVoiceHoldStart = onVoiceHoldStart,
            onVoiceHoldEnd = onVoiceHoldEnd
        )
        actions.forEach { action ->
            Column(
                modifier = Modifier
                    .width(84.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color(action.toneColor).copy(alpha = 0.16f))
                    .border(1.dp, ChestnutPalette.Border, RoundedCornerShape(18.dp))
                    .clickable { currentOnAction(action) }
                    .padding(vertical = 11.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                BabyLogIconTile(
                    icon = quickActionIcon(action.eventType),
                    tint = Color(action.toneColor),
                    tileColor = Color(action.toneColor).copy(alpha = 0.18f),
                    modifier = Modifier.size(44.dp),
                    iconSize = 28.dp
                )
                Spacer(Modifier.height(5.dp))
                Text(
                    text = action.label,
                    color = ChestnutPalette.Ink,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun VoiceQuickTile(
    voiceState: SmartVoiceUiState,
    onVoiceTap: () -> Unit,
    onVoiceHoldStart: () -> Unit,
    onVoiceHoldEnd: () -> Unit
) {
    val currentOnVoiceTap by rememberUpdatedState(onVoiceTap)
    val currentOnVoiceHoldStart by rememberUpdatedState(onVoiceHoldStart)
    val currentOnVoiceHoldEnd by rememberUpdatedState(onVoiceHoldEnd)
    val transition = rememberInfiniteTransition(label = "voiceQuickPulse")
    val pulseAlpha by transition.animateFloat(
        initialValue = 0.40f,
        targetValue = if (voiceState.isRecording) 1.0f else 0.40f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 520),
            repeatMode = RepeatMode.Reverse
        ),
        label = "voiceQuickPulseAlpha"
    )
    val title = when {
        voiceState.isRecording -> "松开结束"
        voiceState.isTranscribing -> "识别中"
        else -> "按住说话"
    }
    val subtitle = when {
        voiceState.isRecording -> "正在录音"
        voiceState.isTranscribing -> "转写后进入候选"
        else -> "点按文字录入"
    }

    Column(
        modifier = Modifier
            .width(104.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(
                if (voiceState.isRecording) {
                    ChestnutPalette.Primary.copy(alpha = 0.22f)
                } else {
                    ChestnutPalette.Primary.copy(alpha = 0.12f)
                }
            )
            .border(1.dp, ChestnutPalette.Primary.copy(alpha = 0.46f), RoundedCornerShape(18.dp))
            .pointerInput(voiceState.isTranscribing) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val longPress = awaitLongPressOrCancellation(down.id)
                    if (longPress == null) {
                        currentOnVoiceTap()
                        return@awaitEachGesture
                    }
                    if (voiceState.isTranscribing) {
                        waitForUpOrCancellation()
                        return@awaitEachGesture
                    }
                    currentOnVoiceHoldStart()
                    try {
                        waitForUpOrCancellation()
                    } finally {
                        currentOnVoiceHoldEnd()
                    }
                }
            }
            .padding(vertical = 11.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(ChestnutPalette.Primary.copy(alpha = if (voiceState.isRecording) pulseAlpha else 1.0f)),
            contentAlignment = Alignment.Center
        ) {
            BabyLogMaterialIcon(
                icon = LineIcon.Voice,
                tint = Color.White,
                modifier = Modifier.size(27.dp)
            )
        }
        Spacer(Modifier.height(5.dp))
        Text(
            text = title,
            color = ChestnutPalette.Ink,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = subtitle,
            color = ChestnutPalette.Muted,
            fontSize = 10.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
