package app.babylog.nativeapp

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.json.JSONArray
import org.json.JSONObject

@Composable
internal fun RecordDetailScreen(
    event: BabyLogDomain.BabyLogEvent?,
    allEvents: List<BabyLogDomain.BabyLogEvent>,
    attachments: List<BabyLogDomain.AttachmentRecord>,
    onBack: () -> Unit,
    onPreviewAttachment: (BabyLogDomain.AttachmentRecord) -> Unit,
    onOpenPreVisitQuestions: () -> Unit,
    onEdit: (BabyLogDomain.BabyLogEvent) -> Unit,
    onDelete: (BabyLogDomain.BabyLogEvent) -> Unit
) {
    val subtitle = event?.let {
        "${BabyLogFormatters.eventLabel(it.eventType)} · ${BabyLogFormatters.formatEventDay(it.occurredAt)} ${BabyLogFormatters.formatEventTime(it.occurredAt)}"
    } ?: "记录不存在或已删除"
    SettingsPageScaffold(
        title = "记录详情",
        subtitle = subtitle,
        onBack = onBack
    ) {
        if (event == null) {
            item { EmptyPanel("这条记录暂时无法查看。") }
            return@SettingsPageScaffold
        }

        item {
            SettingsPanel("摘要") {
                DetailField("类型", BabyLogFormatters.eventLabel(event.eventType))
                DetailField("时间", "${BabyLogFormatters.formatEventDay(event.occurredAt)} ${BabyLogFormatters.formatEventTime(event.occurredAt)}")
                // 类型行已有类型名，内容只放详情；空详情保留占位提示。
                DetailField(
                    "内容",
                    BabyLogFormatters.detailOnlySummary(BabyLogFormatters.eventSummary(event), event.eventType)
                        .ifBlank { "待补充详情" }
                )
            }
        }
        item {
            val fields = remember(event) { payloadFields(event.payload) }
            if (fields.isNotEmpty()) {
                SettingsPanel("记录字段") {
                    fields.forEach { field ->
                        DetailField(field.label, field.value)
                    }
                }
            }
        }
        if (event.eventType == "fetal_movement") {
            item {
                val pattern = remember(allEvents) { fetalMovementPatternPoints(allEvents) }
                FetalMovementPatternPanel(pattern)
            }
        }
        if (event.eventType == "pregnancy_checkup") {
            item {
                SettingsPanel("复诊准备") {
                    DetailField("说明", "可把这次产检想追问的内容加入复诊清单。")
                    OutlinedButton(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                        onClick = onOpenPreVisitQuestions
                    ) {
                        Text("添加给医生的问题", color = ChestnutPalette.Primary, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        item {
            val eventAttachments = remember(event, attachments) {
                attachments.filter { attachment -> event.attachmentIds.contains(attachment.id) }
            }
            SettingsPanel("附件") {
                if (eventAttachments.isEmpty()) {
                    DetailField("附件", "暂无")
                } else {
                    eventAttachments.forEach { attachment ->
                        DetailField(
                            attachment.kind.ifBlank { "附件" },
                            "${attachment.originalName.ifBlank { attachment.id }} · ${BabyLogFormatters.formatByteSize(attachment.byteSize)}"
                        )
                        OutlinedButton(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 4.dp),
                            onClick = { onPreviewAttachment(attachment) }
                        ) {
                            Text("查看附件", color = ChestnutPalette.Primary, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
        item {
            SettingsPanel("元数据") {
                DetailField("记录 ID", event.id)
                DetailField("创建时间", BabyLogFormatters.formatDateTime(event.createdAt))
                DetailField("更新时间", BabyLogFormatters.formatDateTime(event.updatedAt))
                DetailField("更新者", event.updatedBy)
            }
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (isEditablePregnancyRecord(event.eventType)) {
                    Button(
                        modifier = Modifier.weight(1f),
                        onClick = { onEdit(event) },
                        colors = ButtonDefaults.buttonColors(backgroundColor = ChestnutPalette.Primary)
                    ) {
                        Text("编辑", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
                OutlinedButton(
                    modifier = Modifier.weight(1f),
                    onClick = { onDelete(event) }
                ) {
                    Text("删除", color = ChestnutPalette.Danger, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun DetailField(label: String, value: String) {
    if (value.isBlank()) {
        return
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Text(label, color = ChestnutPalette.Muted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Text(value, color = ChestnutPalette.Ink, fontSize = 16.sp)
    }
}

private data class RecordDetailField(
    val label: String,
    val value: String
)

private fun payloadFields(payload: JSONObject?): List<RecordDetailField> {
    if (payload == null) {
        return emptyList()
    }
    val fields = mutableListOf<RecordDetailField>()
    val keys = mutableListOf<String>()
    val iterator = payload.keys()
    while (iterator.hasNext()) {
        keys.add(iterator.next())
    }
    keys.sorted().forEach { key ->
        if (key == "targetCount") {
            return@forEach
        }
        val value = payload.opt(key)
        val text = formatPayloadValue(value)
        if (text.isNotBlank()) {
            fields.add(RecordDetailField(payloadLabel(key), text))
        }
    }
    return fields
}

private fun formatPayloadValue(value: Any?): String {
    return when (value) {
        null, JSONObject.NULL -> ""
        is String -> value.trim()
        is Number, is Boolean -> value.toString()
        is JSONArray, is JSONObject -> value.toString()
        else -> value.toString()
    }
}

private fun payloadLabel(key: String): String {
    return when (key) {
        "examDate" -> "检查日期"
        "gestationalAge" -> "孕周"
        "gestationalAgeDays" -> "孕周天数"
        "hospital" -> "医院"
        "reportTime" -> "报告时间"
        "diagnosisText" -> "超声诊断"
        "checkupDate" -> "检查日期"
        "screeningDate" -> "检查日期"
        "provider" -> "医院 / 机构"
        "department" -> "科室"
        "doctorConclusion" -> "医生结论"
        "finding" -> "结论"
        "treatmentAdvice" -> "处理及建议"
        "reportType" -> "报告类型"
        "bpdMm" -> "BPD"
        "hcMm" -> "HC"
        "acMm" -> "AC"
        "flMm" -> "FL"
        "efwGram" -> "EFW"
        "afiCm" -> "AFI"
        "deepestPocketCm" -> "最大羊水池"
        "placentaLocation" -> "胎盘位置"
        "placentaGrade" -> "胎盘成熟度"
        "fetalPresentation" -> "胎位"
        "fetalHeartRateBpm" -> "胎心率"
        "fetalCount" -> "胎儿个数"
        "fetalMovement" -> "胎动"
        "umbilicalInsertion" -> "脐带插入处"
        "cervicalLengthMm" -> "宫颈管长度"
        "crlMm" -> "CRL"
        "ntMm" -> "NT"
        "umbilicalSd" -> "脐血流 S/D"
        "umbilicalPi" -> "脐血流 PI"
        "umbilicalRi" -> "脐血流 RI"
        "weightKg" -> "体重"
        "systolicBp" -> "收缩压"
        "diastolicBp" -> "舒张压"
        "fundalHeightCm" -> "宫高"
        "abdominalCircumferenceCm" -> "腹围"
        "edema" -> "水肿"
        "urineRoutine" -> "尿常规"
        "urineProtein" -> "尿蛋白"
        "hemoglobinGL" -> "血红蛋白"
        "highRiskFactors" -> "高危因素"
        "glucoseMmolL" -> "血糖"
        "glucoseContext" -> "血糖情境"
        "nextVisitDate" -> "下次产检"
        "nextVisitNote" -> "下次产检备注"
        "riskT21" -> "21 三体风险"
        "riskT18" -> "18 三体风险"
        "riskOntd" -> "开放性神经管风险"
        "riskLevel" -> "报告分级"
        "t21Result" -> "T21"
        "t18Result" -> "T18"
        "t13Result" -> "T13"
        "sexChromosome" -> "性染色体"
        "structureConclusion" -> "结构结论"
        "fastingGlucoseMmolL" -> "空腹血糖"
        "oneHourGlucoseMmolL" -> "1h 血糖"
        "twoHourGlucoseMmolL" -> "2h 血糖"
        "abnormalFlag" -> "报告标注"
        "gbsResult" -> "GBS"
        "nstResult" -> "胎心监护"
        "conclusion" -> "结论"
        "movementWindow" -> "胎动时段"
        "movementCount" -> "胎动次数"
        "entryMode" -> "记录方式"
        "startedAt" -> "开始时间"
        "endedAt" -> "结束时间"
        "durationMinutes" -> "持续分钟"
        "contractionStart" -> "宫缩开始"
        "intervalMinutes" -> "间隔分钟"
        "durationSeconds" -> "持续秒"
        "startIso" -> "开始时间"
        "endIso" -> "结束时间"
        "durationSec" -> "持续秒"
        "sessionId" -> "会话 ID"
        "intervalFromPrevSec" -> "距上次秒数"
        "feedType" -> "喂养方式"
        "amountMl" -> "奶量"
        "sleepStart" -> "睡眠开始"
        "sleepEnd" -> "睡眠结束"
        "sleepPlace" -> "睡眠地点"
        "diaperType" -> "尿布类型"
        "diaperKind" -> "尿布类型编码"
        "diaperDetail" -> "尿布详情"
        "temperatureC" -> "体温"
        "measureMethod" -> "测量方式"
        "medicationName" -> "药名"
        "dosage" -> "剂量"
        "reason" -> "原因"
        "checkupInstitution" -> "儿保机构"
        "checkupConclusion" -> "儿保记录"
        "nextCheckupDate" -> "下次儿保日期"
        "detail" -> "详情"
        "quickAction" -> "快捷记录"
        "attachmentNote" -> "附件备注"
        "note" -> "备注"
        "notes" -> "备注"
        "summary" -> "摘要"
        "warningText" -> "提示"
        else -> key
    }
}

private data class FetalMovementPatternPoint(
    val dayLabel: String,
    val count: Int,
    val durationMinutes: Int
)

@Composable
private fun FetalMovementPatternPanel(points: List<FetalMovementPatternPoint>) {
    SettingsPanel("胎动规律观察") {
        DetailField("说明", "记录本次会话用时与次数，与你以往模式比较。")
        DetailField("通用提示", "胎动规律有改变、明显变少或停止时，请立即联系产科或助产士。")
        if (points.isEmpty()) {
            DetailField("历史模式", "保存几次会话后显示趋势。")
            return@SettingsPanel
        }
        FetalMovementPatternChart(points)
        points.takeLast(5).reversed().forEach { point ->
            DetailField(
                point.dayLabel,
                listOf(
                    "${point.count} 次",
                    if (point.durationMinutes > 0) "${point.durationMinutes} 分钟" else ""
                ).filter { it.isNotBlank() }.joinToString(" · ")
            )
        }
    }
}

@Composable
private fun FetalMovementPatternChart(points: List<FetalMovementPatternPoint>) {
    val countColor = ChestnutPalette.Primary
    val durationColor = ChestnutPalette.Green
    val axisColor = ChestnutPalette.Border
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(148.dp)
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        if (points.isEmpty()) return@Canvas
        val left = 8f
        val right = size.width - 8f
        val top = 12f
        val bottom = size.height - 18f
        val chartWidth = right - left
        val chartHeight = bottom - top
        drawLine(axisColor, Offset(left, bottom), Offset(right, bottom), strokeWidth = 2f)
        drawLine(axisColor, Offset(left, top), Offset(left, bottom), strokeWidth = 2f)
        fun xAt(index: Int): Float {
            return if (points.size == 1) left + chartWidth / 2f else left + chartWidth * index / (points.size - 1)
        }
        fun yAt(value: Int, max: Int): Float {
            val safeMax = max.coerceAtLeast(1)
            return bottom - chartHeight * value.coerceAtLeast(0) / safeMax
        }
        val maxCount = points.maxOf { it.count }.coerceAtLeast(1)
        val maxDuration = points.maxOf { it.durationMinutes }.coerceAtLeast(1)
        fun drawSeries(valueAt: (FetalMovementPatternPoint) -> Int, max: Int, color: Color) {
            points.forEachIndexed { index, point ->
                val current = Offset(xAt(index), yAt(valueAt(point), max))
                if (index > 0) {
                    val previousPoint = points[index - 1]
                    drawLine(
                        color,
                        Offset(xAt(index - 1), yAt(valueAt(previousPoint), max)),
                        current,
                        strokeWidth = 4f
                    )
                }
                drawCircle(color, radius = 5f, center = current)
            }
        }
        drawSeries({ it.durationMinutes }, maxDuration, durationColor)
        drawSeries({ it.count }, maxCount, countColor)
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("次数", color = ChestnutPalette.Primary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Text("用时", color = ChestnutPalette.Green, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

private fun fetalMovementPatternPoints(events: List<BabyLogDomain.BabyLogEvent>): List<FetalMovementPatternPoint> {
    return events
        .filter { it.deletedAt == null && it.eventType == "fetal_movement" }
        .sortedBy { it.occurredAt }
        .mapNotNull { event ->
            val payload = event.payload ?: return@mapNotNull null
            val count = payload.optInt("movementCount", 0)
            val duration = payload.optInt("durationMinutes", 0)
            if (count <= 0 && duration <= 0) {
                null
            } else {
                FetalMovementPatternPoint(
                    BabyLogFormatters.formatEventDay(event.occurredAt),
                    count.coerceAtLeast(0),
                    duration.coerceAtLeast(0)
                )
            }
        }
        .takeLast(14)
}
