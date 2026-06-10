@file:Suppress("LongParameterList")

package app.babylog.nativeapp

internal data class BabyLogAppState(
    val ui: BabyLogUiState,
    val navigation: BabyLogNavigationState,
    val home: BabyLogHomeState,
    val library: BabyLogLibraryState,
    val settings: BabyLogSettingsState,
    val sync: BabyLogSyncState,
    val record: BabyLogRecordState,
    val smartEntry: BabyLogSmartEntryState
)

internal data class BabyLogNavigationState(
    val pendingNavRoute: String?,
    val pendingNavNonce: Long,
    val recordReturnRoute: String,
    val recordDetailEventId: String?,
    val recordDetailReturnRoute: String,
    val highlightedEventId: String?,
    val timelineFilter: String,
    val selectedBabyDay: String
)

internal data class BabyLogHomeState(
    val quickActions: List<BabyLogService.QuickAction>
)

internal data class BabyLogLibraryState(
    val attachmentListPageState: AttachmentListPageState?,
    val previewAttachment: BabyLogDomain.AttachmentRecord?
)

internal data class BabyLogSettingsState(
    val profilePageState: ProfileDialogState?,
    val smartSettingsConfig: BabyLogSmartConfigStore.Config?,
    val speechSettingsConfig: BabyLogSmartConfigStore.SpeechConfig?,
    val smartConfigSummary: String,
    val speechConfigSummary: String,
    val appVersionLabel: String,
    val appUpdateStatus: String,
    val appUpdateRunning: Boolean
)

internal data class BabyLogSyncState(
    val familyKeyConfigured: Boolean,
    val checkRunning: Boolean,
    val checkMessage: String,
    val checkOk: Boolean?,
    val pushRunning: Boolean,
    val pushMessage: String,
    val pullRunning: Boolean,
    val pullMessage: String
)

internal data class BabyLogRecordState(
    val babyCareAction: BabyLogService.QuickAction?,
    val babyCareDraft: SmartEntryDraft?,
    val pregnancyAction: BabyLogService.QuickAction?,
    val pregnancyDraft: SmartEntryDraft?,
    val maternalMetricDraft: SmartEntryDraft?,
    val ultrasoundDraft: SmartEntryDraft?,
    val editingEventType: String?,
    val pendingUltrasoundPhotoPath: String?,
    val pendingUltrasoundPhotoName: String?,
    val pendingCheckupAttachmentPath: String?,
    val pendingCheckupAttachmentName: String?,
    val ultrasoundOcrRunning: Boolean,
    val ultrasoundOcrCandidate: BabyLogSmartInput.UltrasoundOcrCandidate?,
    val checkupOcrRunning: Boolean,
    val checkupOcrCandidate: BabyLogSmartTextClient.SmartFillCandidate?
)

internal data class BabyLogSmartEntryState(
    val running: Boolean,
    val voiceState: SmartVoiceUiState,
    val candidate: BabyLogSmartTextClient.SmartEntryCandidate?
)

internal data class BabyLogAppActions(
    val navigation: BabyLogNavigationActions,
    val library: BabyLogLibraryActions,
    val settings: BabyLogSettingsActions,
    val sync: BabyLogSyncActions,
    val profile: BabyLogProfileActions,
    val reminder: BabyLogReminderActions,
    val record: BabyLogRecordActions,
    val smartEntry: BabyLogSmartEntryActions
)

internal data class BabyLogNavigationActions(
    val onNavRouteConsumed: () -> Unit,
    val onTimelineFilterSelected: (String) -> Unit,
    val onBabyDaySelected: (String) -> Unit,
    val onSmartEntryClick: (String) -> Unit,
    val onSmartVoiceHoldStart: (String) -> Unit,
    val onSmartVoiceHoldEnd: (String) -> Unit,
    val onQuickAction: (BabyLogService.QuickAction, String) -> String?
)

internal data class BabyLogLibraryActions(
    val onShowAttachments: (String, List<BabyLogDomain.AttachmentRecord>) -> Unit,
    val onOpenVisitSummary: () -> Unit,
    val onOpenPreVisitQuestions: () -> Unit,
    val onCloseAttachmentList: () -> Unit,
    val onPreviewAttachment: (BabyLogDomain.AttachmentRecord) -> Unit,
    val onCloseAttachmentPreview: () -> Unit,
    val onCopyVisitSummary: (String) -> Unit,
    val onShareVisitSummary: (String) -> Unit,
    val onSaveVisitSummary: (String) -> Unit,
    val onPolishVisitSummary: (String, (String?) -> Unit) -> Unit
)

