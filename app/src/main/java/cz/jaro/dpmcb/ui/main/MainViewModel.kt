package cz.jaro.dpmcb.ui.main

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
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

class MainViewModel(
    repo: SpojeRepository,
    closeDrawer: () -> Unit,
    link: String?,
    navController: NavHostController,
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
}