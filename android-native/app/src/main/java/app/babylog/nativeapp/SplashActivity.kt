package app.babylog.nativeapp

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.height
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

class SplashActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.AppTheme)
        super.onCreate(savedInstanceState)
        window.statusBarColor = ChestnutPalette.SplashBgArgb
        window.navigationBarColor = ChestnutPalette.SplashBgArgb

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
            .background(ChestnutPalette.SplashBg),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Image(
                painter = painterResource(R.drawable.chestnut_main_logo),
                contentDescription = null,
                modifier = Modifier.size(270.dp)
            )
            Spacer(modifier = Modifier.height(22.dp))
            Text(
                text = "BabyLog",
                color = ChestnutPalette.Ink,
                fontSize = 22.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(5.dp))
            Text(
                text = "栗记",
                color = ChestnutPalette.Muted,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
