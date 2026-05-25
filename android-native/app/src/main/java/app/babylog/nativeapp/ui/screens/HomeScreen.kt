package app.babylog.nativeapp

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
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
    onOpenWeightGain: () -> Unit,
    onOpenReminderCenter: () -> Unit,
    syncPulling: Boolean,
    onPullSyncNow: () -> Unit,
    onDismissSyncBanner: () -> Unit,
    onQuickRailVisibilityChange: (Boolean) -> Unit
) {
    val stage = currentCareStage(state.childProfile)
    val pregnancyDerivedUiMuted = BabyLogFormatters.shouldMutePregnancyDerivedUi(stage)
    val remoteUpdateCount = state.dashboard?.remoteUpdateBannerCount ?: 0
    val babyDailySummary = remember(state.timeline, selectedBabyDay, stage) {
        if (stage == BabyLogDomain.STAGE_BABY) {
            BabyLogDailySummaryCalculator.calculate(state.timeline, selectedBabyDay)
        } else {
            null
        }
    }
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
    BabyLogPullRefreshContainer(
        refreshing = syncPulling,
        onRefresh = onPullSyncNow
    ) {
        BabyLogScreenColumn(
            inner = inner,
            modifier = Modifier.nestedScroll(railNestedScroll),
            listState = listState
        ) {
            if (remoteUpdateCount > 0) {
                item {
                    ChestnutSyncBanner(
                        count = remoteUpdateCount,
                        onDismiss = onDismissSyncBanner
                    )
                }
            }
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
                } else if (pregnancyDerivedUiMuted) {
                    PregnancyQuietCard(stage)
                } else {
                    WeekCard(state.childProfile)
                }
            }
            if (stage == BabyLogDomain.STAGE_BABY) {
                val dayEvents = state.timeline.filter {
                    BabyLogFormatters.recordDay(it.occurredAt) == selectedBabyDay && isEventVisibleInHome(it, stage)
                }
                item { babyDailySummary?.let { DailyBabySummaryCard(it) } }
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
                item { PregnancySummaryPanel(state.timeline, onOpenWeightGain) }
                item { NextCheckupReminderPanel(state.reminders, onOpenReminderCenter) }
                item { PrenatalScreeningTodoPanel(state.childProfile, state.timeline) }
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
            } else if (!pregnancyDerivedUiMuted) {
                item { SectionHeader(title = "趋势") }
                item { TrendPanel(state.timeline, stage) }
            }
        }
    }
}

@Composable
private fun NextCheckupReminderPanel(
    reminders: List<BabyLogReminderStore.Reminder>,
    onOpenReminderCenter: () -> Unit
) {
    val next = reminders
        .asSequence()
        .filter { it.kind == BabyLogReminderStore.KIND_CHECKUP_TODO }
        .filter { BabyLogReminderStore.isActionable(it) }
        .sortedBy { it.dueAtIso.ifBlank { "9999" } }
        .firstOrNull() ?: return
    val dueDate = next.dueAtIso.take(10)
    val days = if (BabyLogFormatters.isValidDateInput(dueDate)) {
        BabyLogFormatters.daysBetweenDateInputs(BabyLogFormatters.todayDateInput(), dueDate)
    } else {
        null
    }
    val dateLabel = when {
        days == null -> "日期待核对"
        days > 0 -> "${days} 天后"
        days == 0 -> "今天"
        else -> "日期已到"
    }
    Panel {
        SectionHeader("下次产检提醒", action = "提醒中心", onAction = onOpenReminderCenter)
        Text(
            "$dateLabel · ${next.title}",
            color = ChestnutPalette.Ink,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold
        )
        if (next.note.isNotBlank()) {
            Text(next.note, color = ChestnutPalette.Muted, fontSize = 12.sp)
        }
    }
}

@Composable
private fun PregnancyQuietCard(stage: String) {
    Panel {
        SectionHeader(if (stage == BabyLogDomain.STAGE_PAUSED) "记录已暂停提醒" else "妊娠记录已静音")
        Text(
            "记录会保留，时间线、详情和导出仍可使用。你可以在档案里切换状态。",
            color = ChestnutPalette.Muted,
            fontSize = 13.sp
        )
    }
}

@Composable
private fun PrenatalScreeningTodoPanel(
    profile: BabyLogDomain.ChildProfile,
    events: List<BabyLogDomain.BabyLogEvent>
) {
    var hidden by rememberSaveable { mutableStateOf(false) }
    if (hidden) return
    val gestationalDays = BabyLogFormatters.parseGestationalAgeDays(currentGestationalAgeInput(profile)) ?: return
    val completed = events.map { it.eventType }.toSet()
    val suggestions = prenatalScreeningSuggestions(gestationalDays)
        .filterNot { completed.contains(it.eventType) }
    if (suggestions.isEmpty()) return
    Panel {
        SectionHeader("专项清单", action = "忽略", onAction = { hidden = true })
        Text(
            "按当前孕周给出的中性提醒，可按医生安排决定是否记录；App 不判断风险。",
            color = ChestnutPalette.Muted,
            fontSize = 12.sp
        )
        suggestions.forEach { item ->
            Text(
                "${item.label} · ${item.window}",
                color = ChestnutPalette.Ink,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

private data class PrenatalScreeningSuggestion(
    val eventType: String,
    val label: String,
    val window: String
)

private fun prenatalScreeningSuggestions(gestationalDays: Int): List<PrenatalScreeningSuggestion> {
    fun inWindow(startWeek: Int, endWeek: Int, endExtraDays: Int = 6): Boolean {
        return gestationalDays in (startWeek * 7)..(endWeek * 7 + endExtraDays)
    }
    return buildList {
        if (inWindow(11, 13)) add(PrenatalScreeningSuggestion("screening_nt", "NT", "11-13+6 周"))
        if (inWindow(15, 20)) add(PrenatalScreeningSuggestion("screening_serum", "唐筛", "15-20+6 周"))
        if (inWindow(12, 22)) add(PrenatalScreeningSuggestion("screening_nipt", "无创 DNA", "约 12-22 周"))
        if (inWindow(20, 24, 0)) add(PrenatalScreeningSuggestion("screening_anomaly", "大排畸 / 系统超声", "20-24 周"))
        if (inWindow(24, 28, 0)) add(PrenatalScreeningSuggestion("screening_ogtt", "糖耐 OGTT", "24-28 周"))
        if (gestationalDays >= 32 * 7) add(PrenatalScreeningSuggestion("screening_nst", "胎心监护 NST", "32 周后按医嘱"))
        if (inWindow(35, 37, 0)) add(PrenatalScreeningSuggestion("screening_gbs", "GBS", "35-37 周"))
    }
}
