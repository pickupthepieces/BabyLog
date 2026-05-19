package app.babylog.nativeapp

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
internal fun MaternalMetricFormScreen(
    draft: SmartEntryDraft?,
    isEditing: Boolean = false,
    onBack: () -> Unit,
    onSave: (BabyLogService.MaternalMetricInput) -> Unit
) {
    val values = draft?.values.orEmpty()
    var weight by rememberSaveable(draft?.nonce) { mutableStateOf(values["weightKg"].orEmpty()) }
    var systolic by rememberSaveable(draft?.nonce) { mutableStateOf(values["systolicBp"].orEmpty()) }
    var diastolic by rememberSaveable(draft?.nonce) { mutableStateOf(values["diastolicBp"].orEmpty()) }
    var glucose by rememberSaveable(draft?.nonce) { mutableStateOf(values["glucoseMmolL"].orEmpty()) }
    var glucoseContext by rememberSaveable(draft?.nonce) {
        mutableStateOf(values["glucoseContext"]?.let { normalizeGlucoseContext(it) } ?: "fasting")
    }
    var note by rememberSaveable(draft?.nonce) { mutableStateOf(values["note"].orEmpty()) }
    val glucoseWarning = BabyLogFormatters.formatMaternalGlucoseWarning(
        BabyLogFormatters.parseOptionalNumber(glucose),
        glucoseContext
    )

    RecordFormScaffold(
        title = if (isEditing) "编辑孕妈指标" else "孕妈指标",
        subtitle = "体重、血压、血糖用于家庭趋势记录",
        saveText = if (isEditing) "保存修改" else "保存孕妈指标",
        onBack = onBack,
        onSave = {
            onSave(
                BabyLogService.MaternalMetricInput.create(
                    weight,
                    systolic,
                    diastolic,
                    glucose,
                    glucoseContext,
                    note
                )
            )
        }
    ) {
        item { UnitInputRow("体重", weight, { weight = it }, "kg") }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                UnitInputRow("收缩压", systolic, { systolic = it }, "mmHg", Modifier.weight(1f))
                UnitInputRow("舒张压", diastolic, { diastolic = it }, "mmHg", Modifier.weight(1f))
            }
        }
        item { UnitInputRow("血糖", glucose, { glucose = it }, "mmol/L") }
        item { GlucoseContextRow(glucoseContext, onSelect = { glucoseContext = it }) }
        if (glucoseWarning.isNotBlank()) {
            item {
                Text(
                    text = glucoseWarning,
                    color = ChestnutPalette.Accent,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        item { ChestnutLongTextField("备注，可空", note, { note = it }) }
        item {
            Text(
                text = "血糖提示仅用于提醒复核，不构成诊断",
                color = ChestnutPalette.Text3,
                fontSize = 12.sp
            )
        }
    }
}
