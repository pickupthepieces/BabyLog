package app.babylog.nativeapp

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private const val SMART_ENTRY_VOICE_PULSE_TARGET = 1.18f
private const val SMART_ENTRY_VOICE_PULSE_ALPHA_BASE = 1.22f
private const val SMART_ENTRY_VOICE_BUTTON_PULSE_MAX = 1.08f
private const val SMART_ENTRY_VOICE_TRANSCRIBING_SCALE = 0.94f
private const val SMART_ENTRY_VOICE_PULSE_DURATION_MS = 920
private const val SMART_ENTRY_VOICE_PANEL_RECORDING_ALPHA = 0.16f
private const val SMART_ENTRY_VOICE_BORDER_IDLE_ALPHA = 0.54f
private const val SMART_ENTRY_VOICE_PULSE_BG_ALPHA = 0.24f

@Composable
internal fun SmartEntryScreen(
    running: Boolean,
    voiceState: SmartVoiceUiState,
    candidate: BabyLogSmartTextClient.SmartEntryCandidate?,
    onBack: () -> Unit,
    onVoiceStart: () -> Unit,
    onVoiceStop: () -> Unit,
    onSubmit: (String) -> Unit,
    onOpenCandidate: (BabyLogSmartTextClient.SmartEntryCandidate) -> Unit,
    onDismissCandidate: () -> Unit
) {
    var text by rememberSaveable { mutableStateOf("") }
    LaunchedEffect(voiceState.transcriptNonce) {
        if (voiceState.transcriptNonce != 0L && voiceState.transcript.isNotBlank()) {
            text = if (text.isBlank()) voiceState.transcript else text.trimEnd() + "\n" + voiceState.transcript
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ChestnutPalette.Bg)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(ChestnutPalette.Bg)
                .padding(horizontal = 18.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("智能录入", color = ChestnutPalette.Ink, fontSize = 23.sp, fontWeight = FontWeight.Bold)
                Text("先形成可编辑文本，再生成候选字段", color = ChestnutPalette.Muted, fontSize = 13.sp)
            }
            OutlinedButton(
                onClick = onBack,
                enabled = !running && !voiceState.isRecording && !voiceState.isTranscribing,
                shape = CircleShape,
                border = BorderStroke(1.dp, ChestnutPalette.Border.copy(alpha = 0.72f)),
                colors = ButtonDefaults.outlinedButtonColors(backgroundColor = ChestnutPalette.Surface),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Text("返回", color = ChestnutPalette.Primary, fontWeight = FontWeight.Bold)
            }
        }
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                ChestnutLongTextField(
                    label = "输入记录内容",
                    value = text,
                    onValueChange = { text = it },
                    minLines = 7,
                    maxLines = 12
                )
            }
            item {
                VoiceCapturePanel(
                    state = VoiceCaptureState(
                        recording = voiceState.isRecording,
                        transcribing = voiceState.isTranscribing,
                        disabled = running,
                        message = voiceState.message
                    ),
                    onVoiceStart = onVoiceStart,
                    onVoiceStop = onVoiceStop
                )
            }
            if (candidate != null) {
                item {
                    SmartEntryCandidatePanel(
                        candidate = candidate,
                        onOpen = { onOpenCandidate(candidate) },
                        onDismiss = onDismissCandidate
                    )
                }
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(ChestnutPalette.Surface)
                .navigationBarsPadding()
                .padding(horizontal = 18.dp, vertical = 14.dp)
        ) {
            val submitEnabled = text.isNotBlank() && !running && !voiceState.isRecording && !voiceState.isTranscribing
            val submitText = when {
                running -> "识别中..."
                voiceState.isTranscribing -> "转文字中..."
                voiceState.isRecording -> "录音中..."
                else -> "生成候选"
            }
            Button(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                onClick = { onSubmit(text) },
                enabled = submitEnabled,
                shape = RoundedCornerShape(ChestnutRadius.Control),
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = ChestnutPalette.Primary,
                    disabledBackgroundColor = ChestnutPalette.Surface2
                )
            ) {
                Text(
                    submitText,
                    color = if (submitEnabled) Color.White else ChestnutPalette.Muted,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun SmartEntryCandidatePanel(
    candidate: BabyLogSmartTextClient.SmartEntryCandidate,
    onOpen: () -> Unit,
    onDismiss: () -> Unit
) {
    val rows = candidate.values.entries
        .filter { it.value.isNotBlank() }
        .take(8)
    Surface(
        shape = RoundedCornerShape(ChestnutRadius.Control),
        color = ChestnutPalette.PrimarySoft,
        border = BorderStroke(1.dp, ChestnutPalette.Primary.copy(alpha = 0.36f)),
        elevation = 0.dp
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("识别候选", color = ChestnutPalette.Ink, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text(
                if (candidate.eventType.isBlank()) "暂未判断出记录类型" else BabyLogFormatters.eventLabel(candidate.eventType),
                color = ChestnutPalette.Primary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            if (rows.isEmpty()) {
                Text("未识别到可用字段，可修改后重试。", color = ChestnutPalette.Muted, fontSize = 13.sp)
            } else {
                rows.forEach { entry ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(entry.key, color = ChestnutPalette.Muted, fontSize = 12.sp, modifier = Modifier.weight(0.42f))
                        Text(
                            entry.value,
                            color = ChestnutPalette.Ink,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(0.58f)
                        )
                    }
                }
            }
            if (candidate.warnings.isNotEmpty()) {
                Text(
                    "需核对：" + candidate.warnings.joinToString("；"),
                    color = ChestnutPalette.Primary,
                    fontSize = 12.sp
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                TextButton(onClick = onDismiss) {
                    Text("重新编辑", color = ChestnutPalette.Muted)
                }
                Button(
                    enabled = candidate.eventType.isNotBlank(),
                    onClick = onOpen,
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = ChestnutPalette.Primary,
                        disabledBackgroundColor = ChestnutPalette.Surface2
                    )
                ) {
                    Text("核对并填写", color = Color.White)
                }
            }
        }
    }
}

private data class VoiceCaptureState(
    val recording: Boolean,
    val transcribing: Boolean,
    val disabled: Boolean,
    val message: String
) {
    val canStart: Boolean
        get() = !disabled && !transcribing

    val canTap: Boolean
        get() = recording || canStart

    val title: String
        get() = when {
            recording -> "正在听"
            transcribing -> "正在整理文字"
            else -> "语音输入"
        }

    val detail: String
        get() = when {
            recording -> "说完点麦克风结束"
            transcribing -> "转写完成后会放进文本"
            else -> "点麦克风开始说话"
        }

    val panelColor: Color
        get() = when {
            recording -> ChestnutPalette.Primary.copy(alpha = SMART_ENTRY_VOICE_PANEL_RECORDING_ALPHA)
            transcribing -> ChestnutPalette.Surface2
            else -> ChestnutPalette.Surface
        }

    val buttonColor: Color
        get() = when {
            recording -> ChestnutPalette.Danger
            transcribing -> ChestnutPalette.Surface2
            else -> ChestnutPalette.Primary
        }

    val borderColor: Color
        get() = when {
            recording -> ChestnutPalette.Primary
            transcribing -> ChestnutPalette.Border
            else -> ChestnutPalette.Border.copy(alpha = SMART_ENTRY_VOICE_BORDER_IDLE_ALPHA)
        }
}

@Composable
@Suppress("FunctionNaming")
private fun VoiceCapturePanel(
    state: VoiceCaptureState,
    onVoiceStart: () -> Unit,
    onVoiceStop: () -> Unit
) {
    val panelColor by animateColorAsState(
        targetValue = state.panelColor,
        label = "smartEntryVoicePanelColor"
    )
    val buttonColor by animateColorAsState(
        targetValue = state.buttonColor,
        label = "smartEntryVoiceButtonColor"
    )
    val iconColor by animateColorAsState(
        targetValue = if (state.recording || state.canStart) Color.White else ChestnutPalette.Muted,
        label = "smartEntryVoiceIconColor"
    )
    val recordingPulse by rememberInfiniteTransition(label = "smartEntryVoicePulse").animateFloat(
        initialValue = 1f,
        targetValue = SMART_ENTRY_VOICE_PULSE_TARGET,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = SMART_ENTRY_VOICE_PULSE_DURATION_MS),
            repeatMode = RepeatMode.Reverse
        ),
        label = "smartEntryVoicePulseScale"
    )
    val idleScale by animateFloatAsState(
        targetValue = if (state.transcribing) SMART_ENTRY_VOICE_TRANSCRIBING_SCALE else 1f,
        label = "smartEntryVoiceIdleScale"
    )

    Surface(
        shape = RoundedCornerShape(ChestnutRadius.Control),
        color = panelColor,
        border = BorderStroke(1.dp, state.borderColor),
        elevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 15.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier.size(78.dp),
                contentAlignment = Alignment.Center
            ) {
                VoiceMicButton(
                    state = state,
                    motion = VoiceMicMotion(recordingPulse = recordingPulse, idleScale = idleScale),
                    colors = VoiceMicColors(buttonColor = buttonColor, iconColor = iconColor),
                    onVoiceStart = onVoiceStart,
                    onVoiceStop = onVoiceStop
                )
            }
            VoiceCaptureCopy(state = state, modifier = Modifier.weight(1f))
        }
    }
}

