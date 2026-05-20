package app.babylog.nativeapp

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.Image
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
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
            .padding(horizontal = 10.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        actions.forEach { action ->
            Column(
                modifier = Modifier
                    .width(58.dp)
                    .clickable { currentOnAction(action) }
                    .padding(vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val sticker = quickActionSticker(action.eventType)
                if (sticker != null) {
                    Image(
                        painter = sticker,
                        contentDescription = action.label,
                        modifier = Modifier.size(44.dp)
                    )
                } else {
                    val tone = Color(action.toneColor)
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(tone.copy(alpha = 0.16f)),
                        contentAlignment = Alignment.Center
                    ) {
                        BabyLogMaterialIcon(
                            icon = quickActionIcon(action.eventType),
                            tint = tone,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = action.label,
                    color = ChestnutPalette.Ink,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun quickActionSticker(eventType: String): Painter? {
    return when (eventType) {
        "ultrasound" -> painterResource(R.drawable.sticker_b_chao)
        "pregnancy_checkup" -> painterResource(R.drawable.sticker_checkup)
        "fetal_movement" -> painterResource(R.drawable.sticker_fetal_movement)
        "contraction" -> painterResource(R.drawable.sticker_contraction)
        "maternal_metric" -> painterResource(R.drawable.sticker_maternal_metric)
        else -> null
    }
}
