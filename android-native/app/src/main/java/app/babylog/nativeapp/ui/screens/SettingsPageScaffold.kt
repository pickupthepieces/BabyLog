package app.babylog.nativeapp

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
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
internal fun SettingsPageScaffold(
    title: String,
    subtitle: String,
    saveText: String = "保存",
    onBack: () -> Unit,
    onSave: (() -> Unit)? = null,
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
                .background(ChestnutPalette.Primary)
                .padding(horizontal = 18.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = Color.White, fontSize = 23.sp, fontWeight = FontWeight.Bold)
                Text(subtitle, color = Color.White.copy(alpha = 0.78f), fontSize = 13.sp)
            }
            OutlinedButton(onClick = onBack) {
                Text("返回", color = Color.White)
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
        if (onSave != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ChestnutPalette.Surface)
                    .padding(horizontal = 18.dp, vertical = 14.dp)
            ) {
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onSave,
                    colors = ButtonDefaults.buttonColors(backgroundColor = ChestnutPalette.Primary)
                ) {
                    Text(saveText, color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
