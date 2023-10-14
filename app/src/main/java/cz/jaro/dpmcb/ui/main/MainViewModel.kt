package cz.jaro.dpmcb.ui.main

import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import androidx.navigation.NavGraph
import androidx.navigation.NavHostController
import com.google.firebase.crashlytics.ktx.crashlytics
import cz.jaro.dpmcb.data.App
import cz.jaro.dpmcb.data.SpojeRepository
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.funguj
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.navigateToRouteFunction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import org.koin.android.annotation.KoinViewModel
import org.koin.core.annotation.InjectedParam
import java.net.SocketTimeoutException
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.time.LocalDate

@KoinViewModel
class MainViewModel(
    repo: SpojeRepository,
    @InjectedParam closeDrawer: () -> Unit,
    @InjectedParam link: String?,
    @InjectedParam navController: NavHostController,
    @InjectedParam loadingActivityIntent: Intent,
    @InjectedParam startActivity: (Intent) -> Unit,
    @InjectedParam chyba: (String) -> Unit,
) : ViewModel() {

    val jeOnline = repo.isOnline
    val onlineMod = repo.onlineMod
    val upravitOnlineMod = repo::upravitOnlineMod
    val datum = repo.datum
    val upravitDatum = { it: LocalDate -> repo.upravitDatum(it, false) }

    private fun encodeLink(link: String) = link.split("?").let { segments ->
        val path = segments[0].split("/").joinToString("/") {
            URLEncoder.encode(it, StandardCharsets.UTF_8.toString())
        }
        val args = segments.getOrNull(1)?.split("&")?.joinToString("&") { argument ->
            argument.split("=").let {
                val name = it[0]
                val value = URLEncoder.encode(it[1], StandardCharsets.UTF_8.toString())
                "$name=$value"
            }
        }?.let { "?$it" } ?: ""
        "$path$args".funguj().replace(Regex("%25([0-9A-F]{2})"), "%$1").funguj()
    }

    private val NavController.graphOrNull: NavGraph?
        get() = try {
            graph
        } catch (e: IllegalStateException) {
            @Suppress("DEPRECATION")
            Firebase.crashlytics.recordException(e)
            null
        }

    init {
        link?.let {
            viewModelScope.launch(Dispatchers.IO) {
                val url = encodeLink(link.removePrefix("/"))
                while (navController.graphOrNull == null) Unit
                try {
                    withContext(Dispatchers.Main) {
                        App.vybrano = null
                        navController.navigateToRouteFunction(url)
                        closeDrawer()
                    }
                } catch (_: IllegalArgumentException) {
                    chyba("Vadná zkratka")
                }
            }
        }
    }

    val aktualizovatData = {
        startActivity(loadingActivityIntent.apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NO_HISTORY
            putExtra("update", true)
        })
    }
    val aktualizovatAplikaci = {
        viewModelScope.launch(Dispatchers.IO) {
            val response = try {
                withContext(Dispatchers.IO) {
                    Jsoup
                        .connect("https://raw.githubusercontent.com/jaro-jaro/DPMCB/main/app/version.txt")
                        .ignoreContentType(true)
                        .maxBodySize(0)
                        .execute()
                }
            } catch (e: SocketTimeoutException) {
                @Suppress("DEPRECATION")
                Firebase.crashlytics.recordException(e)
                return@launch
            }

            if (response.statusCode() != 200) return@launch

            val nejnovejsiVerze = response.body()

            startActivity(Intent().apply {
                action = Intent.ACTION_VIEW
                data = Uri.parse("https://github.com/jaro-jaro/DPMCB/releases/download/v$nejnovejsiVerze/Lepsi-DPMCB-v$nejnovejsiVerze.apk")
            })
        }
        Unit
    }
}