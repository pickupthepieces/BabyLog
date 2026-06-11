package app.babylog.nativeapp

import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.KeyboardType

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
        subtitle = "基础档案与记录阶段",
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
        item { ChestnutTextField("昵称，例如 栗子", nickname, { nickname = it }, KeyboardType.Text) }
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
        if (stageOverride != BabyLogDomain.STAGE_BABY) {
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
                    Text("按 LMP / CRL 推算预产期", color = ChestnutPalette.Primary)
                }
            }
        }
        if (stageOverride != BabyLogDomain.STAGE_PREGNANCY) {
            item { DateInputRow("出生日期", birthDate, { birthDate = it }) }
        }
        if (stageOverride != BabyLogDomain.STAGE_BABY) {
            item {
                UnitInputRow("孕前体重", prePregnancyWeightKg, { prePregnancyWeightKg = it }, "kg")
            }
            item {
                UnitInputRow("身高", heightCm, { heightCm = it }, "cm")
            }
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
                NoticeBanner("记录会保留；提醒停止，可随时在档案里恢复。")
            }
        }
        item {
            NoticeBanner("日期可稍后补充。")
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
