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
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val SPLASH_ROUTE_DELAY_MS = 650L
private const val SPLASH_CENTER_BIAS = 0.45f
private const val SPLASH_ICON_INITIAL_SCALE = 0.96f
private const val SPLASH_ICON_SETTLE_MS = 300
private const val SPLASH_WORD_DELAY_MS = 110L
private const val SPLASH_WORD_FADE_MS = 160
private const val SPLASH_SMALL_SCREEN_SCALE = 0.85f
private const val SPLASH_ICON_TEXT_GAP_RATIO = 0.4f

private val SplashIconSize = 190.dp
private val SplashSmallScreenWidth = 360.dp
private val SplashTopSafeMargin = 48.dp
private val SplashBottomSafeMargin = 80.dp
private val SplashEnglishWordmarkWidth = 176.dp
private val SplashEnglishWordmarkHeight = 46.dp
private val SplashChineseWordmarkWidth = 96.dp
private val SplashChineseWordmarkHeight = 52.dp
private val SplashWordmarkGap = 2.dp

class SplashActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.AppTheme)
        super.onCreate(savedInstanceState)
        window.statusBarColor = ChestnutPalette.SplashGradientTopArgb
        window.navigationBarColor = ChestnutPalette.SplashGradientBottomArgb
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
                    delay(SPLASH_ROUTE_DELAY_MS)
                    startActivity(Intent(this@SplashActivity, ComposeMainActivity::class.java))
                    finish()
                    overridePendingTransition(R.anim.splash_enter_fade_in, R.anim.splash_exit_fade_out)
                }
                BabyLogSplash()
            }
        }
    }
}

@Composable
private fun BabyLogSplash() {
    val iconScale = remember { Animatable(SPLASH_ICON_INITIAL_SCALE) }
    val wordAlpha = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        launch {
            iconScale.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = SPLASH_ICON_SETTLE_MS,
                    easing = FastOutSlowInEasing
                )
            )
        }
        delay(SPLASH_WORD_DELAY_MS)
        launch {
            wordAlpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = SPLASH_WORD_FADE_MS,
                    easing = FastOutSlowInEasing
                )
            )
        }
    }
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        ChestnutPalette.SplashGradientTop,
                        ChestnutPalette.SplashGradientBottom
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        val iconSize = if (maxWidth <= SplashSmallScreenWidth) {
            SplashIconSize * SPLASH_SMALL_SCREEN_SCALE
        } else {
            SplashIconSize
        }
        val iconTextGap = iconSize * SPLASH_ICON_TEXT_GAP_RATIO
        val offsetY = splashContentOffsetY(
            maxHeight = maxHeight,
            iconSize = iconSize,
            iconTextGap = iconTextGap
        )
        splashLogoColumn(
            iconSize = iconSize,
            iconTextGap = iconTextGap,
            iconScale = iconScale.value,
            wordAlpha = wordAlpha.value,
            offsetY = offsetY
        )
    }
}

@Composable
private fun splashLogoColumn(
    iconSize: Dp,
    iconTextGap: Dp,
    iconScale: Float,
    wordAlpha: Float,
    offsetY: Dp
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.offset(y = offsetY)
    ) {
        Image(
            painter = painterResource(R.drawable.chestnut_splash_ip_centered),
            contentDescription = null,
            modifier = Modifier
                .size(iconSize)
                .graphicsLayer {
                    scaleX = iconScale
                    scaleY = iconScale
                }
        )
        Spacer(modifier = Modifier.height(iconTextGap))
        splashWordmarks(wordAlpha = wordAlpha)
    }
}

@Composable
private fun splashContentOffsetY(
    maxHeight: Dp,
    iconSize: Dp,
    iconTextGap: Dp
): Dp {
    val statusTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val textBlockHeight = SplashEnglishWordmarkHeight + SplashWordmarkGap + SplashChineseWordmarkHeight
    val groupHeight = iconSize + iconTextGap + textBlockHeight
    val usableHeight = maxHeight - statusTop - navBottom
    val targetCenterY = statusTop + usableHeight * SPLASH_CENTER_BIAS
    val minCenterY = statusTop + SplashTopSafeMargin + groupHeight / 2f
    val maxCenterY = maxHeight - navBottom - SplashBottomSafeMargin - groupHeight / 2f
    val centerY = if (maxCenterY >= minCenterY) {
        targetCenterY.coerceIn(minCenterY, maxCenterY)
    } else {
        targetCenterY
    }
    return centerY - maxHeight / 2f
}

@Composable
private fun splashWordmarks(wordAlpha: Float) {
    Image(
        painter = painterResource(R.drawable.wordmark_babylog_tight),
        contentDescription = null,
        modifier = Modifier
            .width(SplashEnglishWordmarkWidth)
            .height(SplashEnglishWordmarkHeight)
            .graphicsLayer {
                alpha = wordAlpha
            }
    )
    Spacer(modifier = Modifier.height(SplashWordmarkGap))
    Image(
        painter = painterResource(R.drawable.wordmark_lijji),
        contentDescription = null,
        modifier = Modifier
            .width(SplashChineseWordmarkWidth)
            .height(SplashChineseWordmarkHeight)
            .graphicsLayer {
                alpha = wordAlpha
            }
    )
}
