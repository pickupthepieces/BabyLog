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
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitLongPressOrCancellation
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.AlertDialog
import androidx.compose.material.BottomNavigation
import androidx.compose.material.BottomNavigationItem
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Card
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Scaffold
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.TextField
import androidx.compose.material.TextFieldDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Article
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Checklist
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FormatListBulleted
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LocalDrink
import androidx.compose.material.icons.rounded.MedicalInformation
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.MonitorHeart
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SsidChart
import androidx.compose.material.icons.rounded.Timelapse
import androidx.compose.material.icons.rounded.Vaccines
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.core.content.ContextCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import org.json.JSONException
import org.json.JSONObject
import app.babylog.nativeapp.ui.screens.BabyLogRoutes
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

public final class ComposeMainActivity : ComponentActivity() {
    private lateinit var repository: BabyLogRepository
    private lateinit var service: BabyLogService
    private lateinit var smartConfigStore: BabyLogSmartConfigStore
    private lateinit var syncSecretStore: BabyLogSyncSecretStore
    private lateinit var disclaimerStore: BabyLogDisclaimerStore
    private lateinit var preVisitQuestionStore: BabyLogPreVisitQuestionStore
    private lateinit var reminderStore: BabyLogReminderStore
    private val smartVisionClient = BabyLogSmartVisionClient()
    private val smartTextClient = BabyLogSmartTextClient()
    private val speechClient = BabyLogParaformerSpeechClient()
    private val remoteSyncClient = BabyLogRemoteSyncClient()
    private lateinit var audioPermissionLauncher: ActivityResultLauncher<String>
    private lateinit var notificationPermissionLauncher: ActivityResultLauncher<String>
    private lateinit var cameraPermissionLauncher: ActivityResultLauncher<String>
    private lateinit var cameraLauncher: ActivityResultLauncher<Uri>
    private lateinit var pickImageLauncher: ActivityResultLauncher<String>
    private lateinit var exportBackupLauncher: ActivityResultLauncher<String>
    private lateinit var importBackupLauncher: ActivityResultLauncher<Array<String>>
    private lateinit var visitSummarySaveLauncher: ActivityResultLauncher<String>

    private var uiState by mutableStateOf(BabyLogUiState())
    private var pendingNavRoute by mutableStateOf<String?>(null)
    private var pendingNavNonce by mutableStateOf(0L)
    private var recordReturnRoute by mutableStateOf(BabyLogRoutes.Home)
    private var recordDetailEventId by mutableStateOf<String?>(null)
    private var recordDetailReturnRoute by mutableStateOf(BabyLogRoutes.Timeline)
    private var highlightedEventId by mutableStateOf<String?>(null)
    private var timelineFilter by mutableStateOf("all")
    private var selectedBabyDay by mutableStateOf(BabyLogFormatters.todayDateInput())
    private var babyCareAction by mutableStateOf<BabyLogService.QuickAction?>(null)
    private var pregnancyAction by mutableStateOf<BabyLogService.QuickAction?>(null)
    private var showFetalMovementSession by mutableStateOf(false)
    private var smartSettingsConfig by mutableStateOf<BabyLogSmartConfigStore.Config?>(null)
    private var speechSettingsConfig by mutableStateOf<BabyLogSmartConfigStore.SpeechConfig?>(null)
    private var smartConfigSummary by mutableStateOf("智能识别未配置")
    private var speechConfigSummary by mutableStateOf("语音识别未配置")
    private var ultrasoundOcrRunning by mutableStateOf(false)
    private var checkupOcrRunning by mutableStateOf(false)
    private var smartEntryRunning by mutableStateOf(false)
    private var smartVoiceState by mutableStateOf(SmartVoiceUiState())
    private var smartEntryCandidate by mutableStateOf<BabyLogSmartTextClient.SmartEntryCandidate?>(null)
    private var longTextVoiceApply: ((String) -> Unit)? = null
    private var babyCareDraft by mutableStateOf<SmartEntryDraft?>(null)
    private var pregnancyDraft by mutableStateOf<SmartEntryDraft?>(null)
    private var maternalMetricDraft by mutableStateOf<SmartEntryDraft?>(null)
    private var ultrasoundDraft by mutableStateOf<SmartEntryDraft?>(null)
    private var ultrasoundOcrCandidate by mutableStateOf<BabyLogSmartInput.UltrasoundOcrCandidate?>(null)
    private var checkupOcrCandidate by mutableStateOf<BabyLogSmartTextClient.SmartFillCandidate?>(null)
    private var showClearLocalConfirm by mutableStateOf(false)
    private var undoImportConfirm by mutableStateOf(false)
    private var profilePageState by mutableStateOf<ProfileDialogState?>(null)
    private var importConfirm by mutableStateOf<ImportConfirmState?>(null)
    private var syncConfirmState by mutableStateOf<SyncConfirmState?>(null)
    private var syncFamilyKeyConfigured by mutableStateOf(false)
    private var syncCheckRunning by mutableStateOf(false)
    private var syncCheckMessage by mutableStateOf("")
    private var syncCheckOk by mutableStateOf<Boolean?>(null)
    private var syncPushRunning by mutableStateOf(false)
    private var syncPushMessage by mutableStateOf("")
    private var syncPushConfirmState by mutableStateOf<SyncPushConfirmState?>(null)
    private var syncPullRunning by mutableStateOf(false)
    private var syncPullMessage by mutableStateOf("")
    private var attachmentListPageState by mutableStateOf<AttachmentListPageState?>(null)
    private var previewAttachment by mutableStateOf<BabyLogDomain.AttachmentRecord?>(null)
    private var editingEvent by mutableStateOf<BabyLogDomain.BabyLogEvent?>(null)
    private var deleteEventConfirm by mutableStateOf<BabyLogDomain.BabyLogEvent?>(null)
    private var infoDialog by mutableStateOf<InfoDialogState?>(null)
    private var voiceRecorder: BabyLogPcmVoiceRecorder? = null
    private var pendingCameraFile: File? = null
    private var pendingUltrasoundPhotoPath by mutableStateOf<String?>(null)
    private var pendingUltrasoundPhotoName by mutableStateOf<String?>(null)
    private var pendingCheckupAttachmentPath by mutableStateOf<String?>(null)
    private var pendingCheckupAttachmentName by mutableStateOf<String?>(null)
    private var pendingImageTarget by mutableStateOf(ImagePickTarget.Ultrasound)
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
        window.statusBarColor = ChestnutPalette.PrimaryArgb
        window.navigationBarColor = ChestnutPalette.PrimaryArgb

        repository = BabyLogRepository(this)
        service = BabyLogService(this, repository)
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
                BabyLogApp(
                    state = uiState,
                    pendingNavRoute = pendingNavRoute,
                    pendingNavNonce = pendingNavNonce,
                    onNavRouteConsumed = { pendingNavRoute = null },
                    recordReturnRoute = recordReturnRoute,
                    recordDetailEventId = recordDetailEventId,
                    recordDetailReturnRoute = recordDetailReturnRoute,
                    highlightedEventId = highlightedEventId,
                    timelineFilter = timelineFilter,
                    selectedBabyDay = selectedBabyDay,
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
                    quickActions = quickActions(),
                    onQuickAction = { action, sourceRoute ->
                        recordReturnRoute = sourceRoute
                        handleQuickAction(action)
                    },
                    attachmentListPageState = attachmentListPageState,
                    previewAttachment = previewAttachment,
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
                    onPolishVisitSummary = ::polishVisitSummary,
                    onSyncNow = ::requestPushSyncNow,
                    onExportBackup = ::exportBackup,
                    onImportBackup = ::importBackup,
                    onUndoImport = { undoImportConfirm = true },
                    onOpenSyncSettings = { pendingNavRoute = BabyLogRoutes.SettingsSync },
                    onOpenSmartSettings = ::openSmartSettings,
                    onOpenSpeechSettings = ::openSpeechSettings,
                    onAcceptDisclaimer = ::acceptMedicalDisclaimer,
                    profilePageState = profilePageState,
                    smartSettingsConfig = smartSettingsConfig,
                    speechSettingsConfig = speechSettingsConfig,
                    smartConfigSummary = smartConfigSummary,
                    speechConfigSummary = speechConfigSummary,
                    syncFamilyKeyConfigured = syncFamilyKeyConfigured,
                    syncCheckRunning = syncCheckRunning,
                    syncCheckMessage = syncCheckMessage,
                    syncCheckOk = syncCheckOk,
                    syncPushRunning = syncPushRunning,
                    syncPushMessage = syncPushMessage,
                    syncPullRunning = syncPullRunning,
                    syncPullMessage = syncPullMessage,
                    onCloseSettingsPage = {
                        profilePageState = null
                        smartSettingsConfig = null
                        speechSettingsConfig = null
                    },
                    onCheckSyncConnection = ::checkSyncConnection,
                    onPushSyncNow = ::requestPushSyncNow,
                    onPullSyncNow = ::requestPullSyncNow,
                    onDismissRemoteUpdateBanner = ::dismissRemoteUpdateBanner,
                    onSaveSyncSettings = ::saveSyncSettings,
                    onSaveSmartSettings = ::saveSmartSettings,
                    onSaveSpeechSettings = ::saveSpeechSettings,
                    onSaveProfile = ::saveProfile,
                    onClearLocalData = { showClearLocalConfirm = true },
                    onOpenTrash = { pendingNavRoute = BabyLogRoutes.LibraryTrash },
                    onOpenDisclaimer = { pendingNavRoute = BabyLogRoutes.SettingsDisclaimer },
                    onOpenDueDateCalculator = { pendingNavRoute = BabyLogRoutes.SettingsDueDateCalc },
                    onOpenWeightGain = { pendingNavRoute = BabyLogRoutes.ToolsWeightGain },
                    onSavePreVisitQuestion = ::savePreVisitQuestion,
                    onDeletePreVisitQuestion = ::deletePreVisitQuestion,
                    onOpenReminderCenter = { pendingNavRoute = BabyLogRoutes.ReminderCenter },
                    onRequestNotificationPermission = ::requestNotificationPermission,
                    onSaveUserReminder = ::saveUserReminder,
                    onToggleReminder = ::toggleReminder,
                    onDismissReminder = ::dismissReminder,
                    onCompleteReminder = ::completeReminder,
                    onDeleteReminder = ::deleteReminder,
                    onOpenDueDateCalculatorFromProfile = ::openDueDateCalculatorFromProfile,
                    onApplyDueDateFromCalculator = ::applyDueDateFromCalculator,
                    onRestoreTrashEvent = { event ->
                        restoreEvent(event.id)
                    },
                    onCreatePregnancyProfile = { openNewFamilyForm(BabyLogDomain.STAGE_PREGNANCY) },
                    onCreateBabyProfile = { openNewFamilyForm(BabyLogDomain.STAGE_BABY) },
                    onEditProfile = { openProfileEditDialog() },
                    onOpenEventDetail = { event, sourceRoute -> openEventDetail(event, sourceRoute) },
                    onEditEvent = { event, sourceRoute -> openEventForEdit(event, sourceRoute) },
                    onDeleteEvent = { deleteEventConfirm = it },
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
                    checkupOcrCandidate = checkupOcrCandidate,
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
                    smartEntryRunning = smartEntryRunning,
                    smartVoiceState = smartVoiceState,
                    smartEntryCandidate = smartEntryCandidate,
                    onSmartEntryBack = {
                        if (!smartEntryRunning) {
                            cancelSmartVoiceRecording()
                            smartEntryCandidate = null
                        }
                    },
                    onSmartEntryVoiceStart = ::startSmartVoiceRecording,
                    onSmartEntryVoiceStop = ::finishSmartVoiceRecording,
                    onLongTextVoiceStart = ::startLongTextVoiceRecording,
                    onLongTextVoiceStop = ::finishLongTextVoiceRecording,
                    onSmartEntrySubmit = ::requestSmartEntry,
                    onSmartEntryCandidateConfirm = ::openSmartEntryCandidate,
                    onSmartEntryCandidateDismiss = { smartEntryCandidate = null }
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

                syncConfirmState?.let { confirm ->
                    ConfirmDialog(
                        title = "确认启用同步",
                        message = "启用后会按你配置的地址和家庭密钥尝试同步。家庭密钥仅保存在本机加密存储中，不会进入导出、备份或家庭同步；当前真实推拉仍在接入中，记录会保留在本机待同步队列中。请确认服务器地址、家庭密钥和医疗数据跨设备风险都已知晓。",
                        confirmText = "我已知晓并保存",
                        destructive = false,
                        onDismiss = { syncConfirmState = null },
                        onConfirm = {
                            syncConfirmState = null
                            persistSyncSettings(confirm.backendBaseUrl, confirm.familyKey)
                        }
                    )
                }

                syncPushConfirmState?.let { confirm ->
                    ConfirmDialog(
                        title = "确认推送",
                        message = "将把 ${confirm.pendingCount} 条本机记录加密上传到 ${confirm.backendBaseUrl}。家庭密钥仅本机保存，服务器仅看到密文。",
                        confirmText = "加密推送",
                        destructive = false,
                        onDismiss = { syncPushConfirmState = null },
                        onConfirm = {
                            syncPushConfirmState = null
                            pushSyncNow()
                        }
                    )
                }

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

    private fun reloadData() {
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

    private fun openUltrasoundForm(draft: SmartEntryDraft? = null) {
        editingEvent = null
        ultrasoundDraft = draft
        pendingUltrasoundPhotoPath = null
        pendingUltrasoundPhotoName = null
        ultrasoundOcrCandidate = null
        ultrasoundOcrRunning = false
        pendingNavRoute = BabyLogRoutes.RecordUltrasound
    }

    private fun openEventDetail(event: BabyLogDomain.BabyLogEvent, sourceRoute: String) {
        recordDetailEventId = event.id
        recordDetailReturnRoute = if (BabyLogRoutes.isTopLevel(sourceRoute)) sourceRoute else BabyLogRoutes.Timeline
        pendingNavRoute = BabyLogRoutes.RecordDetail
        pendingNavNonce = System.nanoTime()
    }

    private fun openEventForEdit(event: BabyLogDomain.BabyLogEvent, sourceRoute: String) {
        if (!isEditablePregnancyRecord(event.eventType)) {
            showInfo("暂不支持编辑", "${BabyLogFormatters.eventLabel(event.eventType)} 记录暂时只支持删除后重录。")
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
                service.recordQuickEvent(action)
                showToast("已记录：${action.label}")
                reloadData()
            } catch (error: Exception) {
                showInfo("保存失败", error.message ?: "无法保存记录")
            }
        }
    }

    private fun handleQuickAction(action: BabyLogService.QuickAction) {
        editingEvent = null
        if (isBabyCareAction(action.eventType)) {
            babyCareDraft = null
            babyCareAction = action
            pendingNavRoute = BabyLogRoutes.RecordBabyCare
        } else if (action.eventType == "fetal_movement") {
            showFetalMovementSession = true
        } else if (action.eventType == "contraction") {
            pendingNavRoute = BabyLogRoutes.RecordContractionSession
        } else if (isPregnancyFormAction(action.eventType)) {
            pregnancyDraft = null
            pregnancyAction = action
            pendingCheckupAttachmentPath = null
            pendingCheckupAttachmentName = null
            checkupOcrCandidate = null
            checkupOcrRunning = false
            pendingNavRoute = BabyLogRoutes.RecordPregnancyEvent
        } else if (action.eventType == "maternal_metric") {
            maternalMetricDraft = null
            pendingNavRoute = BabyLogRoutes.RecordMaternalMetric
        } else if (action.eventType == "ultrasound") {
            openUltrasoundForm()
        } else {
            recordQuickAction(action)
        }
    }

    private fun recordBabyCare(input: BabyLogService.BabyCareInput) {
        runInBackground {
            try {
                val editing = editingEvent?.takeIf { it.eventType == input.eventType }
                val event = if (editing != null) {
                    service.updateBabyCareEvent(editing.id, input)
                } else {
                    service.recordBabyCareEvent(input)
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
                showInfo("保存失败", error.message ?: "无法保存记录")
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
                showInfo("保存失败", error.message ?: "无法保存记录")
            }
        }
    }

    private fun recordFetalMovementSession(input: BabyLogService.FetalMovementSessionInput) {
        runInBackground {
            try {
                service.recordFetalMovementSession(input)
                showToast("已保存胎动计数")
                reloadData()
            } catch (error: JSONException) {
                showInfo("保存失败", error.message ?: "无法保存胎动计数")
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
                showInfo("保存失败", error.message ?: "无法保存宫缩会话")
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
                showInfo("保存失败", error.message ?: "无法保存孕妈指标")
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
                showInfo("保存失败", error.message ?: "无法保存 B 超记录")
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
                showInfo("移入回收站失败", error.message ?: "无法处理这条记录")
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
                showInfo("恢复失败", error.message ?: "无法恢复这条记录")
            }
        }
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
            showInfo("先选择图片", "请先拍照或选择 B 超单图片，再主动识别字段。")
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
                    showInfo("智能识别未配置", "请先在设置页填写多模态模型的 Base URL、模型和 API Key。Key 只保存在本机。")
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
            showInfo("先输入内容", "可以按住说话转成文本，或直接输入一段话，再点智能录入。")
            return
        }
        if (smartEntryRunning) {
            return
        }
        val stage = currentCareStage(uiState.childProfile)
        val forms = smartEntryForms(stage)
        if (forms.isEmpty()) {
            showInfo("暂无可识别表单", "当前阶段还没有可用于智能录入的表单，请先用快捷记录手动选择。")
            return
        }
        smartEntryRunning = true
        smartEntryCandidate = null
        runInBackground {
            try {
                val config = smartConfigStore.load()
                if (!config.isConfigured()) {
                    showInfo("智能识别未配置", "请先在设置页填写模型 Base URL、模型和 API Key。Key 只保存在本机。")
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
            smartVoiceState = smartVoiceState.copy(message = "语音识别需要先配置语音转文字 API；你仍可手动输入文本")
            return
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            smartVoiceState = smartVoiceState.copy(message = "需要麦克风权限；拒绝后可在系统设置中重新授权")
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
                        smartVoiceState = SmartVoiceUiState(message = "语音识别需要先配置语音转文字 API；你仍可手动输入文本")
                    }
                    return@runInBackground
                }
                val result = speechClient.transcribePcm(audioFile, config)
                runOnUiThread {
                    if (fieldApply != null) {
                        val text = result.text.trim()
                        if (text.isNotBlank()) {
                            fieldApply(text)
                            smartVoiceState = SmartVoiceUiState(message = "语音已填入当前字段，请核对后保存")
                        } else {
                            smartVoiceState = SmartVoiceUiState(message = "语音识别未返回文字，可继续手动输入")
                        }
                    } else {
                        smartVoiceState = SmartVoiceUiState(
                            transcript = result.text,
                            transcriptNonce = System.nanoTime(),
                            message = "语音已转文字，请核对后再识别"
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
            showInfo("无法判断记录类型", candidate.warnings.joinToString("\n").ifBlank { "请改写得更具体一点，或从快捷记录手动选择表单。" })
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
                    showInfo("暂不支持", "当前阶段没有 ${candidate.eventType} 表单。")
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
                    showInfo("暂不支持", "当前阶段没有 ${candidate.eventType} 表单。")
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
                    showInfo("暂不支持", "当前阶段没有 ${candidate.eventType} 表单。")
                    return
                }
                babyCareDraft = draft
                babyCareAction = action
                pendingNavRoute = BabyLogRoutes.RecordBabyCare
            }
            else -> {
                showInfo("暂不支持", "已识别为 ${candidate.eventType}，但还没有对应的确认表单。")
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
                "模型响应超时。可以稍后重试，或换用 qwen3-vl-flash 这类更快的模型。"
            message.contains(" 401") || message.contains(" 403") || message.contains("unauthorized", ignoreCase = true) ->
                "模型认证失败。请检查 API Key、Base URL 和模型名称。"
            message.contains(" 413") || message.contains("too large", ignoreCase = true) ->
                "图片仍然过大。已在上传前压缩，若服务商仍拒绝，请重新裁剪 B 超单主体后再试。"
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
                "当前网络不可用，语音作为增强功能已降级；可以直接手动输入"
            else -> message
        }
    }

    private fun copyVisitSummary(text: String) {
        if (text.isBlank()) {
            showToast("没有可复制的汇总内容")
            return
        }
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("BabyLog 复诊汇总", text))
        showToast("已复制复诊汇总")
    }

    private fun shareVisitSummary(text: String) {
        if (text.isBlank()) {
            showToast("没有可分享的汇总内容")
            return
        }
        val send = Intent(Intent.ACTION_SEND)
            .setType("text/markdown")
            .putExtra(Intent.EXTRA_SUBJECT, "BabyLog 复诊汇总")
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
                        showInfo("智能识别未配置", "请先在设置页填写模型 Base URL、模型和 API Key。未配置时仍可导出原始模板。")
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
                showInfo("导出失败", error.message ?: "无法导出备份")
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
                showInfo("导入失败", error.message ?: "无法导入备份")
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
                showInfo("导入失败", error.message ?: "无法导入备份")
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
                showInfo("撤销失败", error.message ?: "没有可恢复的导入快照")
                reloadData()
            }
        }
    }

