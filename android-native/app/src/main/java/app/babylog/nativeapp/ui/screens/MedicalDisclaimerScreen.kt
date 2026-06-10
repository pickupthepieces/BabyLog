@file:Suppress("FunctionNaming")

package app.babylog.nativeapp

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Card
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
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
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            item {
                MedicalDisclaimerHeader()
            }
            item {
                MedicalDisclaimerBody(showFull = showFull)
            }
            item {
                DisclaimerDisclosureRow(
                    expanded = showFull,
                    onClick = { showFull = !showFull }
                )
            }
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(ChestnutPalette.Surface)
        ) {
            Divider(color = ChestnutPalette.Border.copy(alpha = 0.55f))
            Button(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
                    .height(52.dp),
                onClick = onAccept,
                shape = RoundedCornerShape(ChestnutRadius.Small),
                colors = ButtonDefaults.buttonColors(backgroundColor = ChestnutPalette.Primary)
            ) {
                Text("同意并继续", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 17.sp)
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
        subtitle = "家庭记录边界与 AI 候选说明",
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
            text = "栗记",
            color = ChestnutPalette.Primary,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = "医疗免责声明",
            color = ChestnutPalette.Ink,
            fontSize = 34.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.sp
        )
        Text(
            text = "本应用用于家庭记录与复诊沟通；所有参考范围、曲线、提醒和 AI 候选均受本页说明约束。",
            color = ChestnutPalette.Muted,
            fontSize = 15.sp,
            lineHeight = 22.sp
        )
    }
}

@Composable
private fun MedicalDisclaimerBody(
    showFull: Boolean
) {
    Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
        DisclaimerGroup(
            title = "使用边界",
            rows = listOf(
                "记录工具" to "用于整理孕育与育儿记录；不是医疗器械或诊疗软件。",
                "统一适用" to "本页声明适用于所有记录、参考范围、曲线、提醒、导出摘要和 AI 候选。",
                "参考信息" to "范围提示、百分位、Z-score、曲线和趋势仅用于记录与沟通，不构成医学结论。",
                "AI 结果需核对" to "OCR、语音和大模型只生成候选内容，保存前请人工核对。",
                "紧急情况" to "涉及健康、妊娠、胎儿或婴幼儿安全的问题，请及时联系医生。"
            )
        )
        DisclaimerGroup(
            title = "风险提示",
            rows = listOf(
                "FGR 参考未校准" to "当前仍为近似参考，可能存在误差；软范围和复核提示不等同筛查或诊断。"
            )
        )
        if (showFull) {
            DisclaimerGroup(
                title = "完整声明",
                rows = listOf(
                    "不构成医患关系" to "使用本应用不构成医患关系，作者与贡献者不是你的医疗服务提供者。",
                    "不要自行诊疗" to "请勿依据应用内容自行诊断、用药或调整治疗方案。",
                    "按现状提供" to "本项目按现状提供，不承诺准确性、完整性、及时性或特定用途适用性。",
                    "责任边界" to "在法律允许范围内，作者与贡献者不对使用本项目造成的损害承担责任。",
                    "自用范围" to "本项目面向个人/家庭自用，不用于临床、商业运营或第三方医疗服务。",
                    "再分发责任" to "fork、修改、再分发或构建其他产品时，使用者需自行承担合规责任。",
                    "法律与合规" to "本声明不构成法律意见；公开发布或分发前请咨询专业人士。"
                )
            )
        }
        Text(
            text = "如应用提示与免责声明不一致，请以更保守、更安全的解释为准。",
            color = ChestnutPalette.Text3,
            fontSize = 13.sp,
            lineHeight = 19.sp
        )
    }
}

@Composable
private fun DisclaimerDisclosureRow(
    expanded: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(ChestnutRadius.Small),
        backgroundColor = ChestnutPalette.Surface,
        border = BorderStroke(1.dp, ChestnutPalette.Border.copy(alpha = 0.45f)),
        elevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 52.dp)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (expanded) "收起完整声明" else "查看完整声明",
                color = ChestnutPalette.Primary,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = if (expanded) "⌃" else "›",
                color = ChestnutPalette.Text3,
                fontSize = 24.sp
            )
        }
    }
}

@Composable
private fun DisclaimerGroup(
    title: String,
    rows: List<Pair<String, String>>
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            color = ChestnutPalette.Text3,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(ChestnutRadius.Small),
            backgroundColor = ChestnutPalette.Surface,
            border = BorderStroke(1.dp, ChestnutPalette.Border.copy(alpha = 0.45f)),
            elevation = 0.dp
        ) {
            Column {
                rows.forEachIndexed { index, row ->
                    DisclaimerRow(title = row.first, body = row.second)
                    if (index < rows.lastIndex) {
                        Divider(
                            modifier = Modifier.padding(start = 16.dp),
                            color = ChestnutPalette.Border.copy(alpha = 0.55f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DisclaimerRow(
    title: String,
    body: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 70.dp)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(title, color = ChestnutPalette.Ink, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
        Text(body, color = ChestnutPalette.Muted, fontSize = 14.sp, lineHeight = 20.sp)
    }
}
