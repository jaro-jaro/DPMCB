package cz.jaro.dpmcb.ui.detail.spoje

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Accessible
import androidx.compose.material.icons.filled.AccessibleForward
import androidx.compose.material.icons.filled.NotAccessible
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.WheelchairPickup
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cz.jaro.dpmcb.data.App
import cz.jaro.dpmcb.data.App.Companion.repo
import cz.jaro.dpmcb.data.helperclasses.Cas
import cz.jaro.dpmcb.data.helperclasses.Cas.Companion.toCas
import cz.jaro.dpmcb.data.helperclasses.Trvani
import cz.jaro.dpmcb.data.helperclasses.Trvani.Companion.min
import cz.jaro.dpmcb.data.helperclasses.Trvani.Companion.sek
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
        )
    )
    val state = _state.asStateFlow()

    private val _uiEvent = Channel<UiEvent>()
    val uiEvent = _uiEvent.receiveAsFlow()

    val vyska = _state.combine(Cas.presneTed) { state, ted ->

        val prejetychUseku = state.zastavkyNaJihu?.count { it.passed }?.minus(1)?.coerceAtLeast(0) ?: 0

        val casOdjezduPosledni = state.zpozdeni?.min?.let { zpozdeni ->
            state.zastavkyNaJihu?.findLast { it.passed }?.departureTime?.toCas()?.plus(zpozdeni)
        }

        val casPrijezduDoPristi = state.zpozdeni?.min?.let { zpozdeni ->
            state.zastavkyNaJihu?.find { !it.passed }?.arrivalTime?.toCas()?.plus(zpozdeni)
        }

        val dobaJizdy = casOdjezduPosledni?.let {
            casPrijezduDoPristi?.minus(it)
        } ?: Trvani.nekonecne

        val ubehlo = casOdjezduPosledni?.let {
            ted.minus(it).coerceAtLeast(0.sek)
        } ?: 0.sek

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
            val (spoj, zastavky) = repo.spojSeZastavkySpojeNaKterychStavi(spojId)
            _state.update { state ->
                state.copy(
                    zastavky = zastavky.map { zastavka ->
                        zastavka.copy(
                            odjezd = zastavka.odjezd.takeUnless { it == Cas.nikdy },
                            prijezd = zastavka.prijezd.takeUnless { it == Cas.nikdy },
                        )
                    },
                    cisloLinky = spoj.linka,
                    nizkopodlaznost = when {
                        Random.nextFloat() < .01F -> Icons.Default.ShoppingCart
                        spoj.pevneKody.contains("24") -> Icons.Default.Accessible
                        Random.nextFloat() < .33F -> Icons.Default.WheelchairPickup
                        else -> Icons.Default.NotAccessible
                    },
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
    )
}