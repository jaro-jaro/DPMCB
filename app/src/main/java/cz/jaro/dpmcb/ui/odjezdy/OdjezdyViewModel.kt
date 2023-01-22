package cz.jaro.dpmcb.ui.odjezdy

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cz.jaro.dpmcb.data.App.Companion.dopravaRepo
import cz.jaro.dpmcb.data.App.Companion.repo
import cz.jaro.dpmcb.data.helperclasses.Cas
import cz.jaro.dpmcb.data.helperclasses.Cas.Companion.toCas
import cz.jaro.dpmcb.data.helperclasses.Smer
import cz.jaro.dpmcb.data.helperclasses.Trvani.Companion.min
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.pristiZastavka
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.reversedIf
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.vsechnyIndexy
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.zastavkySpoje
import cz.jaro.dpmcb.ui.UiEvent
import cz.jaro.dpmcb.ui.destinations.DetailSpojeScreenDestination
import cz.jaro.dpmcb.ui.destinations.JizdniRadyScreenDestination
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.zip
import kotlinx.coroutines.launch

class OdjezdyViewModel(
    zastavka: String,
    cas: String? = null,
    private val doba: Int = 5,
) : ViewModel() {

    private val _state = MutableStateFlow(
        OdjezdyState(
            zacatek = cas.toCas(),
            konec = cas.toCas() + doba.min,
            zastavka = zastavka,
            indexScrollovani = Int.MAX_VALUE / 2
        )
    )
    val state = _state.asStateFlow()

    private val _uiEvent = Channel<UiEvent>()
    val uiEvent = _uiEvent.receiveAsFlow()

    fun poslatEvent(event: OdjezdyEvent) {
        when (event) {
            is OdjezdyEvent.ZmensitCas -> {
                _state.update {
                    it.copy(zacatek = it.zacatek - 5.min)
                }
            }

            is OdjezdyEvent.ZvetsitCas -> {
                _state.update {
                    it.copy(zacatek = it.zacatek + 5.min)
                }
            }

            is OdjezdyEvent.ZmenitCas -> {
                _state.update {
                    it.copy(zacatek = event.novejCas, konec = event.novejCas + doba.min)
                }
            }

            is OdjezdyEvent.KliklNaDetailSpoje -> {
                viewModelScope.launch {
                    _uiEvent.send(
                        UiEvent.Navigovat(
                            kam = DetailSpojeScreenDestination(
                                event.spoj
                            )
                        )
                    )
                }
            }

            is OdjezdyEvent.KliklNaZjr -> {
                viewModelScope.launch(Dispatchers.IO) {
                    _uiEvent.send(
                        UiEvent.Navigovat(
                            kam = JizdniRadyScreenDestination(
                                cisloLinky = event.spoj.cisloLinky,
                                zastavka = state.value.zastavka,
                                pristiZastavka = event.spoj.pristiZastavka,
                            )
                        )
                    )
                }
            }

            is OdjezdyEvent.NacistDalsi -> {
                _state.update {
                    it.copy(konec = it.konec + doba.min)
                }
            }

            is OdjezdyEvent.NacistPredchozi -> {
                _state.update {
                    it.copy(zacatek = it.zacatek - doba.min)
                }
            }
        }
    }

    data class KartickaState(
        val konecna: String,
        val pristiZastavka: String,
        val cisloLinky: Int,
        val cas: Cas,
        val JePosledniZastavka: Boolean,
        val idSpoje: Long,
        val nizkopodlaznost: Boolean,
        val zpozdeni: Flow<Int?>,
    )

    data class OdjezdyState(
        val zacatek: Cas,
        val konec: Cas,
        val zastavka: String,
        val seznam: List<Deferred<KartickaState>> = emptyList(),
        val nacitaSe: Boolean = false,
        val indexScrollovani: Int,
    )

    init {
        viewModelScope.launch(Dispatchers.IO) {
            repo.typDne.zip(state) { typDne, state -> typDne to state }
                .collect { (typDne, state) ->
                    println("Collecting!")

                    val spojeAZastavky = repo
                        .spojeJedouciVTypDneZastavujiciNaZastavceSeZastavkySpoje(typDne, state.zastavka)
                        .flatMap { (spoj, zastavkySpoje) ->
                            zastavkySpoje.vsechnyIndexy(state.zastavka).map { index ->
                                spoj to zastavkySpoje[index]
                            }
                        }
                        .sortedBy { (_, zast) ->
                            zast.cas
                        }
                    println(spojeAZastavky)

                    val indexScrollovani = spojeAZastavky.indexOfFirst { (_, zast) ->
                        zast.cas >= state.zacatek
                    } + ((Int.MAX_VALUE / 2) / spojeAZastavky.size) * spojeAZastavky.size
                    println(indexScrollovani)

                    _state.update { odjezdyState ->
                        odjezdyState.copy(
                            seznam = spojeAZastavky.map { (spoj, zastavka) ->
                                async {
                                    val index = zastavka.indexNaLince
                                    val zastavky = spoj.zastavkySpoje()
                                    val poslZast = zastavky.reversedIf { spoj.smer == Smer.NEGATIVNI }.last { it.cas != Cas.nikdy }
                                    val spojNaMape = dopravaRepo.spojNaMapePodleSpojeNeboUlozenehoId(spoj, zastavky)

                                    KartickaState(
                                        konecna = poslZast.nazevZastavky,
                                        cisloLinky = spoj.cisloLinky,
                                        cas = zastavka.cas,
                                        JePosledniZastavka = zastavky.indexOf(poslZast) == index,
                                        pristiZastavka = zastavky.pristiZastavka(spoj.smer, index)?.nazevZastavky ?: poslZast.nazevZastavky,
                                        idSpoje = spoj.id,
                                        nizkopodlaznost = spoj.nizkopodlaznost,
                                        zpozdeni = spojNaMape.map { it?.delay },
                                    )
                                }
                            }/*.awaitAll()*/,
                            nacitaSe = false,
                            indexScrollovani = indexScrollovani
                        )
                    }
                    println("state updated")
                }
        }
    }
}
