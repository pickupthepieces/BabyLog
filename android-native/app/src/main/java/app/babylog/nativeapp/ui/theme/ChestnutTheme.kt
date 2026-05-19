package app.babylog.nativeapp

import androidx.compose.material.MaterialTheme
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

object ChestnutPalette {
    val Bg = Color(0xFFFBF3E9)
    val Surface = Color(0xFFFFFFFF)
    val Surface2 = Color(0xFFF4EADC)
    val Border = Color(0xFFEFE3D2)
    val Ink = Color(0xFF2A1E14)
    val Muted = Color(0xFF7A6452)
    val Text3 = Color(0xFFAE9A86)
    val Primary = Color(0xFFE8896B)
    val PrimarySoft = Color(0xFFFBE3DA)
    val Accent = Color(0xFFD98A4A)
    val AccentSoft = Color(0xFFFFE9C6)
    val Rose = Color(0xFFE79399)
    val Blue = Color(0xFF8AA9CF)
    val Violet = Color(0xFFB99BC5)
    val Green = Color(0xFF88B47D)
    val Yellow = Color(0xFFE9C865)
    val Peach = Color(0xFFE8A173)
    val Danger = Color(0xFFC9544A)

    val BgArgb = 0xFFFBF3E9.toInt()
    val SurfaceArgb = 0xFFFFFFFF.toInt()
    val RoseArgb = 0xFFE79399.toInt()
    val PrimaryArgb = 0xFFE8896B.toInt()
    val BlueArgb = 0xFF8AA9CF.toInt()
    val VioletArgb = 0xFFB99BC5.toInt()
    val GreenArgb = 0xFF88B47D.toInt()
    val YellowArgb = 0xFFE9C865.toInt()
    val PeachArgb = 0xFFE8A173.toInt()
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
