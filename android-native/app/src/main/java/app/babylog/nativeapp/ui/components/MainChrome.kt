@file:Suppress("FunctionNaming")

package app.babylog.nativeapp

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitLongPressOrCancellation
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
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
            bottom = inner.calculateBottomPadding() + 132.dp
        ),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        content = content
    )
}

@Composable
internal fun TopBrandBand(activeTab: String, state: BabyLogUiState) {
    val stage = currentCareStage(state.childProfile)
    val showBrand = activeTab == BabyLogRoutes.Home || !state.setupCompleted
    val title = if (showBrand) "BabyLog" else tabTitle(activeTab)
    val subtitle = if (!state.setupCompleted) {
        "设置家庭档案"
    } else {
        val nickname = state.childProfile.nickname.ifBlank { "宝宝" }
        "$nickname · ${stageLabel(stage)}"
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(ChestnutPalette.Bg)
            .padding(start = 20.dp, top = 30.dp, end = 20.dp, bottom = 14.dp)
    ) {
        if (showBrand) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Image(
                        painter = painterResource(R.drawable.wordmark_babylog_tight),
                        contentDescription = title,
                        modifier = Modifier
                            .width(118.dp)
                            .height(31.dp)
                    )
                    Text(
                        text = subtitle,
                        color = ChestnutPalette.Muted,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(start = 1.dp, top = 6.dp)
                    )
                }
                Image(
                    painter = painterResource(R.drawable.chestnut_main_ip_cutout),
                    contentDescription = null,
                    modifier = Modifier.size(64.dp)
                )
            }
        } else {
            Text(
                text = title,
                color = ChestnutPalette.Ink,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.sp
            )
        }
    }
}

@Composable
internal fun FirstRunScreen(
    onCreatePregnancyProfile: () -> Unit,
    onCreateBabyProfile: () -> Unit,
    onImportBackup: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
        FirstRunNotice()
        SetupChoiceGroup(
            title = "新建档案",
            choices = listOf(
                SetupChoice(
                    title = "孕期家庭",
                    subtitle = "记录产检、胎动、宫缩和 B 超资料",
                    icon = LineIcon.Movement,
                    tint = ChestnutPalette.Rose,
                    onClick = onCreatePregnancyProfile
                ),
                SetupChoice(
                    title = "出生后家庭",
                    subtitle = "记录喂养、睡眠、尿布、身高体重",
                    icon = LineIcon.Home,
                    tint = ChestnutPalette.Green,
                    onClick = onCreateBabyProfile
                )
            )
        )
        SetupChoiceGroup(
            title = "已有数据",
            choices = listOf(
                SetupChoice(
                    title = "导入备份",
                    subtitle = "从 BabyLog JSON 恢复本机记录和档案",
                    icon = LineIcon.Library,
                    tint = ChestnutPalette.Blue,
                    onClick = onImportBackup
                )
            )
        )
    }
}

@Composable
private fun FirstRunNotice() {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "开始使用",
            color = ChestnutPalette.Ink,
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.sp
        )
        Text(
            text = "数据默认保存在本机。BabyLog 只做家庭记录和复诊沟通辅助。",
            color = ChestnutPalette.Muted,
            fontSize = 15.sp,
            lineHeight = 22.sp
        )
    }
}

