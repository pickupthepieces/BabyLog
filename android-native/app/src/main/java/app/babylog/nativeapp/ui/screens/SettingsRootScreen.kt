package app.babylog.nativeapp

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable

@Composable
internal fun SettingsRootScreen(
    inner: PaddingValues,
    state: BabyLogUiState,
    smartConfigSummary: String,
    speechConfigSummary: String,
    onSyncNow: () -> Unit,
    onExportBackup: () -> Unit,
    onImportBackup: () -> Unit,
    onOpenSyncSettings: () -> Unit,
    onOpenSmartSettings: () -> Unit,
    onOpenSpeechSettings: () -> Unit,
    onClearLocalData: () -> Unit,
    onOpenTrash: () -> Unit,
    onEditProfile: () -> Unit
) {
    BabyLogScreenColumn(inner) {
        item {
            SettingsScreen(
                state = state,
                onSyncNow = onSyncNow,
                onExportBackup = onExportBackup,
                onImportBackup = onImportBackup,
                onOpenSyncSettings = onOpenSyncSettings,
                onOpenSmartSettings = onOpenSmartSettings,
                onOpenSpeechSettings = onOpenSpeechSettings,
                smartConfigSummary = smartConfigSummary,
                speechConfigSummary = speechConfigSummary,
                onClearLocalData = onClearLocalData,
                onOpenTrash = onOpenTrash,
                onEditProfile = onEditProfile
            )
        }
    }
}
