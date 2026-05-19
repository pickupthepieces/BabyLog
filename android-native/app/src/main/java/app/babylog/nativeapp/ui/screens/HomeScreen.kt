package app.babylog.nativeapp

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import app.babylog.nativeapp.ui.screens.BabyLogRoutes
import kotlinx.coroutines.flow.distinctUntilChanged

@Composable
internal fun HomeScreen(
    inner: PaddingValues,
    state: BabyLogUiState,
    selectedBabyDay: String,
    highlightedEventId: String?,
    onBabyDaySelected: (String) -> Unit,
    onShowTimeline: () -> Unit,
    onOpenDetail: (BabyLogDomain.BabyLogEvent) -> Unit,
    onEditEvent: (BabyLogDomain.BabyLogEvent) -> Unit,
    onDeleteEvent: (BabyLogDomain.BabyLogEvent) -> Unit,
    onQuickRailVisibilityChange: (Boolean) -> Unit
) {
    val stage = currentCareStage(state.childProfile)
    val listState = rememberLazyListState()
    val currentOnQuickRailVisibilityChange by rememberUpdatedState(onQuickRailVisibilityChange)
    val railTargetVisible = remember { mutableStateOf(true) }
    val railNestedScroll = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                when {
                    available.y < -6f && railTargetVisible.value -> railTargetVisible.value = false
                    available.y > 6f && !railTargetVisible.value -> railTargetVisible.value = true
                }
                return Offset.Zero
            }
        }
    }
    LaunchedEffect(Unit) {
        snapshotFlow { railTargetVisible.value }
            .distinctUntilChanged()
            .collect { visible -> currentOnQuickRailVisibilityChange(visible) }
    }
    LaunchedEffect(listState) {
        snapshotFlow {
            val atTop = listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset == 0
            !listState.isScrollInProgress || atTop
        }.distinctUntilChanged().collect { shouldShow ->
            if (shouldShow) {
                railTargetVisible.value = true
            }
        }
    }
    BabyLogScreenColumn(
        inner = inner,
        modifier = Modifier.nestedScroll(railNestedScroll),
        listState = listState
    ) {
        item {
            if (stage == BabyLogDomain.STAGE_BABY) {
                BabyDayCard(
                    profile = state.childProfile,
                    selectedDay = selectedBabyDay,
                    onPreviousDay = {
                        onBabyDaySelected(BabyLogFormatters.offsetDateInput(selectedBabyDay, -1))
                    },
                    onToday = { onBabyDaySelected(BabyLogFormatters.todayDateInput()) },
                    onNextDay = {
                        onBabyDaySelected(BabyLogFormatters.offsetDateInput(selectedBabyDay, 1))
                    }
                )
            } else {
                WeekCard(state.childProfile)
            }
        }
        if (stage == BabyLogDomain.STAGE_BABY) {
            val dayEvents = state.timeline.filter {
                BabyLogFormatters.recordDay(it.occurredAt) == selectedBabyDay && isEventVisibleInHome(it, stage)
            }
            item { BabyDaySummary(dayEvents, selectedBabyDay) }
            item {
                SectionHeader(
                    title = if (selectedBabyDay == BabyLogFormatters.todayDateInput()) "今天记录" else "当天记录"
                )
            }
            if (dayEvents.isEmpty()) {
                item { EmptyPanel("这一天还没有记录") }
            } else {
                items(dayEvents, key = { it.id }) { event ->
                    TimelineRow(
                        event,
                        highlighted = event.id == highlightedEventId,
                        onClick = { onOpenDetail(event) },
                        onEdit = if (isEditablePregnancyRecord(event.eventType)) { { onEditEvent(event) } } else null,
                        onDelete = { onDeleteEvent(event) }
                    )
                }
            }
        }
        if (stage == BabyLogDomain.STAGE_PREGNANCY) {
            item { PregnancySummaryPanel(state.timeline) }
        }
        if (stage != BabyLogDomain.STAGE_BABY) {
            item {
                SectionHeader(
                    title = "最近记录",
                    action = "全部记录",
                    onAction = onShowTimeline
                )
            }
            val recent = state.dashboard?.recentEvents.orEmpty()
                .filter { isEventVisibleInHome(it, stage) }
                .take(4)
            if (recent.isEmpty()) {
                item { EmptyPanel("还没有记录") }
            } else {
                items(recent, key = { it.id }) { event ->
                    TimelineRow(
                        event,
                        highlighted = event.id == highlightedEventId,
                        onClick = { onOpenDetail(event) },
                        onEdit = if (isEditablePregnancyRecord(event.eventType)) { { onEditEvent(event) } } else null,
                        onDelete = { onDeleteEvent(event) }
                    )
                }
            }
        }
        if (stage == BabyLogDomain.STAGE_PREGNANCY) {
            item { FetalGrowthPanel(state.timeline) }
        } else {
            item { SectionHeader(title = "趋势") }
            item { TrendPanel(state.timeline, stage) }
        }
    }
}
