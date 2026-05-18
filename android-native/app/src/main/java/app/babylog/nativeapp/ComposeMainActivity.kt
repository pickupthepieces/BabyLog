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
import androidx.compose.material.AlertDialog
import androidx.compose.material.BottomNavigation
import androidx.compose.material.BottomNavigationItem
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Card
import androidx.compose.material.Divider
import androidx.compose.material.FloatingActionButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Scaffold
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.TextField
import androidx.compose.material.TextFieldDefaults
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
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
    private lateinit var cameraPermissionLauncher: ActivityResultLauncher<String>
    private lateinit var cameraLauncher: ActivityResultLauncher<Uri>
    private lateinit var pickImageLauncher: ActivityResultLauncher<String>
    private lateinit var exportBackupLauncher: ActivityResultLauncher<String>
    private lateinit var importBackupLauncher: ActivityResultLauncher<Array<String>>

    private var uiState by mutableStateOf(BabyLogUiState())
    private var activeTab by mutableStateOf("home")
    private var timelineFilter by mutableStateOf("all")
    private var showQuickSheet by mutableStateOf(false)
    private var babyCareAction by mutableStateOf<BabyLogService.QuickAction?>(null)
    private var showUltrasoundForm by mutableStateOf(false)
    private var showSyncSettings by mutableStateOf(false)
    private var showClearLocalConfirm by mutableStateOf(false)
    private var profileDialog by mutableStateOf<ProfileDialogState?>(null)
    private var importConfirm by mutableStateOf<ImportConfirmState?>(null)
    private var syncConfirmUrl by mutableStateOf<String?>(null)
    private var attachmentDialog by mutableStateOf<AttachmentDialogState?>(null)
    private var previewAttachment by mutableStateOf<BabyLogDomain.AttachmentRecord?>(null)
    private var infoDialog by mutableStateOf<InfoDialogState?>(null)
    private var pendingCameraFile: File? = null
    private var pendingUltrasoundPhotoPath by mutableStateOf<String?>(null)
    private var pendingUltrasoundPhotoName by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = ChestnutPalette.BgArgb
        window.navigationBarColor = ChestnutPalette.SurfaceArgb

        repository = BabyLogRepository(this)
        service = BabyLogService(this, repository)
        registerLaunchers()

        setContent {
            ChestnutTheme {
                BabyLogApp(
                    state = uiState,
                    activeTab = activeTab,
                    timelineFilter = timelineFilter,
                    onTabSelected = { activeTab = it },
                    onTimelineFilterSelected = { timelineFilter = it },
                    onQuickClick = { showQuickSheet = true },
                    onShowAttachments = { title, attachments ->
                        attachmentDialog = AttachmentDialogState(title, attachments)
                    },
                    onSyncNow = ::syncNow,
                    onExportBackup = ::exportBackup,
                    onImportBackup = ::importBackup,
                    onOpenSyncSettings = { showSyncSettings = true },
                    onClearLocalData = { showClearLocalConfirm = true },
                    onCreatePregnancyProfile = { openNewFamilyForm(BabyLogDomain.STAGE_PREGNANCY) },
                    onCreateBabyProfile = { openNewFamilyForm(BabyLogDomain.STAGE_BABY) },
                    onEditProfile = { openProfileEditDialog() }
                )

                if (showQuickSheet) {
                    QuickActionDialog(
                        actions = quickActions(),
                        onDismiss = { showQuickSheet = false },
                        onAction = { action ->
                            showQuickSheet = false
                            if (isBabyCareAction(action.eventType)) {
                                babyCareAction = action
                            } else if (action.eventType == "ultrasound") {
                                openUltrasoundForm()
                            } else {
                                recordQuickAction(action)
                            }
                        }
                    )
                }

                babyCareAction?.let { action ->
                    BabyCareDialog(
                        action = action,
                        onDismiss = { babyCareAction = null },
                        onSave = { input ->
                            babyCareAction = null
                            recordBabyCare(input)
                        }
                    )
                }

                if (showUltrasoundForm) {
                    UltrasoundDialog(
                        photoPath = pendingUltrasoundPhotoPath,
                        photoName = pendingUltrasoundPhotoName,
                        onPickPhoto = ::pickImage,
                        onCapturePhoto = ::requestCameraOrLaunch,
                        onDismiss = { showUltrasoundForm = false },
                        onSave = { input ->
                            showUltrasoundForm = false
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
            val nextState = BabyLogUiState(
                dashboard = service.loadDashboard(),
                timeline = service.listRecentEvents(100),
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

    private fun openUltrasoundForm() {
        pendingUltrasoundPhotoPath = null
        pendingUltrasoundPhotoName = null
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

    private fun recordUltrasound(input: BabyLogService.UltrasoundInput) {
        runInBackground {
            try {
                service.recordUltrasound(input)
                pendingUltrasoundPhotoPath = null
                pendingUltrasoundPhotoName = null
                showToast("已保存 B 超记录")
                reloadData()
            } catch (error: JSONException) {
                showInfo("保存失败", error.message ?: "无法保存 B 超记录")
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
            val uri = FileProvider.getUriForFile(this, "$packageName.files", file)
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
                }
            } catch (error: IOException) {
                showInfo("导入图片失败", error.message ?: "无法读取图片")
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
                activeTab = "home"
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
            activeTab = "home"
            showToast("本机数据已清空")
            reloadData()
        }
    }

    private fun quickActions(): List<BabyLogService.QuickAction> {
        if (!uiState.setupCompleted) return emptyList()
        return quickActionsForStage(currentCareStage(uiState.childProfile))
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
                activeTab = "home"
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
                BabyLogService.QuickAction("B超", "指标 / 照片 / OCR占位", R.drawable.ultrasound_sheet, ChestnutPalette.RoseArgb, "ultrasound", "B 超快捷记录 · 待补指标/照片"),
                BabyLogService.QuickAction("产检", "检查 / 医嘱 / 备注", R.drawable.baby_diary_notebook, ChestnutPalette.VioletArgb, "pregnancy_checkup", "产检快捷记录 · 待补充详情"),
                BabyLogService.QuickAction("胎动", "次数 / 时段 / 备注", R.drawable.family_heart, ChestnutPalette.GreenArgb, "fetal_movement", "胎动快捷记录 · 待补充详情"),
                BabyLogService.QuickAction("宫缩", "间隔 / 持续 / 备注", R.drawable.icon_chart, ChestnutPalette.PeachArgb, "contraction", "宫缩快捷记录 · 待补充详情")
            )
        }
        if (stage == BabyLogDomain.STAGE_BABY) {
            return listOf(
                BabyLogService.QuickAction("喂养", "母乳 / 奶瓶 / 辅食", R.drawable.feeding_bottle, ChestnutPalette.PeachArgb, "feed", "快捷记录 · 待补充奶量/方式"),
                BabyLogService.QuickAction("睡眠", "开始 / 结束 / 地点", R.drawable.sleep_moon, ChestnutPalette.BlueArgb, "sleep", "快捷记录 · 待补充睡眠时长"),
                BabyLogService.QuickAction("尿布", "尿 / 便 / 性状", R.drawable.diaper, ChestnutPalette.YellowArgb, "diaper", "快捷记录 · 待补充尿/便细节"),
                BabyLogService.QuickAction("体温", "温度 / 测量方式", R.drawable.thermometer, ChestnutPalette.GreenArgb, "temperature", "快捷记录 · 待补充温度数值"),
                BabyLogService.QuickAction("用药", "药名 / 剂量 / 时间", R.drawable.icon_pill, ChestnutPalette.VioletArgb, "medication", "快捷记录 · 待补充药名/剂量")
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

private object ChestnutPalette {
    val Bg = Color(0xFFFFF6EA)
    val Surface = Color(0xFFFFFDF8)
    val Surface2 = Color(0xFFF8E7D0)
    val Border = Color(0xFFE6CDB2)
    val Ink = Color(0xFF342319)
    val Muted = Color(0xFF745D4D)
    val Text3 = Color(0xFFA88B78)
    val Primary = Color(0xFF8D3E24)
    val PrimarySoft = Color(0xFFF2D7C4)
    val Accent = Color(0xFFC77733)
    val AccentSoft = Color(0xFFFFE8BE)
    val Rose = Color(0xFFC85E64)
    val Blue = Color(0xFF6D8EB6)
    val Violet = Color(0xFF9B7AA5)
    val Green = Color(0xFF5E9169)
    val Yellow = Color(0xFFD9A441)
    val Peach = Color(0xFFD58A55)
    val Danger = Color(0xFFAA4036)

    val BgArgb = 0xFFFFF6EA.toInt()
    val SurfaceArgb = 0xFFFFFDF8.toInt()
    val RoseArgb = 0xFFC85E64.toInt()
    val BlueArgb = 0xFF6D8EB6.toInt()
    val VioletArgb = 0xFF9B7AA5.toInt()
    val GreenArgb = 0xFF5E9169.toInt()
    val YellowArgb = 0xFFD9A441.toInt()
    val PeachArgb = 0xFFD58A55.toInt()
}

@Composable
private fun ChestnutTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colors = lightColors(
            primary = ChestnutPalette.Primary,
            primaryVariant = ChestnutPalette.Primary,
            secondary = ChestnutPalette.Accent,
            background = ChestnutPalette.Bg,
            surface = ChestnutPalette.Surface,
            error = ChestnutPalette.Danger,
            onPrimary = Color.White,
            onSecondary = Color.White,
            onBackground = ChestnutPalette.Ink,
            onSurface = ChestnutPalette.Ink,
            onError = Color.White
        ),
        content = content
    )
}

@Composable
private fun BabyLogApp(
    state: BabyLogUiState,
    activeTab: String,
    timelineFilter: String,
    onTabSelected: (String) -> Unit,
    onTimelineFilterSelected: (String) -> Unit,
    onQuickClick: () -> Unit,
    onShowAttachments: (String, List<BabyLogDomain.AttachmentRecord>) -> Unit,
    onSyncNow: () -> Unit,
    onExportBackup: () -> Unit,
    onImportBackup: () -> Unit,
    onOpenSyncSettings: () -> Unit,
    onClearLocalData: () -> Unit,
    onCreatePregnancyProfile: () -> Unit,
    onCreateBabyProfile: () -> Unit,
    onEditProfile: () -> Unit
) {
    Scaffold(
        backgroundColor = ChestnutPalette.Bg,
        floatingActionButton = {
            if (state.setupCompleted) {
                FloatingActionButton(
                    onClick = onQuickClick,
                    backgroundColor = ChestnutPalette.Primary,
                    contentColor = Color.White
                ) {
                    Text("+", fontSize = 30.sp, fontWeight = FontWeight.Medium)
                }
            }
        },
        bottomBar = {
            if (state.setupCompleted) {
                BottomNav(activeTab = activeTab, onTabSelected = onTabSelected)
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
                top = inner.calculateTopPadding() + 14.dp,
                end = 18.dp,
                bottom = inner.calculateBottomPadding() + 22.dp
            ),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item { Header(activeTab, state) }
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
                            BabyDayCard(state.childProfile)
                        } else {
                            WeekCard(state.childProfile)
                        }
                    }
                    if (stage == BabyLogDomain.STAGE_BABY) {
                        item { TodayPanel(state.dashboard) }
                    }
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
                        item { EmptyPanel("还没有记录，点下方 + 开始。") }
                    } else {
                        items(recent, key = { it.id }) { event ->
                            TimelineRow(event)
                        }
                    }
                    item { SectionHeader(title = "趋势", action = "点击查看曲线") }
                    item { TrendPanel(state.dashboard, stage) }
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
                            TimelineRow(event)
                        }
                    }
                }
                "library" -> {
                    item {
                        LibraryScreen(
                            attachments = state.attachments,
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
                            onClearLocalData = onClearLocalData,
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
private fun Header(activeTab: String, state: BabyLogUiState) {
    val stage = currentCareStage(state.childProfile)
    val title = if (state.setupCompleted && activeTab != "home") tabTitle(activeTab) else "BabyLog"
    val subtitle = if (!state.setupCompleted) {
        "先建档，再进入家庭记录"
    } else {
        val nickname = state.childProfile.nickname.ifBlank { "宝宝" }
        "$nickname · ${stageLabel(stage)} · 本机模式"
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = ChestnutPalette.Ink,
                fontSize = if (activeTab == "home") 34.sp else 26.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.sp
            )
            if (activeTab == "home" || !state.setupCompleted) {
                Text(
                    text = subtitle,
                    color = ChestnutPalette.Muted,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
        if (state.setupCompleted && activeTab == "home") {
            Text(
                text = "离线可用",
                color = ChestnutPalette.Primary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(ChestnutPalette.PrimarySoft)
                    .padding(horizontal = 15.dp, vertical = 8.dp)
            )
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
            text = "BabyLog 仅做家庭记录和复诊沟通辅助；数据默认保存在本机。后续接服务器也只做家庭成员身份校验和同步授权，不接第三方账户。",
            color = Color(0xFF7C4A21),
            modifier = Modifier
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xFFFFEBCB))
                .padding(14.dp)
        )
        Panel {
            Text("开始使用", color = ChestnutPalette.Ink, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Text("先选择当前家庭状态，BabyLog 会按孕期或出生后显示不同首页。", color = ChestnutPalette.Muted)
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
        Text(
            text = "连接已有家庭后续在设置页填写服务器地址和家庭密钥；首登只保留新建与导入，避免重复建档。",
            color = ChestnutPalette.Text3,
            fontSize = 13.sp
        )
    }
}

@Composable
private fun WeekCard(profile: BabyLogDomain.ChildProfile) {
    val dueDate = profile.expectedDueDate
    val validDueDate = BabyLogFormatters.isValidDateInput(dueDate)
    val daysToDue = if (validDueDate) daysBetween(BabyLogFormatters.todayDateInput(), dueDate) else 0
    val gestationalDays = if (validDueDate) (280 - daysToDue).coerceIn(0, 280) else -1
    Card(
        shape = RoundedCornerShape(22.dp),
        backgroundColor = ChestnutPalette.Surface,
        border = BorderStroke(1.dp, ChestnutPalette.Border),
        elevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .background(
                    Brush.linearGradient(
                        listOf(ChestnutPalette.Surface, ChestnutPalette.AccentSoft)
                    )
                )
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Chip("孕期", ChestnutPalette.AccentSoft, Color(0xFF7C4A21))
                    Chip(if (validDueDate) "距预产期 $daysToDue 天" else "预产期待补", Color.White.copy(alpha = 0.45f), ChestnutPalette.Muted)
                }
                Spacer(Modifier.height(14.dp))
                Text(
                    if (gestationalDays >= 0) BabyLogFormatters.formatGestationalAge(gestationalDays) else "孕期档案待补全",
                    color = ChestnutPalette.Ink,
                    fontSize = 34.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(if (validDueDate) "预产期 $dueDate" else "设置页可补录预产期", color = ChestnutPalette.Muted, fontSize = 15.sp)
                Spacer(Modifier.height(14.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(9.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.48f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(if (gestationalDays >= 0) (gestationalDays / 280f).coerceIn(0.03f, 1f) else 0.03f)
                            .height(9.dp)
                            .clip(CircleShape)
                            .background(ChestnutPalette.Primary)
                    )
                }
            }
            Spacer(Modifier.width(14.dp))
            Card(
                shape = RoundedCornerShape(20.dp),
                backgroundColor = Color(0xFFFFF3DB),
                elevation = 3.dp
            ) {
                Image(
                    painter = painterResource(R.drawable.chestnut_mascot),
                    contentDescription = null,
                    modifier = Modifier
                        .size(116.dp)
                        .padding(3.dp),
                    contentScale = ContentScale.Crop
                )
            }
        }
    }
}

@Composable
private fun BabyDayCard(profile: BabyLogDomain.ChildProfile) {
    val nickname = profile.nickname.ifBlank { "宝宝" }
    val age = if (BabyLogFormatters.isValidDateInput(profile.birthDate)) {
        "出生日期 ${profile.birthDate} · 第 ${kotlin.math.max(1, daysBetween(profile.birthDate, BabyLogFormatters.todayDateInput()) + 1)} 天"
    } else {
        "出生日期待补；设置页可补录"
    }
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
            Text("第一轮先按今天倒序展示记录；24 小时刻度时间轴放到下一阶段。", color = ChestnutPalette.Text3, fontSize = 12.sp)
        }
    }
}

@Composable
private fun TodayPanel(dashboard: BabyLogService.DashboardSnapshot?) {
    Panel {
        SectionHeader(title = "今日", action = "00:00 起算")
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
private fun TrendPanel(dashboard: BabyLogService.DashboardSnapshot?, stage: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        TrendCard(
            title = if (stage == BabyLogDomain.STAGE_BABY) "宝宝体重" else "胎儿 EFW",
            value = if (stage == BabyLogDomain.STAGE_BABY) "暂无数据" else latestEfwValue(dashboard),
            subtitle = if (stage == BabyLogDomain.STAGE_BABY) "录入成长后显示" else latestEfwCaption(dashboard),
            tone = ChestnutPalette.Rose,
            modifier = Modifier.weight(1f)
        )
        TrendCard(
            title = if (stage == BabyLogDomain.STAGE_BABY) "身长 / 头围" else "孕妈体重",
            value = "暂无数据",
            subtitle = if (stage == BabyLogDomain.STAGE_BABY) "录入儿保后显示" else "录入后显示",
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
private fun TimelineRow(event: BabyLogDomain.BabyLogEvent) {
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
                Text(
                    text = BabyLogFormatters.eventSummary(event),
                    color = ChestnutPalette.Ink,
                    fontSize = 17.sp,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
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
    onShowAttachments: (String, List<BabyLogDomain.AttachmentRecord>) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        LibraryItem(
            title = "B 超单",
            count = attachmentCount(attachments.filter { it.kind == "ultrasound_image" }),
            note = "已保存本机；OCR 待接入",
            asset = R.drawable.ultrasound_sheet,
            onClick = { onShowAttachments("B 超单", attachments.filter { it.kind == "ultrasound_image" }) }
        )
        LibraryItem(
            title = "检查单",
            count = attachmentCount(attachments.filter { it.kind == "document_image" }),
            note = "孕期常规检查、血检报告",
            asset = R.drawable.baby_diary_notebook,
            onClick = { onShowAttachments("检查单", attachments.filter { it.kind == "document_image" }) }
        )
        LibraryItem(
            title = "出生证明",
            count = "待支持",
            note = "出生资料归档入口待补",
            asset = R.drawable.vaccine_card,
            onClick = null
        )
        LibraryItem(
            title = "疫苗本",
            count = attachmentCount(attachments.filter { it.kind == "vaccine_image" }),
            note = "出生后启用；可显示已导入附件",
            asset = R.drawable.vaccine_card,
            onClick = { onShowAttachments("疫苗本", attachments.filter { it.kind == "vaccine_image" }) }
        )
    }
}

@Composable
private fun SettingsScreen(
    state: BabyLogUiState,
    onSyncNow: () -> Unit,
    onExportBackup: () -> Unit,
    onImportBackup: () -> Unit,
    onOpenSyncSettings: () -> Unit,
    onClearLocalData: () -> Unit,
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
private fun QuickActionDialog(
    actions: List<BabyLogService.QuickAction>,
    onDismiss: () -> Unit,
    onAction: (BabyLogService.QuickAction) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("快捷记录", color = ChestnutPalette.Ink, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
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
                        Image(
                            painter = painterResource(action.assetResId),
                            contentDescription = null,
                            modifier = Modifier.size(42.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(action.label, color = ChestnutPalette.Ink, fontWeight = FontWeight.Bold)
                            Text(action.hint, color = ChestnutPalette.Muted, fontSize = 13.sp)
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
private fun BabyCareDialog(
    action: BabyLogService.QuickAction,
    onDismiss: () -> Unit,
    onSave: (BabyLogService.BabyCareInput) -> Unit
) {
    var primary by rememberSaveable(action.eventType) { mutableStateOf("") }
    var secondary by rememberSaveable(action.eventType) { mutableStateOf("") }
    var tertiary by rememberSaveable(action.eventType) { mutableStateOf("") }
    var note by rememberSaveable(action.eventType) { mutableStateOf("") }
    val labels = babyCareLabels(action.eventType)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(action.label, color = ChestnutPalette.Ink, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                ChestnutTextField(labels.primary, primary, { primary = it }, labels.primaryKeyboard)
                ChestnutTextField(labels.secondary, secondary, { secondary = it }, labels.secondaryKeyboard)
                if (labels.tertiary != null) {
                    ChestnutTextField(labels.tertiary, tertiary, { tertiary = it }, KeyboardType.Text)
                }
                if (labels.note != null) {
                    ChestnutTextField(labels.note, note, { note = it }, KeyboardType.Text)
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
private fun UltrasoundDialog(
    photoPath: String?,
    photoName: String?,
    onPickPhoto: () -> Unit,
    onCapturePhoto: () -> Unit,
    onDismiss: () -> Unit,
    onSave: (BabyLogService.UltrasoundInput) -> Unit
) {
    var examDate by rememberSaveable { mutableStateOf(BabyLogFormatters.todayDateInput()) }
    var gestationalAge by rememberSaveable { mutableStateOf("28+3") }
    var bpd by rememberSaveable { mutableStateOf("") }
    var hc by rememberSaveable { mutableStateOf("") }
    var ac by rememberSaveable { mutableStateOf("") }
    var fl by rememberSaveable { mutableStateOf("") }
    var efw by rememberSaveable { mutableStateOf("") }

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

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("B 超记录", color = ChestnutPalette.Ink, fontWeight = FontWeight.Bold) },
        text = {
            LazyColumn(
                modifier = Modifier.height(460.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item { ChestnutTextField("检查日期，例如 2026-05-15", examDate, { examDate = it }, KeyboardType.Text) }
                item { ChestnutTextField("孕周，例如 28+3", gestationalAge, { gestationalAge = it }, KeyboardType.Text) }
                item { ChestnutTextField("BPD mm", bpd, { bpd = it }, KeyboardType.Decimal) }
                item { ChestnutTextField("HC mm", hc, { hc = it }, KeyboardType.Decimal) }
                item { ChestnutTextField("AC mm", ac, { ac = it }, KeyboardType.Decimal) }
                item { ChestnutTextField("FL mm", fl, { fl = it }, KeyboardType.Decimal) }
                item { ChestnutTextField("EFW g", efw, { efw = it }, KeyboardType.Decimal) }
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
                    onSave(
                        BabyLogService.UltrasoundInput(
                            examDate,
                            gestationalAge,
                            bpd,
                            hc,
                            ac,
                            fl,
                            efw,
                            photoPath,
                            photoName
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
private fun ProfileDialog(
    state: ProfileDialogState,
    onDismiss: () -> Unit,
    onSave: (ProfileInput) -> Unit
) {
    var nickname by rememberSaveable(state.title) { mutableStateOf(state.profile.nickname) }
    var sex by rememberSaveable(state.title) {
        mutableStateOf(if (state.profile.sex == "unknown") "" else state.profile.sex)
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
                item { ChestnutTextField("性别：female / male / unknown，可空", sex, { sex = it }, KeyboardType.Text) }
                item { ChestnutTextField("预产期 yyyy-MM-dd，可空", expectedDueDate, { expectedDueDate = it }, KeyboardType.Text) }
                item { ChestnutTextField("出生日期 yyyy-MM-dd，可空", birthDate, { birthDate = it }, KeyboardType.Text) }
                item { ChestnutTextField("阶段覆盖：auto / pregnancy / baby / unknown", stageOverride, { stageOverride = it }, KeyboardType.Text) }
                item {
                    Text(
                        "日期可以后补；缺失时只显示补全入口和空态，不使用假孕周、假日龄或假成长曲线。",
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
                            Image(
                                painter = painterResource(R.drawable.ultrasound_sheet),
                                contentDescription = null,
                                modifier = Modifier.size(42.dp)
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
private fun Panel(content: @Composable ColumnScope.() -> Unit) {
    Card(
        shape = RoundedCornerShape(18.dp),
        backgroundColor = ChestnutPalette.Surface,
        border = BorderStroke(1.dp, ChestnutPalette.Border),
        elevation = 3.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            content = content
        )
    }
}

@Composable
private fun MetricCard(
    title: String,
    value: String,
    subtitle: String,
    tone: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(ChestnutPalette.Bg.copy(alpha = 0.74f))
            .padding(11.dp)
            .height(76.dp)
    ) {
        Box(
            modifier = Modifier
                .width(28.dp)
                .height(3.dp)
                .clip(CircleShape)
                .background(tone)
        )
        Spacer(Modifier.height(7.dp))
        Text(title, color = ChestnutPalette.Muted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        Text(value, color = ChestnutPalette.Ink, fontSize = 20.sp, fontWeight = FontWeight.Bold, maxLines = 1)
        Text(subtitle, color = ChestnutPalette.Text3, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun TrendCard(
    title: String,
    value: String,
    subtitle: String,
    tone: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        backgroundColor = ChestnutPalette.Surface,
        border = BorderStroke(1.dp, ChestnutPalette.Border),
        elevation = 2.dp
    ) {
        Column(Modifier.padding(14.dp)) {
            Box(
                modifier = Modifier
                    .width(34.dp)
                    .height(3.dp)
                    .background(tone)
            )
            Spacer(Modifier.height(10.dp))
            Text(title, color = ChestnutPalette.Muted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text(value, color = ChestnutPalette.Ink, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text(subtitle, color = ChestnutPalette.Muted, fontSize = 12.sp)
        }
    }
}

@Composable
private fun LibraryItem(
    title: String,
    count: String,
    note: String,
    asset: Int,
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
            Image(painter = painterResource(asset), contentDescription = null, modifier = Modifier.size(58.dp))
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
private fun SectionHeader(title: String, action: String? = null, onAction: (() -> Unit)? = null) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            title,
            color = ChestnutPalette.Ink,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f)
        )
        if (action != null) {
            Text(
                action,
                color = if (onAction == null) ChestnutPalette.Text3 else ChestnutPalette.Primary,
                fontWeight = FontWeight.Bold,
                modifier = if (onAction == null) Modifier else Modifier.clickable { onAction() }
            )
        }
    }
}

@Composable
private fun Chip(text: String, bg: Color, fg: Color) {
    Text(
        text = text,
        color = fg,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .clip(CircleShape)
            .background(bg)
            .padding(horizontal = 11.dp, vertical = 6.dp)
    )
}

@Composable
private fun EmptyPanel(text: String) {
    Text(
        text = text,
        color = ChestnutPalette.Muted,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(ChestnutPalette.Surface)
            .border(1.dp, ChestnutPalette.Border, RoundedCornerShape(14.dp))
            .padding(18.dp),
        textAlign = TextAlign.Center
    )
}

@Composable
private fun ChestnutTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    keyboardType: KeyboardType
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        colors = TextFieldDefaults.textFieldColors(
            backgroundColor = ChestnutPalette.Surface,
            focusedIndicatorColor = ChestnutPalette.Primary,
            unfocusedIndicatorColor = ChestnutPalette.Border,
            textColor = ChestnutPalette.Ink,
            focusedLabelColor = ChestnutPalette.Primary,
            unfocusedLabelColor = ChestnutPalette.Muted,
            cursorColor = ChestnutPalette.Primary
        )
    )
}

@Composable
private fun BottomNav(activeTab: String, onTabSelected: (String) -> Unit) {
    val items = listOf(
        NavItem("home", "首页", R.drawable.baby_diary_notebook),
        NavItem("timeline", "时间线", R.drawable.calendar),
        NavItem("library", "资料", R.drawable.growth_ruler),
        NavItem("settings", "设置", R.drawable.icon_settings)
    )
    BottomNavigation(
        backgroundColor = ChestnutPalette.Surface,
        contentColor = ChestnutPalette.Primary,
        elevation = 9.dp
    ) {
        items.forEach { item ->
            BottomNavigationItem(
                selected = activeTab == item.key,
                onClick = { onTabSelected(item.key) },
                icon = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .width(24.dp)
                                .height(3.dp)
                                .clip(CircleShape)
                                .background(if (activeTab == item.key) ChestnutPalette.Primary else Color.Transparent)
                        )
                        Spacer(Modifier.height(4.dp))
                        Image(
                            painter = painterResource(item.asset),
                            contentDescription = null,
                            modifier = Modifier.size(25.dp)
                        )
                    }
                },
                label = {
                    Text(
                        item.label,
                        color = if (activeTab == item.key) ChestnutPalette.Primary else ChestnutPalette.Muted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                selectedContentColor = ChestnutPalette.Primary,
                unselectedContentColor = ChestnutPalette.Muted
            )
        }
    }
}

private data class NavItem(val key: String, val label: String, val asset: Int)

private data class BabyCareLabels(
    val primary: String,
    val secondary: String,
    val tertiary: String?,
    val note: String?,
    val primaryKeyboard: KeyboardType = KeyboardType.Text,
    val secondaryKeyboard: KeyboardType = KeyboardType.Text
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

private fun currentCareStage(profile: BabyLogDomain.ChildProfile): String {
    return BabyLogFormatters.resolveCareStage(profile, BabyLogFormatters.todayDateInput())
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

private fun latestEfwValue(dashboard: BabyLogService.DashboardSnapshot?): String {
    val event = dashboard?.recentEvents?.firstOrNull { it.eventType == "ultrasound" } ?: return "暂无"
    val efw = BabyLogFormatters.parseOptionalNumber(event.payload.optString("efwGram", ""))
    return if (efw == null) "待补" else "${BabyLogFormatters.formatNumber(efw)} g"
}

private fun latestEfwCaption(dashboard: BabyLogService.DashboardSnapshot?): String {
    val event = dashboard?.recentEvents?.firstOrNull { it.eventType == "ultrasound" } ?: return "保存 B 超后显示"
    return BabyLogFormatters.formatEventDay(event.occurredAt)
}

private fun tabTitle(activeTab: String): String {
    return when (activeTab) {
        "timeline" -> "时间线"
        "library" -> "资料"
        "settings" -> "设置"
        else -> "首页"
    }
}
