package cz.jaro.dpmcb.ui.kurz

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cz.jaro.dpmcb.data.DopravaRepository
import cz.jaro.dpmcb.data.SpojeRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.WhileSubscribed
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import org.koin.android.annotation.KoinViewModel
import org.koin.core.annotation.InjectedParam
import kotlin.time.Duration.Companion.seconds

@KoinViewModel
class KurzViewModel(
    private val repo: SpojeRepository,
    dopravaRepo: DopravaRepository,
    @InjectedParam private val spojIds: List<String>,
) : ViewModel() {

    private val info: Flow<KurzState> = combine(repo.datum, repo.oblibene, repo.maPristupKJihu) { datum, oblibene, online ->
        val spoje = repo.zobrazitKurz(spojIds, datum)

        KurzState.OK(
            kurzId = "a",
            spoje = spoje.map { (spoj, zastavky) ->
                SpojKurzuState.Offline(
                    spojId = spoj.spojId,
                    zastavky = zastavky,
                    cisloLinky = spoj.linka,
                    nizkopodlaznost = spoj.nizkopodlaznost,
                )
            }
        )
    }

    val state = combine(info, dopravaRepo.seznamSpojuKterePraveJedou()) { info, spojeNaJihu ->
        if (info !is KurzState.OK) info
        else info.copy(
            spoje = info.spoje.map { spoj ->
                val spojNaJihu = spojeNaJihu.find { it.id == spoj.spojId }
                if (spojNaJihu?.zpozdeniMin == null) spoj
                else SpojKurzuState.Online(
                    state = spoj as SpojKurzuState.Offline,
                    zpozdeniMin = spojNaJihu.zpozdeniMin,
                    vuz = spojNaJihu.vuz,
                    potvrzenaNizkopodlaznost = spojNaJihu.nizkopodlaznost
                )
            }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5.seconds), KurzState.Loading)
}