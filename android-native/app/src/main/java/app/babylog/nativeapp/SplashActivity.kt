package app.babylog.nativeapp

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

class SplashActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.AppTheme)
        super.onCreate(savedInstanceState)
        window.statusBarColor = ChestnutPalette.PrimaryArgb
        window.navigationBarColor = ChestnutPalette.PrimaryArgb

        setContent {
            ChestnutTheme {
                LaunchedEffect(Unit) {
                    delay(1100)
                    startActivity(Intent(this@SplashActivity, ComposeMainActivity::class.java))
                    finish()
                    overridePendingTransition(0, 0)
                }
                BabyLogSplash()
            }
        }
    }
}

@Composable
private fun BabyLogSplash() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ChestnutPalette.Primary),
        contentAlignment = Alignment.Center
    ) {
        SplashBackdrop()
        Image(
            painter = painterResource(R.drawable.chestnut_main_logo),
            contentDescription = null,
            modifier = Modifier.size(238.dp)
        )
    }
}

@Composable
private fun SplashBackdrop() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        drawCircle(
            color = Color(0xFFFFD8A8).copy(alpha = 0.20f),
            radius = size.width * 0.48f,
            center = Offset(size.width * 0.18f, size.height * 0.12f)
        )
        drawCircle(
            color = Color.White.copy(alpha = 0.10f),
            radius = size.width * 0.68f,
            center = Offset(size.width * 0.86f, size.height * 0.88f)
        )
        drawRoundRect(
            color = Color.White.copy(alpha = 0.09f),
            topLeft = Offset(-size.width * 0.10f, size.height * 0.76f),
            size = Size(size.width * 1.20f, size.height * 0.15f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(56f, 56f)
        )
        val marks = listOf(
            ConfettiMark(0.12f, 0.18f, 28f, 8f, 18f, Color.White.copy(alpha = 0.30f)),
            ConfettiMark(0.82f, 0.16f, 22f, 7f, -22f, Color(0xFFFFE9C6).copy(alpha = 0.52f)),
            ConfettiMark(0.18f, 0.72f, 20f, 7f, -16f, Color(0xFFFFDAD0).copy(alpha = 0.50f)),
            ConfettiMark(0.78f, 0.76f, 30f, 8f, 20f, Color.White.copy(alpha = 0.26f)),
            ConfettiMark(0.50f, 0.20f, 10f, 10f, 0f, Color(0xFFFFE9C6).copy(alpha = 0.46f)),
            ConfettiMark(0.30f, 0.84f, 11f, 11f, 0f, Color.White.copy(alpha = 0.22f))
        )
        marks.forEach { mark ->
            rotate(mark.rotation, Offset(size.width * mark.x, size.height * mark.y)) {
                drawRoundRect(
                    color = mark.color,
                    topLeft = Offset(
                        x = size.width * mark.x - mark.width,
                        y = size.height * mark.y - mark.height / 2f
                    ),
                    size = Size(mark.width * 2f, mark.height),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(mark.height / 2f, mark.height / 2f)
                )
            }
        }
    }
}

private data class ConfettiMark(
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float,
    val rotation: Float,
    val color: Color
)
