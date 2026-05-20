package app.babylog.nativeapp

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Divider
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
internal fun DueDateCalcScreen(
    currentExpectedDueDate: String,
    onBack: () -> Unit,
    onApplyDueDate: (String) -> Unit
) {
    var lmpDate by rememberSaveable { mutableStateOf("") }
    var cycleLength by rememberSaveable { mutableStateOf(BabyLogDueDateCalculator.DEFAULT_CYCLE_DAYS.toString()) }
    var showCrl by rememberSaveable { mutableStateOf(false) }
    var crlExamDate by rememberSaveable { mutableStateOf(BabyLogFormatters.todayDateInput()) }
    var crlMm by rememberSaveable { mutableStateOf("") }
    var selectedSource by rememberSaveable { mutableStateOf("lmp") }

    val today = BabyLogFormatters.todayDateInput()
    val cycleDays = cycleLength.toIntOrNull() ?: BabyLogDueDateCalculator.DEFAULT_CYCLE_DAYS
    val lmpResult = if (BabyLogFormatters.isValidDateInput(lmpDate)) {
        BabyLogDueDateCalculator.fromLmp(lmpDate, cycleDays, today)
    } else {
        null
    }
    val crlValue = crlMm.trim().toDoubleOrNull()
    val crlResult = if (crlValue != null) {
        BabyLogDueDateCalculator.fromCrl(crlValue, crlExamDate)
    } else {
        null
    }
    val lmpDueDate = if (lmpResult?.valid == true) lmpResult.estimatedDueDate else null
    val crlDueDate = if (crlResult?.valid == true) crlResult.estimatedDueDate else null
    val selectedDueDate = when (selectedSource) {
        "crl" -> crlDueDate
        else -> lmpDueDate
    } ?: lmpDueDate ?: crlDueDate

    LaunchedEffect(lmpDueDate, crlDueDate) {
        if (selectedSource == "crl" && crlDueDate == null) {
            selectedSource = if (lmpDueDate != null) "lmp" else "crl"
        } else if (selectedSource == "lmp" && lmpDueDate == null && crlDueDate != null) {
            selectedSource = "crl"
        }
    }

    SettingsPageScaffold(
        title = "孕周 / 预产期计算器",
        subtitle = "LMP 与早期 B 超 CRL 辅助推算",
        onBack = onBack
    ) {
        item {
            Text(
                "本计算器仅作参考，最终预产期请以医生确认为准。",
                color = Color(0xFF7C4A21),
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFFFFEBCB))
                    .padding(14.dp)
            )
        }
        item {
            SettingsPanel("末次月经推算") {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    DateInputRow("末次月经 LMP", lmpDate, { lmpDate = it })
                    ChestnutTextField("周期长度", cycleLength, { cycleLength = it.filter(Char::isDigit) }, KeyboardType.Number)
                    lmpResult?.let { result ->
                        ResultLine("预产期", result.estimatedDueDate)
                        ResultLine("当前孕周", result.gestationalAgeLabel)
                        BabyLogDueDateCalculator.formatCycleWarning(result.nonStandardCycle)
                            .takeIf { it.isNotBlank() }
                            ?.let { WarningText(it) }
                        BabyLogDueDateCalculator.formatRangeWarning(result.beyondTypicalRange)
                            .takeIf { it.isNotBlank() }
                            ?.let { WarningText(it) }
                    } ?: Text("选择 LMP 后显示推算结果。", color = ChestnutPalette.Text3, fontSize = 13.sp)
                }
            }
        }
        item {
            OutlinedButton(onClick = { showCrl = !showCrl }) {
                Text(if (showCrl) "收起早期 B 超 CRL" else "填写早期 B 超 CRL", color = ChestnutPalette.Primary)
            }
        }
        if (showCrl) {
            item {
                SettingsPanel("早期 B 超 CRL 推算") {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        DateInputRow("检查日期", crlExamDate, { crlExamDate = it }, allowClear = false)
                        UnitInputRow("CRL", crlMm, { crlMm = it }, "mm")
                        crlResult?.let { result ->
                            if (result.valid) {
                                ResultLine("预产期", result.estimatedDueDate)
                                ResultLine("CRL 推算孕周", result.gestationalAgeLabel)
                                Text(
                                    "公式：8.052 × √CRL + 23.73；结果仅供核对。",
                                    color = ChestnutPalette.Text3,
                                    fontSize = 12.sp
                                )
                            } else {
                                WarningText(result.message)
                            }
                        } ?: Text("填写 CRL 后显示推算结果。", color = ChestnutPalette.Text3, fontSize = 13.sp)
                    }
                }
            }
        }
        if (lmpDueDate != null && crlDueDate != null) {
            item {
                SettingsPanel("双源差异") {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ResultLine("LMP-EDD", lmpDueDate)
                        ResultLine("CRL-EDD", crlDueDate)
                        ResultLine("差异", "${BabyLogDueDateCalculator.diffDays(lmpDueDate, crlDueDate)} 天")
                        Text(BabyLogDueDateCalculator.CLINICAL_NOTE, color = ChestnutPalette.Muted, fontSize = 13.sp)
                    }
                }
            }
        }
        item {
            SettingsPanel("应用到档案") {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (currentExpectedDueDate.isNotBlank()) {
                        ResultLine("当前档案预产期", currentExpectedDueDate)
                        Divider(color = ChestnutPalette.Border)
                    }
                    if (lmpDueDate != null && crlDueDate != null) {
                        ChoiceChipRow(
                            label = "使用来源",
                            selected = selectedSource,
                            options = listOf("lmp" to "LMP", "crl" to "CRL"),
                            onSelect = { selectedSource = it }
                        )
                    } else if (selectedDueDate == null) {
                        Text("先完成至少一种推算，再应用到档案。", color = ChestnutPalette.Text3, fontSize = 13.sp)
                    }
                    selectedDueDate?.let { dueDate ->
                        ResultLine("将填入预产期", dueDate)
                        Button(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = { onApplyDueDate(dueDate) },
                            colors = ButtonDefaults.buttonColors(backgroundColor = ChestnutPalette.Primary)
                        ) {
                            Text("应用为预产期", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                        Text(
                            "应用后会回到档案页；需要你在档案页再点保存才会生效。",
                            color = ChestnutPalette.Muted,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ResultLine(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = ChestnutPalette.Muted, fontSize = 13.sp)
        Text(value, color = ChestnutPalette.Ink, fontWeight = FontWeight.Bold, fontSize = 15.sp)
    }
}

@Composable
private fun WarningText(message: String) {
    Text(
        message,
        color = Color(0xFF7C4A21),
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFFFEBCB))
            .padding(12.dp)
    )
}
