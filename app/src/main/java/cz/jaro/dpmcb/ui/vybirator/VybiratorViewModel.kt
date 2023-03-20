package cz.jaro.dpmcb.ui.vybirator

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ramcosta.composedestinations.result.ResultBackNavigator
import cz.jaro.dpmcb.data.App.Companion.repo
import cz.jaro.dpmcb.data.helperclasses.TypAdapteru
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.funguj
import cz.jaro.dpmcb.ui.UiEvent
import cz.jaro.dpmcb.ui.destinations.JizdniRadyScreenDestination
import cz.jaro.dpmcb.ui.destinations.OdjezdyScreenDestination
import cz.jaro.dpmcb.ui.destinations.VybiratorScreenDestination
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.Normalizer

class VybiratorViewModel(
    private val typ: TypAdapteru,
    private val cisloLinky: Int = -1,
    private val zastavka: String?,
    private val resultNavigator: ResultBackNavigator<Vysledek>,
) : ViewModel() {

    private val _state = MutableStateFlow(VybiratorState())
    val state = _state.asStateFlow()

    init {
        cisloLinky.funguj(zastavka, typ)
        state.funguj()
        viewModelScope.launch {
            _state.update {
                it.copy(puvodniSeznam = when (typ) {
                    TypAdapteru.ZASTAVKY -> repo.zastavky().sortedBy { Normalizer.normalize(it, Normalizer.Form.NFD) }
                    TypAdapteru.LINKY -> repo.cislaLinek().sorted().map { it.toString() }
                    TypAdapteru.ZASTAVKY_LINKY -> repo.nazvyZastavekLinky(cisloLinky).distinct()
                    TypAdapteru.PRISTI_ZASTAVKA -> pristiZastavky(cisloLinky, zastavka!!)
                    TypAdapteru.ZASTAVKY_ZPET_1 -> repo.zastavky().sortedBy { Normalizer.normalize(it, Normalizer.Form.NFD) }
                    TypAdapteru.ZASTAVKA_ZPET_2 -> repo.zastavky().sortedBy { Normalizer.normalize(it, Normalizer.Form.NFD) }
                    TypAdapteru.LINKA_ZPET -> repo.cislaLinek().sorted().map { it.toString() }
                    TypAdapteru.ZASTAVKA_ZPET -> repo.zastavky().sortedBy { Normalizer.normalize(it, Normalizer.Form.NFD) }
                })
            }
        }
        state.funguj()

        if (typ == TypAdapteru.ZASTAVKY_LINKY) _state.update {
            it.copy(info = "$cisloLinky: ? -> ?")
        }
        if (typ == TypAdapteru.PRISTI_ZASTAVKA) _state.update {
            it.copy(info = "$cisloLinky: $zastavka -> ?")
        }
        state.funguj()
    }

    private val _uiEvent = Channel<UiEvent>()
    val uiEvent = _uiEvent.receiveAsFlow()

    fun poslatEvent(event: VybiratorEvent) {
        when (event) {
            is VybiratorEvent.KliklEnter -> hotovo(state.value.seznam.first())
            is VybiratorEvent.KliklNaSeznam -> hotovo(event.vec)
            is VybiratorEvent.NapsalNeco -> {
                _state.update {
                    it.copy(hledani = event.co.replace("\n", ""))
                }
                if (state.value.seznam.count() == 1) hotovo(state.value.seznam.first())
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
    ) = repo.pristiZastavky(cisloLinky, zastavka)

    private var job: Job? = null

    private fun hotovo(
        vysledek: String,
    ) {
        if (job != null && typ.name.contains("ZPET")) return
        job = viewModelScope.launch {
            when (typ) {
                TypAdapteru.ZASTAVKY -> _uiEvent.send(
                    UiEvent.Navigovat(
                        OdjezdyScreenDestination(
                            zastavka = vysledek,
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
                        pristiZastavky(cisloLinky, vysledek).let { pz: List<String> ->
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

                TypAdapteru.ZASTAVKY_ZPET_1 -> {
                    resultNavigator.navigateBack(result = Vysledek(vysledek, typ))
                }

                TypAdapteru.ZASTAVKA_ZPET_2 -> {
                    resultNavigator.navigateBack(result = Vysledek(vysledek, typ))
                }

                TypAdapteru.LINKA_ZPET -> {
                    resultNavigator.navigateBack(result = Vysledek(vysledek, typ))
                }

                TypAdapteru.ZASTAVKA_ZPET -> {
                    resultNavigator.navigateBack(result = Vysledek(vysledek, typ))
                }
            }
        }
    }
}
