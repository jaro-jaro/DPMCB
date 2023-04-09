package cz.jaro.dpmcb.ui.spoj

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Accessible
import androidx.compose.material.icons.filled.NotAccessible
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.WheelchairPickup
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cz.jaro.dpmcb.data.App
import cz.jaro.dpmcb.data.App.Companion.repo
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.funguj
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.tedFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.LocalDate
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
                    !it.jede && it.v.start == LocalDate.of(0, 1, 1) && it.v.endInclusive == LocalDate.of(0, 1, 1)
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

    val projetychUseku = combine(info, stateZJihu, tedFlow, repo.datum) { info, state, ted, datum ->
        when {
            info == null -> 0
            datum > LocalDate.now() -> 0
            datum < LocalDate.now() -> info.zastavky.lastIndex
            state.zastavkyNaJihu != null -> state.zastavkyNaJihu.indexOfLast { it.passed }.coerceAtLeast(0)
            info.zastavky.last().cas < ted -> info.zastavky.lastIndex
            else -> info.zastavky.indexOfLast { it.cas < ted }.takeUnless { it == -1 } ?: 0
        }.funguj()
    }

    val vyska = combine(info, stateZJihu, tedFlow, projetychUseku) { info, state, ted, projetychUseku ->

        if (projetychUseku == 0 || info == null) return@combine 0F

        val casOdjezduPosledni = info.zastavky[projetychUseku].cas.plusMinutes(state.zpozdeni?.toLong() ?: 0L)

        val casPrijezduDoPristi = info.zastavky.getOrNull(projetychUseku + 1)?.cas?.plusMinutes(state.zpozdeni?.toLong() ?: 0L)

        val dobaJizdy = casPrijezduDoPristi?.let { Duration.between(it, casOdjezduPosledni) } ?: Duration.ofSeconds(Long.MAX_VALUE)

        val ubehlo = Duration.between(ted, casOdjezduPosledni).coerceAtLeast(Duration.ZERO)

        UtilFunctions.funguj(
            ted,
            projetychUseku,
            casOdjezduPosledni,
            casPrijezduDoPristi,
            dobaJizdy,
            ubehlo,
            (ubehlo.seconds / dobaJizdy.seconds).toFloat().coerceAtMost(1F),
            projetychUseku + (ubehlo.seconds / dobaJizdy.seconds).toFloat().coerceAtMost(1F)
        )
        projetychUseku + (ubehlo.seconds / dobaJizdy.seconds).toFloat().coerceAtMost(1F)
    }
}