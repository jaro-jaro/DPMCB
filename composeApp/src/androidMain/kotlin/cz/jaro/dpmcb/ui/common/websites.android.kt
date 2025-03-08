package cz.jaro.dpmcb.ui.common

import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import io.ktor.client.HttpClient
import io.ktor.client.plugins.onDownload
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText

actual val openWebsiteLauncher: (url: String) -> Unit
    @Composable
    get() {
        val intent =
            CustomTabsIntent.Builder()
                .setShowTitle(true)
                .build()
        val ctx = LocalContext.current
        return {
            intent.launchUrl(ctx, it.toUri())
        }
    }

private val client = HttpClient()

actual suspend fun fetch(url: String, progress: (Float?) -> Unit) =
    client.get(url) {
        onDownload { bytesSentTotal, contentLength ->
            progress(bytesSentTotal.toFloat() / contentLength)
        }
    }.bodyAsText()