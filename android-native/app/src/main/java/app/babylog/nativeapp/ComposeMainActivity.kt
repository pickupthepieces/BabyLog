package app.babylog.nativeapp

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.material.Checkbox
import androidx.compose.material.Divider
import androidx.compose.material.FloatingActionButton
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
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Article
import androidx.compose.material.icons.rounded.Assessment
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Checklist
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FormatListBulleted
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LocalDrink
import androidx.compose.material.icons.rounded.MedicalInformation
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import org.json.JSONException
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

public final class ComposeMainActivity : ComponentActivity() {
    private lateinit var repository: BabyLogRepository
    private lateinit var service: BabyLogService
    private lateinit var smartConfigStore: BabyLogSmartConfigStore
    private val smartVisionClient = BabyLogSmartVisionClient()
    private val smartTextClient = BabyLogSmartTextClient()
    private lateinit var cameraPermissionLauncher: ActivityResultLauncher<String>
    private lateinit var cameraLauncher: ActivityResultLauncher<Uri>
    private lateinit var pickImageLauncher: ActivityResultLauncher<String>
    private lateinit var exportBackupLauncher: ActivityResultLauncher<String>
    private lateinit var importBackupLauncher: ActivityResultLauncher<Array<String>>

    private var uiState by mutableStateOf(BabyLogUiState())
    private var activeTab by mutableStateOf("home")
    private var timelineFilter by mutableStateOf("all")
    private var selectedBabyDay by mutableStateOf(BabyLogFormatters.todayDateInput())
    private var showQuickSheet by mutableStateOf(false)
    private var babyCareAction by mutableStateOf<BabyLogService.QuickAction?>(null)
    private var pregnancyAction by mutableStateOf<BabyLogService.QuickAction?>(null)
    private var showFetalMovementSession by mutableStateOf(false)
    private var showMaternalMetricForm by mutableStateOf(false)
    private var showUltrasoundForm by mutableStateOf(false)
    private var showSyncSettings by mutableStateOf(false)
    private var smartSettingsConfig by mutableStateOf<BabyLogSmartConfigStore.Config?>(null)
    private var smartConfigSummary by mutableStateOf("智能识别未配置")
    private var ultrasoundOcrRunning by mutableStateOf(false)
    private var showSmartEntryDialog by mutableStateOf(false)
    private var smartEntryRunning by mutableStateOf(false)
    private var babyCareDraft by mutableStateOf<SmartEntryDraft?>(null)
    private var pregnancyDraft by mutableStateOf<SmartEntryDraft?>(null)
    private var maternalMetricDraft by mutableStateOf<SmartEntryDraft?>(null)
    private var ultrasoundDraft by mutableStateOf<SmartEntryDraft?>(null)
    private var ultrasoundOcrCandidate by mutableStateOf<BabyLogSmartInput.UltrasoundOcrCandidate?>(null)
    private var showClearLocalConfirm by mutableStateOf(false)
    private var showTrashDialog by mutableStateOf(false)
    private var profileDialog by mutableStateOf<ProfileDialogState?>(null)
    private var importConfirm by mutableStateOf<ImportConfirmState?>(null)
    private var syncConfirmUrl by mutableStateOf<String?>(null)
    private var attachmentDialog by mutableStateOf<AttachmentDialogState?>(null)
    private var previewAttachment by mutableStateOf<BabyLogDomain.AttachmentRecord?>(null)
    private var deleteEventConfirm by mutableStateOf<BabyLogDomain.BabyLogEvent?>(null)
    private var infoDialog by mutableStateOf<InfoDialogState?>(null)
    private var pendingCameraFile: File? = null
    private var pendingUltrasoundPhotoPath by mutableStateOf<String?>(null)
    private var pendingUltrasoundPhotoName by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.AppTheme)
        super.onCreate(savedInstanceState)
        window.statusBarColor = ChestnutPalette.PrimaryArgb
        window.navigationBarColor = ChestnutPalette.PrimaryArgb

        repository = BabyLogRepository(this)
        service = BabyLogService(this, repository)
        smartConfigStore = BabyLogSmartConfigStore(this)
        registerLaunchers()
        refreshSmartConfigSummary()

        setContent {
            ChestnutTheme {
                BabyLogApp(
                    state = uiState,
                    activeTab = activeTab,
                    timelineFilter = timelineFilter,
                    selectedBabyDay = selectedBabyDay,
                    onTabSelected = { activeTab = it },
                    onTimelineFilterSelected = { timelineFilter = it },
                    onBabyDaySelected = { selectedBabyDay = it },
                    onQuickClick = { showQuickSheet = true },
                    quickActions = quickActions(),
                    onQuickAction = ::handleQuickAction,
                    onShowAttachments = { title, attachments ->
                        attachmentDialog = AttachmentDialogState(title, attachments)
                    },
                    onSyncNow = ::syncNow,
                    onExportBackup = ::exportBackup,
                    onImportBackup = ::importBackup,
                    onOpenSyncSettings = { showSyncSettings = true },
                    onOpenSmartSettings = ::openSmartSettings,
                    smartConfigSummary = smartConfigSummary,
                    onClearLocalData = { showClearLocalConfirm = true },
                    onOpenTrash = { showTrashDialog = true },
                    onCreatePregnancyProfile = { openNewFamilyForm(BabyLogDomain.STAGE_PREGNANCY) },
                    onCreateBabyProfile = { openNewFamilyForm(BabyLogDomain.STAGE_BABY) },
                    onEditProfile = { openProfileEditDialog() },
                    onDeleteEvent = { deleteEventConfirm = it }
                )

                if (showQuickSheet) {
                    QuickActionDialog(
                        actions = quickActions(),
                        onDismiss = { showQuickSheet = false },
                        onSmartEntry = {
                            showQuickSheet = false
                            window.decorView.postDelayed({
                                if (!isFinishing && !isDestroyed) {
                                    showSmartEntryDialog = true
                                }
                            }, 120L)
                        },
                        onAction = { action ->
                            showQuickSheet = false
                            handleQuickActionAfterQuickSheetDismiss(action)
                        }
                    )
                }

                if (showSmartEntryDialog) {
                    SmartEntryDialog(
                        running = smartEntryRunning,
                        onDismiss = {
                            if (!smartEntryRunning) {
                                showSmartEntryDialog = false
                            }
                        },
                        onSubmit = ::requestSmartEntry
                    )
                }

                babyCareAction?.let { action ->
                    BabyCareDialog(
                        action = action,
                        draft = babyCareDraft,
                        onDismiss = {
                            babyCareAction = null
                            babyCareDraft = null
                        },
                        onSave = { input ->
                            babyCareAction = null
                            babyCareDraft = null
                            recordBabyCare(input)
                        }
                    )
                }

                pregnancyAction?.let { action ->
                    PregnancyEventDialog(
                        action = action,
                        draft = pregnancyDraft,
                        onDismiss = {
                            pregnancyAction = null
                            pregnancyDraft = null
                        },
                        onSave = { input ->
                            pregnancyAction = null
                            pregnancyDraft = null
                            recordPregnancy(input)
                        }
                    )
                }

                if (showFetalMovementSession) {
                    FetalMovementSessionDialog(
                        onDismiss = { showFetalMovementSession = false },
                        onSave = { input ->
                            showFetalMovementSession = false
                            recordFetalMovementSession(input)
                        }
                    )
                }

                if (showMaternalMetricForm) {
                    MaternalMetricDialog(
                        draft = maternalMetricDraft,
                        onDismiss = {
                            showMaternalMetricForm = false
                            maternalMetricDraft = null
                        },
                        onSave = { input ->
                            showMaternalMetricForm = false
                            maternalMetricDraft = null
                            recordMaternalMetric(input)
                        }
                    )
                }

                if (showUltrasoundForm) {
                    UltrasoundDialog(
                        defaultGestationalAge = currentGestationalAgeInput(uiState.childProfile),
                        expectedDueDate = uiState.childProfile.expectedDueDate,
                        draft = ultrasoundDraft,
                        photoPath = pendingUltrasoundPhotoPath,
                        photoName = pendingUltrasoundPhotoName,
                        ocrRunning = ultrasoundOcrRunning,
                        ocrCandidate = ultrasoundOcrCandidate,
                        onPickPhoto = ::pickImage,
                        onCapturePhoto = ::requestCameraOrLaunch,
                        onRecognizePhoto = ::recognizeUltrasoundPhoto,
                        onCandidateDismiss = { ultrasoundOcrCandidate = null },
                        onCandidateApplied = {
                            ultrasoundOcrCandidate = null
                            showToast("已应用识别字段，请核对后保存")
                        },
                        onDismiss = {
                            showUltrasoundForm = false
                            ultrasoundDraft = null
                            ultrasoundOcrCandidate = null
                            ultrasoundOcrRunning = false
                        },
                        onSave = { input ->
                            showUltrasoundForm = false
                            ultrasoundDraft = null
                            ultrasoundOcrCandidate = null
                            recordUltrasound(input)
                        }
                    )
                }

                if (showSyncSettings) {
                    SyncSettingsDialog(
                        config = uiState.syncConfig,
                        onDismiss = { showSyncSettings = false },
                        onSave = { backendBaseUrl ->
                            showSyncSettings = false
                            saveSyncSettings(backendBaseUrl)
                        }
                    )
                }

                smartSettingsConfig?.let { config ->
                    SmartModelSettingsDialog(
                        config = config,
                        onDismiss = { smartSettingsConfig = null },
                        onSave = { next ->
                            smartSettingsConfig = null
                            saveSmartSettings(next)
                        }
                    )
                }

                profileDialog?.let { dialog ->
                    ProfileDialog(
                        state = dialog,
                        onDismiss = { profileDialog = null },
                        onSave = { input ->
                            saveProfile(input, dialog.firstRun)
                            profileDialog = null
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

                syncConfirmUrl?.let { backendBaseUrl ->
                    ConfirmDialog(
                        title = "确认启用同步",
                        message = "启用后会按你配置的地址尝试上传；当前后端仍未就绪，记录会保留在本机待同步队列中。请确认服务器地址、地域和医疗数据跨设备风险都已知晓。",
                        confirmText = "我已知晓并保存",
                        destructive = false,
                        onDismiss = { syncConfirmUrl = null },
                        onConfirm = {
                            syncConfirmUrl = null
                            persistSyncSettings(backendBaseUrl)
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

                if (showTrashDialog) {
                    TrashDialog(
                        events = uiState.trashEvents,
                        onDismiss = { showTrashDialog = false },
                        onRestore = { event ->
                            restoreEvent(event.id)
                        }
                    )
                }

                attachmentDialog?.let { dialog ->
                    AttachmentListDialog(
                        title = dialog.title,
                        attachments = dialog.attachments,
                        onDismiss = { attachmentDialog = null },
                        onPreview = { previewAttachment = it }
                    )
                }

                previewAttachment?.let { attachment ->
                    AttachmentPreviewDialog(
                        attachment = attachment,
                        onDismiss = { previewAttachment = null }
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

    private fun registerLaunchers() {
        cameraPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                startCameraCapture()
            } else {
                showToast("没有相机权限，无法拍照")
            }
        }
        cameraLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            val captured = pendingCameraFile
            pendingCameraFile = null
            if (!success || captured == null) {
                captured?.delete()
                return@registerForActivityResult
            }
            runInBackground {
                try {
                    val compressedPath = service.compressImageFileToPrivateFile(captured, "ultrasound.jpg")
                    captured.delete()
                    runOnUiThread {
                        pendingUltrasoundPhotoPath = compressedPath
                        pendingUltrasoundPhotoName = File(compressedPath).name
                        ultrasoundOcrCandidate = null
                    }
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
    }

    private fun reloadData() {
        runInBackground {
            service.purgeExpiredTrash()
            val nextState = BabyLogUiState(
                dashboard = service.loadDashboard(),
                timeline = service.listRecentEvents(100),
                trashEvents = service.listTrashEvents(),
                attachments = service.listAttachmentsNewestFirst(),
                syncConfig = repository.loadSyncSettings(),
                childProfile = repository.loadChildProfile(),
                setupCompleted = repository.hasCompletedSetup(),
                lastBackupExportMs = getSharedPreferences(META_PREFS_NAME, MODE_PRIVATE)
                    .getLong(LAST_BACKUP_EXPORT_MS, 0L)
            )
            runOnUiThread { uiState = nextState }
        }
    }

    private fun refreshSmartConfigSummary() {
        smartConfigSummary = if (smartConfigStore.isConfigured()) {
            "已配置；可用于 B 超 OCR 和全局智能录入"
        } else {
            "未配置；Key 仅保存在本机，不同步不备份"
        }
    }

    private fun openSmartSettings() {
        runInBackground {
            try {
                val config = smartConfigStore.load()
                runOnUiThread { smartSettingsConfig = config }
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
                runOnUiThread { refreshSmartConfigSummary() }
                showToast(if (config.isConfigured()) "已保存智能识别配置" else "已关闭智能识别")
            } catch (error: Exception) {
                showInfo("保存失败", error.message ?: "无法保存智能识别配置")
            }
        }
    }

    private fun openUltrasoundForm(draft: SmartEntryDraft? = null) {
        ultrasoundDraft = draft
        pendingUltrasoundPhotoPath = null
        pendingUltrasoundPhotoName = null
        ultrasoundOcrCandidate = null
        ultrasoundOcrRunning = false
        showUltrasoundForm = true
    }

    private fun recordQuickAction(action: BabyLogService.QuickAction) {
        runInBackground {
            try {
                service.recordQuickEvent(action)
                showToast("已记录：${action.label}")
                reloadData()
            } catch (error: JSONException) {
                showInfo("保存失败", error.message ?: "无法保存记录")
            }
        }
    }

    private fun handleQuickAction(action: BabyLogService.QuickAction) {
        if (isBabyCareAction(action.eventType)) {
            babyCareDraft = null
            babyCareAction = action
        } else if (action.eventType == "fetal_movement") {
            showFetalMovementSession = true
        } else if (isPregnancyFormAction(action.eventType)) {
            pregnancyDraft = null
            pregnancyAction = action
        } else if (action.eventType == "maternal_metric") {
            maternalMetricDraft = null
            showMaternalMetricForm = true
        } else if (action.eventType == "ultrasound") {
            openUltrasoundForm()
        } else {
            recordQuickAction(action)
        }
    }

    private fun handleQuickActionAfterQuickSheetDismiss(action: BabyLogService.QuickAction) {
        window.decorView.postDelayed({
            if (!isFinishing && !isDestroyed) {
                handleQuickAction(action)
            }
        }, 120L)
    }

    private fun recordBabyCare(input: BabyLogService.BabyCareInput) {
        runInBackground {
            try {
                service.recordBabyCareEvent(input)
                showToast("已保存记录")
                reloadData()
            } catch (error: JSONException) {
                showInfo("保存失败", error.message ?: "无法保存记录")
            }
        }
    }

    private fun recordPregnancy(input: BabyLogService.PregnancyInput) {
        runInBackground {
            try {
                service.recordPregnancyEvent(input)
                showToast("已保存记录")
                reloadData()
            } catch (error: JSONException) {
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

    private fun recordMaternalMetric(input: BabyLogService.MaternalMetricInput) {
        runInBackground {
            try {
                service.recordMaternalMetric(input)
                showToast("已保存孕妈指标")
                reloadData()
            } catch (error: JSONException) {
                showInfo("保存失败", error.message ?: "无法保存孕妈指标")
            }
        }
    }

    private fun recordUltrasound(input: BabyLogService.UltrasoundInput) {
        runInBackground {
            try {
                service.recordUltrasound(input)
                runOnUiThread {
                    pendingUltrasoundPhotoPath = null
                    pendingUltrasoundPhotoName = null
                    ultrasoundOcrCandidate = null
                }
                showToast("已保存 B 超记录")
                reloadData()
            } catch (error: JSONException) {
                showInfo("保存失败", error.message ?: "无法保存 B 超记录")
            }
        }
    }

    private fun deleteEvent(eventId: String) {
        runInBackground {
            try {
                service.deleteEvent(eventId)
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

    private fun pickImage() {
        pickImageLauncher.launch("image/*")
    }

    private fun requestCameraOrLaunch() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startCameraCapture()
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun startCameraCapture() {
        try {
            val file = service.createCameraCaptureFile("ultrasound.jpg")
            pendingCameraFile = file
            val uri = BabyLogFileProvider.getUriForFile(this, file)
            cameraLauncher.launch(uri)
        } catch (error: IOException) {
            showInfo("无法打开相机", error.message ?: "无法创建拍照文件")
        }
    }

    private fun copyPickedImage(uri: Uri) {
        runInBackground {
            try {
                val path = service.copyImageUriToPrivateFile(uri, "ultrasound.jpg")
                runOnUiThread {
                    pendingUltrasoundPhotoPath = path
                    pendingUltrasoundPhotoName = File(path).name
                    ultrasoundOcrCandidate = null
                }
            } catch (error: IOException) {
                showInfo("导入图片失败", error.message ?: "无法读取图片")
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
            showInfo("先输入内容", "可以先用系统键盘语音输入一段话，再点智能录入。")
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
                runOnUiThread {
                    openSmartEntryCandidate(candidate)
                }
            } catch (error: Exception) {
                showInfo("智能录入失败", smartVisionErrorMessage(error))
            } finally {
                runOnUiThread { smartEntryRunning = false }
            }
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
                showSmartEntryDialog = false
                openUltrasoundForm(draft)
            }
            "pregnancy_checkup", "contraction" -> {
                val action = quickActionByEventType(candidate.eventType)
                if (action == null) {
                    showInfo("暂不支持", "当前阶段没有 ${candidate.eventType} 表单。")
                    return
                }
                showSmartEntryDialog = false
                pregnancyDraft = draft
                pregnancyAction = action
            }
            "maternal_metric" -> {
                showSmartEntryDialog = false
                maternalMetricDraft = draft
                showMaternalMetricForm = true
            }
            "feed", "sleep", "diaper", "temperature", "medication" -> {
                val action = quickActionByEventType(candidate.eventType)
                if (action == null) {
                    showInfo("暂不支持", "当前阶段没有 ${candidate.eventType} 表单。")
                    return
                }
                showSmartEntryDialog = false
                babyCareDraft = draft
                babyCareAction = action
            }
            else -> {
                showInfo("暂不支持", "已识别为 ${candidate.eventType}，但还没有对应的确认表单。")
                return
            }
        }
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
                runOnUiThread { activeTab = "home" }
                showToast("导入完成：$count 条记录")
                reloadData()
            } catch (error: Exception) {
                showInfo("导入失败", error.message ?: "无法导入备份")
            }
        }
    }

    private fun syncNow() {
        runInBackground {
            try {
                val result = service.runSyncNow()
                showToast(if (result.ok) "同步队列为空" else "同步未完成：${formatSyncError(result.code)}")
                reloadData()
            } catch (error: JSONException) {
                showInfo("同步失败", error.message ?: "同步队列更新失败")
            }
        }
    }

    private fun saveSyncSettings(backendBaseUrl: String) {
        val normalized = BabyLogFormatters.normalizeBackendBaseUrl(backendBaseUrl)
        if (normalized.isNotEmpty()) {
            syncConfirmUrl = normalized
            return
        }
        persistSyncSettings(normalized)
    }

    private fun persistSyncSettings(backendBaseUrl: String) {
        runInBackground {
            try {
                repository.saveSyncSettings(BabyLogDomain.BackendConfig(backendBaseUrl.isNotEmpty(), backendBaseUrl, "cn", null))
                showToast(if (backendBaseUrl.isEmpty()) "已关闭后端同步" else "已保存同步地址")
                reloadData()
            } catch (error: JSONException) {
                showInfo("保存失败", error.message ?: "无法保存同步设置")
            }
        }
    }

    private fun clearLocalData() {
        runInBackground {
            service.clearLocalData()
            runOnUiThread { activeTab = "home" }
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
                "secondary" to "医院 / 机构",
                "tertiary" to "结论 / 医嘱摘要",
                "note" to "下次复诊 / 备注"
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
            forms["sleep"] = smartFormFields(
                "primary" to "开始时间",
                "secondary" to "结束时间",
                "tertiary" to "时长",
                "note" to "备注"
            )
        }
        return forms
    }

    private fun openNewFamilyForm(stage: String) {
        val profile = BabyLogDomain.ChildProfile.createForNewFamily("", "unknown", "", "", stage, true)
        profileDialog = ProfileDialogState(
            title = if (stage == BabyLogDomain.STAGE_PREGNANCY) "新建孕期家庭" else "新建出生后家庭",
            profile = profile,
            firstRun = true,
            initialStage = stage
        )
    }

    private fun openProfileEditDialog() {
        profileDialog = ProfileDialogState(
            title = "编辑宝宝档案",
            profile = uiState.childProfile,
            firstRun = false,
            initialStage = uiState.childProfile.stageOverride
        )
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
        runInBackground {
            try {
                val child = BabyLogDomain.ChildProfile.createForNewFamily(
                    input.nickname,
                    normalizeSexInput(input.sex),
                    input.expectedDueDate,
                    input.birthDate,
                    normalizeStageInput(input.stageOverride),
                    true
                )
                if (firstRun) {
                    repository.saveProfileBundle(
                        BabyLogDomain.FamilyProfile.localDefault(),
                        child,
                        BabyLogDomain.FamilyMember.localManager()
                    )
                } else {
                    repository.saveChildProfile(child)
                }
                runOnUiThread { activeTab = "home" }
                showToast("档案已保存")
                reloadData()
            } catch (error: JSONException) {
                showInfo("保存失败", error.message ?: "无法保存档案")
            }
        }
    }

    private fun quickActionsForStage(stage: String): List<BabyLogService.QuickAction> {
        if (stage == BabyLogDomain.STAGE_PREGNANCY) {
            return listOf(
                BabyLogService.QuickAction("B超", "指标 / 照片 / 识别", ChestnutPalette.RoseArgb, "ultrasound"),
                BabyLogService.QuickAction("产检", "日期 / 医院 / 结论", ChestnutPalette.VioletArgb, "pregnancy_checkup"),
                BabyLogService.QuickAction("胎动", "会话计数 / 10 次目标", ChestnutPalette.GreenArgb, "fetal_movement"),
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
            else -> code ?: "未知错误"
        }
    }

    private companion object {
        private const val META_PREFS_NAME = "babylog_native_meta_v1"
        private const val LAST_BACKUP_EXPORT_MS = "lastBackupExportMs"
    }
}

private data class BabyLogUiState(
    val dashboard: BabyLogService.DashboardSnapshot? = null,
    val timeline: List<BabyLogDomain.BabyLogEvent> = emptyList(),
    val trashEvents: List<BabyLogDomain.BabyLogEvent> = emptyList(),
    val attachments: List<BabyLogDomain.AttachmentRecord> = emptyList(),
    val syncConfig: BabyLogDomain.BackendConfig = BabyLogDomain.BackendConfig.disabled(),
    val childProfile: BabyLogDomain.ChildProfile = BabyLogDomain.ChildProfile.empty(),
    val setupCompleted: Boolean = false,
    val lastBackupExportMs: Long = 0L
)

private data class ProfileDialogState(
    val title: String,
    val profile: BabyLogDomain.ChildProfile,
    val firstRun: Boolean,
    val initialStage: String
)

private data class ProfileInput(
    val nickname: String,
    val sex: String,
    val expectedDueDate: String,
    val birthDate: String,
    val stageOverride: String
)

private data class ImportPreview(
    val eventCount: Int,
    val profileLabel: String
)

private data class ImportConfirmState(
    val raw: String,
    val eventCount: Int,
    val profileLabel: String
)

private data class AttachmentDialogState(
    val title: String,
    val attachments: List<BabyLogDomain.AttachmentRecord>
)

private data class InfoDialogState(
    val title: String,
    val message: String
)

private data class SmartEntryDraft(
    val nonce: Long = System.nanoTime(),
    val values: Map<String, String> = emptyMap()
)

@Composable
private fun BabyLogApp(
    state: BabyLogUiState,
    activeTab: String,
    timelineFilter: String,
    selectedBabyDay: String,
    onTabSelected: (String) -> Unit,
    onTimelineFilterSelected: (String) -> Unit,
    onBabyDaySelected: (String) -> Unit,
    onQuickClick: () -> Unit,
    quickActions: List<BabyLogService.QuickAction>,
    onQuickAction: (BabyLogService.QuickAction) -> Unit,
    onShowAttachments: (String, List<BabyLogDomain.AttachmentRecord>) -> Unit,
    onSyncNow: () -> Unit,
    onExportBackup: () -> Unit,
    onImportBackup: () -> Unit,
    onOpenSyncSettings: () -> Unit,
    onOpenSmartSettings: () -> Unit,
    smartConfigSummary: String,
    onClearLocalData: () -> Unit,
    onOpenTrash: () -> Unit,
    onCreatePregnancyProfile: () -> Unit,
    onCreateBabyProfile: () -> Unit,
    onEditProfile: () -> Unit,
    onDeleteEvent: (BabyLogDomain.BabyLogEvent) -> Unit
) {
    Scaffold(
        backgroundColor = ChestnutPalette.Bg,
        topBar = {
            TopBrandBand(activeTab = activeTab, state = state)
        },
        floatingActionButton = {
            if (state.setupCompleted && quickActions.isNotEmpty()) {
                FloatingActionButton(
                    onClick = onQuickClick,
                    backgroundColor = ChestnutPalette.Surface,
                    contentColor = ChestnutPalette.Primary
                ) {
                    BabyLogMaterialIcon(
                        icon = LineIcon.Plus,
                        tint = ChestnutPalette.Primary,
                        modifier = Modifier.size(34.dp)
                    )
                }
            }
        },
        bottomBar = {
            if (state.setupCompleted) {
                Column {
                    if (activeTab == "home" && currentCareStage(state.childProfile) == BabyLogDomain.STAGE_BABY) {
                        BabyQuickRail(actions = quickActions, onAction = onQuickAction)
                    }
                    BottomNav(activeTab = activeTab, onTabSelected = onTabSelected)
                }
            }
        }
    ) { inner ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(ChestnutPalette.Bg, Color(0xFFFFFBF4), ChestnutPalette.Bg)
                    )
                ),
            contentPadding = PaddingValues(
                start = 18.dp,
                top = inner.calculateTopPadding() + 16.dp,
                end = 18.dp,
                bottom = inner.calculateBottomPadding() + 22.dp
            ),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (!state.setupCompleted) {
                item {
                    FirstRunScreen(
                        onCreatePregnancyProfile = onCreatePregnancyProfile,
                        onCreateBabyProfile = onCreateBabyProfile,
                        onImportBackup = onImportBackup
                    )
                }
            } else {
                val stage = currentCareStage(state.childProfile)
                when (activeTab) {
                "home" -> {
                    item {
                        if (stage == BabyLogDomain.STAGE_BABY) {
                            BabyDayCard(
                                profile = state.childProfile,
                                selectedDay = selectedBabyDay,
                                onPreviousDay = {
                                    onBabyDaySelected(BabyLogFormatters.offsetDateInput(selectedBabyDay, -1))
                                },
                                onToday = { onBabyDaySelected(BabyLogFormatters.todayDateInput()) },
                                onNextDay = {
                                    onBabyDaySelected(BabyLogFormatters.offsetDateInput(selectedBabyDay, 1))
                                }
                            )
                        } else {
                            WeekCard(state.childProfile)
                        }
                    }
                    if (stage == BabyLogDomain.STAGE_BABY) {
                        val dayEvents = state.timeline.filter {
                            BabyLogFormatters.recordDay(it.occurredAt) == selectedBabyDay && isEventVisibleInHome(it, stage)
                        }
                        item { BabyDaySummary(dayEvents, selectedBabyDay) }
                        item {
                            SectionHeader(
                                title = if (selectedBabyDay == BabyLogFormatters.todayDateInput()) "今天记录" else "当天记录"
                            )
                        }
                        if (dayEvents.isEmpty()) {
                            item { EmptyPanel("这一天还没有记录") }
                        } else {
                            items(dayEvents, key = { it.id }) { event ->
                                TimelineRow(event, onDelete = { onDeleteEvent(event) })
                            }
                        }
                    }
                    if (stage == BabyLogDomain.STAGE_PREGNANCY) {
                        item { PregnancySummaryPanel(state.timeline) }
                    }
                    if (stage != BabyLogDomain.STAGE_BABY) {
                        item {
                            SectionHeader(
                                title = "最近记录",
                                action = "全部记录",
                                onAction = { onTabSelected("timeline") }
                            )
                        }
                        val recent = state.dashboard?.recentEvents.orEmpty()
                            .filter { isEventVisibleInHome(it, stage) }
                            .take(4)
                        if (recent.isEmpty()) {
                            item { EmptyPanel("还没有记录") }
                        } else {
                            items(recent, key = { it.id }) { event ->
                                TimelineRow(event, onDelete = { onDeleteEvent(event) })
                            }
                        }
                    }
                    if (stage == BabyLogDomain.STAGE_PREGNANCY) {
                        item { FetalGrowthPanel(state.timeline) }
                    } else {
                        item { SectionHeader(title = "趋势") }
                        item { TrendPanel(state.timeline, stage) }
                    }
                }
                "timeline" -> {
                    item {
                        TimelineFilters(
                            selected = timelineFilter,
                            onSelect = onTimelineFilterSelected
                        )
                    }
                    val events = state.timeline.filter {
                        BabyLogFormatters.matchesTimelineFilter(it.eventType, timelineFilter)
                    }
                    if (events.isEmpty()) {
                        item { EmptyPanel("这个分类暂时没有记录。") }
                    } else {
                        items(events, key = { it.id }) { event ->
                            TimelineRow(event, onDelete = { onDeleteEvent(event) })
                        }
                    }
                }
                "library" -> {
                    item {
                        LibraryScreen(
                            attachments = state.attachments,
                            stage = stage,
                            onShowAttachments = onShowAttachments
                        )
                    }
                }
                "settings" -> {
                    item {
                        SettingsScreen(
                            state = state,
                            onSyncNow = onSyncNow,
                            onExportBackup = onExportBackup,
                            onImportBackup = onImportBackup,
                            onOpenSyncSettings = onOpenSyncSettings,
                            onOpenSmartSettings = onOpenSmartSettings,
                            smartConfigSummary = smartConfigSummary,
                            onClearLocalData = onClearLocalData,
                            onOpenTrash = onOpenTrash,
                            onEditProfile = onEditProfile
                        )
                    }
                }
                }
            }
        }
    }
}

@Composable
private fun TopBrandBand(activeTab: String, state: BabyLogUiState) {
    val stage = currentCareStage(state.childProfile)
    val title = if (state.setupCompleted && activeTab != "home") tabTitle(activeTab) else "BabyLog"
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
                fontSize = if (activeTab == "home") 32.sp else 24.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.sp
            )
            if (activeTab == "home" || !state.setupCompleted) {
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
private fun WeekCard(profile: BabyLogDomain.ChildProfile) {
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
private fun BabyDayCard(
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
private fun BabyDaySummary(events: List<BabyLogDomain.BabyLogEvent>, selectedDay: String) {
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
private fun PregnancySummaryPanel(events: List<BabyLogDomain.BabyLogEvent>) {
    val latestUltrasound = events.firstOrNull { it.eventType == "ultrasound" }
    val latestCheckup = events.firstOrNull { it.eventType == "pregnancy_checkup" }
    val latestMaternalMetric = events.firstOrNull { it.eventType == "maternal_metric" }
    val reviewCount = events.count { it.eventType == "ultrasound" && ultrasoundWarningText(it).isNotBlank() }
    val pendingReview = events.firstOrNull { it.eventType == "ultrasound" && ultrasoundWarningText(it).isNotBlank() }
    val nextVisitDate = latestCheckup?.payload?.optString("nextVisitNote", "")?.let(::extractDateInput)
    val nextVisitDays = nextVisitDate?.let { daysBetween(BabyLogFormatters.todayDateInput(), it) }
    val hasAnyData = latestUltrasound != null || latestCheckup != null || latestMaternalMetric != null
    Panel {
        SectionHeader(title = "孕期摘要")
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
                    ?: if (latestUltrasound == null) "录入 B 超后检查" else "B 超范围正常",
                tone = if (reviewCount == 0) ChestnutPalette.Green else ChestnutPalette.Danger,
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                title = "下次产检",
                value = nextVisitDays?.let {
                    when {
                        it < 0 -> "已过期"
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
private fun TrendPanel(events: List<BabyLogDomain.BabyLogEvent>, stage: String) {
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
private fun TimelineFilters(selected: String, onSelect: (String) -> Unit) {
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
private fun TimelineRow(
    event: BabyLogDomain.BabyLogEvent,
    onDelete: (() -> Unit)? = null
) {
    val tone = eventTone(event.eventType)
    Card(
        shape = RoundedCornerShape(14.dp),
        backgroundColor = ChestnutPalette.Surface,
        border = BorderStroke(1.dp, ChestnutPalette.Border),
        elevation = 2.dp
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
                        text = "${BabyLogFormatters.formatEventDay(event.occurredAt)} ${BabyLogFormatters.formatEventTime(event.occurredAt)}",
                        color = ChestnutPalette.Muted,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f)
                    )
                    Chip(
                        text = BabyLogFormatters.eventLabel(event.eventType),
                        bg = tone.copy(alpha = 0.14f),
                        fg = tone
                    )
                }
                Spacer(Modifier.height(7.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = BabyLogFormatters.eventSummary(event),
                        color = ChestnutPalette.Ink,
                        fontSize = 17.sp,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
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
                    Text("附件 ${event.attachmentIds.size}", color = ChestnutPalette.Primary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun LibraryScreen(
    attachments: List<BabyLogDomain.AttachmentRecord>,
    stage: String,
    onShowAttachments: (String, List<BabyLogDomain.AttachmentRecord>) -> Unit
) {
    data class LibraryEntry(
        val title: String,
        val count: String,
        val note: String,
        val icon: LineIcon,
        val detailAttachments: List<BabyLogDomain.AttachmentRecord>?
    )
    val ultrasoundAttachments = attachments.filter { it.kind == "ultrasound_image" }
    val documentAttachments = attachments.filter { it.kind == "document_image" }
    val vaccineAttachments = attachments.filter { it.kind == "vaccine_image" }
    val pregnancyEntries = listOf(
        LibraryEntry("B 超单", attachmentCount(ultrasoundAttachments), "已保存本机；表单内可识别字段", LineIcon.Ultrasound, ultrasoundAttachments),
        LibraryEntry("检查单", attachmentCount(documentAttachments), "孕期常规检查、血检报告", LineIcon.Checkup, documentAttachments),
        LibraryEntry("出生证明", "待支持", "出生资料归档入口待补", LineIcon.File, null),
        LibraryEntry("疫苗本", attachmentCount(vaccineAttachments), "出生后启用；可显示已导入附件", LineIcon.Vaccine, vaccineAttachments)
    )
    val babyEntries = listOf(
        LibraryEntry("出生证明", "待支持", "出生资料归档入口待补", LineIcon.File, null),
        LibraryEntry("疫苗本", attachmentCount(vaccineAttachments), "出生后启用；可显示已导入附件", LineIcon.Vaccine, vaccineAttachments),
        LibraryEntry("B 超单", attachmentCount(ultrasoundAttachments), "孕期资料仍可查看", LineIcon.Ultrasound, ultrasoundAttachments),
        LibraryEntry("检查单", attachmentCount(documentAttachments), "孕期常规检查、血检报告", LineIcon.Checkup, documentAttachments)
    )
    val entries = if (stage == BabyLogDomain.STAGE_BABY) babyEntries else pregnancyEntries
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
private fun SettingsScreen(
    state: BabyLogUiState,
    onSyncNow: () -> Unit,
    onExportBackup: () -> Unit,
    onImportBackup: () -> Unit,
    onOpenSyncSettings: () -> Unit,
    onOpenSmartSettings: () -> Unit,
    smartConfigSummary: String,
    onClearLocalData: () -> Unit,
    onOpenTrash: () -> Unit,
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
                title = "立即同步",
                subtitle = "待同步 ${state.dashboard?.pendingSyncCount ?: 0} 条，失败 ${state.dashboard?.failedSyncCount ?: 0} 条",
                action = "同步",
                onClick = onSyncNow
            )
        }
        SettingsPanel("智能识别") {
            ActionRow(
                title = "多模态模型",
                subtitle = smartConfigSummary,
                action = "设置",
                onClick = onOpenSmartSettings
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
private fun BabyQuickRail(
    actions: List<BabyLogService.QuickAction>,
    onAction: (BabyLogService.QuickAction) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(ChestnutPalette.Surface)
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        actions.forEach { action ->
            Column(
                modifier = Modifier
                    .width(84.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color(action.toneColor).copy(alpha = 0.16f))
                    .border(1.dp, ChestnutPalette.Border, RoundedCornerShape(18.dp))
                    .clickable { onAction(action) }
                    .padding(vertical = 11.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                BabyLogIconTile(
                    icon = quickActionIcon(action.eventType),
                    tint = Color(action.toneColor),
                    tileColor = Color(action.toneColor).copy(alpha = 0.18f),
                    modifier = Modifier.size(44.dp),
                    iconSize = 28.dp
                )
                Spacer(Modifier.height(5.dp))
                Text(
                    action.label,
                    color = ChestnutPalette.Ink,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun QuickActionDialog(
    actions: List<BabyLogService.QuickAction>,
    onDismiss: () -> Unit,
    onSmartEntry: () -> Unit,
    onAction: (BabyLogService.QuickAction) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("快捷记录", color = ChestnutPalette.Ink, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(ChestnutPalette.Primary.copy(alpha = 0.10f))
                        .border(1.dp, ChestnutPalette.Primary.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
                        .clickable { onSmartEntry() }
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BabyLogIconTile(
                        icon = LineIcon.Smart,
                        tint = ChestnutPalette.Primary,
                        tileColor = ChestnutPalette.Primary.copy(alpha = 0.16f),
                        modifier = Modifier.size(48.dp),
                        iconSize = 28.dp
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("智能录入", color = ChestnutPalette.Ink, fontWeight = FontWeight.Bold)
                        Text("一句话判断类型，打开表单后再确认保存", color = ChestnutPalette.Muted, fontSize = 13.sp)
                    }
                    Text("识别", color = ChestnutPalette.Primary, fontWeight = FontWeight.Bold)
                }
                actions.forEach { action ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(ChestnutPalette.Surface)
                            .border(1.dp, ChestnutPalette.Border, RoundedCornerShape(14.dp))
                            .clickable { onAction(action) }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        BabyLogIconTile(
                            icon = quickActionIcon(action.eventType),
                            tint = Color(action.toneColor),
                            tileColor = Color(action.toneColor).copy(alpha = 0.16f),
                            modifier = Modifier.size(48.dp),
                            iconSize = 29.dp
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(action.label, color = ChestnutPalette.Ink, fontWeight = FontWeight.Bold)
                            if (action.hint.isNotBlank()) {
                                Text(action.hint, color = ChestnutPalette.Muted, fontSize = 13.sp)
                            }
                        }
                        Text("记录", color = ChestnutPalette.Primary, fontWeight = FontWeight.Bold)
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消", color = ChestnutPalette.Muted) }
        },
        backgroundColor = ChestnutPalette.Bg
    )
}

@Composable
private fun SmartEntryDialog(
    running: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (String) -> Unit
) {
    var text by rememberSaveable { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = { if (!running) onDismiss() },
        title = { Text("智能录入", color = ChestnutPalette.Ink, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "用系统键盘语音或直接输入一句话。模型只会生成候选字段，仍需你在表单里手动保存。",
                    color = ChestnutPalette.Muted,
                    fontSize = 13.sp
                )
                ChestnutLongTextField(
                    label = "例如：今天产检在奉化妇幼，血糖餐后一小时 8.8",
                    value = text,
                    onValueChange = { text = it },
                    minLines = 4,
                    maxLines = 6
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSubmit(text) },
                enabled = text.isNotBlank() && !running,
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = ChestnutPalette.Primary,
                    disabledBackgroundColor = ChestnutPalette.Surface2
                )
            ) {
                Text(if (running) "识别中..." else "识别并打开表单", color = if (running) ChestnutPalette.Text3 else Color.White)
            }
        },
        dismissButton = {
            TextButton(enabled = !running, onClick = onDismiss) { Text("取消", color = ChestnutPalette.Muted) }
        },
        backgroundColor = ChestnutPalette.Bg
    )
}

@Composable
private fun BabyCareDialog(
    action: BabyLogService.QuickAction,
    draft: SmartEntryDraft?,
    onDismiss: () -> Unit,
    onSave: (BabyLogService.BabyCareInput) -> Unit
) {
    val values = draft?.values.orEmpty()
    var primary by rememberSaveable(action.eventType, draft?.nonce) { mutableStateOf(values["primary"].orEmpty()) }
    var secondary by rememberSaveable(action.eventType, draft?.nonce) { mutableStateOf(values["secondary"].orEmpty()) }
    var tertiary by rememberSaveable(action.eventType, draft?.nonce) { mutableStateOf(values["tertiary"].orEmpty()) }
    var note by rememberSaveable(action.eventType, draft?.nonce) { mutableStateOf(values["note"].orEmpty()) }
    val labels = babyCareLabels(action.eventType)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(action.label, color = ChestnutPalette.Ink, fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .height(460.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ChestnutTextField(labels.primary, primary, { primary = it }, labels.primaryKeyboard)
                ChestnutTextField(labels.secondary, secondary, { secondary = it }, labels.secondaryKeyboard)
                if (labels.tertiary != null) {
                    if (action.eventType == "medication") {
                        ChestnutLongTextField(labels.tertiary, tertiary, { tertiary = it }, minLines = 2, maxLines = 4)
                    } else {
                        ChestnutTextField(labels.tertiary, tertiary, { tertiary = it }, KeyboardType.Text)
                    }
                }
                if (labels.note != null) {
                    ChestnutLongTextField(labels.note, note, { note = it })
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(buildBabyCareInput(action.eventType, primary, secondary, tertiary, note))
                },
                colors = ButtonDefaults.buttonColors(backgroundColor = ChestnutPalette.Primary)
            ) {
                Text("保存", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消", color = ChestnutPalette.Muted) }
        },
        backgroundColor = ChestnutPalette.Bg
    )
}

@Composable
private fun PregnancyEventDialog(
    action: BabyLogService.QuickAction,
    draft: SmartEntryDraft?,
    onDismiss: () -> Unit,
    onSave: (BabyLogService.PregnancyInput) -> Unit
) {
    val values = draft?.values.orEmpty()
    var primary by rememberSaveable(action.eventType, draft?.nonce) {
        mutableStateOf(values["primary"] ?: defaultPregnancyPrimary(action.eventType))
    }
    var secondary by rememberSaveable(action.eventType, draft?.nonce) { mutableStateOf(values["secondary"].orEmpty()) }
    var tertiary by rememberSaveable(action.eventType, draft?.nonce) { mutableStateOf(values["tertiary"].orEmpty()) }
    var note by rememberSaveable(action.eventType, draft?.nonce) { mutableStateOf(values["note"].orEmpty()) }
    val labels = pregnancyLabels(action.eventType)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(action.label, color = ChestnutPalette.Ink, fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .height(460.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (action.eventType == "pregnancy_checkup") {
                    DateInputRow("检查日期", primary, { primary = it }, allowClear = false)
                } else {
                    ChestnutTextField(labels.primary, primary, { primary = it }, labels.primaryKeyboard)
                }
                ChestnutTextField(labels.secondary, secondary, { secondary = it }, labels.secondaryKeyboard)
                if (labels.tertiary != null) {
                    if (action.eventType == "pregnancy_checkup") {
                        ChestnutLongTextField(labels.tertiary, tertiary, { tertiary = it })
                    } else {
                        ChestnutTextField(labels.tertiary, tertiary, { tertiary = it }, labels.tertiaryKeyboard)
                    }
                }
                if (labels.note != null) {
                    ChestnutLongTextField(labels.note, note, { note = it })
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(buildPregnancyInput(action.eventType, primary, secondary, tertiary, note))
                },
                colors = ButtonDefaults.buttonColors(backgroundColor = ChestnutPalette.Primary)
            ) {
                Text("保存", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消", color = ChestnutPalette.Muted) }
        },
        backgroundColor = ChestnutPalette.Bg
    )
}

@Composable
private fun MaternalMetricDialog(
    draft: SmartEntryDraft?,
    onDismiss: () -> Unit,
    onSave: (BabyLogService.MaternalMetricInput) -> Unit
) {
    val values = draft?.values.orEmpty()
    var weight by rememberSaveable(draft?.nonce) { mutableStateOf(values["weightKg"].orEmpty()) }
    var systolic by rememberSaveable(draft?.nonce) { mutableStateOf(values["systolicBp"].orEmpty()) }
    var diastolic by rememberSaveable(draft?.nonce) { mutableStateOf(values["diastolicBp"].orEmpty()) }
    var glucose by rememberSaveable(draft?.nonce) { mutableStateOf(values["glucoseMmolL"].orEmpty()) }
    var glucoseContext by rememberSaveable(draft?.nonce) {
        mutableStateOf(values["glucoseContext"]?.let { normalizeGlucoseContext(it) } ?: "fasting")
    }
    var note by rememberSaveable(draft?.nonce) { mutableStateOf(values["note"].orEmpty()) }
    val glucoseWarning = BabyLogFormatters.formatMaternalGlucoseWarning(
        BabyLogFormatters.parseOptionalNumber(glucose),
        glucoseContext
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("孕妈指标", color = ChestnutPalette.Ink, fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .height(460.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                UnitInputRow("体重", weight, { weight = it }, "kg")
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    UnitInputRow("收缩压", systolic, { systolic = it }, "mmHg", Modifier.weight(1f))
                    UnitInputRow("舒张压", diastolic, { diastolic = it }, "mmHg", Modifier.weight(1f))
                }
                UnitInputRow("血糖", glucose, { glucose = it }, "mmol/L")
                GlucoseContextRow(glucoseContext, onSelect = { glucoseContext = it })
                if (glucoseWarning.isNotBlank()) {
                    Text(
                        text = glucoseWarning,
                        color = ChestnutPalette.Accent,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                ChestnutLongTextField("备注，可空", note, { note = it })
                Text(
                    text = "血糖提示仅用于提醒复核，不构成诊断",
                    color = ChestnutPalette.Text3,
                    fontSize = 12.sp
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        BabyLogService.MaternalMetricInput.create(
                            weight,
                            systolic,
                            diastolic,
                            glucose,
                            glucoseContext,
                            note
                        )
                    )
                },
                colors = ButtonDefaults.buttonColors(backgroundColor = ChestnutPalette.Primary)
            ) {
                Text("保存", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消", color = ChestnutPalette.Muted) }
        },
        backgroundColor = ChestnutPalette.Bg
    )
}

@Composable
private fun UltrasoundDialog(
    defaultGestationalAge: String,
    expectedDueDate: String,
    draft: SmartEntryDraft?,
    photoPath: String?,
    photoName: String?,
    ocrRunning: Boolean,
    ocrCandidate: BabyLogSmartInput.UltrasoundOcrCandidate?,
    onPickPhoto: () -> Unit,
    onCapturePhoto: () -> Unit,
    onRecognizePhoto: () -> Unit,
    onCandidateDismiss: () -> Unit,
    onCandidateApplied: () -> Unit,
    onDismiss: () -> Unit,
    onSave: (BabyLogService.UltrasoundInput) -> Unit
) {
    val values = draft?.values.orEmpty()
    var examDate by rememberSaveable(draft?.nonce) {
        mutableStateOf(values["examDate"]?.takeIf { BabyLogFormatters.isValidDateInput(it) } ?: BabyLogFormatters.todayDateInput())
    }
    var gestationalAge by rememberSaveable(defaultGestationalAge, draft?.nonce) {
        mutableStateOf(values["gestationalAge"] ?: defaultGestationalAge)
    }
    var gestationalAgeEdited by rememberSaveable(draft?.nonce) {
        mutableStateOf(!values["gestationalAge"].isNullOrBlank())
    }
    var hospital by rememberSaveable(draft?.nonce) { mutableStateOf(values["hospital"].orEmpty()) }
    var reportTime by rememberSaveable(draft?.nonce) { mutableStateOf(values["reportTime"].orEmpty()) }
    var diagnosisText by rememberSaveable(draft?.nonce) { mutableStateOf(values["diagnosisText"].orEmpty()) }
    var bpd by rememberSaveable(draft?.nonce) { mutableStateOf(values["bpdMm"].orEmpty()) }
    var hc by rememberSaveable(draft?.nonce) { mutableStateOf(values["hcMm"].orEmpty()) }
    var ac by rememberSaveable(draft?.nonce) { mutableStateOf(values["acMm"].orEmpty()) }
    var fl by rememberSaveable(draft?.nonce) { mutableStateOf(values["flMm"].orEmpty()) }
    var efw by rememberSaveable(draft?.nonce) { mutableStateOf(values["efwGram"].orEmpty()) }
    var afi by rememberSaveable(draft?.nonce) { mutableStateOf(values["afiCm"].orEmpty()) }
    var deepestPocket by rememberSaveable(draft?.nonce) { mutableStateOf(values["deepestPocketCm"].orEmpty()) }
    var placentaLocation by rememberSaveable(draft?.nonce) { mutableStateOf(values["placentaLocation"].orEmpty()) }
    var placentaGrade by rememberSaveable(draft?.nonce) { mutableStateOf(values["placentaGrade"].orEmpty()) }
    var fetalPresentation by rememberSaveable(draft?.nonce) { mutableStateOf(values["fetalPresentation"].orEmpty()) }
    var fetalHeartRate by rememberSaveable(draft?.nonce) { mutableStateOf(values["fetalHeartRateBpm"].orEmpty()) }
    var fetalCount by rememberSaveable(draft?.nonce) { mutableStateOf(values["fetalCount"].orEmpty()) }
    var fetalMovement by rememberSaveable(draft?.nonce) { mutableStateOf(values["fetalMovement"].orEmpty()) }
    var umbilicalInsertion by rememberSaveable(draft?.nonce) { mutableStateOf(values["umbilicalInsertion"].orEmpty()) }
    var cervicalLength by rememberSaveable(draft?.nonce) { mutableStateOf(values["cervicalLengthMm"].orEmpty()) }
    var crl by rememberSaveable(draft?.nonce) { mutableStateOf(values["crlMm"].orEmpty()) }
    var nt by rememberSaveable(draft?.nonce) { mutableStateOf(values["ntMm"].orEmpty()) }
    var umbilicalSd by rememberSaveable(draft?.nonce) { mutableStateOf(values["umbilicalSd"].orEmpty()) }
    var umbilicalPi by rememberSaveable(draft?.nonce) { mutableStateOf(values["umbilicalPi"].orEmpty()) }
    var umbilicalRi by rememberSaveable(draft?.nonce) { mutableStateOf(values["umbilicalRi"].orEmpty()) }
    var showAdvanced by rememberSaveable(draft?.nonce) {
        mutableStateOf(hasAdvancedUltrasoundDraft(values))
    }
    var saveError by rememberSaveable { mutableStateOf("") }

    val warnings = remember(gestationalAge, bpd, hc, ac, fl, efw) {
        BabyLogFormatters.formatUltrasoundSoftRangeWarnings(
            BabyLogFormatters.parseGestationalAgeDays(gestationalAge),
            BabyLogFormatters.parseOptionalNumber(bpd),
            BabyLogFormatters.parseOptionalNumber(hc),
            BabyLogFormatters.parseOptionalNumber(ac),
            BabyLogFormatters.parseOptionalNumber(fl),
            BabyLogFormatters.parseOptionalNumber(efw)
        )
    }
    val estimatedEfw = remember(bpd, ac, fl, efw) {
        if (efw.trim().isNotEmpty()) {
            null
        } else {
            val parsedBpd = BabyLogFormatters.parseOptionalNumber(bpd)
            val parsedAc = BabyLogFormatters.parseOptionalNumber(ac)
            val parsedFl = BabyLogFormatters.parseOptionalNumber(fl)
            if (parsedBpd != null && parsedAc != null && parsedFl != null) {
                BabyLogFetalGrowthReference.estimateEfwHadlock3Gram(parsedBpd, parsedAc, parsedFl)
            } else {
                null
            }
        }
    }
    LaunchedEffect(examDate, expectedDueDate) {
        if (!gestationalAgeEdited) {
            val inferred = gestationalAgeInputForDate(expectedDueDate, examDate)
            if (inferred.isNotBlank()) {
                gestationalAge = inferred
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("B 超记录", color = ChestnutPalette.Ink, fontWeight = FontWeight.Bold) },
        text = {
            LazyColumn(
                modifier = Modifier.height(460.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("B 超单照片", color = ChestnutPalette.Ink, fontWeight = FontWeight.Bold)
                        Text(
                            "可拍照/选图识别，或手动填写下方指标",
                            color = ChestnutPalette.Muted,
                            fontSize = 12.sp
                        )
                    }
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(
                            onClick = onCapturePhoto,
                            border = BorderStroke(1.dp, ChestnutPalette.Primary)
                        ) { Text("拍照", color = ChestnutPalette.Primary) }
                        OutlinedButton(
                            onClick = onPickPhoto,
                            border = BorderStroke(1.dp, ChestnutPalette.Primary)
                        ) { Text("选图", color = ChestnutPalette.Primary) }
                        Button(
                            enabled = photoPath != null && !ocrRunning,
                            onClick = onRecognizePhoto,
                            colors = ButtonDefaults.buttonColors(
                                backgroundColor = if (photoPath != null) ChestnutPalette.Primary else ChestnutPalette.Surface2,
                                disabledBackgroundColor = ChestnutPalette.Surface2
                            )
                        ) {
                            Text(
                                if (ocrRunning) "识别中..." else if (photoPath == null) "先选图" else "识别",
                                color = if (photoPath != null && !ocrRunning) Color.White else ChestnutPalette.Text3
                            )
                        }
                    }
                }
                if (photoPath != null) {
                    item {
                        Text(
                            "已选择：${photoName ?: File(photoPath).name}",
                            color = ChestnutPalette.Primary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
                if (saveError.isNotBlank()) {
                    item {
                        Text(
                            saveError,
                            color = Color(0xFF7C4A21),
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFFFFEBCB))
                                .padding(12.dp)
                        )
                    }
                }
                if (ocrCandidate != null) {
                    item {
                        UltrasoundOcrCandidatePanel(
                            candidate = ocrCandidate,
                            onApply = {
                                ocrCandidate.examDate.value?.takeIf { BabyLogFormatters.isValidDateInput(it) }?.let {
                                    examDate = it
                                    if (!gestationalAgeEdited) {
                                        gestationalAgeInputForDate(expectedDueDate, it).takeIf { value -> value.isNotBlank() }?.let { value ->
                                            gestationalAge = value
                                        }
                                    }
                                }
                                ocrCandidate.hospital.value?.let { hospital = it; showAdvanced = true }
                                ocrCandidate.reportTime.value?.let { reportTime = it; showAdvanced = true }
                                ocrCandidate.diagnosisText.value?.let { diagnosisText = it; showAdvanced = true }
                                ocrCandidate.bpdMm.value?.let { bpd = BabyLogFormatters.formatNumber(it) }
                                ocrCandidate.hcMm.value?.let { hc = BabyLogFormatters.formatNumber(it) }
                                ocrCandidate.acMm.value?.let { ac = BabyLogFormatters.formatNumber(it) }
                                ocrCandidate.flMm.value?.let { fl = BabyLogFormatters.formatNumber(it) }
                                ocrCandidate.efwGram.value?.let { efw = BabyLogFormatters.formatNumber(it) }
                                ocrCandidate.afiCm.value?.let { afi = BabyLogFormatters.formatNumber(it); showAdvanced = true }
                                ocrCandidate.deepestPocketCm.value?.let { deepestPocket = BabyLogFormatters.formatNumber(it); showAdvanced = true }
                                ocrCandidate.placentaLocation.value?.let { placentaLocation = it; showAdvanced = true }
                                ocrCandidate.placentaGrade.value?.let { placentaGrade = it; showAdvanced = true }
                                ocrCandidate.fetalPresentation.value?.let { fetalPresentation = it; showAdvanced = true }
                                ocrCandidate.fetalHeartRateBpm.value?.let { fetalHeartRate = BabyLogFormatters.formatNumber(it); showAdvanced = true }
                                ocrCandidate.fetalCount.value?.let { fetalCount = it; showAdvanced = true }
                                ocrCandidate.fetalMovement.value?.let { fetalMovement = it; showAdvanced = true }
                                ocrCandidate.umbilicalInsertion.value?.let { umbilicalInsertion = it; showAdvanced = true }
                                ocrCandidate.cervicalLengthMm.value?.let { cervicalLength = BabyLogFormatters.formatNumber(it); showAdvanced = true }
                                ocrCandidate.crlMm.value?.let { crl = BabyLogFormatters.formatNumber(it); showAdvanced = true }
                                ocrCandidate.ntMm.value?.let { nt = BabyLogFormatters.formatNumber(it); showAdvanced = true }
                                ocrCandidate.umbilicalSd.value?.let { umbilicalSd = BabyLogFormatters.formatNumber(it); showAdvanced = true }
                                ocrCandidate.umbilicalPi.value?.let { umbilicalPi = BabyLogFormatters.formatNumber(it); showAdvanced = true }
                                ocrCandidate.umbilicalRi.value?.let { umbilicalRi = BabyLogFormatters.formatNumber(it); showAdvanced = true }
                                onCandidateApplied()
                            },
                            onDismiss = onCandidateDismiss
                        )
                    }
                }
                item { DateInputRow("检查日期", examDate, { examDate = it }, allowClear = false) }
                item {
                    ChestnutTextField(
                        "孕周，例如 28+3",
                        gestationalAge,
                        {
                            gestationalAgeEdited = true
                            gestationalAge = it
                        },
                        KeyboardType.Text
                    )
                }
                item { Text("胎儿生长指标", color = ChestnutPalette.Ink, fontWeight = FontWeight.Bold) }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        UnitInputRow("BPD", bpd, { bpd = it }, "mm", Modifier.weight(1f))
                        UnitInputRow("HC", hc, { hc = it }, "mm", Modifier.weight(1f))
                    }
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        UnitInputRow("AC", ac, { ac = it }, "mm", Modifier.weight(1f))
                        UnitInputRow("FL", fl, { fl = it }, "mm", Modifier.weight(1f))
                    }
                }
                item { UnitInputRow("EFW", efw, { efw = it }, "g") }
                if (estimatedEfw != null) {
                    item {
                        Text(
                            "EFW 留空保存时，会按 Hadlock 3 估算为 ${BabyLogFormatters.formatNumber(estimatedEfw)} g。",
                            color = ChestnutPalette.Muted,
                            fontSize = 12.sp
                        )
                    }
                }
                item {
                    OutlinedButton(
                        onClick = { showAdvanced = !showAdvanced },
                        border = BorderStroke(1.dp, ChestnutPalette.Border),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            if (showAdvanced) "收起羊水 / 胎盘 / 脐血流" else "填写更多医学信息（可选）",
                            color = ChestnutPalette.Muted
                        )
                    }
                }
                if (showAdvanced) {
                    item { Text("公共信息", color = ChestnutPalette.Ink, fontWeight = FontWeight.Bold) }
                    item { ChestnutTextField("医院 / 机构", hospital, { hospital = it }, KeyboardType.Text) }
                    item { ChestnutTextField("报告时间", reportTime, { reportTime = it }, KeyboardType.Text) }
                    item { ChestnutLongTextField("超声诊断 / 提示", diagnosisText, { diagnosisText = it }, minLines = 2, maxLines = 4) }
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            UnitInputRow("胎心率", fetalHeartRate, { fetalHeartRate = it }, "bpm", Modifier.weight(1f))
                            UnitInputRow("CRL", crl, { crl = it }, "mm", Modifier.weight(1f))
                        }
                    }
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            UnitInputRow("NT", nt, { nt = it }, "mm", Modifier.weight(1f))
                            UnitInputRow("宫颈管", cervicalLength, { cervicalLength = it }, "mm", Modifier.weight(1f))
                        }
                    }
                    item {
                        ChoiceChipRow(
                            label = "胎儿个数",
                            selected = fetalCount,
                            options = listOf(
                                "单胎" to "单胎",
                                "双胎" to "双胎",
                                "多胎" to "多胎",
                                "未写" to "未写"
                            ),
                            onSelect = { fetalCount = it }
                        )
                    }
                    item {
                        ChoiceChipRow(
                            label = "胎动",
                            selected = fetalMovement,
                            options = listOf(
                                "有" to "有",
                                "可见" to "可见",
                                "无" to "无",
                                "未写" to "未写"
                            ),
                            onSelect = { fetalMovement = it }
                        )
                    }
                    item { ChestnutTextField("脐带插入处", umbilicalInsertion, { umbilicalInsertion = it }, KeyboardType.Text) }
                    item { Text("羊水 / 胎盘 / 胎位", color = ChestnutPalette.Ink, fontWeight = FontWeight.Bold) }
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            UnitInputRow("AFI", afi, { afi = it }, "cm", Modifier.weight(1f))
                            UnitInputRow("最大羊水池", deepestPocket, { deepestPocket = it }, "cm", Modifier.weight(1f))
                        }
                    }
                    item {
                        ChoiceChipRow(
                            label = "胎盘位置",
                            selected = placentaLocation,
                            options = listOf(
                                "前壁" to "前壁",
                                "后壁" to "后壁",
                                "侧壁" to "侧壁",
                                "低置" to "低置",
                                "前置" to "前置",
                                "其他" to "其他"
                            ),
                            onSelect = { placentaLocation = it }
                        )
                    }
                    item {
                        ChoiceChipRow(
                            label = "胎盘成熟度",
                            selected = placentaGrade,
                            options = listOf(
                                "0级" to "0 级",
                                "I 级" to "I 级",
                                "II 级" to "II 级",
                                "III 级" to "III 级",
                                "未写" to "未写"
                            ),
                            onSelect = { placentaGrade = it }
                        )
                    }
                    item {
                        ChoiceChipRow(
                            label = "胎位",
                            selected = fetalPresentation,
                            options = listOf(
                                "头位" to "头位",
                                "臀位" to "臀位",
                                "横位" to "横位",
                                "不详" to "不详"
                            ),
                            onSelect = { fetalPresentation = it }
                        )
                    }
                    item { Text("脐动脉血流", color = ChestnutPalette.Ink, fontWeight = FontWeight.Bold) }
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            UnitInputRow("S/D", umbilicalSd, { umbilicalSd = it }, "", Modifier.weight(1f))
                            UnitInputRow("PI", umbilicalPi, { umbilicalPi = it }, "", Modifier.weight(1f))
                        }
                    }
                    item { UnitInputRow("RI", umbilicalRi, { umbilicalRi = it }, "") }
                }
                if (warnings.isNotBlank()) {
                    item {
                        Text(
                            warnings,
                            color = Color(0xFF7C4A21),
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFFFFEBCB))
                                .padding(12.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val input = BabyLogService.UltrasoundInput(
                        examDate,
                        gestationalAge,
                        hospital,
                        reportTime,
                        diagnosisText,
                        bpd,
                        hc,
                        ac,
                        fl,
                        efw,
                        afi,
                        deepestPocket,
                        placentaLocation,
                        placentaGrade,
                        fetalPresentation,
                        fetalHeartRate,
                        fetalCount,
                        fetalMovement,
                        umbilicalInsertion,
                        cervicalLength,
                        crl,
                        nt,
                        umbilicalSd,
                        umbilicalPi,
                        umbilicalRi,
                        photoPath,
                        photoName
                    )
                    if (!BabyLogService.hasUltrasoundMinimumContent(input)) {
                        saveError = "请先选择 B 超单图片，或填写 BPD/HC/AC/FL/EFW 任一生长指标。"
                        return@Button
                    }
                    saveError = ""
                    onSave(input)
                },
                colors = ButtonDefaults.buttonColors(backgroundColor = ChestnutPalette.Primary)
            ) {
                Text("保存生长指标", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消", color = ChestnutPalette.Muted) }
        },
        backgroundColor = ChestnutPalette.Bg
    )
}

@Composable
private fun UltrasoundOcrCandidatePanel(
    candidate: BabyLogSmartInput.UltrasoundOcrCandidate,
    onApply: () -> Unit,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(ChestnutPalette.PrimarySoft)
            .border(1.dp, ChestnutPalette.Primary.copy(alpha = 0.45f), RoundedCornerShape(14.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("识别候选（生长指标 + 公共信息）", color = ChestnutPalette.Ink, fontWeight = FontWeight.Bold)
        Text(
            "孕周不由模型识别，请按报告手动填写",
            color = ChestnutPalette.Muted,
            fontSize = 12.sp
        )
        val rows = ultrasoundCandidateRows(candidate)
        if (rows.isEmpty()) {
            Text("模型没有返回可用字段，请手动录入。", color = ChestnutPalette.Muted, fontSize = 13.sp)
        } else {
            rows.take(14).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(row.first, color = ChestnutPalette.Muted, fontSize = 12.sp, modifier = Modifier.width(90.dp))
                    Text(row.second, color = ChestnutPalette.Ink, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
            if (rows.size > 14) {
                Text("另有 ${rows.size - 14} 项候选，应用后进入表单核对。", color = ChestnutPalette.Muted, fontSize = 12.sp)
            }
        }
        if (candidate.warnings.isNotEmpty()) {
            Text(
                "需核对：" + candidate.warnings.joinToString("；"),
                color = Color(0xFF7C4A21),
                fontSize = 12.sp
            )
        }
        if (!candidate.rawText.isNullOrBlank()) {
            Text("识别原文片段", color = ChestnutPalette.Muted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text(
                candidate.rawText.take(220),
                color = ChestnutPalette.Muted,
                fontSize = 12.sp,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(
                onClick = onDismiss,
                border = BorderStroke(1.dp, ChestnutPalette.Border)
            ) { Text("忽略", color = ChestnutPalette.Muted) }
            Button(
                enabled = rows.isNotEmpty(),
                onClick = onApply,
                colors = ButtonDefaults.buttonColors(backgroundColor = ChestnutPalette.Primary)
            ) { Text("应用到表单", color = Color.White) }
        }
    }
}

@Composable
private fun ProfileDialog(
    state: ProfileDialogState,
    onDismiss: () -> Unit,
    onSave: (ProfileInput) -> Unit
) {
    var nickname by rememberSaveable(state.title) { mutableStateOf(state.profile.nickname) }
    var sex by rememberSaveable(state.title) {
        mutableStateOf(if (state.profile.sex == "unknown") "unknown" else state.profile.sex)
    }
    var expectedDueDate by rememberSaveable(state.title) { mutableStateOf(state.profile.expectedDueDate) }
    var birthDate by rememberSaveable(state.title) { mutableStateOf(state.profile.birthDate) }
    var stageOverride by rememberSaveable(state.title) { mutableStateOf(state.initialStage) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(state.title, color = ChestnutPalette.Ink, fontWeight = FontWeight.Bold) },
        text = {
            LazyColumn(
                modifier = Modifier.height(440.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item { ChestnutTextField("乳名 / 昵称，例如 栗子", nickname, { nickname = it }, KeyboardType.Text) }
                item {
                    ChoiceChipRow(
                        label = "性别",
                        selected = sex,
                        options = listOf(
                            "female" to "女宝",
                            "male" to "男宝",
                            "unknown" to "暂不确定"
                        ),
                        onSelect = { sex = it }
                    )
                }
                item { DateInputRow("预产期", expectedDueDate, { expectedDueDate = it }) }
                item { DateInputRow("出生日期", birthDate, { birthDate = it }) }
                item {
                    ChoiceChipRow(
                        label = "阶段",
                        selected = stageOverride,
                        options = listOf(
                            BabyLogDomain.STAGE_AUTO to "自动",
                            BabyLogDomain.STAGE_PREGNANCY to "孕期",
                            BabyLogDomain.STAGE_BABY to "出生后",
                            BabyLogDomain.STAGE_UNKNOWN to "未知"
                        ),
                        onSelect = { stageOverride = it }
                    )
                }
                item {
                    Text(
                        "日期可后补",
                        color = Color(0xFF7C4A21),
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFFFEBCB))
                            .padding(12.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        ProfileInput(
                            nickname.trim(),
                            sex.trim(),
                            expectedDueDate.trim(),
                            birthDate.trim(),
                            stageOverride.trim()
                        )
                    )
                },
                colors = ButtonDefaults.buttonColors(backgroundColor = ChestnutPalette.Primary)
            ) { Text("保存", color = Color.White) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消", color = ChestnutPalette.Muted) }
        },
        backgroundColor = ChestnutPalette.Bg
    )
}

@Composable
private fun SyncSettingsDialog(
    config: BabyLogDomain.BackendConfig,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var backendBaseUrl by remember(config.backendBaseUrl) { mutableStateOf(config.backendBaseUrl) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("同步设置", color = ChestnutPalette.Ink, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                ChestnutTextField(
                    label = "后端地址，例如 https://api.example.com",
                    value = backendBaseUrl,
                    onValueChange = { backendBaseUrl = it },
                    keyboardType = KeyboardType.Uri
                )
                Text("留空会关闭后端同步，本机记录不受影响。", color = ChestnutPalette.Muted, fontSize = 13.sp)
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(backendBaseUrl) },
                colors = ButtonDefaults.buttonColors(backgroundColor = ChestnutPalette.Primary)
            ) { Text("保存", color = Color.White) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消", color = ChestnutPalette.Muted) }
        },
        backgroundColor = ChestnutPalette.Bg
    )
}

@Composable
private fun SmartModelSettingsDialog(
    config: BabyLogSmartConfigStore.Config,
    onDismiss: () -> Unit,
    onSave: (BabyLogSmartConfigStore.Config) -> Unit
) {
    var enabled by rememberSaveable(config.isEnabled()) { mutableStateOf(config.isEnabled()) }
    var baseUrl by rememberSaveable(config.getBaseUrl()) { mutableStateOf(config.getBaseUrl()) }
    var model by rememberSaveable(config.getModel()) { mutableStateOf(config.getModel()) }
    var apiKey by rememberSaveable(config.getApiKey()) { mutableStateOf(config.getApiKey()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("智能识别模型", color = ChestnutPalette.Ink, fontWeight = FontWeight.Bold) },
        text = {
            LazyColumn(
                modifier = Modifier.height(390.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = enabled, onCheckedChange = { enabled = it })
                        Text("启用智能识别", color = ChestnutPalette.Ink, fontWeight = FontWeight.Bold)
                    }
                }
                item {
                    Text("服务商预设", color = ChestnutPalette.Muted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        smartModelPresets().forEach { preset ->
                            OutlinedButton(
                                onClick = {
                                    enabled = true
                                    baseUrl = preset.baseUrl
                                    model = preset.model
                                },
                                border = BorderStroke(1.dp, ChestnutPalette.Border)
                            ) {
                                Text(preset.label, color = ChestnutPalette.Ink, fontSize = 12.sp)
                            }
                        }
                    }
                    Text(
                        "预设只填 Base URL 和模型名，API Key 仍需你手动填写。",
                        color = ChestnutPalette.Muted,
                        fontSize = 12.sp
                    )
                }
                item {
                    ChestnutTextField(
                        label = "Base URL",
                        value = baseUrl,
                        onValueChange = { baseUrl = it },
                        keyboardType = KeyboardType.Uri
                    )
                }
                item {
                    ChestnutTextField(
                        label = "模型",
                        value = model,
                        onValueChange = { model = it },
                        keyboardType = KeyboardType.Text
                    )
                }
                item {
                    ChestnutTextField(
                        label = "API Key",
                        value = apiKey,
                        onValueChange = { apiKey = it },
                        keyboardType = KeyboardType.Password,
                        visualTransformation = PasswordVisualTransformation()
                    )
                }
                item {
                    Text(
                        "Key 只保存在本机加密存储中，不进入 BabyLog 备份、家庭同步或日志。图片或文字只会在你主动点击识别/智能录入时发送给该模型服务商。",
                        color = Color(0xFF7C4A21),
                        fontSize = 13.sp,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFFFEBCB))
                            .padding(12.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        BabyLogSmartConfigStore.Config(
                            baseUrl.trim(),
                            model.trim(),
                            apiKey.trim(),
                            enabled
                        )
                    )
                },
                colors = ButtonDefaults.buttonColors(backgroundColor = ChestnutPalette.Primary)
            ) { Text("保存", color = Color.White) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消", color = ChestnutPalette.Muted) }
        },
        backgroundColor = ChestnutPalette.Bg
    )
}

private data class SmartModelPreset(
    val label: String,
    val baseUrl: String,
    val model: String
)

private fun smartModelPresets() = listOf(
    SmartModelPreset(
        "Qwen Plus",
        "https://dashscope.aliyuncs.com/compatible-mode/v1",
        "qwen3-vl-plus"
    ),
    SmartModelPreset(
        "Qwen Flash",
        "https://dashscope.aliyuncs.com/compatible-mode/v1",
        "qwen3-vl-flash"
    ),
    SmartModelPreset(
        "OpenAI",
        "https://api.openai.com/v1",
        "gpt-4o-mini"
    )
)

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
private fun AttachmentListDialog(
    title: String,
    attachments: List<BabyLogDomain.AttachmentRecord>,
    onDismiss: () -> Unit,
    onPreview: (BabyLogDomain.AttachmentRecord) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, color = ChestnutPalette.Ink, fontWeight = FontWeight.Bold) },
        text = {
            if (attachments.isEmpty()) {
                Text("暂无附件。", color = ChestnutPalette.Muted)
            } else {
                LazyColumn(
                    modifier = Modifier.height(360.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(attachments, key = { it.id }) { attachment ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(ChestnutPalette.Surface)
                                .border(1.dp, ChestnutPalette.Border, RoundedCornerShape(12.dp))
                                .clickable { onPreview(attachment) }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            BabyLogIconTile(
                                icon = LineIcon.File,
                                tint = ChestnutPalette.Primary,
                                tileColor = ChestnutPalette.Primary.copy(alpha = 0.14f),
                                modifier = Modifier.size(44.dp),
                                iconSize = 26.dp
                            )
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(attachment.originalName, color = ChestnutPalette.Ink, fontWeight = FontWeight.Bold)
                                Text(
                                    "${BabyLogFormatters.formatDateTime(attachment.createdAt)} · ${BabyLogFormatters.formatByteSize(attachment.byteSize)}",
                                    color = ChestnutPalette.Muted,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("关闭", color = ChestnutPalette.Primary) }
        },
        backgroundColor = ChestnutPalette.Bg
    )
}

@Composable
private fun AttachmentPreviewDialog(
    attachment: BabyLogDomain.AttachmentRecord,
    onDismiss: () -> Unit
) {
    val bitmap = remember(attachment.localPath) {
        BitmapFactory.decodeFile(attachment.localPath)?.asImageBitmap()
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(attachment.originalName, color = ChestnutPalette.Ink, fontWeight = FontWeight.Bold) },
        text = {
            if (bitmap == null) {
                Text("本机文件不存在或无法读取。", color = ChestnutPalette.Muted)
            } else {
                Image(
                    bitmap = bitmap,
                    contentDescription = attachment.originalName,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(360.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(ChestnutPalette.Surface2),
                    contentScale = ContentScale.Fit
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("关闭", color = ChestnutPalette.Primary) }
        },
        backgroundColor = ChestnutPalette.Bg
    )
}

@Composable
private fun TrashDialog(
    events: List<BabyLogDomain.BabyLogEvent>,
    onDismiss: () -> Unit,
    onRestore: (BabyLogDomain.BabyLogEvent) -> Unit
) {
    val nowIso = remember(events) { BabyLogFormatters.nowIso() }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("回收站", color = ChestnutPalette.Ink, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "删除记录保留 7 天，超期自动永久清理",
                    color = Color(0xFF7C4A21),
                    fontSize = 13.sp,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFFFEBCB))
                        .padding(12.dp)
                )
                if (events.isEmpty()) {
                    EmptyPanel("回收站是空的")
                } else {
                    LazyColumn(
                        modifier = Modifier.height(390.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(events, key = { it.id }) { event ->
                            TrashRow(
                                event = event,
                                nowIso = nowIso,
                                onRestore = { onRestore(event) }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("关闭", color = ChestnutPalette.Primary) }
        },
        backgroundColor = ChestnutPalette.Bg
    )
}

@Composable
private fun TrashRow(
    event: BabyLogDomain.BabyLogEvent,
    nowIso: String,
    onRestore: () -> Unit
) {
    val remainingDays = BabyLogService.trashRemainingDays(event.deletedAt, nowIso)
    Card(
        shape = RoundedCornerShape(14.dp),
        backgroundColor = ChestnutPalette.Surface,
        border = BorderStroke(1.dp, ChestnutPalette.Border),
        elevation = 1.dp
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "${BabyLogFormatters.eventLabel(event.eventType)} · ${BabyLogFormatters.formatEventDay(event.occurredAt)} ${BabyLogFormatters.formatEventTime(event.occurredAt)}",
                        color = ChestnutPalette.Muted,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        BabyLogFormatters.eventSummary(event),
                        color = ChestnutPalette.Ink,
                        fontSize = 16.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    if (remainingDays <= 0) "即将清理" else "剩 $remainingDays 天",
                    color = if (remainingDays <= 1) ChestnutPalette.Danger else ChestnutPalette.Accent,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(if (remainingDays <= 1) Color(0xFFFFE4DF) else ChestnutPalette.AccentSoft)
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "删除于 ${BabyLogFormatters.formatDateTime(event.deletedAt)}",
                    color = ChestnutPalette.Text3,
                    fontSize = 12.sp,
                    modifier = Modifier.weight(1f)
                )
                Button(
                    onClick = onRestore,
                    colors = ButtonDefaults.buttonColors(backgroundColor = ChestnutPalette.Primary),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text("恢复", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun LibraryItem(
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
private fun SettingsPanel(title: String, content: @Composable ColumnScope.() -> Unit) {
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
private fun ActionRow(
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
private fun BabyLogIconTile(
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
private fun BabyLogMaterialIcon(
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

private fun quickActionIcon(eventType: String): LineIcon {
    return when (eventType) {
        "ultrasound" -> LineIcon.Ultrasound
        "pregnancy_checkup" -> LineIcon.Checkup
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
private fun BottomNav(activeTab: String, onTabSelected: (String) -> Unit) {
    val items = listOf(
        NavItem("home", "首页", LineIcon.Home),
        NavItem("timeline", "时间线", LineIcon.Timeline),
        NavItem("library", "资料", LineIcon.Library),
        NavItem("settings", "设置", LineIcon.Settings)
    )
    BottomNavigation(
        backgroundColor = ChestnutPalette.Primary,
        contentColor = Color.White,
        elevation = 0.dp
    ) {
        items.forEach { item ->
            val selected = activeTab == item.key
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
    }
}

private data class NavItem(val key: String, val label: String, val icon: LineIcon)

private enum class LineIcon(val imageVector: ImageVector) {
    Home(Icons.Rounded.Home),
    Timeline(Icons.Rounded.FormatListBulleted),
    Library(Icons.Rounded.Article),
    Settings(Icons.Rounded.Settings),
    Plus(Icons.Rounded.Add),
    Smart(Icons.Rounded.Assessment),
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

private data class BabyCareLabels(
    val primary: String,
    val secondary: String,
    val tertiary: String?,
    val note: String?,
    val primaryKeyboard: KeyboardType = KeyboardType.Text,
    val secondaryKeyboard: KeyboardType = KeyboardType.Text
)

private data class PregnancyLabels(
    val primary: String,
    val secondary: String,
    val tertiary: String?,
    val note: String?,
    val primaryKeyboard: KeyboardType = KeyboardType.Text,
    val secondaryKeyboard: KeyboardType = KeyboardType.Text,
    val tertiaryKeyboard: KeyboardType = KeyboardType.Text
)

private fun babyCareLabels(eventType: String): BabyCareLabels {
    return when (eventType) {
        "feed" -> BabyCareLabels("方式，例如 母乳 / 奶瓶 / 辅食", "奶量 ml，例如 120", null, "备注", KeyboardType.Text, KeyboardType.Decimal)
        "sleep" -> BabyCareLabels("开始时间，例如 22:10", "结束时间，例如 01:20", "地点，例如 卧室", "备注")
        "diaper" -> BabyCareLabels("类型，例如 尿 / 便 / 混合", "性状或备注", null, "备注")
        "temperature" -> BabyCareLabels("体温", "测量方式，例如 腋温", null, "备注", KeyboardType.Decimal, KeyboardType.Text)
        "medication" -> BabyCareLabels("药名", "剂量，例如 2 ml", "原因", null)
        else -> BabyCareLabels("详情", "备注", null, null)
    }
}

private fun pregnancyLabels(eventType: String): PregnancyLabels {
    return when (eventType) {
        "pregnancy_checkup" -> PregnancyLabels(
            "检查日期 yyyy-MM-dd",
            "医院 / 医生，例如 市妇幼产科",
            "结论 / 医嘱",
            "下次产检或备注"
        )
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

private fun defaultPregnancyPrimary(eventType: String): String {
    return if (eventType == "pregnancy_checkup") BabyLogFormatters.todayDateInput() else ""
}

private fun buildBabyCareInput(
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
        else -> BabyLogService.BabyCareInput.feed(primary, secondary, note)
    }
}

private fun buildPregnancyInput(
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
        else -> BabyLogService.PregnancyInput.fetalMovement(primary, secondary, note)
    }
}

private fun currentCareStage(profile: BabyLogDomain.ChildProfile): String {
    return BabyLogFormatters.resolveCareStage(profile, BabyLogFormatters.todayDateInput())
}

private fun currentGestationalAgeInput(profile: BabyLogDomain.ChildProfile): String {
    if (!BabyLogFormatters.isValidDateInput(profile.expectedDueDate)) {
        return ""
    }
    return gestationalAgeInputForDate(profile.expectedDueDate, BabyLogFormatters.todayDateInput())
}

private fun gestationalAgeInputForDate(expectedDueDate: String, examDate: String): String {
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

private fun hasAdvancedUltrasoundDraft(values: Map<String, String>): Boolean {
    val basicKeys = setOf("examDate", "gestationalAge", "bpdMm", "hcMm", "acMm", "flMm", "efwGram")
    return values.any { (key, value) -> key !in basicKeys && value.isNotBlank() }
}

private fun normalizeGlucoseContext(value: String): String {
    val normalized = value.trim().lowercase(Locale.US)
    return when {
        normalized == "fasting" || value.contains("空腹") -> "fasting"
        normalized == "after_1h" || value.contains("1h") || value.contains("1小时") || value.contains("一小时") -> "after_1h"
        normalized == "after_2h" || value.contains("2h") || value.contains("2小时") || value.contains("两小时") -> "after_2h"
        normalized == "random" || value.contains("随机") -> "random"
        else -> "random"
    }
}

private fun ultrasoundCandidateRows(candidate: BabyLogSmartInput.UltrasoundOcrCandidate): List<Pair<String, String>> {
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
        else -> "待补档案"
    }
}

private fun stageOverrideLabel(stageOverride: String): String {
    return when (stageOverride) {
        BabyLogDomain.STAGE_PREGNANCY -> "固定孕期"
        BabyLogDomain.STAGE_BABY -> "固定出生后"
        BabyLogDomain.STAGE_UNKNOWN -> "固定未知"
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
        "孕期", BabyLogDomain.STAGE_PREGNANCY -> BabyLogDomain.STAGE_PREGNANCY
        "出生后", "育儿", BabyLogDomain.STAGE_BABY -> BabyLogDomain.STAGE_BABY
        "未知", BabyLogDomain.STAGE_UNKNOWN -> BabyLogDomain.STAGE_UNKNOWN
        else -> BabyLogDomain.STAGE_AUTO
    }
}

private fun isEventVisibleInHome(event: BabyLogDomain.BabyLogEvent, stage: String): Boolean {
    val group = BabyLogFormatters.timelineFilterGroup(event.eventType)
    return when (stage) {
        BabyLogDomain.STAGE_PREGNANCY -> group == "pregnancy" || group == "ultrasound" || group == "checkup"
        BabyLogDomain.STAGE_BABY -> group == "baby" || group == "temperature"
        else -> false
    }
}

private fun daysBetween(fromDate: String, toDate: String): Int {
    if (!BabyLogFormatters.isValidDateInput(fromDate) || !BabyLogFormatters.isValidDateInput(toDate)) {
        return 0
    }
    return try {
        val format = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val from = format.parse(fromDate)
        val to = format.parse(toDate)
        if (from == null || to == null) 0 else ((to.time - from.time) / 86_400_000L).toInt()
    } catch (_: Exception) {
        0
    }
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
        "timeline" -> "时间线"
        "library" -> "资料"
        "settings" -> "设置"
        else -> "首页"
    }
}
