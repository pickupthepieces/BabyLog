package app.babylog.nativeapp

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.AlertDialog
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
internal fun ReminderCenterScreen(
    reminders: List<BabyLogReminderStore.Reminder>,
    systemMuted: Boolean,
    notificationPermissionGranted: Boolean,
    onBack: () -> Unit,
    onRequestNotificationPermission: () -> Unit,
    onSaveUserReminder: (String?, String, String, String, String, Boolean, () -> Unit) -> Unit,
    onToggleReminder: (BabyLogReminderStore.Reminder, Boolean) -> Unit,
    onDismissReminder: (BabyLogReminderStore.Reminder) -> Unit,
    onCompleteReminder: (BabyLogReminderStore.Reminder) -> Unit,
    onDeleteReminder: (BabyLogReminderStore.Reminder) -> Unit
) {
    var editingId by rememberSaveable { mutableStateOf<String?>(null) }
    var title by rememberSaveable { mutableStateOf("") }
    var note by rememberSaveable { mutableStateOf("") }
    var dueDate by rememberSaveable { mutableStateOf(BabyLogFormatters.todayDateInput()) }
    var dueTime by rememberSaveable { mutableStateOf("09:00") }
    var enabled by rememberSaveable { mutableStateOf(true) }
    var errorText by rememberSaveable { mutableStateOf("") }
    var pendingDeleteId by rememberSaveable { mutableStateOf<String?>(null) }
    val pendingDelete = reminders.firstOrNull { it.id == pendingDeleteId }

    if (pendingDelete != null) {
        AlertDialog(
            onDismissRequest = { pendingDeleteId = null },
            title = { Text("删除提醒", color = ChestnutPalette.Ink, fontWeight = FontWeight.Bold) },
            text = { Text("删除后这条自定义提醒不会再显示。", color = ChestnutPalette.Muted) },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteReminder(pendingDelete)
                        pendingDeleteId = null
                    },
                    colors = ButtonDefaults.buttonColors(backgroundColor = ChestnutPalette.Danger)
                ) { Text("删除", color = Color.White) }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteId = null }) { Text("取消", color = ChestnutPalette.Muted) }
            },
            backgroundColor = ChestnutPalette.Bg
        )
    }

    SettingsPageScaffold(
        title = "提醒中心",
        subtitle = "本机提醒与待办",
        onBack = onBack
    ) {
        if (systemMuted) {
            item {
                SettingsPanel("提醒静音") {
                    Text(
                        text = "当前档案状态下，系统提醒已静音；数据、时间线和导出仍可使用。",
                        color = ChestnutPalette.Muted,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(14.dp)
                    )
                }
            }
        }
        if (!notificationPermissionGranted) {
            item {
                SettingsPanel("通知权限") {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "未开启系统通知权限时，提醒只在本页显示。",
                            color = ChestnutPalette.Muted,
                            fontSize = 13.sp
                        )
                        Button(
                            onClick = onRequestNotificationPermission,
                            colors = ButtonDefaults.buttonColors(backgroundColor = ChestnutPalette.Primary)
                        ) {
                            Text("允许通知", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
        item {
            SettingsPanel(if (editingId == null) "新建自定义提醒" else "编辑自定义提醒") {
                Column(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ChestnutTextField("标题", title, { title = it }, KeyboardType.Text)
                    ChestnutTextField(
                        "备注",
                        note,
                        { note = it },
                        KeyboardType.Text,
                        singleLine = false,
                        minLines = 2,
                        maxLines = 4
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        DateInputRow("日期", dueDate, { dueDate = it }, modifier = Modifier.weight(1f), allowClear = false)
                        ChestnutTextField("时间 HH:mm", dueTime, { dueTime = it }, KeyboardType.Text, modifier = Modifier.weight(1f))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            modifier = Modifier.weight(1f),
                            onClick = {
                                val cleanTitle = title.trim()
                                if (cleanTitle.isBlank()) {
                                    errorText = "请填写提醒标题"
                                    return@Button
                                }
                                if (!BabyLogFormatters.isValidDateInput(dueDate)) {
                                    errorText = "日期格式应为 yyyy-MM-dd"
                                    return@Button
                                }
                                if (!isValidReminderTimeInput(dueTime)) {
                                    errorText = "时间格式应为 HH:mm"
                                    return@Button
                                }
                                onSaveUserReminder(editingId, cleanTitle, note, dueDate, dueTime, enabled) {
                                    editingId = null
                                    title = ""
                                    note = ""
                                    dueDate = BabyLogFormatters.todayDateInput()
                                    dueTime = "09:00"
                                    enabled = true
                                    errorText = ""
                                }
                            },
                            colors = ButtonDefaults.buttonColors(backgroundColor = ChestnutPalette.Primary)
                        ) { Text("保存提醒", color = Color.White, fontWeight = FontWeight.Bold) }
                        OutlinedButton(
                            modifier = Modifier.weight(1f),
                            onClick = {
                                editingId = null
                                title = ""
                                note = ""
                                dueDate = BabyLogFormatters.todayDateInput()
                                dueTime = "09:00"
                                enabled = true
                                errorText = ""
                            }
                        ) { Text("清空", color = ChestnutPalette.Muted, fontWeight = FontWeight.Bold) }
                    }
                    if (errorText.isNotBlank()) {
                        Text(errorText, color = ChestnutPalette.Danger, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        item {
            SettingsPanel("提醒列表") {
                if (reminders.isEmpty()) {
                    Text(
                        text = "暂无提醒。",
                        color = ChestnutPalette.Muted,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(14.dp)
                    )
                } else {
                    reminders.sortedWith(compareBy<BabyLogReminderStore.Reminder> { it.dueAtIso.ifBlank { "9999" } }
                        .thenBy { it.title }).forEach { reminder ->
                        ReminderRow(
                            reminder = reminder,
                            systemMuted = systemMuted,
                            onEdit = {
                                editingId = reminder.id
                                title = reminder.title
                                note = reminder.note
                                dueDate = reminder.dueAtIso.take(10).ifBlank { BabyLogFormatters.todayDateInput() }
                                dueTime = reminder.dueAtIso.drop(11).take(5).ifBlank { "09:00" }
                                enabled = reminder.enabled
                            },
                            onToggle = { onToggleReminder(reminder, !reminder.enabled) },
                            onDismiss = { onDismissReminder(reminder) },
                            onComplete = { onCompleteReminder(reminder) },
                            onDelete = { pendingDeleteId = reminder.id }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ReminderRow(
    reminder: BabyLogReminderStore.Reminder,
    systemMuted: Boolean,
    onEdit: () -> Unit,
    onToggle: () -> Unit,
    onDismiss: () -> Unit,
    onComplete: () -> Unit,
    onDelete: () -> Unit
) {
    val isSystem = reminder.source == BabyLogReminderStore.SOURCE_SYSTEM
    val muted = isSystem && systemMuted
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(reminder.title, color = ChestnutPalette.Ink, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Text(
            listOf(
                reminderKindLabel(reminder.kind),
                reminder.dueAtIso.take(16).replace("T", " "),
                if (muted) "已静音" else if (!reminder.enabled) "已关闭" else "",
                if (reminder.dismissedAt.isNotBlank()) "已忽略" else "",
                if (reminder.completedAt.isNotBlank()) "已完成" else ""
            ).filter { it.isNotBlank() }.joinToString(" · "),
            color = ChestnutPalette.Muted,
            fontSize = 12.sp
        )
        if (reminder.note.isNotBlank()) {
            Text(reminder.note, color = ChestnutPalette.Muted, fontSize = 13.sp)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(modifier = Modifier.weight(1f), onClick = onToggle) {
                Text(if (reminder.enabled) "关闭" else "开启", color = ChestnutPalette.Primary, fontWeight = FontWeight.Bold)
            }
            OutlinedButton(modifier = Modifier.weight(1f), onClick = onDismiss) {
                Text("忽略", color = ChestnutPalette.Muted, fontWeight = FontWeight.Bold)
            }
            OutlinedButton(modifier = Modifier.weight(1f), onClick = onComplete) {
                Text("标已做", color = ChestnutPalette.Primary, fontWeight = FontWeight.Bold)
            }
        }
        if (!isSystem) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(modifier = Modifier.weight(1f), onClick = onEdit) {
                    Text("编辑", color = ChestnutPalette.Primary, fontWeight = FontWeight.Bold)
                }
                OutlinedButton(modifier = Modifier.weight(1f), onClick = onDelete) {
                    Text("删除", color = ChestnutPalette.Danger, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

private fun reminderKindLabel(kind: String): String {
    return when (kind) {
        BabyLogReminderStore.KIND_CHECKUP_TODO -> "产检"
        BabyLogReminderStore.KIND_SCREENING_WINDOW -> "专项"
        BabyLogReminderStore.KIND_FETAL_OBSERVATION_HINT -> "胎动观察"
        BabyLogReminderStore.KIND_BACKUP -> "备份"
        else -> "自定义"
    }
}

private fun isValidReminderTimeInput(value: String): Boolean {
    if (!value.matches(Regex("\\d{2}:\\d{2}"))) {
        return false
    }
    val hour = value.substring(0, 2).toIntOrNull() ?: return false
    val minute = value.substring(3, 5).toIntOrNull() ?: return false
    return hour in 0..23 && minute in 0..59
}
