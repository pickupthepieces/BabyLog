@file:Suppress("LongParameterList")

package app.babylog.nativeapp

import androidx.compose.ui.text.input.KeyboardType
internal data class BabyCareLabels(
    val primary: String,
    val secondary: String,
    val tertiary: String?,
    val note: String?,
    val primaryKeyboard: KeyboardType = KeyboardType.Text,
    val secondaryKeyboard: KeyboardType = KeyboardType.Text,
    val tertiaryKeyboard: KeyboardType = KeyboardType.Text,
    val primaryOptions: List<Option>? = null,
    val secondaryOptions: List<Option>? = null,
    val tertiaryOptions: List<Option>? = null,
    val primaryAllowCustom: Boolean = false,
    val secondaryAllowCustom: Boolean = false,
    val tertiaryAllowCustom: Boolean = false,
    val primaryIsTime: Boolean = false,
    val secondaryIsTime: Boolean = false
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
        "feed" -> BabyCareLabels(
            primary = "方式",
            secondary = "奶量 ml，例如 120",
            tertiary = "侧别 / 辅食食材",
            note = "备注",
            primaryKeyboard = KeyboardType.Text,
            secondaryKeyboard = KeyboardType.Decimal,
            primaryOptions = feedMethodOptions,
            tertiaryOptions = feedSideOptions,
            primaryAllowCustom = true,
            tertiaryAllowCustom = true
        )
        "sleep" -> BabyCareLabels(
            primary = "开始时间",
            secondary = "结束时间",
            tertiary = "地点，例如 卧室",
            note = "备注",
            primaryIsTime = true,
            secondaryIsTime = true
        )
        "diaper" -> BabyCareLabels(
            primary = "类型",
            secondary = "尿布详情，例如 尿量 / 便量",
            tertiary = "颜色 / 性状（可选）",
            note = "备注",
            primaryOptions = diaperTypeOptions,
            tertiaryOptions = diaperObservationOptions,
            primaryAllowCustom = true,
            tertiaryAllowCustom = true
        )
        "temperature" -> BabyCareLabels(
            primary = "体温",
            secondary = "测量方式",
            tertiary = null,
            note = "备注",
            primaryKeyboard = KeyboardType.Decimal,
            secondaryKeyboard = KeyboardType.Text,
            secondaryOptions = temperatureMethodOptions,
            secondaryAllowCustom = true
        )
        "medication" -> BabyCareLabels("药名", "剂量，例如 2 ml", "原因", null)
        "growth" -> growthLabels
        "child_checkup" -> childCheckupLabels
        "breastfeed" -> BabyCareLabels("左侧时长（分钟）", "右侧时长（分钟）", "备注", null, KeyboardType.Decimal, KeyboardType.Decimal)
        "bottle" -> BabyCareLabels("奶量 mL", "品牌", "备注", null, KeyboardType.Decimal, KeyboardType.Text)
        "wake" -> BabyCareLabels("状态，例如 自然醒 / 哭醒", "备注", null, null)
        "pee" -> BabyCareLabels("尿布情况", "备注", null, null)
        "poop" -> BabyCareLabels("性状 / 颜色", "备注", null, null)
        else -> BabyCareLabels("详情", "备注", null, null)
    }
}

private val growthLabels = BabyCareLabels(
    "体重 kg",
    "身长 cm",
    "头围 cm",
    "备注",
    KeyboardType.Decimal,
    KeyboardType.Decimal,
    KeyboardType.Decimal
)

private val childCheckupLabels = BabyCareLabels(
    "体重 kg",
    "身长 cm",
    "头围 cm",
    "家庭备注",
    KeyboardType.Decimal,
    KeyboardType.Decimal,
    KeyboardType.Decimal
)

private val feedMethodOptions = listOf(
    Option("母乳", "母乳"),
    Option("奶瓶", "奶瓶"),
    Option("辅食", "辅食")
)

private val feedSideOptions = listOf(
    Option("L", "左侧"),
    Option("R", "右侧"),
    Option("BOTH", "双侧")
)

private val diaperTypeOptions = listOf(
    Option("尿", "尿"),
    Option("便", "便"),
    Option("混合", "混合")
)

private val diaperObservationOptions = listOf(
    Option("黄色成型", "黄 · 成型"),
    Option("黄色软便", "黄 · 软便"),
    Option("黄色偏稀", "黄 · 稀"),
    Option("黄色水样", "黄 · 水样"),
    Option("绿色软便", "绿 · 软便"),
    Option("绿色偏稀", "绿 · 稀"),
    Option("黑色成型", "黑 · 成型"),
    Option("黑色软便", "黑 · 软便"),
    Option("红色偏稀", "红 · 稀"),
    Option("白色软便", "白 · 软便")
)

private val temperatureMethodOptions = listOf(
    Option("腋温", "腋温"),
    Option("耳温", "耳温"),
    Option("额温", "额温"),
    Option("口温", "口温")
)

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
    note: String,
    occurredTime: String = ""
): BabyLogService.BabyCareInput {
    val input = when (eventType) {
        "feed" -> BabyLogService.BabyCareInput.feed(primary, secondary, tertiary, note)
        "sleep" -> BabyLogService.BabyCareInput.sleep(primary, secondary, tertiary, note)
        "diaper" -> BabyLogService.BabyCareInput.diaper(primary, secondary, tertiary, note)
        "temperature" -> BabyLogService.BabyCareInput.temperature(primary, secondary, note)
        "medication" -> BabyLogService.BabyCareInput.medication(primary, secondary, tertiary)
        "growth" -> BabyLogService.BabyCareInput.growth(primary, secondary, tertiary, note)
        "child_checkup" -> BabyLogService.BabyCareInput.childCheckup(primary, secondary, tertiary, "", "", "", note)
        "breastfeed" -> BabyLogService.BabyCareInput.breastfeed(primary, secondary, tertiary)
        "bottle" -> BabyLogService.BabyCareInput.bottle(primary, secondary, tertiary)
        "wake", "pee", "poop" -> BabyLogService.BabyCareInput.quick(eventType, primary, secondary)
        else -> BabyLogService.BabyCareInput.feed(primary, secondary, note)
    }
    return input.withOccurredTime(occurredTime)
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
