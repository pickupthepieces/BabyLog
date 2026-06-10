@file:Suppress("LongParameterList")

package app.babylog.nativeapp

import androidx.compose.ui.text.input.KeyboardType
internal data class BabyCareLabels(
    val primary: String,
    val secondary: String,
    val tertiary: String?,
    val note: String?,
    val primaryKeyboard: KeyboardType = KeyboardType.Text,
    val secondaryKeyboard: KeyboardType = KeyboardType.Text
)

internal data class PregnancyLabels(
    val primary: String,
    val secondary: String,
    val tertiary: String?,
    val note: String?,
    val primaryKeyboard: KeyboardType = KeyboardType.Text,
    val secondaryKeyboard: KeyboardType = KeyboardType.Text,
    val tertiaryKeyboard: KeyboardType = KeyboardType.Text
)

internal fun babyCareLabels(eventType: String): BabyCareLabels {
    return when (eventType) {
        "feed" -> BabyCareLabels("方式，例如 母乳 / 奶瓶 / 辅食", "奶量 ml，例如 120", "侧别 L / R / BOTH 或辅食食材", "备注", KeyboardType.Text, KeyboardType.Decimal)
        "sleep" -> BabyCareLabels("开始时间，例如 22:10", "结束时间，例如 01:20", "地点，例如 卧室", "备注")
        "diaper" -> BabyCareLabels("类型，例如 尿 / 便 / 混合", "尿布详情，例如 尿量 / 便量", "颜色 / 性状（可选）", "备注")
        "temperature" -> BabyCareLabels("体温", "测量方式，例如 腋温", null, "备注", KeyboardType.Decimal, KeyboardType.Text)
        "medication" -> BabyCareLabels("药名", "剂量，例如 2 ml", "原因", null)
        "breastfeed" -> BabyCareLabels("左侧时长（分钟）", "右侧时长（分钟）", "备注", null, KeyboardType.Decimal, KeyboardType.Decimal)
        "bottle" -> BabyCareLabels("奶量 mL", "品牌", "备注", null, KeyboardType.Decimal, KeyboardType.Text)
        "wake" -> BabyCareLabels("状态，例如 自然醒 / 哭醒", "备注", null, null)
        "pee" -> BabyCareLabels("尿布情况", "备注", null, null)
        "poop" -> BabyCareLabels("性状 / 颜色", "备注", null, null)
        else -> BabyCareLabels("详情", "备注", null, null)
    }
}

internal fun pregnancyLabels(eventType: String): PregnancyLabels {
    return when (eventType) {
        "pregnancy_checkup" -> PregnancyLabels(
            "检查日期 yyyy-MM-dd",
            "医院 / 机构",
            "医生结论 / 建议",
            "备注"
        )
        "screening_nt" -> PregnancyLabels("检查日期 yyyy-MM-dd", "NT 值 mm", "结论文本", "备注", KeyboardType.Text, KeyboardType.Decimal)
        "screening_serum" -> PregnancyLabels("检查日期 yyyy-MM-dd", "21 三体风险值", "18 三体风险值", "备注")
        "screening_nipt" -> PregnancyLabels("检查日期 yyyy-MM-dd", "T21 结果", "T18 结果", "结论文本")
        "screening_anomaly" -> PregnancyLabels("检查日期 yyyy-MM-dd", "结构结论", null, "备注")
        "screening_ogtt" -> PregnancyLabels("检查日期 yyyy-MM-dd", "空腹血糖", "1h 血糖", "备注", KeyboardType.Text, KeyboardType.Decimal, KeyboardType.Decimal)
        "screening_gbs" -> PregnancyLabels("检查日期 yyyy-MM-dd", "GBS 结果", null, "备注")
        "screening_nst" -> PregnancyLabels("检查日期 yyyy-MM-dd", "胎心监护结果", null, "备注")
        "fetal_movement" -> PregnancyLabels(
            "时段，例如 20:00-21:00",
            "次数，例如 10",
            null,
            "备注",
            KeyboardType.Text,
            KeyboardType.Decimal
        )
        "contraction" -> PregnancyLabels(
            "开始时间，例如 22:10",
            "间隔分钟，例如 5",
            "持续秒，例如 40",
            "备注",
            KeyboardType.Text,
            KeyboardType.Decimal,
            KeyboardType.Decimal
        )
        else -> PregnancyLabels("详情", "备注", null, null)
    }
}

internal fun defaultPregnancyPrimary(eventType: String): String {
    return if (eventType == "pregnancy_checkup" || BabyLogService.isScreeningEventType(eventType)) BabyLogFormatters.todayDateInput() else ""
}

internal fun buildBabyCareInput(
    eventType: String,
    primary: String,
    secondary: String,
    tertiary: String,
    note: String
): BabyLogService.BabyCareInput {
    return when (eventType) {
        "feed" -> BabyLogService.BabyCareInput.feed(primary, secondary, tertiary, note)
        "sleep" -> BabyLogService.BabyCareInput.sleep(primary, secondary, tertiary, note)
        "diaper" -> BabyLogService.BabyCareInput.diaper(primary, secondary, tertiary, note)
        "temperature" -> BabyLogService.BabyCareInput.temperature(primary, secondary, note)
        "medication" -> BabyLogService.BabyCareInput.medication(primary, secondary, tertiary)
        "breastfeed" -> BabyLogService.BabyCareInput.breastfeed(primary, secondary, tertiary)
        "bottle" -> BabyLogService.BabyCareInput.bottle(primary, secondary, tertiary)
        "wake", "pee", "poop" -> BabyLogService.BabyCareInput.quick(eventType, primary, secondary)
        else -> BabyLogService.BabyCareInput.feed(primary, secondary, note)
    }
}

internal fun buildPregnancyInput(
    eventType: String,
    primary: String,
    secondary: String,
    tertiary: String,
    note: String
): BabyLogService.PregnancyInput {
    return when (eventType) {
        "pregnancy_checkup" -> BabyLogService.PregnancyInput.checkup(primary, secondary, tertiary, note)
        "fetal_movement" -> BabyLogService.PregnancyInput.fetalMovement(primary, secondary, note)
        "contraction" -> BabyLogService.PregnancyInput.contraction(primary, secondary, tertiary, note)
        else -> if (BabyLogService.isScreeningEventType(eventType)) {
            BabyLogService.PregnancyInput.screening(
                eventType,
                primary,
                "",
                mapOf("conclusion" to tertiary, "detail" to secondary),
                note,
                "",
                ""
            )
        } else {
            BabyLogService.PregnancyInput.fetalMovement(primary, secondary, note)
        }
    }
}
