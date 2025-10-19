package cz.jaro.dpmcb.ui.loading

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import cz.jaro.dpmcb.BuildKonfig
import io.github.z4kn4fein.semver.toVersion
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import javax.net.ssl.HttpsURLConnection

class AndroidAppUpdater(
    ctx: ComponentActivity,
) : AppUpdater {
    private val packageManager = ctx.packageManager
    private val filesDir = ctx.filesDir
    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    private val getUri: File.() -> Uri = {
        FileProvider.getUriForFile(ctx, "${ctx.packageName}.provider", this)
    }
    private val startActivity: (Intent) -> Unit = {
        ctx.startActivity(it)
    }
    private lateinit var callback: () -> Unit
    private val launcher: ActivityResultLauncher<Intent> = ctx.registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { callback() }

    private val intentToLaunch = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
        data = "package:${ctx.packageName}".toUri()
    }

    override fun updateApp(
        loadingDialog: (String?) -> Unit,
    ) {
        callback = { updateApp(loadingDialog) }
        if (!packageManager.canRequestPackageInstalls()) {
            launcher.launch(intentToLaunch)
            return
        }
        loadingDialog("Hledání nové verze…")

        val apkDir = File(filesDir, "apk").apply {
            if (!exists()) mkdir()
        }

        coroutineScope.launch(Dispatchers.Main) {
            val localVersion = BuildKonfig.versionName.toVersion(false)
            val latestVersion = latestAppVersion()
            val latestPreReleaseVersion =
                if (latestVersion != null && localVersion < latestVersion) latestVersion
                else latestAppPreReleaseVersion(localVersion) ?: return@launch

            val apkUrl =
                "https://github.com/jaro-jaro/DPMCB/releases/download/v$latestPreReleaseVersion/Lepsi-DPMCB-v$latestPreReleaseVersion.apk"

            val file = File(apkDir, "$latestPreReleaseVersion.apk")
            file.createNewFile()

            val connection = URL(apkUrl).openConnection() as HttpsURLConnection
            loadingDialog("Stahování…")
            connection.use { input ->
                withContext(Dispatchers.IO) {
                    file.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                loadingDialog("Instalace…")

                Intent(Intent.ACTION_VIEW).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
                    setDataAndType(file.getUri(), "application/vnd.android.package-archive")
                }.let(startActivity)
                loadingDialog(null)
            }
        }
    }
}

suspend fun HttpURLConnection.use(block: suspend (BufferedInputStream) -> Unit) =
    withContext(Dispatchers.IO) {
        try {
            connect()
            inputStream.use {
                withContext(Dispatchers.Main) {
                    block(it.let(::BufferedInputStream))
                }
            }
        } finally {
            disconnect()
        }
    }

class MyFileProvider : FileProvider()