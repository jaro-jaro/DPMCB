package cz.jaro.dpmcb.ui.loading

import com.fleeksoft.ksoup.Ksoup
import com.fleeksoft.ksoup.network.parseGetRequest
import cz.jaro.dpmcb.data.helperclasses.IO
import cz.jaro.dpmcb.data.recordException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

fun interface AppUpdater {
    fun updateApp(
        loadingDialog: (String?) -> Unit,
    )
}

suspend fun latestAppVersion(): String? = withContext(Dispatchers.IO) {
    val document = try {
        Ksoup.parseGetRequest("https://raw.githubusercontent.com/jaro-jaro/DPMCB/main/app/version.txt")
    } catch (e: Exception) {
        e.printStackTrace()
        recordException(e)
        return@withContext null
    }

    return@withContext document.text()
}