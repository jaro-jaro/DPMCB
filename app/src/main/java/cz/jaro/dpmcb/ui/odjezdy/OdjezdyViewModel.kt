package cz.jaro.dpmcb.ui.odjezdy

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cz.jaro.dpmcb.data.App.Companion.dopravaRepo
import cz.jaro.dpmcb.data.App.Companion.repo
import cz.jaro.dpmcb.data.helperclasses.Cas
import cz.jaro.dpmcb.data.helperclasses.Cas.Companion.cas
import cz.jaro.dpmcb.data.helperclasses.Cas.Companion.toCas
import cz.jaro.dpmcb.data.helperclasses.Smer
import cz.jaro.dpmcb.data.helperclasses.Trvani.Companion.hod
import cz.jaro.dpmcb.data.helperclasses.Trvani.Companion.min
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions
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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope

class OdjezdyViewModel(
    zastavka: String,
    cas: String? = null,
    private val doba: Int = 5,
) : ViewModel() {

    private val _state = MutableStateFlow(
        OdjezdyState(
            zacatek = cas.toCas(),
            konec = cas.toCas() + doba.min,
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
        val seznam: List<KartickaState> = emptyList(),
        val nacitaSe: Boolean = false,
    )

    suspend fun nacistDalsi(typDne: UtilFunctions.VDP) = supervisorScope {
        _state.update {
            it.copy(nacitaSe = true)
        }

        launch(Dispatchers.IO) {
            val spojeAZastavky = List((state.value.konec.toTrvani() / 24.hod).toInt() + 1) { i ->
                val z = when (i) {
                    0 -> state.value.zacatek
                    else -> 0 cas 0
                }
                val k = when (i) {
                    (state.value.konec.toTrvani() / 24.hod).toInt() -> (state.value.konec.toTrvani() % 24.hod).toCas()
                    else -> 23 cas 59
                }
                funguj(state.value.zacatek, state.value.konec, z, k)

                repo
                    .spojeJedouciVTypDneZastavujiciNaZastavceSeZastavkySpoje(typDne, state.value.zastavka).also { funguj(1, it) }
                    .map { (spoj, zastavkySpoje) ->
                        (spoj to zastavkySpoje) to zastavkySpoje.vsechnyIndexy(state.value.zastavka)
                    }.also { funguj(2, it) }
                    .flatMap { (spojSeZastavkami, indexy) ->
                        indexy.map { spojSeZastavkami to it }
                    }.also { funguj(3, it) }
                    .map { (spojSeZastavkami, index) ->
                        spojSeZastavkami.first to spojSeZastavkami.second[index]
                    }.also { funguj(4, it) }
                    .filter { (_, zast) ->
                        zast.run { cas != Cas.nikdy && z <= cas && cas <= k }
                    }.also { funguj(5, it) }
                    .sortedBy { (_, zast) ->
                        zast.cas
                    }.also { funguj(6, it) }
            }.flatten()

            funguj(7, spojeAZastavky)

            _state.update { odjezdyState ->
                odjezdyState.copy(seznam = spojeAZastavky.map { (spoj, zastavka) ->

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
                        zpozdeni = spojNaMape.map { it?.delay }
                    )
                }, nacitaSe = false)
            }
        }
    }
}
