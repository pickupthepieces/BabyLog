@file:Suppress("FunctionNaming", "LongMethod", "CyclomaticComplexMethod", "MagicNumber")

package app.babylog.nativeapp

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Card
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
@Composable
internal fun WeekCard(profile: BabyLogDomain.ChildProfile) {
    val dueDate = profile.expectedDueDate
    val validDueDate = BabyLogFormatters.isValidDateInput(dueDate)
    val daysToDue = if (validDueDate) daysBetween(BabyLogFormatters.todayDateInput(), dueDate) else 0
    val gestationalDays = if (validDueDate) (280 - daysToDue).coerceIn(0, 280) else -1
    Card(
        shape = RoundedCornerShape(ChestnutRadius.Card),
        backgroundColor = ChestnutPalette.Surface,
        border = BorderStroke(1.dp, ChestnutPalette.Border.copy(alpha = 0.45f)),
        elevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        if (gestationalDays >= 0) BabyLogFormatters.formatGestationalAge(gestationalDays) else "孕期档案待补全",
                        color = ChestnutPalette.Ink,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.sp
                    )
                    Text(
                        if (validDueDate) "预产期 $dueDate" else "可在设置里补录预产期",
                        color = ChestnutPalette.Muted,
                        fontSize = 14.sp
                    )
                }
                Text(
                    if (validDueDate) "还剩 $daysToDue 天" else "待补",
                    color = ChestnutPalette.Primary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Spacer(Modifier.height(14.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(ChestnutPalette.Surface2)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(if (gestationalDays >= 0) (gestationalDays / 280f).coerceIn(0.03f, 1f) else 0.03f)
                        .height(4.dp)
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
        shape = RoundedCornerShape(ChestnutRadius.Card),
        backgroundColor = ChestnutPalette.Surface,
        border = BorderStroke(1.dp, ChestnutPalette.Border.copy(alpha = 0.45f)),
        elevation = 0.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text("${nickname}的日视图", color = ChestnutPalette.Ink, fontSize = 28.sp, fontWeight = FontWeight.Bold)
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
            Text(
                "记录第一次产检或 B 超后显示摘要和待复核提醒。",
                color = ChestnutPalette.Muted,
                fontSize = 14.sp,
                lineHeight = 20.sp
            )
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
                text = pendingReview?.let { "待复核 B 超：${BabyLogFormatters.eventSummary(it)}。请核对报告原文。" }
                    ?: "有 B 超指标超出常用软范围，请核对报告原文。",
                color = ChestnutPalette.Danger,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
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
                color = if (active) ChestnutPalette.Primary else ChestnutPalette.Muted,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(if (active) ChestnutPalette.PrimarySoft else ChestnutPalette.Surface)
                    .border(
                        1.dp,
                        if (active) ChestnutPalette.Primary.copy(alpha = 0.32f) else ChestnutPalette.Border.copy(alpha = 0.54f),
                        CircleShape
                    )
                    .clickable { onSelect(key) }
                    .padding(horizontal = 15.dp, vertical = 9.dp)
            )
        }
    }
}

