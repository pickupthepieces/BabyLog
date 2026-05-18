package app.babylog.nativeapp

import androidx.compose.material.MaterialTheme
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

object ChestnutPalette {
    val Bg = Color(0xFFFFF6EA)
    val Surface = Color(0xFFFFFDF8)
    val Surface2 = Color(0xFFF8E7D0)
    val Border = Color(0xFFE6CDB2)
    val Ink = Color(0xFF342319)
    val Muted = Color(0xFF745D4D)
    val Text3 = Color(0xFFA88B78)
    val Primary = Color(0xFF8D3E24)
    val PrimarySoft = Color(0xFFF2D7C4)
    val Accent = Color(0xFFC77733)
    val AccentSoft = Color(0xFFFFE8BE)
    val Rose = Color(0xFFC85E64)
    val Blue = Color(0xFF6D8EB6)
    val Violet = Color(0xFF9B7AA5)
    val Green = Color(0xFF5E9169)
    val Yellow = Color(0xFFD9A441)
    val Peach = Color(0xFFD58A55)
    val Danger = Color(0xFFAA4036)

    val BgArgb = 0xFFFFF6EA.toInt()
    val SurfaceArgb = 0xFFFFFDF8.toInt()
    val RoseArgb = 0xFFC85E64.toInt()
    val BlueArgb = 0xFF6D8EB6.toInt()
    val VioletArgb = 0xFF9B7AA5.toInt()
    val GreenArgb = 0xFF5E9169.toInt()
    val YellowArgb = 0xFFD9A441.toInt()
    val PeachArgb = 0xFFD58A55.toInt()
}

@Composable
fun ChestnutTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colors = lightColors(
            primary = ChestnutPalette.Primary,
            primaryVariant = ChestnutPalette.Primary,
            secondary = ChestnutPalette.Accent,
            background = ChestnutPalette.Bg,
            surface = ChestnutPalette.Surface,
            error = ChestnutPalette.Danger,
            onPrimary = Color.White,
            onSecondary = Color.White,
            onBackground = ChestnutPalette.Ink,
            onSurface = ChestnutPalette.Ink,
            onError = Color.White
        ),
        content = content
    )
}
