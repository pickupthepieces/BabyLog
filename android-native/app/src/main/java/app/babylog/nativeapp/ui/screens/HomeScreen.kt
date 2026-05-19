package app.babylog.nativeapp

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import app.babylog.nativeapp.ui.screens.BabyLogRoutes

@Composable
internal fun HomeScreen(
    inner: PaddingValues,
    state: BabyLogUiState,
    selectedBabyDay: String,
    highlightedEventId: String?,
    onBabyDaySelected: (String) -> Unit,
    onShowTimeline: () -> Unit,
    onDeleteEvent: (BabyLogDomain.BabyLogEvent) -> Unit
) {
    val stage = currentCareStage(state.childProfile)
    BabyLogScreenColumn(inner) {
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
