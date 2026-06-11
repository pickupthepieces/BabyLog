@file:Suppress("FunctionNaming", "LongParameterList")

package app.babylog.nativeapp

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.AlertDialog
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Card
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Article
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Checklist
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FormatListBulleted
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LocalDrink
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.MonitorHeart
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SsidChart
import androidx.compose.material.icons.rounded.Timelapse
import androidx.compose.material.icons.rounded.Vaccines
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
@Composable
internal fun VoiceRecordingPopup() {
    Popup(
        alignment = Alignment.Center,
        properties = PopupProperties(focusable = false)
    ) {
        Surface(
            color = ChestnutPalette.Surface,
            shape = RoundedCornerShape(ChestnutRadius.Card),
            elevation = 12.dp
        ) {
            Row(
                modifier = Modifier
                    .border(1.dp, ChestnutPalette.Primary.copy(alpha = 0.28f), RoundedCornerShape(ChestnutRadius.Card))
                    .padding(horizontal = 22.dp, vertical = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                BabyLogIconTile(
                    icon = LineIcon.Voice,
                    tint = ChestnutPalette.Primary,
                    tileColor = ChestnutPalette.Primary.copy(alpha = 0.14f),
                    modifier = Modifier.size(48.dp),
                    iconSize = 28.dp
                )
                Column {
                    Text("正在录音", color = ChestnutPalette.Ink, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text("松开后转成文字", color = ChestnutPalette.Muted, fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
@Suppress("FunctionNaming")
internal fun ConfirmDialog(
    title: String,
    message: String,
    confirmText: String,
    destructive: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, color = ChestnutPalette.Ink, fontWeight = FontWeight.Bold) },
        text = { Text(message, color = ChestnutPalette.Muted) },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = if (destructive) ChestnutPalette.Danger else ChestnutPalette.Primary
                )
            ) { Text(confirmText, color = Color.White) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消", color = ChestnutPalette.Muted) }
        },
        backgroundColor = ChestnutPalette.Bg
    )
}

@Composable
internal fun LibraryItem(
    title: String,
    count: String,
    note: String,
    icon: LineIcon,
    onClick: (() -> Unit)?
) {
    val interactionSource = remember { MutableInteractionSource() }
    Card(
        shape = RoundedCornerShape(ChestnutRadius.Card),
        backgroundColor = ChestnutPalette.Surface,
        border = BorderStroke(1.dp, ChestnutPalette.Border.copy(alpha = 0.36f)),
        elevation = 0.dp,
        modifier = Modifier
            .fillMaxWidth()
            .babyLogPressScale(interactionSource, pressedScale = 0.985f)
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = null
                    ) { onClick() }
                } else {
                    Modifier
                }
            )
    ) {
        Row(
            modifier = Modifier
                .heightIn(min = 72.dp)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BabyLogIconTile(
                icon = icon,
                tint = ChestnutPalette.Primary,
                tileColor = ChestnutPalette.PrimarySoft,
                modifier = Modifier.size(48.dp),
                iconSize = 26.dp
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, color = ChestnutPalette.Ink, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                Text(
                    note,
                    color = ChestnutPalette.Muted,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.width(12.dp))
            LibraryCountBadge(count = count, enabled = onClick != null)
        }
    }
}

@Composable
private fun LibraryCountBadge(count: String, enabled: Boolean) {
    Text(
        count,
        color = if (enabled) ChestnutPalette.Primary else ChestnutPalette.Text3,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .clip(CircleShape)
            .background(if (enabled) ChestnutPalette.PrimarySoft else ChestnutPalette.Surface2)
            .padding(horizontal = 10.dp, vertical = 6.dp)
    )
}

@Composable
internal fun SettingsPanel(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Text(
            title,
            color = ChestnutPalette.Muted,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 6.dp)
        )
        Card(
            shape = RoundedCornerShape(ChestnutRadius.Card),
            backgroundColor = ChestnutPalette.Surface,
            border = BorderStroke(1.dp, ChestnutPalette.Border.copy(alpha = 0.36f)),
            elevation = 0.dp
        ) {
            Column(content = content)
        }
    }
}

