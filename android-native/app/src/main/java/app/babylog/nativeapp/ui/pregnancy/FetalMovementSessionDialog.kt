package app.babylog.nativeapp

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.AlertDialog
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.max

private const val FETAL_MOVEMENT_TARGET = 10
private const val FETAL_MOVEMENT_LIMIT_MS = 60L * 60L * 1000L

@Composable
internal fun FetalMovementSessionDialog(
    voiceState: SmartVoiceUiState,
    onLongTextVoiceStart: LongTextVoiceStart,
    onLongTextVoiceStop: () -> Unit,
    onDismiss: () -> Unit,
    onSave: (BabyLogService.FetalMovementSessionInput) -> Unit
) {
    var startedAtMs by rememberSaveable { mutableStateOf(0L) }
    var tickMs by rememberSaveable { mutableStateOf(0L) }
    var count by rememberSaveable { mutableStateOf(0) }
    var note by rememberSaveable { mutableStateOf("") }
    val running = startedAtMs > 0L
    val now = if (tickMs > 0L) tickMs else System.currentTimeMillis()
    val elapsedMs = if (running) max(0L, now - startedAtMs) else 0L
    val reachedLimit = elapsedMs >= FETAL_MOVEMENT_LIMIT_MS

    LaunchedEffect(running, startedAtMs) {
        while (running) {
            tickMs = System.currentTimeMillis()
            delay(1_000L)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("胎动计数", color = ChestnutPalette.Ink, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = if (running) formatElapsed(elapsedMs) else "未开始",
                    color = ChestnutPalette.Primary,
                    fontSize = 34.sp,
                    fontWeight = FontWeight.Bold,
                    style = TextStyle(fontFeatureSettings = "tnum"),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "$count 次",
                    color = ChestnutPalette.Ink,
                    fontSize = 42.sp,
                    fontWeight = FontWeight.Bold,
                    style = TextStyle(fontFeatureSettings = "tnum"),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick = {
                            if (!running) {
                                startedAtMs = System.currentTimeMillis()
                                tickMs = startedAtMs
                            } else {
                                startedAtMs = 0L
                                tickMs = 0L
                                count = 0
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(if (running) "重置" else "开始", color = ChestnutPalette.Primary, fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(
                        onClick = { if (count > 0) count -= 1 },
                        enabled = count > 0,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("撤销", color = ChestnutPalette.Muted, fontWeight = FontWeight.Bold)
                    }
                }
                Button(
                    onClick = {
                        if (!running) {
                            startedAtMs = System.currentTimeMillis()
                            tickMs = startedAtMs
                        }
                        count += 1
                    },
                    colors = ButtonDefaults.buttonColors(backgroundColor = ChestnutPalette.Green),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(62.dp)
                        .clip(CircleShape)
                ) {
                    Text("+1 胎动", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
                Text(
                    text = when {
                        reachedLimit -> "已满 1 小时，可以保存本次观察。"
                        running -> "记录每次感受到的胎动，保存后可和以往会话比较。"
                        else -> "点开始或直接点 +1 开始计时。"
                    },
                    color = if (reachedLimit) ChestnutPalette.Green else ChestnutPalette.Text3,
                    fontSize = 12.sp,
                    fontWeight = if (reachedLimit) FontWeight.Bold else FontWeight.Normal,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(ChestnutPalette.Bg.copy(alpha = 0.74f), RoundedCornerShape(12.dp))
                        .padding(10.dp)
                )
                ChestnutLongTextField(
                    "备注，可空",
                    note,
                    { note = it },
                    voiceState = voiceState,
                    onVoiceStart = onLongTextVoiceStart,
                    onVoiceStop = onLongTextVoiceStop
                )
            }
        },
        confirmButton = {
            Button(
                enabled = running && count > 0,
                onClick = {
                    val endedAtMs = System.currentTimeMillis()
                    onSave(
                        BabyLogService.FetalMovementSessionInput.create(
                            formatIso(startedAtMs),
                            formatIso(endedAtMs),
                            count,
                            durationMinutes(endedAtMs - startedAtMs),
                            FETAL_MOVEMENT_TARGET,
                            note
                        )
                    )
                },
                colors = ButtonDefaults.buttonColors(backgroundColor = ChestnutPalette.Primary)
            ) {
                Text("保存", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消", color = ChestnutPalette.Muted) }
        },
        backgroundColor = ChestnutPalette.Bg
    )
}

private fun formatElapsed(elapsedMs: Long): String {
    val totalSeconds = max(0L, elapsedMs / 1_000L)
    val minutes = totalSeconds / 60L
    val seconds = totalSeconds % 60L
    return String.format(Locale.US, "%02d:%02d", minutes, seconds)
}

private fun durationMinutes(elapsedMs: Long): Int {
    if (elapsedMs <= 0L) {
        return 0
    }
    return max(1, ((elapsedMs + 59_999L) / 60_000L).toInt())
}

private fun formatIso(millis: Long): String {
    val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ", Locale.US)
    format.timeZone = TimeZone.getTimeZone("Asia/Shanghai")
    return format.format(Date(millis))
}
