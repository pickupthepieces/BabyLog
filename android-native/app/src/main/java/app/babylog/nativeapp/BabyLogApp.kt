package app.babylog.nativeapp

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Scaffold
import androidx.compose.material.SnackbarDuration
import androidx.compose.material.SnackbarResult
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import app.babylog.nativeapp.ui.screens.BabyLogRoutes
import kotlinx.coroutines.withTimeoutOrNull

private const val QUICK_RAIL_ENTER_SLIDE_MILLIS = 280
private const val QUICK_RAIL_ENTER_EXPAND_MILLIS = 260
private const val QUICK_RAIL_ENTER_FADE_MILLIS = 180
private const val QUICK_RAIL_EXIT_SLIDE_MILLIS = 220
private const val QUICK_RAIL_EXIT_SHRINK_MILLIS = 210
private const val QUICK_RAIL_EXIT_FADE_MILLIS = 130
private const val QUICK_UNDO_SNACKBAR_MILLIS = 5_000L

@Suppress("LongParameterList", "LongMethod", "CyclomaticComplexMethod", "ComplexCondition", "FunctionNaming")
@Composable
internal fun BabyLogApp(
    appState: BabyLogAppState,
    actions: BabyLogAppActions
) {
    val uiState = appState.ui
    val navigationState = appState.navigation
    val homeState = appState.home
    val libraryState = appState.library
    val settingsState = appState.settings
    val syncState = appState.sync
    val recordState = appState.record
    val smartEntryState = appState.smartEntry
    val navigationActions = actions.navigation
    val libraryActions = actions.library
    val settingsActions = actions.settings
    val syncActions = actions.sync
    val profileActions = actions.profile
    val reminderActions = actions.reminder
    val recordActions = actions.record
    val smartEntryActions = actions.smartEntry
    val pendingNavRoute = navigationState.pendingNavRoute
    val pendingNavNonce = navigationState.pendingNavNonce
    val onNavRouteConsumed = navigationActions.onNavRouteConsumed
    val recordReturnRoute = navigationState.recordReturnRoute
    val recordDetailEventId = navigationState.recordDetailEventId
    val recordDetailReturnRoute = navigationState.recordDetailReturnRoute
    val highlightedEventId = navigationState.highlightedEventId
    val timelineFilter = navigationState.timelineFilter
    val selectedBabyDay = navigationState.selectedBabyDay
    val onTimelineFilterSelected = navigationActions.onTimelineFilterSelected
    val onBabyDaySelected = navigationActions.onBabyDaySelected
    val onSmartEntryClick = navigationActions.onSmartEntryClick
    val onSmartVoiceHoldStart = navigationActions.onSmartVoiceHoldStart
    val onSmartVoiceHoldEnd = navigationActions.onSmartVoiceHoldEnd
    val quickActions = homeState.quickActions
    val quickUndoRequest = homeState.quickUndoRequest
    val onQuickAction = navigationActions.onQuickAction
    val attachmentListPageState = libraryState.attachmentListPageState
    val previewAttachment = libraryState.previewAttachment
    val onShowAttachments = libraryActions.onShowAttachments
    val onOpenVisitSummary = libraryActions.onOpenVisitSummary
    val onOpenPreVisitQuestions = libraryActions.onOpenPreVisitQuestions
    val onCloseAttachmentList = libraryActions.onCloseAttachmentList
    val onPreviewAttachment = libraryActions.onPreviewAttachment
    val onCloseAttachmentPreview = libraryActions.onCloseAttachmentPreview
    val onCopyVisitSummary = libraryActions.onCopyVisitSummary
    val onShareVisitSummary = libraryActions.onShareVisitSummary
    val onSaveVisitSummary = libraryActions.onSaveVisitSummary
    val onPolishVisitSummary = libraryActions.onPolishVisitSummary
    val onSyncNow = settingsActions.onSyncNow
    val onExportBackup = settingsActions.onExportBackup
    val onImportBackup = settingsActions.onImportBackup
    val onUndoImport = settingsActions.onUndoImport
    val onOpenSyncSettings = settingsActions.onOpenSyncSettings
    val onOpenSmartSettings = settingsActions.onOpenSmartSettings
    val onOpenSpeechSettings = settingsActions.onOpenSpeechSettings
    val onAcceptDisclaimer = settingsActions.onAcceptDisclaimer
    val profilePageState = settingsState.profilePageState
    val smartSettingsConfig = settingsState.smartSettingsConfig
    val speechSettingsConfig = settingsState.speechSettingsConfig
    val smartConfigSummary = settingsState.smartConfigSummary
    val speechConfigSummary = settingsState.speechConfigSummary
    val syncFamilyKeyConfigured = syncState.familyKeyConfigured
    val syncCheckRunning = syncState.checkRunning
    val syncCheckMessage = syncState.checkMessage
    val syncCheckOk = syncState.checkOk
    val syncPushRunning = syncState.pushRunning
    val syncPushMessage = syncState.pushMessage
    val syncPullRunning = syncState.pullRunning
    val syncPullMessage = syncState.pullMessage
    val onCloseSettingsPage = settingsActions.onCloseSettingsPage
    val onCheckSyncConnection = syncActions.onCheckConnection
    val onPushSyncNow = syncActions.onPushNow
    val onPullSyncNow = syncActions.onPullNow
    val onDismissRemoteUpdateBanner = syncActions.onDismissRemoteUpdateBanner
    val onSaveSyncSettings = syncActions.onSaveSettings
    val onSaveSmartSettings = settingsActions.onSaveSmartSettings
    val onSaveSpeechSettings = settingsActions.onSaveSpeechSettings
    val onSaveProfile = profileActions.onSaveProfile
    val onClearLocalData = settingsActions.onClearLocalData
    val onOpenTrash = settingsActions.onOpenTrash
    val onOpenDisclaimer = settingsActions.onOpenDisclaimer
    val onOpenDueDateCalculator = settingsActions.onOpenDueDateCalculator
    val onOpenWeightGain = settingsActions.onOpenWeightGain
    val appVersionLabel = settingsState.appVersionLabel
    val appUpdateStatus = settingsState.appUpdateStatus
    val appUpdateRunning = settingsState.appUpdateRunning
    val onCheckAppUpdate = settingsActions.onCheckAppUpdate
    val onSavePreVisitQuestion = reminderActions.onSavePreVisitQuestion
    val onDeletePreVisitQuestion = reminderActions.onDeletePreVisitQuestion
    val onOpenReminderCenter = reminderActions.onOpenReminderCenter
    val onRequestNotificationPermission = reminderActions.onRequestNotificationPermission
    val onSaveUserReminder = reminderActions.onSaveUserReminder
    val onToggleReminder = reminderActions.onToggleReminder
    val onDismissReminder = reminderActions.onDismissReminder
    val onCompleteReminder = reminderActions.onCompleteReminder
    val onDeleteReminder = reminderActions.onDeleteReminder
    val onOpenDueDateCalculatorFromProfile = profileActions.onOpenDueDateCalculatorFromProfile
    val onApplyDueDateFromCalculator = profileActions.onApplyDueDateFromCalculator
    val onRestoreTrashEvent = reminderActions.onRestoreTrashEvent
    val onCreatePregnancyProfile = profileActions.onCreatePregnancyProfile
    val onCreateBabyProfile = profileActions.onCreateBabyProfile
    val onEditProfile = profileActions.onEditProfile
    val onOpenEventDetail = recordActions.onOpenEventDetail
    val onEditEvent = recordActions.onEditEvent
    val onDeleteEvent = recordActions.onDeleteEvent
    val babyCareAction = recordState.babyCareAction
    val babyCareDraft = recordState.babyCareDraft
    val pregnancyAction = recordState.pregnancyAction
    val pregnancyDraft = recordState.pregnancyDraft
    val maternalMetricDraft = recordState.maternalMetricDraft
    val ultrasoundDraft = recordState.ultrasoundDraft
    val editingEventType = recordState.editingEventType
    val pendingUltrasoundPhotoPath = recordState.pendingUltrasoundPhotoPath
    val pendingUltrasoundPhotoName = recordState.pendingUltrasoundPhotoName
    val pendingCheckupAttachmentPath = recordState.pendingCheckupAttachmentPath
    val pendingCheckupAttachmentName = recordState.pendingCheckupAttachmentName
    val ultrasoundOcrRunning = recordState.ultrasoundOcrRunning
    val ultrasoundOcrCandidate = recordState.ultrasoundOcrCandidate
    val checkupOcrRunning = recordState.checkupOcrRunning
    val checkupOcrCandidate = recordState.checkupOcrCandidate
    val onBabyCareCancel = recordActions.onBabyCareCancel
    val onPregnancyCancel = recordActions.onPregnancyCancel
    val onMaternalMetricCancel = recordActions.onMaternalMetricCancel
    val onUltrasoundCancel = recordActions.onUltrasoundCancel
    val onBabyCareSave = recordActions.onBabyCareSave
    val onPregnancySave = recordActions.onPregnancySave
    val onContractionSessionSave = recordActions.onContractionSessionSave
    val onMaternalMetricSave = recordActions.onMaternalMetricSave
    val onUltrasoundSave = recordActions.onUltrasoundSave
    val onPickCheckupAttachment = recordActions.onPickCheckupAttachment
    val onCaptureCheckupAttachment = recordActions.onCaptureCheckupAttachment
    val onPickUltrasoundPhoto = recordActions.onPickUltrasoundPhoto
    val onCaptureUltrasoundPhoto = recordActions.onCaptureUltrasoundPhoto
    val onRecognizeUltrasoundPhoto = recordActions.onRecognizeUltrasoundPhoto
    val onDismissUltrasoundCandidate = recordActions.onDismissUltrasoundCandidate
    val onApplyUltrasoundCandidate = recordActions.onApplyUltrasoundCandidate
    val onRecognizeCheckupAttachment = recordActions.onRecognizeCheckupAttachment
    val onDismissCheckupCandidate = recordActions.onDismissCheckupCandidate
    val onApplyCheckupCandidate = recordActions.onApplyCheckupCandidate
    val smartEntryRunning = smartEntryState.running
    val smartVoiceState = smartEntryState.voiceState
    val smartEntryCandidate = smartEntryState.candidate
    val onSmartEntryBack = smartEntryActions.onBack
    val onSmartEntryVoiceStart = smartEntryActions.onVoiceStart
    val onSmartEntryVoiceStop = smartEntryActions.onVoiceStop
    val onLongTextVoiceStart = recordActions.onLongTextVoiceStart
    val onLongTextVoiceStop = recordActions.onLongTextVoiceStop
    val onSmartEntrySubmit = smartEntryActions.onSubmit
    val onSmartEntryCandidateConfirm = smartEntryActions.onCandidateConfirm
    val onSmartEntryCandidateDismiss = smartEntryActions.onCandidateDismiss
    val state = uiState
    val navController = rememberNavController()
    val scaffoldState = rememberScaffoldState()
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStackEntry?.destination?.route
    val activeTab = if (BabyLogRoutes.isTopLevel(currentRoute)) currentRoute ?: BabyLogRoutes.Home else BabyLogRoutes.Home
    var quickRailVisible by rememberSaveable { mutableStateOf(true) }
    var recordSheetRoute by rememberSaveable { mutableStateOf<String?>(null) }
    var recordSheetExpanded by rememberSaveable { mutableStateOf(false) }
    var recordSheetVisible by rememberSaveable { mutableStateOf(false) }
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
    fun removeRecordSheet() {
        recordSheetRoute = null
        recordSheetExpanded = false
        recordSheetVisible = false
    }
    fun hideRecordSheet() {
        recordSheetExpanded = false
        recordSheetVisible = false
    }
    fun closeRecordSheet() {
        when (recordSheetRoute) {
            BabyLogRoutes.RecordBabyCare -> onBabyCareCancel()
            BabyLogRoutes.RecordPregnancyEvent -> onPregnancyCancel()
            BabyLogRoutes.RecordMaternalMetric -> onMaternalMetricCancel()
            BabyLogRoutes.RecordUltrasound -> onUltrasoundCancel()
        }
        hideRecordSheet()
    }
    fun openRecordSheet(route: String?) {
        if (route == null) return
        recordSheetRoute = route
        recordSheetExpanded = false
        recordSheetVisible = true
    }
    LaunchedEffect(pendingNavRoute, pendingNavNonce) {
        val route = pendingNavRoute ?: return@LaunchedEffect
        if (recordSheetRoute != null && route != recordSheetRoute) {
            removeRecordSheet()
        }
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
    LaunchedEffect(quickUndoRequest?.nonce) {
        val request = quickUndoRequest ?: return@LaunchedEffect
        val result = withTimeoutOrNull(QUICK_UNDO_SNACKBAR_MILLIS) {
            scaffoldState.snackbarHostState.showSnackbar(
                message = "已记录：${request.label}",
                actionLabel = "撤销",
                duration = SnackbarDuration.Indefinite
            )
        }
        if (result == null) {
            scaffoldState.snackbarHostState.currentSnackbarData?.dismiss()
        }
        navigationActions.onQuickUndoRequestConsumed(request.nonce)
        if (result == SnackbarResult.ActionPerformed) {
            navigationActions.onUndoQuickEvent(request.eventId)
        }
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

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            scaffoldState = scaffoldState,
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
                                enter = slideInVertically(
                                    animationSpec = tween(durationMillis = QUICK_RAIL_ENTER_SLIDE_MILLIS, easing = FastOutSlowInEasing),
                                    initialOffsetY = { fullHeight -> fullHeight }
                                ) + expandVertically(
                                    animationSpec = tween(durationMillis = QUICK_RAIL_ENTER_EXPAND_MILLIS, easing = FastOutSlowInEasing),
                                    expandFrom = Alignment.Bottom
                                ) + fadeIn(animationSpec = tween(durationMillis = QUICK_RAIL_ENTER_FADE_MILLIS)),
                                exit = slideOutVertically(
                                    animationSpec = tween(durationMillis = QUICK_RAIL_EXIT_SLIDE_MILLIS, easing = FastOutSlowInEasing),
                                    targetOffsetY = { fullHeight -> fullHeight }
                                ) + shrinkVertically(
                                    animationSpec = tween(durationMillis = QUICK_RAIL_EXIT_SHRINK_MILLIS, easing = FastOutSlowInEasing),
                                    shrinkTowards = Alignment.Bottom
                                ) + fadeOut(animationSpec = tween(durationMillis = QUICK_RAIL_EXIT_FADE_MILLIS))
                            ) {
                                PersistentQuickRail(
                                    actions = quickActions,
                                    onAction = { action ->
                                        openRecordSheet(onQuickAction(action, BabyLogRoutes.Home))
                                    }
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
                    onOpenDetail = { event -> onOpenEventDetail(event, BabyLogRoutes.Timeline) }
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
                    appVersionLabel = appVersionLabel,
                    appUpdateStatus = appUpdateStatus,
                    appUpdateRunning = appUpdateRunning,
                    onCheckAppUpdate = onCheckAppUpdate,
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
                    pendingAttachmentUploadCount = state.dashboard?.pendingAttachmentUploadCount ?: 0,
                    pendingAttachmentUploadBytes = state.dashboard?.pendingAttachmentUploadBytes ?: 0L,
                    pendingAttachmentDownloadCount = state.dashboard?.pendingAttachmentDownloadCount ?: 0,
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
        recordEntrySheet(
            state = RecordEntrySheetState(
                route = recordSheetRoute,
                visible = recordSheetVisible,
                expanded = recordSheetExpanded
            ),
            onExpandedChange = { recordSheetExpanded = it },
            onDismiss = ::closeRecordSheet,
            onDismissAnimationEnd = ::removeRecordSheet
        ) { route ->
            recordSheetRouteContent(
                route = route,
                appState = appState,
                actions = actions,
                onClose = ::hideRecordSheet
            )
        }
    }
}

@Suppress("LongMethod", "CyclomaticComplexMethod")
@Composable
private fun recordSheetRouteContent(
    route: String,
    appState: BabyLogAppState,
    actions: BabyLogAppActions,
    onClose: () -> Unit
) {
    val state = appState.ui
    val recordState = appState.record
    val smartVoiceState = appState.smartEntry.voiceState
    val recordActions = actions.record
    when (route) {
        BabyLogRoutes.RecordBabyCare -> BabyCareFormScreen(
            action = recordState.babyCareAction,
            draft = recordState.babyCareDraft,
            isEditing = false,
            voiceState = smartVoiceState,
            onLongTextVoiceStart = recordActions.onLongTextVoiceStart,
            onLongTextVoiceStop = recordActions.onLongTextVoiceStop,
            onBack = {
                recordActions.onBabyCareCancel()
                onClose()
            },
            onSave = recordActions.onBabyCareSave
        )
        BabyLogRoutes.RecordPregnancyEvent -> PregnancyEventFormScreen(
            action = recordState.pregnancyAction,
            draft = recordState.pregnancyDraft,
            isEditing = false,
            expectedDueDate = state.childProfile.expectedDueDate,
            attachmentPath = recordState.pendingCheckupAttachmentPath,
            attachmentName = recordState.pendingCheckupAttachmentName,
            ocrRunning = recordState.checkupOcrRunning,
            ocrCandidate = recordState.checkupOcrCandidate,
            onPickAttachment = recordActions.onPickCheckupAttachment,
            onCaptureAttachment = recordActions.onCaptureCheckupAttachment,
            onRecognizeAttachment = recordActions.onRecognizeCheckupAttachment,
            onCandidateDismiss = recordActions.onDismissCheckupCandidate,
            onCandidateApplied = recordActions.onApplyCheckupCandidate,
            voiceState = smartVoiceState,
            onLongTextVoiceStart = recordActions.onLongTextVoiceStart,
            onLongTextVoiceStop = recordActions.onLongTextVoiceStop,
            onBack = {
                recordActions.onPregnancyCancel()
                onClose()
            },
            onSave = recordActions.onPregnancySave
        )
        BabyLogRoutes.RecordContractionSession -> ContractionSessionScreen(
            onBack = onClose,
            onSave = recordActions.onContractionSessionSave
        )
        BabyLogRoutes.RecordMaternalMetric -> MaternalMetricFormScreen(
            draft = recordState.maternalMetricDraft,
            isEditing = false,
            voiceState = smartVoiceState,
            onLongTextVoiceStart = recordActions.onLongTextVoiceStart,
            onLongTextVoiceStop = recordActions.onLongTextVoiceStop,
            onBack = {
                recordActions.onMaternalMetricCancel()
                onClose()
            },
            onSave = recordActions.onMaternalMetricSave
        )
        BabyLogRoutes.RecordUltrasound -> UltrasoundFormScreen(
            defaultGestationalAge = currentGestationalAgeInput(state.childProfile),
            expectedDueDate = state.childProfile.expectedDueDate,
            draft = recordState.ultrasoundDraft,
            isEditing = false,
            photoPath = recordState.pendingUltrasoundPhotoPath,
            photoName = recordState.pendingUltrasoundPhotoName,
            ocrRunning = recordState.ultrasoundOcrRunning,
            ocrCandidate = recordState.ultrasoundOcrCandidate,
            onPickPhoto = recordActions.onPickUltrasoundPhoto,
            onCapturePhoto = recordActions.onCaptureUltrasoundPhoto,
            onRecognizePhoto = recordActions.onRecognizeUltrasoundPhoto,
            onCandidateDismiss = recordActions.onDismissUltrasoundCandidate,
            onCandidateApplied = recordActions.onApplyUltrasoundCandidate,
            voiceState = smartVoiceState,
            onLongTextVoiceStart = recordActions.onLongTextVoiceStart,
            onLongTextVoiceStop = recordActions.onLongTextVoiceStop,
            onBack = {
                recordActions.onUltrasoundCancel()
                onClose()
            },
            onSave = recordActions.onUltrasoundSave
        )
    }
}
