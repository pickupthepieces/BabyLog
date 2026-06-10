@file:Suppress("MagicNumber")

package app.babylog.nativeapp

import androidx.compose.material.MaterialTheme
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

object ChestnutPalette {
    val Bg = Color(0xFFFFF7EE)
    val Surface = Color(0xFFFFFFFF)
    val Surface2 = Color(0xFFF8EDDF)
    val Border = Color(0xFFE9D9C6)
    val Ink = Color(0xFF2A1B12)
    val Muted = Color(0xFF7A6250)
    val Text3 = Color(0xFFA48B76)
    val Primary = Color(0xFF8B4A24)
    val SplashBg = Color(0xFFFFEFE5)
    val PrimarySoft = Color(0xFFF8E3D4)
    val Accent = Color(0xFFD9823B)
    val AccentSoft = Color(0xFFFFE8C7)
    val Rose = Color(0xFFC95C54)
    val Blue = Color(0xFF4F8F9D)
    val Violet = Color(0xFF8A6E9F)
    val Green = Color(0xFF5F8B57)
    val Yellow = Color(0xFFE8B94A)
    val Peach = Color(0xFFE28A4F)
    val Danger = Color(0xFFC94A3A)

    val BgArgb = 0xFFFFF7EE.toInt()
    val SurfaceArgb = 0xFFFFFFFF.toInt()
    val RoseArgb = 0xFFC95C54.toInt()
    val PrimaryArgb = 0xFF8B4A24.toInt()
    val SplashBgArgb = 0xFFFFEFE5.toInt()
    val BlueArgb = 0xFF4F8F9D.toInt()
    val VioletArgb = 0xFF8A6E9F.toInt()
    val GreenArgb = 0xFF5F8B57.toInt()
    val YellowArgb = 0xFFE8B94A.toInt()
    val PeachArgb = 0xFFE28A4F.toInt()
}

object ChestnutRadius {
    val Small = 12.dp
    val Control = 16.dp
    val Card = 20.dp
    val Sheet = 28.dp
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
