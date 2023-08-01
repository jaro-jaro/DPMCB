package cz.jaro.dpmcb.ui.oblibene

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.crashlytics.ktx.crashlytics
import com.google.firebase.ktx.Firebase
import cz.jaro.dpmcb.data.DopravaRepository
import cz.jaro.dpmcb.data.SpojeRepository
import cz.jaro.dpmcb.data.helperclasses.NavigateFunction
import cz.jaro.dpmcb.data.helperclasses.Quintuple
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.combine
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.nullable
import cz.jaro.dpmcb.ui.destinations.SpojDestination
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEmpty
import kotlinx.coroutines.flow.stateIn
import org.koin.android.annotation.KoinViewModel
import org.koin.core.annotation.InjectedParam
import kotlin.math.roundToInt

@KoinViewModel
class OblibeneViewModel(
    private val repo: SpojeRepository,
    private val dopravaRepo: DopravaRepository,
    @InjectedParam private val params: Parameters,
) : ViewModel() {

    data class Parameters(
        val navigate: NavigateFunction,
    )

    fun onEvent(e: OblibeneEvent) {
        when (e) {
            is OblibeneEvent.VybralSpojDnes -> {
                params.navigate(SpojDestination(e.id))
            }

            is OblibeneEvent.VybralSpojJindy -> {
                repo.upravitDatum(e.dalsiPojede ?: return)
                params.navigate(SpojDestination(e.id))
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val state = repo.oblibene
        .flatMapLatest { oblibene ->
            oblibene
                .map { id ->
                    dopravaRepo.spojPodleId(id)
                }
                .combine {
                    it.nullable()
                }
                .onEmpty {
                    emit(null)
                }
                .combine(repo.datum) { it, datum ->
                    it?.zip(oblibene) { (spojNaMape, detailSpoje), id ->
                        val spoj = try {
                            repo.spojSeZastavkySpojeNaKterychStaviAJedeV(id, datum)
                        } catch (e: Exception) {
                            Firebase.crashlytics.recordException(e)
                            return@zip null
                        }
                        Quintuple(spojNaMape, detailSpoje, spoj.first, spoj.second, spoj.third)
                    }
                }
        }
        .combine(repo.datum) { spoje, datum ->
            (spoje ?: emptyList()).filterNotNull().map { (spojNaMape, detailSpoje, info, zastavky, jedeV) ->
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

            if (spoje.isEmpty()) return@map OblibeneState.ZadneOblibene

            val dnes = spoje.filter { it.dalsiPojede == datum }.sortedBy { it.vychoziZastavkaCas }
            val jindy = spoje.filter { it.dalsiPojede != datum }.sortedWith(compareBy<KartickaState> { it.dalsiPojede }.thenBy { it.vychoziZastavkaCas })

            if (dnes.isEmpty()) return@map OblibeneState.JedeJenJindy(jindy, datum)

            if (jindy.isEmpty()) return@map OblibeneState.JedeJenDnes(dnes, datum)

            return@map OblibeneState.JedeFurt(dnes, jindy, datum)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), OblibeneState.NacitaSe)
}