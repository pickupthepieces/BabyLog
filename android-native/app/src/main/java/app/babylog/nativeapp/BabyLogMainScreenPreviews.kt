@file:Suppress("FunctionNaming", "MatchingDeclarationName", "UnusedPrivateMember")

package app.babylog.nativeapp

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import app.babylog.nativeapp.ui.screens.BabyLogRoutes

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun homeScreenPreview() {
    babyLogPreview {
        HomeScreen(
            inner = PreviewInnerPadding,
            state = previewUiState(),
            selectedBabyDay = "2026-06-09",
            highlightedEventId = "event_metric_1",
            onBabyDaySelected = {},
            onShowTimeline = {},
            onOpenDetail = {},
            onOpenWeightGain = {},
            onOpenReminderCenter = {},
            syncPulling = false,
            onPullSyncNow = {},
            onDismissSyncBanner = {},
            onQuickRailVisibilityChange = {}
        )
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun timelineScreenPreview() {
    babyLogPreview {
        TimelineScreen(
            inner = PreviewInnerPadding,
            state = previewUiState(),
            selectedFilter = BabyLogRoutes.Timeline,
            highlightedEventId = "event_ultrasound_1",
            syncPulling = false,
            onFilterSelected = {},
            onPullSyncNow = {},
            onDismissSyncBanner = {},
            onOpenDetail = {}
        )
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun libraryRootScreenPreview() {
    babyLogPreview {
        LibraryRootScreen(
            inner = PreviewInnerPadding,
            state = previewUiState(),
            onShowAttachments = { _, _ -> },
            onOpenVisitSummary = {},
            onOpenPreVisitQuestions = {}
        )
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun settingsRootScreenPreview() {
    babyLogPreview {
        SettingsRootScreen(
            inner = PreviewInnerPadding,
            state = previewUiState(),
            smartConfigSummary = "模型已配置",
            speechConfigSummary = "语音识别已配置",
            onSyncNow = {},
            onExportBackup = {},
            onImportBackup = {},
            onUndoImport = {},
            onOpenSyncSettings = {},
            onOpenSmartSettings = {},
            onOpenSpeechSettings = {},
            onClearLocalData = {},
            onOpenTrash = {},
            onOpenDisclaimer = {},
            onOpenDueDateCalculator = {},
            onOpenWeightGain = {},
            appVersionLabel = "1.0.0-preview",
            appUpdateStatus = "预览构建",
            appUpdateRunning = false,
            onCheckAppUpdate = {},
            onOpenPreVisitQuestions = {},
            onOpenReminderCenter = {},
            onEditProfile = {}
        )
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun smartEntryScreenPreview() {
    babyLogPreview {
        SmartEntryScreen(
            running = false,
            voiceState = SmartVoiceUiState(message = "可以输入或按住语音按钮"),
            candidate = null,
            onBack = {},
            onVoiceStart = {},
            onVoiceStop = {},
            onSubmit = {},
            onOpenCandidate = {},
            onDismissCandidate = {}
        )
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun medicalDisclaimerGateScreenPreview() {
    babyLogPreview {
        MedicalDisclaimerGateScreen(onAccept = {})
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun medicalDisclaimerReviewScreenPreview() {
    babyLogPreview {
        MedicalDisclaimerReviewScreen(onBack = {})
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun firstRunScreenPreview() {
    babyLogPreview {
        FirstRunScreen(
            onCreatePregnancyProfile = {},
            onCreateBabyProfile = {},
            onImportBackup = {}
        )
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun libraryScreenPreview() {
    babyLogPreview {
        LibraryScreen(
            attachments = previewAttachments(),
            stage = BabyLogDomain.STAGE_PREGNANCY,
            onShowAttachments = { _, _ -> }
        )
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun settingsScreenPreview() {
    babyLogPreview {
        SettingsScreen(
            state = previewUiState(),
            onSyncNow = {},
            onExportBackup = {},
            onImportBackup = {},
            onUndoImport = {},
            onOpenSyncSettings = {},
            onOpenSmartSettings = {},
            onOpenSpeechSettings = {},
            smartConfigSummary = "模型已配置",
            speechConfigSummary = "语音识别已配置",
            onClearLocalData = {},
            onOpenTrash = {},
            onOpenDisclaimer = {},
            onOpenDueDateCalculator = {},
            onOpenWeightGain = {},
            appVersionLabel = "1.0.0-preview",
            appUpdateStatus = "预览构建",
            appUpdateRunning = false,
            onCheckAppUpdate = {},
            onOpenPreVisitQuestions = {},
            onOpenReminderCenter = {},
            onEditProfile = {}
        )
    }
}
