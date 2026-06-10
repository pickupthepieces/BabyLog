package app.babylog.nativeapp

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.io.File

@Composable
internal fun AttachmentListScreen(
    state: AttachmentListPageState?,
    onBack: () -> Unit,
    onPreview: (BabyLogDomain.AttachmentRecord) -> Unit
) {
    val title = state?.title ?: "附件"
    val attachments = state?.attachments.orEmpty()
    SettingsPageScaffold(
        title = title,
        subtitle = "本机附件列表",
        onBack = onBack
    ) {
        if (attachments.isEmpty()) {
            item { EmptyPanel("暂无附件。") }
        } else {
            items(attachments, key = { it.id }) { attachment ->
                AttachmentListRow(
                    attachment = attachment,
                    onClick = { onPreview(attachment) }
                )
            }
        }
    }
}

@Composable
private fun AttachmentListRow(
    attachment: BabyLogDomain.AttachmentRecord,
    onClick: () -> Unit
) {
    val hasLocalFile = attachment.localPath.isNotBlank() && File(attachment.localPath).exists()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(ChestnutRadius.Small))
            .background(ChestnutPalette.Surface)
            .border(1.dp, ChestnutPalette.Border, RoundedCornerShape(ChestnutRadius.Small))
            .clickable { onClick() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(ChestnutRadius.Small))
                .background(ChestnutPalette.Primary.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                if (hasLocalFile) "文" else "下",
                color = ChestnutPalette.Primary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(attachment.originalName, color = ChestnutPalette.Ink, fontWeight = FontWeight.Bold)
            Text(
                "${if (hasLocalFile) "已在本机" else "等待下载"} · ${BabyLogFormatters.formatDateTime(attachment.createdAt)} · ${BabyLogFormatters.formatByteSize(attachment.byteSize)}",
                color = ChestnutPalette.Muted,
                fontSize = 12.sp
            )
        }
    }
}
