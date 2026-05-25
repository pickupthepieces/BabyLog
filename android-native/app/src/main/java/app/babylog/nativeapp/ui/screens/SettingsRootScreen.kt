package app.babylog.nativeapp

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable

@Suppress("LongParameterList", "FunctionNaming")
@Composable
internal fun SettingsRootScreen(
    inner: PaddingValues,
    state: BabyLogUiState,
    smartConfigSummary: String,
    speechConfigSummary: String,
    onSyncNow: () -> Unit,
    onExportBackup: () -> Unit,
    onImportBackup: () -> Unit,
    onUndoImport: () -> Unit,
    onOpenSyncSettings: () -> Unit,
    onOpenSmartSettings: () -> Unit,
    onOpenSpeechSettings: () -> Unit,
    onClearLocalData: () -> Unit,
    onOpenTrash: () -> Unit,
    onOpenDisclaimer: () -> Unit,
    onOpenDueDateCalculator: () -> Unit,
    onOpenWeightGain: () -> Unit,
    appVersionLabel: String,
    appUpdateStatus: String,
    appUpdateRunning: Boolean,
    onCheckAppUpdate: () -> Unit,
    onOpenPreVisitQuestions: () -> Unit,
    onOpenReminderCenter: () -> Unit,
    onEditProfile: () -> Unit
) {
    BabyLogScreenColumn(inner) {
        item {
            SettingsScreen(
                state = state,
                onSyncNow = onSyncNow,
                onExportBackup = onExportBackup,
                onImportBackup = onImportBackup,
                onUndoImport = onUndoImport,
                onOpenSyncSettings = onOpenSyncSettings,
                onOpenSmartSettings = onOpenSmartSettings,
                onOpenSpeechSettings = onOpenSpeechSettings,
                smartConfigSummary = smartConfigSummary,
                speechConfigSummary = speechConfigSummary,
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
    }
}
