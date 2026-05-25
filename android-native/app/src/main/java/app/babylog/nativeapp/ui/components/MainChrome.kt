package app.babylog.nativeapp

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitLongPressOrCancellation
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.BottomNavigation
import androidx.compose.material.BottomNavigationItem
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.babylog.nativeapp.ui.screens.BabyLogRoutes

@Composable
internal fun BabyLogScreenColumn(
    inner: PaddingValues,
    modifier: Modifier = Modifier,
    listState: LazyListState = rememberLazyListState(),
    content: LazyListScope.() -> Unit
) {
    LazyColumn(
        state = listState,
        modifier = modifier
            .fillMaxSize()
            .background(ChestnutPalette.Bg),
        contentPadding = PaddingValues(
            start = 18.dp,
            top = inner.calculateTopPadding() + 16.dp,
            end = 18.dp,
            bottom = inner.calculateBottomPadding() + 22.dp
        ),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        content = content
    )
}

@Composable
internal fun TopBrandBand(activeTab: String, state: BabyLogUiState) {
    val stage = currentCareStage(state.childProfile)
    val title = if (state.setupCompleted && activeTab != BabyLogRoutes.Home) tabTitle(activeTab) else "BabyLog"
    val subtitle = if (!state.setupCompleted) {
        "先建档，再进入家庭记录"
    } else {
        val nickname = state.childProfile.nickname.ifBlank { "宝宝" }
        "$nickname · ${stageLabel(stage)}"
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(ChestnutPalette.Primary)
            .padding(horizontal = 22.dp, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = Color.White,
                fontSize = if (activeTab == BabyLogRoutes.Home) 32.sp else 24.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.sp
            )
            if (activeTab == BabyLogRoutes.Home || !state.setupCompleted) {
                Text(
                    text = subtitle,
                    color = Color.White.copy(alpha = 0.82f),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
internal fun FirstRunScreen(
    onCreatePregnancyProfile: () -> Unit,
    onCreateBabyProfile: () -> Unit,
    onImportBackup: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text(
            text = "BabyLog 仅做家庭记录和复诊沟通辅助；数据默认保存在本机。",
            color = Color(0xFF7C4A21),
            modifier = Modifier
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xFFFFEBCB))
                .padding(14.dp)
        )
        Panel {
            Text("开始使用", color = ChestnutPalette.Ink, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Text("选择当前家庭状态", color = ChestnutPalette.Muted)
            ActionRow(
                title = "新建孕期家庭",
                subtitle = "录入乳名、性别和预产期；日期可后补",
                action = "建档",
                onClick = onCreatePregnancyProfile
            )
            Divider(color = ChestnutPalette.Border)
            ActionRow(
                title = "新建出生后家庭",
                subtitle = "录入乳名、性别和出生日期；日期可后补",
                action = "建档",
                onClick = onCreateBabyProfile
            )
            Divider(color = ChestnutPalette.Border)
            ActionRow(
                title = "导入备份",
                subtitle = "从 BabyLog JSON 恢复本机记录和档案",
                action = "导入",
                onClick = onImportBackup
            )
        }
    }
}

@Composable
internal fun BottomNav(
    activeTab: String,
    voiceState: SmartVoiceUiState,
    onTabSelected: (String) -> Unit,
    onSmartEntryClick: () -> Unit,
    onVoiceHoldStart: () -> Unit,
    onVoiceHoldEnd: () -> Unit
) {
    val leadingItems = listOf(
        NavItem(BabyLogRoutes.Home, "首页", LineIcon.Home),
        NavItem(BabyLogRoutes.Timeline, "时间线", LineIcon.Timeline)
    )
    val trailingItems = listOf(
        NavItem(BabyLogRoutes.Library, "资料", LineIcon.Library),
        NavItem(BabyLogRoutes.Settings, "设置", LineIcon.Settings)
    )
    BottomNavigation(
        backgroundColor = ChestnutPalette.Primary,
        contentColor = Color.White,
        elevation = 0.dp
    ) {
        leadingItems.forEach { item ->
            BottomNavTab(item = item, selected = activeTab == item.key, onTabSelected = onTabSelected)
        }
        BottomNavVoiceAction(
            voiceState = voiceState,
            onSmartEntryClick = onSmartEntryClick,
            onVoiceHoldStart = onVoiceHoldStart,
            onVoiceHoldEnd = onVoiceHoldEnd
        )
        trailingItems.forEach { item ->
            BottomNavTab(item = item, selected = activeTab == item.key, onTabSelected = onTabSelected)
        }
    }
}

@Composable
private fun RowScope.BottomNavTab(
    item: NavItem,
    selected: Boolean,
    onTabSelected: (String) -> Unit
) {
    val itemColor = if (selected) Color.White else Color.White.copy(alpha = 0.68f)
    BottomNavigationItem(
        selected = selected,
        onClick = { onTabSelected(item.key) },
        icon = {
            BabyLogIconTile(
                icon = item.icon,
                tint = itemColor,
                tileColor = if (selected) Color.White.copy(alpha = 0.20f) else Color.White.copy(alpha = 0.10f),
                modifier = Modifier.size(40.dp),
                iconSize = 24.dp
            )
        },
        label = {
            Text(
                item.label,
                color = itemColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        },
        selectedContentColor = Color.White,
        unselectedContentColor = Color.White.copy(alpha = 0.68f)
    )
}

@Composable
private fun RowScope.BottomNavVoiceAction(
    voiceState: SmartVoiceUiState,
    onSmartEntryClick: () -> Unit,
    onVoiceHoldStart: () -> Unit,
    onVoiceHoldEnd: () -> Unit
) {
    val currentOnSmartEntryClick by rememberUpdatedState(onSmartEntryClick)
    val currentOnVoiceHoldStart by rememberUpdatedState(onVoiceHoldStart)
    val currentOnVoiceHoldEnd by rememberUpdatedState(onVoiceHoldEnd)
    val label = when {
        voiceState.isRecording -> "松开"
        voiceState.isTranscribing -> "识别"
        else -> "语音"
    }
    Column(
        modifier = Modifier
            .weight(1f)
            .height(56.dp)
            .pointerInput(voiceState.isTranscribing) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val longPress = awaitLongPressOrCancellation(down.id)
                    if (longPress == null) {
                        currentOnSmartEntryClick()
                        return@awaitEachGesture
                    }
                    if (voiceState.isTranscribing) {
                        waitForUpOrCancellation()
                        return@awaitEachGesture
                    }
                    currentOnVoiceHoldStart()
                    try {
                        waitForUpOrCancellation()
                    } finally {
                        currentOnVoiceHoldEnd()
                    }
                }
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(
                    if (voiceState.isRecording) {
                        Color.White.copy(alpha = 0.34f)
                    } else {
                        Color.White.copy(alpha = 0.22f)
                    }
                )
                .border(1.dp, Color.White.copy(alpha = 0.52f), RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {
            BabyLogMaterialIcon(
                icon = LineIcon.Voice,
                tint = Color.White,
                modifier = Modifier.size(26.dp)
            )
        }
        Text(
            text = label,
            color = Color.White,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

private data class NavItem(val key: String, val label: String, val icon: LineIcon)
