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
import java.time.LocalDate

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
                params.navigate(SpojDestination(e.id))
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val state = repo.oblibene
        .flatMapLatest { oblibene ->
            oblibene
                .map { castSpoje ->
                    dopravaRepo.spojNaMapePodleId(castSpoje.spojId)
                }
                .combine {
                    it.nullable()
                }
                .onEmpty {
                    emit(null)
                }
                .combine(repo.datum) { it, datum ->
                    it?.zip(oblibene) { spojNaMape, castSpoje ->
                        val spoj = try {
                            repo.spojSeZastavkySpojeNaKterychStavi(castSpoje.spojId, datum)
                        } catch (e: Exception) {
                            Firebase.crashlytics.recordException(e)
                            return@zip null
                        }
                        Quintuple(spojNaMape, spoj.first, spoj.second, repo.spojJedeV(castSpoje.spojId), castSpoje)
                    }
                }
        }
        .combine(repo.datum) { spoje, datum ->
            (spoje ?: emptyList()).filterNotNull().map { (spojNaMape, info, zastavky, jedeV, cast) ->
                if (spojNaMape?.zpozdeniMin != null && datum == LocalDate.now()) KartickaState.Online(
                    spojId = info.spojId,
                    linka = info.linka,
                    zpozdeni = spojNaMape.zpozdeniMin,
                    vychoziZastavka = zastavky[cast.start].nazev,
                    vychoziZastavkaCas = zastavky[cast.start].cas,
                    aktualniZastavka = zastavky.last { it.cas == spojNaMape.pristiZastavka }.nazev,
                    aktualniZastavkaCas = zastavky.last { it.cas == spojNaMape.pristiZastavka }.cas,
                    cilovaZastavka = zastavky[cast.end].nazev,
                    cilovaZastavkaCas = zastavky[cast.end].cas,
                    mistoAktualniZastavky = when {
                        zastavky.indexOfLast { it.cas == spojNaMape.pristiZastavka } < cast.start -> -1
                        zastavky.indexOfLast { it.cas == spojNaMape.pristiZastavka } > cast.end -> 1
                        else -> 0
                    },
                )
                else KartickaState.Offline(
                    spojId = info.spojId,
                    linka = info.linka,
                    vychoziZastavka = zastavky[cast.start].nazev,
                    vychoziZastavkaCas = zastavky[cast.start].cas,
                    cilovaZastavka = zastavky[cast.end].nazev,
                    cilovaZastavkaCas = zastavky[cast.end].cas,
                    dalsiPojede = List(365) { datum.plusDays(it.toLong()) }.firstOrNull { jedeV(it) },
                )
            } to datum
        }
        .map { (spoje, datum) ->

            if (spoje.isEmpty()) return@map OblibeneState.ZadneOblibene

            val dnes = spoje
                .filter { it.dalsiPojede == datum }
                .sortedBy { it.vychoziZastavkaCas }
            val jindy = spoje
                .filter { it.dalsiPojede != datum }
                .sortedWith(compareBy<KartickaState> { it.dalsiPojede }
                    .thenBy { it.vychoziZastavkaCas })
                .filterIsInstance<KartickaState.Offline>() // To by mělo být vždy

            if (dnes.isEmpty()) return@map OblibeneState.JedeJenJindy(jindy, datum)

            if (jindy.isEmpty()) return@map OblibeneState.JedeJenDnes(dnes, datum)

            return@map OblibeneState.JedeFurt(dnes, jindy, datum)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), OblibeneState.NacitaSe)
}