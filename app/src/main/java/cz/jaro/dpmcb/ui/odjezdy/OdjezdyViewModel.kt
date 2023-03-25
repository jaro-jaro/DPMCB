package cz.jaro.dpmcb.ui.odjezdy

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ramcosta.composedestinations.spec.Direction
import cz.jaro.datum_cas.Cas
import cz.jaro.datum_cas.Trvani
import cz.jaro.datum_cas.min
import cz.jaro.dpmcb.data.App.Companion.dopravaRepo
import cz.jaro.dpmcb.data.App.Companion.repo
import cz.jaro.dpmcb.data.helperclasses.TypAdapteru
import cz.jaro.dpmcb.ui.destinations.DetailSpojeScreenDestination
import cz.jaro.dpmcb.ui.vybirator.Vysledek
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
import java.time.LocalTime

class OdjezdyViewModel(
    val zastavka: String,
    cas: Cas = Cas.ted,
) : ViewModel() {

    lateinit var scrollovat: suspend (Int) -> Unit
    lateinit var navigovat: (Direction) -> Unit

    private val _state = MutableStateFlow(OdjezdyState(cas = cas))
    val state = _state.asStateFlow()

    val seznam = repo.datum
        .combine(dopravaRepo.seznamSpojuKterePraveJedou()) { datum, spojeNaMape ->
            println(LocalTime.now().toNanoOfDay())
            repo.spojeJedouciVdatumZastavujiciNaIndexechZastavkySeZastavkySpoje(datum, zastavka)
                .map {
                    val spojNaMape = with(dopravaRepo) { spojeNaMape.spojDPMCBPodleId(it.spojId) }
                    it to spojNaMape
                }
                .sortedBy { (zast, spojNaMape) ->
                    zast.cas + (spojNaMape?.delay?.min ?: 0.min)
                }
                .map { (zastavka, spojNaMape) ->

                    val posledniZastavka = zastavka.zastavkySpoje.last { it.second != Cas.nikdy }
                    val aktualniNasledujiciZastavka = spojNaMape?.delay?.let { zpozdeni ->
                        zastavka.zastavkySpoje.find { (it.second + zpozdeni.min) > Cas.ted }
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
                        jedeZa = spojNaMape?.delay?.min?.let { zastavka.cas + it - Cas.ted },
                    )
                }.also {
                    println(LocalTime.now().toNanoOfDay() to 2)
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
                            filtrovanejSeznam.indexOfFirst { zast ->
                                zast.cas >= state.cas
                            } + ((Int.MAX_VALUE / 2) / filtrovanejSeznam.size) * filtrovanejSeznam.size
                        )
                    }
                }
        }
        .flowOn(Dispatchers.IO)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun kliklNaDetailSpoje(spoj: KartickaState) {
        navigovat(
            DetailSpojeScreenDestination(
                spoj.idSpoje
            )
        )
    }

    fun zmenitCas(cas: Cas) {
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

    data class KartickaState(
        val konecna: String,
        val aktualniNasledujiciZastavka: Pair<String, Cas>?,
        val cisloLinky: Int,
        val cas: Cas,
        val idSpoje: String,
        val nizkopodlaznost: Boolean,
        val zpozdeni: Int?,
        val jedePres: List<String>,
        val jedeZa: Trvani?,
    )

    data class OdjezdyState(
        val cas: Cas,
        val indexScrollovani: Int = Int.MAX_VALUE / 2,
        val filtrLinky: Int? = null,
        val filtrZastavky: String? = null,
        val kompaktniRezim: Boolean = false,
    )

    init {
        viewModelScope.launch(Dispatchers.IO) {
            while (seznam.value.isNullOrEmpty()) Unit
            while (!::scrollovat.isInitialized) Unit
            withContext(Dispatchers.Main) {
                scrollovat(
                    seznam.value!!.indexOfFirst { zast ->
                        zast.cas >= cas
                    } + ((Int.MAX_VALUE / 2) / seznam.value!!.size) * seznam.value!!.size
                )
            }
        }
    }
}
