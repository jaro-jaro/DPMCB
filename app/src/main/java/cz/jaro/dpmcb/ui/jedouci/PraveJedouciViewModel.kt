package cz.jaro.dpmcb.ui.jedouci

import androidx.lifecycle.ViewModel
import cz.jaro.dpmcb.data.App.Companion.dopravaRepo
import cz.jaro.dpmcb.data.App.Companion.repo
import cz.jaro.dpmcb.data.DopravaRepository.Companion.upravit
import cz.jaro.dpmcb.data.helperclasses.Cas
import cz.jaro.dpmcb.data.helperclasses.Smer
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.reversedIf
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onEmpty
import kotlinx.coroutines.flow.toList

class PraveJedouciViewModel : ViewModel() {

    private val _filtry = MutableStateFlow(emptyList<Int>())
    val filtry = _filtry.asStateFlow()

    private val _nacitaSe = MutableStateFlow(false)
    val nacitaSe = _nacitaSe.asStateFlow()

    fun upravitFiltry(upravit: MutableList<Int>.() -> Unit) {
        _filtry.value = buildList {
            addAll(filtry.value)
            apply(upravit)
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val seznam = dopravaRepo.spojeNaMape()
        .combine(filtry) { spojeNaMape, filtry ->
            _nacitaSe.value = true
            spojeNaMape
                .filter {
                    it.lineNumber?.minus(325_000) in filtry
                }
                .map { spojNaMape ->
                    spojNaMape.id
                }
        }
        .map { idcka ->
            idcka.map { id ->
                dopravaRepo.spojPodleId(id)
            }
        }
        .flatMapLatest { seznamSpojFlowuu ->
            combine(seznamSpojFlowuu) {
                it.asSequence()
            }.onEmpty {
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
                    repo.spojSeZastavkamiPodleJihu(spojNaMape = spojNaMape)?.let { (spoj, zastavky) ->
                        repo.idSpoju = repo.idSpoju + (spoj.id to spojNaMape.id)
                        JedouciSpoj(
                            cisloLinky = spoj.cisloLinky,
                            spojId = spoj.id,
                            cilovaZastavka = zastavky
                                .reversedIf { spoj.smer == Smer.NEGATIVNI }
                                .last { it.cas != Cas.nikdy }
                                .let { it.nazevZastavky to it.cas },
                            pristiZastavka = zastavky
                                .find { zastavka ->
                                    zastavka.nazevZastavky.upravit() == detailSpoje.stations.find { !it.passed }!!.name.upravit()
                                            && zastavka.cas.toString() == detailSpoje.stations.find { !it.passed }!!.departureTime
                                }
                                ?.let { it.nazevZastavky to it.cas }
                                ?: return@mapNotNull null,
                            zpozdeni = spojNaMape.delay,
                            indexNaLince = zastavky
                                .sortedBy { it.indexNaLince }
                                .reversedIf { spoj.smer == Smer.NEGATIVNI }
                                .indexOfFirst { zastavka ->
                                    zastavka.nazevZastavky.upravit() == detailSpoje.stations.find { !it.passed }!!.name.upravit()
                                            && zastavka.cas.toString() == detailSpoje.stations.find { !it.passed }!!.departureTime
                                }
                                .takeUnless { it == -1 }
                                ?: return@mapNotNull null,
                            smer = spoj.smer
                        )
                    }
                }
                .toList()
                .sortedWith(
                    compareBy<JedouciSpoj> { it.cisloLinky }
                        .thenBy { it.smer }
                        .thenBy { it.indexNaLince }
                        .thenBy { it.pristiZastavka.second }
                )
                .groupBy { it.cisloLinky to it.cilovaZastavka.first }
                .map { Triple(it.key.first, it.key.second, it.value) }
                .also { _nacitaSe.value = false }
        }

    val cislaLinek = repo.cislaLinek

    data class JedouciSpoj(
        val cisloLinky: Int,
        val spojId: Long,
        val cilovaZastavka: Pair<String, Cas>,
        val pristiZastavka: Pair<String, Cas>,
        val zpozdeni: Int,
        val indexNaLince: Int,
        val smer: Smer,
    )
}