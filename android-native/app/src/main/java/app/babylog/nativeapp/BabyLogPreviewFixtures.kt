@file:Suppress("MagicNumber", "MatchingDeclarationName", "TooManyFunctions")

package app.babylog.nativeapp

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import org.json.JSONObject

private const val PREVIEW_NOW = "2026-06-09T09:30:00.000+0800"

internal val PreviewInnerPadding = PaddingValues(0.dp)
internal val PreviewSmartVoiceState = SmartVoiceUiState(message = "预览：语音服务未连接")
internal val PreviewLongTextVoiceStart: LongTextVoiceStart = { onTranscript -> onTranscript("预览语音转写文本") }

@Composable
internal fun babyLogPreview(content: @Composable () -> Unit) {
    ChestnutTheme(content)
}

internal fun previewUiState(stage: String = BabyLogDomain.STAGE_PREGNANCY): BabyLogUiState {
    val events = previewEvents()
    val attachments = previewAttachments()
    return BabyLogUiState(
        dashboard = previewDashboard(events, attachments),
        timeline = events,
        trashEvents = listOf(previewTrashEvent()),
        attachments = attachments,
        syncConfig = previewBackendConfig(),
        childProfile = previewProfile(stage),
        preVisitQuestions = previewQuestions(),
        reminders = previewReminders(),
        notificationPermissionGranted = false,
        setupCompleted = true,
        disclaimerAccepted = true,
        hasImportUndoSnapshot = true,
        lastBackupExportMs = 1_780_940_800_000L
    )
}

internal fun previewProfile(stage: String = BabyLogDomain.STAGE_PREGNANCY): BabyLogDomain.ChildProfile {
    return if (stage == BabyLogDomain.STAGE_BABY) {
        BabyLogDomain.ChildProfile.createForNewFamily(
            "栗子",
            "female",
            "",
            "2026-04-20",
            stage,
            true
        )
    } else {
        BabyLogDomain.ChildProfile.createForNewFamily(
            "小栗子",
            "unknown",
            "2026-09-18",
            "",
            52.0,
            162.0,
            stage,
            true
        )
    }
}

internal fun previewEvents(): List<BabyLogDomain.BabyLogEvent> = listOf(
    previewEvent(
        id = "event_ultrasound_1",
        type = "ultrasound",
        occurredAt = "2026-06-08T09:20:00.000+0800",
        payload = JSONObject()
            .put("gestationalAge", "25+3")
            .put("bpdMm", "64")
            .put("flMm", "48")
            .put("efwG", "760")
            .put("amnioticFluid", "正常")
            .put("placenta", "后壁")
            .put("conclusion", "胎儿生长符合孕周")
    ),
    previewEvent(
        id = "event_checkup_1",
        type = "pregnancy_checkup",
        occurredAt = "2026-06-05T10:10:00.000+0800",
        payload = JSONObject()
            .put("provider", "宁波妇儿医院")
            .put("department", "产科门诊")
            .put("doctorConclusion", "血压、尿常规正常")
            .put("nextVisitDate", "2026-06-19")
    ),
    previewEvent(
        id = "event_metric_1",
        type = "maternal_metric",
        occurredAt = "2026-06-09T07:40:00.000+0800",
        payload = JSONObject()
            .put("weightKg", "59.6")
            .put("systolicBp", "112")
            .put("diastolicBp", "72")
            .put("glucoseMmolL", "5.4")
    ),
    previewEvent(
        id = "event_baby_feed_1",
        type = "feeding",
        occurredAt = "2026-06-09T08:30:00.000+0800",
        payload = JSONObject()
            .put("amountMl", "120")
            .put("method", "奶瓶")
            .put("note", "精神状态好")
    )
)

