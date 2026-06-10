@file:Suppress("FunctionNaming", "InvalidPackageDeclaration", "MagicNumber", "TooManyFunctions")

package app.babylog.nativeapp

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

internal fun LazyListScope.ultrasoundClinicalSection(state: UltrasoundFormState) {
    item(key = "advanced_tabs") {
        UltrasoundAdvancedSectionTabs(state.advancedSection, state.updateAdvancedSection)
    }
    when (state.advancedSection) {
        "report" -> ultrasoundReportSection(state)
        "fetal" -> ultrasoundFetalSection(state)
        "amniotic" -> ultrasoundAmnioticSection(state)
        "flow" -> ultrasoundFlowSection(state)
        else -> item(key = "advanced_hint") {
            Text(
                "按报告内容选择一组填写；未填写的项目保存时会自动留空。",
                color = ChestnutPalette.Muted,
                fontSize = 12.sp
            )
        }
    }
}

private fun LazyListScope.ultrasoundReportSection(state: UltrasoundFormState) {
    item(key = "report_header") { Text("报告信息", color = ChestnutPalette.Ink, fontWeight = FontWeight.Bold) }
    item(key = "hospital") { UltrasoundHospitalInput(state) }
    item(key = "report_time") { UltrasoundReportTimeInput(state) }
}

private fun LazyListScope.ultrasoundFetalSection(state: UltrasoundFormState) {
    item(key = "fetal_header") { Text("胎儿附加信息", color = ChestnutPalette.Ink, fontWeight = FontWeight.Bold) }
    item(key = "fetal_heart_crl") {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            UltrasoundFetalHeartRateInput(state)
            UltrasoundCrlInput(state)
        }
    }
    item(key = "nt_cervical_length") {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            UltrasoundNtInput(state)
            UltrasoundCervicalLengthInput(state)
        }
    }
    item(key = "fetal_count") { UltrasoundFetalCountInput(state) }
    item(key = "fetal_movement") { UltrasoundFetalMovementInput(state) }
    item(key = "umbilical_insertion") { UltrasoundUmbilicalInsertionInput(state) }
}

private fun LazyListScope.ultrasoundAmnioticSection(state: UltrasoundFormState) {
    item(key = "amniotic_header") { Text("羊水 / 胎盘 / 胎位", color = ChestnutPalette.Ink, fontWeight = FontWeight.Bold) }
    item(key = "afi_deepest_pocket") {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            UltrasoundAfiInput(state)
            UltrasoundDeepestPocketInput(state)
        }
    }
    item(key = "placenta_location") { UltrasoundPlacentaLocationInput(state) }
    item(key = "placenta_grade") { UltrasoundPlacentaGradeInput(state) }
    item(key = "fetal_presentation") { UltrasoundFetalPresentationInput(state) }
}

private fun LazyListScope.ultrasoundFlowSection(state: UltrasoundFormState) {
    item(key = "flow_header") { Text("脐动脉血流", color = ChestnutPalette.Ink, fontWeight = FontWeight.Bold) }
    item(key = "sd_pi") {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            UltrasoundSdInput(state)
            UltrasoundPiInput(state)
        }
    }
    item(key = "ri") { UltrasoundRiInput(state) }
}

@Composable
private fun UltrasoundHospitalInput(state: UltrasoundFormState) {
    ChestnutTextField("医院 / 机构", state.hospital, state.updateHospital, KeyboardType.Text)
}

@Composable
private fun UltrasoundReportTimeInput(state: UltrasoundFormState) {
    ChestnutTextField("报告时间", state.reportTime, state.updateReportTime, KeyboardType.Text)
}

@Composable
private fun RowScope.UltrasoundFetalHeartRateInput(state: UltrasoundFormState) {
    UnitInputRow("胎心率", state.fetalHeartRate, state.updateFetalHeartRate, "bpm", Modifier.weight(1f))
}

@Composable
private fun RowScope.UltrasoundCrlInput(state: UltrasoundFormState) {
    UnitInputRow("CRL", state.crl, state.updateCrl, "mm", Modifier.weight(1f))
}

@Composable
private fun RowScope.UltrasoundNtInput(state: UltrasoundFormState) {
    UnitInputRow("NT", state.nt, state.updateNt, "mm", Modifier.weight(1f))
}

