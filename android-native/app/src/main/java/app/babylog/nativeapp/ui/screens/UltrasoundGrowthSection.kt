@file:Suppress("FunctionNaming", "InvalidPackageDeclaration", "MagicNumber")

package app.babylog.nativeapp

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

internal fun LazyListScope.ultrasoundGrowthSection(
    state: UltrasoundFormState,
    voiceState: SmartVoiceUiState,
    onLongTextVoiceStart: LongTextVoiceStart,
    onLongTextVoiceStop: () -> Unit
) {
    item(key = "growth_header") { Text("胎儿生长指标", color = ChestnutPalette.Ink, fontWeight = FontWeight.Bold) }
    item(key = "growth_bpd_hc") {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            UltrasoundBpdInput(state)
            UltrasoundHcInput(state)
        }
    }
    item(key = "growth_ac_fl") {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            UltrasoundAcInput(state)
            UltrasoundFlInput(state)
        }
    }
    item(key = "efw") { UltrasoundEfwInput(state) }
    item(key = "diagnosis_text_core") {
        UltrasoundDiagnosisInput(state, voiceState, onLongTextVoiceStart, onLongTextVoiceStop)
    }
    item(key = "soft_warnings") { UltrasoundSoftWarnings(state) }
}

@Composable
private fun RowScope.UltrasoundBpdInput(state: UltrasoundFormState) {
    UnitInputRow("BPD", state.bpd, state.updateBpd, "mm", Modifier.weight(1f))
}

@Composable
private fun RowScope.UltrasoundHcInput(state: UltrasoundFormState) {
    UnitInputRow("HC", state.hc, state.updateHc, "mm", Modifier.weight(1f))
}

@Composable
private fun RowScope.UltrasoundAcInput(state: UltrasoundFormState) {
    UnitInputRow("AC", state.ac, state.updateAc, "mm", Modifier.weight(1f))
}

@Composable
private fun RowScope.UltrasoundFlInput(state: UltrasoundFormState) {
    UnitInputRow("FL", state.fl, state.updateFl, "mm", Modifier.weight(1f))
}

@Composable
private fun UltrasoundDiagnosisInput(
    state: UltrasoundFormState,
    voiceState: SmartVoiceUiState,
    onLongTextVoiceStart: LongTextVoiceStart,
    onLongTextVoiceStop: () -> Unit
) {
    ChestnutLongTextField(
        "医生结论 / 提示",
        state.diagnosisText,
        state.updateDiagnosisText,
        minLines = 2,
        maxLines = 4,
        voiceState = voiceState,
        onVoiceStart = onLongTextVoiceStart,
        onVoiceStop = onLongTextVoiceStop
    )
}

@Composable
private fun UltrasoundEfwInput(state: UltrasoundFormState) {
    val estimatedEfw by remember(state) {
        derivedStateOf { state.estimatedEfwGram() }
    }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        UnitInputRow("EFW", state.efw, state.updateEfw, "g")
        val estimate = estimatedEfw
        if (estimate != null) {
            Text(
                "EFW 留空保存时，会按 Hadlock 3 估算为 ${BabyLogFormatters.formatNumber(estimate)} g。",
                color = ChestnutPalette.Muted,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun UltrasoundSoftWarnings(state: UltrasoundFormState) {
    val warnings by remember(state) {
        derivedStateOf { state.softWarnings() }
    }
    if (warnings.isNotBlank()) {
        NoticeBanner(warnings)
    }
}
