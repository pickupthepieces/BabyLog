package app.babylog.nativeapp

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.material.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.HashSet
import java.util.Locale
import java.util.TimeZone

@Composable
internal fun VisitSummaryScreen(
    events: List<BabyLogDomain.BabyLogEvent>,
    attachments: List<BabyLogDomain.AttachmentRecord>,
    preVisitQuestions: List<BabyLogPreVisitQuestionStore.Question>,
    onBack: () -> Unit,
    onCopy: (String) -> Unit,
    onShare: (String) -> Unit,
    onSaveFile: (String) -> Unit,
    onPolish: (String, (String?) -> Unit) -> Unit
) {
    var range by rememberSaveable { mutableStateOf("all") }
    var customStart by rememberSaveable { mutableStateOf("") }
    var customEnd by rememberSaveable { mutableStateOf("") }
    var selectedCsv by rememberSaveable {
        mutableStateOf(BabyLogVisitSummaryExporter.DEFAULT_CATEGORIES.joinToString(","))
    }
    var preview by rememberSaveable { mutableStateOf("") }
    var original by rememberSaveable { mutableStateOf("") }
    var polishing by rememberSaveable { mutableStateOf(false) }
    var polished by rememberSaveable { mutableStateOf(false) }

    val selected = remember(selectedCsv) {
        selectedCsv.split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .toSet()
    }
    val rangeDates = remember(range, customStart, customEnd) {
        visitSummaryRangeDates(range, customStart, customEnd)
    }
    val generated = remember(events, attachments, preVisitQuestions, rangeDates, selectedCsv) {
        BabyLogVisitSummaryExporter.buildMarkdown(
            events,
            attachments,
            rangeDates.first,
            rangeDates.second,
            HashSet(selected),
            preVisitQuestions
        )
    }

    LaunchedEffect(generated) {
        original = generated
        preview = generated
        polished = false
        polishing = false
    }

    SettingsPageScaffold(
        title = "复诊汇总导出",
        subtitle = "可编辑 Markdown，复制或分享给医生前请人工核对",
        onBack = onBack
    ) {
        item {
            SettingsPanel("筛选") {
                VisitSummaryRangeSelector(
                    selected = range,
                    onSelect = { range = it }
                )
                if (range == "custom") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        DateInputRow(
                            label = "开始",
                            value = customStart,
                            onValueChange = { customStart = it },
                            modifier = Modifier.weight(1f)
                        )
                        DateInputRow(
                            label = "结束",
                            value = customEnd,
                            onValueChange = { customEnd = it },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                VisitSummaryCategorySelector(
                    selected = selected,
                    onToggle = { category ->
                        val next = selected.toMutableSet()
                        if (!next.add(category)) {
                            next.remove(category)
                        }
                        selectedCsv = BabyLogVisitSummaryExporter.DEFAULT_CATEGORIES
                            .filter { next.contains(it) }
                            .joinToString(",")
                    }
                )
            }
        }
        item {
            SettingsPanel("预览") {
                Text(
                    text = "只列已填写字段；筛查分级和风险值保留“报告原文”标注。",
                    color = ChestnutPalette.Muted,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                )
                TextField(
                    value = preview,
                    onValueChange = {
                        preview = it
                        polished = false
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                        .height(360.dp),
                    singleLine = false,
                    minLines = 16,
                    maxLines = 24,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Text),
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
                if (preview != original || polished) {
                    OutlinedButton(
                        onClick = {
                            preview = original
                            polished = false
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text("恢复原始模板", color = ChestnutPalette.Primary, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        item {
            SettingsPanel("导出") {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        VisitSummaryActionButton(
                            text = "复制",
                            modifier = Modifier.weight(1f),
                            onClick = { onCopy(preview) }
                        )
                        VisitSummaryActionButton(
                            text = "系统分享",
                            modifier = Modifier.weight(1f),
                            onClick = { onShare(preview) }
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        VisitSummaryActionButton(
                            text = "保存 .md",
                            modifier = Modifier.weight(1f),
                            onClick = { onSaveFile(preview) }
                        )
                        VisitSummaryActionButton(
                            text = if (polishing) "润色中..." else "用大模型润色",
                            modifier = Modifier.weight(1f),
                            enabled = !polishing,
                            primary = true,
                            onClick = {
                                polishing = true
                                onPolish(preview) { polishedText ->
                                    polishing = false
                                    if (!polishedText.isNullOrBlank()) {
                                        preview = polishedText
                                        polished = true
                                    }
                                }
                            }
                        )
                    }
                    Text(
                        text = "大模型润色只在你点按后发送当前预览文本；结果不会入库，可继续手动编辑。",
                        color = ChestnutPalette.Muted,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun VisitSummaryRangeSelector(
    selected: String,
    onSelect: (String) -> Unit
) {
    val options = listOf(
        "all" to "全部",
        "30" to "最近30天",
        "60" to "最近60天",
        "90" to "最近90天",
        "custom" to "自定义"
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        options.forEach { (key, label) ->
            VisitSummaryChip(
                label = label,
                active = selected == key,
                onClick = { onSelect(key) }
            )
        }
    }
}

@Composable
private fun VisitSummaryCategorySelector(
    selected: Set<String>,
    onToggle: (String) -> Unit
) {
    Column(Modifier.padding(horizontal = 14.dp, vertical = 8.dp)) {
        Text("记录类型", color = ChestnutPalette.Muted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            BabyLogVisitSummaryExporter.DEFAULT_CATEGORIES.forEach { category ->
                VisitSummaryChip(
                    label = BabyLogVisitSummaryExporter.categoryLabel(category),
                    active = selected.contains(category),
                    onClick = { onToggle(category) }
                )
            }
        }
    }
}

@Composable
private fun VisitSummaryChip(
    label: String,
    active: Boolean,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        border = BorderStroke(1.dp, if (active) ChestnutPalette.Primary else ChestnutPalette.Border),
        colors = ButtonDefaults.outlinedButtonColors(
            backgroundColor = if (active) ChestnutPalette.PrimarySoft else ChestnutPalette.Surface,
            contentColor = if (active) ChestnutPalette.Primary else ChestnutPalette.Ink
        )
    ) {
        Text(label, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun VisitSummaryActionButton(
    text: String,
    modifier: Modifier,
    enabled: Boolean = true,
    primary: Boolean = false,
    onClick: () -> Unit
) {
    if (primary) {
        Button(
            modifier = modifier,
            enabled = enabled,
            onClick = onClick,
            colors = ButtonDefaults.buttonColors(backgroundColor = ChestnutPalette.Primary)
        ) {
            Text(text, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }
    } else {
        OutlinedButton(
            modifier = modifier,
            enabled = enabled,
            onClick = onClick
        ) {
            Text(text, color = ChestnutPalette.Primary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }
    }
}

private fun visitSummaryRangeDates(
    range: String,
    customStart: String,
    customEnd: String
): Pair<String, String> {
    if (range == "custom") {
        return customStart to customEnd
    }
    val days = range.toIntOrNull() ?: return "" to ""
    return visitSummaryDateDaysAgo(days) to BabyLogFormatters.todayDateInput()
}

private fun visitSummaryDateDaysAgo(days: Int): String {
    val calendar = Calendar.getInstance(TimeZone.getTimeZone("Asia/Shanghai"), Locale.CHINA)
    calendar.add(Calendar.DAY_OF_YEAR, -days)
    val format = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    format.timeZone = TimeZone.getTimeZone("Asia/Shanghai")
    return format.format(calendar.time)
}
