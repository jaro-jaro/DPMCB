package cz.jaro.dpmcb.ui.jedouci

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cz.jaro.dpmcb.data.DopravaRepository
import cz.jaro.dpmcb.data.SpojeRepository
import cz.jaro.dpmcb.data.helperclasses.NavigateFunction
import cz.jaro.dpmcb.data.helperclasses.Smer
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.combine
import cz.jaro.dpmcb.ui.destinations.SpojDestination
import cz.jaro.dpmcb.ui.jedouci.PraveJedouciViewModel.JedouciSpojADalsiVeci.Companion.jenJedouciSpoje
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onEmpty
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.toList
import org.koin.android.annotation.KoinViewModel
import org.koin.core.annotation.InjectedParam
import java.time.LocalDate
import java.time.LocalTime
import kotlin.math.roundToInt

@KoinViewModel
class PraveJedouciViewModel(
    private val repo: SpojeRepository,
    private val dopravaRepo: DopravaRepository,
    @InjectedParam private val params: Parameters,
) : ViewModel() {

    data class Parameters(
        val filtry: List<Int>,
        val navigate: NavigateFunction,
    )

    private val filtry = MutableStateFlow(params.filtry)
    private val nacitaSe = MutableStateFlow(true)

    fun onEvent(e: PraveJedouciEvent) = when (e) {
        is PraveJedouciEvent.ZmenitFiltr -> {
            if (e.cisloLinky in filtry.value) filtry.value -= (e.cisloLinky) else filtry.value += (e.cisloLinky)
        }

        is PraveJedouciEvent.KliklNaSpoj -> {
            repo.upravitDatum(LocalDate.now())
            params.navigate(SpojDestination(spojId = e.spojId))
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private val seznam = filtry.map {
        it.ifEmpty { null }
    }
        .combine(dopravaRepo.spojeDPMCBNaMape()) { filtry, spojeNaMape ->
            nacitaSe.value = true
            filtry?.let {
                spojeNaMape
                    .filter {
                        it.lineNumber?.minus(325_000) in filtry
                    }
                    .map { spojNaMape ->
                        spojNaMape.id
                    }
            } ?: emptyList()
        }
        .flatMapLatest { idcka ->
            idcka
                .map { id ->
                    dopravaRepo.spojPodleId(id)
                }
                .combine {
                    it.asSequence()
                }
                .onEmpty {
                    emit(emptySequence())
                }
        }
        .map { spoje ->
            spoje
                .mapNotNull { (spojNaMape, detailSpoje) ->
                    detailSpoje?.let { spojNaMape?.to(it) }
                }
                .asFlow()
                .mapNotNull { (spojNaMape, detailSpoje) ->
                    repo.spojSeZastavkamiPodleId(spojNaMape.id, LocalDate.now()).let { (spoj, zastavky) ->
                        JedouciSpojADalsiVeci(
                            spojId = spoj.id,
                            pristiZastavkaNazev = zastavky[detailSpoje.stations.indexOfFirst { !it.passed }].nazev,
                            pristiZastavkaCas = zastavky[detailSpoje.stations.indexOfFirst { !it.passed }].cas!!,
                            zpozdeni = detailSpoje.realneZpozdeni?.roundToInt() ?: spojNaMape.delay,
                            indexNaLince = detailSpoje.stations.indexOfFirst { !it.passed },
                            smer = spoj.smer,
                            cisloLinky = spoj.linka - 325_000,
                            cil = zastavky.last().nazev,
                        )
                    }
                }
                .toList()
                .sortedWith(
                    compareBy<JedouciSpojADalsiVeci> { it.cisloLinky }
                        .thenBy { it.smer }
                        .thenBy { it.cil }
                        .thenBy { it.indexNaLince }
                        .thenByDescending { it.pristiZastavkaCas }
                )
                .groupBy { it.cisloLinky to it.cil }
                .map { (_, seznam) ->
                    JedouciLinkaVeSmeru(
                        cisloLinky = seznam.first().cisloLinky,
                        cilovaZastavka = seznam.first().cil,
                        spoje = seznam.jenJedouciSpoje()
                    )
                }
                .also { nacitaSe.value = false }
        }

    private val cislaLinek = flow {
        emit(repo.cislaLinek(LocalDate.now()))
    }

    val state = combine(cislaLinek, seznam, nacitaSe, repo.maPristupKJihu, filtry) { cislaLinek, seznam, nacitaSeSeznam, jeOnline, filtry ->
        if (!jeOnline) return@combine PraveJedouciState.Offline

        if (cislaLinek.isEmpty()) return@combine PraveJedouciState.ZadneLinky

        if (filtry.isEmpty()) return@combine PraveJedouciState.NeniNicVybrano(cislaLinek)

        if (seznam.isNotEmpty()) return@combine PraveJedouciState.OK(cislaLinek, filtry, seznam)

        if (nacitaSeSeznam) return@combine PraveJedouciState.Nacitani(cislaLinek, filtry)

        return@combine PraveJedouciState.PraveNicNejede(cislaLinek, filtry)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PraveJedouciState.NacitaniLinek)

    data class JedouciSpojADalsiVeci(
        val spojId: String,
        val pristiZastavkaNazev: String,
        val pristiZastavkaCas: LocalTime,
        val zpozdeni: Int,
        val indexNaLince: Int,
        val smer: Smer,
        val cisloLinky: Int,
        val cil: String,
    ) {
        private fun toJedouciSpoj() = JedouciSpoj(
            spojId = spojId,
            pristiZastavkaNazev = pristiZastavkaNazev,
            pristiZastavkaCas = pristiZastavkaCas,
            zpozdeni = zpozdeni
        )

        companion object {
            fun List<JedouciSpojADalsiVeci>.jenJedouciSpoje() = map(JedouciSpojADalsiVeci::toJedouciSpoj)
        }
    }
}
