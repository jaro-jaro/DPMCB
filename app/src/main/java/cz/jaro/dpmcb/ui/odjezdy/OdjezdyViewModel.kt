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

    private val _info = MutableStateFlow(OdjezdyInfo(cas = params.cas, filtrLinky = params.linka, filtrZastavky = params.pres))
    val info = _info.asStateFlow()

    val maPristupKJihu = repo.maPristupKJihu

    private val seznam = repo.datum
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

    val state = _info
        .runningFold(null as Pair<OdjezdyInfo?, OdjezdyInfo>?) { minuly, novy ->
            minuly?.second to novy
        }
        .filterNotNull()
        .combine(seznam) { (minulyState, info), seznam ->
            val filtrovanejSeznam = seznam
                .filter {
                    info.filtrLinky?.let { filtr -> it.cisloLinky == filtr } ?: true
                }
                .filter {
                    info.filtrZastavky?.let { filtr -> it.pojedePres.contains(filtr) } ?: true
                }
                .also { filtrovanejSeznam ->
                    if (minulyState == null) return@also
                    if (minulyState.cas == info.cas && minulyState.filtrZastavky == info.filtrZastavky && minulyState.filtrLinky == info.filtrLinky) return@also
                    if (filtrovanejSeznam.isEmpty()) return@also
                    viewModelScope.launch(Dispatchers.Main) {
                        scrollovat(
                            filtrovanejSeznam.withIndex().firstOrNull { (_, zast) ->
                                zast.cas >= info.cas
                            }?.index ?: filtrovanejSeznam.lastIndex
                        )
                    }
                }

            if (filtrovanejSeznam.isEmpty()) {
                if (info.filtrLinky == null && info.filtrZastavky == null) OdjezdyState.VubecNicNejede
                else if (info.filtrLinky == null) OdjezdyState.SemNicNejede
                else if (info.filtrZastavky == null) OdjezdyState.LinkaNejede
                else OdjezdyState.LinkaSemNejede
            } else OdjezdyState.Jede(
                seznam = filtrovanejSeznam
            )
        }
        .flowOn(Dispatchers.IO)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), OdjezdyState.Loading)

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
            _info.update { oldState ->
                oldState.copy(
                    cas = cas,
                )
            }
        }
    }

    fun scrolluje(i: Int) {
        viewModelScope.launch(Dispatchers.Main) {
            _info.update {
                it.copy(
                    indexScrollovani = i
                )
            }
        }
    }

    fun vybral(vysledek: Vysledek) {
        viewModelScope.launch(Dispatchers.Main) {
            _info.update { oldState ->
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
            _info.update { oldState ->
                when (typVybiratoru) {
                    TypVybiratoru.LINKA_ZPET -> oldState.copy(filtrLinky = null)
                    TypVybiratoru.ZASTAVKA_ZPET -> oldState.copy(filtrZastavky = null)
                    else -> return@launch
                }
            }
        }
    }

    fun zmenilKompaktniRezim() {
        _info.update {
            it.copy(
                kompaktniRezim = !it.kompaktniRezim
            )
        }
    }

    init {
        viewModelScope.launch(Dispatchers.IO) {
            while (state.value == OdjezdyState.Loading) Unit
            if (state.value !is OdjezdyState.Jede) return@launch
            while (!::scrollovat.isInitialized) Unit
            withContext(Dispatchers.Main) {
                val seznam = (state.value as OdjezdyState.Jede).seznam
                scrollovat(
                    seznam.withIndex().firstOrNull { (_, zast) ->
                        zast.cas >= params.cas
                    }?.index ?: seznam.lastIndex
                )
            }
        }
    }
}
