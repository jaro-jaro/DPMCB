package cz.jaro.dpmcb.ui.zjr

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cz.jaro.dpmcb.data.SpojeRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.WhileSubscribed
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.koin.android.annotation.KoinViewModel
import org.koin.core.annotation.InjectedParam
import kotlin.time.Duration.Companion.seconds

@KoinViewModel
class JizdniRadyViewModel(
    private val repo: SpojeRepository,
    @InjectedParam params: Parameters,
) : ViewModel() {

    data class Parameters(
        val cisloLinky: Int,
        val zastavka: String,
        val pristiZastavka: String,
    )

    val state = repo.datum.map {
        JizdniRadyState.Success(repo.zastavkyJedouciVDatumSPristiZastavkou(params.cisloLinky, params.zastavka, params.pristiZastavka, it))
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = JizdniRadyState.Loading
    )

    val zobrazitNizkopodlaznostZMinule = repo.zobrazitNizkopodlaznost
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5.seconds), false)
    val nstaveni = repo.nastaveni
    fun upravitZobrazeniNizkopodlaznosti(value: Boolean) {
        viewModelScope.launch {
            repo.zmenitNizkopodlaznost(value)
        }
    }
}