    private fun requestPushSyncNow() {
        if (syncPushRunning) {
            return
        }
        val dashboard = uiState.dashboard
        val pendingCount = dashboard?.pendingSyncCount ?: 0
        if (pendingCount <= 0) {
            showToast("暂无待推送记录")
            return
        }
        val config = uiState.syncConfig
        if (!config.enabled || config.backendBaseUrl.isBlank()) {
            showInfo("同步未配置", "请先在同步设置里填写家庭后端地址和家庭密钥。")
            return
        }
        if ((dashboard?.syncedSyncCount ?: 0) == 0) {
            syncPushConfirmState = SyncPushConfirmState(config.backendBaseUrl, pendingCount)
            return
        }
        pushSyncNow()
    }

    private fun pushSyncNow() {
        if (syncPushRunning) {
            return
        }
        syncPushRunning = true
        syncPushMessage = "正在加密并推送本机记录..."
        runInBackground {
            try {
                val summary = BabyLogSyncPushOrchestrator().pushOnce(
                    service,
                    repository,
                    syncSecretStore,
                    repository.loadSyncSettings(),
                    remoteSyncClient
                )
                runOnUiThread {
                    syncPushRunning = false
                    syncPushMessage = "上次推送：刚刚，成功 ${summary.pushed}、失败 ${summary.failed}"
                    if (summary.failed > 0 && summary.lastError.isNotBlank()) {
                        syncPushMessage += "；失败原因：${formatSyncError(summary.lastError)}"
                    }
                    showToast(if (summary.failed == 0) "已加密推送 ${summary.pushed} 条" else "推送完成，失败 ${summary.failed} 条")
                }
                reloadData()
            } catch (error: Exception) {
                runOnUiThread {
                    syncPushRunning = false
                    syncPushMessage = "上次推送失败：${error.message ?: "网络不可用"}"
                    showInfo("推送失败", error.message ?: "无法推送本机记录")
                }
            }
        }
    }

    private fun requestPullSyncNow() {
        pullSyncNow(silent = false)
    }

    private fun startForegroundPullLoop() {
        syncPullHandler.removeCallbacks(foregroundPullRunnable)
        if (shouldAutoPullSync()) {
            pullSyncNow(silent = true)
            syncPullHandler.postDelayed(foregroundPullRunnable, 120_000L)
        }
    }

    private fun shouldAutoPullSync(): Boolean {
        val config = repository.loadSyncSettings()
        return config.enabled && config.backendBaseUrl.isNotEmpty() && syncFamilyKeyConfigured
    }

    private fun pullSyncNow(silent: Boolean) {
        if (syncPullRunning) {
            return
        }
        if (!shouldAutoPullSync()) {
            if (!silent) {
                showInfo("同步未配置", "请先在同步设置里填写家庭后端地址和家庭密钥。")
            }
            return
        }
        syncPullRunning = true
        if (!silent) {
            syncPullMessage = "正在拉取家人更新..."
        }
        runInBackground {
            try {
                val summary = BabyLogSyncPullOrchestrator().pullOnce(
                    repository,
                    syncSecretStore,
                    repository.loadSyncSettings(),
                    remoteSyncClient
                )
                runOnUiThread {
                    syncPullRunning = false
                    if (!silent) {
                        syncPullMessage = if (summary.lastError.isBlank()) {
                            "上次拉取：刚刚，新增 ${summary.applied}、忽略 ${summary.skipped}"
                        } else {
                            "上次拉取失败：${formatSyncError(summary.lastError)}"
                        }
                    }
                }
                if (summary.lastError.isBlank()) {
                    reloadData()
                }
                if (!silent) {
                    if (summary.lastError.isBlank()) {
                        showToast(if (summary.applied > 0) "已同步 ${summary.applied} 条家人更新" else "已是最新")
                    } else {
                        showToast("同步失败：${formatSyncError(summary.lastError)}")
                    }
                }
            } catch (error: Exception) {
                runOnUiThread {
                    syncPullRunning = false
                    if (!silent) {
                        syncPullMessage = "上次拉取失败：${error.message ?: "网络不可用"}"
                    }
                }
                if (!silent) {
                    showToast("同步失败：${error.message ?: "网络不可用"}")
                }
            }
        }
    }

