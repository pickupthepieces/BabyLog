package app.babylog.nativeapp

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.io.File

@Composable
internal fun AttachmentPreviewScreen(
    attachment: BabyLogDomain.AttachmentRecord?,
    onBack: () -> Unit
) {
    val title = attachment?.originalName ?: "附件预览"
    SettingsPageScaffold(
        title = title,
        subtitle = "本机文件查看",
        onBack = onBack
    ) {
        if (attachment == null) {
            item { EmptyPanel("附件内容暂不可用，请返回后重试。") }
        } else {
            item {
                val hasLocalFile = remember(attachment.localPath) {
                    attachment.localPath.isNotBlank() && File(attachment.localPath).exists()
                }
                val bitmap = remember(attachment.localPath) {
                    if (hasLocalFile) BitmapFactory.decodeFile(attachment.localPath)?.asImageBitmap() else null
                }
                if (bitmap == null) {
                    AttachmentDownloadPlaceholder()
                } else {
                    Image(
                        bitmap = bitmap,
                        contentDescription = attachment.originalName,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(620.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(ChestnutPalette.Surface2),
                        contentScale = ContentScale.Fit
                    )
                }
            }
        }
    }
}

@Suppress("FunctionNaming")
@Composable
private fun AttachmentDownloadPlaceholder() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(260.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(ChestnutPalette.Surface2)
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(color = ChestnutPalette.Primary)
        Text("等待下载加密附件...", color = ChestnutPalette.Ink, fontSize = 15.sp, modifier = Modifier.padding(top = 14.dp))
        Text("同步完成后这里会自动显示原图。", color = ChestnutPalette.Muted, fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp))
    }
}
