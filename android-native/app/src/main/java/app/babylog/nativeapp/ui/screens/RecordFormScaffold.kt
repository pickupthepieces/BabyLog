package app.babylog.nativeapp

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
internal fun RecordFormScaffold(
    title: String,
    subtitle: String,
    saveText: String,
    onBack: () -> Unit,
    onSave: () -> Unit,
    content: LazyListScope.() -> Unit
) {
    BackHandler(onBack = onBack)
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
                Text(title, color = ChestnutPalette.Ink, fontSize = 23.sp, fontWeight = FontWeight.Bold)
                Text(subtitle, color = ChestnutPalette.Muted, fontSize = 13.sp)
            }
            OutlinedButton(
                onClick = onBack,
                shape = CircleShape,
                border = BorderStroke(1.dp, ChestnutPalette.Border.copy(alpha = 0.72f)),
                colors = ButtonDefaults.outlinedButtonColors(backgroundColor = ChestnutPalette.Surface),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Text("取消", color = ChestnutPalette.Primary, fontWeight = FontWeight.Bold)
            }
        }
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = content
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(ChestnutPalette.Surface)
                .navigationBarsPadding()
                .padding(horizontal = 18.dp, vertical = 14.dp)
        ) {
            Button(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                onClick = onSave,
                shape = RoundedCornerShape(ChestnutRadius.Control),
                colors = ButtonDefaults.buttonColors(backgroundColor = ChestnutPalette.Primary)
            ) {
                Text(saveText, color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}
