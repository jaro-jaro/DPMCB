package cz.jaro.dpmcb.ui.vybirator

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cz.jaro.dpmcb.data.SpojeRepository
import cz.jaro.dpmcb.data.helperclasses.NavigateBackFunction
import cz.jaro.dpmcb.data.helperclasses.NavigateFunction
import cz.jaro.dpmcb.data.helperclasses.TypAdapteru
import cz.jaro.dpmcb.data.helperclasses.Vysledek
import cz.jaro.dpmcb.ui.UiEvent
import cz.jaro.dpmcb.ui.destinations.JizdniRadyDestination
import cz.jaro.dpmcb.ui.destinations.OdjezdyDestination
import cz.jaro.dpmcb.ui.destinations.VybiratorDestination
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.Normalizer

class VybiratorViewModel(
    private val repo: SpojeRepository,
    private val typ: TypAdapteru,
    private val cisloLinky: Int = -1,
    private val zastavka: String?,
    private val navigate: NavigateFunction,
    private val navigateBack: NavigateBackFunction<Vysledek>,
) : ViewModel() {

    private val puvodniSeznam = viewModelScope.async {
        when (typ) {
            TypAdapteru.ZASTAVKY -> repo.zastavky().sortedBy { Normalizer.normalize(it, Normalizer.Form.NFD) }
            TypAdapteru.LINKY -> repo.cislaLinek().sorted().map { it.toString() }
            TypAdapteru.ZASTAVKY_LINKY -> repo.nazvyZastavekLinky(cisloLinky).distinct()
            TypAdapteru.PRISTI_ZASTAVKA -> pristiZastavky(cisloLinky, zastavka!!)
            TypAdapteru.ZASTAVKY_ZPET_1 -> repo.zastavky().sortedBy { Normalizer.normalize(it, Normalizer.Form.NFD) }
            TypAdapteru.ZASTAVKA_ZPET_2 -> repo.zastavky().sortedBy { Normalizer.normalize(it, Normalizer.Form.NFD) }
            TypAdapteru.LINKA_ZPET -> repo.cislaLinek().sorted().map { it.toString() }
            TypAdapteru.ZASTAVKA_ZPET -> repo.zastavky().sortedBy { Normalizer.normalize(it, Normalizer.Form.NFD) }
        }
    }

    private val _hledani = MutableStateFlow("")
    val hledani = _hledani.asStateFlow()

    fun hledani(hledani: String) {
        _hledani.value = hledani
    }

    val seznam = _hledani.map { filtr ->
        if (filtr.isBlank()) puvodniSeznam.await()
        else puvodniSeznam.await().filter { polozka ->
            filtr.lowercase().oddelatDiakritiku().split(" ").all { slovoHledani ->
                polozka.lowercase().oddelatDiakritiku().split(" ").any { slovoPolozky ->
                    slovoPolozky.startsWith(slovoHledani)
                }
            }
        }.also { seznam ->
            if (seznam.count() == 1) hotovo(seznam.first())
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val info = when (typ) {
        TypAdapteru.ZASTAVKY_LINKY -> "$cisloLinky: ? -> ?"
        TypAdapteru.PRISTI_ZASTAVKA -> "$cisloLinky: $zastavka -> ?"
        else -> ""
    }

    private val _uiEvent = Channel<UiEvent>()
    val uiEvent = _uiEvent.receiveAsFlow()

    fun napsalNeco(co: String) {
        _hledani.value = co.replace("\n", "")
    }

    fun kliklNaVecZeSeznamu(vec: String) = hotovo(vec)

    fun kliklEnter() = hotovo(seznam.value.first())

    private fun String.oddelatDiakritiku() = Normalizer.normalize(this, Normalizer.Form.NFD).replace("\\p{Mn}+".toRegex(), "")

    private suspend fun pristiZastavky(
        cisloLinky: Int,
        zastavka: String,
    ) = repo.pristiZastavky(cisloLinky, zastavka)

    private fun hotovo(
        vysledek: String,
    ) {
//        if (job != null && typ.name.contains("ZPET")) return
        when (typ) {
            TypAdapteru.ZASTAVKY -> navigate(
                OdjezdyDestination(
                    zastavka = vysledek,
                )
            )

            TypAdapteru.LINKY -> navigate(
                VybiratorDestination(
                    cisloLinky = vysledek.toInt(),
                    zastavka = null,
                    typ = TypAdapteru.ZASTAVKY_LINKY

                )
            )

            TypAdapteru.ZASTAVKY_LINKY -> viewModelScope.launch(Dispatchers.IO) {
                pristiZastavky(cisloLinky, vysledek).let { pz: List<String> ->
                    withContext(Dispatchers.Main) {
                        navigate(
                            if (pz.size == 1)
                                JizdniRadyDestination(
                                    cisloLinky = cisloLinky,
                                    zastavka = vysledek,
                                    pristiZastavka = pz.first(),
                                )
                            else
                                VybiratorDestination(
                                    cisloLinky = cisloLinky,
                                    zastavka = vysledek,
                                    typ = TypAdapteru.PRISTI_ZASTAVKA
                                )
                        )
                    }
                }
            }

            TypAdapteru.PRISTI_ZASTAVKA -> navigate(
                JizdniRadyDestination(
                    cisloLinky = cisloLinky,
                    zastavka = zastavka!!,
                    pristiZastavka = vysledek,
                )
            )

            TypAdapteru.ZASTAVKY_ZPET_1 -> {
                navigateBack(Vysledek(vysledek, typ))
            }

            TypAdapteru.ZASTAVKA_ZPET_2 -> {
                navigateBack(Vysledek(vysledek, typ))
            }

            TypAdapteru.LINKA_ZPET -> {
                navigateBack(Vysledek(vysledek, typ))
            }

            TypAdapteru.ZASTAVKA_ZPET -> {
                navigateBack(Vysledek(vysledek, typ))
            }
        }
    }
}