internal fun previewEvent(
    id: String = "event_preview",
    type: String = "note",
    occurredAt: String = PREVIEW_NOW,
    payload: JSONObject = JSONObject()
): BabyLogDomain.BabyLogEvent = BabyLogDomain.BabyLogEvent(
    id,
    BabyLogDomain.FAMILY_ID,
    BabyLogDomain.CHILD_ID,
    type,
    occurredAt,
    payload,
    if (type == "ultrasound") listOf("attachment_ultrasound_1") else emptyList(),
    "preview",
    occurredAt,
    occurredAt,
    BabyLogDomain.UPDATED_BY_LOCAL,
    BabyLogDomain.SCHEMA_VERSION,
    null
)

internal fun previewAttachments(): List<BabyLogDomain.AttachmentRecord> = listOf(
    previewAttachment("attachment_ultrasound_1", "B超报告.jpg", "ultrasound_image"),
    previewAttachment("attachment_checkup_1", "产检化验单.pdf", "checkup_file")
)

internal fun previewAttachment(
    id: String = "attachment_preview",
    name: String = "预览附件.jpg",
    kind: String = "other"
): BabyLogDomain.AttachmentRecord = BabyLogDomain.AttachmentRecord(
    id,
    BabyLogDomain.FAMILY_ID,
    BabyLogDomain.CHILD_ID,
    kind,
    name,
    if (name.endsWith(".pdf")) "application/pdf" else "image/jpeg",
    384_000L,
    "",
    1080,
    1440,
    "preview-hash-$id",
    "",
    "ready",
    PREVIEW_NOW,
    PREVIEW_NOW,
    BabyLogDomain.UPDATED_BY_LOCAL,
    BabyLogDomain.SCHEMA_VERSION,
    null
)

internal fun previewQuestions(): List<BabyLogPreVisitQuestionStore.Question> = listOf(
    BabyLogPreVisitQuestionStore.Question(
        "question_1",
        "下次产检是否需要提前空腹？",
        "2026-06-19",
        PREVIEW_NOW,
        PREVIEW_NOW
    ),
    BabyLogPreVisitQuestionStore.Question(
        "question_2",
        "夜间胎动变少时需要观察多久？",
        "2026-06-19",
        PREVIEW_NOW,
        PREVIEW_NOW
    )
)

internal fun previewReminders(): List<BabyLogReminderStore.Reminder> = listOf(
    BabyLogReminderStore.Reminder(
        "reminder_checkup",
        BabyLogReminderStore.KIND_CHECKUP_TODO,
        "明天产检",
        "带上上次 B 超单和空腹抽血单",
        "2026-06-10T08:30:00.000+0800",
        "",
        BabyLogReminderStore.SOURCE_SYSTEM,
        true,
        "",
        "",
        PREVIEW_NOW,
        PREVIEW_NOW
    ),
    BabyLogReminderStore.Reminder(
        "reminder_custom",
        BabyLogReminderStore.KIND_USER_CUSTOM,
        "晚间胎动计数",
        "睡前完成一次胎动记录",
        "2026-06-09T21:30:00.000+0800",
        "",
        BabyLogReminderStore.SOURCE_USER,
        true,
        "",
        "",
        PREVIEW_NOW,
        PREVIEW_NOW
    )
)

internal fun previewDashboard(
    events: List<BabyLogDomain.BabyLogEvent> = previewEvents(),
    attachments: List<BabyLogDomain.AttachmentRecord> = previewAttachments()
): BabyLogService.DashboardSnapshot = BabyLogService.DashboardSnapshot(
    events.take(3),
    attachments,
    mapOf("ultrasound" to 1, "maternal_metric" to 1, "feeding" to 1),
    2,
    0,
    8,
    1,
    384_000L,
    0,
    "2026-06-09 08:55",
    1,
    24_576_000L,
    8_589_934_592L
)

internal fun previewBackendConfig(): BabyLogDomain.BackendConfig = BabyLogDomain.BackendConfig(
    true,
    "https://babylog.example.com",
    "cn-east",
    "2026-06-09 08:55"
)

internal fun previewTrashEvent(): BabyLogDomain.BabyLogEvent = previewEvent(
    id = "event_trash_1",
    type = "note",
    payload = JSONObject().put("note", "已删除的预览记录")
).withDeletedAt("2026-06-08T20:00:00.000+0800")
