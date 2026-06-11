package app.babylog.nativeapp

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Card
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.material.TextFieldDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
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

internal typealias LongTextVoiceStart = ((String) -> Unit) -> Unit

data class Option(val value: String, val label: String)

/** 统一的提示条：边界说明、隐私提示、保留策略等弱警示文案。 */
@Composable
@Suppress("FunctionNaming")
fun NoticeBanner(text: String) {
    Text(
        text,
        color = ChestnutPalette.Notice,
        fontSize = 13.sp,
        lineHeight = 19.sp,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(ChestnutRadius.Small))
            .background(ChestnutPalette.NoticeBg)
            .padding(12.dp)
    )
}

private const val GESTATIONAL_WEEK_MAX_DIGITS = 2
private const val GESTATIONAL_DAY_MAX_DIGITS = 1
private const val GESTATIONAL_MAX_EXTRA_DAYS = 6

@Composable
internal fun Modifier.babyLogPressScale(
    interactionSource: InteractionSource,
    pressedScale: Float = 0.95f
): Modifier {
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) pressedScale else 1f,
        label = "babyLogPressScale"
    )
    return graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}

@Composable
fun Panel(content: @Composable ColumnScope.() -> Unit) {
    Card(
        shape = RoundedCornerShape(ChestnutRadius.Card),
        backgroundColor = ChestnutPalette.Surface,
        border = BorderStroke(1.dp, ChestnutPalette.Border.copy(alpha = 0.36f)),
        elevation = 0.dp
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
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
            .clip(RoundedCornerShape(ChestnutRadius.Card))
            .background(ChestnutPalette.Surface2.copy(alpha = 0.64f))
            .padding(13.dp)
            .heightIn(min = 82.dp)
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
        shape = RoundedCornerShape(ChestnutRadius.Card),
        backgroundColor = ChestnutPalette.Surface,
        border = BorderStroke(1.dp, ChestnutPalette.Border.copy(alpha = 0.28f)),
        elevation = 0.dp
    ) {
        Column(Modifier.padding(16.dp)) {
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
        Text(
            title,
            color = ChestnutPalette.Ink,
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f)
        )
        if (action != null && onAction != null) {
            Text(
                action,
                color = ChestnutPalette.Primary,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.clickable { onAction() }
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
            .padding(vertical = 12.dp, horizontal = 16.dp),
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
fun ChestnutSyncBanner(
    count: Int,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (count <= 0) {
        return
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(ChestnutRadius.Control))
            .background(ChestnutPalette.PrimarySoft)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Rounded.Sync,
            contentDescription = null,
            tint = ChestnutPalette.Primary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = "已同步 $count 条家人更新",
            color = ChestnutPalette.Ink,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f)
        )
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .clickable { onDismiss() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.Close,
                contentDescription = "关闭同步提示",
                tint = ChestnutPalette.Muted,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun BabyLogPullRefreshContainer(
    refreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val pullState = rememberPullRefreshState(refreshing = refreshing, onRefresh = onRefresh)
    Box(modifier = modifier.pullRefresh(pullState)) {
        content()
        PullRefreshIndicator(
            refreshing = refreshing,
            state = pullState,
            modifier = Modifier.align(Alignment.TopCenter),
            backgroundColor = ChestnutPalette.Surface,
            contentColor = ChestnutPalette.Primary
        )
    }
}

@Suppress("FunctionNaming", "LongParameterList")
@Composable
fun UnitInputRow(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    unit: String,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Decimal
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(58.dp)
            .clip(RoundedCornerShape(ChestnutRadius.Control))
            .background(ChestnutPalette.Surface)
            .border(1.dp, ChestnutPalette.Border.copy(alpha = 0.55f), RoundedCornerShape(ChestnutRadius.Control)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label) },
            modifier = Modifier.weight(1f),
            singleLine = true,
            textStyle = TextStyle(fontFeatureSettings = "tnum"),
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            colors = TextFieldDefaults.textFieldColors(
                backgroundColor = ChestnutPalette.Surface,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
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

@Suppress("FunctionNaming", "MagicNumber")
@Composable
fun GestationalAgeInputRow(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val parts = remember(value) { splitGestationalAgeInput(value) }
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        UnitInputRow(
            label = "$label（周）",
            value = parts.weeks,
            onValueChange = {
                val weeks = cleanGestationalNumber(it, maxLength = GESTATIONAL_WEEK_MAX_DIGITS)
                onValueChange(joinGestationalAgeInput(weeks, parts.days))
            },
            unit = "周",
            modifier = Modifier.weight(1f),
            keyboardType = KeyboardType.Number
        )
        UnitInputRow(
            label = "天（0-6）",
            value = parts.days,
            onValueChange = {
                val days = cleanGestationalDays(it)
                onValueChange(joinGestationalAgeInput(parts.weeks, days))
            },
            unit = "天",
            modifier = Modifier.weight(1f),
            keyboardType = KeyboardType.Number
        )
    }
}

private data class GestationalAgeParts(val weeks: String, val days: String)

private fun splitGestationalAgeInput(value: String): GestationalAgeParts {
    val normalized = value
        .trim()
        .replace(" ", "")
        .replace("　", "")
        .replace("＋", "+")
        .replace("週", "+")
        .replace("周", "+")
        .replace("W", "+")
        .replace("w", "+")
        .replace("天", "")
        .replace("D", "")
        .replace("d", "")
    val chunks = normalized.split("+", limit = 2)
    val weeks = cleanGestationalNumber(chunks.getOrNull(0).orEmpty(), maxLength = GESTATIONAL_WEEK_MAX_DIGITS)
    val days = cleanGestationalDays(chunks.getOrNull(1).orEmpty())
    return GestationalAgeParts(weeks, days)
}

private fun joinGestationalAgeInput(weeks: String, days: String): String {
    val cleanWeeks = cleanGestationalNumber(weeks, maxLength = GESTATIONAL_WEEK_MAX_DIGITS)
    if (cleanWeeks.isBlank()) {
        return ""
    }
    val cleanDays = cleanGestationalDays(days)
    return if (cleanDays.isBlank()) cleanWeeks else "$cleanWeeks+$cleanDays"
}

private fun cleanGestationalNumber(value: String, maxLength: Int): String {
    return value.filter { it.isDigit() }.take(maxLength)
}

private fun cleanGestationalDays(value: String): String {
    val digits = cleanGestationalNumber(value, maxLength = GESTATIONAL_DAY_MAX_DIGITS)
    return digits.takeIf { it.isBlank() || it.toInt() <= GESTATIONAL_MAX_EXTRA_DAYS }.orEmpty()
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

@Suppress("FunctionNaming", "LongParameterList", "MagicNumber")
@Composable
fun ChoiceChipsRow(
    label: String,
    options: List<Option>,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    allowCustom: Boolean = false,
    customLabel: String = "其它",
    customPlaceholder: String = "请输入其它内容"
) {
    val optionValues = remember(options) { options.map { it.value }.toSet() }
    val trimmedValue = value.trim()
    val customSelected = allowCustom && trimmedValue.isNotBlank() && trimmedValue !in optionValues
    var customMode by remember(trimmedValue, customSelected) { mutableStateOf(customSelected) }
    var customDraft by remember(customSelected, trimmedValue) {
        mutableStateOf(if (customSelected) value else "")
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Text(label, color = ChestnutPalette.Muted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            options.forEach { option ->
                ChoiceChipButton(
                    text = option.label,
                    active = value == option.value,
                    onClick = {
                        customMode = false
                        customDraft = ""
                        onValueChange(option.value)
                    }
                )
            }
            if (allowCustom) {
                ChoiceChipButton(
                    text = customLabel,
                    active = customSelected || customMode,
                    onClick = {
                        customMode = true
                        customDraft = if (customSelected) value else ""
                        onValueChange(customDraft)
                    }
                )
            }
        }
        if (allowCustom && (customSelected || customMode)) {
            Spacer(Modifier.height(8.dp))
            ChestnutTextField(
                label = customPlaceholder,
                value = customDraft,
                onValueChange = {
                    customDraft = it
                    onValueChange(it)
                },
                keyboardType = KeyboardType.Text
            )
        }
    }
}

@Suppress("FunctionNaming", "MagicNumber")
@Composable
private fun ChoiceChipButton(
    text: String,
    active: Boolean,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
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
            .clip(RoundedCornerShape(ChestnutRadius.Control))
            .background(ChestnutPalette.Surface)
            .border(1.dp, ChestnutPalette.Border.copy(alpha = 0.55f), RoundedCornerShape(ChestnutRadius.Control))
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

@Suppress("FunctionNaming", "MagicNumber")
@Composable
fun TimeInputRow(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    allowClear: Boolean = true
) {
    val context = LocalContext.current
    val openPicker = {
        val (hour, minute) = clockFromTime(value)
        TimePickerDialog(
            context,
            { _, selectedHour, selectedMinute ->
                onValueChange(String.format(Locale.US, "%02d:%02d", selectedHour, selectedMinute))
            },
            hour,
            minute,
            true
        ).show()
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(58.dp)
            .clip(RoundedCornerShape(ChestnutRadius.Control))
            .background(ChestnutPalette.Surface)
            .border(1.dp, ChestnutPalette.Border.copy(alpha = 0.55f), RoundedCornerShape(ChestnutRadius.Control))
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
    visualTransformation: VisualTransformation = VisualTransformation.None,
    trailingIcon: @Composable (() -> Unit)? = null
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = placeholder?.let { hint -> { Text(hint, color = ChestnutPalette.Text3) } },
        trailingIcon = trailingIcon,
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(ChestnutRadius.Control))
            .border(1.dp, ChestnutPalette.Border.copy(alpha = 0.55f), RoundedCornerShape(ChestnutRadius.Control)),
        singleLine = singleLine,
        minLines = if (singleLine) 1 else minLines,
        maxLines = if (singleLine) 1 else maxLines,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        visualTransformation = visualTransformation,
        shape = RoundedCornerShape(ChestnutRadius.Control),
        colors = TextFieldDefaults.textFieldColors(
            backgroundColor = ChestnutPalette.Surface,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            textColor = ChestnutPalette.Ink,
            focusedLabelColor = ChestnutPalette.Primary,
            unfocusedLabelColor = ChestnutPalette.Muted,
            cursorColor = ChestnutPalette.Primary
        )
    )
}

@Composable
internal fun ChestnutLongTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    minLines: Int = 3,
    maxLines: Int = 7,
    voiceState: SmartVoiceUiState? = null,
    onVoiceStart: LongTextVoiceStart? = null,
    onVoiceStop: (() -> Unit)? = null
) {
    val currentValue by rememberUpdatedState(value)
    val currentOnValueChange by rememberUpdatedState(onValueChange)
    ChestnutTextField(
        label = label,
        value = value,
        onValueChange = onValueChange,
        keyboardType = KeyboardType.Text,
        modifier = modifier,
        singleLine = false,
        minLines = minLines,
        maxLines = maxLines,
        placeholder = "可使用键盘语音输入，保存前请核对",
        trailingIcon = if (voiceState != null && onVoiceStart != null && onVoiceStop != null) {
            {
                LongTextVoiceButton(
                    voiceState = voiceState,
                    onVoiceStart = {
                        onVoiceStart { transcript ->
                            val merged = appendVoiceTranscript(currentValue, transcript)
                            if (merged != currentValue) {
                                currentOnValueChange(merged)
                            }
                        }
                    },
                    onVoiceStop = onVoiceStop
                )
            }
        } else {
            null
        }
    )
}

private fun appendVoiceTranscript(current: String, transcript: String): String {
    val clean = transcript.trim()
    if (clean.isBlank()) return current
    return if (current.isBlank()) {
        clean
    } else {
        current.trimEnd() + "\n\n" + clean
    }
}

@Composable
private fun LongTextVoiceButton(
    voiceState: SmartVoiceUiState,
    onVoiceStart: () -> Unit,
    onVoiceStop: () -> Unit
) {
    val currentOnVoiceStart by rememberUpdatedState(onVoiceStart)
    val currentOnVoiceStop by rememberUpdatedState(onVoiceStop)
    val enabled = !voiceState.isTranscribing
    val active = voiceState.isRecording
    val bg = when {
        active -> ChestnutPalette.Primary
        voiceState.isTranscribing -> ChestnutPalette.Surface2
        else -> ChestnutPalette.Primary.copy(alpha = 0.12f)
    }
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(bg)
            .border(1.dp, ChestnutPalette.Primary.copy(alpha = if (active) 0.9f else 0.32f), CircleShape)
            .then(
                if (enabled) {
                    Modifier.pointerInput(Unit) {
                        detectTapGestures(
                            onPress = {
                                currentOnVoiceStart()
                                try {
                                    tryAwaitRelease()
                                } finally {
                                    currentOnVoiceStop()
                                }
                            }
                        )
                    }
                } else {
                    Modifier
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        BabyLogMaterialIcon(
            icon = LineIcon.Voice,
            tint = if (active) Color.White else ChestnutPalette.Primary,
            modifier = Modifier.size(20.dp)
        )
    }
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

@Suppress("ComplexCondition", "MagicNumber")
private fun clockFromTime(value: String): Pair<Int, Int> {
    val parts = value.split(":")
    if (parts.size == 2) {
        val hour = parts[0].toIntOrNull()
        val minute = parts[1].toIntOrNull()
        if (hour != null && minute != null && hour in 0..23 && minute in 0..59) {
            return hour to minute
        }
    }
    val calendar = Calendar.getInstance(TimeZone.getTimeZone("Asia/Shanghai"))
    return calendar.get(Calendar.HOUR_OF_DAY) to calendar.get(Calendar.MINUTE)
}
