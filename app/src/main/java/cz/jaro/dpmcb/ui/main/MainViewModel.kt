package cz.jaro.dpmcb.ui.main

import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import androidx.navigation.NavGraph
import androidx.navigation.NavHostController
import cz.jaro.dpmcb.data.App
import cz.jaro.dpmcb.data.SpojeRepository
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.funguj
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.navigateToRouteFunction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import java.net.SocketTimeoutException
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

class MainViewModel(
    repo: SpojeRepository,
    closeDrawer: () -> Unit,
    link: String?,
    navController: NavHostController,
    loadingActivityIntent: Intent,
    startActivity: (Intent) -> Unit,
) : ViewModel() {

    val jeOnline = repo.isOnline
    val onlineMod = repo.onlineMod
    val upravitOnlineMod = repo::upravitOnlineMod
    val datum = repo.datum
    val upravitDatum = repo::upravitDatum

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
            null
        }

    init {
        link?.let {
            viewModelScope.launch(Dispatchers.IO) {
                val url = encodeLink(link.removePrefix("/"))
                while (navController.graphOrNull == null) Unit
                withContext(Dispatchers.Main) {
                    App.vybrano = null
                    navController.navigateToRouteFunction(url)
                    closeDrawer()
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
                return@launch
            }

            if (response.statusCode() != 200) return@launch

            val nejnovejsiVerze = response.body()

            startActivity(Intent().apply {
                action = Intent.ACTION_VIEW
                data = Uri.parse("https://github.com/jaro-jaro/DPMCB/releases/download/v$nejnovejsiVerze/DPMCB-v$nejnovejsiVerze.apk")
            })
        }
        Unit
    }
}