internal data class BabyLogSettingsActions(
    val onSyncNow: () -> Unit,
    val onExportBackup: () -> Unit,
    val onImportBackup: () -> Unit,
    val onUndoImport: () -> Unit,
    val onOpenSyncSettings: () -> Unit,
    val onOpenSmartSettings: () -> Unit,
    val onOpenSpeechSettings: () -> Unit,
    val onAcceptDisclaimer: () -> Unit,
    val onCloseSettingsPage: () -> Unit,
    val onSaveSmartSettings: (BabyLogSmartConfigStore.Config) -> Unit,
    val onSaveSpeechSettings: (BabyLogSmartConfigStore.SpeechConfig) -> Unit,
    val onClearLocalData: () -> Unit,
    val onOpenTrash: () -> Unit,
    val onOpenDisclaimer: () -> Unit,
    val onOpenDueDateCalculator: () -> Unit,
    val onOpenWeightGain: () -> Unit,
    val onCheckAppUpdate: () -> Unit
)

internal data class BabyLogSyncActions(
    val onCheckConnection: (String, String) -> Unit,
    val onPushNow: () -> Unit,
    val onPullNow: () -> Unit,
    val onDismissRemoteUpdateBanner: () -> Unit,
    val onSaveSettings: (String, String) -> Unit
)

internal data class BabyLogProfileActions(
    val onSaveProfile: (ProfileInput, Boolean) -> Unit,
    val onOpenDueDateCalculatorFromProfile: (ProfileInput, Boolean) -> Unit,
    val onApplyDueDateFromCalculator: (String) -> Unit,
    val onCreatePregnancyProfile: () -> Unit,
    val onCreateBabyProfile: () -> Unit,
    val onEditProfile: () -> Unit
)

internal data class BabyLogReminderActions(
    val onSavePreVisitQuestion: (String?, String, String, () -> Unit) -> Unit,
    val onDeletePreVisitQuestion: (BabyLogPreVisitQuestionStore.Question) -> Unit,
    val onOpenReminderCenter: () -> Unit,
    val onRequestNotificationPermission: () -> Unit,
    val onSaveUserReminder: (String?, String, String, String, String, Boolean, () -> Unit) -> Unit,
    val onToggleReminder: (BabyLogReminderStore.Reminder, Boolean) -> Unit,
    val onDismissReminder: (BabyLogReminderStore.Reminder) -> Unit,
    val onCompleteReminder: (BabyLogReminderStore.Reminder) -> Unit,
    val onDeleteReminder: (BabyLogReminderStore.Reminder) -> Unit,
    val onRestoreTrashEvent: (BabyLogDomain.BabyLogEvent) -> Unit
)

internal data class BabyLogRecordActions(
    val onOpenEventDetail: (BabyLogDomain.BabyLogEvent, String) -> Unit,
    val onEditEvent: (BabyLogDomain.BabyLogEvent, String) -> Unit,
    val onDeleteEvent: (BabyLogDomain.BabyLogEvent) -> Unit,
    val onBabyCareCancel: () -> Unit,
    val onPregnancyCancel: () -> Unit,
    val onMaternalMetricCancel: () -> Unit,
    val onUltrasoundCancel: () -> Unit,
    val onBabyCareSave: (BabyLogService.BabyCareInput) -> Unit,
    val onPregnancySave: (BabyLogService.PregnancyInput) -> Unit,
    val onContractionSessionSave: (BabyLogService.ContractionSessionInput) -> Unit,
    val onMaternalMetricSave: (BabyLogService.MaternalMetricInput) -> Unit,
    val onUltrasoundSave: (BabyLogService.UltrasoundInput) -> Unit,
    val onPickCheckupAttachment: () -> Unit,
    val onCaptureCheckupAttachment: () -> Unit,
    val onPickUltrasoundPhoto: () -> Unit,
    val onCaptureUltrasoundPhoto: () -> Unit,
    val onRecognizeUltrasoundPhoto: () -> Unit,
    val onDismissUltrasoundCandidate: () -> Unit,
    val onApplyUltrasoundCandidate: () -> Unit,
    val onRecognizeCheckupAttachment: () -> Unit,
    val onDismissCheckupCandidate: () -> Unit,
    val onApplyCheckupCandidate: () -> Unit,
    val onLongTextVoiceStart: LongTextVoiceStart,
    val onLongTextVoiceStop: () -> Unit
)

internal data class BabyLogSmartEntryActions(
    val onBack: () -> Unit,
    val onVoiceStart: () -> Unit,
    val onVoiceStop: () -> Unit,
    val onSubmit: (String) -> Unit,
    val onCandidateConfirm: (BabyLogSmartTextClient.SmartEntryCandidate) -> Unit,
    val onCandidateDismiss: () -> Unit
)
