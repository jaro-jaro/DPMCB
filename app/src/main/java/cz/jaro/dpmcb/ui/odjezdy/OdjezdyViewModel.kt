package cz.jaro.dpmcb.ui.odjezdy

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ramcosta.composedestinations.spec.Direction
import cz.jaro.dpmcb.data.App.Companion.dopravaRepo
import cz.jaro.dpmcb.data.App.Companion.repo
import cz.jaro.dpmcb.data.helperclasses.TypAdapteru
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.plus
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.ted
import cz.jaro.dpmcb.data.helperclasses.Vysledek
import cz.jaro.dpmcb.ui.destinations.DetailSpojeDestination
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.runningFold
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Duration
import java.time.LocalTime
import kotlin.time.Duration.Companion.minutes

class OdjezdyViewModel(
    val zastavka: String,
    cas: LocalTime = ted,
) : ViewModel() {

    lateinit var scrollovat: suspend (Int) -> Unit
    lateinit var navigovat: (Direction) -> Unit

    private val _state = MutableStateFlow(OdjezdyState(cas = cas))
    val state = _state.asStateFlow()

    val seznam = repo.datum
        .combine(dopravaRepo.seznamSpojuKterePraveJedou()) { datum, spojeNaMape ->
            println(ted.toNanoOfDay())
            repo.spojeJedouciVdatumZastavujiciNaIndexechZastavkySeZastavkySpoje(datum, zastavka)
                .map {
                    val spojNaMape = with(dopravaRepo) { spojeNaMape.spojDPMCBPodleId(it.spojId) }
                    it to spojNaMape
                }
                .sortedBy { (zast, spojNaMape) ->
                    zast.cas.plusMinutes(spojNaMape?.delay?.toLong() ?: 0L)
                }
                .map { (zastavka, spojNaMape) ->

                    val posledniZastavka = zastavka.zastavkySpoje.last { it.second != null }
                    val aktualniNasledujiciZastavka = spojNaMape?.delay?.let { zpozdeni ->
                        zastavka.zastavkySpoje
                            .filter { it.second != null }
                            .find { it.second!!.plusMinutes(zpozdeni.toLong()) > ted }
                            ?.let { it.first to it.second!! }
                    }

                    KartickaState(
                        konecna = posledniZastavka.first,
                        cisloLinky = zastavka.linka,
                        cas = zastavka.cas,
                        aktualniNasledujiciZastavka = aktualniNasledujiciZastavka,
                        idSpoje = zastavka.spojId,
                        nizkopodlaznost = zastavka.nizkopodlaznost,
                        zpozdeni = spojNaMape?.delay,
                        jedePres = zastavka.zastavkySpoje.map { it.first },
                        jedeZa = spojNaMape?.delay?.let { Duration.between(ted, zastavka.cas + it.minutes) },
                    )
                }.also {
                    println(ted.toNanoOfDay() to 2)
                }
        }
        .flowOn(Dispatchers.IO)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val filtrovanejSeznam = _state
        .runningFold(null as Pair<OdjezdyState?, OdjezdyState>?) { minuly, novy ->
            minuly?.second to novy
        }
        .filterNotNull()
        .combine(seznam) { (minulyState, state), seznam ->
            seznam
                ?.filter {
                    state.filtrLinky?.let { filtr -> it.cisloLinky == filtr } ?: true
                }
                ?.filter {
                    state.filtrZastavky?.let { filtr -> it.jedePres.contains(filtr) } ?: true
                }
                ?.also { filtrovanejSeznam ->
                    if (minulyState == null) return@also
                    if (minulyState.cas == state.cas && minulyState.filtrZastavky == state.filtrZastavky && minulyState.filtrLinky == state.filtrLinky) return@also
                    if (filtrovanejSeznam.isEmpty()) return@also
                    viewModelScope.launch(Dispatchers.Main) {
                        scrollovat(
                            filtrovanejSeznam.withIndex().firstOrNull { (_, zast) ->
                                zast.cas >= state.cas
                            }?.index ?: filtrovanejSeznam.lastIndex
                        )
                    }
                }
        }
        .flowOn(Dispatchers.IO)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun kliklNaDetailSpoje(spoj: KartickaState) {
        navigovat(
            DetailSpojeDestination(
                spoj.idSpoje
            )
        )
    }

    fun zmenitCas(cas: LocalTime) {
        viewModelScope.launch(Dispatchers.Main) {
            _state.update { oldState ->
                oldState.copy(
                    cas = cas,
                )
            }
        }
    }

    fun scrolluje(i: Int) {
        viewModelScope.launch(Dispatchers.Main) {
            _state.update {
                it.copy(
                    indexScrollovani = i
                )
            }
        }
    }

    fun vybral(vysledek: Vysledek) {
        viewModelScope.launch(Dispatchers.Main) {
            _state.update { oldState ->
                when (vysledek.typAdapteru) {
                    TypAdapteru.LINKA_ZPET -> oldState.copy(filtrLinky = vysledek.value.toInt())
                    TypAdapteru.ZASTAVKA_ZPET -> oldState.copy(filtrZastavky = vysledek.value)
                    else -> return@launch
                }
            }
        }
    }

    fun zrusil(typAdapteru: TypAdapteru) {
        viewModelScope.launch(Dispatchers.IO) {
            _state.update { oldState ->
                when (typAdapteru) {
                    TypAdapteru.LINKA_ZPET -> oldState.copy(filtrLinky = null)
                    TypAdapteru.ZASTAVKA_ZPET -> oldState.copy(filtrZastavky = null)
                    else -> return@launch
                }
            }
        }
    }

    fun zmenilKompaktniRezim() {
        _state.update {
            it.copy(
                kompaktniRezim = !it.kompaktniRezim
            )
        }
    }

    init {
        viewModelScope.launch(Dispatchers.IO) {
            while (seznam.value.isNullOrEmpty()) Unit
            while (!::scrollovat.isInitialized) Unit
            withContext(Dispatchers.Main) {
                scrollovat(
                    seznam.value!!.withIndex().firstOrNull { (_, zast) ->
                        zast.cas >= cas
                    }?.index ?: seznam.value!!.lastIndex
                )
            }
        }
    }
}
