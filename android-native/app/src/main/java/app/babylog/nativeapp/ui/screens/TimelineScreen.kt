package app.babylog.nativeapp

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale

@Composable
internal fun TimelineScreen(
    inner: PaddingValues,
    state: BabyLogUiState,
    selectedFilter: String,
    highlightedEventId: String?,
    syncPulling: Boolean,
    onFilterSelected: (String) -> Unit,
    onPullSyncNow: () -> Unit,
    onDismissSyncBanner: () -> Unit,
    onOpenDetail: (BabyLogDomain.BabyLogEvent) -> Unit
) {
    var keyword by rememberSaveable { mutableStateOf("") }
    var startDate by rememberSaveable { mutableStateOf("") }
    var endDate by rememberSaveable { mutableStateOf("") }
    var searchExpanded by rememberSaveable { mutableStateOf(false) }
    val events = remember(state.timeline, selectedFilter, keyword, startDate, endDate) {
        state.timeline.filter { event ->
            BabyLogFormatters.matchesTimelineFilter(event.eventType, selectedFilter) &&
                eventMatchesTimelineSearch(event, keyword, startDate, endDate)
        }
    }
    val remoteUpdateCount = state.dashboard?.remoteUpdateBannerCount ?: 0
    BabyLogPullRefreshContainer(
        refreshing = syncPulling,
        onRefresh = onPullSyncNow
    ) {
        BabyLogScreenColumn(inner) {
            if (remoteUpdateCount > 0) {
                item {
                    ChestnutSyncBanner(
                        count = remoteUpdateCount,
                        onDismiss = onDismissSyncBanner
                    )
                }
            }
            item {
                TimelineFilters(
                    selected = selectedFilter,
                    onSelect = onFilterSelected
                )
            }
            item {
                TimelineSearchPanel(
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
            if (events.isEmpty()) {
                item { EmptyPanel("没有匹配的记录。") }
            } else {
                items(events, key = { it.id }) { event ->
                    TimelineRow(
                        event,
                        highlighted = event.id == highlightedEventId,
                        onClick = { onOpenDetail(event) }
                    )
                }
            }
        }
    }
}

@Composable
private fun TimelineSearchPanel(
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
        if (expanded) {
            Spacer(Modifier.height(12.dp))
            ChestnutTextField(
                label = "关键词",
                value = keyword,
                onValueChange = onKeywordChange,
                keyboardType = KeyboardType.Text,
                placeholder = "类型、摘要、医生结论、数值"
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

private fun eventMatchesTimelineSearch(
    event: BabyLogDomain.BabyLogEvent,
    keyword: String,
    startDate: String,
    endDate: String
): Boolean {
    val eventDate = dateToken(event.occurredAt)
    if (startDate.isNotBlank() && eventDate.isNotBlank() && eventDate < startDate) {
        return false
    }
    if (endDate.isNotBlank() && eventDate.isNotBlank() && eventDate > endDate) {
        return false
    }
    val needle = keyword.trim().lowercase(Locale.ROOT)
    if (needle.isBlank()) {
        return true
    }
    val haystack = listOf(
        BabyLogFormatters.eventLabel(event.eventType),
        BabyLogFormatters.eventSummary(event),
        event.eventType,
        event.occurredAt,
        event.payload.toString()
    ).joinToString(" ").lowercase(Locale.ROOT)
    return haystack.contains(needle)
}

private fun dateToken(iso: String?): String {
    if (iso.isNullOrBlank() || iso.length < 10) {
        return ""
    }
    return iso.substring(0, 10)
}