@Composable
private fun RowScope.UltrasoundCervicalLengthInput(state: UltrasoundFormState) {
    UnitInputRow("宫颈管", state.cervicalLength, state.updateCervicalLength, "mm", Modifier.weight(1f))
}

@Composable
private fun RowScope.UltrasoundAfiInput(state: UltrasoundFormState) {
    UnitInputRow("AFI", state.afi, state.updateAfi, "cm", Modifier.weight(1f))
}

@Composable
private fun RowScope.UltrasoundDeepestPocketInput(state: UltrasoundFormState) {
    UnitInputRow("最大羊水池", state.deepestPocket, state.updateDeepestPocket, "cm", Modifier.weight(1f))
}

@Composable
private fun RowScope.UltrasoundSdInput(state: UltrasoundFormState) {
    UnitInputRow("S/D", state.umbilicalSd, state.updateUmbilicalSd, "", Modifier.weight(1f))
}

@Composable
private fun RowScope.UltrasoundPiInput(state: UltrasoundFormState) {
    UnitInputRow("PI", state.umbilicalPi, state.updateUmbilicalPi, "", Modifier.weight(1f))
}

@Composable
private fun UltrasoundRiInput(state: UltrasoundFormState) {
    UnitInputRow("RI", state.umbilicalRi, state.updateUmbilicalRi, "")
}

@Composable
private fun UltrasoundFetalCountInput(state: UltrasoundFormState) {
    ChoiceChipRow(
        label = "胎儿个数",
        selected = state.fetalCount,
        options = listOf("单胎" to "单胎", "双胎" to "双胎", "多胎" to "多胎", "未写" to "未写"),
        onSelect = state.updateFetalCount
    )
}

@Composable
private fun UltrasoundFetalMovementInput(state: UltrasoundFormState) {
    ChoiceChipRow(
        label = "胎动",
        selected = state.fetalMovement,
        options = listOf("有" to "有", "可见" to "可见", "无" to "无", "未写" to "未写"),
        onSelect = state.updateFetalMovement
    )
}

@Composable
private fun UltrasoundUmbilicalInsertionInput(state: UltrasoundFormState) {
    ChestnutTextField("脐带插入处", state.umbilicalInsertion, state.updateUmbilicalInsertion, KeyboardType.Text)
}

@Composable
private fun UltrasoundPlacentaLocationInput(state: UltrasoundFormState) {
    ChoiceChipRow(
        label = "胎盘位置",
        selected = state.placentaLocation,
        options = listOf("前壁" to "前壁", "后壁" to "后壁", "侧壁" to "侧壁", "低置" to "低置", "前置" to "前置", "其他" to "其他"),
        onSelect = state.updatePlacentaLocation
    )
}

@Composable
private fun UltrasoundPlacentaGradeInput(state: UltrasoundFormState) {
    ChoiceChipRow(
        label = "胎盘成熟度",
        selected = state.placentaGrade,
        options = listOf("0级" to "0 级", "I 级" to "I 级", "II 级" to "II 级", "III 级" to "III 级", "未写" to "未写"),
        onSelect = state.updatePlacentaGrade
    )
}

@Composable
private fun UltrasoundFetalPresentationInput(state: UltrasoundFormState) {
    ChoiceChipRow(
        label = "胎位",
        selected = state.fetalPresentation,
        options = listOf("头位" to "头位", "臀位" to "臀位", "横位" to "横位", "不详" to "不详"),
        onSelect = state.updateFetalPresentation
    )
}

@Composable
private fun UltrasoundAdvancedSectionTabs(
    selected: String,
    onSelect: (String) -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(
            "report" to "报告",
            "fetal" to "胎儿",
            "amniotic" to "羊水胎盘",
            "flow" to "脐血流"
        ).forEach { (key, label) ->
            val active = selected == key
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(ChestnutRadius.Small))
                    .background(if (active) ChestnutPalette.Primary else ChestnutPalette.Surface)
                    .border(
                        1.dp,
                        if (active) ChestnutPalette.Primary else ChestnutPalette.Border,
                        RoundedCornerShape(ChestnutRadius.Small)
                    )
                    .clickable { onSelect(key) }
                    .padding(horizontal = 6.dp, vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    label,
                    color = if (active) Color.White else ChestnutPalette.Ink,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
            }
        }
    }
}