@Composable
private fun SetupChoiceGroup(
    title: String,
    choices: List<SetupChoice>
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            color = ChestnutPalette.Text3,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(ChestnutRadius.Card),
            backgroundColor = ChestnutPalette.Surface,
            border = BorderStroke(1.dp, ChestnutPalette.Border.copy(alpha = 0.34f)),
            elevation = 0.dp
        ) {
            Column {
                choices.forEachIndexed { index, choice ->
                    SetupChoiceRow(choice)
                    if (index < choices.lastIndex) {
                        Divider(
                            modifier = Modifier.padding(start = 58.dp),
                            color = ChestnutPalette.Border.copy(alpha = 0.55f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SetupChoiceRow(choice: SetupChoice) {
    val interactionSource = remember { MutableInteractionSource() }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 72.dp)
            .babyLogPressScale(interactionSource)
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) { choice.onClick() }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        BabyLogIconTile(
            icon = choice.icon,
            tint = Color.White,
            tileColor = choice.tint,
            modifier = Modifier.size(30.dp),
            iconSize = 18.dp
        )
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(choice.title, color = ChestnutPalette.Ink, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
            Text(choice.subtitle, color = ChestnutPalette.Muted, fontSize = 13.sp, lineHeight = 18.sp)
        }
        Text("›", color = ChestnutPalette.Text3, fontSize = 28.sp)
    }
}

private data class SetupChoice(
    val title: String,
    val subtitle: String,
    val icon: LineIcon,
    val tint: Color,
    val onClick: () -> Unit
)

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
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(ChestnutPalette.Surface.copy(alpha = 0.96f))
            .navigationBarsPadding()
    ) {
        Divider(color = ChestnutPalette.Border.copy(alpha = 0.42f))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
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
}

@Composable
private fun RowScope.BottomNavTab(
    item: NavItem,
    selected: Boolean,
    onTabSelected: (String) -> Unit
) {
    val itemColor by animateColorAsState(
        targetValue = if (selected) ChestnutPalette.Primary else ChestnutPalette.Text3,
        label = "bottomNavItemColor"
    )
    val iconSize by animateDpAsState(
        targetValue = if (selected) 25.dp else 23.dp,
        label = "bottomNavIconSize"
    )
    val selectedScale by animateFloatAsState(
        targetValue = if (selected) 1.08f else 1f,
        label = "bottomNavSelectedScale"
    )
    val interactionSource = remember { MutableInteractionSource() }
    Column(
        modifier = Modifier
            .weight(1f)
            .height(64.dp)
            .clip(RoundedCornerShape(ChestnutRadius.Card))
            .babyLogPressScale(interactionSource)
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) { onTabSelected(item.key) },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        BabyLogMaterialIcon(
            icon = item.icon,
            tint = itemColor,
            modifier = Modifier
                .size(iconSize)
                .graphicsLayer {
                    scaleX = selectedScale
                    scaleY = selectedScale
                }
        )
        Spacer(Modifier.height(4.dp))
        Text(
            item.label,
            color = itemColor,
            fontSize = 11.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
        )
    }
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
    var pressed by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier
            .weight(1f)
            .height(68.dp)
            .pointerInput(voiceState.isTranscribing) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    pressed = true
                    val longPress = awaitLongPressOrCancellation(down.id)
                    if (longPress == null) {
                        pressed = false
                        currentOnSmartEntryClick()
                        return@awaitEachGesture
                    }
                    if (voiceState.isTranscribing) {
                        waitForUpOrCancellation()
                        pressed = false
                        return@awaitEachGesture
                    }
                    currentOnVoiceHoldStart()
                    try {
                        waitForUpOrCancellation()
                    } finally {
                        pressed = false
                        currentOnVoiceHoldEnd()
                    }
                }
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        val tileColor by animateColorAsState(
            targetValue = when {
                voiceState.isRecording -> ChestnutPalette.Danger
                voiceState.isTranscribing -> ChestnutPalette.PrimarySoft
                else -> ChestnutPalette.PrimarySoft
            },
            label = "voiceTileColor"
        )
        val iconColor by animateColorAsState(
            targetValue = if (voiceState.isRecording) Color.White else ChestnutPalette.Primary,
            label = "voiceIconColor"
        )
        val tileSize by animateDpAsState(
            targetValue = if (pressed || voiceState.isRecording) 54.dp else 60.dp,
            label = "voiceTileSize"
        )
        val recordingPulse by rememberInfiniteTransition(label = "voiceRecordingPulse").animateFloat(
            initialValue = 1f,
            targetValue = 1.07f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 680, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "voiceRecordingPulseScale"
        )
        Box(
            modifier = Modifier
                .size(tileSize)
                .clip(CircleShape)
                .background(tileColor)
                .graphicsLayer {
                    val pulse = if (voiceState.isRecording) recordingPulse else 1f
                    scaleX = pulse
                    scaleY = pulse
                },
            contentAlignment = Alignment.Center
        ) {
            BabyLogMaterialIcon(
                icon = LineIcon.Voice,
                tint = iconColor,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

private data class NavItem(val key: String, val label: String, val icon: LineIcon)
