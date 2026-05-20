package app.babylog.nativeapp

import androidx.activity.compose.BackHandler
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
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import java.util.UUID
import kotlin.math.max
import kotlin.math.roundToInt

@Composable
internal fun ContractionSessionScreen(
    onBack: () -> Unit,
    onSave: (BabyLogService.ContractionSessionInput) -> Unit
) {
    val sessionId = rememberSaveable { UUID.randomUUID().toString() }
    val entries = remember { mutableStateListOf<ContractionSessionEntry>() }
    var currentStartMs by rememberSaveable { mutableStateOf(0L) }
    var tickMs by rememberSaveable { mutableStateOf(0L) }
    var formError by rememberSaveable { mutableStateOf("") }
    var confirmExit by rememberSaveable { mutableStateOf(false) }
    val running = currentStartMs > 0L
    val now = if (tickMs > 0L) tickMs else System.currentTimeMillis()
    val elapsedMs = if (running) max(0L, now - currentStartMs) else 0L

    fun hasUnsavedWork(): Boolean = running || entries.isNotEmpty()
    fun requestBack() {
        if (hasUnsavedWork()) {
            confirmExit = true
        } else {
            onBack()
        }
    }

    BackHandler { requestBack() }

    LaunchedEffect(running, currentStartMs) {
        while (running) {
            tickMs = System.currentTimeMillis()
            delay(1_000L)
        }
    }

    SettingsPageScaffold(
        title = "宫缩计时",
        subtitle = "仅作记录与参考，具体判断请以医生意见为准",
        saveText = "结束会话并保存",
        onBack = ::requestBack,
        onSave = {
            when {
                running -> formError = "请先结束本次宫缩，再保存会话"
                entries.isEmpty() -> formError = "请至少记录一次宫缩"
                else -> {
                    formError = ""
                    onSave(
                        BabyLogService.ContractionSessionInput.create(
                            sessionId,
                            entries.map { entry ->
                                BabyLogService.ContractionEntryInput.create(
                                    formatIso(entry.startMs),
                                    formatIso(entry.endMs),
                                    durationSeconds(entry.durationMs),
                                    entry.intervalFromPrevMs?.let { durationSeconds(it) }
                                )
                            }
                        )
                    )
                }
            }
        }
    ) {
        item {
            Text(
                "本页只记录开始、结束、持续与间隔，不判断是否临产，也不替代医生意见。",
                color = ChestnutPalette.Muted,
                fontSize = 13.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ChestnutPalette.Surface2, RoundedCornerShape(14.dp))
                    .padding(14.dp)
            )
        }
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ChestnutPalette.Surface, RoundedCornerShape(16.dp))
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = if (running) formatElapsed(elapsedMs) else "00:00",
                    color = ChestnutPalette.Primary,
                    fontSize = 44.sp,
                    fontWeight = FontWeight.Bold,
                    style = TextStyle(fontFeatureSettings = "tnum"),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
                Button(
                    onClick = {
                        val current = System.currentTimeMillis()
                        if (!running) {
                            formError = ""
                            currentStartMs = current
                            tickMs = current
                        } else {
                            val interval = entries.lastOrNull()?.let { currentStartMs - it.startMs }
                            entries.add(
                                ContractionSessionEntry(
                                    startMs = currentStartMs,
                                    endMs = current,
                                    durationMs = max(0L, current - currentStartMs),
                                    intervalFromPrevMs = interval?.takeIf { it > 0L }
                                )
                            )
                            currentStartMs = 0L
                            tickMs = 0L
                        }
                    },
                    colors = ButtonDefaults.buttonColors(backgroundColor = ChestnutPalette.Primary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .clip(CircleShape)
                ) {
                    Text(if (running) "结束本次" else "开始一次", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        if (formError.isNotBlank()) {
            item { Text(formError, color = ChestnutPalette.Danger, fontWeight = FontWeight.Bold) }
        }
        item {
            ContractionStats(entries)
        }
        if (entries.isEmpty()) {
            item { EmptyPanel("还没有记录。点“开始一次”，结束后会加入本会话列表。") }
        } else {
            entries.forEachIndexed { index, entry ->
                item(key = "contraction-entry-$index-${entry.startMs}") {
                    ContractionEntryRow(index + 1, entry)
                }
            }
        }
    }

    if (confirmExit) {
        AlertDialog(
            onDismissRequest = { confirmExit = false },
            title = { Text("丢弃本次计时？", color = ChestnutPalette.Ink, fontWeight = FontWeight.Bold) },
            text = { Text("当前会话还没有保存，返回会丢失本次临时计时。") },
            confirmButton = {
                Button(
                    onClick = {
                        confirmExit = false
                        onBack()
                    },
                    colors = ButtonDefaults.buttonColors(backgroundColor = ChestnutPalette.Danger)
                ) {
                    Text("丢弃", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmExit = false }) {
                    Text("继续记录", color = ChestnutPalette.Primary)
                }
            },
            backgroundColor = ChestnutPalette.Bg
        )
    }
}

@Composable
private fun ContractionStats(entries: List<ContractionSessionEntry>) {
    val durations = entries.map { durationSeconds(it.durationMs) }
    val avg = if (durations.isEmpty()) 0 else durations.average().roundToInt()
    val min = durations.minOrNull() ?: 0
    val max = durations.maxOrNull() ?: 0
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(ChestnutPalette.Surface, RoundedCornerShape(16.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("本会话统计", color = ChestnutPalette.Ink, fontWeight = FontWeight.Bold)
        Text("次数 ${entries.size}", color = ChestnutPalette.Muted, style = TextStyle(fontFeatureSettings = "tnum"))
        if (entries.isNotEmpty()) {
            Text(
                "平均持续 ${avg} 秒；最短 ${min} 秒；最长 ${max} 秒",
                color = ChestnutPalette.Muted,
                style = TextStyle(fontFeatureSettings = "tnum")
            )
        }
    }
}

@Composable
private fun ContractionEntryRow(index: Int, entry: ContractionSessionEntry) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(ChestnutPalette.Surface, RoundedCornerShape(14.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text("第 $index 次", color = ChestnutPalette.Ink, fontWeight = FontWeight.Bold)
        Text(
            "${formatClock(entry.startMs)} - ${formatClock(entry.endMs)} · 持续 ${durationSeconds(entry.durationMs)} 秒",
            color = ChestnutPalette.Muted,
            style = TextStyle(fontFeatureSettings = "tnum")
        )
        entry.intervalFromPrevMs?.let {
            Text("距上次开始 ${formatInterval(it)}", color = ChestnutPalette.Text3, style = TextStyle(fontFeatureSettings = "tnum"))
        }
    }
}

private data class ContractionSessionEntry(
    val startMs: Long,
    val endMs: Long,
    val durationMs: Long,
    val intervalFromPrevMs: Long?
)

private fun formatElapsed(elapsedMs: Long): String {
    val totalSeconds = durationSeconds(elapsedMs)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format(Locale.US, "%02d:%02d", minutes, seconds)
}

private fun formatClock(millis: Long): String {
    val format = SimpleDateFormat("HH:mm:ss", Locale.CHINA)
    format.timeZone = TimeZone.getTimeZone("Asia/Shanghai")
    return format.format(Date(millis))
}

private fun formatInterval(millis: Long): String {
    val totalSeconds = durationSeconds(millis)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return if (minutes > 0) {
        "${minutes}分${seconds}秒"
    } else {
        "${seconds}秒"
    }
}

private fun durationSeconds(elapsedMs: Long): Int {
    if (elapsedMs <= 0L) {
        return 0
    }
    return max(1, ((elapsedMs + 999L) / 1_000L).toInt())
}

private fun formatIso(millis: Long): String {
    val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ", Locale.US)
    format.timeZone = TimeZone.getTimeZone("Asia/Shanghai")
    return format.format(Date(millis))
}
