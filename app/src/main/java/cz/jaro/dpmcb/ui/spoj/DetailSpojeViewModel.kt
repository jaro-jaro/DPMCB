package cz.jaro.dpmcb.ui.spoj

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Accessible
import androidx.compose.material.icons.filled.AccessibleForward
import androidx.compose.material.icons.filled.NotAccessible
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.WheelchairPickup
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cz.jaro.datum_cas.Cas
import cz.jaro.datum_cas.Datum
import cz.jaro.datum_cas.Trvani
import cz.jaro.datum_cas.min
import cz.jaro.datum_cas.sek
import cz.jaro.dpmcb.data.App
import cz.jaro.dpmcb.data.App.Companion.repo
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions
import cz.jaro.dpmcb.data.naJihu.ZastavkaSpojeNaJihu
import cz.jaro.dpmcb.data.realtions.CasNazevSpojId
import cz.jaro.dpmcb.ui.UiEvent
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.random.Random

class DetailSpojeViewModel(
    spojId: String,
) : ViewModel() {

    private val _state = MutableStateFlow(
        DetailSpojeState(
            zastavky = emptyList(),
            cisloLinky = -1,
            nizkopodlaznost = Icons.Default.AccessibleForward,
            zpozdeni = null,
            zastavkyNaJihu = null,
            caskody = emptyList(),
            pevneKody = emptyList(),
            nazevSpoje = spojId.split("-").let { "${it[1]}/${it[2]}" },
            deeplink = "https://jaro-jaro.github.io/DPMCB/spoj/$spojId"
        )
    )
    val state = _state.asStateFlow()

    private val _uiEvent = Channel<UiEvent>()
    val uiEvent = _uiEvent.receiveAsFlow()

    val vyska = _state.combine(Cas.tedFlow) { state, ted ->

        val prejetychUseku = when {
            state.zastavkyNaJihu != null -> state.zastavkyNaJihu.indexOfLast { it.passed }.coerceAtLeast(0)
            state.zastavky.last().cas < ted -> state.zastavky.lastIndex
            else -> state.zastavky.indexOfLast { it.cas < ted }
        }

        val casOdjezduPosledni = state.zastavky[prejetychUseku].cas.plus(state.zpozdeni?.min ?: Trvani.zadne)

        val casPrijezduDoPristi = state.zastavky[prejetychUseku + 1].cas.plus(state.zpozdeni?.min ?: Trvani.zadne)

        val dobaJizdy = casOdjezduPosledni.let {
            casPrijezduDoPristi.minus(it)
        }

        val ubehlo = casOdjezduPosledni.let {
            ted.minus(it).coerceAtLeast(0.sek)
        }

        UtilFunctions.funguj(
            ted,
            prejetychUseku,
            casOdjezduPosledni,
            casPrijezduDoPristi,
            dobaJizdy,
            ubehlo,
            (ubehlo / dobaJizdy).toFloat().coerceAtMost(1F),
            prejetychUseku + (ubehlo / dobaJizdy).toFloat().coerceAtMost(1F)
        )
        prejetychUseku + (ubehlo / dobaJizdy).toFloat().coerceAtMost(1F)
    }

    init {
        viewModelScope.launch {
            val (spoj, zastavky, caskody, pevneKody) = repo.spojSeZastavkySpojeNaKterychStaviACaskody(spojId)
            _state.update { state ->
                state.copy(
                    zastavky = zastavky,
                    cisloLinky = spoj.linka,
                    nizkopodlaznost = when {
                        Random.nextFloat() < .01F -> Icons.Default.ShoppingCart
                        spoj.nizkopodlaznost -> Icons.Default.Accessible
                        Random.nextFloat() < .33F -> Icons.Default.WheelchairPickup
                        else -> Icons.Default.NotAccessible
                    },
                    caskody = caskody.filterNot {
                        !it.jede && it.v == Datum(0, 0, 0)..Datum(0, 0, 0)
                    }.groupBy({ it.jede }, {
                        if (it.v.start != it.v.endInclusive) "od ${it.v.start} do ${it.v.endInclusive}" else "${it.v.start}"
                    }).map { (jede, terminy) ->
                        (if (jede) "Jede " else "Nejede ") + terminy.joinToString()
                    },
                    pevneKody = pevneKody,
                    nacitaSe = false,
                )
            }

            App.dopravaRepo.spojPodleId(spojId).collect { (spojNaMape, detailSpoje) ->
                _state.update {
                    it.copy(
                        zpozdeni = spojNaMape?.delay,
                        zastavkyNaJihu = detailSpoje?.stations
                    )
                }
            }
        }
    }

    data class DetailSpojeState(
        val zastavky: List<CasNazevSpojId>,
        val cisloLinky: Int,
        val nizkopodlaznost: ImageVector,
        val zpozdeni: Int?,
        val zastavkyNaJihu: List<ZastavkaSpojeNaJihu>?,
        val nacitaSe: Boolean = true,
        val caskody: List<String>,
        val pevneKody: List<String>,
        val nazevSpoje: String,
        val deeplink: String,
    )
}