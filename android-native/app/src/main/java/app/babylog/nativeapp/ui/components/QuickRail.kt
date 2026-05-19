package app.babylog.nativeapp

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
                    .clip(RoundedCornerShape(15.dp))
                    .background(Color(action.toneColor).copy(alpha = 0.16f))
                    .clickable { currentOnAction(action) }
                    .padding(vertical = 5.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                BabyLogIconTile(
                    icon = quickActionIcon(action.eventType),
                    tint = Color(action.toneColor),
                    tileColor = Color(action.toneColor).copy(alpha = 0.18f),
                    modifier = Modifier.size(28.dp),
                    iconSize = 18.dp
                )
                Spacer(Modifier.height(3.dp))
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
