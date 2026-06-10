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
internal fun PreVisitQuestionsScreen(
    questions: List<BabyLogPreVisitQuestionStore.Question>,
    onBack: () -> Unit,
    onSave: (String?, String, String, () -> Unit) -> Unit,
    onDelete: (BabyLogPreVisitQuestionStore.Question) -> Unit
) {
    var editingId by rememberSaveable { mutableStateOf<String?>(null) }
    var questionText by rememberSaveable { mutableStateOf("") }
    var visitDate by rememberSaveable { mutableStateOf("") }
    var errorText by rememberSaveable { mutableStateOf("") }
    var pendingDeleteId by rememberSaveable { mutableStateOf<String?>(null) }
    val pendingDelete = questions.firstOrNull { it.id == pendingDeleteId }

    if (pendingDelete != null) {
        AlertDialog(
            onDismissRequest = { pendingDeleteId = null },
            title = { Text("删除问题", color = ChestnutPalette.Ink, fontWeight = FontWeight.Bold) },
            text = { Text("删除后不会出现在复诊汇总中。", color = ChestnutPalette.Muted) },
            confirmButton = {
                Button(
                    onClick = {
                        onDelete(pendingDelete)
                        pendingDeleteId = null
                    },
                    colors = ButtonDefaults.buttonColors(backgroundColor = ChestnutPalette.Danger)
                ) {
                    Text("删除", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteId = null }) {
                    Text("取消", color = ChestnutPalette.Muted)
                }
            },
            backgroundColor = ChestnutPalette.Bg
        )
    }

    SettingsPageScaffold(
        title = "待问问题",
        subtitle = "复诊前整理要点，汇总时自动带上",
        onBack = onBack
    ) {
        item {
            SettingsPanel(if (editingId == null) "新增问题" else "编辑问题") {
                Column(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ChestnutTextField(
                        label = "问题",
                        value = questionText,
                        onValueChange = { questionText = it },
                        keyboardType = KeyboardType.Text,
                        singleLine = false,
                        minLines = 3,
                        maxLines = 5,
                        placeholder = "例如：这次报告里哪些项目需要医生解释？"
                    )
                    DateInputRow(
                        label = "关联产检日期",
                        value = visitDate,
                        onValueChange = { visitDate = it }
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                        modifier = Modifier.weight(1f),
                        onClick = {
                            val cleanQuestion = questionText.trim()
                            if (cleanQuestion.isBlank()) {
                                errorText = "请先填写想问的问题"
                                return@Button
                            }
                            if (visitDate.isNotBlank() && !BabyLogFormatters.isValidDateInput(visitDate)) {
                                errorText = "日期格式应为 yyyy-MM-dd"
                                return@Button
                            }
                            onSave(editingId, questionText, visitDate) {
                                editingId = null
                                questionText = ""
                                visitDate = ""
                                errorText = ""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(backgroundColor = ChestnutPalette.Primary)
                    ) {
                            Text("保存问题", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                        OutlinedButton(
                            modifier = Modifier.weight(1f),
                            onClick = {
                                editingId = null
                                questionText = ""
                                visitDate = ""
                                errorText = ""
                            }
                        ) {
                            Text("清空", color = ChestnutPalette.Muted, fontWeight = FontWeight.Bold)
                        }
                    }
                    Text(
                        text = "仅用于复诊沟通清单，不写入时间线。",
                        color = ChestnutPalette.Muted,
                        fontSize = 12.sp
                    )
                    if (errorText.isNotBlank()) {
                        Text(errorText, color = ChestnutPalette.Danger, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        item {
            SettingsPanel("待问清单") {
                if (questions.isEmpty()) {
                    Text(
                        text = "还没有记录问题。",
                        color = ChestnutPalette.Muted,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(14.dp)
                    )
                } else {
                    questions.forEach { question ->
                        PreVisitQuestionRow(
                            question = question,
                            onEdit = {
                                editingId = question.id
                                questionText = question.text
                                visitDate = question.visitDate
                            },
                            onDelete = { pendingDeleteId = question.id }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PreVisitQuestionRow(
    question: BabyLogPreVisitQuestionStore.Question,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(question.text, color = ChestnutPalette.Ink, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        if (question.visitDate.isNotBlank()) {
            Text("关联日期 ${question.visitDate}", color = ChestnutPalette.Muted, fontSize = 12.sp)
        }
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
