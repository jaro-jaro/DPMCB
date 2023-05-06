package cz.jaro.dpmcb.ui.oblibene

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cz.jaro.dpmcb.data.DopravaRepository
import cz.jaro.dpmcb.data.SpojeRepository
import cz.jaro.dpmcb.data.helperclasses.Quintuple
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.combine
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEmpty
import kotlinx.coroutines.flow.stateIn
import kotlin.math.roundToInt

class OblibeneViewModel(
    private val repo: SpojeRepository,
    private val dopravaRepo: DopravaRepository,
) : ViewModel() {

    val datum = repo.datum
    val upravitDatum = repo::upravitDatum

    @OptIn(ExperimentalCoroutinesApi::class)
    val state = repo.oblibene
        .flatMapLatest { oblibene ->
            oblibene
                .map { id ->
                    dopravaRepo.spojPodleId(id)
                }
                .combine {
                    it
                }
                .combine(repo.datum) { it, datum ->
                    it.zip(oblibene) { (spojNaMape, detailSpoje), id ->
                        val spoj = try {
                            repo.spojSeZastavkySpojeNaKterychStaviAJedeV(id, datum)
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
        .combine(repo.datum) { spoje, datum ->
            spoje.filterNotNull().map { (spojNaMape, detailSpoje, info, zastavky, jedeV) ->
                KartickaState(
                    spojId = info.spojId,
                    linka = info.linka,
                    zpozdeni = detailSpoje?.realneZpozdeni?.roundToInt() ?: spojNaMape?.delay,
                    vychoziZastavka = zastavky.first().nazev,
                    vychoziZastavkaCas = zastavky.first().cas,
                    aktualniZastavka = detailSpoje?.stations?.indexOfFirst { !it.passed }?.let { i -> zastavky[i].nazev },
                    aktualniZastavkaCas = detailSpoje?.stations?.indexOfFirst { !it.passed }?.let { i -> zastavky[i].cas },
                    cilovaZastavka = zastavky.last().nazev,
                    cilovaZastavkaCas = zastavky.last().cas,
                    dalsiPojede = List(365) { datum.plusDays(it.toLong()) }.firstOrNull { jedeV(it) }
                )
            } to datum
        }
        .map { (spoje, datum) ->
            OblibeneState(
                nacitaSe = false,
                nejake = spoje.any(),
                dnes = spoje.filter { it.dalsiPojede == datum }.sortedBy { it.vychoziZastavkaCas },
                jindy = spoje.filter { it.dalsiPojede != datum }.sortedWith(compareBy<KartickaState> { it.dalsiPojede }.thenBy { it.vychoziZastavkaCas }),
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), OblibeneState(nacitaSe = true, nejake = false, dnes = emptyList(), jindy = emptyList()))
}