package cz.jaro.dpmcb.ui.oblibene

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cz.jaro.datum_cas.Datum
import cz.jaro.datum_cas.dni
import cz.jaro.dpmcb.data.App.Companion.dopravaRepo
import cz.jaro.dpmcb.data.App.Companion.repo
import cz.jaro.dpmcb.data.helperclasses.Quintuple
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.combine
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEmpty
import kotlinx.coroutines.flow.stateIn

class OblibeneViewModel : ViewModel() {

    @OptIn(ExperimentalCoroutinesApi::class)
    val state = repo.oblibene
        .flatMapLatest { oblibene ->
            oblibene
                .map { id ->
                    println(id)
                    dopravaRepo.spojPodleId(id)
                }
                .combine {
                    it.zip(oblibene) { (spojNaMape, detailSpoje), id ->
                        val spoj = try {
                            repo.spojSeZastavkySpojeNaKterychStaviAJedeV(id)
                        } catch (e: Exception) {
                            return@zip null
                        }
                        Quintuple(spojNaMape, detailSpoje, spoj.first, spoj.second, spoj.third)
                    }
                }
                .onEmpty {
                    emit(emptyList())
                }
        }
        .map { spoje ->
            spoje.filterNotNull().map { (spojNaMape, detailSpoje, info, zastavky, jedeV) ->
                KartickaState(
                    spojId = info.spojId,
                    linka = info.linka,
                    zpozdeni = spojNaMape?.delay,
                    vychoziZastavka = zastavky.first().nazev,
                    vychoziZastavkaCas = zastavky.first().cas,
                    aktualniZastavka = detailSpoje?.stations?.indexOfFirst { !it.passed }?.let { i -> zastavky[i].nazev },
                    aktualniZastavkaCas = detailSpoje?.stations?.indexOfFirst { !it.passed }?.let { i -> zastavky[i].cas },
                    cilovaZastavka = zastavky.last().nazev,
                    cilovaZastavkaCas = zastavky.last().cas,
                    dalsiPojede = List(365) { Datum.dnes + it.dni }.firstOrNull { jedeV(it) }
                )
            }
        }
        .map { spoje ->
            OblibeneState(
                nacitaSe = false,
                nejake = spoje.any(),
                dnes = spoje.filter { it.dalsiPojede == Datum.dnes }.sortedBy { it.vychoziZastavkaCas },
                jindy = spoje.filter { it.dalsiPojede != Datum.dnes }.sortedWith(compareBy<KartickaState> { it.dalsiPojede }.thenBy { it.vychoziZastavkaCas }),
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), OblibeneState(nacitaSe = true, nejake = false, dnes = emptyList(), jindy = emptyList()))
}