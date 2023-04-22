package cz.jaro.dpmcb.ui.jedouci

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cz.jaro.dpmcb.data.App.Companion.dopravaRepo
import cz.jaro.dpmcb.data.App.Companion.repo
import cz.jaro.dpmcb.data.helperclasses.MutateListLambda
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.combine
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onEmpty
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.toList
import kotlin.math.roundToInt

class PraveJedouciViewModel : ViewModel() {

    private val _filtry = MutableStateFlow(emptyList<Int>())
    val filtry = _filtry.asStateFlow()

    private val _nacitaSe = MutableStateFlow(false)
    val nacitaSe = _nacitaSe.asStateFlow()

    fun upravitFiltry(upravit: MutateListLambda<Int>) {
        _filtry.value = buildList {
            addAll(filtry.value)
            apply(upravit)
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val seznam = filtry.map {
        it.ifEmpty { null }
    }
        .combine(dopravaRepo.spojeDPMCBNaMape()) { filtry, spojeNaMape ->
            _nacitaSe.value = true
            filtry?.let {
                spojeNaMape
                    .filter {
                        it.lineNumber?.minus(325_000) in filtry
                    }
                    .map { spojNaMape ->
                        spojNaMape.id
                    }
            } ?: emptyList()
        }
        .flatMapLatest { idcka ->
            idcka
                .map { id ->
                    dopravaRepo.spojPodleId(id)
                }
                .combine {
                    it.asSequence()
                }
                .onEmpty {
                    emit(emptySequence())
                }
        }
        .map { spoje ->
            spoje
                .mapNotNull { (spojNaMape, detailSpoje) ->
                    detailSpoje?.let { spojNaMape?.to(it) }
                }
                .asFlow()
                .mapNotNull { (spojNaMape, detailSpoje) ->
                    repo.spojSeZastavkamiPodleId(spojNaMape.id).let { (spoj, zastavky) ->
                        JedouciSpoj(
                            cisloLinky = spoj.linka - 325_000,
                            spojId = spoj.id,
                            cilovaZastavka = zastavky.last().let { it.nazev to it.cas!! },
                            pristiZastavka = zastavky[detailSpoje.stations.indexOfFirst { !it.passed }].let { it.nazev to it.cas!! },
                            zpozdeni = detailSpoje.realneZpozdeni?.roundToInt() ?: spojNaMape.delay,
                            indexNaLince = detailSpoje.stations.indexOfFirst { !it.passed },
                            smer = spoj.smer
                        )
                    }
                }
                .toList()
                .sortedWith(
                    compareBy<JedouciSpoj> { it.cisloLinky }
                        .thenBy { it.smer }
                        .thenBy { it.cilovaZastavka.first }
                        .thenBy { it.indexNaLince }
                        .thenByDescending { it.pristiZastavka.second }
                )
                .groupBy { it.cisloLinky to it.cilovaZastavka.first }
                .map { Triple(it.key.first, it.key.second, it.value) }
                .also { _nacitaSe.value = false }
        }

    val cislaLinek = flow {
        emit(repo.cislaLinek())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

}