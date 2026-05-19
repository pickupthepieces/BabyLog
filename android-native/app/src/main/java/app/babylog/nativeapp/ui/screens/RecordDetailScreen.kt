package app.babylog.nativeapp

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.json.JSONArray
import org.json.JSONObject

@Composable
internal fun RecordDetailScreen(
    event: BabyLogDomain.BabyLogEvent?,
    attachments: List<BabyLogDomain.AttachmentRecord>,
    onBack: () -> Unit,
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
                DetailField("内容", BabyLogFormatters.eventSummary(event))
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
                    }
                }
            }
        }
        item {
            SettingsPanel("元数据") {
                DetailField("记录 ID", event.id)
                DetailField("来源", event.source)
                DetailField("创建时间", event.createdAt)
                DetailField("更新时间", event.updatedAt)
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
        "department" -> "科室"
        "doctorConclusion" -> "医生结论"
        "notes" -> "备注"
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
        "umbilicalSd" -> "脐血流 S/D"
        "umbilicalPi" -> "脐血流 PI"
        "umbilicalRi" -> "脐血流 RI"
        "weightKg" -> "体重"
        "systolic" -> "收缩压"
        "diastolic" -> "舒张压"
        "glucoseMmolL" -> "血糖"
        "glucoseContext" -> "血糖情境"
        "nextVisitDate" -> "下次产检"
        else -> key
    }
}
