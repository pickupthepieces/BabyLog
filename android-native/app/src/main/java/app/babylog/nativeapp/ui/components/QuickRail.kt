package app.babylog.nativeapp

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Text
import androidx.compose.material.Surface
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
internal fun VoiceEntryRail(
    voiceState: SmartVoiceUiState,
    onTextEntry: () -> Unit,
    onVoiceHoldStart: () -> Unit,
    onVoiceHoldEnd: () -> Unit
) {
    val currentOnVoiceHoldStart by rememberUpdatedState(onVoiceHoldStart)
    val currentOnVoiceHoldEnd by rememberUpdatedState(onVoiceHoldEnd)
    val voiceEnabled = !voiceState.isTranscribing
    val transition = rememberInfiniteTransition(label = "voiceRailPulse")
    val pulseAlpha by transition.animateFloat(
        initialValue = 0.40f,
        targetValue = if (voiceState.isRecording) 1.0f else 0.40f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 520),
            repeatMode = RepeatMode.Reverse
        ),
        label = "voiceRailPulseAlpha"
    )
    val title = when {
        voiceState.isRecording -> "正在录音"
        voiceState.isTranscribing -> "正在转文字"
        else -> "按住说话"
    }
    val subtitle = when {
        voiceState.isRecording -> "松开后转写并进入智能解析"
        voiceState.isTranscribing -> "识别完成后会回填到智能录入页"
        else -> "普通话录入，AI 只生成候选，确认后才保存"
    }

    Surface(
        color = ChestnutPalette.Surface,
        elevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, ChestnutPalette.Border)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(18.dp))
                    .background(
                        if (voiceState.isRecording) {
                            ChestnutPalette.Primary.copy(alpha = 0.14f)
                        } else {
                            ChestnutPalette.Primary.copy(alpha = 0.08f)
                        }
                    )
                    .border(1.dp, ChestnutPalette.Primary.copy(alpha = 0.32f), RoundedCornerShape(18.dp))
                    .then(
                        if (voiceEnabled) {
                            Modifier.pointerInput(Unit) {
                                detectTapGestures(
                                    onPress = {
                                        currentOnVoiceHoldStart()
                                        try {
                                            tryAwaitRelease()
                                        } finally {
                                            currentOnVoiceHoldEnd()
                                        }
                                    }
                                )
                            }
                        } else {
                            Modifier
                        }
                    )
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(ChestnutPalette.Primary.copy(alpha = if (voiceState.isRecording) pulseAlpha else 0.16f)),
                    contentAlignment = Alignment.Center
                ) {
                    BabyLogMaterialIcon(
                        icon = LineIcon.Voice,
                        tint = if (voiceState.isRecording) Color.White else ChestnutPalette.Primary,
                        modifier = Modifier.size(25.dp)
                    )
                }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = title,
                        color = ChestnutPalette.Ink,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = subtitle,
                        color = ChestnutPalette.Muted,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            OutlinedButton(onClick = onTextEntry) {
                Text("文字录入", color = ChestnutPalette.Primary, fontWeight = FontWeight.Bold)
            }
        }
    }
}
