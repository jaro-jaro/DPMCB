package cz.jaro.dpmcb.ui.odjezdy

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ramcosta.composedestinations.spec.Direction
import cz.jaro.dpmcb.data.App.Companion.dopravaRepo
import cz.jaro.dpmcb.data.App.Companion.repo
import cz.jaro.dpmcb.data.helperclasses.Cas
import cz.jaro.dpmcb.data.helperclasses.Trvani
import cz.jaro.dpmcb.data.helperclasses.Trvani.Companion.min
import cz.jaro.dpmcb.data.helperclasses.TypAdapteru
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.ifTake
import cz.jaro.dpmcb.data.naJihu.SpojNaMape
import cz.jaro.dpmcb.ui.vybirator.Vysledek
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class OdjezdyViewModel(
    val zastavka: String,
    cas: Cas = Cas.ted,
) : ViewModel() {

    lateinit var scrollovat: suspend (Int) -> Unit
    lateinit var navigovat: (Direction) -> Unit

    private val _state = MutableStateFlow(OdjezdyState(cas = cas))
    val state = _state.asStateFlow()

    private val lejzove = mutableMapOf<String, Lazy<SpojNaMape?>>()

    init {
        viewModelScope.launch(Dispatchers.IO) {
            repo.datum.combine(dopravaRepo.seznamSpojuKterePraveJedou()) { datum, spojeNaMape ->
                repo.spojeJedouciVdatumZastavujiciNaIndexechZastavkySeZastavkySpoje(datum, zastavka)
                    .map {
                        val spojNaMape = lejzove.getOrPut(it.spojId) {
                            lazy { with(dopravaRepo) { spojeNaMape.spojDPMCBPodleId(it.spojId) } }
                        }
                        it to spojNaMape
                    }
                    .sortedBy { (zast, spojNaMape) ->
                        zast.cas + (ifTake(spojNaMape.isInitialized()) { spojNaMape.value?.delay?.min } ?: 0.min)
                    }
                    .map { (zastavka, spojNaMape) ->

                        val posledniZastavka = zastavka.zastavkySpoje.last { it.second != Cas.nikdy }
                        val aktualniNasledujiciZastavka = lazy {
                            spojNaMape.value?.delay?.let { zpozdeni ->
                                zastavka.zastavkySpoje.find { (it.second + zpozdeni.min) > Cas.ted }
                            }
                        }

                        KartickaState(
                            konecna = posledniZastavka.first,
                            cisloLinky = zastavka.linka,
                            cas = zastavka.cas,
                            aktualniNasledujiciZastavka = aktualniNasledujiciZastavka,
                            idSpoje = zastavka.spojId,
                            nizkopodlaznost = zastavka.nizkopodlaznost,
                            zpozdeni = lazy { spojNaMape.value?.delay },
                            jedePres = zastavka.zastavkySpoje.map { it.first },
                            jedeZa = lazy { spojNaMape.value?.delay?.min?.let { zastavka.cas + it - Cas.ted } }
                        )
                    }
            }
                .flowOn(Dispatchers.IO)
                .collect { seznam ->
                    _state.update {
                        it.copy(
                            seznam = seznam,
                            nacitaSe = false,
                        )
                    }
                }
        }
    }

    fun kliklNaDetailSpoje(spoj: KartickaState) {
        navigovat(
            cz.jaro.dpmcb.ui.destinations.DetailSpojeScreenDestination(
                spoj.idSpoje
            )
        )
    }

    fun zmenitCas(cas: Cas) {
        viewModelScope.launch(Dispatchers.Main) {
            _state.update { oldState ->
                oldState.copy(
                    cas = cas,
                ).also { newState ->
                    if (newState.filtrovanejSeznam.isEmpty()) return@also
                    scrollovat(newState.filtrovanejSeznam.indexOfFirst { zast ->
                        zast.cas >= cas
                    } + ((Int.MAX_VALUE / 2) / newState.filtrovanejSeznam.size) * newState.filtrovanejSeznam.size)
                }
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
                }.also { newState ->
                    if (newState.filtrovanejSeznam.isEmpty()) return@also
                    scrollovat(
                        newState.filtrovanejSeznam.indexOfFirst { zast ->
                            zast.cas >= newState.cas
                        } + ((Int.MAX_VALUE / 2) / newState.filtrovanejSeznam.size) * newState.filtrovanejSeznam.size
                    )
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
                }.also { newState ->
                    if (newState.filtrovanejSeznam.isEmpty()) return@also
                    launch(Dispatchers.Main) {
                        scrollovat(
                            newState.filtrovanejSeznam.indexOfFirst { zast ->
                                zast.cas >= newState.cas
                            } + ((Int.MAX_VALUE / 2) / newState.filtrovanejSeznam.size) * newState.filtrovanejSeznam.size
                        )
                    }
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
        val aktualniNasledujiciZastavka: Lazy<Pair<String, Cas>?>,
        val cisloLinky: Int,
        val cas: Cas,
        val idSpoje: String,
        val nizkopodlaznost: Boolean,
        val zpozdeni: Lazy<Int?>,
        val jedePres: List<String>,
        val jedeZa: Lazy<Trvani?>,
    )

    data class OdjezdyState(
        val seznam: List<KartickaState> = emptyList(),
        val nacitaSe: Boolean = true,
        val cas: Cas,
        val indexScrollovani: Int = Int.MAX_VALUE / 2,
        val filtrLinky: Int? = null,
        val filtrZastavky: String? = null,
        val kompaktniRezim: Boolean = false,
    ) {
        val filtrovanejSeznam
            get() = seznam
                .filter {
                    filtrLinky?.let { filtr -> it.cisloLinky == filtr } ?: true
                }
                .filter {
                    filtrZastavky?.let { filtr -> it.jedePres.contains(filtr) } ?: true
                }
    }

    init {
        viewModelScope.launch(Dispatchers.IO) {
            while (state.value.seznam.isEmpty()) Unit
            while (!::scrollovat.isInitialized) Unit
            withContext(Dispatchers.Main) {
                scrollovat(state.value.seznam.indexOfFirst { zast ->
                    zast.cas >= cas
                } + ((Int.MAX_VALUE / 2) / state.value.seznam.size) * state.value.seznam.size)
            }
        }
    }
}
