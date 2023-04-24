package cz.jaro.dpmcb.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cz.jaro.dpmcb.data.App
import cz.jaro.dpmcb.data.SpojeRepository
import cz.jaro.dpmcb.data.helperclasses.NavigateFunction
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.plus
import cz.jaro.dpmcb.ui.destinations.DetailSpojeDestination
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.days

class MainViewModel(
    repo: SpojeRepository,
    closeDrawer: () -> Unit,
    link: String?,
    navigate: NavigateFunction,
) : ViewModel() {

    val jeOnline = repo.isOnline
    val onlineMod = repo.onlineMod
    val upravitOnlineMod = repo::upravitOnlineMod
    val datum = repo.datum
    val upravitDatum = repo::upravitDatum

    init {
        link?.let {
            if (!link.startsWith("/spoj")) return@let
            val id = link.split("/").last()

            viewModelScope.launch {
                val jedeV = repo.spojJedeV(id)

                val datum = repo.datum.value
                repo.upravitDatum(List(365) { datum + it.days }.firstOrNull { jedeV(it) } ?: return@launch)

                navigate(DetailSpojeDestination(id))

                App.vybrano = null
                closeDrawer()
            }
        }
    }
}