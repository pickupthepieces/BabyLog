package app.babylog.nativeapp

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable

@Composable
internal fun TimelineScreen(
    inner: PaddingValues,
    state: BabyLogUiState,
    selectedFilter: String,
    highlightedEventId: String?,
    onFilterSelected: (String) -> Unit,
    onEditEvent: (BabyLogDomain.BabyLogEvent) -> Unit,
    onDeleteEvent: (BabyLogDomain.BabyLogEvent) -> Unit
) {
    BabyLogScreenColumn(inner) {
        item {
            TimelineFilters(
                selected = selectedFilter,
                onSelect = onFilterSelected
            )
        }
        val events = state.timeline.filter {
            BabyLogFormatters.matchesTimelineFilter(it.eventType, selectedFilter)
        }
        if (events.isEmpty()) {
            item { EmptyPanel("这个分类暂时没有记录。") }
        } else {
            items(events, key = { it.id }) { event ->
                TimelineRow(
                    event,
                    highlighted = event.id == highlightedEventId,
                    onEdit = if (isEditablePregnancyRecord(event.eventType)) { { onEditEvent(event) } } else null,
                    onDelete = { onDeleteEvent(event) }
                )
            }
        }
    }
}
