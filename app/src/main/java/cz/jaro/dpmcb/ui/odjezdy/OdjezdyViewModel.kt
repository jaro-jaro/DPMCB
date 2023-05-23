package cz.jaro.dpmcb.ui.odjezdy

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ramcosta.composedestinations.spec.Direction
import cz.jaro.dpmcb.data.DopravaRepository
import cz.jaro.dpmcb.data.SpojeRepository
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.plus
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.ted
import cz.jaro.dpmcb.data.helperclasses.Vysledek
import cz.jaro.dpmcb.ui.destinations.JizdniRadyDestination
import cz.jaro.dpmcb.ui.destinations.SpojDestination
import cz.jaro.dpmcb.ui.vybirator.TypVybiratoru
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.runningFold
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.android.annotation.KoinViewModel
import org.koin.core.annotation.InjectedParam
import java.time.Duration
import java.time.LocalTime
import kotlin.time.Duration.Companion.minutes

@KoinViewModel
class OdjezdyViewModel(
    private val repo: SpojeRepository,
    private val dopravaRepo: DopravaRepository,
    @InjectedParam private val params: Parameters,
) : ViewModel() {

    data class Parameters(
        val zastavka: String,
        val cas: LocalTime,
        val linka: Int?,
        val pres: String?,
    )

    lateinit var scrollovat: suspend (Int) -> Unit
    lateinit var navigovat: (Direction) -> Unit

    private val _state = MutableStateFlow(OdjezdyState(cas = params.cas, filtrLinky = params.linka, filtrZastavky = params.pres))
    val state = _state.asStateFlow()

    val maPristupKJihu = repo.maPristupKJihu

    val seznam = repo.datum
        .combine(dopravaRepo.seznamSpojuKterePraveJedou()) { datum, spojeNaMape ->
            repo.spojeJedouciVdatumZastavujiciNaIndexechZastavkySeZastavkySpoje(datum, params.zastavka)
                .map {
                    val spojNaMape = with(dopravaRepo) { spojeNaMape.spojDPMCBPodleId(it.spojId) }
                    it to spojNaMape
                }
                .sortedBy { (zast, spojNaMape) ->
                    zast.cas.plusMinutes(spojNaMape?.delay?.toLong() ?: 0L)
                }
                .map { (zastavka, spojNaMape) ->

                    val posledniZastavka = zastavka.zastavkySpoje.last { it.cas != null }
                    val aktualniNasledujiciZastavka = spojNaMape?.delay?.let { zpozdeni ->
                        zastavka.zastavkySpoje
                            .filter { it.cas != null }
                            .find { it.cas!!.plusMinutes(zpozdeni.toLong()) > ted }
                            ?.let { it.nazev to it.cas!! }
                    }
                    val indexTyhle = zastavka.zastavkySpoje.indexOfFirst { it.indexZastavkyNaLince == zastavka.indexZastavkyNaLince }

                    KartickaState(
                        konecna = posledniZastavka.nazev,
                        cisloLinky = zastavka.linka,
                        cas = zastavka.cas,
                        aktualniNasledujiciZastavka = aktualniNasledujiciZastavka,
                        idSpoje = zastavka.spojId,
                        nizkopodlaznost = zastavka.nizkopodlaznost,
                        zpozdeni = spojNaMape?.delay,
                        pojedePres = zastavka.zastavkySpoje.map { it.nazev }.filterIndexed { i, _ -> i > indexTyhle },
                        jedeZa = spojNaMape?.delay?.let { Duration.between(ted, zastavka.cas + it.minutes) },
                        pristiZastavka = zastavka.zastavkySpoje.map { it.nazev }.getOrNull(indexTyhle + 1)
                    )
                }
        }
        .flowOn(Dispatchers.IO)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val filtrovanejSeznam = _state
        .runningFold(null as Pair<OdjezdyState?, OdjezdyState>?) { minuly, novy ->
            minuly?.second to novy
        }
        .filterNotNull()
        .combine(seznam) { (minulyState, state), seznam ->
            seznam
                ?.filter {
                    state.filtrLinky?.let { filtr -> it.cisloLinky == filtr } ?: true
                }
                ?.filter {
                    state.filtrZastavky?.let { filtr -> it.pojedePres.contains(filtr) } ?: true
                }
                ?.also { filtrovanejSeznam ->
                    if (minulyState == null) return@also
                    if (minulyState.cas == state.cas && minulyState.filtrZastavky == state.filtrZastavky && minulyState.filtrLinky == state.filtrLinky) return@also
                    if (filtrovanejSeznam.isEmpty()) return@also
                    viewModelScope.launch(Dispatchers.Main) {
                        scrollovat(
                            filtrovanejSeznam.withIndex().firstOrNull { (_, zast) ->
                                zast.cas >= state.cas
                            }?.index ?: filtrovanejSeznam.lastIndex
                        )
                    }
                }
        }
        .flowOn(Dispatchers.IO)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun kliklNaSpoj(spoj: KartickaState) {
        navigovat(
            SpojDestination(
                spoj.idSpoje
            )
        )
    }

    fun kliklNaZjr(spoj: KartickaState) {
        spoj.pristiZastavka?.let {
            navigovat(
                JizdniRadyDestination(
                    cisloLinky = spoj.cisloLinky,
                    zastavka = params.zastavka,
                    pristiZastavka = spoj.pristiZastavka,
                )
            )
        }
    }

    fun zmenitCas(cas: LocalTime) {
        viewModelScope.launch(Dispatchers.Main) {
            _state.update { oldState ->
                oldState.copy(
                    cas = cas,
                )
            }
        }
    }

    fun scrolluje(i: Int) {
        viewModelScope.launch(Dispatchers.Main) {
            _state.update {
                it.copy(
                    indexScrollovani = i
                )
            }
        }
    }

    fun vybral(vysledek: Vysledek) {
        viewModelScope.launch(Dispatchers.Main) {
            _state.update { oldState ->
                when (vysledek.typVybiratoru) {
                    TypVybiratoru.LINKA_ZPET -> oldState.copy(filtrLinky = vysledek.value.toInt())
                    TypVybiratoru.ZASTAVKA_ZPET -> oldState.copy(filtrZastavky = vysledek.value)
                    else -> return@launch
                }
            }
        }
    }

    fun zrusil(typVybiratoru: TypVybiratoru) {
        viewModelScope.launch(Dispatchers.IO) {
            _state.update { oldState ->
                when (typVybiratoru) {
                    TypVybiratoru.LINKA_ZPET -> oldState.copy(filtrLinky = null)
                    TypVybiratoru.ZASTAVKA_ZPET -> oldState.copy(filtrZastavky = null)
                    else -> return@launch
                }
            }
        }
    }

    fun zmenilKompaktniRezim() {
        _state.update {
            it.copy(
                kompaktniRezim = !it.kompaktniRezim
            )
        }
    }

    init {
        viewModelScope.launch(Dispatchers.IO) {
            while (filtrovanejSeznam.value.isNullOrEmpty()) Unit
            while (!::scrollovat.isInitialized) Unit
            withContext(Dispatchers.Main) {
                scrollovat(
                    filtrovanejSeznam.value!!.withIndex().firstOrNull { (_, zast) ->
                        zast.cas >= params.cas
                    }?.index ?: seznam.value!!.lastIndex
                )
            }
        }
    }
}
