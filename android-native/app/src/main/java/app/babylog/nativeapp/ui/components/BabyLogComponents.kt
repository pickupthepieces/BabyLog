package app.babylog.nativeapp

import android.app.DatePickerDialog
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Card
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.material.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

@Composable
fun Panel(content: @Composable ColumnScope.() -> Unit) {
    Card(
        shape = RoundedCornerShape(16.dp),
        backgroundColor = ChestnutPalette.Surface,
        border = null,
        elevation = 0.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            content = content
        )
    }
}

@Composable
fun MetricCard(
    title: String,
    value: String,
    subtitle: String,
    tone: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(ChestnutPalette.Surface2.copy(alpha = 0.72f))
            .padding(11.dp)
            .height(76.dp)
    ) {
        Box(
            modifier = Modifier
                .width(28.dp)
                .height(3.dp)
                .clip(CircleShape)
                .background(tone)
        )
        Spacer(Modifier.height(7.dp))
        Text(title, color = ChestnutPalette.Muted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        Text(value, color = ChestnutPalette.Ink, fontSize = 20.sp, fontWeight = FontWeight.Bold, maxLines = 1)
        Text(subtitle, color = ChestnutPalette.Text3, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
fun TrendCard(
    title: String,
    value: String,
    subtitle: String,
    tone: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        backgroundColor = ChestnutPalette.Surface,
        border = null,
        elevation = 0.dp
    ) {
        Column(Modifier.padding(14.dp)) {
            Box(
                modifier = Modifier
                    .width(34.dp)
                    .height(3.dp)
                    .background(tone)
            )
            Spacer(Modifier.height(10.dp))
            Text(title, color = ChestnutPalette.Muted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text(value, color = ChestnutPalette.Ink, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text(subtitle, color = ChestnutPalette.Muted, fontSize = 12.sp)
        }
    }
}

@Composable
fun SectionHeader(title: String, action: String? = null, onAction: (() -> Unit)? = null) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(18.dp)
                .clip(CircleShape)
                .background(ChestnutPalette.Primary)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            title,
            color = ChestnutPalette.Ink,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f)
        )
        if (action != null) {
            Text(
                action,
                color = if (onAction == null) ChestnutPalette.Text3 else ChestnutPalette.Primary,
                fontWeight = FontWeight.Bold,
                modifier = if (onAction == null) Modifier else Modifier.clickable { onAction() }
            )
        }
    }
}

@Composable
fun Chip(text: String, bg: Color, fg: Color) {
    Text(
        text = text,
        color = fg,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .clip(CircleShape)
            .background(bg)
            .padding(horizontal = 11.dp, vertical = 6.dp)
    )
}

