package cz.jaro.dpmcb.ui.zjr

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cz.jaro.dpmcb.data.SpojeRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class JizdniRadyViewModel(
    private val repo: SpojeRepository,
    private val cisloLinky: Int,
    private val zastavka: String,
    private val pristiZastavka: String,
) : ViewModel() {

    val state = repo.datum.map {
        JizdniRadyState.Success(repo.zastavkyJedouciVDatumSPristiZastavkou(cisloLinky, zastavka, pristiZastavka, it))
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = JizdniRadyState.Loading
    )

    val zobrazitNizkopodlaznostZMinule = repo.zobrazitNizkopodlaznost
    val nstaveni = repo.nastaveni
    val upravitZobrazeniNizkopodlaznosti = repo::zmenitNizkopodlaznost
}
