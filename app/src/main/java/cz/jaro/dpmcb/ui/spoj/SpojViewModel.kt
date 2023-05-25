package cz.jaro.dpmcb.ui.spoj

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Accessible
import androidx.compose.material.icons.filled.NotAccessible
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.WheelchairPickup
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cz.jaro.dpmcb.data.DopravaRepository
import cz.jaro.dpmcb.data.SpojeRepository
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.asString
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.plus
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.tedFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.WhileSubscribed
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import org.koin.android.annotation.KoinViewModel
import org.koin.core.annotation.InjectedParam
import java.time.Duration
import java.time.LocalDate
import kotlin.random.Random
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

@KoinViewModel
class SpojViewModel(
    private val repo: SpojeRepository,
    dopravaRepo: DopravaRepository,
    @InjectedParam spojId: String,
) : ViewModel() {

    val info = repo.datum.map { datum ->
        val existuje = repo.existujeSpoj(spojId, datum)
        if (!existuje) return@map SpojInfo.Neexistuje(spojId)
        val (spoj, zastavky, caskody, pevneKody, jedeDnes)
                = repo.spojSeZastavkySpojeNaKterychStaviACaskody(spojId, datum)
        if (!jedeDnes) return@map SpojInfo.Neexistuje(spojId)

        val vyluka = repo.maVyluku(spojId, datum)
        val platnost = repo.platnostLinky(spojId, datum)
        SpojInfo.OK(
            spojId = spojId,
            zastavky = zastavky,
            cisloLinky = spoj.linka,
            nizkopodlaznost = when {
                Random.nextFloat() < .01F -> Icons.Default.ShoppingCart
                spoj.nizkopodlaznost -> Icons.Default.Accessible
                Random.nextFloat() < .33F -> Icons.Default.WheelchairPickup
                else -> Icons.Default.NotAccessible
            } to if (spoj.nizkopodlaznost) "Nízkopodlažní vůz" else "Nenízkopodlažní vůz",
            caskody = caskody.filterNot {
                !it.jede && it.v.start == LocalDate.of(0, 1, 1) && it.v.endInclusive == LocalDate.of(0, 1, 1)
            }.groupBy({ it.jede }, {
                if (it.v.start != it.v.endInclusive) "od ${it.v.start.asString()} do ${it.v.endInclusive.asString()}" else it.v.start.asString()
            }).map { (jede, terminy) ->
                (if (jede) "Jede " else "Nejede ") + terminy.joinToString()
            },
            pevneKody = pevneKody,
            linkaKod = "JŘ linky platí od ${platnost.platnostOd.asString()} do ${platnost.platnostDo.asString()}",
            nazevSpoje = spojId.split("-").let { "${it[1]}/${it[2]}" },
            deeplink = "https://jaro-jaro.github.io/DPMCB/spoj/$spojId",
            vyluka = vyluka,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5.seconds), SpojInfo.Loading)

    val oblibene = repo.oblibene
    val datum = repo.datum
    val pridatOblibeny = repo::pridatOblibeny
    val odebratOblibeny = repo::odebratOblibeny

    val stateZJihu = dopravaRepo.spojPodleId(spojId).map { (spojNaMape, detailSpoje) ->
        SpojStateZJihu(
            zpozdeni = detailSpoje?.realneZpozdeni?.times(60)?.toLong()?.seconds ?: spojNaMape?.delay?.minutes,
            zastavkyNaJihu = detailSpoje?.stations
        )
    }
        .flowOn(Dispatchers.IO)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SpojStateZJihu())

    val projetychUseku = combine(info, stateZJihu, tedFlow, repo.datum) { info, state, ted, datum ->
        when {
            info !is SpojInfo.OK -> 0
            datum > LocalDate.now() -> 0
            datum < LocalDate.now() -> info.zastavky.lastIndex
            // Je na mapě && má detail spoje
            state.zpozdeni != null && state.zastavkyNaJihu != null -> state.zastavkyNaJihu.indexOfLast { it.passed }.coerceAtLeast(0)
            info.zastavky.last().cas < ted -> info.zastavky.lastIndex
            else -> info.zastavky.indexOfLast { it.cas < ted }.takeUnless { it == -1 } ?: 0
        }
    }

    val vyska = combine(info, stateZJihu, tedFlow, projetychUseku) { info, state, ted, projetychUseku ->

        if (info !is SpojInfo.OK) return@combine 0F

        if (projetychUseku == 0) return@combine 0F

        val casOdjezduPosledni = info.zastavky[projetychUseku].cas + (state.zpozdeni ?: 0.minutes)

        val casPrijezduDoPristi = info.zastavky.getOrNull(projetychUseku + 1)?.cas?.plus(state.zpozdeni ?: 0.minutes)

        val dobaJizdy = casPrijezduDoPristi?.let { Duration.between(casOdjezduPosledni, it) } ?: Duration.ofSeconds(Long.MAX_VALUE)

        val ubehlo = Duration.between(casOdjezduPosledni, ted).coerceAtLeast(Duration.ZERO)

//        UtilFunctions.funguj(
//            ted,
//            projetychUseku,
//            casOdjezduPosledni,
//            casPrijezduDoPristi,
//            dobaJizdy,
//            ubehlo,
//            (ubehlo.seconds / dobaJizdy.seconds.toFloat()).coerceAtMost(1F),
//            projetychUseku + (ubehlo.seconds / dobaJizdy.seconds.toFloat()).coerceAtMost(1F)
//        )
        projetychUseku + (ubehlo.seconds / dobaJizdy.seconds.toFloat()).coerceAtMost(1F)
    }
}