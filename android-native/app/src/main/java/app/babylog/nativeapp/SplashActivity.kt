package app.babylog.nativeapp

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val SPLASH_IP_INITIAL_SCALE = 0.94f
private const val SPLASH_IP_FADE_MS = 460
private const val SPLASH_IP_MOTION_MS = 560
private const val SPLASH_WORD_DELAY_MS = 140L
private const val SPLASH_WORD_MOTION_MS = 420

class SplashActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.AppTheme)
        super.onCreate(savedInstanceState)
        window.statusBarColor = ChestnutPalette.SplashBgArgb
        window.navigationBarColor = ChestnutPalette.SplashBgArgb
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            var systemUiFlags = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                systemUiFlags = systemUiFlags or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
            }
            window.decorView.systemUiVisibility = systemUiFlags
        }

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
    val density = LocalDensity.current
    val ipAlpha = remember { Animatable(0f) }
    val ipScale = remember { Animatable(SPLASH_IP_INITIAL_SCALE) }
    val ipOffset = remember { Animatable(with(density) { 20.dp.toPx() }) }
    val wordAlpha = remember { Animatable(0f) }
    val wordOffset = remember { Animatable(with(density) { 14.dp.toPx() }) }
    LaunchedEffect(Unit) {
        launch { ipAlpha.animateTo(1f, tween(durationMillis = SPLASH_IP_FADE_MS, easing = FastOutSlowInEasing)) }
        launch { ipScale.animateTo(1f, tween(durationMillis = SPLASH_IP_MOTION_MS, easing = FastOutSlowInEasing)) }
        launch { ipOffset.animateTo(0f, tween(durationMillis = SPLASH_IP_MOTION_MS, easing = FastOutSlowInEasing)) }
        delay(SPLASH_WORD_DELAY_MS)
        launch { wordAlpha.animateTo(1f, tween(durationMillis = SPLASH_WORD_MOTION_MS, easing = FastOutSlowInEasing)) }
        launch { wordOffset.animateTo(0f, tween(durationMillis = SPLASH_WORD_MOTION_MS, easing = FastOutSlowInEasing)) }
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ChestnutPalette.SplashBg),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Image(
                painter = painterResource(R.drawable.chestnut_main_ip_cutout),
                contentDescription = null,
                modifier = Modifier
                    .size(270.dp)
                    .graphicsLayer {
                        alpha = ipAlpha.value
                        scaleX = ipScale.value
                        scaleY = ipScale.value
                        translationY = ipOffset.value
                    }
            )
            Spacer(modifier = Modifier.height(18.dp))
            splashWordmarks(wordAlpha = wordAlpha.value, wordOffset = wordOffset.value)
        }
    }
}

@Composable
private fun splashWordmarks(wordAlpha: Float, wordOffset: Float) {
    Image(
        painter = painterResource(R.drawable.wordmark_babylog_tight),
        contentDescription = null,
        modifier = Modifier
            .width(176.dp)
            .height(46.dp)
            .graphicsLayer {
                alpha = wordAlpha
                translationY = wordOffset
            }
    )
    Spacer(modifier = Modifier.height(6.dp))
    Image(
        painter = painterResource(R.drawable.wordmark_lijji),
        contentDescription = null,
        modifier = Modifier
            .width(96.dp)
            .height(52.dp)
            .graphicsLayer {
                alpha = wordAlpha
                translationY = wordOffset
            }
    )
}
