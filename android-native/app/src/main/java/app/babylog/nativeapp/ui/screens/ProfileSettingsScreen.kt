package app.babylog.nativeapp

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

@Composable
internal fun ProfileSettingsScreen(
    state: ProfileDialogState?,
    onBack: () -> Unit,
    onSave: (ProfileInput, Boolean) -> Unit
) {
    if (state == null) {
        SettingsPageScaffold(
            title = "宝宝档案",
            subtitle = "档案状态读取中",
            onBack = onBack
        ) {
            item {
                Text("档案内容暂不可用，请返回后重试。", color = ChestnutPalette.Muted)
            }
        }
        return
    }

    var nickname by rememberSaveable(state.title) { mutableStateOf(state.profile.nickname) }
    var sex by rememberSaveable(state.title) {
        mutableStateOf(if (state.profile.sex == "unknown") "unknown" else state.profile.sex)
    }
    var expectedDueDate by rememberSaveable(state.title) { mutableStateOf(state.profile.expectedDueDate) }
    var birthDate by rememberSaveable(state.title) { mutableStateOf(state.profile.birthDate) }
    var stageOverride by rememberSaveable(state.title) { mutableStateOf(state.initialStage) }

    SettingsPageScaffold(
        title = state.title,
        subtitle = "日期可后补，阶段可手动覆盖",
        onBack = onBack,
        onSave = {
            onSave(
                ProfileInput(
                    nickname.trim(),
                    sex.trim(),
                    expectedDueDate.trim(),
                    birthDate.trim(),
                    stageOverride.trim()
                ),
                state.firstRun
            )
        }
    ) {
        item { ChestnutTextField("乳名 / 昵称，例如 栗子", nickname, { nickname = it }, KeyboardType.Text) }
        item {
            ChoiceChipRow(
                label = "性别",
                selected = sex,
                options = listOf(
                    "female" to "女宝",
                    "male" to "男宝",
                    "unknown" to "暂不确定"
                ),
                onSelect = { sex = it }
            )
        }
        item { DateInputRow("预产期", expectedDueDate, { expectedDueDate = it }) }
        item { DateInputRow("出生日期", birthDate, { birthDate = it }) }
        item {
            ChoiceChipRow(
                label = "阶段",
                selected = stageOverride,
                options = listOf(
                    BabyLogDomain.STAGE_AUTO to "自动",
                    BabyLogDomain.STAGE_PREGNANCY to "孕期",
                    BabyLogDomain.STAGE_BABY to "出生后",
                    BabyLogDomain.STAGE_UNKNOWN to "未知"
                ),
                onSelect = { stageOverride = it }
            )
        }
        item {
            Text(
                "日期可后补",
                color = Color(0xFF7C4A21),
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFFFEBCB))
                    .padding(12.dp)
            )
        }
    }
}
