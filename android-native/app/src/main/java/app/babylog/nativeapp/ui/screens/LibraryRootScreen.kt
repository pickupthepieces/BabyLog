package app.babylog.nativeapp

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable

@Composable
internal fun LibraryRootScreen(
    inner: PaddingValues,
    state: BabyLogUiState,
    onShowAttachments: (String, List<BabyLogDomain.AttachmentRecord>) -> Unit
) {
    BabyLogScreenColumn(inner) {
        item {
            LibraryScreen(
                attachments = state.attachments,
                stage = currentCareStage(state.childProfile),
                onShowAttachments = onShowAttachments
            )
        }
    }
}
