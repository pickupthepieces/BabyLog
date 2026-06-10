package app.babylog.nativeapp

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.AlertDialog
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.ui.graphics.Color
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import org.json.JSONException
import org.json.JSONObject
import app.babylog.nativeapp.ui.screens.BabyLogRoutes
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

public final class ComposeMainActivity : ComponentActivity() {
    internal lateinit var repository: BabyLogRepository
    internal lateinit var service: BabyLogService
    private lateinit var smartConfigStore: BabyLogSmartConfigStore
    internal lateinit var syncSecretStore: BabyLogSyncSecretStore
    private lateinit var disclaimerStore: BabyLogDisclaimerStore
    private lateinit var preVisitQuestionStore: BabyLogPreVisitQuestionStore
    private lateinit var reminderStore: BabyLogReminderStore
    private val smartVisionClient = BabyLogSmartVisionClient()
    private val smartTextClient = BabyLogSmartTextClient()
    private val speechClient = BabyLogParaformerSpeechClient()
    internal val remoteSyncClient = BabyLogRemoteSyncClient()
    private lateinit var audioPermissionLauncher: ActivityResultLauncher<String>
    private lateinit var notificationPermissionLauncher: ActivityResultLauncher<String>
    private lateinit var cameraPermissionLauncher: ActivityResultLauncher<String>
    private lateinit var cameraLauncher: ActivityResultLauncher<Uri>
    private lateinit var pickImageLauncher: ActivityResultLauncher<String>
    private lateinit var exportBackupLauncher: ActivityResultLauncher<String>
    private lateinit var importBackupLauncher: ActivityResultLauncher<Array<String>>
    private lateinit var visitSummarySaveLauncher: ActivityResultLauncher<String>

    private lateinit var homeViewModel: HomeViewModel
    private lateinit var recordViewModel: RecordViewModel
    private lateinit var syncViewModel: SyncViewModel
    private lateinit var settingsViewModel: SettingsViewModel
    private lateinit var navigationViewModel: NavigationViewModel

    internal var uiState: BabyLogUiState
        get() = homeViewModel.uiState
        set(value) { homeViewModel.uiState = value }
    private var pendingNavRoute: String?
        get() = navigationViewModel.pendingNavRoute
        set(value) { navigationViewModel.pendingNavRoute = value }
    private var pendingNavNonce: Long
        get() = navigationViewModel.pendingNavNonce
        set(value) { navigationViewModel.pendingNavNonce = value }
    private var recordReturnRoute: String
        get() = navigationViewModel.recordReturnRoute
        set(value) { navigationViewModel.recordReturnRoute = value }
    private var recordDetailEventId: String?
        get() = recordViewModel.recordDetailEventId
        set(value) { recordViewModel.recordDetailEventId = value }
    private var recordDetailReturnRoute: String
        get() = recordViewModel.recordDetailReturnRoute
        set(value) { recordViewModel.recordDetailReturnRoute = value }
    private var highlightedEventId: String?
        get() = homeViewModel.highlightedEventId
        set(value) { homeViewModel.highlightedEventId = value }
    private var timelineFilter: String
        get() = homeViewModel.timelineFilter
        set(value) { homeViewModel.timelineFilter = value }
    private var selectedBabyDay: String
        get() = homeViewModel.selectedBabyDay
        set(value) { homeViewModel.selectedBabyDay = value }
    private var quickUndoRequest: QuickUndoRequest?
        get() = homeViewModel.quickUndoRequest
        set(value) { homeViewModel.quickUndoRequest = value }
    private var babyCareAction: BabyLogService.QuickAction?
        get() = recordViewModel.babyCareAction
        set(value) { recordViewModel.babyCareAction = value }
    private var pregnancyAction: BabyLogService.QuickAction?
        get() = recordViewModel.pregnancyAction
        set(value) { recordViewModel.pregnancyAction = value }
    private var showFetalMovementSession: Boolean
        get() = recordViewModel.showFetalMovementSession
        set(value) { recordViewModel.showFetalMovementSession = value }
    private var smartSettingsConfig: BabyLogSmartConfigStore.Config?
        get() = settingsViewModel.smartSettingsConfig
        set(value) { settingsViewModel.smartSettingsConfig = value }
    private var speechSettingsConfig: BabyLogSmartConfigStore.SpeechConfig?
        get() = settingsViewModel.speechSettingsConfig
        set(value) { settingsViewModel.speechSettingsConfig = value }
    private var smartConfigSummary: String
        get() = settingsViewModel.smartConfigSummary
        set(value) { settingsViewModel.smartConfigSummary = value }
    private var speechConfigSummary: String
        get() = settingsViewModel.speechConfigSummary
        set(value) { settingsViewModel.speechConfigSummary = value }
    private var ultrasoundOcrRunning: Boolean
        get() = recordViewModel.ultrasoundOcrRunning
        set(value) { recordViewModel.ultrasoundOcrRunning = value }
    private var checkupOcrRunning: Boolean
        get() = recordViewModel.checkupOcrRunning
        set(value) { recordViewModel.checkupOcrRunning = value }
    private var smartEntryRunning: Boolean
        get() = recordViewModel.smartEntryRunning
        set(value) { recordViewModel.smartEntryRunning = value }
    private var smartVoiceState: SmartVoiceUiState
        get() = recordViewModel.smartVoiceState
        set(value) { recordViewModel.smartVoiceState = value }
    private var smartEntryCandidate: BabyLogSmartTextClient.SmartEntryCandidate?
        get() = recordViewModel.smartEntryCandidate
        set(value) { recordViewModel.smartEntryCandidate = value }
    private var longTextVoiceApply: ((String) -> Unit)? = null
    private var babyCareDraft: SmartEntryDraft?
        get() = recordViewModel.babyCareDraft
        set(value) { recordViewModel.babyCareDraft = value }
    private var pregnancyDraft: SmartEntryDraft?
        get() = recordViewModel.pregnancyDraft
        set(value) { recordViewModel.pregnancyDraft = value }
    private var maternalMetricDraft: SmartEntryDraft?
        get() = recordViewModel.maternalMetricDraft
        set(value) { recordViewModel.maternalMetricDraft = value }
    private var ultrasoundDraft: SmartEntryDraft?
        get() = recordViewModel.ultrasoundDraft
        set(value) { recordViewModel.ultrasoundDraft = value }
    private var ultrasoundOcrCandidate: BabyLogSmartInput.UltrasoundOcrCandidate?
        get() = recordViewModel.ultrasoundOcrCandidate
        set(value) { recordViewModel.ultrasoundOcrCandidate = value }
    private var checkupOcrCandidate: BabyLogSmartTextClient.SmartFillCandidate?
        get() = recordViewModel.checkupOcrCandidate
        set(value) { recordViewModel.checkupOcrCandidate = value }
    private var showClearLocalConfirm: Boolean
        get() = settingsViewModel.showClearLocalConfirm
        set(value) { settingsViewModel.showClearLocalConfirm = value }
    private var undoImportConfirm: Boolean
        get() = settingsViewModel.undoImportConfirm
        set(value) { settingsViewModel.undoImportConfirm = value }
    private var profilePageState: ProfileDialogState?
        get() = settingsViewModel.profilePageState
        set(value) { settingsViewModel.profilePageState = value }
    private var importConfirm: ImportConfirmState?
        get() = settingsViewModel.importConfirm
        set(value) { settingsViewModel.importConfirm = value }
    internal var syncConfirmState: SyncConfirmState?
        get() = syncViewModel.syncConfirmState
        set(value) { syncViewModel.syncConfirmState = value }
    internal var syncFamilyKeyConfigured: Boolean
        get() = syncViewModel.syncFamilyKeyConfigured
        set(value) { syncViewModel.syncFamilyKeyConfigured = value }
    private var syncCheckRunning: Boolean
        get() = syncViewModel.syncCheckRunning
        set(value) { syncViewModel.syncCheckRunning = value }
    private var syncCheckMessage: String
        get() = syncViewModel.syncCheckMessage
        set(value) { syncViewModel.syncCheckMessage = value }
    private var syncCheckOk: Boolean?
        get() = syncViewModel.syncCheckOk
        set(value) { syncViewModel.syncCheckOk = value }
    internal var syncPushRunning: Boolean
        get() = syncViewModel.syncPushRunning
        set(value) { syncViewModel.syncPushRunning = value }
    internal var syncPushMessage: String
        get() = syncViewModel.syncPushMessage
        set(value) { syncViewModel.syncPushMessage = value }
    internal var syncPushConfirmState: SyncPushConfirmState?
        get() = syncViewModel.syncPushConfirmState
        set(value) { syncViewModel.syncPushConfirmState = value }
    internal var syncPullRunning: Boolean
        get() = syncViewModel.syncPullRunning
        set(value) { syncViewModel.syncPullRunning = value }
    internal var syncPullMessage: String
        get() = syncViewModel.syncPullMessage
        set(value) { syncViewModel.syncPullMessage = value }
    internal var appUpdateRunning: Boolean
        get() = settingsViewModel.appUpdateRunning
        set(value) { settingsViewModel.appUpdateRunning = value }
    internal var appUpdateStatus: String
        get() = settingsViewModel.appUpdateStatus
        set(value) { settingsViewModel.appUpdateStatus = value }
    internal var appUpdateCandidate: BabyLogAppUpdateManager.UpdateInfo?
        get() = settingsViewModel.appUpdateCandidate
        set(value) { settingsViewModel.appUpdateCandidate = value }
    private var attachmentListPageState: AttachmentListPageState?
        get() = homeViewModel.attachmentListPageState
        set(value) { homeViewModel.attachmentListPageState = value }
    private var previewAttachment: BabyLogDomain.AttachmentRecord?
        get() = homeViewModel.previewAttachment
        set(value) { homeViewModel.previewAttachment = value }
    private var editingEvent: BabyLogDomain.BabyLogEvent?
        get() = recordViewModel.editingEvent
        set(value) { recordViewModel.editingEvent = value }
    private var deleteEventConfirm: BabyLogDomain.BabyLogEvent?
        get() = recordViewModel.deleteEventConfirm
        set(value) { recordViewModel.deleteEventConfirm = value }
    private var infoDialog: InfoDialogState?
        get() = settingsViewModel.infoDialog
        set(value) { settingsViewModel.infoDialog = value }
    private var voiceRecorder: BabyLogPcmVoiceRecorder? = null
    private var pendingCameraFile: File? = null
    private var pendingUltrasoundPhotoPath: String?
        get() = recordViewModel.pendingUltrasoundPhotoPath
        set(value) { recordViewModel.pendingUltrasoundPhotoPath = value }
    private var pendingUltrasoundPhotoName: String?
        get() = recordViewModel.pendingUltrasoundPhotoName
        set(value) { recordViewModel.pendingUltrasoundPhotoName = value }
    private var pendingCheckupAttachmentPath: String?
        get() = recordViewModel.pendingCheckupAttachmentPath
        set(value) { recordViewModel.pendingCheckupAttachmentPath = value }
    private var pendingCheckupAttachmentName: String?
        get() = recordViewModel.pendingCheckupAttachmentName
        set(value) { recordViewModel.pendingCheckupAttachmentName = value }
    private var pendingImageTarget: ImagePickTarget
        get() = recordViewModel.pendingImageTarget
        set(value) { recordViewModel.pendingImageTarget = value }
    private var pendingVisitSummaryText: String? = null
    private val syncPullHandler = Handler(Looper.getMainLooper())
    private val foregroundPullRunnable = object : Runnable {
        override fun run() {
            if (shouldAutoPullSync()) {
                pullSyncNow(silent = true)
                syncPullHandler.postDelayed(this, 120_000L)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.AppTheme)
        super.onCreate(savedInstanceState)
        homeViewModel = ViewModelProvider(this)[HomeViewModel::class.java]
        recordViewModel = ViewModelProvider(this)[RecordViewModel::class.java]
        syncViewModel = ViewModelProvider(this)[SyncViewModel::class.java]
        settingsViewModel = ViewModelProvider(this)[SettingsViewModel::class.java]
        navigationViewModel = ViewModelProvider(this)[NavigationViewModel::class.java]
        window.statusBarColor = ChestnutPalette.BgArgb
        window.navigationBarColor = ChestnutPalette.SurfaceArgb
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            var systemUiFlags = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                systemUiFlags = systemUiFlags or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
            }
            window.decorView.systemUiVisibility = systemUiFlags
        }

