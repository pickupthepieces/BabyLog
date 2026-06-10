@file:Suppress("FunctionNaming", "MatchingDeclarationName", "UnusedPrivateMember")

package app.babylog.nativeapp

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun attachmentListScreenPreview() {
    babyLogPreview {
        AttachmentListScreen(
            state = AttachmentListPageState("产检附件", previewAttachments()),
            onBack = {},
            onPreview = {}
        )
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun attachmentPreviewScreenPreview() {
    babyLogPreview {
        AttachmentPreviewScreen(
            attachment = previewAttachment(name = "B超报告.jpg", kind = "ultrasound_image"),
            onBack = {}
        )
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun trashScreenPreview() {
    babyLogPreview {
        TrashScreen(
            events = listOf(previewTrashEvent()),
            onBack = {},
            onRestore = {}
        )
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun visitSummaryScreenPreview() {
    babyLogPreview {
        VisitSummaryScreen(
            events = previewEvents(),
            attachments = previewAttachments(),
            preVisitQuestions = previewQuestions(),
            onBack = {},
            onCopy = {},
            onShare = {},
            onSaveFile = {},
            onPolish = { _, done -> done(null) }
        )
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun weightGainScreenPreview() {
    babyLogPreview {
        WeightGainScreen(
            profile = previewProfile(),
            events = previewEvents(),
            onBack = {},
            onEditProfile = {}
        )
    }
}
