package cz.jaro.dpmcb.ui.loading

import android.content.Context
import android.content.Intent
import androidx.core.net.toUri
import com.fleeksoft.ksoup.Ksoup
import com.fleeksoft.ksoup.network.parseGetRequest
import com.google.firebase.Firebase
import com.google.firebase.crashlytics.crashlytics
import io.ktor.client.network.sockets.SocketTimeoutException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.core.annotation.Single

@Single
class AppUpdater(
    private val ctx: Context,
) {
    suspend fun updateApp() {
        val doc = try {
            withContext(Dispatchers.IO) {
                Ksoup.parseGetRequest("https://raw.githubusercontent.com/jaro-jaro/DPMCB/main/app/version.txt")
            }
        } catch (e: SocketTimeoutException) {
            Firebase.crashlytics.recordException(e)
            return
        }

        val newestVersion = doc.text()

        ctx.startActivity(Intent().apply {
            action = Intent.ACTION_VIEW
            data = "https://github.com/jaro-jaro/DPMCB/releases/download/v$newestVersion/Lepsi-DPMCB-v$newestVersion.apk".toUri()
        })
    }
}