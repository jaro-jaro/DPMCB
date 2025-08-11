package cz.jaro.dpmcb.ui.common

import androidx.compose.runtime.Composable
import kotlinx.browser.window
import kotlinx.coroutines.await
import org.w3c.fetch.RequestInit

actual val openWebsiteLauncher: (url: String) -> Unit
    @Composable get() = { window.open(it, "_blank"); }

actual suspend fun fetch(url: String, progress: (Float?) -> Unit): String {
    progress(null)
    val response = window.fetch(url, RequestInit(
        method = "get",
    ))
    return response.await().text().await()
}