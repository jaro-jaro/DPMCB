package cz.jaro.dpmcb.ui.zjr

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cz.jaro.dpmcb.data.DopravaRepository
import cz.jaro.dpmcb.data.SpojeRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.WhileSubscribed
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.koin.android.annotation.KoinViewModel
import org.koin.core.annotation.InjectedParam
import kotlin.time.Duration.Companion.seconds

@KoinViewModel
class JizdniRadyViewModel(
    private val repo: SpojeRepository,
    private val dopravaRepo: DopravaRepository,
    @InjectedParam params: Parameters,
) : ViewModel() {

    data class Parameters(
        val cisloLinky: Int,
        val zastavka: String,
        val pristiZastavka: String,
    )

    val state = repo.datum.map { datum ->
        JizdniRadyState.Success(
            repo.zastavkyJedouciVDatumSPristiZastavkou(params.cisloLinky, params.zastavka, params.pristiZastavka, datum)
        )
    }.combine(dopravaRepo.seznamSpojuKterePraveJedou()) { jr, spojeNaMape ->
        jr.copy(
            data = jr.data.map { spoj ->
                spoj.copy(
                    zpozdeni = spojeNaMape.find { it.id == spoj.spojId }?.zpozdeniMin
                )
            }
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = JizdniRadyState.Loading
    )

    val zobrazitNizkopodlaznostZMinule = repo.zobrazitNizkopodlaznost
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5.seconds), false)

    fun upravitZobrazeniNizkopodlaznosti(value: Boolean) {
        viewModelScope.launch {
            repo.zmenitNizkopodlaznost(value)
        }
    }
}
