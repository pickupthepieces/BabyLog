@file:Suppress("MatchingDeclarationName")

package app.babylog.nativeapp

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import app.babylog.nativeapp.ui.screens.BabyLogRoutes

internal class HomeViewModel : ViewModel() {
    var uiState by mutableStateOf(BabyLogUiState())
    var highlightedEventId by mutableStateOf<String?>(null)
    var timelineFilter by mutableStateOf("all")
    var selectedBabyDay by mutableStateOf(BabyLogFormatters.todayDateInput())
    var quickUndoRequest by mutableStateOf<QuickUndoRequest?>(null)
    var attachmentListPageState by mutableStateOf<AttachmentListPageState?>(null)
    var previewAttachment by mutableStateOf<BabyLogDomain.AttachmentRecord?>(null)
}

internal class RecordViewModel : ViewModel() {
    var recordDetailEventId by mutableStateOf<String?>(null)
    var recordDetailReturnRoute by mutableStateOf(BabyLogRoutes.Timeline)
    var babyCareAction by mutableStateOf<BabyLogService.QuickAction?>(null)
    var pregnancyAction by mutableStateOf<BabyLogService.QuickAction?>(null)
    var showFetalMovementSession by mutableStateOf(false)
    var ultrasoundOcrRunning by mutableStateOf(false)
    var checkupOcrRunning by mutableStateOf(false)
    var smartEntryRunning by mutableStateOf(false)
    var smartVoiceState by mutableStateOf(SmartVoiceUiState())
    var smartEntryCandidate by mutableStateOf<BabyLogSmartTextClient.SmartEntryCandidate?>(null)
    var babyCareDraft by mutableStateOf<SmartEntryDraft?>(null)
    var pregnancyDraft by mutableStateOf<SmartEntryDraft?>(null)
    var maternalMetricDraft by mutableStateOf<SmartEntryDraft?>(null)
    var ultrasoundDraft by mutableStateOf<SmartEntryDraft?>(null)
    var ultrasoundOcrCandidate by mutableStateOf<BabyLogSmartInput.UltrasoundOcrCandidate?>(null)
    var checkupOcrCandidate by mutableStateOf<BabyLogSmartTextClient.SmartFillCandidate?>(null)
    var editingEvent by mutableStateOf<BabyLogDomain.BabyLogEvent?>(null)
    var deleteEventConfirm by mutableStateOf<BabyLogDomain.BabyLogEvent?>(null)
    var pendingUltrasoundPhotoPath by mutableStateOf<String?>(null)
    var pendingUltrasoundPhotoName by mutableStateOf<String?>(null)
    var pendingCheckupAttachmentPath by mutableStateOf<String?>(null)
    var pendingCheckupAttachmentName by mutableStateOf<String?>(null)
    var pendingImageTarget by mutableStateOf(ImagePickTarget.Ultrasound)
}

internal class SyncViewModel : ViewModel() {
    var syncConfirmState by mutableStateOf<SyncConfirmState?>(null)
    var syncFamilyKeyConfigured by mutableStateOf(false)
    var syncCheckRunning by mutableStateOf(false)
    var syncCheckMessage by mutableStateOf("")
    var syncCheckOk by mutableStateOf<Boolean?>(null)
    var syncPushRunning by mutableStateOf(false)
    var syncPushMessage by mutableStateOf("")
    var syncPushConfirmState by mutableStateOf<SyncPushConfirmState?>(null)
    var syncPullRunning by mutableStateOf(false)
    var syncPullMessage by mutableStateOf("")
}

internal class SettingsViewModel : ViewModel() {
    var smartSettingsConfig by mutableStateOf<BabyLogSmartConfigStore.Config?>(null)
    var speechSettingsConfig by mutableStateOf<BabyLogSmartConfigStore.SpeechConfig?>(null)
    var smartConfigSummary by mutableStateOf("智能识别未配置")
    var speechConfigSummary by mutableStateOf("语音识别未配置")
    var showClearLocalConfirm by mutableStateOf(false)
    var undoImportConfirm by mutableStateOf(false)
    var profilePageState by mutableStateOf<ProfileDialogState?>(null)
    var importConfirm by mutableStateOf<ImportConfirmState?>(null)
    var appUpdateRunning by mutableStateOf(false)
    var appUpdateStatus by mutableStateOf("未检查")
    var appUpdateCandidate by mutableStateOf<BabyLogAppUpdateManager.UpdateInfo?>(null)
    var infoDialog by mutableStateOf<InfoDialogState?>(null)
}

internal class NavigationViewModel : ViewModel() {
    var pendingNavRoute by mutableStateOf<String?>(null)
    var pendingNavNonce by mutableStateOf(0L)
    var recordReturnRoute by mutableStateOf(BabyLogRoutes.Home)
}