@Composable
internal fun ActionRow(
    title: String,
    subtitle: String,
    action: String,
    actionColor: Color = ChestnutPalette.Primary,
    onClick: (() -> Unit)?
) {
    val interactionSource = remember { MutableInteractionSource() }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 62.dp)
            .babyLogPressScale(interactionSource, pressedScale = 0.99f)
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = null
                    ) { onClick() }
                } else {
                    Modifier
                }
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(title, color = ChestnutPalette.Ink, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            Text(
                subtitle,
                color = ChestnutPalette.Muted,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(Modifier.width(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                action,
                color = if (onClick == null) ChestnutPalette.Text3 else actionColor,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
            if (onClick != null) {
                Spacer(Modifier.width(4.dp))
                Text("›", color = actionColor.copy(alpha = 0.7f), fontSize = 24.sp, fontWeight = FontWeight.Normal)
            }
        }
    }
}

@Composable
internal fun SettingsDivider() {
    Divider(
        modifier = Modifier.padding(start = 16.dp),
        color = ChestnutPalette.Border.copy(alpha = 0.5f),
        thickness = 0.7.dp
    )
}

@Composable
internal fun BabyLogIconTile(
    icon: LineIcon,
    tint: Color,
    tileColor: Color,
    modifier: Modifier = Modifier.size(44.dp),
    iconSize: Dp = 26.dp
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(ChestnutRadius.Control))
            .background(tileColor),
        contentAlignment = Alignment.Center
    ) {
        BabyLogMaterialIcon(
            icon = icon,
            tint = tint,
            modifier = Modifier.size(iconSize)
        )
    }
}

@Composable
internal fun BabyLogMaterialIcon(
    icon: LineIcon,
    tint: Color,
    modifier: Modifier = Modifier.size(24.dp)
) {
    Icon(
        imageVector = icon.imageVector,
        contentDescription = null,
        tint = tint,
        modifier = modifier
    )
}

internal fun quickActionIcon(eventType: String): LineIcon {
    return when (eventType) {
        "ultrasound" -> LineIcon.Ultrasound
        "pregnancy_checkup" -> LineIcon.Checkup
        "screening_nt", "screening_serum", "screening_nipt", "screening_anomaly", "screening_ogtt", "screening_gbs", "screening_nst" -> LineIcon.Checkup
        "fetal_movement" -> LineIcon.Movement
        "contraction" -> LineIcon.Contraction
        "maternal_metric", "growth" -> LineIcon.Metric
        "breastfeed" -> LineIcon.Breastfeed
        "bottle" -> LineIcon.Bottle
        "sleep" -> LineIcon.Sleep
        "wake" -> LineIcon.Wake
        "pee", "poop", "diaper" -> LineIcon.Diaper
        else -> LineIcon.File
    }
}

internal enum class LineIcon(val imageVector: ImageVector) {
    Home(Icons.Rounded.Home),
    Timeline(Icons.Rounded.FormatListBulleted),
    Library(Icons.Rounded.Article),
    Settings(Icons.Rounded.Settings),
    Voice(Icons.Rounded.Mic),
    Ultrasound(Icons.Rounded.MonitorHeart),
    Checkup(Icons.Rounded.Checklist),
    Movement(Icons.Rounded.Favorite),
    Contraction(Icons.Rounded.Timelapse),
    Metric(Icons.Rounded.SsidChart),
    Breastfeed(Icons.Rounded.Favorite),
    Bottle(Icons.Rounded.LocalDrink),
    Sleep(Icons.Rounded.Bedtime),
    Wake(Icons.Rounded.WbSunny),
    Diaper(Icons.Rounded.WaterDrop),
    File(Icons.Rounded.Description),
    Vaccine(Icons.Rounded.Vaccines)
}
