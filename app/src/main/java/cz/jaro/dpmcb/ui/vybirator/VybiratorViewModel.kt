package cz.jaro.dpmcb.ui.vybirator

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cz.jaro.dpmcb.data.SpojeRepository
import cz.jaro.dpmcb.data.helperclasses.NavigateBackFunction
import cz.jaro.dpmcb.data.helperclasses.NavigateFunction
import cz.jaro.dpmcb.data.helperclasses.TypAdapteru
import cz.jaro.dpmcb.data.helperclasses.Vysledek
import cz.jaro.dpmcb.ui.destinations.JizdniRadyDestination
import cz.jaro.dpmcb.ui.destinations.OdjezdyDestination
import cz.jaro.dpmcb.ui.destinations.VybiratorDestination
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.android.annotation.KoinViewModel
import org.koin.core.annotation.InjectedParam
import java.text.Normalizer
import java.time.LocalDate

@KoinViewModel
class VybiratorViewModel(
    private val repo: SpojeRepository,
    @InjectedParam private val typ: TypAdapteru,
    @InjectedParam private val cisloLinky: Int = -1,
    @InjectedParam private val zastavka: String?,
    @InjectedParam private val navigate: NavigateFunction,
    @InjectedParam private val navigateBack: NavigateBackFunction<Vysledek>,
) : ViewModel() {

    private val puvodniSeznam = repo.datum.map { datum ->
        when (typ) {
            TypAdapteru.ZASTAVKY -> repo.zastavky(datum).sortedBy { Normalizer.normalize(it, Normalizer.Form.NFD) }
            TypAdapteru.LINKY -> repo.cislaLinek(datum).sorted().map { it.toString() }
            TypAdapteru.ZASTAVKY_LINKY -> repo.nazvyZastavekLinky(cisloLinky, datum).distinct()
            TypAdapteru.PRISTI_ZASTAVKA -> repo.pristiZastavky(cisloLinky, zastavka!!, datum)
            TypAdapteru.ZASTAVKY_ZPET_1 -> repo.zastavky(datum).sortedBy { Normalizer.normalize(it, Normalizer.Form.NFD) }
            TypAdapteru.ZASTAVKA_ZPET_2 -> repo.zastavky(datum).sortedBy { Normalizer.normalize(it, Normalizer.Form.NFD) }
            TypAdapteru.LINKA_ZPET -> repo.cislaLinek(datum).sorted().map { it.toString() }
            TypAdapteru.ZASTAVKA_ZPET -> repo.zastavky(datum).sortedBy { Normalizer.normalize(it, Normalizer.Form.NFD) }
        }
    }

    private val _hledani = MutableStateFlow("")
    val hledani = _hledani.asStateFlow()

    fun hledani(hledani: String) {
        _hledani.value = hledani
    }

    val seznam = _hledani.combine(puvodniSeznam) { filtr, puvodniSeznam ->
        if (filtr.isBlank()) puvodniSeznam
        else puvodniSeznam.filter { polozka ->
            filtr.lowercase().oddelatDiakritiku().split(" ").all { slovoHledani ->
                polozka.lowercase().oddelatDiakritiku().split(" ").any { slovoPolozky ->
                    slovoPolozky.startsWith(slovoHledani)
                }
            }
        }.also { seznam ->
            if (seznam.count() == 1) hotovo(seznam.first(), repo.datum.value)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val info = when (typ) {
        TypAdapteru.ZASTAVKY_LINKY -> "$cisloLinky: ? -> ?"
        TypAdapteru.PRISTI_ZASTAVKA -> "$cisloLinky: $zastavka -> ?"
        else -> ""
    }

    fun napsalNeco(co: String) {
        _hledani.value = co.replace("\n", "")
    }

    fun kliklNaVecZeSeznamu(vec: String) = hotovo(vec, repo.datum.value)

    fun kliklEnter() = hotovo(seznam.value.first(), repo.datum.value)

    private fun String.oddelatDiakritiku() = Normalizer.normalize(this, Normalizer.Form.NFD).replace("\\p{Mn}+".toRegex(), "")

    private fun hotovo(
        vysledek: String,
        datum: LocalDate,
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
                repo.pristiZastavky(cisloLinky, vysledek, datum).let { pz: List<String> ->
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
