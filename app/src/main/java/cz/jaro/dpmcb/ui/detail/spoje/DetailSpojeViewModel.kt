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
import cz.jaro.dpmcb.data.entities.Spoj
import cz.jaro.dpmcb.data.entities.ZastavkaSpoje
import cz.jaro.dpmcb.data.helperclasses.Cas
import cz.jaro.dpmcb.data.helperclasses.Cas.Companion.toCas
import cz.jaro.dpmcb.data.helperclasses.Smer
import cz.jaro.dpmcb.data.helperclasses.Trvani
import cz.jaro.dpmcb.data.helperclasses.Trvani.Companion.min
import cz.jaro.dpmcb.data.helperclasses.Trvani.Companion.sek
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.funguj
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.reversedIf
import cz.jaro.dpmcb.data.naJihu.ZastavkaSpojeNaJihu
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
    spojId: Long,
) : ViewModel() {

    private val _state = MutableStateFlow(
        DetailSpojeState(
            zastavky = emptyList(),
            cisloLinky = -1,
            nizkopodlaznost = Icons.Default.AccessibleForward,
            idNaJihu = null,
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
            ted.minus(it)
        } ?: 0.sek

        funguj(
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
            this@DetailSpojeViewModel.spoj = spoj
            _state.update {
                it.copy(
                    zastavky = zastavky.reversedIf { spoj.smer == Smer.NEGATIVNI },
                    cisloLinky = spoj.cisloLinky,
                    nizkopodlaznost = when {
                        Random.nextFloat() < .01F -> Icons.Default.ShoppingCart
                        spoj.nizkopodlaznost -> Icons.Default.Accessible
                        Random.nextFloat() < .33F -> Icons.Default.WheelchairPickup
                        else -> Icons.Default.NotAccessible
                    },
                    nacitaSe = false,
                )
            }

            App.dopravaRepo.spojPodleSpojeNeboUlozenehoId(spoj, zastavky).collect { (spojNaMape, detailSpoje) ->
                _state.update {
                    it.copy(
                        idNaJihu = repo.idSpoju.getOrElse(spojId) { spojNaMape?.id ?: detailSpoje?.id },
                        zpozdeni = spojNaMape?.delay,
                        zastavkyNaJihu = detailSpoje?.stations
                    )
                }
            }
        }
    }

    data class DetailSpojeState(
        val zastavky: List<ZastavkaSpoje>,
        val cisloLinky: Int,
        val nizkopodlaznost: ImageVector,
        val idNaJihu: String?,
        val zpozdeni: Int?,
        val zastavkyNaJihu: List<ZastavkaSpojeNaJihu>?,
        val nacitaSe: Boolean = true,
    )

    lateinit var spoj: Spoj

//    fun detailKurzu() {
//        viewModelScope.launch {
//            _uiEvent.send(
//                UiEvent.Navigovat(
//                    DetailKurzuScreenDestination(
//                        kurz = spoj.nazevKurzu
//                    )
//                )
//            )
//        }
//    }
}