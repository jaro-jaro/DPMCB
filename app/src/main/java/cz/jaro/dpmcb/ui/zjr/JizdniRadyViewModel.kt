package cz.jaro.dpmcb.ui.zjr

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cz.jaro.dpmcb.data.App.Companion.repo
import cz.jaro.dpmcb.data.entities.ZastavkaSpoje
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.funguj
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.pristiZastavka
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class JizdniRadyViewModel(
    cisloLinky: Int,
    zastavka: String,
    pristiZastavka: String,
) : ViewModel() {

    var state by mutableStateOf(JizdniRadyState())

    init {
        viewModelScope.launch(Dispatchers.IO) {
            delay(500)
            state = state.copy(nacitaSe = true)

            val spoje = repo.spojeLinkyJedouciVDatumSeZastavkamiSpoju(cisloLinky, repo.datum).also { println(it) }

            val zastavky = spoje.flatMap { (spoj, zastavkySpoje) ->

                //println(zastavkySpoje)

                zastavkySpoje.filterIndexed { i, zastavkaTohotoSpoje ->

                    val pristiZastavkaTohotoSpoje = zastavkySpoje.pristiZastavka(spoj.smer, i)?.nazevZastavky ?: ""

                    funguj(zastavkaTohotoSpoje.nazevZastavky, zastavka, pristiZastavkaTohotoSpoje, pristiZastavka)

                    zastavkaTohotoSpoje.nazevZastavky == zastavka && pristiZastavkaTohotoSpoje == pristiZastavka
                }.map { it to spoj.id }
            }

            println(zastavky)

            println(zastavka to pristiZastavka)

            state = state.copy(casyOdjezduSeSpoji = zastavky, nacitaSe = false)
        }
    }

    fun dataProHodinu(h: Int) = state.casyOdjezduSeSpoji.filter { h == it.first.cas.h }

    data class JizdniRadyState(
        val nacitaSe: Boolean = true,
        val casyOdjezduSeSpoji: List<Pair<ZastavkaSpoje, Long>> = emptyList(),
    )
}