private data class VoiceMicMotion(
    val recordingPulse: Float,
    val idleScale: Float
)

private data class VoiceMicColors(
    val buttonColor: Color,
    val iconColor: Color
)

@Composable
@Suppress("FunctionNaming")
private fun VoiceMicButton(
    state: VoiceCaptureState,
    motion: VoiceMicMotion,
    colors: VoiceMicColors,
    onVoiceStart: () -> Unit,
    onVoiceStop: () -> Unit
) {
    val currentOnVoiceStart by rememberUpdatedState(onVoiceStart)
    val currentOnVoiceStop by rememberUpdatedState(onVoiceStop)
    if (state.recording) {
        Box(
            modifier = Modifier
                .size(68.dp)
                .graphicsLayer {
                    scaleX = motion.recordingPulse
                    scaleY = motion.recordingPulse
                    alpha = SMART_ENTRY_VOICE_PULSE_ALPHA_BASE - motion.recordingPulse
                }
                .background(ChestnutPalette.Danger.copy(alpha = SMART_ENTRY_VOICE_PULSE_BG_ALPHA), CircleShape)
        )
    }
    Button(
        modifier = Modifier
            .size(64.dp)
            .semantics {
                contentDescription = if (state.recording) "结束语音输入" else "开始语音输入"
            }
            .graphicsLayer {
                val scale = if (state.recording) {
                    motion.recordingPulse.coerceAtMost(SMART_ENTRY_VOICE_BUTTON_PULSE_MAX)
                } else {
                    motion.idleScale
                }
                scaleX = scale
                scaleY = scale
            },
        onClick = {
            if (state.recording) {
                currentOnVoiceStop()
            } else if (state.canStart) {
                currentOnVoiceStart()
            }
        },
        enabled = state.canTap,
        shape = CircleShape,
        contentPadding = PaddingValues(0.dp),
        colors = ButtonDefaults.buttonColors(
            backgroundColor = colors.buttonColor,
            disabledBackgroundColor = ChestnutPalette.Surface2
        )
    ) {
        BabyLogMaterialIcon(
            icon = LineIcon.Voice,
            tint = colors.iconColor,
            modifier = Modifier.size(30.dp)
        )
    }
}

@Composable
@Suppress("FunctionNaming")
private fun VoiceCaptureCopy(state: VoiceCaptureState, modifier: Modifier = Modifier) {
    val detail = state.detail
    val status = state.message.takeIf { it.isNotBlank() && it != detail }
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(state.title, color = ChestnutPalette.Ink, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Text(detail, color = ChestnutPalette.Muted, fontSize = 13.sp)
        if (status != null) {
            Text(
                status,
                color = if (state.recording || state.transcribing) ChestnutPalette.Primary else ChestnutPalette.Muted,
                fontSize = 12.sp,
                fontWeight = if (state.recording || state.transcribing) FontWeight.SemiBold else FontWeight.Normal
            )
        }
    }
}
