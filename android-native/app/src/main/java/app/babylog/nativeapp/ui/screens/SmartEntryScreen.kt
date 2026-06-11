package app.babylog.nativeapp

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

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
                Text("输入文字或按住说话，生成候选字段", color = ChestnutPalette.Muted, fontSize = 13.sp)
            }
            OutlinedButton(
                onClick = onBack,
                enabled = !running,
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
                VoiceHoldButton(
                    recording = voiceState.isRecording,
                    transcribing = voiceState.isTranscribing,
                    disabled = running,
                    onVoiceStart = onVoiceStart,
                    onVoiceStop = onVoiceStop
                )
            }
            if (voiceState.message.isNotBlank()) {
                item {
                    Text(
                        voiceState.message,
                        color = if (voiceState.isRecording || voiceState.isTranscribing) ChestnutPalette.Primary else ChestnutPalette.Muted,
                        fontSize = 12.sp,
                        fontWeight = if (voiceState.isRecording || voiceState.isTranscribing) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
            item {
                ChestnutLongTextField(
                    label = "输入记录内容",
                    value = text,
                    onValueChange = { text = it },
                    minLines = 6,
                    maxLines = 10
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
                    if (running) "识别中..." else "生成候选",
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

@Composable
private fun VoiceHoldButton(
    recording: Boolean,
    transcribing: Boolean,
    disabled: Boolean,
    onVoiceStart: () -> Unit,
    onVoiceStop: () -> Unit
) {
    val currentOnVoiceStart by rememberUpdatedState(onVoiceStart)
    val currentOnVoiceStop by rememberUpdatedState(onVoiceStop)
    val enabled = !disabled && !transcribing
    val bg = when {
        recording -> ChestnutPalette.Primary
        transcribing -> ChestnutPalette.Surface2
        else -> ChestnutPalette.Primary.copy(alpha = 0.12f)
    }
    val borderColor = if (recording) ChestnutPalette.Primary else ChestnutPalette.Primary.copy(alpha = 0.42f)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(ChestnutRadius.Control))
            .background(bg)
            .border(1.dp, borderColor, RoundedCornerShape(ChestnutRadius.Control))
            .then(
                if (enabled) {
                    Modifier.pointerInput(Unit) {
                        detectTapGestures(
                            onPress = {
                                currentOnVoiceStart()
                                try {
                                    tryAwaitRelease()
                                } finally {
                                    currentOnVoiceStop()
                                }
                            }
                        )
                    }
                } else {
                    Modifier
                }
            )
            .padding(vertical = 14.dp, horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            when {
                recording -> "松开结束录音"
                transcribing -> "正在转文字..."
                else -> "按住说话"
            },
            color = if (recording) Color.White else ChestnutPalette.Primary,
            fontWeight = FontWeight.Bold
        )
    }
}
