package cz.jaro.dpmcb.ui.odjezdy

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ramcosta.composedestinations.spec.Direction
import cz.jaro.dpmcb.data.App.Companion.dopravaRepo
import cz.jaro.dpmcb.data.App.Companion.repo
import cz.jaro.dpmcb.data.entities.ZastavkaSpoje
import cz.jaro.dpmcb.data.helperclasses.Cas
import cz.jaro.dpmcb.data.helperclasses.Quadruple
import cz.jaro.dpmcb.data.helperclasses.Smer
import cz.jaro.dpmcb.data.helperclasses.Trvani.Companion.min
import cz.jaro.dpmcb.data.helperclasses.TypAdapteru
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.ifTake
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.pristiZastavka
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.reversedIf
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.vsechnyIndexy
import cz.jaro.dpmcb.data.naJihu.SpojNaMape
import cz.jaro.dpmcb.ui.destinations.DetailSpojeScreenDestination
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

    private val lejzove = mutableMapOf<Long, Lazy<SpojNaMape?>>()

    init {
        viewModelScope.launch(Dispatchers.IO) {
            repo.typDne.combine(dopravaRepo.seznamSpojuKterePraveJedou()) { typDne, spojeNaMape ->
                repo
                    .spojeJedouciVTypDneZastavujiciNaZastavceSeZastavkySpoje(typDne, zastavka)
                    .flatMap { (spoj, zastavkySpoje) ->
                        val spojNaMape = lejzove.getOrPut(spoj.id) {
                            lazy { with(dopravaRepo) { spojeNaMape.spojNaMapePodleSpojeNeboUlozenehoId(spoj, zastavkySpoje) } }
                        }
                        zastavkySpoje.vsechnyIndexy(zastavka).map { index ->
                            Quadruple(spoj, zastavkySpoje[index], spojNaMape, zastavkySpoje)
                        }
                    }
                    .sortedBy { (_, zast, spojNaMape, _) ->
                        zast.cas + (ifTake(spojNaMape.isInitialized()) { spojNaMape.value?.delay?.min } ?: 0.min)
                    }
                    .map { (spoj, zastavka, spojNaMape, zastavkySpoje) ->

                        val index = zastavkySpoje.indexOf(zastavka)
                        val posledniZastavka = zastavkySpoje.reversedIf { spoj.smer == Smer.NEGATIVNI }.last { it.cas != Cas.nikdy }
                        val pristiZastavkaSpoje = zastavkySpoje.pristiZastavka(spoj.smer, index) ?: posledniZastavka
                        val aktualniNasledujiciZastavka = lazy {
                            spojNaMape.value?.delay?.let { zpozdeni ->
                                zastavkySpoje.reversedIf { spoj.smer == Smer.NEGATIVNI }.find { (it.cas + zpozdeni.min) > Cas.ted }
                            }
                        }

                        KartickaState(
                            konecna = posledniZastavka.nazevZastavky,
                            cisloLinky = spoj.cisloLinky,
                            cas = zastavka.cas,
                            jePosledniZastavka = zastavkySpoje.indexOf(posledniZastavka) == index,
                            pristiZastavka = pristiZastavkaSpoje.nazevZastavky,
                            aktualniNasledujiciZastavka = aktualniNasledujiciZastavka,
                            idSpoje = spoj.id,
                            nizkopodlaznost = spoj.nizkopodlaznost,
                            zpozdeni = lazy { spojNaMape.value?.delay },
                            jedePres = zastavkySpoje.map { it.nazevZastavky }
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

    data class KartickaState(
        val konecna: String,
        val pristiZastavka: String,
        val aktualniNasledujiciZastavka: Lazy<ZastavkaSpoje?>,
        val cisloLinky: Int,
        val cas: Cas,
        val jePosledniZastavka: Boolean,
        val idSpoje: Long,
        val nizkopodlaznost: Boolean,
        val zpozdeni: Lazy<Int?>,
        val jedePres: List<String>,
    )

    data class OdjezdyState(
        val seznam: List<KartickaState> = emptyList(),
        val nacitaSe: Boolean = true,
        val cas: Cas,
        val indexScrollovani: Int = Int.MAX_VALUE / 2,
        val filtrLinky: Int? = null,
        val filtrZastavky: String? = null,
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
