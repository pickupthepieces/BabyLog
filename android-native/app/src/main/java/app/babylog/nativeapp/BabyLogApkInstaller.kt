package app.babylog.nativeapp

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import java.io.File
import java.io.IOException

internal object BabyLogApkInstaller {
    fun install(context: Context, apk: File, showInfo: (String, String) -> Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !context.packageManager.canRequestPackageInstalls()) {
            showInfo("需要允许安装更新", "请在系统设置里允许栗记安装未知应用，然后回到 App 再次检查更新。")
            context.startActivity(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${context.packageName}")))
            return
        }
        try {
            val uri = BabyLogFileProvider.getUriForFile(context, apk)
            context.startActivity(Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            })
        } catch (error: IOException) {
            showInfo("无法安装更新", error.message ?: "更新包不可读取")
        } catch (error: ActivityNotFoundException) {
            showInfo("无法安装更新", error.message ?: "系统没有可用的 APK 安装器")
        }
    }
}
