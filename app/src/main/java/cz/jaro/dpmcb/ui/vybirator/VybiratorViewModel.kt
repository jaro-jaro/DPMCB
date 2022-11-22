package cz.jaro.dpmcb.ui.vybirator

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ramcosta.composedestinations.result.ResultBackNavigator
import cz.jaro.dpmcb.data.App.Companion.repo
import cz.jaro.dpmcb.data.helperclasses.TypAdapteru
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.pristiZastavka
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.proVsechnyIndexy
import cz.jaro.dpmcb.ui.UiEvent
import cz.jaro.dpmcb.ui.destinations.JizdniRadyScreenDestination
import cz.jaro.dpmcb.ui.destinations.OdjezdyScreenDestination
import cz.jaro.dpmcb.ui.destinations.VybiratorScreenDestination
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import java.text.Normalizer

class VybiratorViewModel(
    private val typ: TypAdapteru,
    private val cisloLinky: Int = -1,
    private val zastavka: String?,
    private val resultNavigator: ResultBackNavigator<Vysledek>,
) : ViewModel() {

    var state by mutableStateOf(
        VybiratorState()
    )

    init {
        viewModelScope.launch {
            state = state.copy(puvodniSeznam = when (typ) {
                TypAdapteru.ZASTAVKY -> repo.zastavky.sortedBy { Normalizer.normalize(it, Normalizer.Form.NFD) }
                TypAdapteru.LINKY -> repo.cislaLinek.sorted().map { it.toString() }
                TypAdapteru.ZASTAVKY_LINKY -> repo.zastavkyLinky(cisloLinky).distinct()
                TypAdapteru.PRISTI_ZASTAVKA -> pristiZastavky(cisloLinky, zastavka!!)
                TypAdapteru.PRVNI_ZASTAVKA -> repo.zastavky.sortedBy { Normalizer.normalize(it, Normalizer.Form.NFD) }
                TypAdapteru.DRUHA_ZASTAVKA -> repo.zastavky.sortedBy { Normalizer.normalize(it, Normalizer.Form.NFD) }
            })
        }

        if (typ == TypAdapteru.ZASTAVKY_LINKY)
            state = state.copy(info = "$cisloLinky: ? -> ?")
        if (typ == TypAdapteru.PRISTI_ZASTAVKA)
            state = state.copy(info = "$cisloLinky: $zastavka -> ?")
    }

    private val _uiEvent = Channel<UiEvent>()
    val uiEvent = _uiEvent.receiveAsFlow()

    fun poslatEvent(event: VybiratorEvent) {
        viewModelScope.launch {
            when (event) {
                is VybiratorEvent.KliklEnter -> hotovo(state.seznam.first())
                is VybiratorEvent.KliklNaSeznam -> hotovo(event.vec)
                is VybiratorEvent.NapsalNeco -> {
                    state = state.copy(hledani = event.co.replace("\n", ""))
                    if (state.seznam.count() == 1) hotovo(state.seznam.first())
                }
            }
        }
    }

    data class VybiratorState(
        val hledani: String = "",
        val info: String = "",
        val puvodniSeznam: List<String> = emptyList(),
    ) {
        val seznam =
            if (hledani.isBlank()) puvodniSeznam
            else puvodniSeznam.filter { polozka ->
                hledani.lowercase().oddelatDiakritiku().split(" ").all { slovoHledani ->
                    polozka.lowercase().oddelatDiakritiku().split(" ").any { slovoPolozky ->
                        slovoPolozky.startsWith(slovoHledani)
                    }
                }
            }

        private fun String.oddelatDiakritiku() = Normalizer.normalize(this, Normalizer.Form.NFD).replace("\\p{Mn}+".toRegex(), "")
    }

    private suspend fun pristiZastavky(
        cisloLinky: Int,
        zastavka: String,
    ) = repo.zastavkyLinky(cisloLinky)
        .proVsechnyIndexy(zastavka) { index ->
            repo.spojeLinkyZastavujiciVZastavceSeZastavkamiSpoju(cisloLinky, index)
                .mapNotNull { (spoj, zastavkySpoje) ->
                    zastavkySpoje.pristiZastavka(spoj.smer, index)?.nazevZastavky
                }
        }.flatten().distinct()

    private suspend fun hotovo(
        vysledek: String,
    ) {
        when (typ) {
            TypAdapteru.ZASTAVKY -> _uiEvent.send(
                UiEvent.Navigovat(
                    OdjezdyScreenDestination(
                        zastavka = vysledek,
                        cas = null,
                    )
                )
            )
            TypAdapteru.LINKY -> _uiEvent.send(
                UiEvent.Navigovat(
                    VybiratorScreenDestination(
                        cisloLinky = vysledek.toInt(),
                        zastavka = null,
                        typ = TypAdapteru.ZASTAVKY_LINKY
                    )
                )
            )
            TypAdapteru.ZASTAVKY_LINKY -> _uiEvent.send(
                UiEvent.Navigovat(
                    pristiZastavky(cisloLinky, vysledek).let { pz ->
                        if (pz.size == 1)
                            JizdniRadyScreenDestination(
                                cisloLinky = cisloLinky,
                                zastavka = vysledek,
                                pristiZastavka = pz.first(),
                            )
                        else
                            VybiratorScreenDestination(
                                cisloLinky = cisloLinky,
                                zastavka = vysledek,
                                typ = TypAdapteru.PRISTI_ZASTAVKA
                            )
                    })
            )
            TypAdapteru.PRISTI_ZASTAVKA -> _uiEvent.send(
                UiEvent.Navigovat(
                    JizdniRadyScreenDestination(
                        cisloLinky = cisloLinky,
                        zastavka = zastavka!!,
                        pristiZastavka = vysledek,
                    )
                )
            )
            TypAdapteru.PRVNI_ZASTAVKA -> {
                resultNavigator.navigateBack(result = Vysledek(vysledek to true))
            }
            TypAdapteru.DRUHA_ZASTAVKA -> {
                resultNavigator.navigateBack(result = Vysledek(vysledek to false))
            }
        }
    }
}
