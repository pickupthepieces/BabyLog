package app.babylog.nativeapp

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
internal fun MedicalDisclaimerGateScreen(
    onAccept: () -> Unit
) {
    BackHandler(enabled = true) {
        // Blocking first-run gate: the only exit is explicit acceptance.
    }
    var showFull by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ChestnutPalette.Bg)
    ) {
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 22.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                MedicalDisclaimerHeader()
            }
            item {
                MedicalDisclaimerBody(showFull = showFull)
            }
            item {
                OutlinedButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { showFull = !showFull }
                ) {
                    Text(if (showFull) "收起完整声明" else "查看完整声明")
                }
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(ChestnutPalette.Surface)
                .padding(horizontal = 18.dp, vertical = 14.dp)
        ) {
            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = onAccept,
                colors = ButtonDefaults.buttonColors(backgroundColor = ChestnutPalette.Primary)
            ) {
                Text("我已阅读并同意", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
internal fun MedicalDisclaimerReviewScreen(
    onBack: () -> Unit
) {
    SettingsPageScaffold(
        title = "医疗免责声明",
        subtitle = "可随时复阅；BabyLog 只做家庭记录",
        onBack = onBack
    ) {
        item {
            MedicalDisclaimerBody(showFull = true)
        }
    }
}

@Composable
private fun MedicalDisclaimerHeader() {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "医疗免责声明",
            color = ChestnutPalette.Ink,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "继续使用前，请先确认 BabyLog 的用途边界。",
            color = ChestnutPalette.Muted,
            fontSize = 14.sp
        )
    }
}

@Composable
private fun MedicalDisclaimerBody(
    showFull: Boolean
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = ChestnutPalette.Surface,
        shape = RoundedCornerShape(18.dp),
        elevation = 0.dp
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            DisclaimerSection(
                title = "请先确认",
                lines = listOf(
                    "BabyLog 是个人/家庭自用的孕育与育儿记录工具，不是医疗器械，也不是医疗用途软件。",
                    "应用内数值、范围提示、百分位、Z-score、生长曲线和趋势图仅供家庭记录与复诊沟通参考，不构成医学结论。",
                    "FGR 参考引擎仍是未校准近似实现，可能存在显著误差；软范围/复核提示不是筛查、诊断或结果判定。",
                    "OCR、语音转文字和大模型结构化都可能识别错误；所有识别结果只是候选，必须由你人工核对并手动保存。",
                    "任何健康、妊娠、胎儿或婴幼儿相关判断，请始终咨询具备资质的医疗专业人员；紧急或疑虑情况请线下就医。"
                )
            )
            if (showFull) {
                DisclaimerSection(
                    title = "完整声明",
                    lines = listOf(
                        "使用本应用不构成医患关系，作者与贡献者不是你的医疗服务提供者。",
                        "切勿依据本应用显示内容自行诊断、自行用药或调整医生治疗方案。",
                        "本项目按“现状”（AS IS）提供，不附带准确性、完整性、及时性或特定用途适用性担保。",
                        "在适用法律允许的最大范围内，作者与贡献者不对使用或无法使用本项目产生的损害承担责任。",
                        "本项目面向个人/家庭自用，不面向临床使用、商业化运营或向第三方提供医疗相关服务。",
                        "若 fork、修改、再分发或基于本项目构建其他产品，上述免责条款同样适用，使用者需自行承担合规与责任。",
                        "本声明不构成法律意见；如需公开发布或分发，请先咨询专业法律与医疗合规人士。"
                    )
                )
            }
            Text(
                text = "如应用内提示与免责声明存在冲突，以更保守、更有利于用户安全的解释为准。",
                color = ChestnutPalette.Muted,
                fontSize = 13.sp
            )
        }
    }
}

@Composable
private fun DisclaimerSection(
    title: String,
    lines: List<String>
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, color = ChestnutPalette.Ink, fontWeight = FontWeight.Bold, fontSize = 17.sp)
        lines.forEach { line ->
            Text(
                text = "• $line",
                color = ChestnutPalette.Ink,
                fontSize = 14.sp,
                lineHeight = 21.sp
            )
        }
    }
}