        repository = BabyLogRepository(this)
        service = BabyLogService(this, repository, BabyLogWorkManagerSyncTrigger(this))
        smartConfigStore = BabyLogSmartConfigStore(this)
        syncSecretStore = BabyLogSyncSecretStore(this)
        disclaimerStore = BabyLogDisclaimerStore(this)
        preVisitQuestionStore = BabyLogPreVisitQuestionStore(this)
        reminderStore = BabyLogReminderStore(this)
        uiState = uiState.copy(disclaimerAccepted = disclaimerStore.hasAcceptedCurrentVersion())
        syncFamilyKeyConfigured = syncSecretStore.hasFamilyKey()
        registerLaunchers()
        refreshSmartConfigSummary()
        handleNavigationIntent(intent)

        setContent {
            ChestnutTheme {
                val appState = BabyLogAppState(
                    ui = uiState,
                    navigation = BabyLogNavigationState(
                        pendingNavRoute = pendingNavRoute,
                        pendingNavNonce = pendingNavNonce,
                        recordReturnRoute = recordReturnRoute,
                        recordDetailEventId = recordDetailEventId,
                        recordDetailReturnRoute = recordDetailReturnRoute,
                        highlightedEventId = highlightedEventId,
                        timelineFilter = timelineFilter,
                        selectedBabyDay = selectedBabyDay
                    ),
                    home = BabyLogHomeState(
                        quickActions = quickActions(),
                        quickUndoRequest = quickUndoRequest
                    ),
                    library = BabyLogLibraryState(
                        attachmentListPageState = attachmentListPageState,
                        previewAttachment = previewAttachment
                    ),
                    settings = BabyLogSettingsState(
                        profilePageState = profilePageState,
                        smartSettingsConfig = smartSettingsConfig,
                        speechSettingsConfig = speechSettingsConfig,
                        smartConfigSummary = smartConfigSummary,
                        speechConfigSummary = speechConfigSummary,
                        appVersionLabel = "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                        appUpdateStatus = appUpdateStatus,
                        appUpdateRunning = appUpdateRunning
                    ),
                    sync = BabyLogSyncState(
                        familyKeyConfigured = syncFamilyKeyConfigured,
                        checkRunning = syncCheckRunning,
                        checkMessage = syncCheckMessage,
                        checkOk = syncCheckOk,
                        pushRunning = syncPushRunning,
                        pushMessage = syncPushMessage,
                        pullRunning = syncPullRunning,
                        pullMessage = syncPullMessage
                    ),
                    record = BabyLogRecordState(
                        babyCareAction = babyCareAction,
                        babyCareDraft = babyCareDraft,
                        pregnancyAction = pregnancyAction,
                        pregnancyDraft = pregnancyDraft,
                        maternalMetricDraft = maternalMetricDraft,
                        ultrasoundDraft = ultrasoundDraft,
                        editingEventType = editingEvent?.eventType,
                        pendingUltrasoundPhotoPath = pendingUltrasoundPhotoPath,
                        pendingUltrasoundPhotoName = pendingUltrasoundPhotoName,
                        pendingCheckupAttachmentPath = pendingCheckupAttachmentPath,
                        pendingCheckupAttachmentName = pendingCheckupAttachmentName,
                        ultrasoundOcrRunning = ultrasoundOcrRunning,
                        ultrasoundOcrCandidate = ultrasoundOcrCandidate,
                        checkupOcrRunning = checkupOcrRunning,
                        checkupOcrCandidate = checkupOcrCandidate
                    ),
                    smartEntry = BabyLogSmartEntryState(
                        running = smartEntryRunning,
                        voiceState = smartVoiceState,
                        candidate = smartEntryCandidate
                    )
                )
                val appActions = BabyLogAppActions(
                    navigation = BabyLogNavigationActions(
                        onNavRouteConsumed = { pendingNavRoute = null },
                        onTimelineFilterSelected = { timelineFilter = it },
                        onBabyDaySelected = { selectedBabyDay = it },
                        onSmartEntryClick = { sourceRoute ->
                            recordReturnRoute = sourceRoute
                            pendingNavRoute = BabyLogRoutes.SmartEntry
                        },
                        onSmartVoiceHoldStart = { sourceRoute ->
                            recordReturnRoute = sourceRoute
                            startNavVoiceRecording()
                        },
                        onSmartVoiceHoldEnd = { sourceRoute ->
                            recordReturnRoute = sourceRoute
                            finishNavVoiceRecording()
                        },
                        onQuickAction = { action, sourceRoute ->
                            recordReturnRoute = sourceRoute
                            handleQuickAction(action)
                        },
                        onQuickUndoRequestConsumed = ::consumeQuickUndoRequest,
                        onUndoQuickEvent = ::undoQuickEvent
                    ),
                    library = BabyLogLibraryActions(
                        onShowAttachments = { title, attachments ->
                            attachmentListPageState = AttachmentListPageState(title, attachments)
                            pendingNavRoute = BabyLogRoutes.LibraryAttachments
                        },
                        onOpenVisitSummary = {
                            pendingNavRoute = BabyLogRoutes.LibraryVisitSummary
                        },
                        onOpenPreVisitQuestions = {
                            pendingNavRoute = BabyLogRoutes.PreVisitQuestions
                        },
                        onCloseAttachmentList = {
                            attachmentListPageState = null
                        },
                        onPreviewAttachment = { attachment ->
                            previewAttachment = attachment
                            pendingNavRoute = BabyLogRoutes.AttachmentPreview
                        },
                        onCloseAttachmentPreview = {
                            previewAttachment = null
                        },
                        onCopyVisitSummary = ::copyVisitSummary,
                        onShareVisitSummary = ::shareVisitSummary,
                        onSaveVisitSummary = ::saveVisitSummary,
                        onPolishVisitSummary = ::polishVisitSummary
                    ),
                    settings = BabyLogSettingsActions(
                        onSyncNow = ::requestPushSyncNow,
                        onExportBackup = ::exportBackup,
                        onImportBackup = ::importBackup,
                        onUndoImport = { undoImportConfirm = true },
                        onOpenSyncSettings = { pendingNavRoute = BabyLogRoutes.SettingsSync },
                        onOpenSmartSettings = ::openSmartSettings,
                        onOpenSpeechSettings = ::openSpeechSettings,
                        onAcceptDisclaimer = ::acceptMedicalDisclaimer,
                        onCloseSettingsPage = {
                            profilePageState = null
                            smartSettingsConfig = null
                            speechSettingsConfig = null
                        },
                        onSaveSmartSettings = ::saveSmartSettings,
                        onSaveSpeechSettings = ::saveSpeechSettings,
                        onClearLocalData = { showClearLocalConfirm = true },
                        onOpenTrash = { pendingNavRoute = BabyLogRoutes.LibraryTrash },
                        onOpenDisclaimer = { pendingNavRoute = BabyLogRoutes.SettingsDisclaimer },
                        onOpenDueDateCalculator = { pendingNavRoute = BabyLogRoutes.SettingsDueDateCalc },
                        onOpenWeightGain = { pendingNavRoute = BabyLogRoutes.ToolsWeightGain },
                        onCheckAppUpdate = ::checkAppUpdate
                    ),
                    sync = BabyLogSyncActions(
                        onCheckConnection = ::checkSyncConnection,
                        onPushNow = ::requestPushSyncNow,
                        onPullNow = ::requestPullSyncNow,
                        onDismissRemoteUpdateBanner = ::dismissRemoteUpdateBanner,
                        onSaveSettings = ::saveSyncSettings
                    ),
                    profile = BabyLogProfileActions(
                        onSaveProfile = ::saveProfile,
                        onOpenDueDateCalculatorFromProfile = ::openDueDateCalculatorFromProfile,
                        onApplyDueDateFromCalculator = ::applyDueDateFromCalculator,
                        onCreatePregnancyProfile = { openNewFamilyForm(BabyLogDomain.STAGE_PREGNANCY) },
                        onCreateBabyProfile = { openNewFamilyForm(BabyLogDomain.STAGE_BABY) },
                        onEditProfile = { openProfileEditDialog() }
                    ),
                    reminder = BabyLogReminderActions(
                        onSavePreVisitQuestion = ::savePreVisitQuestion,
                        onDeletePreVisitQuestion = ::deletePreVisitQuestion,
                        onOpenReminderCenter = { pendingNavRoute = BabyLogRoutes.ReminderCenter },
                        onRequestNotificationPermission = ::requestNotificationPermission,
                        onSaveUserReminder = ::saveUserReminder,
                        onToggleReminder = ::toggleReminder,
                        onDismissReminder = ::dismissReminder,
                        onCompleteReminder = ::completeReminder,
                        onDeleteReminder = ::deleteReminder,
                        onRestoreTrashEvent = { event ->
                            restoreEvent(event.id)
                        }
                    ),
                    record = BabyLogRecordActions(
                        onOpenEventDetail = { event, sourceRoute -> openEventDetail(event, sourceRoute) },
                        onEditEvent = { event, sourceRoute -> openEventForEdit(event, sourceRoute) },
                        onDeleteEvent = { deleteEventConfirm = it },
                        onBabyCareCancel = {
                            editingEvent = null
                            babyCareAction = null
                            babyCareDraft = null
                        },
                        onPregnancyCancel = {
                            editingEvent = null
                            pregnancyAction = null
                            pregnancyDraft = null
                            pendingCheckupAttachmentPath = null
                            pendingCheckupAttachmentName = null
                            checkupOcrCandidate = null
                            checkupOcrRunning = false
                        },
                        onMaternalMetricCancel = {
                            editingEvent = null
                            maternalMetricDraft = null
                        },
                        onUltrasoundCancel = {
                            editingEvent = null
                            ultrasoundDraft = null
                            pendingUltrasoundPhotoPath = null
                            pendingUltrasoundPhotoName = null
                            ultrasoundOcrCandidate = null
                            ultrasoundOcrRunning = false
                        },
                        onBabyCareSave = ::recordBabyCare,
                        onPregnancySave = ::recordPregnancy,
                        onContractionSessionSave = ::recordContractionSession,
                        onMaternalMetricSave = ::recordMaternalMetric,
                        onUltrasoundSave = ::recordUltrasound,
                        onPickCheckupAttachment = { pickImage(ImagePickTarget.Checkup) },
                        onCaptureCheckupAttachment = { requestCameraOrLaunch(ImagePickTarget.Checkup) },
                        onPickUltrasoundPhoto = { pickImage(ImagePickTarget.Ultrasound) },
                        onCaptureUltrasoundPhoto = { requestCameraOrLaunch(ImagePickTarget.Ultrasound) },
                        onRecognizeUltrasoundPhoto = ::recognizeUltrasoundPhoto,
                        onDismissUltrasoundCandidate = { ultrasoundOcrCandidate = null },
                        onApplyUltrasoundCandidate = {
                            ultrasoundOcrCandidate = null
                            showToast("已应用识别字段，请核对后保存")
                        },
                        onRecognizeCheckupAttachment = ::recognizeCheckupAttachment,
                        onDismissCheckupCandidate = { checkupOcrCandidate = null },
                        onApplyCheckupCandidate = {
                            checkupOcrCandidate = null
                            showToast("已应用识别字段，请核对后保存")
                        },
                        onLongTextVoiceStart = ::startLongTextVoiceRecording,
                        onLongTextVoiceStop = ::finishLongTextVoiceRecording
                    ),
                    smartEntry = BabyLogSmartEntryActions(
                        onBack = {
                            if (!smartEntryRunning) {
                                cancelSmartVoiceRecording()
                                smartEntryCandidate = null
                            }
                        },
                        onVoiceStart = ::startSmartVoiceRecording,
                        onVoiceStop = ::finishSmartVoiceRecording,
                        onSubmit = ::requestSmartEntry,
                        onCandidateConfirm = ::openSmartEntryCandidate,
                        onCandidateDismiss = { smartEntryCandidate = null }
                    )
                )
                BabyLogApp(
                    appState = appState,
                    actions = appActions
                )

                if (smartVoiceState.isRecording) {
                    VoiceRecordingPopup()
                }

                if (showFetalMovementSession) {
                    FetalMovementSessionDialog(
                        voiceState = smartVoiceState,
                        onLongTextVoiceStart = ::startLongTextVoiceRecording,
                        onLongTextVoiceStop = ::finishLongTextVoiceRecording,
                        onDismiss = { showFetalMovementSession = false },
                        onSave = { input ->
                            showFetalMovementSession = false
                            recordFetalMovementSession(input)
                        }
                    )
                }

                importConfirm?.let { confirm ->
                    ConfirmDialog(
                        title = "确认导入备份",
                        message = "导入会覆盖当前本机全部记录、附件索引、待同步队列和宝宝档案。建议先导出当前数据。\n\n备份内容：${confirm.eventCount} 条记录，${confirm.profileLabel}",
                        confirmText = "覆盖导入",
                        destructive = true,
                        onDismiss = { importConfirm = null },
                        onConfirm = {
                            importConfirm = null
                            importBackupNow(confirm.raw)
                        }
                    )
                }

                SyncConfirmDialogs(
                    syncConfirmState = syncConfirmState,
                    syncPushConfirmState = syncPushConfirmState,
                    onDismissSyncConfirm = { syncConfirmState = null },
                    onConfirmSyncSettings = { confirm ->
                        syncConfirmState = null
                        persistSyncSettings(confirm.backendBaseUrl, confirm.familyKey)
                    },
                    onDismissPushConfirm = { syncPushConfirmState = null },
                    onConfirmPushSync = {
                        syncPushConfirmState = null
                        pushSyncNow()
                    }
                )

                if (showClearLocalConfirm) {
                    ConfirmDialog(
                        title = "清空本机数据",
                        message = "会删除本机记录、附件和同步队列。这个操作不能撤销。",
                        confirmText = "清空",
                        destructive = true,
                        onDismiss = { showClearLocalConfirm = false },
                        onConfirm = {
                            showClearLocalConfirm = false
                            clearLocalData()
                        }
                    )
                }

                if (undoImportConfirm) {
                    ConfirmDialog(
                        title = "撤销上次导入",
                        message = "会恢复到上一次导入前的本机快照，并覆盖当前本机记录、附件索引、待同步队列和宝宝档案。",
                        confirmText = "撤销导入",
                        destructive = true,
                        onDismiss = { undoImportConfirm = false },
                        onConfirm = {
                            undoImportConfirm = false
                            undoLastImport()
                        }
                    )
                }

                deleteEventConfirm?.let { event ->
                    ConfirmDialog(
                        title = "移入回收站",
                        message = "这条${BabyLogFormatters.eventLabel(event.eventType)}记录会先放入回收站，7 天内可在“设置 > 回收站”恢复；7 天后会自动永久删除。\n\n${BabyLogFormatters.formatEventDay(event.occurredAt)} ${BabyLogFormatters.formatEventTime(event.occurredAt)}\n${BabyLogFormatters.eventSummary(event)}",
                        confirmText = "放入回收站",
                        destructive = true,
                        onDismiss = { deleteEventConfirm = null },
                        onConfirm = {
                            val eventId = event.id
                            deleteEventConfirm = null
                            deleteEvent(eventId)
                        }
                    )
                }
                AppUpdateConfirmDialog(
                    update = appUpdateCandidate,
                    onDismiss = { appUpdateCandidate = null },
                    onConfirm = { update ->
                        appUpdateCandidate = null
                        downloadAndInstallAppUpdate(update)
                    }
                )

                infoDialog?.let { dialog ->
                    AlertDialog(
                        onDismissRequest = { infoDialog = null },
                        title = { Text(dialog.title, color = ChestnutPalette.Ink) },
                        text = { Text(dialog.message, color = ChestnutPalette.Muted) },
                        confirmButton = {
                            TextButton(onClick = { infoDialog = null }) {
                                Text("知道了", color = ChestnutPalette.Primary)
                            }
                        },
                        backgroundColor = ChestnutPalette.Surface
                    )
                }
            }
        }

