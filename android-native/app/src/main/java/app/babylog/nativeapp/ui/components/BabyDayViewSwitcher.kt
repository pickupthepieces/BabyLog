@file:Suppress("FunctionNaming", "InvalidPackageDeclaration", "MatchingDeclarationName")

package app.babylog.nativeapp

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

internal enum class BabyDayViewMode {
    Timeline,
    List
}

@Composable
internal fun BabyDayViewSwitcher(
    mode: BabyDayViewMode,
    onModeChange: (BabyDayViewMode) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        BabyDayModeButton(
            text = "时间轴",
            selected = mode == BabyDayViewMode.Timeline,
            onClick = { onModeChange(BabyDayViewMode.Timeline) },
            modifier = Modifier.weight(1f)
        )
        BabyDayModeButton(
            text = "列表",
            selected = mode == BabyDayViewMode.List,
            onClick = { onModeChange(BabyDayViewMode.List) },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun BabyDayModeButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(ChestnutRadius.Sheet),
        colors = ButtonDefaults.outlinedButtonColors(
            backgroundColor = if (selected) ChestnutPalette.Primary else ChestnutPalette.Surface,
            contentColor = if (selected) androidx.compose.ui.graphics.Color.White else ChestnutPalette.Muted
        )
    ) {
        Text(text, fontWeight = FontWeight.Bold)
    }
}
