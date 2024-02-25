package cz.jaro.dpmcb.ui.kurz

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cz.jaro.dpmcb.data.DopravaRepository
import cz.jaro.dpmcb.data.SpojeRepository
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.asString
import cz.jaro.dpmcb.data.zcitelnitPevneKody
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.WhileSubscribed
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import org.koin.android.annotation.KoinViewModel
import org.koin.core.annotation.InjectedParam
import java.time.LocalDate
import kotlin.time.Duration.Companion.seconds

@KoinViewModel
class KurzViewModel(
    private val repo: SpojeRepository,
    dopravaRepo: DopravaRepository,
    @InjectedParam private val puvodniKurz: String,
) : ViewModel() {

    private val info: Flow<KurzState> = combine(repo.datum, repo.oblibene, repo.maPristupKJihu) { datum, oblibene, online ->
        val (kurz, predtim, spoje, potom, caskody, pevneKody) = (
            repo.zobrazitKurz(puvodniKurz, datum) ?: return@combine KurzState.Neexistuje(puvodniKurz)
        )

        KurzState.OK.Offline(
            kurz = kurz,
            caskody = caskody.filterNot {
                !it.jede && it.v.start == LocalDate.of(0, 1, 1) && it.v.endInclusive == LocalDate.of(0, 1, 1)
            }.groupBy({ it.jede }, {
                if (it.v.start != it.v.endInclusive) "od ${it.v.start.asString()} do ${it.v.endInclusive.asString()}" else it.v.start.asString()
            }).map { (jede, terminy) ->
                (if (jede) "Jede " else "Nejede ") + terminy.joinToString()
            },
            pevneKody = zcitelnitPevneKody(pevneKody),
            navaznostiPredtim = predtim,
            navaznostiPotom = potom,
            spoje = spoje.map { (spoj, zastavky) ->
                SpojKurzuState(
                    spojId = spoj.spojId,
                    zastavky = zastavky,
                    cisloLinky = spoj.linka,
                    nizkopodlaznost = spoj.nizkopodlaznost,
                    jede = false,
                )
            },
            jedeDnes = repo.jedeV(caskody = caskody, pevneKody = pevneKody, datum = LocalDate.now()),
        )
    }

    val state = combine(info, dopravaRepo.seznamSpojuKterePraveJedou()) { info, spojeNaJihu ->
        if (info !is KurzState.OK) return@combine info
        val spojNaJihu = spojeNaJihu.find { onlineSpoj -> onlineSpoj.id in info.spoje.map { it.spojId } }
        if (spojNaJihu?.zpozdeniMin == null) return@combine info
        KurzState.OK.Online(
            state = info,
            zpozdeniMin = spojNaJihu.zpozdeniMin,
            vuz = spojNaJihu.vuz,
            potvrzenaNizkopodlaznost = spojNaJihu.nizkopodlaznost
        ).copy(
            spoje = info.spoje.map {
                if (it.spojId != spojNaJihu.id) it else it.copy(
                    jede = true
                )
            }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5.seconds), KurzState.Loading)
}