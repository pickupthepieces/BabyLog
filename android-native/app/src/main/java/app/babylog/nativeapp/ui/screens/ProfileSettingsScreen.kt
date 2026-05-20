package app.babylog.nativeapp

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.material.TextButton
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
    onOpenDueDateCalculator: (ProfileInput, Boolean) -> Unit,
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

    var nickname by rememberSaveable(state.title, state.profile.nickname) { mutableStateOf(state.profile.nickname) }
    var sex by rememberSaveable(state.title, state.profile.sex) {
        mutableStateOf(if (state.profile.sex == "unknown") "unknown" else state.profile.sex)
    }
    var expectedDueDate by rememberSaveable(state.title, state.profile.expectedDueDate) { mutableStateOf(state.profile.expectedDueDate) }
    var birthDate by rememberSaveable(state.title, state.profile.birthDate) { mutableStateOf(state.profile.birthDate) }
    var prePregnancyWeightKg by rememberSaveable(state.title, state.profile.prePregnancyWeightKg) {
        mutableStateOf(state.profile.prePregnancyWeightKg?.let { BabyLogFormatters.formatNumber(it) }.orEmpty())
    }
    var heightCm by rememberSaveable(state.title, state.profile.heightCm) {
        mutableStateOf(state.profile.heightCm?.let { BabyLogFormatters.formatNumber(it) }.orEmpty())
    }
    var stageOverride by rememberSaveable(
        state.title,
        state.initialStage,
        state.profile.expectedDueDate,
        state.profile.birthDate
    ) {
        mutableStateOf(profileStageChoice(state.profile, state.initialStage))
    }
    var showPregnancyEndedNotice by rememberSaveable(state.title, state.initialStage) { mutableStateOf(false) }

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
                    prePregnancyWeightKg.trim(),
                    heightCm.trim(),
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
        item {
            DateInputRow("预产期", expectedDueDate, { expectedDueDate = it })
            TextButton(
                onClick = {
                    onOpenDueDateCalculator(
                        ProfileInput(
                            nickname.trim(),
                            sex.trim(),
                            expectedDueDate.trim(),
                            birthDate.trim(),
                            prePregnancyWeightKg.trim(),
                            heightCm.trim(),
                            stageOverride.trim()
                        ),
                        state.firstRun
                    )
                }
            ) {
                Text("用 LMP / CRL 推算", color = ChestnutPalette.Primary)
            }
        }
        item { DateInputRow("出生日期", birthDate, { birthDate = it }) }
        item {
            UnitInputRow("孕前体重", prePregnancyWeightKg, { prePregnancyWeightKg = it }, "kg")
        }
        item {
            UnitInputRow("身高", heightCm, { heightCm = it }, "cm")
        }
        item {
            ChoiceChipRow(
                label = "妊娠状态",
                selected = stageOverride,
                options = listOf(
                    BabyLogDomain.STAGE_PREGNANCY to "孕期中",
                    BabyLogDomain.STAGE_BABY to "出生后",
                    BabyLogDomain.STAGE_PREGNANCY_ENDED to "妊娠结束",
                    BabyLogDomain.STAGE_PAUSED to "暂停"
                ),
                onSelect = {
                    if (it == BabyLogDomain.STAGE_PREGNANCY_ENDED && stageOverride != it) {
                        showPregnancyEndedNotice = true
                    }
                    stageOverride = it
                }
            )
        }
        if (showPregnancyEndedNotice && stageOverride == BabyLogDomain.STAGE_PREGNANCY_ENDED) {
            item {
                Text(
                    "我们会保留你的记录；之后想看可以随时回来。不再发提醒，你可以在档案里再切换。",
                    color = Color(0xFF7C4A21),
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFFFEBCB))
                        .padding(12.dp)
                )
            }
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

private fun profileStageChoice(profile: BabyLogDomain.ChildProfile, initialStage: String): String {
    return when (initialStage) {
        BabyLogDomain.STAGE_PREGNANCY,
        BabyLogDomain.STAGE_BABY,
        BabyLogDomain.STAGE_PREGNANCY_ENDED,
        BabyLogDomain.STAGE_PAUSED -> initialStage
        else -> if (BabyLogFormatters.isValidDateInput(profile.birthDate)) {
            BabyLogDomain.STAGE_BABY
        } else {
            BabyLogDomain.STAGE_PREGNANCY
        }
    }
}
