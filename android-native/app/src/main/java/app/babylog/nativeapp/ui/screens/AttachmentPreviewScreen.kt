package app.babylog.nativeapp

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp

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
                val bitmap = remember(attachment.localPath) {
                    BitmapFactory.decodeFile(attachment.localPath)?.asImageBitmap()
                }
                if (bitmap == null) {
                    Text("本机文件不存在或无法读取。", color = ChestnutPalette.Muted)
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
