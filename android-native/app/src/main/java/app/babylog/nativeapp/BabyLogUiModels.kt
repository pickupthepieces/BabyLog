@file:Suppress("MatchingDeclarationName")

package app.babylog.nativeapp

internal data class BabyLogUiState(
    val dashboard: BabyLogService.DashboardSnapshot? = null,
    val timeline: List<BabyLogDomain.BabyLogEvent> = emptyList(),
    val trashEvents: List<BabyLogDomain.BabyLogEvent> = emptyList(),
    val attachments: List<BabyLogDomain.AttachmentRecord> = emptyList(),
    val syncConfig: BabyLogDomain.BackendConfig = BabyLogDomain.BackendConfig.disabled(),
    val childProfile: BabyLogDomain.ChildProfile = BabyLogDomain.ChildProfile.empty(),
    val preVisitQuestions: List<BabyLogPreVisitQuestionStore.Question> = emptyList(),
    val reminders: List<BabyLogReminderStore.Reminder> = emptyList(),
    val notificationPermissionGranted: Boolean = true,
    val setupCompleted: Boolean = false,
    val disclaimerAccepted: Boolean = false,
    val hasImportUndoSnapshot: Boolean = false,
    val lastBackupExportMs: Long = 0L
)

internal data class ProfileDialogState(
    val title: String,
    val profile: BabyLogDomain.ChildProfile,
    val firstRun: Boolean,
    val initialStage: String
)

internal data class ProfileInput(
    val nickname: String,
    val sex: String,
    val expectedDueDate: String,
    val birthDate: String,
    val prePregnancyWeightKg: String,
    val heightCm: String,
    val stageOverride: String
)

internal data class OptionalProfileNumber(val value: Double?)

internal data class ImportPreview(
    val eventCount: Int,
    val profileLabel: String
)

internal data class ImportConfirmState(
    val raw: String,
    val eventCount: Int,
    val profileLabel: String
)

internal data class SyncConfirmState(
    val backendBaseUrl: String,
    val familyKey: String
)

internal data class SyncPushConfirmState(
    val backendBaseUrl: String,
    val pendingCount: Int
)

internal data class AttachmentListPageState(
    val title: String,
    val attachments: List<BabyLogDomain.AttachmentRecord>
)

internal data class InfoDialogState(
    val title: String,
    val message: String
)

internal data class SmartEntryDraft(
    val nonce: Long = System.nanoTime(),
    val values: Map<String, String> = emptyMap()
)

internal enum class ImagePickTarget(val fileName: String) {
    Ultrasound("ultrasound.jpg"),
    Checkup("checkup.jpg")
}

internal data class SmartVoiceUiState(
    val isRecording: Boolean = false,
    val isTranscribing: Boolean = false,
    val transcript: String = "",
    val transcriptNonce: Long = 0L,
    val message: String = ""
)
