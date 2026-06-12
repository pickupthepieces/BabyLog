package app.babylog.nativeapp

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Card
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
internal fun TrashScreen(
    events: List<BabyLogDomain.BabyLogEvent>,
    onBack: () -> Unit,
    onRestore: (BabyLogDomain.BabyLogEvent) -> Unit
) {
    val nowIso = remember(events) { BabyLogFormatters.nowIso() }
    SettingsPageScaffold(
        title = "回收站",
        subtitle = "删除记录保留 7 天，超期自动永久清理",
        onBack = onBack
    ) {
        if (events.isEmpty()) {
            item { EmptyPanel("回收站是空的") }
        } else {
            items(events, key = { it.id }) { event ->
                TrashRow(
                    event = event,
                    nowIso = nowIso,
                    onRestore = { onRestore(event) }
                )
            }
        }
    }
}

@Composable
private fun TrashRow(
    event: BabyLogDomain.BabyLogEvent,
    nowIso: String,
    onRestore: () -> Unit
) {
    val remainingDays = BabyLogService.trashRemainingDays(event.deletedAt, nowIso)
    Card(
        shape = RoundedCornerShape(ChestnutRadius.Control),
        backgroundColor = ChestnutPalette.Surface,
        border = BorderStroke(1.dp, ChestnutPalette.Border),
        elevation = 1.dp
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "${BabyLogFormatters.eventLabel(event.eventType)} · ${BabyLogFormatters.formatEventDay(event.occurredAt)} ${BabyLogFormatters.formatEventTime(event.occurredAt)}",
                        color = ChestnutPalette.Muted,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    TrashRowDetail(event)
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    if (remainingDays <= 0) "即将清理" else "剩 $remainingDays 天",
                    color = if (remainingDays <= 1) ChestnutPalette.Danger else ChestnutPalette.Accent,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(if (remainingDays <= 1) ChestnutPalette.DangerSoft else ChestnutPalette.AccentSoft)
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "删除于 ${BabyLogFormatters.formatDateTime(event.deletedAt)}",
                    color = ChestnutPalette.Text3,
                    fontSize = 12.sp,
                    modifier = Modifier.weight(1f)
                )
                Button(
                    onClick = onRestore,
                    colors = ButtonDefaults.buttonColors(backgroundColor = ChestnutPalette.Primary),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text("恢复", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// 标题行已含类型名，这里只放纯详情，没有详情就不占一行。
@Suppress("FunctionNaming")
@Composable
private fun TrashRowDetail(event: BabyLogDomain.BabyLogEvent) {
    val detail = BabyLogFormatters.detailOnlySummary(
        BabyLogFormatters.eventSummary(event),
        event.eventType
    )
    if (detail.isNotBlank()) {
        Text(
            detail,
            color = ChestnutPalette.Ink,
            fontSize = 16.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}
