package cz.jaro.dpmcb.ui.vybirator

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cz.jaro.dpmcb.data.SpojeRepository
import cz.jaro.dpmcb.data.helperclasses.NavigateBackFunction
import cz.jaro.dpmcb.data.helperclasses.NavigateFunction
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
    @InjectedParam private val params: Parameters,
) : ViewModel() {

    data class Parameters(
        val typ: TypVybiratoru,
        val cisloLinky: Int = -1,
        val zastavka: String?,
        val navigate: NavigateFunction,
        val navigateBack: NavigateBackFunction<Vysledek>,
    )

    private val puvodniSeznam = repo.datum.map { datum ->
        when (params.typ) {
            TypVybiratoru.ZASTAVKY -> repo.zastavky(datum).sortedBy { Normalizer.normalize(it, Normalizer.Form.NFD) }
            TypVybiratoru.LINKY -> repo.cislaLinek(datum).sorted().map { it.toString() }
            TypVybiratoru.ZASTAVKY_LINKY -> repo.nazvyZastavekLinky(params.cisloLinky, datum).distinct()
            TypVybiratoru.PRISTI_ZASTAVKA -> repo.pristiZastavky(params.cisloLinky, params.zastavka!!, datum)
            TypVybiratoru.ZASTAVKY_ZPET_1 -> repo.zastavky(datum).sortedBy { Normalizer.normalize(it, Normalizer.Form.NFD) }
            TypVybiratoru.ZASTAVKA_ZPET_2 -> repo.zastavky(datum).sortedBy { Normalizer.normalize(it, Normalizer.Form.NFD) }
            TypVybiratoru.LINKA_ZPET -> repo.cislaLinek(datum).sorted().map { it.toString() }
            TypVybiratoru.ZASTAVKA_ZPET -> repo.zastavky(datum).sortedBy { Normalizer.normalize(it, Normalizer.Form.NFD) }
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

    val info = when (params.typ) {
        TypVybiratoru.ZASTAVKY_LINKY -> "${params.cisloLinky}: ? -> ?"
        TypVybiratoru.PRISTI_ZASTAVKA -> "${params.cisloLinky}: ${params.zastavka} -> ?"
        else -> ""
    }

    fun napsalNeco(co: String) {
        _hledani.value = co.replace("\n", "")
    }

    fun kliklNaVecZeSeznamu(vec: String) = hotovo(vec, repo.datum.value)

    fun kliklEnter() = seznam.value.firstOrNull()?.let { hotovo(it, repo.datum.value) } ?: Unit

    private fun String.oddelatDiakritiku() = Normalizer.normalize(this, Normalizer.Form.NFD).replace("\\p{Mn}+".toRegex(), "")

    private fun hotovo(
        vysledek: String,
        datum: LocalDate,
    ) {
//        if (job != null && typ.name.contains("ZPET")) return
        when (params.typ) {
            TypVybiratoru.ZASTAVKY -> params.navigate(
                OdjezdyDestination(
                    zastavka = vysledek,
                )
            )

            TypVybiratoru.LINKY -> params.navigate(
                VybiratorDestination(
                    cisloLinky = vysledek.toInt(),
                    zastavka = null,
                    typ = TypVybiratoru.ZASTAVKY_LINKY

                )
            )

            TypVybiratoru.ZASTAVKY_LINKY -> viewModelScope.launch(Dispatchers.IO) {
                repo.pristiZastavky(params.cisloLinky, vysledek, datum).let { pz: List<String> ->
                    withContext(Dispatchers.Main) {
                        params.navigate(
                            if (pz.size == 1)
                                JizdniRadyDestination(
                                    cisloLinky = params.cisloLinky,
                                    zastavka = vysledek,
                                    pristiZastavka = pz.first(),
                                )
                            else
                                VybiratorDestination(
                                    cisloLinky = params.cisloLinky,
                                    zastavka = vysledek,
                                    typ = TypVybiratoru.PRISTI_ZASTAVKA
                                )
                        )
                    }
                }
            }

            TypVybiratoru.PRISTI_ZASTAVKA -> params.navigate(
                JizdniRadyDestination(
                    cisloLinky = params.cisloLinky,
                    zastavka = params.zastavka!!,
                    pristiZastavka = vysledek,
                )
            )

            TypVybiratoru.ZASTAVKY_ZPET_1 -> {
                params.navigateBack(Vysledek(vysledek, params.typ))
            }

            TypVybiratoru.ZASTAVKA_ZPET_2 -> {
                params.navigateBack(Vysledek(vysledek, params.typ))
            }

            TypVybiratoru.LINKA_ZPET -> {
                params.navigateBack(Vysledek(vysledek, params.typ))
            }

            TypVybiratoru.ZASTAVKA_ZPET -> {
                params.navigateBack(Vysledek(vysledek, params.typ))
            }
        }
    }
}
