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
import cz.jaro.dpmcb.data.helperclasses.CastSpoje
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.asString
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.plus
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.tedFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.WhileSubscribed
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.koin.android.annotation.KoinViewModel
import org.koin.core.annotation.InjectedParam
import java.time.Duration
import java.time.LocalDate
import kotlin.math.roundToInt
import kotlin.random.Random
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

@KoinViewModel
class SpojViewModel(
    private val repo: SpojeRepository,
    dopravaRepo: DopravaRepository,
    @InjectedParam private val spojId: String,
) : ViewModel() {

    private val info: Flow<SpojState> = combine(repo.datum, repo.oblibene, repo.maPristupKJihu) { datum, oblibene, online ->
        val existuje = repo.existujeSpoj(spojId)
        if (!existuje) return@combine SpojState.Neexistuje(spojId)
        val jedeV = repo.spojJedeV(spojId)
        if (!jedeV(datum)) {
            return@combine SpojState.Nejede(
                spojId = spojId,
                datum = datum,
                pristeJedePoDnesku = List(365) { LocalDate.now().plusDays(it.toLong()) }.firstOrNull { jedeV(it) },
                pristeJedePoDatu = List(365) { datum.plusDays(it.toLong()) }.firstOrNull { jedeV(it) }
            )
        }

        val (spoj, zastavky, caskody, pevneKody) = repo.spojSeZastavkySpojeNaKterychStaviACaskody(spojId, datum)
        val vyluka = repo.maVyluku(spojId, datum)
        val platnost = repo.platnostLinky(spojId, datum)
        SpojState.OK.Offline(
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
            oblibeny = oblibene.find { it.spojId == spojId },
            vyska = 0F,
            projetychUseku = 0,
            online = online,
        )
    }

    fun odebratOblibeny() {
        viewModelScope.launch {
            repo.odebratOblibeny(spojId)
        }
    }

    fun upravitOblibeny(cast: CastSpoje) {
        viewModelScope.launch {
            repo.upravitOblibeny(cast)
        }
    }

    fun zmenitdatum(datum: LocalDate) {
        viewModelScope.launch {
            repo.upravitDatum(datum)
        }
    }

    private val stateZJihu = dopravaRepo.spojPodleId(spojId).map { (spojNaMape, detailSpoje) ->
        SpojStateZJihu(
            zpozdeni = spojNaMape?.delay?.minutes,
            zastavkyNaJihu = detailSpoje?.stations
        )
    }
        .flowOn(Dispatchers.IO)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SpojStateZJihu())

    private val projetychUseku = combine(info, stateZJihu, tedFlow, repo.datum) { info, state, ted, datum ->
        when {
            info !is SpojState.OK -> 0
            datum > LocalDate.now() -> 0
            datum < LocalDate.now() -> info.zastavky.lastIndex
            // Je na mapě && má detail spoje
            state.zpozdeni != null && state.zastavkyNaJihu != null -> state.zastavkyNaJihu.indexOfLast { it.passed }.coerceAtLeast(0)
            info.zastavky.last().cas < ted -> info.zastavky.lastIndex
            else -> info.zastavky.indexOfLast { it.cas < ted }.takeUnless { it == -1 } ?: 0
        }
    }

    private val vyska = combine(info, stateZJihu, tedFlow, projetychUseku) { info, state, ted, projetychUseku ->

        if (info !is SpojState.OK) return@combine 0F

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

    val state = combine(info, projetychUseku, vyska, stateZJihu) { info, projetychUseku, vyska, stateZJihu ->
        if (info !is SpojState.OK) info
        else (info as SpojState.OK.Offline).copy(
            vyska = vyska,
            projetychUseku = projetychUseku
        ).let { state ->
            if (stateZJihu.zpozdeni == null || stateZJihu.zastavkyNaJihu == null) state
            else SpojState.OK.Online(
                state = state,
                zpozdeni = stateZJihu.zpozdeni.inWholeSeconds.div(60F).roundToInt(),
                zastavkyNaJihu = stateZJihu.zastavkyNaJihu
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5.seconds), SpojState.Loading)
}