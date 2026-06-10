@file:Suppress("FunctionNaming", "MatchingDeclarationName", "UnusedPrivateMember")

package app.babylog.nativeapp

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun babyCareFormScreenPreview() {
    babyLogPreview {
        BabyCareFormScreen(
            action = BabyLogService.QuickAction("喂奶", "记录本次喂养", ChestnutPalette.PrimaryArgb, "feeding"),
            draft = SmartEntryDraft(values = mapOf("primary" to "120", "note" to "精神状态好")),
            isEditing = false,
            voiceState = PreviewSmartVoiceState,
            onLongTextVoiceStart = PreviewLongTextVoiceStart,
            onLongTextVoiceStop = {},
            onBack = {},
            onSave = {}
        )
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun missingRecordRouteScreenPreview() {
    babyLogPreview {
        MissingRecordRouteScreen(title = "暂不支持的记录", onBack = {})
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun pregnancyEventFormScreenPreview() {
    babyLogPreview {
        PregnancyEventFormScreen(
            action = BabyLogService.QuickAction("产检", "记录产检结果", ChestnutPalette.RoseArgb, "pregnancy_checkup"),
            draft = SmartEntryDraft(
                values = mapOf(
                    "primary" to "宁波妇儿医院",
                    "secondary" to "血压正常，尿常规正常",
                    "note" to "下次复查糖耐"
                )
            ),
            expectedDueDate = previewProfile().expectedDueDate,
            attachmentPath = null,
            attachmentName = null,
            ocrRunning = false,
            ocrCandidate = null,
            onPickAttachment = {},
            onCaptureAttachment = {},
            onRecognizeAttachment = {},
            onCandidateDismiss = {},
            onCandidateApplied = {},
            voiceState = PreviewSmartVoiceState,
            onLongTextVoiceStart = PreviewLongTextVoiceStart,
            onLongTextVoiceStop = {},
            onBack = {},
            onSave = {}
        )
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun maternalMetricFormScreenPreview() {
    babyLogPreview {
        MaternalMetricFormScreen(
            draft = SmartEntryDraft(
                values = mapOf("weightKg" to "59.6", "systolicBp" to "112", "diastolicBp" to "72")
            ),
            voiceState = PreviewSmartVoiceState,
            onLongTextVoiceStart = PreviewLongTextVoiceStart,
            onLongTextVoiceStop = {},
            onBack = {},
            onSave = {}
        )
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun ultrasoundFormScreenPreview() {
    babyLogPreview {
        UltrasoundFormScreen(
            defaultGestationalAge = "25+3",
            expectedDueDate = previewProfile().expectedDueDate,
            draft = SmartEntryDraft(values = mapOf("bpdMm" to "64", "flMm" to "48", "efwG" to "760")),
            photoPath = null,
            photoName = null,
            ocrRunning = false,
            ocrCandidate = null,
            onPickPhoto = {},
            onCapturePhoto = {},
            onRecognizePhoto = {},
            onCandidateDismiss = {},
            onCandidateApplied = {},
            voiceState = PreviewSmartVoiceState,
            onLongTextVoiceStart = PreviewLongTextVoiceStart,
            onLongTextVoiceStop = {},
            onBack = {},
            onSave = {}
        )
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun contractionSessionScreenPreview() {
    babyLogPreview {
        ContractionSessionScreen(onBack = {}, onSave = {})
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun recordDetailScreenPreview() {
    babyLogPreview {
        RecordDetailScreen(
            event = previewEvents().first(),
            allEvents = previewEvents(),
            attachments = previewAttachments(),
            onBack = {},
            onPreviewAttachment = {},
            onOpenPreVisitQuestions = {},
            onEdit = {},
            onDelete = {}
        )
    }
}