@Composable
fun EmptyPanel(text: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 20.dp, horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = text,
            color = ChestnutPalette.Text3,
            fontSize = 13.sp,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun UnitInputRow(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    unit: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(58.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(ChestnutPalette.Surface),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label) },
            modifier = Modifier.weight(1f),
            singleLine = true,
            textStyle = TextStyle(fontFeatureSettings = "tnum"),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            colors = TextFieldDefaults.textFieldColors(
                backgroundColor = ChestnutPalette.Surface,
                focusedIndicatorColor = ChestnutPalette.Primary,
                unfocusedIndicatorColor = ChestnutPalette.Border,
                textColor = ChestnutPalette.Ink,
                focusedLabelColor = ChestnutPalette.Primary,
                unfocusedLabelColor = ChestnutPalette.Muted,
                cursorColor = ChestnutPalette.Primary
            )
        )
        if (unit.isNotBlank()) {
            Box(
                modifier = Modifier
                    .height(58.dp)
                    .background(ChestnutPalette.PrimarySoft)
                    .padding(horizontal = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    unit,
                    color = ChestnutPalette.Primary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun GlucoseContextRow(selected: String, onSelect: (String) -> Unit) {
    val options = listOf(
        "fasting" to "空腹",
        "after_1h" to "餐后1h",
        "after_2h" to "餐后2h",
        "random" to "随机"
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        options.forEach { (key, label) ->
            OutlinedButton(
                onClick = { onSelect(key) },
                border = BorderStroke(1.dp, if (selected == key) ChestnutPalette.Primary else ChestnutPalette.Border),
                colors = ButtonDefaults.outlinedButtonColors(
                    backgroundColor = if (selected == key) ChestnutPalette.PrimarySoft else ChestnutPalette.Surface,
                    contentColor = ChestnutPalette.Ink
                ),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 7.dp)
            ) {
                Text(label, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun ChoiceChipRow(
    label: String,
    selected: String,
    options: List<Pair<String, String>>,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(label, color = ChestnutPalette.Muted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            options.forEach { (value, text) ->
                val active = selected == value
                OutlinedButton(
                    onClick = { onSelect(value) },
                    border = BorderStroke(1.dp, if (active) ChestnutPalette.Primary else ChestnutPalette.Border),
                    colors = ButtonDefaults.outlinedButtonColors(
                        backgroundColor = if (active) ChestnutPalette.PrimarySoft else ChestnutPalette.Surface,
                        contentColor = if (active) ChestnutPalette.Primary else ChestnutPalette.Ink
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 7.dp)
                ) {
                    Text(text, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun DateInputRow(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    allowClear: Boolean = true
) {
    val context = LocalContext.current
    val openPicker = {
        val calendar = calendarFromDate(value)
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                onValueChange(String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, dayOfMonth))
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(58.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(ChestnutPalette.Surface)
            .border(1.dp, ChestnutPalette.Border, RoundedCornerShape(4.dp))
            .clickable { openPicker() }
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(label, color = ChestnutPalette.Muted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Text(
                text = value.ifBlank { "未填写" },
                color = if (value.isBlank()) ChestnutPalette.Text3 else ChestnutPalette.Ink,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                style = TextStyle(fontFeatureSettings = "tnum")
            )
        }
        Text(
            text = "选择",
            color = ChestnutPalette.Primary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp)
        )
        if (allowClear && value.isNotBlank()) {
            Text(
                text = "清空",
                color = ChestnutPalette.Text3,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .clickable { onValueChange("") }
                    .padding(horizontal = 6.dp, vertical = 6.dp)
            )
        }
    }
}

@Composable
fun ChestnutTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    keyboardType: KeyboardType,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
    minLines: Int = 1,
    maxLines: Int = if (singleLine) 1 else 6,
    placeholder: String? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = placeholder?.let { hint -> { Text(hint, color = ChestnutPalette.Text3) } },
        modifier = modifier.fillMaxWidth(),
        singleLine = singleLine,
        minLines = if (singleLine) 1 else minLines,
        maxLines = if (singleLine) 1 else maxLines,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        visualTransformation = visualTransformation,
        colors = TextFieldDefaults.textFieldColors(
            backgroundColor = ChestnutPalette.Surface,
            focusedIndicatorColor = ChestnutPalette.Primary,
            unfocusedIndicatorColor = ChestnutPalette.Border,
            textColor = ChestnutPalette.Ink,
            focusedLabelColor = ChestnutPalette.Primary,
            unfocusedLabelColor = ChestnutPalette.Muted,
            cursorColor = ChestnutPalette.Primary
        )
    )
}

@Composable
fun ChestnutLongTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    minLines: Int = 3,
    maxLines: Int = 7
) {
    ChestnutTextField(
        label = label,
        value = value,
        onValueChange = onValueChange,
        keyboardType = KeyboardType.Text,
        modifier = modifier,
        singleLine = false,
        minLines = minLines,
        maxLines = maxLines,
        placeholder = "可使用键盘语音输入，保存前请核对"
    )
}

private fun calendarFromDate(value: String): Calendar {
    val calendar = Calendar.getInstance(TimeZone.getTimeZone("Asia/Shanghai"))
    if (!BabyLogFormatters.isValidDateInput(value)) {
        return calendar
    }
    val format = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    format.timeZone = TimeZone.getTimeZone("Asia/Shanghai")
    format.isLenient = false
    return try {
        val parsed = format.parse(value)
        if (parsed != null) {
            calendar.time = parsed
        }
        calendar
    } catch (_: Exception) {
        calendar
    }
}