    private fun dismissRemoteUpdateBanner() {
        runInBackground {
            service.dismissRemoteUpdateBanner()
            reloadData()
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
            showInfo("先选择图片", "请先拍照或选择产检报告图片，再主动识别字段。")
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
                    showInfo("智能识别未配置", "请先在设置页填写多模态模型的 Base URL、模型和 API Key。Key 只保存在本机。")
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
                "sexChromosome" to "性染色体结果，可空",
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
                "secondary" to "性状 / 颜色 / 量",
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
        if (!BabyLogFormatters.isValidDateInput(dueDate) || !isValidReminderTimeInput(dueTime)) {
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

    private fun runInBackground(block: () -> Unit) {
        Thread(block).start()
    }

    private fun showToast(message: String) {
        runOnUiThread {
            if (!isFinishing && !isDestroyed) {
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showInfo(title: String, message: String) {
        runOnUiThread {
            if (!isFinishing && !isDestroyed) {
                infoDialog = InfoDialogState(title, message)
            }
        }
    }

    private fun previewBackup(raw: String): ImportPreview {
        val backup = JSONObject(raw)
        val data = backup.optJSONObject("data") ?: throw JSONException("Invalid BabyLog backup data")
        val eventCount = data.optJSONArray("events")?.length() ?: 0
        val hasProfile = (data.optJSONArray("childProfiles")?.length() ?: 0) > 0
        return ImportPreview(eventCount, if (hasProfile) "含宝宝档案" else "不含宝宝档案，导入后需重新建档")
    }

    private fun formatSyncError(code: String?): String {
        return when (code) {
            "BACKEND_NOT_CONFIGURED" -> "后端未配置"
            "BACKEND_UNREACHABLE" -> "后端暂不可达"
            "FAMILY_KEY_MISSING" -> "家庭密钥未配置"
            "FAMILY_KEY_LOAD_FAILED" -> "家庭密钥读取失败"
            "ENTITY_NOT_FOUND" -> "本机记录不存在"
            "ENCRYPT_FAILED" -> "加密失败"
            "PUSH_FAILED" -> "网络推送失败"
            "PULL_FAILED" -> "网络拉取失败"
            "STATUS_UPDATE_FAILED" -> "状态更新失败"
            else -> code ?: "未知错误"
        }
    }

    private companion object {
        private const val META_PREFS_NAME = "babylog_native_meta_v1"
        private const val LAST_BACKUP_EXPORT_MS = "lastBackupExportMs"
    }
}

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

private data class OptionalProfileNumber(val value: Double?)

private data class ImportPreview(
    val eventCount: Int,
    val profileLabel: String
)

private data class ImportConfirmState(
    val raw: String,
    val eventCount: Int,
    val profileLabel: String
)

private data class SyncConfirmState(
    val backendBaseUrl: String,
    val familyKey: String
)

private data class SyncPushConfirmState(
    val backendBaseUrl: String,
    val pendingCount: Int
)

internal data class AttachmentListPageState(
    val title: String,
    val attachments: List<BabyLogDomain.AttachmentRecord>
)

private data class InfoDialogState(
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

@Composable
private fun BabyLogApp(
    state: BabyLogUiState,
    pendingNavRoute: String?,
    pendingNavNonce: Long,
    onNavRouteConsumed: () -> Unit,
    recordReturnRoute: String,
    recordDetailEventId: String?,
    recordDetailReturnRoute: String,
    highlightedEventId: String?,
    timelineFilter: String,
    selectedBabyDay: String,
    onTimelineFilterSelected: (String) -> Unit,
    onBabyDaySelected: (String) -> Unit,
    onSmartEntryClick: (String) -> Unit,
    onSmartVoiceHoldStart: (String) -> Unit,
    onSmartVoiceHoldEnd: (String) -> Unit,
    quickActions: List<BabyLogService.QuickAction>,
    onQuickAction: (BabyLogService.QuickAction, String) -> Unit,
    attachmentListPageState: AttachmentListPageState?,
    previewAttachment: BabyLogDomain.AttachmentRecord?,
    onShowAttachments: (String, List<BabyLogDomain.AttachmentRecord>) -> Unit,
    onOpenVisitSummary: () -> Unit,
    onOpenPreVisitQuestions: () -> Unit,
    onCloseAttachmentList: () -> Unit,
    onPreviewAttachment: (BabyLogDomain.AttachmentRecord) -> Unit,
    onCloseAttachmentPreview: () -> Unit,
    onCopyVisitSummary: (String) -> Unit,
    onShareVisitSummary: (String) -> Unit,
    onSaveVisitSummary: (String) -> Unit,
    onPolishVisitSummary: (String, (String?) -> Unit) -> Unit,
    onSyncNow: () -> Unit,
    onExportBackup: () -> Unit,
    onImportBackup: () -> Unit,
    onUndoImport: () -> Unit,
    onOpenSyncSettings: () -> Unit,
    onOpenSmartSettings: () -> Unit,
    onOpenSpeechSettings: () -> Unit,
    onAcceptDisclaimer: () -> Unit,
    profilePageState: ProfileDialogState?,
    smartSettingsConfig: BabyLogSmartConfigStore.Config?,
    speechSettingsConfig: BabyLogSmartConfigStore.SpeechConfig?,
    smartConfigSummary: String,
    speechConfigSummary: String,
    syncFamilyKeyConfigured: Boolean,
    syncCheckRunning: Boolean,
    syncCheckMessage: String,
    syncCheckOk: Boolean?,
    syncPushRunning: Boolean,
    syncPushMessage: String,
    syncPullRunning: Boolean,
    syncPullMessage: String,
    onCloseSettingsPage: () -> Unit,
    onCheckSyncConnection: (String, String) -> Unit,
    onPushSyncNow: () -> Unit,
    onPullSyncNow: () -> Unit,
    onDismissRemoteUpdateBanner: () -> Unit,
    onSaveSyncSettings: (String, String) -> Unit,
    onSaveSmartSettings: (BabyLogSmartConfigStore.Config) -> Unit,
    onSaveSpeechSettings: (BabyLogSmartConfigStore.SpeechConfig) -> Unit,
    onSaveProfile: (ProfileInput, Boolean) -> Unit,
    onClearLocalData: () -> Unit,
    onOpenTrash: () -> Unit,
    onOpenDisclaimer: () -> Unit,
    onOpenDueDateCalculator: () -> Unit,
    onOpenWeightGain: () -> Unit,
    onSavePreVisitQuestion: (String?, String, String, () -> Unit) -> Unit,
    onDeletePreVisitQuestion: (BabyLogPreVisitQuestionStore.Question) -> Unit,
    onOpenReminderCenter: () -> Unit,
    onRequestNotificationPermission: () -> Unit,
    onSaveUserReminder: (String?, String, String, String, String, Boolean, () -> Unit) -> Unit,
    onToggleReminder: (BabyLogReminderStore.Reminder, Boolean) -> Unit,
    onDismissReminder: (BabyLogReminderStore.Reminder) -> Unit,
    onCompleteReminder: (BabyLogReminderStore.Reminder) -> Unit,
    onDeleteReminder: (BabyLogReminderStore.Reminder) -> Unit,
    onOpenDueDateCalculatorFromProfile: (ProfileInput, Boolean) -> Unit,
    onApplyDueDateFromCalculator: (String) -> Unit,
    onRestoreTrashEvent: (BabyLogDomain.BabyLogEvent) -> Unit,
    onCreatePregnancyProfile: () -> Unit,
    onCreateBabyProfile: () -> Unit,
    onEditProfile: () -> Unit,
    onOpenEventDetail: (BabyLogDomain.BabyLogEvent, String) -> Unit,
    onEditEvent: (BabyLogDomain.BabyLogEvent, String) -> Unit,
    onDeleteEvent: (BabyLogDomain.BabyLogEvent) -> Unit,
    babyCareAction: BabyLogService.QuickAction?,
    babyCareDraft: SmartEntryDraft?,
    pregnancyAction: BabyLogService.QuickAction?,
    pregnancyDraft: SmartEntryDraft?,
    maternalMetricDraft: SmartEntryDraft?,
    ultrasoundDraft: SmartEntryDraft?,
    editingEventType: String?,
    pendingUltrasoundPhotoPath: String?,
    pendingUltrasoundPhotoName: String?,
    pendingCheckupAttachmentPath: String?,
    pendingCheckupAttachmentName: String?,
    ultrasoundOcrRunning: Boolean,
    ultrasoundOcrCandidate: BabyLogSmartInput.UltrasoundOcrCandidate?,
    checkupOcrRunning: Boolean,
    checkupOcrCandidate: BabyLogSmartTextClient.SmartFillCandidate?,
    onBabyCareCancel: () -> Unit,
    onPregnancyCancel: () -> Unit,
    onMaternalMetricCancel: () -> Unit,
    onUltrasoundCancel: () -> Unit,
    onBabyCareSave: (BabyLogService.BabyCareInput) -> Unit,
    onPregnancySave: (BabyLogService.PregnancyInput) -> Unit,
    onContractionSessionSave: (BabyLogService.ContractionSessionInput) -> Unit,
    onMaternalMetricSave: (BabyLogService.MaternalMetricInput) -> Unit,
    onUltrasoundSave: (BabyLogService.UltrasoundInput) -> Unit,
    onPickCheckupAttachment: () -> Unit,
    onCaptureCheckupAttachment: () -> Unit,
    onPickUltrasoundPhoto: () -> Unit,
    onCaptureUltrasoundPhoto: () -> Unit,
    onRecognizeUltrasoundPhoto: () -> Unit,
    onDismissUltrasoundCandidate: () -> Unit,
    onApplyUltrasoundCandidate: () -> Unit,
    onRecognizeCheckupAttachment: () -> Unit,
    onDismissCheckupCandidate: () -> Unit,
    onApplyCheckupCandidate: () -> Unit,
    smartEntryRunning: Boolean,
    smartVoiceState: SmartVoiceUiState,
    smartEntryCandidate: BabyLogSmartTextClient.SmartEntryCandidate?,
    onSmartEntryBack: () -> Unit,
    onSmartEntryVoiceStart: () -> Unit,
    onSmartEntryVoiceStop: () -> Unit,
    onLongTextVoiceStart: LongTextVoiceStart,
    onLongTextVoiceStop: () -> Unit,
    onSmartEntrySubmit: (String) -> Unit,
    onSmartEntryCandidateConfirm: (BabyLogSmartTextClient.SmartEntryCandidate) -> Unit,
    onSmartEntryCandidateDismiss: () -> Unit
) {
    val navController = rememberNavController()
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStackEntry?.destination?.route
    val activeTab = if (BabyLogRoutes.isTopLevel(currentRoute)) currentRoute ?: BabyLogRoutes.Home else BabyLogRoutes.Home
    var quickRailVisible by rememberSaveable { mutableStateOf(true) }
    val showTopLevelChrome = state.disclaimerAccepted && state.setupCompleted && BabyLogRoutes.isTopLevel(currentRoute)
    val selectTopLevelTab: (String) -> Unit = { route ->
        if (BabyLogRoutes.isTopLevel(route)) {
            navController.navigate(route) {
                popUpTo(BabyLogRoutes.Home) { saveState = true }
                launchSingleTop = true
                restoreState = true
            }
        }
    }
    LaunchedEffect(pendingNavRoute, pendingNavNonce) {
        val route = pendingNavRoute ?: return@LaunchedEffect
        if (BabyLogRoutes.isTopLevel(route)) {
            if (!navController.popBackStack(route, false)) {
                navController.navigate(route) {
                    popUpTo(BabyLogRoutes.Home) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
            }
        } else {
            navController.navigate(route) {
                launchSingleTop = true
            }
        }
        onNavRouteConsumed()
    }
    LaunchedEffect(activeTab) {
        quickRailVisible = true
    }
    fun closeRecord(onCancel: () -> Unit) {
        onCancel()
        if (!navController.popBackStack()) {
            selectTopLevelTab(if (BabyLogRoutes.isTopLevel(recordReturnRoute)) recordReturnRoute else BabyLogRoutes.Home)
        }
    }
    fun closeSmartEntry() {
        onSmartEntryBack()
        if (!navController.popBackStack()) {
            selectTopLevelTab(if (BabyLogRoutes.isTopLevel(recordReturnRoute)) recordReturnRoute else BabyLogRoutes.Home)
        }
    }
    fun closeSettingsPage() {
        onCloseSettingsPage()
        if (!navController.popBackStack()) {
            selectTopLevelTab(if (state.setupCompleted) BabyLogRoutes.Settings else BabyLogRoutes.Home)
        }
    }
    fun closeDueDateCalculator() {
        if (!navController.popBackStack()) {
            selectTopLevelTab(BabyLogRoutes.Settings)
        }
    }
    fun closeBrowseSubpage() {
        if (!navController.popBackStack()) {
            selectTopLevelTab(BabyLogRoutes.Library)
        }
    }
    fun closeAttachmentList() {
        onCloseAttachmentList()
        if (!navController.popBackStack()) {
            selectTopLevelTab(BabyLogRoutes.Library)
        }
    }
    fun closeAttachmentPreview() {
        onCloseAttachmentPreview()
        if (!navController.popBackStack()) {
            selectTopLevelTab(BabyLogRoutes.Library)
        }
    }
    fun closeTrashPage() {
        if (!navController.popBackStack()) {
            selectTopLevelTab(BabyLogRoutes.Settings)
        }
    }
    fun closeRecordDetail() {
        if (!navController.popBackStack()) {
            selectTopLevelTab(if (BabyLogRoutes.isTopLevel(recordDetailReturnRoute)) recordDetailReturnRoute else BabyLogRoutes.Timeline)
        }
    }

    Scaffold(
        backgroundColor = ChestnutPalette.Bg,
        topBar = {
            if (state.disclaimerAccepted &&
                currentRoute != BabyLogRoutes.Disclaimer &&
                !BabyLogRoutes.isRecord(currentRoute) &&
                currentRoute != BabyLogRoutes.SmartEntry &&
                !BabyLogRoutes.isSettingsSubpage(currentRoute) &&
                !BabyLogRoutes.isBrowseSubpage(currentRoute)
            ) {
                TopBrandBand(activeTab = activeTab, state = state)
            }
        },
        bottomBar = {
            if (showTopLevelChrome) {
                Column {
                    if (activeTab == BabyLogRoutes.Home) {
                        AnimatedVisibility(
                            visible = quickActions.isNotEmpty() && quickRailVisible,
                            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
                        ) {
                            PersistentQuickRail(
                                actions = quickActions,
                                onAction = { action -> onQuickAction(action, BabyLogRoutes.Home) }
                            )
                        }
                    }
                    BottomNav(
                        activeTab = activeTab,
                        voiceState = smartVoiceState,
                        onTabSelected = selectTopLevelTab,
                        onSmartEntryClick = { onSmartEntryClick(activeTab) },
                        onVoiceHoldStart = { onSmartVoiceHoldStart(activeTab) },
                        onVoiceHoldEnd = { onSmartVoiceHoldEnd(activeTab) }
                    )
                }
            }
        }
    ) { inner ->
        NavHost(
            navController = navController,
            startDestination = if (state.disclaimerAccepted) BabyLogRoutes.Home else BabyLogRoutes.Disclaimer,
            enterTransition = { EnterTransition.None },
            exitTransition = { ExitTransition.None },
            popEnterTransition = { EnterTransition.None },
            popExitTransition = { ExitTransition.None }
        ) {
            composable(BabyLogRoutes.Disclaimer) {
                MedicalDisclaimerGateScreen(onAccept = onAcceptDisclaimer)
            }
            composable(BabyLogRoutes.Home) {
                if (!state.setupCompleted) {
                    BabyLogScreenColumn(inner) {
                        item {
                            FirstRunScreen(
                                onCreatePregnancyProfile = onCreatePregnancyProfile,
                                onCreateBabyProfile = onCreateBabyProfile,
                                onImportBackup = onImportBackup
                            )
                        }
                    }
                } else {
                    HomeScreen(
                        inner = inner,
                        state = state,
                        selectedBabyDay = selectedBabyDay,
                        highlightedEventId = highlightedEventId,
                        onBabyDaySelected = onBabyDaySelected,
                        onShowTimeline = { selectTopLevelTab(BabyLogRoutes.Timeline) },
                        onOpenDetail = { event -> onOpenEventDetail(event, BabyLogRoutes.Home) },
                        onEditEvent = { event -> onEditEvent(event, BabyLogRoutes.Home) },
                        onDeleteEvent = onDeleteEvent,
                        onOpenWeightGain = { navController.navigate(BabyLogRoutes.ToolsWeightGain) },
                        onOpenReminderCenter = onOpenReminderCenter,
                        syncPulling = syncPullRunning,
                        onPullSyncNow = onPullSyncNow,
                        onDismissSyncBanner = onDismissRemoteUpdateBanner,
                        onQuickRailVisibilityChange = { visible ->
                            if (quickRailVisible != visible) {
                                quickRailVisible = visible
                            }
                        }
                    )
                }

            }
            composable(BabyLogRoutes.Timeline) {
                TimelineScreen(
                    inner = inner,
                    state = state,
                    selectedFilter = timelineFilter,
                    highlightedEventId = highlightedEventId,
                    syncPulling = syncPullRunning,
                    onFilterSelected = onTimelineFilterSelected,
                    onPullSyncNow = onPullSyncNow,
                    onDismissSyncBanner = onDismissRemoteUpdateBanner,
                    onOpenDetail = { event -> onOpenEventDetail(event, BabyLogRoutes.Timeline) },
                    onEditEvent = { event -> onEditEvent(event, BabyLogRoutes.Timeline) },
                    onDeleteEvent = onDeleteEvent
                )
            }
            composable(BabyLogRoutes.RecordDetail) {
                val event = state.timeline.firstOrNull { it.id == recordDetailEventId }
                    ?: state.trashEvents.firstOrNull { it.id == recordDetailEventId }
                RecordDetailScreen(
                    event = event,
                    allEvents = state.timeline,
                    attachments = state.attachments,
                    onBack = ::closeRecordDetail,
                    onPreviewAttachment = onPreviewAttachment,
                    onOpenPreVisitQuestions = onOpenPreVisitQuestions,
                    onEdit = { detailEvent -> onEditEvent(detailEvent, recordDetailReturnRoute) },
                    onDelete = onDeleteEvent
                )
            }
            composable(BabyLogRoutes.Library) {
                LibraryRootScreen(
                    inner = inner,
                    state = state,
                    onShowAttachments = onShowAttachments,
                    onOpenVisitSummary = onOpenVisitSummary,
                    onOpenPreVisitQuestions = onOpenPreVisitQuestions
                )
            }
            composable(BabyLogRoutes.LibraryVisitSummary) {
                VisitSummaryScreen(
                    events = state.timeline,
                    attachments = state.attachments,
                    preVisitQuestions = state.preVisitQuestions,
                    onBack = ::closeBrowseSubpage,
                    onCopy = onCopyVisitSummary,
                    onShare = onShareVisitSummary,
                    onSaveFile = onSaveVisitSummary,
                    onPolish = onPolishVisitSummary
                )
            }
            composable(BabyLogRoutes.LibraryAttachments) {
                AttachmentListScreen(
                    state = attachmentListPageState,
                    onBack = ::closeAttachmentList,
                    onPreview = onPreviewAttachment
                )
            }
            composable(BabyLogRoutes.AttachmentPreview) {
                AttachmentPreviewScreen(
                    attachment = previewAttachment,
                    onBack = ::closeAttachmentPreview
                )
            }
            composable(BabyLogRoutes.Settings) {
                SettingsRootScreen(
                    inner = inner,
                    state = state,
                    smartConfigSummary = smartConfigSummary,
                    speechConfigSummary = speechConfigSummary,
                    onSyncNow = onSyncNow,
                    onExportBackup = onExportBackup,
                    onImportBackup = onImportBackup,
                    onUndoImport = onUndoImport,
                    onOpenSyncSettings = onOpenSyncSettings,
                    onOpenSmartSettings = onOpenSmartSettings,
                    onOpenSpeechSettings = onOpenSpeechSettings,
                    onClearLocalData = onClearLocalData,
                    onOpenTrash = onOpenTrash,
                    onOpenDisclaimer = onOpenDisclaimer,
                    onOpenDueDateCalculator = onOpenDueDateCalculator,
                    onOpenWeightGain = onOpenWeightGain,
                    onOpenPreVisitQuestions = onOpenPreVisitQuestions,
                    onOpenReminderCenter = onOpenReminderCenter,
                    onEditProfile = onEditProfile
                )
            }
            composable(BabyLogRoutes.PreVisitQuestions) {
                PreVisitQuestionsScreen(
                    questions = state.preVisitQuestions,
                    onBack = ::closeBrowseSubpage,
                    onSave = onSavePreVisitQuestion,
                    onDelete = onDeletePreVisitQuestion
                )
            }
            composable(BabyLogRoutes.ReminderCenter) {
                ReminderCenterScreen(
                    reminders = state.reminders,
                    systemMuted = BabyLogReminderStore.isSystemMuted(state.childProfile),
                    notificationPermissionGranted = state.notificationPermissionGranted,
                    onBack = ::closeBrowseSubpage,
                    onRequestNotificationPermission = onRequestNotificationPermission,
                    onSaveUserReminder = onSaveUserReminder,
                    onToggleReminder = onToggleReminder,
                    onDismissReminder = onDismissReminder,
                    onCompleteReminder = onCompleteReminder,
                    onDeleteReminder = onDeleteReminder
                )
            }
            composable(BabyLogRoutes.LibraryTrash) {
                TrashScreen(
                    events = state.trashEvents,
                    onBack = ::closeTrashPage,
                    onRestore = onRestoreTrashEvent
                )
            }
            composable(BabyLogRoutes.SettingsProfile) {
                ProfileSettingsScreen(
                    state = profilePageState,
                    onBack = ::closeSettingsPage,
                    onOpenDueDateCalculator = onOpenDueDateCalculatorFromProfile,
                    onSave = onSaveProfile
                )
            }
            composable(BabyLogRoutes.SettingsDueDateCalc) {
                DueDateCalcScreen(
                    currentExpectedDueDate = state.childProfile.expectedDueDate,
                    onBack = ::closeDueDateCalculator,
                    onApplyDueDate = { dueDate ->
                        onApplyDueDateFromCalculator(dueDate)
                        if (!navController.popBackStack(BabyLogRoutes.SettingsProfile, false)) {
                            navController.navigate(BabyLogRoutes.SettingsProfile) {
                                launchSingleTop = true
                            }
                        }
                    }
                )
            }
            composable(BabyLogRoutes.ToolsWeightGain) {
                WeightGainScreen(
                    profile = state.childProfile,
                    events = state.timeline,
                    onBack = {
                        if (!navController.popBackStack()) {
                            selectTopLevelTab(BabyLogRoutes.Home)
                        }
                    },
                    onEditProfile = onEditProfile
                )
            }
            composable(BabyLogRoutes.SettingsSync) {
                SyncSettingsScreen(
                    config = state.syncConfig,
                    familyKeyConfigured = syncFamilyKeyConfigured,
                    checkingConnection = syncCheckRunning,
                    connectionMessage = syncCheckMessage,
                    connectionOk = syncCheckOk,
                    pendingSyncCount = state.dashboard?.pendingSyncCount ?: 0,
                    syncedSyncCount = state.dashboard?.syncedSyncCount ?: 0,
                    failedSyncCount = state.dashboard?.failedSyncCount ?: 0,
                    pushingSync = syncPushRunning,
                    pushMessage = syncPushMessage,
                    pullingSync = syncPullRunning,
                    pullMessage = syncPullMessage,
                    lastPulledAt = state.dashboard?.lastPulledAt ?: "",
                    remoteUpdateBannerCount = state.dashboard?.remoteUpdateBannerCount ?: 0,
                    onBack = ::closeSettingsPage,
                    onCheckConnection = onCheckSyncConnection,
                    onPushNow = onPushSyncNow,
                    onPullNow = onPullSyncNow,
                    onSave = onSaveSyncSettings
                )
            }
            composable(BabyLogRoutes.SettingsModel) {
                SmartModelSettingsScreen(
                    config = smartSettingsConfig,
                    onBack = ::closeSettingsPage,
                    onSave = onSaveSmartSettings
                )
            }
            composable(BabyLogRoutes.SettingsSpeech) {
                SpeechSettingsScreen(
                    config = speechSettingsConfig,
                    onBack = ::closeSettingsPage,
                    onSave = onSaveSpeechSettings
                )
            }
            composable(BabyLogRoutes.SettingsDisclaimer) {
                MedicalDisclaimerReviewScreen(onBack = ::closeSettingsPage)
            }
            composable(BabyLogRoutes.RecordBabyCare) {
                BabyCareFormScreen(
                    action = babyCareAction,
                    draft = babyCareDraft,
                    isEditing = editingEventType == babyCareAction?.eventType,
                    voiceState = smartVoiceState,
                    onLongTextVoiceStart = onLongTextVoiceStart,
                    onLongTextVoiceStop = onLongTextVoiceStop,
                    onBack = { closeRecord(onBabyCareCancel) },
                    onSave = onBabyCareSave
                )
            }
            composable(BabyLogRoutes.RecordPregnancyEvent) {
                PregnancyEventFormScreen(
                    action = pregnancyAction,
                    draft = pregnancyDraft,
                    isEditing = editingEventType == pregnancyAction?.eventType,
                    expectedDueDate = state.childProfile.expectedDueDate,
                    attachmentPath = pendingCheckupAttachmentPath,
                    attachmentName = pendingCheckupAttachmentName,
                    ocrRunning = checkupOcrRunning,
                    ocrCandidate = checkupOcrCandidate,
                    onPickAttachment = onPickCheckupAttachment,
                    onCaptureAttachment = onCaptureCheckupAttachment,
                    onRecognizeAttachment = onRecognizeCheckupAttachment,
                    onCandidateDismiss = onDismissCheckupCandidate,
                    onCandidateApplied = onApplyCheckupCandidate,
                    voiceState = smartVoiceState,
                    onLongTextVoiceStart = onLongTextVoiceStart,
                    onLongTextVoiceStop = onLongTextVoiceStop,
                    onBack = { closeRecord(onPregnancyCancel) },
                    onSave = onPregnancySave
                )
            }
            composable(BabyLogRoutes.RecordContractionSession) {
                ContractionSessionScreen(
                    onBack = {
                        if (!navController.popBackStack()) {
                            selectTopLevelTab(BabyLogRoutes.Home)
                        }
                    },
                    onSave = onContractionSessionSave
                )
            }
            composable(BabyLogRoutes.RecordMaternalMetric) {
                MaternalMetricFormScreen(
                    draft = maternalMetricDraft,
                    isEditing = editingEventType == "maternal_metric",
                    voiceState = smartVoiceState,
                    onLongTextVoiceStart = onLongTextVoiceStart,
                    onLongTextVoiceStop = onLongTextVoiceStop,
                    onBack = { closeRecord(onMaternalMetricCancel) },
                    onSave = onMaternalMetricSave
                )
            }
            composable(BabyLogRoutes.RecordUltrasound) {
                UltrasoundFormScreen(
                    defaultGestationalAge = currentGestationalAgeInput(state.childProfile),
                    expectedDueDate = state.childProfile.expectedDueDate,
                    draft = ultrasoundDraft,
                    isEditing = editingEventType == "ultrasound",
                    photoPath = pendingUltrasoundPhotoPath,
                    photoName = pendingUltrasoundPhotoName,
                    ocrRunning = ultrasoundOcrRunning,
                    ocrCandidate = ultrasoundOcrCandidate,
                    onPickPhoto = onPickUltrasoundPhoto,
                    onCapturePhoto = onCaptureUltrasoundPhoto,
                    onRecognizePhoto = onRecognizeUltrasoundPhoto,
                    onCandidateDismiss = onDismissUltrasoundCandidate,
                    onCandidateApplied = onApplyUltrasoundCandidate,
                    voiceState = smartVoiceState,
                    onLongTextVoiceStart = onLongTextVoiceStart,
                    onLongTextVoiceStop = onLongTextVoiceStop,
                    onBack = { closeRecord(onUltrasoundCancel) },
                    onSave = onUltrasoundSave
                )
            }
            composable(BabyLogRoutes.SmartEntry) {
                SmartEntryScreen(
                    running = smartEntryRunning,
                    voiceState = smartVoiceState,
                    candidate = smartEntryCandidate,
                    onBack = ::closeSmartEntry,
                    onVoiceStart = onSmartEntryVoiceStart,
                    onVoiceStop = onSmartEntryVoiceStop,
                    onSubmit = onSmartEntrySubmit,
                    onOpenCandidate = onSmartEntryCandidateConfirm,
                    onDismissCandidate = onSmartEntryCandidateDismiss
                )
            }
        }
    }
}

@Composable
internal fun BabyLogScreenColumn(
    inner: PaddingValues,
    modifier: Modifier = Modifier,
    listState: LazyListState = rememberLazyListState(),
    content: LazyListScope.() -> Unit
) {
    LazyColumn(
        state = listState,
        modifier = modifier
            .fillMaxSize()
            .background(ChestnutPalette.Bg),
        contentPadding = PaddingValues(
            start = 18.dp,
            top = inner.calculateTopPadding() + 16.dp,
            end = 18.dp,
            bottom = inner.calculateBottomPadding() + 22.dp
        ),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        content = content
    )
}

@Composable
private fun TopBrandBand(activeTab: String, state: BabyLogUiState) {
    val stage = currentCareStage(state.childProfile)
    val title = if (state.setupCompleted && activeTab != BabyLogRoutes.Home) tabTitle(activeTab) else "BabyLog"
    val subtitle = if (!state.setupCompleted) {
        "先建档，再进入家庭记录"
    } else {
        val nickname = state.childProfile.nickname.ifBlank { "宝宝" }
        "$nickname · ${stageLabel(stage)}"
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(ChestnutPalette.Primary)
            .padding(horizontal = 22.dp, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = Color.White,
                fontSize = if (activeTab == BabyLogRoutes.Home) 32.sp else 24.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.sp
            )
            if (activeTab == BabyLogRoutes.Home || !state.setupCompleted) {
                Text(
                    text = subtitle,
                    color = Color.White.copy(alpha = 0.82f),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun FirstRunScreen(
    onCreatePregnancyProfile: () -> Unit,
    onCreateBabyProfile: () -> Unit,
    onImportBackup: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text(
            text = "BabyLog 仅做家庭记录和复诊沟通辅助；数据默认保存在本机。",
            color = Color(0xFF7C4A21),
            modifier = Modifier
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xFFFFEBCB))
                .padding(14.dp)
        )
        Panel {
            Text("开始使用", color = ChestnutPalette.Ink, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Text("选择当前家庭状态", color = ChestnutPalette.Muted)
            ActionRow(
                title = "新建孕期家庭",
                subtitle = "录入乳名、性别和预产期；日期可后补",
                action = "建档",
                onClick = onCreatePregnancyProfile
            )
            Divider(color = ChestnutPalette.Border)
            ActionRow(
                title = "新建出生后家庭",
                subtitle = "录入乳名、性别和出生日期；日期可后补",
                action = "建档",
                onClick = onCreateBabyProfile
            )
            Divider(color = ChestnutPalette.Border)
            ActionRow(
                title = "导入备份",
                subtitle = "从 BabyLog JSON 恢复本机记录和档案",
                action = "导入",
                onClick = onImportBackup
            )
        }
    }
}

@Composable
internal fun WeekCard(profile: BabyLogDomain.ChildProfile) {
    val dueDate = profile.expectedDueDate
    val validDueDate = BabyLogFormatters.isValidDateInput(dueDate)
    val daysToDue = if (validDueDate) daysBetween(BabyLogFormatters.todayDateInput(), dueDate) else 0
    val gestationalDays = if (validDueDate) (280 - daysToDue).coerceIn(0, 280) else -1
    Card(
        shape = RoundedCornerShape(16.dp),
        backgroundColor = ChestnutPalette.Surface,
        border = null,
        elevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Chip("孕期", ChestnutPalette.PrimarySoft, ChestnutPalette.Primary)
                Chip(
                    if (validDueDate) "距预产期 $daysToDue 天" else "预产期待补",
                    ChestnutPalette.Surface2,
                    ChestnutPalette.Muted
                )
            }
            Spacer(Modifier.height(16.dp))
            Text(
                if (gestationalDays >= 0) BabyLogFormatters.formatGestationalAge(gestationalDays) else "孕期档案待补全",
                color = ChestnutPalette.Ink,
                fontSize = 34.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(4.dp))
            Text(
                if (validDueDate) "预产期 $dueDate" else "设置页可补录预产期",
                color = ChestnutPalette.Muted,
                fontSize = 15.sp
            )
            Spacer(Modifier.height(16.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(CircleShape)
                    .background(ChestnutPalette.Surface2)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(if (gestationalDays >= 0) (gestationalDays / 280f).coerceIn(0.03f, 1f) else 0.03f)
                        .height(8.dp)
                        .clip(CircleShape)
                        .background(ChestnutPalette.Primary)
                )
            }
        }
    }
}

@Composable
internal fun BabyDayCard(
    profile: BabyLogDomain.ChildProfile,
    selectedDay: String,
    onPreviousDay: () -> Unit,
    onToday: () -> Unit,
    onNextDay: () -> Unit
) {
    val nickname = profile.nickname.ifBlank { "宝宝" }
    val age = if (BabyLogFormatters.isValidDateInput(profile.birthDate)) {
        "出生日期 ${profile.birthDate} · 第 ${kotlin.math.max(1, daysBetween(profile.birthDate, BabyLogFormatters.todayDateInput()) + 1)} 天"
    } else {
        "出生日期待补；设置页可补录"
    }
    val dayLabel = if (selectedDay == BabyLogFormatters.todayDateInput()) "今天" else selectedDay
    Card(
        shape = RoundedCornerShape(22.dp),
        backgroundColor = ChestnutPalette.Surface,
        border = BorderStroke(1.dp, ChestnutPalette.Border),
        elevation = 4.dp
    ) {
        Column(
            modifier = Modifier
                .background(Brush.linearGradient(listOf(ChestnutPalette.Surface, ChestnutPalette.PrimarySoft)))
                .padding(18.dp)
        ) {
            Chip("出生后", ChestnutPalette.PrimarySoft, ChestnutPalette.Primary)
            Spacer(Modifier.height(12.dp))
            Text("${nickname}的日视图", color = ChestnutPalette.Ink, fontSize = 30.sp, fontWeight = FontWeight.Bold)
            Text(age, color = ChestnutPalette.Muted, fontSize = 14.sp)
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(onClick = onPreviousDay, border = BorderStroke(1.dp, ChestnutPalette.Primary)) {
                    Text("前一天", color = ChestnutPalette.Primary)
                }
                Button(
                    modifier = Modifier.weight(1f),
                    onClick = onToday,
                    colors = ButtonDefaults.buttonColors(backgroundColor = ChestnutPalette.Primary)
                ) {
                    Text(dayLabel, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                OutlinedButton(onClick = onNextDay, border = BorderStroke(1.dp, ChestnutPalette.Primary)) {
                    Text("后一天", color = ChestnutPalette.Primary)
                }
            }
        }
    }
}

@Composable
internal fun BabyDaySummary(events: List<BabyLogDomain.BabyLogEvent>, selectedDay: String) {
    val feedCount = events.count { it.eventType == "feed" || it.eventType == "breastfeed" || it.eventType == "bottle" }
    val sleepCount = events.count { it.eventType == "sleep" || it.eventType == "wake" }
    val diaperCount = events.count { it.eventType == "diaper" || it.eventType == "pee" || it.eventType == "poop" }
    Panel {
        SectionHeader(title = if (selectedDay == BabyLogFormatters.todayDateInput()) "今日摘要" else "当日摘要")
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MetricCard(
                title = "喂养",
                value = "$feedCount 次",
                subtitle = "母乳 / 奶瓶",
                tone = ChestnutPalette.Peach,
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                title = "睡眠",
                value = "$sleepCount 条",
                subtitle = "睡眠 / 起床",
                tone = ChestnutPalette.Violet,
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                title = "尿布",
                value = "$diaperCount 次",
                subtitle = "尿尿 / 便便",
                tone = ChestnutPalette.Yellow,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
internal fun PregnancySummaryPanel(
    events: List<BabyLogDomain.BabyLogEvent>,
    onOpenWeightGain: () -> Unit = {}
) {
    val latestUltrasound = events.firstOrNull { it.eventType == "ultrasound" }
    val latestCheckup = events.firstOrNull { it.eventType == "pregnancy_checkup" }
    val latestMaternalMetric = events.firstOrNull { it.eventType == "maternal_metric" }
    val reviewCount = events.count { it.eventType == "ultrasound" && ultrasoundWarningText(it).isNotBlank() }
    val pendingReview = events.firstOrNull { it.eventType == "ultrasound" && ultrasoundWarningText(it).isNotBlank() }
    val nextVisitDate = latestCheckup?.payload?.optString("nextVisitDate", "")
        ?.takeIf { BabyLogFormatters.isValidDateInput(it) }
        ?: latestCheckup?.payload?.optString("nextVisitNote", "")?.let(::extractDateInput)
    val nextVisitDays = nextVisitDate?.let { daysBetween(BabyLogFormatters.todayDateInput(), it) }
    val hasAnyData = latestUltrasound != null || latestCheckup != null || latestMaternalMetric != null
    Panel {
        SectionHeader(title = "孕期摘要", action = "增重曲线", onAction = onOpenWeightGain)
        if (!hasAnyData) {
            EmptyPanel("记录第一次产检或 B 超，这里会显示摘要和待复核提醒")
            return@Panel
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MetricCard(
                title = "最近 B 超",
                value = latestUltrasound?.let { BabyLogFormatters.formatEventDay(it.occurredAt) } ?: "暂无",
                subtitle = latestUltrasound?.let { BabyLogFormatters.eventSummary(it) } ?: "保存 B 超后显示",
                tone = ChestnutPalette.Rose,
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                title = "最近产检",
                value = latestCheckup?.let { BabyLogFormatters.formatEventDay(it.occurredAt) } ?: "暂无",
                subtitle = latestCheckup?.let { BabyLogFormatters.eventSummary(it) } ?: "记录产检结论",
                tone = ChestnutPalette.Violet,
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MetricCard(
                title = "孕妈指标",
                value = latestMaternalMetric?.let { BabyLogFormatters.formatEventDay(it.occurredAt) } ?: "暂无",
                subtitle = latestMaternalMetric?.let { BabyLogFormatters.eventSummary(it) } ?: "体重 / 血压 / 血糖",
                tone = ChestnutPalette.Blue,
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                title = "待复核",
                value = if (reviewCount == 0) "0 项" else "$reviewCount 项",
                subtitle = pendingReview?.let { BabyLogFormatters.formatEventDay(it.occurredAt) }
                    ?: if (latestUltrasound == null) "录入 B 超后检查" else "暂无待复核",
                tone = if (reviewCount == 0) ChestnutPalette.Green else ChestnutPalette.Danger,
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                title = "下次产检",
                value = nextVisitDays?.let {
                    when {
                        it < 0 -> "日期已到"
                        it == 0 -> "今天"
                        else -> "$it 天"
                    }
                } ?: "未填写",
                subtitle = nextVisitDate ?: "产检备注里写 yyyy-MM-dd",
                tone = ChestnutPalette.Accent,
                modifier = Modifier.weight(1f)
            )
        }
        if (reviewCount > 0) {
            Spacer(Modifier.height(10.dp))
            Text(
                text = pendingReview?.let { "待复核 B 超：${BabyLogFormatters.eventSummary(it)}。请以医生意见为准；这里仅提醒复核录入值。" }
                    ?: "有 B 超指标超出常用软范围，请以医生意见为准；这里仅提醒复核录入值。",
                color = ChestnutPalette.Danger,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun TodayPanel(dashboard: BabyLogService.DashboardSnapshot?) {
    Panel {
        SectionHeader(title = "今日")
        val total = dashboard?.todayCounts?.values?.sum() ?: 0
        val latest = dashboard?.recentEvents?.firstOrNull()
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MetricCard(
                title = "今日记录",
                value = "$total 条",
                subtitle = "本机已保存",
                tone = ChestnutPalette.Primary,
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                title = "上次记录",
                value = latest?.let { BabyLogFormatters.formatRelativeTime(it.occurredAt) } ?: "暂无",
                subtitle = latest?.let { BabyLogFormatters.eventLabel(it.eventType) } ?: "点 + 开始",
                tone = ChestnutPalette.Accent,
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                title = "待同步",
                value = "${dashboard?.pendingSyncCount ?: 0} 条",
                subtitle = "后端未配置",
                tone = ChestnutPalette.Violet,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
internal fun TrendPanel(events: List<BabyLogDomain.BabyLogEvent>, stage: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        TrendCard(
            title = if (stage == BabyLogDomain.STAGE_BABY) "宝宝体重" else "胎儿 EFW",
            value = if (stage == BabyLogDomain.STAGE_BABY) "暂无数据" else latestEfwValue(events),
            subtitle = if (stage == BabyLogDomain.STAGE_BABY) "录入成长后显示" else latestUltrasoundCaption(events),
            tone = ChestnutPalette.Rose,
            modifier = Modifier.weight(1f)
        )
        TrendCard(
            title = if (stage == BabyLogDomain.STAGE_BABY) "身长 / 头围" else "BPD / FL",
            value = if (stage == BabyLogDomain.STAGE_BABY) "暂无数据" else latestBpdFlValue(events),
            subtitle = if (stage == BabyLogDomain.STAGE_BABY) "录入儿保后显示" else latestUltrasoundCaption(events),
            tone = ChestnutPalette.Green,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
internal fun TimelineFilters(selected: String, onSelect: (String) -> Unit) {
    val filters = listOf(
        "all" to "全部",
        "pregnancy" to "孕期",
        "ultrasound" to "B 超",
        "baby" to "宝宝",
        "temperature" to "体温",
        "checkup" to "产检"
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        filters.forEach { (key, label) ->
            val active = key == selected
            Text(
                text = label,
                color = if (active) Color.White else ChestnutPalette.Muted,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(if (active) ChestnutPalette.Primary else ChestnutPalette.Surface2)
                    .border(1.dp, ChestnutPalette.Border, CircleShape)
                    .clickable { onSelect(key) }
                    .padding(horizontal = 16.dp, vertical = 9.dp)
            )
        }
    }
}

@Composable
internal fun TimelineRow(
    event: BabyLogDomain.BabyLogEvent,
    highlighted: Boolean = false,
    onClick: (() -> Unit)? = null,
    onEdit: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null
) {
    val tone = remember(event.eventType) { eventTone(event.eventType) }
    val rowText = remember(event) {
        TimelineRowText(
            occurredAt = "${BabyLogFormatters.formatEventDay(event.occurredAt)} ${BabyLogFormatters.formatEventTime(event.occurredAt)}",
            label = BabyLogFormatters.eventLabel(event.eventType),
            summary = BabyLogFormatters.eventSummary(event),
            attachmentLabel = if (event.attachmentIds.isEmpty()) "" else "附件 ${event.attachmentIds.size}"
        )
    }
    Card(
        modifier = Modifier.then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
        shape = RoundedCornerShape(14.dp),
        backgroundColor = ChestnutPalette.Surface,
        border = BorderStroke(if (highlighted) 2.dp else 1.dp, if (highlighted) ChestnutPalette.Primary else ChestnutPalette.Border),
        elevation = 0.dp
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .width(5.dp)
                    .height(86.dp)
                    .background(tone)
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = rowText.occurredAt,
                        color = ChestnutPalette.Muted,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f)
                    )
                    Chip(
                        text = rowText.label,
                        bg = tone.copy(alpha = 0.14f),
                        fg = tone
                    )
                }
                Spacer(Modifier.height(7.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = rowText.summary,
                        color = ChestnutPalette.Ink,
                        fontSize = 17.sp,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    if (onEdit != null) {
                        Spacer(Modifier.width(8.dp))
                        TextButton(
                            onClick = onEdit,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text("编辑", color = ChestnutPalette.Primary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    if (onDelete != null) {
                        Spacer(Modifier.width(8.dp))
                        TextButton(
                            onClick = onDelete,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text("删除", color = ChestnutPalette.Danger, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                if (event.attachmentIds.isNotEmpty()) {
                    Spacer(Modifier.height(5.dp))
                    Text(rowText.attachmentLabel, color = ChestnutPalette.Primary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

private data class TimelineRowText(
    val occurredAt: String,
    val label: String,
    val summary: String,
    val attachmentLabel: String
)

@Composable
internal fun LibraryScreen(
    attachments: List<BabyLogDomain.AttachmentRecord>,
    stage: String,
    typeFilter: String = "all",
    onShowAttachments: (String, List<BabyLogDomain.AttachmentRecord>) -> Unit
) {
    data class LibraryEntry(
        val title: String,
        val count: String,
        val note: String,
        val icon: LineIcon,
        val type: String,
        val detailAttachments: List<BabyLogDomain.AttachmentRecord>?
    )
    val ultrasoundAttachments = attachments.filter { it.kind == "ultrasound_image" }
    val documentAttachments = attachments.filter { it.kind == "document_image" }
    val vaccineAttachments = attachments.filter { it.kind == "vaccine_image" }
    val pregnancyEntries = listOf(
        LibraryEntry("B 超单", attachmentCount(ultrasoundAttachments), "已保存本机；表单内可识别字段", LineIcon.Ultrasound, "ultrasound_image", ultrasoundAttachments),
        LibraryEntry("检查单", attachmentCount(documentAttachments), "孕期常规检查、血检报告", LineIcon.Checkup, "document_image", documentAttachments),
        LibraryEntry("出生证明", "待支持", "出生资料归档入口待补", LineIcon.File, "other", null),
        LibraryEntry("疫苗本", attachmentCount(vaccineAttachments), "出生后启用；可显示已导入附件", LineIcon.Vaccine, "vaccine_image", vaccineAttachments)
    )
    val babyEntries = listOf(
        LibraryEntry("出生证明", "待支持", "出生资料归档入口待补", LineIcon.File, "other", null),
        LibraryEntry("疫苗本", attachmentCount(vaccineAttachments), "出生后启用；可显示已导入附件", LineIcon.Vaccine, "vaccine_image", vaccineAttachments),
        LibraryEntry("B 超单", attachmentCount(ultrasoundAttachments), "孕期资料仍可查看", LineIcon.Ultrasound, "ultrasound_image", ultrasoundAttachments),
        LibraryEntry("检查单", attachmentCount(documentAttachments), "孕期常规检查、血检报告", LineIcon.Checkup, "document_image", documentAttachments)
    )
    val entries = (if (stage == BabyLogDomain.STAGE_BABY) babyEntries else pregnancyEntries)
        .filter { typeFilter == "all" || it.type == typeFilter }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        entries.forEach { entry ->
            LibraryItem(
                title = entry.title,
                count = entry.count,
                note = entry.note,
                icon = entry.icon,
                onClick = entry.detailAttachments?.let { files -> { onShowAttachments(entry.title, files) } }
            )
        }
    }
}

@Composable
internal fun SettingsScreen(
    state: BabyLogUiState,
    onSyncNow: () -> Unit,
    onExportBackup: () -> Unit,
    onImportBackup: () -> Unit,
    onUndoImport: () -> Unit,
    onOpenSyncSettings: () -> Unit,
    onOpenSmartSettings: () -> Unit,
    onOpenSpeechSettings: () -> Unit,
    smartConfigSummary: String,
    speechConfigSummary: String,
    onClearLocalData: () -> Unit,
    onOpenTrash: () -> Unit,
    onOpenDisclaimer: () -> Unit,
    onOpenDueDateCalculator: () -> Unit,
    onOpenWeightGain: () -> Unit,
    onOpenPreVisitQuestions: () -> Unit,
    onOpenReminderCenter: () -> Unit,
    onEditProfile: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        SettingsPanel("档案") {
            ActionRow(
                title = "宝宝昵称",
                subtitle = state.childProfile.nickname.ifBlank { "待补" },
                action = "编辑",
                onClick = onEditProfile
            )
            Divider(color = ChestnutPalette.Border)
            ActionRow(
                title = "当前阶段",
                subtitle = stageLabel(currentCareStage(state.childProfile)),
                action = "编辑",
                onClick = onEditProfile
            )
            Divider(color = ChestnutPalette.Border)
            ActionRow(
                title = "预产期 / 出生日期",
                subtitle = "预产期 ${state.childProfile.expectedDueDate.ifBlank { "待补" }} · 出生 ${state.childProfile.birthDate.ifBlank { "待补" }}",
                action = "编辑",
                onClick = onEditProfile
            )
            Divider(color = ChestnutPalette.Border)
            ActionRow(
                title = "阶段覆盖",
                subtitle = stageOverrideLabel(state.childProfile.stageOverride),
                action = "编辑",
                onClick = onEditProfile
            )
            Divider(color = ChestnutPalette.Border)
            ActionRow(
                title = "孕周 / 预产期计算器",
                subtitle = "LMP、周期和早期 B 超 CRL 辅助推算",
                action = "打开",
                onClick = onOpenDueDateCalculator
            )
            Divider(color = ChestnutPalette.Border)
            ActionRow(
                title = "孕期增重曲线",
                subtitle = "按孕前 BMI 展示 IOM 参考带和体重历史",
                action = "查看",
                onClick = onOpenWeightGain
            )
            Divider(color = ChestnutPalette.Border)
            ActionRow(
                title = "想问医生的问题",
                subtitle = if (state.preVisitQuestions.isEmpty()) "产检前随手记录待问事项" else "${state.preVisitQuestions.size} 条待问",
                action = "管理",
                onClick = onOpenPreVisitQuestions
            )
        }
        SettingsPanel("提醒") {
            ActionRow(
                title = "提醒中心",
                subtitle = "${state.reminders.count { BabyLogReminderStore.isActionable(it) }} 条可查看提醒；系统通知可单独授权",
                action = "打开",
                onClick = onOpenReminderCenter
            )
        }
        SettingsPanel("同步") {
            ActionRow(
                title = "同步状态",
                subtitle = if (state.syncConfig.enabled) state.syncConfig.backendBaseUrl else "后端未配置，记录保存在本机",
                action = "设置",
                onClick = onOpenSyncSettings
            )
            Divider(color = ChestnutPalette.Border)
            ActionRow(
                title = "立即推送",
                subtitle = "待同步 ${state.dashboard?.pendingSyncCount ?: 0} 条，已推送 ${state.dashboard?.syncedSyncCount ?: 0} 条，失败 ${state.dashboard?.failedSyncCount ?: 0} 条",
                action = "推送",
                onClick = onSyncNow
            )
        }
        SettingsPanel("智能识别") {
            ActionRow(
                title = "OCR / 智能解析模型",
                subtitle = smartConfigSummary,
                action = "设置",
                onClick = onOpenSmartSettings
            )
            Divider(color = ChestnutPalette.Border)
            ActionRow(
                title = "语音转文字 STT",
                subtitle = speechConfigSummary,
                action = "设置",
                onClick = onOpenSpeechSettings
            )
        }
        SettingsPanel("备份") {
            ActionRow(
                title = "导出 BabyLog JSON",
                subtitle = BabyLogFormatters.formatBackupAgeLabel(state.lastBackupExportMs, System.currentTimeMillis()),
                action = "导出",
                onClick = onExportBackup
            )
            Divider(color = ChestnutPalette.Border)
            ActionRow(
                title = "导入 BabyLog JSON",
                subtitle = "会覆盖当前本机事件、附件和同步队列",
                action = "导入",
                onClick = onImportBackup
            )
            Divider(color = ChestnutPalette.Border)
            ActionRow(
                title = "撤销上次导入",
                subtitle = if (state.hasImportUndoSnapshot) "恢复到最近一次导入前的本机快照" else "暂无可撤销的导入快照",
                action = "撤销",
                actionColor = ChestnutPalette.Danger,
                onClick = if (state.hasImportUndoSnapshot) onUndoImport else null
            )
        }
        SettingsPanel("本机") {
            ActionRow(
                title = "回收站",
                subtitle = if (state.trashEvents.isEmpty()) {
                    "暂无已删除记录；误删后 7 天内可恢复"
                } else {
                    "${state.trashEvents.size} 条记录待清理；7 天内可恢复"
                },
                action = "查看",
                onClick = onOpenTrash
            )
            Divider(color = ChestnutPalette.Border)
            ActionRow(
                title = "医疗免责声明",
                subtitle = "查看非医疗器械、AI 候选需人工确认等说明",
                action = "查看",
                onClick = onOpenDisclaimer
            )
            Divider(color = ChestnutPalette.Border)
            ActionRow(
                title = "本机占用",
                subtitle = "记录和附件 ${BabyLogFormatters.formatByteSize(state.dashboard?.localBytes ?: 0)}",
                action = "查看",
                onClick = null
            )
            Divider(color = ChestnutPalette.Border)
            ActionRow(
                title = "清空本机数据",
                subtitle = "删除记录、附件和待同步队列",
                action = "清空",
                actionColor = ChestnutPalette.Danger,
                onClick = onClearLocalData
            )
        }
        Text(
            text = "医疗判断仍以医生意见为准。BabyLog 只做家庭记录和资料整理。",
            color = Color(0xFF7C4A21),
            modifier = Modifier
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xFFFFEBCB))
                .padding(14.dp)
        )
    }
}

@Composable
private fun VoiceRecordingPopup() {
    Popup(
        alignment = Alignment.Center,
        properties = PopupProperties(focusable = false)
    ) {
        Surface(
            color = ChestnutPalette.Surface,
            shape = RoundedCornerShape(24.dp),
            elevation = 12.dp
        ) {
            Row(
                modifier = Modifier
                    .border(1.dp, ChestnutPalette.Primary.copy(alpha = 0.28f), RoundedCornerShape(24.dp))
                    .padding(horizontal = 22.dp, vertical = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                BabyLogIconTile(
                    icon = LineIcon.Voice,
                    tint = ChestnutPalette.Primary,
                    tileColor = ChestnutPalette.Primary.copy(alpha = 0.14f),
                    modifier = Modifier.size(48.dp),
                    iconSize = 28.dp
                )
                Column {
                    Text("正在录音", color = ChestnutPalette.Ink, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text("松开后转成文字", color = ChestnutPalette.Muted, fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
private fun ConfirmDialog(
    title: String,
    message: String,
    confirmText: String,
    destructive: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, color = ChestnutPalette.Ink, fontWeight = FontWeight.Bold) },
        text = { Text(message, color = ChestnutPalette.Muted) },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = if (destructive) ChestnutPalette.Danger else ChestnutPalette.Primary
                )
            ) { Text(confirmText, color = Color.White) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消", color = ChestnutPalette.Muted) }
        },
        backgroundColor = ChestnutPalette.Bg
    )
}

@Composable
internal fun LibraryItem(
    title: String,
    count: String,
    note: String,
    icon: LineIcon,
    onClick: (() -> Unit)?
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        backgroundColor = ChestnutPalette.Surface,
        border = BorderStroke(1.dp, ChestnutPalette.Border),
        elevation = 2.dp,
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BabyLogIconTile(
                icon = icon,
                tint = ChestnutPalette.Primary,
                tileColor = ChestnutPalette.Primary.copy(alpha = 0.14f),
                modifier = Modifier.size(46.dp),
                iconSize = 28.dp
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, color = ChestnutPalette.Ink, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                Text(note, color = ChestnutPalette.Muted, fontSize = 13.sp)
            }
            Text(count, color = ChestnutPalette.Primary, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
internal fun SettingsPanel(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, color = ChestnutPalette.Ink, fontSize = 19.sp, fontWeight = FontWeight.Bold)
        Card(
            shape = RoundedCornerShape(16.dp),
            backgroundColor = ChestnutPalette.Surface,
            border = BorderStroke(1.dp, ChestnutPalette.Border),
            elevation = 2.dp
        ) {
            Column(content = content)
        }
    }
}

@Composable
internal fun ActionRow(
    title: String,
    subtitle: String,
    action: String,
    actionColor: Color = ChestnutPalette.Primary,
    onClick: (() -> Unit)?
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, color = ChestnutPalette.Ink, fontWeight = FontWeight.Bold)
            Text(subtitle, color = ChestnutPalette.Muted, fontSize = 13.sp)
        }
        Text(action, color = if (onClick == null) ChestnutPalette.Text3 else actionColor, fontWeight = FontWeight.Bold)
    }
}

@Composable
internal fun BabyLogIconTile(
    icon: LineIcon,
    tint: Color,
    tileColor: Color,
    modifier: Modifier = Modifier.size(44.dp),
    iconSize: Dp = 26.dp
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(tileColor),
        contentAlignment = Alignment.Center
    ) {
        BabyLogMaterialIcon(
            icon = icon,
            tint = tint,
            modifier = Modifier.size(iconSize)
        )
    }
}

@Composable
internal fun BabyLogMaterialIcon(
    icon: LineIcon,
    tint: Color,
    modifier: Modifier = Modifier.size(24.dp)
) {
    Icon(
        imageVector = icon.imageVector,
        contentDescription = null,
        tint = tint,
        modifier = modifier
    )
}

internal fun quickActionIcon(eventType: String): LineIcon {
    return when (eventType) {
        "ultrasound" -> LineIcon.Ultrasound
        "pregnancy_checkup" -> LineIcon.Checkup
        "screening_nt", "screening_serum", "screening_nipt", "screening_anomaly", "screening_ogtt", "screening_gbs", "screening_nst" -> LineIcon.Checkup
        "fetal_movement" -> LineIcon.Movement
        "contraction" -> LineIcon.Contraction
        "maternal_metric" -> LineIcon.Metric
        "breastfeed" -> LineIcon.Breastfeed
        "bottle" -> LineIcon.Bottle
        "sleep" -> LineIcon.Sleep
        "wake" -> LineIcon.Wake
        "pee", "poop", "diaper" -> LineIcon.Diaper
        else -> LineIcon.File
    }
}

@Composable
private fun BottomNav(
    activeTab: String,
    voiceState: SmartVoiceUiState,
    onTabSelected: (String) -> Unit,
    onSmartEntryClick: () -> Unit,
    onVoiceHoldStart: () -> Unit,
    onVoiceHoldEnd: () -> Unit
) {
    val leadingItems = listOf(
        NavItem(BabyLogRoutes.Home, "首页", LineIcon.Home),
        NavItem(BabyLogRoutes.Timeline, "时间线", LineIcon.Timeline)
    )
    val trailingItems = listOf(
        NavItem(BabyLogRoutes.Library, "资料", LineIcon.Library),
        NavItem(BabyLogRoutes.Settings, "设置", LineIcon.Settings)
    )
    BottomNavigation(
        backgroundColor = ChestnutPalette.Primary,
        contentColor = Color.White,
        elevation = 0.dp
    ) {
        leadingItems.forEach { item ->
            BottomNavTab(item = item, selected = activeTab == item.key, onTabSelected = onTabSelected)
        }
        BottomNavVoiceAction(
            voiceState = voiceState,
            onSmartEntryClick = onSmartEntryClick,
            onVoiceHoldStart = onVoiceHoldStart,
            onVoiceHoldEnd = onVoiceHoldEnd
        )
        trailingItems.forEach { item ->
            BottomNavTab(item = item, selected = activeTab == item.key, onTabSelected = onTabSelected)
        }
    }
}

@Composable
private fun RowScope.BottomNavTab(
    item: NavItem,
    selected: Boolean,
    onTabSelected: (String) -> Unit
) {
    val itemColor = if (selected) Color.White else Color.White.copy(alpha = 0.68f)
    BottomNavigationItem(
        selected = selected,
        onClick = { onTabSelected(item.key) },
        icon = {
            BabyLogIconTile(
                icon = item.icon,
                tint = itemColor,
                tileColor = if (selected) Color.White.copy(alpha = 0.20f) else Color.White.copy(alpha = 0.10f),
                modifier = Modifier.size(40.dp),
                iconSize = 24.dp
            )
        },
        label = {
            Text(
                item.label,
                color = itemColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        },
        selectedContentColor = Color.White,
        unselectedContentColor = Color.White.copy(alpha = 0.68f)
    )
}

@Composable
private fun RowScope.BottomNavVoiceAction(
    voiceState: SmartVoiceUiState,
    onSmartEntryClick: () -> Unit,
    onVoiceHoldStart: () -> Unit,
    onVoiceHoldEnd: () -> Unit
) {
    val currentOnSmartEntryClick by rememberUpdatedState(onSmartEntryClick)
    val currentOnVoiceHoldStart by rememberUpdatedState(onVoiceHoldStart)
    val currentOnVoiceHoldEnd by rememberUpdatedState(onVoiceHoldEnd)
    val label = when {
        voiceState.isRecording -> "松开"
        voiceState.isTranscribing -> "识别"
        else -> "语音"
    }
    Column(
        modifier = Modifier
            .weight(1f)
            .height(56.dp)
            .pointerInput(voiceState.isTranscribing) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val longPress = awaitLongPressOrCancellation(down.id)
                    if (longPress == null) {
                        currentOnSmartEntryClick()
                        return@awaitEachGesture
                    }
                    if (voiceState.isTranscribing) {
                        waitForUpOrCancellation()
                        return@awaitEachGesture
                    }
                    currentOnVoiceHoldStart()
                    try {
                        waitForUpOrCancellation()
                    } finally {
                        currentOnVoiceHoldEnd()
                    }
                }
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(
                    if (voiceState.isRecording) {
                        Color.White.copy(alpha = 0.34f)
                    } else {
                        Color.White.copy(alpha = 0.22f)
                    }
                )
                .border(1.dp, Color.White.copy(alpha = 0.52f), RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {
            BabyLogMaterialIcon(
                icon = LineIcon.Voice,
                tint = Color.White,
                modifier = Modifier.size(26.dp)
            )
        }
        Text(
            text = label,
            color = Color.White,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

private data class NavItem(val key: String, val label: String, val icon: LineIcon)

internal enum class LineIcon(val imageVector: ImageVector) {
    Home(Icons.Rounded.Home),
    Timeline(Icons.Rounded.FormatListBulleted),
    Library(Icons.Rounded.Article),
    Settings(Icons.Rounded.Settings),
    Voice(Icons.Rounded.Mic),
    Ultrasound(Icons.Rounded.MonitorHeart),
    Checkup(Icons.Rounded.Checklist),
    Movement(Icons.Rounded.Favorite),
    Contraction(Icons.Rounded.Timelapse),
    Metric(Icons.Rounded.SsidChart),
    Breastfeed(Icons.Rounded.Favorite),
    Bottle(Icons.Rounded.LocalDrink),
    Sleep(Icons.Rounded.Bedtime),
    Wake(Icons.Rounded.WbSunny),
    Diaper(Icons.Rounded.WaterDrop),
    File(Icons.Rounded.Description),
    Vaccine(Icons.Rounded.Vaccines)
}

internal data class BabyCareLabels(
    val primary: String,
    val secondary: String,
    val tertiary: String?,
    val note: String?,
    val primaryKeyboard: KeyboardType = KeyboardType.Text,
    val secondaryKeyboard: KeyboardType = KeyboardType.Text
)

internal data class PregnancyLabels(
    val primary: String,
    val secondary: String,
    val tertiary: String?,
    val note: String?,
    val primaryKeyboard: KeyboardType = KeyboardType.Text,
    val secondaryKeyboard: KeyboardType = KeyboardType.Text,
    val tertiaryKeyboard: KeyboardType = KeyboardType.Text
)

internal fun babyCareLabels(eventType: String): BabyCareLabels {
    return when (eventType) {
        "feed" -> BabyCareLabels("方式，例如 母乳 / 奶瓶 / 辅食", "奶量 ml，例如 120", null, "备注", KeyboardType.Text, KeyboardType.Decimal)
        "sleep" -> BabyCareLabels("开始时间，例如 22:10", "结束时间，例如 01:20", "地点，例如 卧室", "备注")
        "diaper" -> BabyCareLabels("类型，例如 尿 / 便 / 混合", "性状或备注", null, "备注")
        "temperature" -> BabyCareLabels("体温", "测量方式，例如 腋温", null, "备注", KeyboardType.Decimal, KeyboardType.Text)
        "medication" -> BabyCareLabels("药名", "剂量，例如 2 ml", "原因", null)
        "breastfeed" -> BabyCareLabels("详情，例如 左侧 12 分钟", "备注", null, null)
        "bottle" -> BabyCareLabels("详情，例如 120 ml", "备注", null, null, KeyboardType.Text, KeyboardType.Text)
        "wake" -> BabyCareLabels("状态，例如 自然醒 / 哭醒", "备注", null, null)
        "pee" -> BabyCareLabels("尿布情况", "备注", null, null)
        "poop" -> BabyCareLabels("性状 / 颜色", "备注", null, null)
        else -> BabyCareLabels("详情", "备注", null, null)
    }
}

internal fun pregnancyLabels(eventType: String): PregnancyLabels {
    return when (eventType) {
        "pregnancy_checkup" -> PregnancyLabels(
            "检查日期 yyyy-MM-dd",
            "医院 / 机构",
            "医生结论 / 建议",
            "备注"
        )
        "screening_nt" -> PregnancyLabels("检查日期 yyyy-MM-dd", "NT 值 mm", "结论文本", "备注", KeyboardType.Text, KeyboardType.Decimal)
        "screening_serum" -> PregnancyLabels("检查日期 yyyy-MM-dd", "21 三体风险值", "18 三体风险值", "备注")
        "screening_nipt" -> PregnancyLabels("检查日期 yyyy-MM-dd", "T21 结果", "T18 结果", "结论文本")
        "screening_anomaly" -> PregnancyLabels("检查日期 yyyy-MM-dd", "结构结论", null, "备注")
        "screening_ogtt" -> PregnancyLabels("检查日期 yyyy-MM-dd", "空腹血糖", "1h 血糖", "备注", KeyboardType.Text, KeyboardType.Decimal, KeyboardType.Decimal)
        "screening_gbs" -> PregnancyLabels("检查日期 yyyy-MM-dd", "GBS 结果", null, "备注")
        "screening_nst" -> PregnancyLabels("检查日期 yyyy-MM-dd", "胎心监护结果", null, "备注")
        "fetal_movement" -> PregnancyLabels(
            "时段，例如 20:00-21:00",
            "次数，例如 10",
            null,
            "备注",
            KeyboardType.Text,
            KeyboardType.Decimal
        )
        "contraction" -> PregnancyLabels(
            "开始时间，例如 22:10",
            "间隔分钟，例如 5",
            "持续秒，例如 40",
            "备注",
            KeyboardType.Text,
            KeyboardType.Decimal,
            KeyboardType.Decimal
        )
        else -> PregnancyLabels("详情", "备注", null, null)
    }
}

internal fun defaultPregnancyPrimary(eventType: String): String {
    return if (eventType == "pregnancy_checkup" || BabyLogService.isScreeningEventType(eventType)) BabyLogFormatters.todayDateInput() else ""
}

internal fun buildBabyCareInput(
    eventType: String,
    primary: String,
    secondary: String,
    tertiary: String,
    note: String
): BabyLogService.BabyCareInput {
    return when (eventType) {
        "feed" -> BabyLogService.BabyCareInput.feed(primary, secondary, note)
        "sleep" -> BabyLogService.BabyCareInput.sleep(primary, secondary, tertiary, note)
        "diaper" -> BabyLogService.BabyCareInput.diaper(primary, secondary, note)
        "temperature" -> BabyLogService.BabyCareInput.temperature(primary, secondary, note)
        "medication" -> BabyLogService.BabyCareInput.medication(primary, secondary, tertiary)
        "breastfeed", "bottle", "wake", "pee", "poop" -> BabyLogService.BabyCareInput.quick(eventType, primary, secondary)
        else -> BabyLogService.BabyCareInput.feed(primary, secondary, note)
    }
}

internal fun buildPregnancyInput(
    eventType: String,
    primary: String,
    secondary: String,
    tertiary: String,
    note: String
): BabyLogService.PregnancyInput {
    return when (eventType) {
        "pregnancy_checkup" -> BabyLogService.PregnancyInput.checkup(primary, secondary, tertiary, note)
        "fetal_movement" -> BabyLogService.PregnancyInput.fetalMovement(primary, secondary, note)
        "contraction" -> BabyLogService.PregnancyInput.contraction(primary, secondary, tertiary, note)
        else -> if (BabyLogService.isScreeningEventType(eventType)) {
            BabyLogService.PregnancyInput.screening(
                eventType,
                primary,
                "",
                mapOf("conclusion" to tertiary, "detail" to secondary),
                note,
                "",
                ""
            )
        } else {
            BabyLogService.PregnancyInput.fetalMovement(primary, secondary, note)
        }
    }
}

internal fun currentCareStage(profile: BabyLogDomain.ChildProfile): String {
    return BabyLogFormatters.resolveCareStage(profile, BabyLogFormatters.todayDateInput())
}

internal fun currentGestationalAgeInput(profile: BabyLogDomain.ChildProfile): String {
    if (!BabyLogFormatters.isValidDateInput(profile.expectedDueDate)) {
        return ""
    }
    return gestationalAgeInputForDate(profile.expectedDueDate, BabyLogFormatters.todayDateInput())
}

internal fun gestationalAgeInputForDate(expectedDueDate: String, examDate: String): String {
    if (!BabyLogFormatters.isValidDateInput(expectedDueDate) || !BabyLogFormatters.isValidDateInput(examDate)) {
        return ""
    }
    val daysToDue = daysBetween(examDate, expectedDueDate)
    val gestationalDays = (280 - daysToDue).coerceIn(0, 280)
    return BabyLogFormatters.formatGestationalAge(gestationalDays).removeSuffix(" 周")
}

private fun smartFormFields(vararg values: Pair<String, String?>): Map<String, String> {
    return linkedMapOf(*values.filter { !it.second.isNullOrBlank() }
        .map { it.first to it.second.orEmpty() }
        .toTypedArray())
}

internal fun isEditablePregnancyRecord(eventType: String): Boolean {
    return eventType == "ultrasound" ||
        eventType == "pregnancy_checkup" ||
        eventType == "maternal_metric" ||
        eventType == "fetal_movement" ||
        eventType == "contraction" ||
        BabyLogService.isScreeningEventType(eventType) ||
        isEditableBabyRecord(eventType)
}

private fun isEditableBabyRecord(eventType: String): Boolean {
    return eventType == "feed" ||
        eventType == "sleep" ||
        eventType == "diaper" ||
        eventType == "temperature" ||
        eventType == "medication" ||
        eventType == "breastfeed" ||
        eventType == "bottle" ||
        eventType == "wake" ||
        eventType == "pee" ||
        eventType == "poop"
}

private fun draftFromBabyCareEvent(event: BabyLogDomain.BabyLogEvent): SmartEntryDraft {
    val payload = event.payload
    val values = when (event.eventType) {
        "feed" -> smartFormFields(
            "primary" to payload.optString("feedType"),
            "secondary" to payloadNumberText(payload, "amountMl"),
            "note" to payload.optString("note")
        )
        "sleep" -> smartFormFields(
            "primary" to payload.optString("sleepStart"),
            "secondary" to payload.optString("sleepEnd"),
            "tertiary" to payload.optString("sleepPlace"),
            "note" to payload.optString("note")
        )
        "diaper" -> smartFormFields(
            "primary" to payload.optString("diaperType"),
            "secondary" to payload.optString("diaperDetail"),
            "note" to payload.optString("note")
        )
        "temperature" -> smartFormFields(
            "primary" to payloadNumberText(payload, "temperatureC"),
            "secondary" to payload.optString("measureMethod"),
            "note" to payload.optString("note")
        )
        "medication" -> smartFormFields(
            "primary" to payload.optString("medicationName"),
            "secondary" to payload.optString("dosage"),
            "tertiary" to payload.optString("reason")
        )
        else -> smartFormFields(
            "primary" to payload.optString("detail"),
            "secondary" to payload.optString("note")
        )
    }
    return SmartEntryDraft(values = values)
}

private fun draftFromPregnancyEvent(event: BabyLogDomain.BabyLogEvent): SmartEntryDraft {
    val payload = event.payload
    if (event.eventType == "fetal_movement") {
        return SmartEntryDraft(
            values = smartFormFields(
                "primary" to payload.optString("movementWindow").ifBlank {
                    sessionWindowDraft(payload.optString("startedAt"), payload.optString("endedAt"))
                },
                "secondary" to payloadNumberText(payload, "movementCount"),
                "note" to payload.optString("note")
            )
        )
    }
    if (event.eventType == "contraction") {
        return SmartEntryDraft(
            values = smartFormFields(
                "primary" to payload.optString("contractionStart").ifBlank { BabyLogFormatters.formatEventTime(event.occurredAt).takeUnless { it == "--:--" } },
                "secondary" to payloadNumberText(payload, "intervalMinutes").ifBlank { intervalMinutesDraft(payload) },
                "tertiary" to payloadNumberText(payload, "durationSeconds").ifBlank { payloadNumberText(payload, "durationSec") },
                "note" to payload.optString("note")
            )
        )
    }
    if (BabyLogService.isScreeningEventType(event.eventType)) {
        return SmartEntryDraft(
            values = smartFormFields(
                "primary" to payload.optString("screeningDate").ifBlank { BabyLogFormatters.recordDay(event.occurredAt) },
                "gestationalAge" to gestationalAgeDraftValue(payload),
                "ntMm" to payloadNumberText(payload, "ntMm"),
                "riskT21" to payload.optString("riskT21"),
                "riskT18" to payload.optString("riskT18"),
                "riskOntd" to payload.optString("riskOntd"),
                "riskLevel" to payload.optString("riskLevel"),
                "t21Result" to payload.optString("t21Result"),
                "t18Result" to payload.optString("t18Result"),
                "t13Result" to payload.optString("t13Result"),
                "sexChromosome" to payload.optString("sexChromosome"),
                "structureConclusion" to payload.optString("structureConclusion"),
                "fastingGlucoseMmolL" to payloadNumberText(payload, "fastingGlucoseMmolL"),
                "oneHourGlucoseMmolL" to payloadNumberText(payload, "oneHourGlucoseMmolL"),
                "twoHourGlucoseMmolL" to payloadNumberText(payload, "twoHourGlucoseMmolL"),
                "abnormalFlag" to payload.optString("abnormalFlag"),
                "gbsResult" to payload.optString("gbsResult"),
                "nstResult" to payload.optString("nstResult"),
                "conclusion" to payload.optString("conclusion"),
                "attachmentNote" to payload.optString("attachmentNote"),
                "note" to payload.optString("note")
            )
        )
    }
    return SmartEntryDraft(
        values = smartFormFields(
            "primary" to payload.optString("checkupDate").ifBlank { BabyLogFormatters.recordDay(event.occurredAt) },
            "gestationalAge" to gestationalAgeDraftValue(payload),
            "secondary" to payload.optString("provider"),
            "department" to payload.optString("department"),
            "systolicBp" to payloadNumberText(payload, "systolicBp"),
            "diastolicBp" to payloadNumberText(payload, "diastolicBp"),
            "weightKg" to payloadNumberText(payload, "weightKg"),
            "fundalHeightCm" to payloadNumberText(payload, "fundalHeightCm"),
            "abdominalCircumferenceCm" to payloadNumberText(payload, "abdominalCircumferenceCm"),
            "fetalHeartRateBpm" to payloadNumberText(payload, "fetalHeartRateBpm"),
            "fetalPresentation" to payload.optString("fetalPresentation"),
            "edema" to payload.optString("edema"),
            "urineRoutine" to payload.optString("urineRoutine"),
            "urineProtein" to payload.optString("urineProtein"),
            "hemoglobinGL" to payloadNumberText(payload, "hemoglobinGL"),
            "highRiskFactors" to payload.optString("highRiskFactors"),
            "tertiary" to payload.optString("doctorConclusion").ifBlank { payload.optString("finding") },
            "treatmentAdvice" to payload.optString("treatmentAdvice"),
            "nextVisitDate" to payload.optString("nextVisitDate").ifBlank { extractDateInput(payload.optString("nextVisitNote")) ?: "" },
            "reportType" to payload.optString("reportType"),
            "attachmentNote" to payload.optString("attachmentNote"),
            "note" to payload.optString("note").ifBlank {
                payload.optString("nextVisitNote").takeUnless { BabyLogFormatters.isValidDateInput(it) }.orEmpty()
            }
        )
    )
}

private fun sessionWindowDraft(startedAt: String, endedAt: String): String {
    val start = BabyLogFormatters.formatEventTime(startedAt)
    val end = BabyLogFormatters.formatEventTime(endedAt)
    return when {
        start != "--:--" && end != "--:--" -> "$start-$end"
        start != "--:--" -> start
        end != "--:--" -> end
        else -> ""
    }
}

private fun draftFromMaternalMetricEvent(event: BabyLogDomain.BabyLogEvent): SmartEntryDraft {
    val payload = event.payload
    return SmartEntryDraft(
        values = smartFormFields(
            "weightKg" to payloadNumberText(payload, "weightKg"),
            "systolicBp" to payloadNumberText(payload, "systolicBp"),
            "diastolicBp" to payloadNumberText(payload, "diastolicBp"),
            "glucoseMmolL" to payloadNumberText(payload, "glucoseMmolL"),
            "glucoseContext" to payload.optString("glucoseContext"),
            "note" to payload.optString("note")
        )
    )
}

private fun draftFromUltrasoundEvent(event: BabyLogDomain.BabyLogEvent): SmartEntryDraft {
    val payload = event.payload
    return SmartEntryDraft(
        values = smartFormFields(
            "examDate" to payload.optString("examDate").ifBlank { BabyLogFormatters.recordDay(event.occurredAt) },
            "gestationalAge" to gestationalAgeDraftValue(payload),
            "hospital" to payload.optString("hospital"),
            "reportTime" to payload.optString("reportTime"),
            "diagnosisText" to payload.optString("diagnosisText"),
            "bpdMm" to payloadNumberText(payload, "bpdMm"),
            "hcMm" to payloadNumberText(payload, "hcMm"),
            "acMm" to payloadNumberText(payload, "acMm"),
            "flMm" to payloadNumberText(payload, "flMm"),
            "efwGram" to payloadNumberText(payload, "efwGram"),
            "afiCm" to payloadNumberText(payload, "afiCm"),
            "deepestPocketCm" to payloadNumberText(payload, "deepestPocketCm"),
            "placentaLocation" to payload.optString("placentaLocation"),
            "placentaGrade" to payload.optString("placentaGrade"),
            "fetalPresentation" to payload.optString("fetalPresentation"),
            "fetalHeartRateBpm" to payloadNumberText(payload, "fetalHeartRateBpm"),
            "fetalCount" to payload.optString("fetalCount"),
            "fetalMovement" to payload.optString("fetalMovement"),
            "umbilicalInsertion" to payload.optString("umbilicalInsertion"),
            "cervicalLengthMm" to payloadNumberText(payload, "cervicalLengthMm"),
            "crlMm" to payloadNumberText(payload, "crlMm"),
            "ntMm" to payloadNumberText(payload, "ntMm"),
            "umbilicalSd" to payloadNumberText(payload, "umbilicalSd"),
            "umbilicalPi" to payloadNumberText(payload, "umbilicalPi"),
            "umbilicalRi" to payloadNumberText(payload, "umbilicalRi")
        )
    )
}

private fun payloadNumberText(payload: JSONObject, key: String): String {
    if (!payload.has(key) || payload.isNull(key)) {
        return ""
    }
    return BabyLogFormatters.formatNumber(payload.optDouble(key))
}

private fun intervalMinutesDraft(payload: JSONObject): String {
    if (!payload.has("intervalFromPrevSec") || payload.isNull("intervalFromPrevSec")) {
        return ""
    }
    val seconds = payload.optDouble("intervalFromPrevSec")
    if (seconds <= 0.0) {
        return ""
    }
    return BabyLogFormatters.formatNumber(seconds / 60.0)
}

private fun gestationalAgeDraftValue(payload: JSONObject): String {
    if (!payload.has("gestationalAgeDays") || payload.isNull("gestationalAgeDays")) {
        return ""
    }
    return BabyLogFormatters.formatGestationalAge(payload.optInt("gestationalAgeDays")).removeSuffix(" 周")
}

internal fun hasAdvancedUltrasoundDraft(values: Map<String, String>): Boolean {
    val basicKeys = setOf("examDate", "gestationalAge", "bpdMm", "hcMm", "acMm", "flMm", "efwGram")
    return values.any { (key, value) -> key !in basicKeys && value.isNotBlank() }
}

internal fun normalizeGlucoseContext(value: String): String {
    val normalized = value.trim().lowercase(Locale.US)
    return when {
        normalized == "fasting" || value.contains("空腹") -> "fasting"
        normalized == "after_1h" || value.contains("1h") || value.contains("1小时") || value.contains("一小时") -> "after_1h"
        normalized == "after_2h" || value.contains("2h") || value.contains("2小时") || value.contains("两小时") -> "after_2h"
        normalized == "random" || value.contains("随机") -> "random"
        else -> "random"
    }
}

internal fun ultrasoundCandidateRows(candidate: BabyLogSmartInput.UltrasoundOcrCandidate): List<Pair<String, String>> {
    val rows = mutableListOf<Pair<String, String>>()
    addCandidateRow(rows, "检查日期", candidate.examDate.value)
    addCandidateRow(rows, "医院", candidate.hospital.value)
    addCandidateRow(rows, "报告时间", candidate.reportTime.value)
    addCandidateRow(rows, "诊断提示", candidate.diagnosisText.value)
    addCandidateRow(rows, "BPD", formatCandidateNumber(candidate.bpdMm.value, "mm"))
    addCandidateRow(rows, "HC", formatCandidateNumber(candidate.hcMm.value, "mm"))
    addCandidateRow(rows, "AC", formatCandidateNumber(candidate.acMm.value, "mm"))
    addCandidateRow(rows, "FL", formatCandidateNumber(candidate.flMm.value, "mm"))
    addCandidateRow(rows, "EFW", formatCandidateNumber(candidate.efwGram.value, "g"))
    addCandidateRow(rows, "AFI", formatCandidateNumber(candidate.afiCm.value, "cm"))
    addCandidateRow(rows, "最大羊水池", formatCandidateNumber(candidate.deepestPocketCm.value, "cm"))
    addCandidateRow(rows, "胎盘", candidate.placentaLocation.value)
    addCandidateRow(rows, "成熟度", candidate.placentaGrade.value)
    addCandidateRow(rows, "胎位", candidate.fetalPresentation.value)
    addCandidateRow(rows, "胎心率", formatCandidateNumber(candidate.fetalHeartRateBpm.value, "bpm"))
    addCandidateRow(rows, "胎儿个数", candidate.fetalCount.value)
    addCandidateRow(rows, "胎动", candidate.fetalMovement.value)
    addCandidateRow(rows, "脐带插入处", candidate.umbilicalInsertion.value)
    addCandidateRow(rows, "宫颈管", formatCandidateNumber(candidate.cervicalLengthMm.value, "mm"))
    addCandidateRow(rows, "CRL", formatCandidateNumber(candidate.crlMm.value, "mm"))
    addCandidateRow(rows, "NT", formatCandidateNumber(candidate.ntMm.value, "mm"))
    addCandidateRow(rows, "S/D", formatCandidateNumber(candidate.umbilicalSd.value, ""))
    addCandidateRow(rows, "PI", formatCandidateNumber(candidate.umbilicalPi.value, ""))
    addCandidateRow(rows, "RI", formatCandidateNumber(candidate.umbilicalRi.value, ""))
    return rows
}

private fun addCandidateRow(rows: MutableList<Pair<String, String>>, label: String, value: String?) {
    if (!value.isNullOrBlank()) {
        rows.add(label to value)
    }
}

private fun formatCandidateNumber(value: Double?, unit: String): String? {
    if (value == null) {
        return null
    }
    val number = BabyLogFormatters.formatNumber(value)
    return if (unit.isBlank()) number else "$number $unit"
}

private fun stageLabel(stage: String): String {
    return when (stage) {
        BabyLogDomain.STAGE_PREGNANCY -> "孕期"
        BabyLogDomain.STAGE_BABY -> "出生后"
        BabyLogDomain.STAGE_PREGNANCY_ENDED -> "妊娠结束"
        BabyLogDomain.STAGE_PAUSED -> "暂停"
        else -> "待补档案"
    }
}

private fun stageOverrideLabel(stageOverride: String): String {
    return when (stageOverride) {
        BabyLogDomain.STAGE_PREGNANCY -> "孕期中"
        BabyLogDomain.STAGE_BABY -> "出生后"
        BabyLogDomain.STAGE_PREGNANCY_ENDED -> "妊娠结束"
        BabyLogDomain.STAGE_PAUSED -> "暂停"
        BabyLogDomain.STAGE_UNKNOWN -> "待补档案"
        else -> "自动"
    }
}

private fun normalizeSexInput(value: String): String {
    val normalized = value.trim().lowercase(Locale.US)
    return when (normalized) {
        "女", "female", "girl" -> "female"
        "男", "male", "boy" -> "male"
        else -> "unknown"
    }
}

private fun normalizeStageInput(value: String): String {
    val normalized = value.trim().lowercase(Locale.US)
    return when (normalized) {
        "孕期", "孕期中", BabyLogDomain.STAGE_PREGNANCY -> BabyLogDomain.STAGE_PREGNANCY
        "出生后", "育儿", BabyLogDomain.STAGE_BABY -> BabyLogDomain.STAGE_BABY
        "妊娠结束", BabyLogDomain.STAGE_PREGNANCY_ENDED -> BabyLogDomain.STAGE_PREGNANCY_ENDED
        "暂停", BabyLogDomain.STAGE_PAUSED -> BabyLogDomain.STAGE_PAUSED
        "未知", BabyLogDomain.STAGE_UNKNOWN -> BabyLogDomain.STAGE_UNKNOWN
        else -> BabyLogDomain.STAGE_AUTO
    }
}

private fun parsePositiveProfileNumber(value: String): OptionalProfileNumber? {
    if (value.trim().isEmpty()) {
        return OptionalProfileNumber(null)
    }
    val parsed = BabyLogFormatters.parseOptionalNumber(value)
    if (parsed == null || parsed <= 0.0) {
        return null
    }
    return OptionalProfileNumber(parsed)
}

private fun isValidReminderTimeInput(value: String): Boolean {
    if (!value.matches(Regex("\\d{2}:\\d{2}"))) {
        return false
    }
    val hour = value.substring(0, 2).toIntOrNull() ?: return false
    val minute = value.substring(3, 5).toIntOrNull() ?: return false
    return hour in 0..23 && minute in 0..59
}

internal fun isEventVisibleInHome(event: BabyLogDomain.BabyLogEvent, stage: String): Boolean {
    val group = BabyLogFormatters.timelineFilterGroup(event.eventType)
    return when (stage) {
        BabyLogDomain.STAGE_PREGNANCY -> group == "pregnancy" || group == "ultrasound" || group == "checkup"
        BabyLogDomain.STAGE_BABY -> group == "baby" || group == "temperature"
        BabyLogDomain.STAGE_PREGNANCY_ENDED,
        BabyLogDomain.STAGE_PAUSED -> group == "pregnancy" || group == "ultrasound" || group == "checkup"
        else -> false
    }
}

private fun daysBetween(fromDate: String, toDate: String): Int {
    return BabyLogFormatters.daysBetweenDateInputs(fromDate, toDate)
}

private fun extractDateInput(text: String?): String? {
    if (text.isNullOrBlank()) {
        return null
    }
    val match = Regex("\\d{4}-\\d{2}-\\d{2}").find(text) ?: return null
    val value = match.value
    return if (BabyLogFormatters.isValidDateInput(value)) value else null
}

private fun eventTone(eventType: String): Color {
    return when (BabyLogFormatters.timelineFilterGroup(eventType)) {
        "pregnancy" -> ChestnutPalette.Accent
        "ultrasound" -> ChestnutPalette.Rose
        "temperature" -> ChestnutPalette.Green
        "checkup" -> ChestnutPalette.Violet
        "baby" -> ChestnutPalette.Peach
        else -> ChestnutPalette.Blue
    }
}

private fun attachmentCount(attachments: List<BabyLogDomain.AttachmentRecord>): String {
    return if (attachments.isEmpty()) "0 张" else "${attachments.size} 张"
}

private fun latestEfwValue(events: List<BabyLogDomain.BabyLogEvent>): String {
    val event = events.firstOrNull { it.eventType == "ultrasound" } ?: return "暂无"
    val efw = payloadNumber(event.payload, "efwGram")
    return if (efw == null) "待补" else "${BabyLogFormatters.formatNumber(efw)} g"
}

private fun latestBpdFlValue(events: List<BabyLogDomain.BabyLogEvent>): String {
    val event = events.firstOrNull { it.eventType == "ultrasound" } ?: return "暂无"
    val bpd = payloadNumber(event.payload, "bpdMm")
    val fl = payloadNumber(event.payload, "flMm")
    if (bpd == null && fl == null) {
        return "待补"
    }
    val bpdText = bpd?.let { "BPD ${BabyLogFormatters.formatNumber(it)}" }
    val flText = fl?.let { "FL ${BabyLogFormatters.formatNumber(it)}" }
    return listOfNotNull(bpdText, flText).joinToString(" / ")
}

private fun latestUltrasoundCaption(events: List<BabyLogDomain.BabyLogEvent>): String {
    val event = events.firstOrNull { it.eventType == "ultrasound" } ?: return "保存 B 超后显示"
    return BabyLogFormatters.formatEventDay(event.occurredAt)
}

private fun ultrasoundWarningText(event: BabyLogDomain.BabyLogEvent): String {
    return BabyLogFormatters.formatUltrasoundSoftRangeWarnings(
        if (event.payload.has("gestationalAgeDays")) event.payload.optInt("gestationalAgeDays") else null,
        payloadNumber(event.payload, "bpdMm"),
        payloadNumber(event.payload, "hcMm"),
        payloadNumber(event.payload, "acMm"),
        payloadNumber(event.payload, "flMm"),
        payloadNumber(event.payload, "efwGram")
    )
}

private fun payloadNumber(payload: JSONObject, key: String): Double? {
    return if (payload.has(key)) BabyLogFormatters.parseOptionalNumber(payload.optString(key, "")) else null
}

private fun tabTitle(activeTab: String): String {
    return when (activeTab) {
        BabyLogRoutes.Timeline -> "时间线"
        BabyLogRoutes.Library -> "资料"
        BabyLogRoutes.Settings -> "设置"
        else -> "首页"
    }
}
