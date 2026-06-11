@file:Suppress("FunctionNaming", "InvalidPackageDeclaration", "LongParameterList", "MagicNumber")

package app.babylog.nativeapp

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.io.File

internal fun LazyListScope.ultrasoundPhotoSection(
    photoPath: String?,
    photoName: String?,
    ocrRunning: Boolean,
    ocrCandidate: BabyLogSmartInput.UltrasoundOcrCandidate?,
    saveError: String,
    onPickPhoto: () -> Unit,
    onCapturePhoto: () -> Unit,
    onRecognizePhoto: () -> Unit,
    onCandidateDismiss: () -> Unit,
    onCandidateApply: (BabyLogSmartInput.UltrasoundOcrCandidate) -> Unit
) {
    item(key = "photo_header") {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("B 超单识别", color = ChestnutPalette.Ink, fontWeight = FontWeight.Bold)
            Text("拍照或选图后识别，保存前请核对。", color = ChestnutPalette.Muted, fontSize = 12.sp)
        }
    }
    item(key = "photo_actions") {
        UltrasoundPhotoActions(
            photoPath = photoPath,
            ocrRunning = ocrRunning,
            onCapturePhoto = onCapturePhoto,
            onPickPhoto = onPickPhoto,
            onRecognizePhoto = onRecognizePhoto
        )
    }
    if (photoPath != null) {
        item(key = "photo_selected") {
            Text(
                "已选择：${photoName ?: File(photoPath).name}",
                color = ChestnutPalette.Primary,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )
        }
    }
    if (saveError.isNotBlank()) {
        item(key = "save_error") { NoticeBanner(saveError) }
    }
    if (ocrCandidate != null) {
        item(key = "ocr_candidate") {
            UltrasoundOcrCandidateCard(
                candidate = ocrCandidate,
                onApply = { onCandidateApply(ocrCandidate) },
                onDismiss = onCandidateDismiss
            )
        }
    }
}

@Composable
private fun UltrasoundPhotoActions(
    photoPath: String?,
    ocrRunning: Boolean,
    onCapturePhoto: () -> Unit,
    onPickPhoto: () -> Unit,
    onRecognizePhoto: () -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        UltrasoundOutlineAction("拍照", Modifier.weight(1f), onCapturePhoto)
        UltrasoundOutlineAction("选图", Modifier.weight(1f), onPickPhoto)
        Button(
            modifier = Modifier
                .weight(1.12f)
                .height(50.dp),
            enabled = photoPath != null && !ocrRunning,
            onClick = onRecognizePhoto,
            shape = RoundedCornerShape(ChestnutRadius.Control),
            colors = ButtonDefaults.buttonColors(
                backgroundColor = if (photoPath != null) ChestnutPalette.Primary else ChestnutPalette.Surface2,
                disabledBackgroundColor = ChestnutPalette.Surface2
            )
        ) {
            Text(
                if (ocrRunning) "识别中..." else if (photoPath == null) "先选图" else "识别",
                color = if (photoPath != null && !ocrRunning) Color.White else ChestnutPalette.Text3,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun UltrasoundOutlineAction(text: String, modifier: Modifier, onClick: () -> Unit) {
    OutlinedButton(
        modifier = modifier.height(50.dp),
        onClick = onClick,
        shape = RoundedCornerShape(ChestnutRadius.Control),
        border = BorderStroke(1.dp, ChestnutPalette.Primary.copy(alpha = 0.84f))
    ) {
        Text(text, color = ChestnutPalette.Primary, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun UltrasoundOcrCandidateCard(
    candidate: BabyLogSmartInput.UltrasoundOcrCandidate,
    onApply: () -> Unit,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(ChestnutRadius.Control))
            .background(ChestnutPalette.PrimarySoft)
            .border(1.dp, ChestnutPalette.Primary.copy(alpha = 0.45f), RoundedCornerShape(ChestnutRadius.Control))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("识别候选", color = ChestnutPalette.Ink, fontWeight = FontWeight.Bold)
        Text("检查日期会同步推算孕周，可手动调整。", color = ChestnutPalette.Muted, fontSize = 12.sp)
        val rows = ultrasoundCandidateRows(candidate)
        if (rows.isEmpty()) {
            Text("未识别到可用字段，可手动填写。", color = ChestnutPalette.Muted, fontSize = 13.sp)
        } else {
            rows.take(14).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(row.first, color = ChestnutPalette.Muted, fontSize = 12.sp, modifier = Modifier.width(90.dp))
                    Text(row.second, color = ChestnutPalette.Ink, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
            if (rows.size > 14) {
                Text("另有 ${rows.size - 14} 项，填入后继续核对。", color = ChestnutPalette.Muted, fontSize = 12.sp)
            }
        }
        if (candidate.warnings.isNotEmpty()) {
            Text("需核对：" + candidate.warnings.joinToString("；"), color = ChestnutPalette.Notice, fontSize = 12.sp)
        }
        if (!candidate.rawText.isNullOrBlank()) {
            Text("原文片段", color = ChestnutPalette.Muted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text(
                candidate.rawText.take(220),
                color = ChestnutPalette.Muted,
                fontSize = 12.sp,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis
            )
        }
        UltrasoundCandidateActions(rows.isNotEmpty(), onApply, onDismiss)
    }
}

@Composable
private fun UltrasoundCandidateActions(
    enabled: Boolean,
    onApply: () -> Unit,
    onDismiss: () -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedButton(
            modifier = Modifier
                .weight(1f)
                .height(48.dp),
            onClick = onDismiss,
            shape = RoundedCornerShape(ChestnutRadius.Control),
            border = BorderStroke(1.dp, ChestnutPalette.Border)
        ) {
            Text("不用", color = ChestnutPalette.Muted)
        }
        Button(
            modifier = Modifier
                .weight(1.5f)
                .height(48.dp),
            enabled = enabled,
            onClick = onApply,
            shape = RoundedCornerShape(ChestnutRadius.Control),
            colors = ButtonDefaults.buttonColors(backgroundColor = ChestnutPalette.Primary)
        ) {
            Text("填入表单", color = Color.White)
        }
    }
}
