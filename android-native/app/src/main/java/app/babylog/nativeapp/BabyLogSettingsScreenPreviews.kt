@file:Suppress("FunctionNaming", "MatchingDeclarationName", "UnusedPrivateMember")

package app.babylog.nativeapp

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun syncSettingsScreenPreview() {
    babyLogPreview {
        SyncSettingsScreen(
            config = previewBackendConfig(),
            familyKeyConfigured = true,
            checkingConnection = false,
            connectionMessage = "上次连接成功",
            connectionOk = true,
            pendingSyncCount = 2,
            syncedSyncCount = 8,
            failedSyncCount = 0,
            pendingAttachmentUploadCount = 1,
            pendingAttachmentUploadBytes = 384_000L,
            pendingAttachmentDownloadCount = 0,
            pushingSync = false,
            pushMessage = "",
            pullingSync = false,
            pullMessage = "",
            lastPulledAt = "2026-06-09 08:55",
            remoteUpdateBannerCount = 1,
            onBack = {},
            onCheckConnection = { _, _ -> },
            onPushNow = {},
            onPullNow = {},
            onSave = { _, _ -> }
        )
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun smartModelSettingsScreenPreview() {
    babyLogPreview {
        SmartModelSettingsScreen(
            config = BabyLogSmartConfigStore.Config(
                "https://dashscope.aliyuncs.com/compatible-mode/v1",
                "qwen-plus",
                "preview-key",
                true
            ),
            onBack = {},
            onSave = {}
        )
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun speechSettingsScreenPreview() {
    babyLogPreview {
        SpeechSettingsScreen(
            config = BabyLogSmartConfigStore.SpeechConfig(
                "preview-key",
                "paraformer-realtime-v2",
                true
            ),
            onBack = {},
            onSave = {}
        )
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun profileSettingsScreenPreview() {
    val profile = previewProfile()
    babyLogPreview {
        ProfileSettingsScreen(
            state = ProfileDialogState(
                title = "宝宝档案",
                profile = profile,
                firstRun = false,
                initialStage = BabyLogDomain.STAGE_PREGNANCY
            ),
            onBack = {},
            onOpenDueDateCalculator = { _, _ -> },
            onSave = { _, _ -> }
        )
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun dueDateCalcScreenPreview() {
    babyLogPreview {
        DueDateCalcScreen(
            currentExpectedDueDate = previewProfile().expectedDueDate,
            onBack = {},
            onApplyDueDate = {}
        )
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun reminderCenterScreenPreview() {
    babyLogPreview {
        ReminderCenterScreen(
            reminders = previewReminders(),
            systemMuted = false,
            notificationPermissionGranted = false,
            onBack = {},
            onRequestNotificationPermission = {},
            onSaveUserReminder = { _, _, _, _, _, _, done -> done() },
            onToggleReminder = { _, _ -> },
            onDismissReminder = {},
            onCompleteReminder = {},
            onDeleteReminder = {}
        )
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun preVisitQuestionsScreenPreview() {
    babyLogPreview {
        PreVisitQuestionsScreen(
            questions = previewQuestions(),
            onBack = {},
            onSave = { _, _, _, done -> done() },
            onDelete = {}
        )
    }
}
