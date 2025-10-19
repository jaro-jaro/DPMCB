package cz.jaro.dpmcb.ui.loading

import com.fleeksoft.ksoup.Ksoup
import com.fleeksoft.ksoup.network.parseGetRequest
import cz.jaro.dpmcb.data.helperclasses.IO
import cz.jaro.dpmcb.data.recordException
import io.github.z4kn4fein.semver.Version
import io.github.z4kn4fein.semver.toVersionOrNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

fun interface AppUpdater {
    fun updateApp(
        loadingDialog: (String?) -> Unit,
    )
}

suspend fun latestAppVersion(): Version? = withContext(Dispatchers.IO) {
    val document = try {
        Ksoup.parseGetRequest("https://raw.githubusercontent.com/jaro-jaro/DPMCB/main/composeApp/version.txt")
    } catch (e: Exception) {
        e.printStackTrace()
        recordException(e)
        return@withContext null
    }

    return@withContext document.text().toVersionOrNull(false)
}

suspend fun latestAppPreReleaseVersion(currentVersion: Version): Version? = withContext(Dispatchers.IO) {
    val mmp = currentVersion.copy(preRelease = null, buildMetadata = null).toString()
    val document = try {
        Ksoup.parseGetRequest("https://raw.githubusercontent.com/jaro-jaro/DPMCB/refs/heads/release/v$mmp/composeApp/version.txt")
    } catch (e: Exception) {
        e.printStackTrace()
        recordException(e)
        return@withContext null
    }

    return@withContext document.text().toVersionOrNull(false)
}