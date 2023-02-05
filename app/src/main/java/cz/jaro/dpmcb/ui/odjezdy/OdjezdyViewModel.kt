package cz.jaro.dpmcb.ui.odjezdy

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cz.jaro.dpmcb.data.App.Companion.dopravaRepo
import cz.jaro.dpmcb.data.App.Companion.repo
import cz.jaro.dpmcb.data.entities.ZastavkaSpoje
import cz.jaro.dpmcb.data.helperclasses.Cas
import cz.jaro.dpmcb.data.helperclasses.Quadruple
import cz.jaro.dpmcb.data.helperclasses.Smer
import cz.jaro.dpmcb.data.helperclasses.Trvani.Companion.min
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.pristiZastavka
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.reversedIf
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.vsechnyIndexy
import cz.jaro.dpmcb.ui.UiEvent
import cz.jaro.dpmcb.ui.destinations.DetailSpojeScreenDestination
import cz.jaro.dpmcb.ui.destinations.JizdniRadyScreenDestination
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.runningReduce
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalCoroutinesApi::class)
class OdjezdyViewModel(
    val zastavka: String,
    cas: Cas = Cas.ted,
) : ViewModel() {

    lateinit var scrollovat: suspend () -> Unit

    @OptIn(ExperimentalCoroutinesApi::class)
    private val _state = MutableStateFlow(OdjezdyState(cas = cas))
    val state = _state.asStateFlow()

    private val _uiEvent = Channel<UiEvent>()
    val uiEvent = _uiEvent.receiveAsFlow()

    init {
        viewModelScope.launch(Dispatchers.IO) {
            repo.typDne.map { typDne ->
                repo
                    .spojeJedouciVTypDneZastavujiciNaZastavceSeZastavkySpoje(typDne, zastavka)
                    .flatMap { (spoj, zastavkySpoje) ->
                        val spojNaMape = dopravaRepo.spojNaMapePodleSpojeNeboUlozenehoId(spoj, zastavkySpoje)
                        zastavkySpoje.vsechnyIndexy(zastavka).map { index ->
                            Quadruple(spoj, zastavkySpoje[index], spojNaMape, zastavkySpoje)
                        }
                    }
                    .sortedBy { (_, zast, _, _) ->
                        zast.cas
                    }
                    .map { (spoj, zastavka, spojNaMape, zastavkySpoje) ->

                        val index = zastavkySpoje.indexOf(zastavka)
                        val posledniZastavka = zastavkySpoje.reversedIf { spoj.smer == Smer.NEGATIVNI }.last { it.cas != Cas.nikdy }
                        val pristiZastavkaSpoje = zastavkySpoje.pristiZastavka(spoj.smer, index) ?: posledniZastavka
                        val aktualniNasledujiciZastavka = spojNaMape.map { spojNaMape ->
                            spojNaMape?.delay?.let { zpozdeni ->
                                zastavkySpoje.reversedIf { spoj.smer == Smer.NEGATIVNI }.find { (it.cas + zpozdeni.min) > Cas.ted }
                            }
                        }.runningReduce { minulaZastavka, pristiZastavka ->
                            when {
                                minulaZastavka == null || pristiZastavka == null -> pristiZastavka
                                spoj.smer == Smer.POZITIVNI && pristiZastavka.indexNaLince < minulaZastavka.indexNaLince -> minulaZastavka
                                spoj.smer == Smer.NEGATIVNI && pristiZastavka.indexNaLince > minulaZastavka.indexNaLince -> minulaZastavka
                                else -> pristiZastavka
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
                            zpozdeni = spojNaMape.map { it?.delay },
                        )
                    }
            }.collect { seznam ->
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
        viewModelScope.launch {
            _uiEvent.send(
                UiEvent.Navigovat(
                    kam = DetailSpojeScreenDestination(
                        spoj.idSpoje
                    )
                )
            )
        }
    }

    fun kliklNaZjr(spoj: KartickaState) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiEvent.send(
                UiEvent.Navigovat(
                    kam = JizdniRadyScreenDestination(
                        cisloLinky = spoj.cisloLinky,
                        zastavka = zastavka,
                        pristiZastavka = spoj.pristiZastavka,
                    )
                )
            )
        }
    }

    fun zmenitCas(cas: Cas) {
        _state.update {
            it.copy(
                cas = cas,
                indexScrollovani = it.seznam.indexOfFirst { zast ->
                    zast.cas >= cas
                } + ((Int.MAX_VALUE / 2) / it.seznam.size) * it.seznam.size
            )
        }
        viewModelScope.launch(Dispatchers.Main) {
            scrollovat()
        }
    }

    fun scrolluje(i: Int) {
        _state.update {
            it.copy(
                indexScrollovani = i
            )
        }
    }

    data class KartickaState(
        val konecna: String,
        val pristiZastavka: String,
        val aktualniNasledujiciZastavka: Flow<ZastavkaSpoje?>,
        val cisloLinky: Int,
        val cas: Cas,
        val jePosledniZastavka: Boolean,
        val idSpoje: Long,
        val nizkopodlaznost: Boolean,
        val zpozdeni: Flow<Int?>,
    )

    data class OdjezdyState(
        val seznam: List<KartickaState> = emptyList(),
        val nacitaSe: Boolean = true,
        val cas: Cas,
        val indexScrollovani: Int = Int.MAX_VALUE / 2,
    )

    init {
        viewModelScope.launch(Dispatchers.IO) {
            while (state.value.seznam.isEmpty()) Unit
            _state.update {
                it.copy(
                    indexScrollovani = it.seznam.indexOfFirst { zast ->
                        zast.cas >= cas
                    } + ((Int.MAX_VALUE / 2) / it.seznam.size) * it.seznam.size
                )
            }
            withContext(Dispatchers.Main) {
                scrollovat()
            }
        }
    }
}