        reloadData()
    }

    override fun onResume() {
        super.onResume()
        startForegroundPullLoop()
    }

    override fun onPause() {
        syncPullHandler.removeCallbacks(foregroundPullRunnable)
        super.onPause()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleNavigationIntent(intent)
    }

    private fun handleNavigationIntent(intent: Intent?) {
        val route = intent?.getStringExtra(BabyLogReminderScheduler.OPEN_ROUTE_EXTRA)
        if (!route.isNullOrBlank()) {
            pendingNavRoute = route
            pendingNavNonce = System.currentTimeMillis()
        }
    }

    private fun registerLaunchers() {
        audioPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                smartVoiceState = smartVoiceState.copy(message = "已授权麦克风，请再次按住说话")
            } else {
                smartVoiceState = smartVoiceState.copy(
                    isRecording = false,
                    isTranscribing = false,
                    message = "麦克风权限被拒绝；可在系统设置中重新授权，也可手动输入文本"
                )
                showToast("没有麦克风权限；可在系统设置中重新授权，也可手动输入")
            }
        }
        notificationPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                showToast("提醒通知已开启")
            } else {
                showToast("未开启通知权限；提醒仍会保留在提醒中心")
            }
            reloadData()
        }
        cameraPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                startCameraCapture(pendingImageTarget)
            } else {
                showToast("没有相机权限，无法拍照")
            }
        }
        cameraLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            val captured = pendingCameraFile
            val target = pendingImageTarget
            pendingCameraFile = null
            if (!success || captured == null) {
                captured?.delete()
                return@registerForActivityResult
            }
            runInBackground {
                try {
                    val compressedPath = service.compressImageFileToPrivateFile(captured, target.fileName)
                    captured.delete()
                    runOnUiThread { setPickedImage(target, compressedPath) }
                } catch (error: IOException) {
                    showInfo("保存照片失败", error.message ?: "无法保存照片")
                }
            }
        }
        pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri != null) {
                copyPickedImage(uri)
            }
        }
        exportBackupLauncher = registerForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
            if (uri != null) {
                writeBackup(uri)
            }
        }
        importBackupLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null) {
                contentResolver.takePersistableUriPermission(uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                readBackup(uri)
            }
        }
        visitSummarySaveLauncher = registerForActivityResult(ActivityResultContracts.CreateDocument("text/markdown")) { uri ->
            if (uri != null) {
                writeVisitSummary(uri)
            } else {
                pendingVisitSummaryText = null
            }
        }
    }

    internal fun reloadData() {
        runInBackground {
            service.purgeExpiredTrash()
            val timeline = service.listTimelineEvents()
            val childProfile = repository.loadChildProfile()
            try {
                reminderStore.syncSystemReminders(childProfile, timeline)
            } catch (ignored: JSONException) {
                // Reminder generation should not block the app shell.
            }
            BabyLogReminderScheduler.scheduleAll(this, reminderStore.listReminders(), childProfile)
            BabyLogReminderScheduler.enqueuePeriodicRefresh(this)
            val nextState = BabyLogUiState(
                dashboard = service.loadDashboard(),
                timeline = timeline,
                trashEvents = service.listTrashEvents(),
                attachments = service.listAttachmentsNewestFirst(),
                syncConfig = repository.loadSyncSettings(),
                childProfile = childProfile,
                preVisitQuestions = preVisitQuestionStore.listQuestions(),
                reminders = reminderStore.listReminders(),
                notificationPermissionGranted = BabyLogReminderScheduler.hasNotificationPermission(this),
                setupCompleted = repository.hasCompletedSetup(),
                disclaimerAccepted = disclaimerStore.hasAcceptedCurrentVersion(),
                hasImportUndoSnapshot = service.hasImportUndoSnapshot(),
                lastBackupExportMs = getSharedPreferences(META_PREFS_NAME, MODE_PRIVATE)
                    .getLong(LAST_BACKUP_EXPORT_MS, 0L)
            )
            runOnUiThread { uiState = nextState }
        }
    }

    private fun refreshSmartConfigSummary() {
        smartConfigSummary = if (smartConfigStore.isConfigured()) {
            "已配置；用于 B 超 OCR 和文本结构化"
        } else {
            "未配置；Key 仅保存在本机，不同步不备份"
        }
        speechConfigSummary = if (smartConfigStore.isSpeechConfigured()) {
            "已配置；用于按住说话转文字"
        } else {
            "未配置；语音会降级为手动输入"
        }
    }

    private fun acceptMedicalDisclaimer() {
        runInBackground {
            try {
                disclaimerStore.markCurrentVersionAccepted()
                runOnUiThread {
                    uiState = uiState.copy(disclaimerAccepted = true)
                    pendingNavRoute = BabyLogRoutes.Home
                }
            } catch (error: IOException) {
                runOnUiThread {
                    Toast.makeText(this, "保存确认状态失败：${error.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun openSmartSettings() {
        runInBackground {
            try {
                val config = smartConfigStore.load()
                runOnUiThread {
                    smartSettingsConfig = config
                    pendingNavRoute = BabyLogRoutes.SettingsModel
                }
            } catch (error: Exception) {
                showInfo("无法读取智能识别配置", error.message ?: "请稍后重试")
            }
        }
    }

    private fun saveSmartSettings(config: BabyLogSmartConfigStore.Config) {
        if (config.isEnabled() && !config.isConfigured()) {
            showInfo("配置不完整", "启用智能识别前，请填写 Base URL、模型和 API Key。")
            return
        }
        runInBackground {
            try {
                smartConfigStore.save(config)
                runOnUiThread {
                    refreshSmartConfigSummary()
                    smartSettingsConfig = null
                    pendingNavRoute = BabyLogRoutes.Settings
                }
                showToast(if (config.isConfigured()) "已保存智能识别配置" else "已关闭智能识别")
            } catch (error: Exception) {
                showInfo("保存失败", error.message ?: "无法保存智能识别配置")
            }
        }
    }

    private fun openSpeechSettings() {
        runInBackground {
            try {
                val config = smartConfigStore.loadSpeechConfig()
                runOnUiThread {
                    speechSettingsConfig = config
                    pendingNavRoute = BabyLogRoutes.SettingsSpeech
                }
            } catch (error: Exception) {
                showInfo("无法读取语音配置", error.message ?: "请稍后重试")
            }
        }
    }

    private fun saveSpeechSettings(config: BabyLogSmartConfigStore.SpeechConfig) {
        if (config.isEnabled() && !config.isConfigured()) {
            showInfo("配置不完整", "启用语音识别前，请填写 Paraformer 模型和 API Key。")
            return
        }
        runInBackground {
            try {
                smartConfigStore.saveSpeechConfig(config)
                runOnUiThread {
                    refreshSmartConfigSummary()
                    speechSettingsConfig = null
                    pendingNavRoute = BabyLogRoutes.Settings
                }
                showToast(if (config.isConfigured()) "已保存语音识别配置" else "已关闭语音识别")
            } catch (error: Exception) {
                showInfo("保存失败", error.message ?: "无法保存语音配置")
            }
        }
    }

    private fun openUltrasoundForm(
        draft: SmartEntryDraft? = null,
        navigate: Boolean = true
    ): String {
        editingEvent = null
        ultrasoundDraft = draft
        pendingUltrasoundPhotoPath = null
        pendingUltrasoundPhotoName = null
        ultrasoundOcrCandidate = null
        ultrasoundOcrRunning = false
        if (navigate) {
            pendingNavRoute = BabyLogRoutes.RecordUltrasound
        }
        return BabyLogRoutes.RecordUltrasound
    }

    private fun openEventDetail(event: BabyLogDomain.BabyLogEvent, sourceRoute: String) {
        recordDetailEventId = event.id
        recordDetailReturnRoute = if (BabyLogRoutes.isTopLevel(sourceRoute)) sourceRoute else BabyLogRoutes.Timeline
        pendingNavRoute = BabyLogRoutes.RecordDetail
        pendingNavNonce = System.nanoTime()
    }

    private fun openEventForEdit(event: BabyLogDomain.BabyLogEvent, sourceRoute: String) {
        if (!isEditablePregnancyRecord(event.eventType)) {
            showInfo("暂不可编辑", "这类记录目前需删除后重录。")
            return
        }
        editingEvent = event
        recordReturnRoute = if (BabyLogRoutes.isTopLevel(sourceRoute)) sourceRoute else BabyLogRoutes.Timeline
        when (event.eventType) {
            "ultrasound" -> {
                ultrasoundDraft = draftFromUltrasoundEvent(event)
                pendingUltrasoundPhotoPath = null
                pendingUltrasoundPhotoName = null
                ultrasoundOcrCandidate = null
                ultrasoundOcrRunning = false
                pendingNavRoute = BabyLogRoutes.RecordUltrasound
            }
            "pregnancy_checkup" -> {
                pregnancyAction = editQuickActionByEventType(event.eventType)
                pregnancyDraft = draftFromPregnancyEvent(event)
                pendingCheckupAttachmentPath = null
                pendingCheckupAttachmentName = null
                checkupOcrCandidate = null
                checkupOcrRunning = false
                pendingNavRoute = BabyLogRoutes.RecordPregnancyEvent
            }
            "screening_nt", "screening_serum", "screening_nipt", "screening_anomaly", "screening_ogtt", "screening_gbs", "screening_nst" -> {
                pregnancyAction = editQuickActionByEventType(event.eventType)
                pregnancyDraft = draftFromPregnancyEvent(event)
                pendingCheckupAttachmentPath = null
                pendingCheckupAttachmentName = null
                checkupOcrCandidate = null
                checkupOcrRunning = false
                pendingNavRoute = BabyLogRoutes.RecordPregnancyEvent
            }
            "maternal_metric" -> {
                maternalMetricDraft = draftFromMaternalMetricEvent(event)
                pendingNavRoute = BabyLogRoutes.RecordMaternalMetric
            }
            "fetal_movement", "contraction" -> {
                pregnancyAction = editQuickActionByEventType(event.eventType)
                pregnancyDraft = draftFromPregnancyEvent(event)
                pendingCheckupAttachmentPath = null
                pendingCheckupAttachmentName = null
                checkupOcrCandidate = null
                checkupOcrRunning = false
                pendingNavRoute = BabyLogRoutes.RecordPregnancyEvent
            }
            else -> {
                if (isEditableBabyRecord(event.eventType)) {
                    babyCareAction = editQuickActionByEventType(event.eventType)
                    babyCareDraft = draftFromBabyCareEvent(event)
                    pendingNavRoute = BabyLogRoutes.RecordBabyCare
                }
            }
        }
    }

    private fun recordQuickAction(action: BabyLogService.QuickAction) {
        runInBackground {
            try {
                val event = service.recordQuickEvent(action)
                runOnUiThread {
                    quickUndoRequest = QuickUndoRequest(event.id, action.label, System.nanoTime())
                }
                reloadData()
            } catch (error: Exception) {
                showBabyLogError("保存失败", "无法保存记录", error)
            }
        }
    }

    private fun consumeQuickUndoRequest(nonce: Long) {
        val current = quickUndoRequest
        if (current != null && current.nonce == nonce) {
            quickUndoRequest = null
        }
    }

    private fun undoQuickEvent(eventId: String) {
        runInBackground {
            try {
                service.deleteEvent(eventId)
                showToast("已撤销记录")
                reloadData()
            } catch (error: Exception) {
                showBabyLogError("撤销失败", "无法撤销这条记录", error)
            }
        }
    }

    private fun handleQuickAction(action: BabyLogService.QuickAction): String? {
        editingEvent = null
        var recordRoute: String? = null
        if (isBabyCareAction(action.eventType)) {
            babyCareDraft = null
            babyCareAction = action
            recordRoute = BabyLogRoutes.RecordBabyCare
        } else if (action.eventType == "fetal_movement") {
            showFetalMovementSession = true
        } else if (action.eventType == "contraction") {
            recordRoute = BabyLogRoutes.RecordContractionSession
        } else if (isPregnancyFormAction(action.eventType)) {
            pregnancyDraft = null
            pregnancyAction = action
            pendingCheckupAttachmentPath = null
            pendingCheckupAttachmentName = null
            checkupOcrCandidate = null
            checkupOcrRunning = false
            recordRoute = BabyLogRoutes.RecordPregnancyEvent
        } else if (action.eventType == "maternal_metric") {
            maternalMetricDraft = null
            recordRoute = BabyLogRoutes.RecordMaternalMetric
        } else if (action.eventType == "ultrasound") {
            recordRoute = openUltrasoundForm(navigate = false)
        } else {
            recordQuickAction(action)
        }
        return recordRoute
    }

    private fun recordBabyCare(input: BabyLogService.BabyCareInput) {
        runInBackground {
            try {
                val editing = editingEvent?.takeIf { it.eventType == input.eventType }
                val event = if (editing != null) {
                    service.updateBabyCareEvent(editing.id, input)
                } else {
                    service.recordBabyCareEvent(input, selectedBabyDay)
                }
                runOnUiThread {
                    editingEvent = null
                    babyCareAction = null
                    babyCareDraft = null
                    markRecordSaved(event)
                }
                showToast(if (editing != null) "已更新记录" else "已保存记录")
                reloadData()
            } catch (error: Exception) {
                showBabyLogError("保存失败", "无法保存记录", error)
            }
        }
    }

    private fun recordPregnancy(input: BabyLogService.PregnancyInput) {
        runInBackground {
            try {
                val editing = editingEvent?.takeIf { it.eventType == input.eventType }
                val event = if (editing != null) {
                    service.updatePregnancyEvent(editing.id, input)
                } else {
                    service.recordPregnancyEvent(input)
                }
                runOnUiThread {
                    editingEvent = null
                    pregnancyAction = null
                    pregnancyDraft = null
                    pendingCheckupAttachmentPath = null
                    pendingCheckupAttachmentName = null
                    checkupOcrCandidate = null
                    checkupOcrRunning = false
                    markRecordSaved(event)
                }
                showToast(if (editing != null) "已更新记录" else "已保存记录")
                reloadData()
            } catch (error: Exception) {
                showBabyLogError("保存失败", "无法保存记录", error)
            }
        }
    }

    private fun recordFetalMovementSession(input: BabyLogService.FetalMovementSessionInput) {
        runInBackground {
            try {
                service.recordFetalMovementSession(input)
                showToast("已保存胎动计数")
                reloadData()
            } catch (error: Exception) {
                showBabyLogError("保存失败", "无法保存胎动计数", error)
            }
        }
    }

    private fun recordContractionSession(input: BabyLogService.ContractionSessionInput) {
        runInBackground {
            try {
                val events = service.recordContractionSession(input)
                val lastEvent = events.lastOrNull()
                runOnUiThread {
                    if (lastEvent != null) {
                        markRecordSaved(lastEvent)
                    } else {
                        pendingNavRoute = BabyLogRoutes.Home
                    }
                }
                showToast("已保存宫缩会话：${events.size} 次")
                reloadData()
            } catch (error: Exception) {
                showBabyLogError("保存失败", "无法保存宫缩会话", error)
            }
        }
    }

    private fun recordMaternalMetric(input: BabyLogService.MaternalMetricInput) {
        runInBackground {
            try {
                val editing = editingEvent?.takeIf { it.eventType == "maternal_metric" }
                val event = if (editing != null) {
                    service.updateMaternalMetric(editing.id, input)
                } else {
                    service.recordMaternalMetric(input)
                }
                runOnUiThread {
                    editingEvent = null
                    maternalMetricDraft = null
                    markRecordSaved(event)
                }
                showToast(if (editing != null) "已更新孕妈指标" else "已保存孕妈指标")
                reloadData()
            } catch (error: Exception) {
                showBabyLogError("保存失败", "无法保存孕妈指标", error)
            }
        }
    }

    private fun recordUltrasound(input: BabyLogService.UltrasoundInput) {
        runInBackground {
            try {
                val editing = editingEvent?.takeIf { it.eventType == "ultrasound" }
                val event = if (editing != null) {
                    service.updateUltrasound(editing.id, input)
                } else {
                    service.recordUltrasound(input)
                }
                runOnUiThread {
                    editingEvent = null
                    ultrasoundDraft = null
                    pendingUltrasoundPhotoPath = null
                    pendingUltrasoundPhotoName = null
                    ultrasoundOcrCandidate = null
                    markRecordSaved(event)
                }
                showToast(if (editing != null) "已更新 B 超记录" else "已保存 B 超记录")
                reloadData()
            } catch (error: Exception) {
                showBabyLogError("保存失败", "无法保存 B 超记录", error)
            }
        }
    }

    private fun markRecordSaved(event: BabyLogDomain.BabyLogEvent) {
        highlightedEventId = event.id
        pendingNavRoute = if (BabyLogRoutes.isTopLevel(recordReturnRoute)) recordReturnRoute else BabyLogRoutes.Home
        pendingNavNonce = System.nanoTime()
        recordReturnRoute = BabyLogRoutes.Home
    }

    private fun deleteEvent(eventId: String) {
        runInBackground {
            try {
                service.deleteEvent(eventId)
                runOnUiThread {
                    if (recordDetailEventId == eventId) {
                        recordDetailEventId = null
                        pendingNavRoute = recordDetailReturnRoute
                        pendingNavNonce = System.nanoTime()
                    }
                }
                showToast("已移入回收站，7 天内可恢复")
                reloadData()
            } catch (error: Exception) {
                showBabyLogError("移入回收站失败", "无法处理这条记录", error)
            }
        }
    }

    private fun restoreEvent(eventId: String) {
        runInBackground {
            try {
                service.restoreEvent(eventId)
                showToast("已恢复记录")
                reloadData()
            } catch (error: Exception) {
                showBabyLogError("恢复失败", "无法恢复这条记录", error)
            }
        }
    }

    private fun showBabyLogError(defaultTitle: String, fallbackMessage: String, error: Exception) {
        val title = when (error) {
            is BabyLogException.ValidationException -> {
                if (defaultTitle == "导入失败") "备份格式无效" else "内容需要补充"
            }
            is BabyLogException.NotFoundException -> {
                if (defaultTitle == "撤销失败") "没有可撤销数据" else "记录不可用"
            }
            is BabyLogException.StorageException -> defaultTitle
            else -> defaultTitle
        }
        showInfo(title, error.message ?: fallbackMessage)
    }

    private fun pickImage(target: ImagePickTarget) {
        pendingImageTarget = target
        pickImageLauncher.launch("image/*")
    }

    private fun requestCameraOrLaunch(target: ImagePickTarget) {
        pendingImageTarget = target
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startCameraCapture(target)
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun startCameraCapture(target: ImagePickTarget = pendingImageTarget) {
        try {
            pendingImageTarget = target
            val file = service.createCameraCaptureFile(target.fileName)
            pendingCameraFile = file
            val uri = BabyLogFileProvider.getUriForFile(this, file)
            cameraLauncher.launch(uri)
        } catch (error: IOException) {
            showInfo("无法打开相机", error.message ?: "无法创建拍照文件")
        }
    }

    private fun copyPickedImage(uri: Uri) {
        val target = pendingImageTarget
        runInBackground {
            try {
                val path = service.copyImageUriToPrivateFile(uri, target.fileName)
                runOnUiThread { setPickedImage(target, path) }
            } catch (error: IOException) {
                showInfo("导入图片失败", error.message ?: "无法读取图片")
            }
        }
    }

    private fun setPickedImage(target: ImagePickTarget, path: String) {
        when (target) {
            ImagePickTarget.Ultrasound -> {
                pendingUltrasoundPhotoPath = path
                pendingUltrasoundPhotoName = File(path).name
                ultrasoundOcrCandidate = null
            }
            ImagePickTarget.Checkup -> {
                pendingCheckupAttachmentPath = path
                pendingCheckupAttachmentName = File(path).name
                checkupOcrCandidate = null
            }
        }
    }

    private fun recognizeUltrasoundPhoto() {
        val path = pendingUltrasoundPhotoPath
        if (path.isNullOrBlank()) {
            showInfo("先选择图片", "拍照或选择 B 超单后再识别。")
            return
        }
        if (ultrasoundOcrRunning) {
            return
        }
        ultrasoundOcrRunning = true
        ultrasoundOcrCandidate = null
        runInBackground {
            try {
                val config = smartConfigStore.load()
                if (!config.isConfigured()) {
                    showInfo("智能识别未配置", "请先在设置中填写模型地址、名称和 API Key。")
                    return@runInBackground
                }
                val candidate = smartVisionClient.recognizeUltrasoundImage(File(path), config)
                runOnUiThread {
                    ultrasoundOcrCandidate = candidate
                    showToast("识别完成，请核对候选字段")
                }
            } catch (error: Exception) {
                showInfo("识别失败", smartVisionErrorMessage(error))
            } finally {
                runOnUiThread { ultrasoundOcrRunning = false }
            }
        }
    }

    private fun requestSmartEntry(rawText: String) {
        if (rawText.isBlank()) {
            showInfo("先输入内容", "输入文字或按住说话后再生成候选。")
            return
        }
        if (smartEntryRunning) {
            return
        }
        val stage = currentCareStage(uiState.childProfile)
        val forms = smartEntryForms(stage)
        if (forms.isEmpty()) {
            showInfo("暂无可用表单", "请先从快捷记录手动选择。")
            return
        }
        smartEntryRunning = true
        smartEntryCandidate = null
        runInBackground {
            try {
                val config = smartConfigStore.load()
                if (!config.isConfigured()) {
                    showInfo("智能识别未配置", "请先在设置中填写模型地址、名称和 API Key。")
                    return@runInBackground
                }
                val candidate = smartTextClient.classifyEntry(
                    stage,
                    forms,
                    rawText,
                    config
                )
                runOnUiThread { smartEntryCandidate = candidate }
            } catch (error: Exception) {
                showInfo("智能录入失败", smartVisionErrorMessage(error))
            } finally {
                runOnUiThread { smartEntryRunning = false }
            }
        }
    }

    private fun startSmartVoiceRecording() {
        if (smartEntryRunning || smartVoiceState.isTranscribing || smartVoiceState.isRecording) {
            return
        }
        if (!smartConfigStore.isSpeechConfigured()) {
            smartVoiceState = smartVoiceState.copy(message = "语音转文字未配置，可直接手动输入")
            return
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            smartVoiceState = smartVoiceState.copy(message = "需要麦克风权限")
            return
        }
        try {
            val recorder = BabyLogPcmVoiceRecorder()
            recorder.start(cacheDir)
            voiceRecorder = recorder
            smartVoiceState = SmartVoiceUiState(isRecording = true, message = "正在录音，松开后转文字")
        } catch (error: Exception) {
            voiceRecorder = null
            smartVoiceState = SmartVoiceUiState(message = error.message ?: "无法开始录音")
        }
    }

    private fun startNavVoiceRecording() {
        startSmartVoiceRecording()
        if (!smartVoiceState.isRecording) {
            pendingNavRoute = BabyLogRoutes.SmartEntry
        }
    }

    private fun finishNavVoiceRecording() {
        pendingNavRoute = BabyLogRoutes.SmartEntry
        finishSmartVoiceRecording()
    }

    private fun startLongTextVoiceRecording(onTranscript: (String) -> Unit) {
        longTextVoiceApply = onTranscript
        startSmartVoiceRecording()
        if (!smartVoiceState.isRecording) {
            longTextVoiceApply = null
        }
    }

    private fun finishLongTextVoiceRecording() {
        finishSmartVoiceRecording()
    }

    private fun finishSmartVoiceRecording() {
        val recorder = voiceRecorder ?: return
        voiceRecorder = null
        val fieldApply = longTextVoiceApply
        longTextVoiceApply = null
        val audioFile = try {
            recorder.stop()
        } catch (error: Exception) {
            longTextVoiceApply = null
            smartVoiceState = SmartVoiceUiState(message = error.message ?: "录音失败")
            return
        }
        smartVoiceState = SmartVoiceUiState(isTranscribing = true, message = "正在转文字...")
        runInBackground {
            try {
                val config = smartConfigStore.loadSpeechConfig()
                if (!config.isConfigured()) {
                    runOnUiThread {
                        smartVoiceState = SmartVoiceUiState(message = "语音转文字未配置，可直接手动输入")
                    }
                    return@runInBackground
                }
                val result = speechClient.transcribePcm(audioFile, config)
                runOnUiThread {
                    if (fieldApply != null) {
                        val text = result.text.trim()
                        if (text.isNotBlank()) {
                            fieldApply(text)
                            smartVoiceState = SmartVoiceUiState(message = "已填入，请核对后保存")
                        } else {
                            smartVoiceState = SmartVoiceUiState(message = "未识别到文字，可手动输入")
                        }
                    } else {
                        smartVoiceState = SmartVoiceUiState(
                            transcript = result.text,
                            transcriptNonce = System.nanoTime(),
                            message = "已转成文字，请核对后生成候选"
                        )
                    }
                }
            } catch (error: Exception) {
                runOnUiThread {
                    smartVoiceState = SmartVoiceUiState(
                        message = smartSpeechErrorMessage(error)
                    )
                }
            } finally {
                audioFile.delete()
            }
        }
    }

    private fun cancelSmartVoiceRecording() {
        voiceRecorder?.cancel()
        voiceRecorder = null
        longTextVoiceApply = null
        if (smartVoiceState.isRecording) {
            smartVoiceState = SmartVoiceUiState()
        }
    }

    private fun openSmartEntryCandidate(candidate: BabyLogSmartTextClient.SmartEntryCandidate) {
        val values = candidate.values.toMap()
        if (candidate.eventType.isBlank()) {
            showInfo("无法判断记录类型", candidate.warnings.joinToString("\n").ifBlank { "请补充具体内容，或从快捷记录手动选择。" })
            return
        }
        val draft = SmartEntryDraft(values = values)
        when (candidate.eventType) {
            "ultrasound" -> {
                openUltrasoundForm(draft)
            }
            "pregnancy_checkup", "contraction", "screening_nt", "screening_serum", "screening_nipt", "screening_anomaly", "screening_ogtt", "screening_gbs", "screening_nst" -> {
                val action = editQuickActionByEventType(candidate.eventType)
                if (action == null) {
                    showInfo("无法打开表单", "当前阶段没有对应表单。")
                    return
                }
                pregnancyDraft = draft
                pregnancyAction = action
                pendingCheckupAttachmentPath = null
                pendingCheckupAttachmentName = null
                checkupOcrCandidate = null
                checkupOcrRunning = false
                pendingNavRoute = BabyLogRoutes.RecordPregnancyEvent
            }
            "fetal_movement" -> {
                val action = editQuickActionByEventType(candidate.eventType)
                if (action == null) {
                    showInfo("无法打开表单", "当前阶段没有对应表单。")
                    return
                }
                pregnancyDraft = draft
                pregnancyAction = action
                pendingCheckupAttachmentPath = null
                pendingCheckupAttachmentName = null
                checkupOcrCandidate = null
                checkupOcrRunning = false
                pendingNavRoute = BabyLogRoutes.RecordPregnancyEvent
            }
            "maternal_metric" -> {
                maternalMetricDraft = draft
                pendingNavRoute = BabyLogRoutes.RecordMaternalMetric
            }
            "feed", "sleep", "diaper", "temperature", "medication", "breastfeed", "bottle", "wake", "pee", "poop" -> {
                val action = quickActionByEventType(candidate.eventType)
                if (action == null) {
                    showInfo("无法打开表单", "当前阶段没有对应表单。")
                    return
                }
                babyCareDraft = draft
                babyCareAction = action
                pendingNavRoute = BabyLogRoutes.RecordBabyCare
            }
            else -> {
                showInfo("无法打开表单", "已识别类型，但没有对应确认表单。")
                return
            }
        }
        smartEntryCandidate = null
        val warning = candidate.warnings.firstOrNull()
        showToast(if (warning.isNullOrBlank()) "已打开候选表单，请核对后保存" else "已打开候选表单：$warning")
    }

    private fun smartVisionErrorMessage(error: Exception): String {
        val message = error.message?.takeIf { it.isNotBlank() } ?: "模型未返回可用字段"
        return when {
            error is java.net.SocketTimeoutException || message.contains("timed out", ignoreCase = true) ->
                "模型响应超时，请稍后重试。"
            message.contains(" 401") || message.contains(" 403") || message.contains("unauthorized", ignoreCase = true) ->
                "模型认证失败。请检查 API Key、Base URL 和模型名称。"
            message.contains(" 413") || message.contains("too large", ignoreCase = true) ->
                "图片过大，请裁剪报告主体后重试。"
            message.contains("无法解码图片") ->
                "无法读取这张图片。请重新拍照或从相册选择清晰的 B 超单。"
            else -> message
        }
    }

    private fun smartSpeechErrorMessage(error: Exception): String {
        val message = error.message?.takeIf { it.isNotBlank() } ?: "语音识别未返回文字"
        return when {
            error is java.net.SocketTimeoutException || message.contains("timed out", ignoreCase = true) ->
                "语音识别超时，可稍后重试或直接手动输入"
            message.contains("401") || message.contains("403") || message.contains("unauthorized", ignoreCase = true) ->
                "语音识别认证失败。请确认语音转文字配置里使用的是可调用 DashScope Paraformer 的 API Key"
            message.contains("network", ignoreCase = true) || message.contains("Unable to resolve", ignoreCase = true) ->
                "当前网络不可用，可直接手动输入"
            else -> message
        }
    }

    private fun copyVisitSummary(text: String) {
        if (text.isBlank()) {
            showToast("没有可复制的汇总内容")
            return
        }
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("栗记复诊汇总", text))
        showToast("已复制复诊汇总")
    }

    private fun shareVisitSummary(text: String) {
        if (text.isBlank()) {
            showToast("没有可分享的汇总内容")
            return
        }
        val send = Intent(Intent.ACTION_SEND)
            .setType("text/markdown")
            .putExtra(Intent.EXTRA_SUBJECT, "栗记复诊汇总")
            .putExtra(Intent.EXTRA_TEXT, text)
        startActivity(Intent.createChooser(send, "分享复诊汇总"))
    }

    private fun saveVisitSummary(text: String) {
        if (text.isBlank()) {
            showToast("没有可保存的汇总内容")
            return
        }
        pendingVisitSummaryText = text
        val date = SimpleDateFormat("yyyyMMdd-HHmm", Locale.US).format(Date())
        visitSummarySaveLauncher.launch("babylog-visit-summary-$date.md")
    }

    private fun writeVisitSummary(uri: Uri) {
        val text = pendingVisitSummaryText
        pendingVisitSummaryText = null
        if (text.isNullOrBlank()) {
            showToast("没有可保存的汇总内容")
            return
        }
        runInBackground {
            try {
                contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { it.write(text) }
                    ?: throw IOException("无法打开导出文件")
                showToast("复诊汇总已保存")
            } catch (error: Exception) {
                showInfo("保存失败", error.message ?: "无法保存复诊汇总")
            }
        }
    }

    private fun polishVisitSummary(text: String, onDone: (String?) -> Unit) {
        if (text.isBlank()) {
            showInfo("没有可润色内容", "请先生成或填写复诊汇总文本。")
            onDone(null)
            return
        }
        runInBackground {
            try {
                val config = smartConfigStore.load()
                if (!config.isConfigured()) {
                    runOnUiThread {
                        showInfo("智能识别未配置", "请先在设置中填写模型地址、名称和 API Key。未配置时仍可导出原始模板。")
                        onDone(null)
                    }
                    return@runInBackground
                }
                val polished = smartTextClient.polishVisitSummary(text, config)
                runOnUiThread {
                    onDone(polished)
                    showToast("润色完成，请核对后导出")
                }
            } catch (error: Exception) {
                runOnUiThread {
                    showInfo("润色失败", smartVisionErrorMessage(error))
                    onDone(null)
                }
            }
        }
    }

    private fun exportBackup() {
        val date = SimpleDateFormat("yyyyMMdd-HHmm", Locale.US).format(Date())
        exportBackupLauncher.launch("babylog-backup-$date.json")
    }

    private fun writeBackup(uri: Uri) {
        runInBackground {
            try {
                val raw = service.createBackupJson()
                contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { it.write(raw) }
                    ?: throw IOException("无法打开导出文件")
                getSharedPreferences(META_PREFS_NAME, MODE_PRIVATE)
                    .edit()
                    .putLong(LAST_BACKUP_EXPORT_MS, System.currentTimeMillis())
                    .apply()
                showToast("备份已导出")
                reloadData()
            } catch (error: Exception) {
                showBabyLogError("导出失败", "无法导出备份", error)
            }
        }
    }

    private fun importBackup() {
        importBackupLauncher.launch(arrayOf("application/json", "text/*", "*/*"))
    }

    private fun readBackup(uri: Uri) {
        runInBackground {
            try {
                val raw = contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                    ?: throw IOException("无法打开备份文件")
                val preview = previewBackup(raw)
                runOnUiThread {
                    importConfirm = ImportConfirmState(raw, preview.eventCount, preview.profileLabel)
                }
            } catch (error: Exception) {
                showBabyLogError("导入失败", "无法导入备份", error)
            }
        }
    }

    private fun importBackupNow(raw: String) {
        runInBackground {
            try {
                val count = service.importBackupJson(raw)
                runOnUiThread { pendingNavRoute = BabyLogRoutes.Home }
                showToast("导入完成：$count 条记录")
                reloadData()
            } catch (error: Exception) {
                showBabyLogError("导入失败", "无法导入备份", error)
            }
        }
    }

    private fun undoLastImport() {
        runInBackground {
            try {
                val count = service.undoLastImport()
                runOnUiThread { pendingNavRoute = BabyLogRoutes.Home }
                showToast("已撤销导入，恢复 $count 条记录")
                reloadData()
            } catch (error: Exception) {
                showBabyLogError("撤销失败", "没有可恢复的导入快照", error)
                reloadData()
            }
        }
    }

    private fun startForegroundPullLoop() {
        syncPullHandler.removeCallbacks(foregroundPullRunnable)
        if (shouldAutoPullSync()) {
            pullSyncNow(silent = true)
            syncPullHandler.postDelayed(foregroundPullRunnable, 120_000L)
        }
    }

    private fun saveSyncSettings(backendBaseUrl: String, familyKey: String) {
        val normalized = BabyLogFormatters.normalizeBackendBaseUrl(backendBaseUrl)
        if (normalized.isNotEmpty()) {
            if (!syncFamilyKeyConfigured && !BabyLogSyncSecretStore.hasUsableFamilyKey(familyKey)) {
                showInfo("配置不完整", "启用家庭同步前，请填写家庭密钥。密钥只保存在本机加密存储中。")
                return
            }
            syncConfirmState = SyncConfirmState(normalized, familyKey)
            return
        }
        persistSyncSettings(normalized, "")
    }

    private fun checkSyncConnection(backendBaseUrl: String, familyKey: String) {
        if (syncCheckRunning) {
            return
        }
        val normalized = BabyLogFormatters.normalizeBackendBaseUrl(backendBaseUrl)
        if (normalized.isEmpty()) {
            syncCheckOk = false
            syncCheckMessage = "请先填写服务器地址"
            return
        }
        syncCheckRunning = true
        syncCheckOk = null
        syncCheckMessage = "正在检测服务器和家庭密钥..."
        runInBackground {
            try {
                val key = if (BabyLogSyncSecretStore.hasUsableFamilyKey(familyKey)) {
                    familyKey
                } else {
                    syncSecretStore.loadFamilyKey()
                }
                val result = remoteSyncClient.checkConnection(normalized, key)
                runOnUiThread {
                    syncCheckRunning = false
                    syncCheckOk = result.ok
                    syncCheckMessage = result.message
                }
            } catch (error: Exception) {
                runOnUiThread {
                    syncCheckRunning = false
                    syncCheckOk = false
                    syncCheckMessage = "连接检测失败：${error.message ?: "网络不可用"}"
                }
            }
        }
    }

    private fun persistSyncSettings(backendBaseUrl: String, familyKey: String) {
        runInBackground {
            try {
                repository.saveSyncSettings(BabyLogDomain.BackendConfig(backendBaseUrl.isNotEmpty(), backendBaseUrl, "cn", null))
                if (backendBaseUrl.isEmpty()) {
                    syncSecretStore.clearFamilyKey()
                } else if (BabyLogSyncSecretStore.hasUsableFamilyKey(familyKey)) {
                    syncSecretStore.saveFamilyKey(familyKey)
                }
                BabyLogSyncPullWorker.ensurePeriodicWork(this)
                showToast(if (backendBaseUrl.isEmpty()) "已关闭后端同步" else "已保存同步地址")
                runOnUiThread {
                    syncFamilyKeyConfigured = syncSecretStore.hasFamilyKey()
                    pendingNavRoute = BabyLogRoutes.Settings
                }
                reloadData()
            } catch (error: Exception) {
                showInfo("保存失败", error.message ?: "无法保存同步设置")
            }
        }
    }

    private fun clearLocalData() {
        runInBackground {
            service.clearLocalData()
            try {
                syncSecretStore.clearFamilyKey()
            } catch (ignored: IOException) {
                // Clearing local app data should still continue if the encrypted key was already absent.
            }
            runOnUiThread {
                syncFamilyKeyConfigured = false
                pendingNavRoute = BabyLogRoutes.Home
            }
            showToast("本机数据已清空")
            reloadData()
        }
    }

    private fun quickActions(): List<BabyLogService.QuickAction> {
        if (!uiState.setupCompleted) return emptyList()
        return quickActionsForStage(currentCareStage(uiState.childProfile))
    }

    private fun quickActionByEventType(eventType: String): BabyLogService.QuickAction? {
        return quickActions().firstOrNull { it.eventType == eventType }
    }

    private fun recognizeCheckupAttachment() {
        val path = pendingCheckupAttachmentPath
        if (path.isNullOrBlank()) {
            showInfo("先选择图片", "拍照或选择产检报告后再识别。")
            return
        }
        if (checkupOcrRunning) {
            return
        }
        checkupOcrRunning = true
        checkupOcrCandidate = null
        runInBackground {
            try {
                val config = smartConfigStore.load()
                if (!config.isConfigured()) {
                    showInfo("智能识别未配置", "请先在设置中填写模型地址、名称和 API Key。")
                    return@runInBackground
                }
                val candidate = smartVisionClient.recognizeCheckupImage(File(path), config)
                runOnUiThread {
                    checkupOcrCandidate = candidate
                    showToast("识别完成，请核对候选字段")
                }
            } catch (error: Exception) {
                showInfo("识别失败", smartVisionErrorMessage(error))
            } finally {
                runOnUiThread { checkupOcrRunning = false }
            }
        }
    }

    private fun editQuickActionByEventType(eventType: String): BabyLogService.QuickAction? {
        return quickActionByEventType(eventType) ?: when (eventType) {
            "pregnancy_checkup" -> BabyLogService.QuickAction("产检", "常规指标 / 结论 / 附件", ChestnutPalette.VioletArgb, "pregnancy_checkup")
            "screening_nt" -> BabyLogService.QuickAction("NT", "NT 值 / 结论 / 附件", ChestnutPalette.VioletArgb, "screening_nt")
            "screening_serum" -> BabyLogService.QuickAction("唐筛", "风险值 / 分级 / 附件", ChestnutPalette.VioletArgb, "screening_serum")
            "screening_nipt" -> BabyLogService.QuickAction("无创", "T21/T18/T13 / 结论", ChestnutPalette.VioletArgb, "screening_nipt")
            "screening_anomaly" -> BabyLogService.QuickAction("大排畸", "结构结论 / 附件", ChestnutPalette.VioletArgb, "screening_anomaly")
            "screening_ogtt" -> BabyLogService.QuickAction("糖耐", "空腹 / 1h / 2h", ChestnutPalette.VioletArgb, "screening_ogtt")
            "screening_gbs" -> BabyLogService.QuickAction("GBS", "阴性 / 阳性 / 备注", ChestnutPalette.VioletArgb, "screening_gbs")
            "screening_nst" -> BabyLogService.QuickAction("胎监", "反应型 / 备注", ChestnutPalette.VioletArgb, "screening_nst")
            "fetal_movement" -> BabyLogService.QuickAction("胎动", "时段 / 次数 / 备注", ChestnutPalette.GreenArgb, "fetal_movement")
            "contraction" -> BabyLogService.QuickAction("宫缩", "开始 / 间隔 / 持续", ChestnutPalette.PeachArgb, "contraction")
            "breastfeed" -> BabyLogService.QuickAction("母乳", "补充时长 / 侧别 / 备注", ChestnutPalette.PeachArgb, "breastfeed")
            "bottle" -> BabyLogService.QuickAction("奶瓶", "补充奶量 / 备注", ChestnutPalette.BlueArgb, "bottle")
            "feed" -> BabyLogService.QuickAction("喂养", "方式 / 奶量 / 备注", ChestnutPalette.BlueArgb, "feed")
            "sleep" -> BabyLogService.QuickAction("睡眠", "开始 / 结束 / 地点", ChestnutPalette.VioletArgb, "sleep")
            "diaper" -> BabyLogService.QuickAction("尿布", "类型 / 性状 / 备注", ChestnutPalette.YellowArgb, "diaper")
            "temperature" -> BabyLogService.QuickAction("体温", "温度 / 测量方式", ChestnutPalette.GreenArgb, "temperature")
            "medication" -> BabyLogService.QuickAction("用药", "药名 / 剂量 / 原因", ChestnutPalette.PeachArgb, "medication")
            "wake" -> BabyLogService.QuickAction("起床", "补充状态 / 备注", ChestnutPalette.GreenArgb, "wake")
            "pee" -> BabyLogService.QuickAction("尿尿", "补充尿布情况 / 备注", ChestnutPalette.YellowArgb, "pee")
            "poop" -> BabyLogService.QuickAction("便便", "补充性状 / 备注", ChestnutPalette.PeachArgb, "poop")
            else -> null
        }
    }

    private fun smartEntryForms(stage: String): Map<String, Map<String, String>> {
        val forms = linkedMapOf<String, Map<String, String>>()
        if (stage == BabyLogDomain.STAGE_PREGNANCY) {
            forms["ultrasound"] = smartFormFields(
                "examDate" to "检查日期 yyyy-MM-dd",
                "gestationalAge" to "孕周，例如 20+3；只在原文明确写出时填写",
                "hospital" to "医院 / 机构",
                "reportTime" to "报告时间",
                "diagnosisText" to "超声诊断 / 提示",
                "bpdMm" to "BPD 双顶径 mm",
                "hcMm" to "HC 头围 mm",
                "acMm" to "AC 腹围 mm",
                "flMm" to "FL 股骨长 mm",
                "efwGram" to "EFW 估重 g",
                "afiCm" to "AFI 羊水指数 cm",
                "deepestPocketCm" to "最大羊水池 cm",
                "placentaLocation" to "胎盘位置",
                "placentaGrade" to "胎盘成熟度",
                "fetalPresentation" to "胎位",
                "fetalHeartRateBpm" to "胎心率 bpm",
                "fetalCount" to "胎儿个数",
                "fetalMovement" to "胎动",
                "umbilicalInsertion" to "脐带插入处",
                "cervicalLengthMm" to "宫颈管长度 mm",
                "crlMm" to "CRL 顶臀径 mm",
                "ntMm" to "NT mm",
                "umbilicalSd" to "脐动脉 S/D",
                "umbilicalPi" to "脐动脉 PI",
                "umbilicalRi" to "脐动脉 RI"
            )
            forms["pregnancy_checkup"] = smartFormFields(
                "primary" to "检查日期 yyyy-MM-dd",
                "gestationalAge" to "孕周，例如 22+5；可从检查日期和预产期推算，也可按报告原文填写",
                "secondary" to "医院 / 机构",
                "department" to "科室",
                "systolicBp" to "收缩压 mmHg",
                "diastolicBp" to "舒张压 mmHg",
                "weightKg" to "体重 kg",
                "fundalHeightCm" to "宫高 cm",
                "abdominalCircumferenceCm" to "腹围 cm",
                "fetalHeartRateBpm" to "胎心率 bpm",
                "fetalPresentation" to "胎位：头位 / 臀位 / 横位 / 未记录",
                "edema" to "水肿：无 / 轻 / 中 / 重 / 未记录",
                "urineRoutine" to "尿常规摘要，如 正常 / 见报告",
                "urineProtein" to "尿蛋白：阴性 / ± / + / ++ / +++ / 见报告",
                "hemoglobinGL" to "血红蛋白 Hb g/L",
                "highRiskFactors" to "高危因素 / 特殊情况",
                "tertiary" to "医生结论 / 建议",
                "treatmentAdvice" to "处理及建议",
                "nextVisitDate" to "下次产检日期 yyyy-MM-dd",
                "reportType" to "报告类型，例如 常规产检 / 血常规 / 尿常规",
                "attachmentNote" to "附件备注",
                "note" to "备注"
            )
            forms["screening_nt"] = smartFormFields(
                "primary" to "检查日期 yyyy-MM-dd",
                "gestationalAge" to "孕周，例如 12+3；只在原文明确写出时填写",
                "ntMm" to "NT 值 mm",
                "conclusion" to "结论文本",
                "attachmentNote" to "附件备注",
                "note" to "备注"
            )
            forms["screening_serum"] = smartFormFields(
                "primary" to "检查日期 yyyy-MM-dd",
                "gestationalAge" to "孕周，例如 16+5",
                "riskT21" to "21 三体风险值，照原文",
                "riskT18" to "18 三体风险值，照原文",
                "riskOntd" to "开放性神经管风险，照原文",
                "riskLevel" to "分级：低危 / 临界 / 高危 / 见报告",
                "conclusion" to "结论文本",
                "note" to "备注"
            )
            forms["screening_nipt"] = smartFormFields(
                "primary" to "检查日期 yyyy-MM-dd",
                "gestationalAge" to "孕周",
                "t21Result" to "T21：低风险 / 高风险 / 见报告",
                "t18Result" to "T18：低风险 / 高风险 / 见报告",
                "t13Result" to "T13：低风险 / 高风险 / 见报告",
                "sexChromosome" to "性染色体结果",
                "conclusion" to "结论文本",
                "note" to "备注"
            )
            forms["screening_anomaly"] = smartFormFields(
                "primary" to "检查日期 yyyy-MM-dd",
                "gestationalAge" to "孕周",
                "structureConclusion" to "结构结论 / 报告描述，照报告原文",
                "conclusion" to "结论文本",
                "attachmentNote" to "附件备注",
                "note" to "备注"
            )
            forms["screening_ogtt"] = smartFormFields(
                "primary" to "检查日期 yyyy-MM-dd",
                "gestationalAge" to "孕周",
                "fastingGlucoseMmolL" to "空腹血糖 mmol/L",
                "oneHourGlucoseMmolL" to "1h 血糖 mmol/L",
                "twoHourGlucoseMmolL" to "2h 血糖 mmol/L",
                "abnormalFlag" to "报告标注：正常 / 需核对 / 见报告",
                "conclusion" to "结论文本",
                "note" to "备注"
            )
            forms["screening_gbs"] = smartFormFields(
                "primary" to "检查日期 yyyy-MM-dd",
                "gestationalAge" to "孕周",
                "gbsResult" to "GBS：阴性 / 阳性 / 见报告",
                "conclusion" to "结论文本",
                "note" to "备注"
            )
            forms["screening_nst"] = smartFormFields(
                "primary" to "检查日期 yyyy-MM-dd",
                "gestationalAge" to "孕周",
                "nstResult" to "胎心监护：反应型 / 无反应型 / 见报告",
                "conclusion" to "结论文本",
                "note" to "备注"
            )
            forms["fetal_movement"] = smartFormFields(
                "primary" to "时段，例如 20:00-21:00",
                "secondary" to "次数",
                "note" to "备注"
            )
            forms["contraction"] = smartFormFields(
                "primary" to "开始时间",
                "secondary" to "间隔",
                "tertiary" to "持续时间",
                "note" to "备注"
            )
            forms["maternal_metric"] = smartFormFields(
                "weightKg" to "体重 kg",
                "systolicBp" to "收缩压 mmHg",
                "diastolicBp" to "舒张压 mmHg",
                "glucoseMmolL" to "血糖 mmol/L",
                "glucoseContext" to "血糖情境：fasting / after_1h / after_2h / random",
                "note" to "备注"
            )
        } else if (stage == BabyLogDomain.STAGE_BABY) {
            forms["feed"] = smartFormFields(
                "primary" to "喂养方式，例如 母乳 / 奶瓶",
                "secondary" to "奶量 ml",
                "note" to "备注"
            )
            forms["breastfeed"] = smartFormFields(
                "primary" to "补充状态",
                "secondary" to "备注"
            )
            forms["bottle"] = smartFormFields(
                "primary" to "奶量或详情",
                "secondary" to "备注"
            )
            forms["sleep"] = smartFormFields(
                "primary" to "开始时间",
                "secondary" to "结束时间",
                "tertiary" to "时长",
                "note" to "备注"
            )
            forms["wake"] = smartFormFields(
                "primary" to "醒来时间或状态",
                "secondary" to "备注"
            )
            forms["diaper"] = smartFormFields(
                "primary" to "尿布类型，例如 尿 / 便",
                "secondary" to "尿布详情，例如 尿量 / 便量",
                "tertiary" to "颜色 / 性状（可选）",
                "note" to "备注"
            )
            forms["pee"] = smartFormFields(
                "primary" to "尿量或状态",
                "secondary" to "备注"
            )
            forms["poop"] = smartFormFields(
                "primary" to "性状 / 颜色",
                "secondary" to "备注"
            )
            forms["temperature"] = smartFormFields(
                "primary" to "体温 ℃",
                "secondary" to "测量方式",
                "note" to "备注"
            )
            forms["medication"] = smartFormFields(
                "primary" to "药名",
                "secondary" to "剂量",
                "tertiary" to "原因"
            )
        }
        return forms
    }

    private fun openNewFamilyForm(stage: String) {
        val profile = BabyLogDomain.ChildProfile.createForNewFamily("", "unknown", "", "", stage, true)
        profilePageState = ProfileDialogState(
            title = if (stage == BabyLogDomain.STAGE_PREGNANCY) "新建孕期家庭" else "新建出生后家庭",
            profile = profile,
            firstRun = true,
            initialStage = stage
        )
        pendingNavRoute = BabyLogRoutes.SettingsProfile
    }

    private fun openProfileEditDialog() {
        profilePageState = ProfileDialogState(
            title = "编辑宝宝档案",
            profile = uiState.childProfile,
            firstRun = false,
            initialStage = uiState.childProfile.stageOverride
        )
        pendingNavRoute = BabyLogRoutes.SettingsProfile
    }

    private fun openDueDateCalculatorFromProfile(input: ProfileInput, firstRun: Boolean) {
        val title = profilePageState?.title ?: if (firstRun) "新建孕期家庭" else "编辑宝宝档案"
        val normalizedStage = normalizeStageInput(input.stageOverride)
        profilePageState = ProfileDialogState(
            title = title,
            profile = BabyLogDomain.ChildProfile.createForNewFamily(
                input.nickname,
                normalizeSexInput(input.sex),
                input.expectedDueDate,
                input.birthDate,
                BabyLogFormatters.parseOptionalNumber(input.prePregnancyWeightKg),
                BabyLogFormatters.parseOptionalNumber(input.heightCm),
                normalizedStage,
                true
            ),
            firstRun = firstRun,
            initialStage = normalizedStage
        )
        pendingNavRoute = BabyLogRoutes.SettingsDueDateCalc
    }

    private fun applyDueDateFromCalculator(dueDate: String) {
        if (!BabyLogFormatters.isValidDateInput(dueDate)) {
            showToast("预产期格式应为 yyyy-MM-dd")
            return
        }
        val current = profilePageState ?: ProfileDialogState(
            title = if (uiState.setupCompleted) "编辑宝宝档案" else "新建孕期家庭",
            profile = if (uiState.setupCompleted) uiState.childProfile else BabyLogDomain.ChildProfile.createForNewFamily("", "unknown", "", "", BabyLogDomain.STAGE_PREGNANCY, true),
            firstRun = !uiState.setupCompleted,
            initialStage = if (uiState.setupCompleted) uiState.childProfile.stageOverride else BabyLogDomain.STAGE_PREGNANCY
        )
        val profile = current.profile
        profilePageState = current.copy(
            profile = BabyLogDomain.ChildProfile.createForNewFamily(
                profile.nickname,
                profile.sex,
                dueDate,
                profile.birthDate,
                profile.prePregnancyWeightKg,
                profile.heightCm,
                current.initialStage,
                true
            )
        )
        showToast("已填入预产期，请在档案页保存后生效")
    }

    private fun saveProfile(input: ProfileInput, firstRun: Boolean) {
        if (input.expectedDueDate.isNotEmpty() && !BabyLogFormatters.isValidDateInput(input.expectedDueDate)) {
            showToast("预产期格式应为 yyyy-MM-dd")
            return
        }
        if (input.birthDate.isNotEmpty() && !BabyLogFormatters.isValidDateInput(input.birthDate)) {
            showToast("出生日期格式应为 yyyy-MM-dd")
            return
        }
        val prePregnancyWeightKg = parsePositiveProfileNumber(input.prePregnancyWeightKg)
        if (prePregnancyWeightKg == null) {
            showToast("孕前体重请填写大于 0 的数字")
            return
        }
        val heightCm = parsePositiveProfileNumber(input.heightCm)
        if (heightCm == null) {
            showToast("身高请填写大于 0 的数字")
            return
        }
        runInBackground {
            try {
                val child = BabyLogDomain.ChildProfile.createForNewFamily(
                    input.nickname,
                    normalizeSexInput(input.sex),
                    input.expectedDueDate,
                    input.birthDate,
                    prePregnancyWeightKg.value,
                    heightCm.value,
                    normalizeStageInput(input.stageOverride),
                    true
                )
                if (firstRun) {
                    repository.saveProfileBundle(
                        BabyLogDomain.FamilyProfile.localDefault(),
                        child,
                        BabyLogDomain.FamilyMember.localManager()
                    )
                    repository.putSyncChange(BabyLogDomain.createSyncChange("familyProfile", BabyLogDomain.FAMILY_ID, "upsert"))
                    repository.putSyncChange(BabyLogDomain.createSyncChange("familyMember", BabyLogDomain.LOCAL_MEMBER_ID, "upsert"))
                } else {
                    repository.saveChildProfile(child)
                }
                repository.putSyncChange(BabyLogDomain.createSyncChange("childProfile", child.id, "upsert"))
                BabyLogSyncPushWorker.enqueueIfConfigured(this)
                runOnUiThread {
                    profilePageState = null
                    pendingNavRoute = BabyLogRoutes.Home
                }
                showToast("档案已保存")
                reloadData()
            } catch (error: JSONException) {
                showInfo("保存失败", error.message ?: "无法保存档案")
            }
        }
    }

    private fun savePreVisitQuestion(id: String?, text: String, visitDate: String, onSaved: () -> Unit) {
        val cleanText = text.trim()
        val cleanDate = visitDate.trim()
        if (cleanText.isBlank()) {
            showToast("请先填写想问的问题")
            return
        }
        if (cleanDate.isNotBlank() && !BabyLogFormatters.isValidDateInput(cleanDate)) {
            showToast("日期格式应为 yyyy-MM-dd")
            return
        }
        runInBackground {
            try {
                preVisitQuestionStore.saveQuestion(id, cleanText, cleanDate)
                runOnUiThread {
                    onSaved()
                    showToast("问题已保存")
                    reloadData()
                }
            } catch (error: JSONException) {
                runOnUiThread {
                    showInfo("保存失败", error.message ?: "无法保存问题")
                }
            }
        }
    }

    private fun deletePreVisitQuestion(question: BabyLogPreVisitQuestionStore.Question) {
        runInBackground {
            preVisitQuestionStore.deleteQuestion(question.id)
            runOnUiThread {
                showToast("问题已删除")
                reloadData()
            }
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT < 33) {
            showToast("当前系统不需要单独开启通知权限")
            return
        }
        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    private fun saveUserReminder(
        id: String?,
        title: String,
        note: String,
        dueDate: String,
        dueTime: String,
        enabled: Boolean,
        onSaved: () -> Unit
    ) {
        if (title.trim().isBlank()) {
            showToast("请填写提醒标题")
            return
        }
        if (!BabyLogFormatters.isValidDateInput(dueDate) || !isValidBabyLogReminderTimeInput(dueTime)) {
            showToast("提醒时间格式不正确")
            return
        }
        val dueAtIso = "${dueDate}T${dueTime}:00.000+0800"
        runInBackground {
            try {
                reminderStore.saveUserReminder(id, title, note, dueAtIso, enabled)
                BabyLogReminderScheduler.scheduleAll(this, reminderStore.listReminders(), repository.loadChildProfile())
                runOnUiThread {
                    onSaved()
                    showToast("提醒已保存")
                    reloadData()
                }
            } catch (error: JSONException) {
                runOnUiThread { showInfo("保存失败", error.message ?: "无法保存提醒") }
            }
        }
    }

    private fun toggleReminder(reminder: BabyLogReminderStore.Reminder, enabled: Boolean) {
        runInBackground {
            reminderStore.setEnabled(reminder.id, enabled)
            BabyLogReminderScheduler.scheduleAll(this, reminderStore.listReminders(), repository.loadChildProfile())
            runOnUiThread { reloadData() }
        }
    }

    private fun dismissReminder(reminder: BabyLogReminderStore.Reminder) {
        runInBackground {
            reminderStore.dismiss(reminder.id)
            BabyLogReminderScheduler.scheduleAll(this, reminderStore.listReminders(), repository.loadChildProfile())
            runOnUiThread { reloadData() }
        }
    }

    private fun completeReminder(reminder: BabyLogReminderStore.Reminder) {
        runInBackground {
            reminderStore.complete(reminder.id)
            BabyLogReminderScheduler.scheduleAll(this, reminderStore.listReminders(), repository.loadChildProfile())
            runOnUiThread { reloadData() }
        }
    }

    private fun deleteReminder(reminder: BabyLogReminderStore.Reminder) {
        if (reminder.source == BabyLogReminderStore.SOURCE_SYSTEM) {
            showToast("系统提醒可忽略或关闭")
            return
        }
        runInBackground {
            reminderStore.delete(reminder.id)
            BabyLogReminderScheduler.scheduleAll(this, reminderStore.listReminders(), repository.loadChildProfile())
            runOnUiThread { reloadData() }
        }
    }

    private fun quickActionsForStage(stage: String): List<BabyLogService.QuickAction> {
        if (stage == BabyLogDomain.STAGE_PREGNANCY) {
            return listOf(
                BabyLogService.QuickAction("B超", "指标 / 照片 / 识别", ChestnutPalette.RoseArgb, "ultrasound"),
                BabyLogService.QuickAction("产检", "常规指标 / 结论 / 附件", ChestnutPalette.VioletArgb, "pregnancy_checkup"),
                BabyLogService.QuickAction("NT", "NT 值 / 结论", ChestnutPalette.VioletArgb, "screening_nt"),
                BabyLogService.QuickAction("唐筛", "风险值 / 分级", ChestnutPalette.VioletArgb, "screening_serum"),
                BabyLogService.QuickAction("无创", "T21/T18/T13", ChestnutPalette.VioletArgb, "screening_nipt"),
                BabyLogService.QuickAction("大排畸", "结构结论", ChestnutPalette.VioletArgb, "screening_anomaly"),
                BabyLogService.QuickAction("糖耐", "空腹 / 1h / 2h", ChestnutPalette.VioletArgb, "screening_ogtt"),
                BabyLogService.QuickAction("GBS", "阴性 / 阳性", ChestnutPalette.VioletArgb, "screening_gbs"),
                BabyLogService.QuickAction("胎监", "反应型 / 备注", ChestnutPalette.VioletArgb, "screening_nst"),
                BabyLogService.QuickAction("胎动", "会话计数 / 规律观察", ChestnutPalette.GreenArgb, "fetal_movement"),
                BabyLogService.QuickAction("宫缩", "开始 / 间隔 / 持续", ChestnutPalette.PeachArgb, "contraction"),
                BabyLogService.QuickAction("孕妈指标", "体重 / 血压 / 血糖", ChestnutPalette.BlueArgb, "maternal_metric")
            )
        }
        if (stage == BabyLogDomain.STAGE_BABY) {
            return listOf(
                BabyLogService.QuickAction("母乳", "", ChestnutPalette.PeachArgb, "breastfeed"),
                BabyLogService.QuickAction("奶瓶", "", ChestnutPalette.BlueArgb, "bottle"),
                BabyLogService.QuickAction("睡眠", "", ChestnutPalette.VioletArgb, "sleep"),
                BabyLogService.QuickAction("起床", "", ChestnutPalette.GreenArgb, "wake"),
                BabyLogService.QuickAction("尿尿", "", ChestnutPalette.YellowArgb, "pee"),
                BabyLogService.QuickAction("便便", "", ChestnutPalette.PeachArgb, "poop")
            )
        }
        return emptyList()
    }

    private fun isBabyCareAction(eventType: String): Boolean {
        return eventType == "feed"
            || eventType == "sleep"
            || eventType == "diaper"
            || eventType == "temperature"
            || eventType == "medication"
    }

    private fun isPregnancyFormAction(eventType: String): Boolean {
        return eventType == "pregnancy_checkup"
            || eventType == "contraction"
            || BabyLogService.isScreeningEventType(eventType)
    }

    internal fun runInBackground(block: () -> Unit) {
        Thread(block).start()
    }

    internal fun showToast(message: String) {
        runOnUiThread {
            if (!isFinishing && !isDestroyed) {
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    internal fun showInfo(title: String, message: String) {
        runOnUiThread {
            if (!isFinishing && !isDestroyed) {
                infoDialog = InfoDialogState(title, message)
            }
        }
    }

    private fun previewBackup(raw: String): ImportPreview {
        try {
            val backup = JSONObject(raw)
            val data = backup.optJSONObject("data")
                ?: throw BabyLogException.ValidationException("无效的栗记备份数据")
            val eventCount = data.optJSONArray("events")?.length() ?: 0
            val hasProfile = (data.optJSONArray("childProfiles")?.length() ?: 0) > 0
            return ImportPreview(eventCount, if (hasProfile) "含宝宝档案" else "不含宝宝档案，导入后需重新建档")
        } catch (error: JSONException) {
            throw BabyLogException.ValidationException(error.message ?: "无效的栗记备份数据", error)
        }
    }

    private companion object {
        private const val META_PREFS_NAME = "babylog_native_meta_v1"
        private const val LAST_BACKUP_EXPORT_MS = "lastBackupExportMs"
    }
}
