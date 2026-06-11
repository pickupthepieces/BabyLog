package app.babylog.nativeapp

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale

@Composable
internal fun LibraryRootScreen(
    inner: PaddingValues,
    state: BabyLogUiState,
    onShowAttachments: (String, List<BabyLogDomain.AttachmentRecord>) -> Unit,
    onOpenVisitSummary: () -> Unit,
    onOpenPreVisitQuestions: () -> Unit
) {
    var keyword by rememberSaveable { mutableStateOf("") }
    var typeFilter by rememberSaveable { mutableStateOf("all") }
    var startDate by rememberSaveable { mutableStateOf("") }
    var endDate by rememberSaveable { mutableStateOf("") }
    var searchExpanded by rememberSaveable { mutableStateOf(false) }
    val filteredAttachments = remember(state.attachments, keyword, typeFilter, startDate, endDate) {
        state.attachments.filter { attachment ->
            attachmentMatchesLibrarySearch(attachment, keyword, typeFilter, startDate, endDate)
        }
    }
    BabyLogScreenColumn(inner) {
        item {
            LibraryTypeFilters(selected = typeFilter, onSelect = { typeFilter = it })
        }
        item {
            LibrarySearchPanel(
                expanded = searchExpanded,
                onToggleExpanded = { searchExpanded = !searchExpanded },
                keyword = keyword,
                onKeywordChange = { keyword = it },
                startDate = startDate,
                onStartDateChange = { startDate = it },
                endDate = endDate,
                onEndDateChange = { endDate = it },
                onClear = {
                    keyword = ""
                    startDate = ""
                    endDate = ""
                }
            )
        }
        item {
            SettingsPanel("复诊资料") {
                ActionRow(
                    title = "复诊汇总导出",
                    subtitle = "整理产检、B 超、筛查与孕妈指标",
                    action = "打开",
                    onClick = onOpenVisitSummary
                )
                SettingsDivider()
                ActionRow(
                    title = "待问问题",
                    subtitle = if (state.preVisitQuestions.isEmpty()) "复诊前先记下想问的事" else "${state.preVisitQuestions.size} 条待问",
                    action = "管理",
                    onClick = onOpenPreVisitQuestions
                )
            }
        }
        item {
            LibraryScreen(
                attachments = filteredAttachments,
                stage = currentCareStage(state.childProfile),
                typeFilter = typeFilter,
                onShowAttachments = onShowAttachments
            )
        }
    }
}

@Composable
private fun LibrarySearchPanel(
    expanded: Boolean,
    onToggleExpanded: () -> Unit,
    keyword: String,
    onKeywordChange: (String) -> Unit,
    startDate: String,
    onStartDateChange: (String) -> Unit,
    endDate: String,
    onEndDateChange: (String) -> Unit,
    onClear: () -> Unit
) {
    val filterActive = keyword.isNotBlank() || startDate.isNotBlank() || endDate.isNotBlank()
    Panel {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onToggleExpanded() },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "搜索与日期筛选",
                color = ChestnutPalette.Ink,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
            if (filterActive && !expanded) {
                Chip(
                    text = "筛选中",
                    bg = ChestnutPalette.PrimarySoft,
                    fg = ChestnutPalette.Primary
                )
                Spacer(Modifier.width(10.dp))
            }
            Text(
                if (expanded) "收起" else "展开",
                color = ChestnutPalette.Primary,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column {
                Spacer(Modifier.height(12.dp))
                ChestnutTextField(
                    label = "关键词",
                    value = keyword,
                    onValueChange = onKeywordChange,
                    keyboardType = KeyboardType.Text,
                    placeholder = "文件名、类型、日期"
                )
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    DateInputRow(
                        label = "开始日期",
                        value = startDate,
                        onValueChange = onStartDateChange,
                        modifier = Modifier.weight(1f)
                    )
                    DateInputRow(
                        label = "结束日期",
                        value = endDate,
                        onValueChange = onEndDateChange,
                        modifier = Modifier.weight(1f)
                    )
                }
                if (filterActive) {
                    Spacer(Modifier.height(4.dp))
                    TextButton(onClick = onClear) {
                        Text("清空筛选条件", color = ChestnutPalette.Muted, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun LibraryTypeFilters(selected: String, onSelect: (String) -> Unit) {
    val options = listOf(
        "all" to "全部",
        "ultrasound_image" to "B 超单",
        "document_image" to "检查单",
        "vaccine_image" to "疫苗本"
    )
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        options.forEach { (key, label) ->
            val active = selected == key
            val chipFg by animateColorAsState(
                targetValue = if (active) ChestnutPalette.Primary else ChestnutPalette.Muted,
                label = "libraryFilterFg"
            )
            val chipBg by animateColorAsState(
                targetValue = if (active) ChestnutPalette.PrimarySoft else ChestnutPalette.Surface,
                label = "libraryFilterBg"
            )
            val chipBorder by animateColorAsState(
                targetValue = if (active) {
                    ChestnutPalette.Primary.copy(alpha = 0.32f)
                } else {
                    ChestnutPalette.Border.copy(alpha = 0.54f)
                },
                label = "libraryFilterBorder"
            )
            Text(
                label,
                color = chipFg,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(chipBg)
                    .border(1.dp, chipBorder, CircleShape)
                    .clickable { onSelect(key) }
                    .padding(horizontal = 15.dp, vertical = 9.dp)
            )
        }
    }
}

private fun attachmentMatchesLibrarySearch(
    attachment: BabyLogDomain.AttachmentRecord,
    keyword: String,
    typeFilter: String,
    startDate: String,
    endDate: String
): Boolean {
    if (typeFilter != "all") {
        val isOther = typeFilter == "other" &&
            attachment.kind != "ultrasound_image" &&
            attachment.kind != "document_image" &&
            attachment.kind != "vaccine_image"
        if (!isOther && attachment.kind != typeFilter) {
            return false
        }
    }
    val createdDate = attachment.createdAt.toDateToken()
    if (startDate.isNotBlank() && createdDate.isNotBlank() && createdDate < startDate) {
        return false
    }
    if (endDate.isNotBlank() && createdDate.isNotBlank() && createdDate > endDate) {
        return false
    }
    val needle = keyword.trim().lowercase(Locale.ROOT)
    if (needle.isBlank()) {
        return true
    }
    val haystack = listOf(
        attachment.originalName,
        attachment.kind,
        attachment.mimeType,
        attachment.createdAt,
        attachment.ocrStatus,
        attachment.localPath
    ).joinToString(" ").lowercase(Locale.ROOT)
    return haystack.contains(needle)
}

private fun String?.toDateToken(): String {
    val value = this ?: return ""
    if (value.isBlank() || value.length < 10) {
        return ""
    }
    return value.substring(0, 10)
}
