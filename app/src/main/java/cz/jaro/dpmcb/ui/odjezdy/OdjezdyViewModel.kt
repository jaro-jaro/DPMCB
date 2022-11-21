package cz.jaro.dpmcb.ui.odjezdy

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cz.jaro.dpmcb.data.App.Companion.repo
import cz.jaro.dpmcb.data.helperclasses.Cas
import cz.jaro.dpmcb.data.helperclasses.Cas.Companion.cas
import cz.jaro.dpmcb.data.helperclasses.Cas.Companion.toCas
import cz.jaro.dpmcb.data.helperclasses.Smer
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.funguj
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.pristiZastavka
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.reversedIf
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.vsechnyIndexy
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.zastavkySpoje
import cz.jaro.dpmcb.ui.UiEvent
import cz.jaro.dpmcb.ui.destinations.DetailSpojeScreenDestination
import cz.jaro.dpmcb.ui.destinations.JizdniRadyScreenDestination
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope

class OdjezdyViewModel(
    zastavka: String,
    cas: String? = null,
    doba: Int = 30,
) : ViewModel() {

    private val _state = MutableStateFlow(
        OdjezdyState(
            zacatek = cas.toCas(),
            konec = cas.toCas() + doba,
            zastavka = zastavka
        )
    )
    val state = _state.asStateFlow()

    private val _uiEvent = Channel<UiEvent>()
    val uiEvent = _uiEvent.receiveAsFlow()

    fun poslatEvent(event: OdjezdyEvent) {
        when (event) {
            is OdjezdyEvent.ZmensitCas -> {
                _state.update {
                    it.copy(zacatek = it.zacatek - 5)
                }
            }

            is OdjezdyEvent.ZvetsitCas -> {
                _state.update {
                    it.copy(zacatek = it.zacatek + 5)
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
                    it.copy(nacitaSe = true, konec = it.konec + 30)
                }
            }

            is OdjezdyEvent.NacistPredchozi -> {
                _state.update {
                    it.copy(nacitaSe = true, zacatek = it.zacatek - 30)
                }
            }
        }
    }

    data class KartickaState(
        val konecna: String,
        val pristiZastavka: String,
        val cisloLinky: Int,
        val cas: String,
        val JePosledniZastavka: Boolean,
        val idSpoje: Long,
    )

    data class OdjezdyState(
        val zacatek: Cas,
        val konec: Cas,
        val zastavka: String,
        val seznam: List<KartickaState> = emptyList(),
        val nacitaSe: Boolean = false,
    )

    suspend fun nacistDalsi() = supervisorScope {

        launch(Dispatchers.IO) {
            val spojeAZastavky = List(state.value.konec.toInt() / (24 * 60) + 1) { i ->
                val z = when (i) {
                    0 -> state.value.zacatek
                    else -> 0 cas 0
                }
                val k = when (i) {
                    (state.value.konec.toInt() / (24 * 60)) -> (state.value.konec.toInt() % (24 * 60)).toCas()
                    else -> 23 cas 59
                }
                val d = repo.datum + i
                funguj(z, k, d)

                repo
                    .spojeJedouciVDatumZastavujiciNaZastavceSeZastavkySpoje(d, state.value.zastavka).also { funguj(it) }
                    .map { (spoj, zastavkySpoje) ->
                        (spoj to zastavkySpoje) to zastavkySpoje.vsechnyIndexy(state.value.zastavka)
                    }.also { funguj(it) }
                    .flatMap { (spojSeZastavkami, indexy) ->
                        indexy.map { spojSeZastavkami to it }
                    }.also { funguj(it) }
                    .map { (spojSeZastavkami, index) ->
                        spojSeZastavkami.first to spojSeZastavkami.second[index]
                    }.also { funguj(it) }
                    .filter { (_, zast) ->
                        zast.also { funguj(it) }.run { cas != Cas.nikdy && z <= cas && cas <= k }
                    }.also { funguj(it) }
                    .sortedBy { (_, zast) ->
                        zast.cas.toInt()
                    }.also { funguj(it) }
            }.flatten()

            funguj(spojeAZastavky)

            _state.update { odjezdyState ->
                odjezdyState.copy(seznam = spojeAZastavky.map { (spoj, zastavka) ->

                    val index = zastavka.indexNaLince
                    val zastavky = spoj.zastavkySpoje()
                    val poslZast = zastavky.reversedIf { spoj.smer == Smer.NEGATIVNI }.last { it.cas != Cas.nikdy }

                    KartickaState(
                        konecna = poslZast.nazevZastavky,
                        cisloLinky = spoj.cisloLinky,
                        cas = zastavka.cas.toString(),
                        JePosledniZastavka = zastavky.indexOf(poslZast) == index,
                        pristiZastavka = spoj.pristiZastavka(index)?.nazevZastavky ?: poslZast.nazevZastavky,
                        idSpoje = spoj.id
                    )
                }, nacitaSe = false)
            }
        }
    }
}