@Composable
internal fun TimelineRow(
    event: BabyLogDomain.BabyLogEvent,
    highlighted: Boolean = false,
    onClick: (() -> Unit)? = null
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
    val interactionSource = remember { MutableInteractionSource() }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .babyLogPressScale(interactionSource, pressedScale = 0.985f)
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = null
                    ) { onClick() }
                } else {
                    Modifier
                }
            ),
        shape = RoundedCornerShape(ChestnutRadius.Card),
        backgroundColor = if (highlighted) ChestnutPalette.PrimarySoft.copy(alpha = 0.52f) else ChestnutPalette.Surface,
        border = BorderStroke(
            if (highlighted) 1.4.dp else 1.dp,
            if (highlighted) ChestnutPalette.Primary.copy(alpha = 0.42f) else ChestnutPalette.Border.copy(alpha = 0.36f)
        ),
        elevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 92.dp)
                .padding(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            BabyLogIconTile(
                icon = quickActionIcon(event.eventType),
                tint = tone,
                tileColor = tone.copy(alpha = 0.12f),
                modifier = Modifier.size(46.dp),
                iconSize = 24.dp
            )
            Spacer(Modifier.width(12.dp))
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(top = 1.dp)
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
                Spacer(Modifier.height(8.dp))
                Text(
                    text = rowText.summary,
                    color = ChestnutPalette.Ink,
                    fontSize = 16.sp,
                    lineHeight = 22.sp,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
                if (event.attachmentIds.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = rowText.attachmentLabel,
                        color = ChestnutPalette.Primary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
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
        LibraryEntry("出生证明", "规划中", "出生资料归档", LineIcon.File, "other", null),
        LibraryEntry("疫苗本", attachmentCount(vaccineAttachments), "出生后启用；可显示已导入附件", LineIcon.Vaccine, "vaccine_image", vaccineAttachments)
    )
    val babyEntries = listOf(
        LibraryEntry("出生证明", "规划中", "出生资料归档", LineIcon.File, "other", null),
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

@Suppress("LongParameterList", "LongMethod", "FunctionNaming")
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
    appVersionLabel: String,
    appUpdateStatus: String,
    appUpdateRunning: Boolean,
    onCheckAppUpdate: () -> Unit,
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
            SettingsDivider()
            ActionRow(
                title = "记录阶段",
                subtitle = stageLabel(currentCareStage(state.childProfile)),
                action = "编辑",
                onClick = onEditProfile
            )
            SettingsDivider()
            ActionRow(
                title = "预产期 / 出生日期",
                subtitle = "预产期 ${state.childProfile.expectedDueDate.ifBlank { "待补" }} · 出生 ${state.childProfile.birthDate.ifBlank { "待补" }}",
                action = "编辑",
                onClick = onEditProfile
            )
            SettingsDivider()
            ActionRow(
                title = "阶段覆盖",
                subtitle = stageOverrideLabel(state.childProfile.stageOverride),
                action = "编辑",
                onClick = onEditProfile
            )
            SettingsDivider()
            ActionRow(
                title = "预产期计算器",
                subtitle = "LMP、周期与早期 B 超 CRL 推算",
                action = "打开",
                onClick = onOpenDueDateCalculator
            )
            SettingsDivider()
            ActionRow(
                title = "孕期增重曲线",
                subtitle = "IOM 参考带与体重历史",
                action = "查看",
                onClick = onOpenWeightGain
            )
            SettingsDivider()
            ActionRow(
                title = "待问问题",
                subtitle = if (state.preVisitQuestions.isEmpty()) "复诊前整理要点" else "${state.preVisitQuestions.size} 条待问",
                action = "管理",
                onClick = onOpenPreVisitQuestions
            )
        }
        SettingsPanel("提醒") {
            ActionRow(
                title = "提醒中心",
                subtitle = "${state.reminders.count { BabyLogReminderStore.isActionable(it) }} 条提醒",
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
            SettingsDivider()
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
            SettingsDivider()
            ActionRow(
                title = "语音转文字",
                subtitle = speechConfigSummary,
                action = "设置",
                onClick = onOpenSpeechSettings
            )
        }
        SettingsPanel("备份") {
            ActionRow(
                title = "导出栗记 JSON",
                subtitle = BabyLogFormatters.formatBackupAgeLabel(state.lastBackupExportMs, System.currentTimeMillis()),
                action = "导出",
                onClick = onExportBackup
            )
            SettingsDivider()
            ActionRow(
                title = "导入栗记 JSON",
                subtitle = "覆盖本机记录、附件和同步队列",
                action = "导入",
                onClick = onImportBackup
            )
            SettingsDivider()
            ActionRow(
                title = "撤销上次导入",
                subtitle = if (state.hasImportUndoSnapshot) "恢复到最近一次导入前的本机快照" else "暂无可撤销的导入快照",
                action = "撤销",
                actionColor = ChestnutPalette.Danger,
                onClick = if (state.hasImportUndoSnapshot) onUndoImport else null
            )
        }
        SettingsPanel("应用") {
            ActionRow(
                title = "应用更新",
                subtitle = "当前版本 $appVersionLabel · $appUpdateStatus",
                action = if (appUpdateRunning) "检查中" else "检查",
                onClick = if (appUpdateRunning) null else onCheckAppUpdate
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
            SettingsDivider()
            ActionRow(
                title = "医疗免责声明",
                subtitle = "家庭记录边界与 AI 候选说明",
                action = "查看",
                onClick = onOpenDisclaimer
            )
            SettingsDivider()
            ActionRow(
                title = "本机占用",
                subtitle = "记录和附件 ${BabyLogFormatters.formatByteSize(state.dashboard?.localBytes ?: 0)}",
                action = "查看",
                onClick = null
            )
            SettingsDivider()
            ActionRow(
                title = "清空本机数据",
                subtitle = "删除记录、附件和待同步队列",
                action = "清空",
                actionColor = ChestnutPalette.Danger,
                onClick = onClearLocalData
            )
        }
        Text(
            text = "栗记用于家庭记录和资料整理；完整边界见医疗免责声明。",
            color = Color(0xFF7C4A21),
            modifier = Modifier
                .clip(RoundedCornerShape(ChestnutRadius.Control))
                .background(Color(0xFFFFEBCB))
                .padding(14.dp)
        )
    }
}
