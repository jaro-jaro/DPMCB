package cz.jaro.dpmcb.ui.spoj

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Accessible
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
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.funguj
import cz.jaro.dpmcb.data.naJihu.ZastavkaSpojeNaJihu
import cz.jaro.dpmcb.data.realtions.CasNazevSpojId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.random.Random

class DetailSpojeViewModel(
    spojId: String,
) : ViewModel() {

    private val _info = MutableStateFlow(null as DetailSpojeInfo?)
    val info = _info.asStateFlow()

    init {
        viewModelScope.launch {
            val (spoj, zastavky, caskody, pevneKody) = repo.spojSeZastavkySpojeNaKterychStaviACaskody(spojId)
            _info.value = DetailSpojeInfo(
                spojId = spojId,
                zastavky = zastavky,
                cisloLinky = spoj.linka,
                nizkopodlaznost = when {
                    Random.nextFloat() < .01F -> Icons.Default.ShoppingCart
                    spoj.nizkopodlaznost -> Icons.Default.Accessible
                    Random.nextFloat() < .33F -> Icons.Default.WheelchairPickup
                    else -> Icons.Default.NotAccessible
                },
                caskody = caskody.filterNot {
                    !it.jede && it.v.start == Datum(0, 0, 0) && it.v.endInclusive == Datum(0, 0, 0)
                }.groupBy({ it.jede }, {
                    if (it.v.start != it.v.endInclusive) "od ${it.v.start} do ${it.v.endInclusive}" else "${it.v.start}"
                }).map { (jede, terminy) ->
                    (if (jede) "Jede " else "Nejede ") + terminy.joinToString()
                },
                pevneKody = pevneKody,
                nazevSpoje = spojId.split("-").let { "${it[1]}/${it[2]}" },
                deeplink = "https://jaro-jaro.github.io/DPMCB/spoj/$spojId",
            )
        }
    }

    val stateZJihu = App.dopravaRepo.spojPodleId(spojId).map { (spojNaMape, detailSpoje) ->
        DetailSpojeStateZJihu(
            zpozdeni = spojNaMape?.delay,
            zastavkyNaJihu = detailSpoje?.stations
        )
    }
        .flowOn(Dispatchers.IO)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DetailSpojeStateZJihu())

    val projetychUseku = combine(info, stateZJihu, Cas.tedFlow, repo.datum) { info, state, ted, datum ->
        when {
            info == null -> 0
            datum > Datum.dnes -> 0
            datum < Datum.dnes -> info.zastavky.lastIndex
            state.zastavkyNaJihu != null -> state.zastavkyNaJihu.indexOfLast { it.passed }.coerceAtLeast(0)
            info.zastavky.last().cas < ted -> info.zastavky.lastIndex
            else -> info.zastavky.indexOfLast { it.cas < ted }.takeUnless { it == -1 } ?: 0
        }.funguj()
    }

    val vyska = combine(info, stateZJihu, Cas.tedFlow, projetychUseku) { info, state, ted, projetychUseku ->

        if (projetychUseku == 0 || info == null) return@combine 0F

        val casOdjezduPosledni = info.zastavky[projetychUseku].cas.plus(state.zpozdeni?.min ?: Trvani.zadne)

        val casPrijezduDoPristi = info.zastavky.getOrNull(projetychUseku + 1)?.cas?.plus(state.zpozdeni?.min ?: Trvani.zadne)

        val dobaJizdy = casPrijezduDoPristi?.minus(casOdjezduPosledni) ?: Trvani.nekonecne

        val ubehlo = ted.minus(casOdjezduPosledni).coerceAtLeast(0.sek)

        UtilFunctions.funguj(
            ted,
            projetychUseku,
            casOdjezduPosledni,
            casPrijezduDoPristi,
            dobaJizdy,
            ubehlo,
            (ubehlo / dobaJizdy).toFloat().coerceAtMost(1F),
            projetychUseku + (ubehlo / dobaJizdy).toFloat().coerceAtMost(1F)
        )
        projetychUseku + (ubehlo / dobaJizdy).toFloat().coerceAtMost(1F)
    }

    data class DetailSpojeInfo(
        val spojId: String,
        val zastavky: List<CasNazevSpojId>,
        val cisloLinky: Int,
        val nizkopodlaznost: ImageVector,
        val caskody: List<String>,
        val pevneKody: List<String>,
        val nazevSpoje: String,
        val deeplink: String,
    )

    data class DetailSpojeStateZJihu(
        val zpozdeni: Int? = null,
        val zastavkyNaJihu: List<ZastavkaSpojeNaJihu>? = null,
    )
}