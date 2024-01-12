package cz.jaro.dpmcb.ui.odjezdy

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ramcosta.composedestinations.spec.Direction
import cz.jaro.dpmcb.data.DopravaRepository
import cz.jaro.dpmcb.data.SpojeRepository
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.plus
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.ted
import cz.jaro.dpmcb.data.spojNaMapePodleId
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
import kotlin.math.roundToLong
import kotlin.time.Duration.Companion.minutes
import kotlin.collections.filterNot as remove

@KoinViewModel
class OdjezdyViewModel(
    private val repo: SpojeRepository,
    dopravaRepo: DopravaRepository,
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
                    val spojNaMape = spojeNaMape.spojNaMapePodleId(it.spojId)
                    it to spojNaMape
                }
                .sortedBy { (zast, spojNaMape) ->
                    zast.cas.plusSeconds(spojNaMape?.zpozdeniMin?.times(60)?.roundToLong() ?: 0L)
                }
                .map { (zastavka, spojNaMape) ->

                    val posledniZastavka = zastavka.zastavkySpoje.last { it.cas != null }
                    val aktualniNasledujiciZastavka = spojNaMape?.pristiZastavka?.let { pristiZastavka ->
                        zastavka.zastavkySpoje
                            .filter { it.cas != null }
                            .findLast { it.cas!! == pristiZastavka }
                            ?.let { it.nazev to it.cas!! }
                    }
                    val indexTyhle = zastavka.zastavkySpoje.indexOfFirst { it.indexZastavkyNaLince == zastavka.indexZastavkyNaLince }
                    val posledniIndexTyhle = zastavka.zastavkySpoje.indexOfLast { it.nazev == zastavka.nazev }.let {
                        if (it == indexTyhle) zastavka.zastavkySpoje.lastIndex else it
                    }

                    KartickaState(
                        konecna = posledniZastavka.nazev,
                        cisloLinky = zastavka.linka,
                        cas = zastavka.cas,
                        aktualniNasledujiciZastavka = aktualniNasledujiciZastavka,
                        idSpoje = zastavka.spojId,
                        nizkopodlaznost = zastavka.nizkopodlaznost,
                        potvrzenaNizkopodlaznost = spojNaMape?.nizkopodlaznost,
                        zpozdeni = spojNaMape?.zpozdeniMin,
                        pojedePres = zastavka.zastavkySpoje.map { it.nazev }.filterIndexed { i, _ -> i in (indexTyhle + 1)..posledniIndexTyhle },
                        jedeZa = Duration.between(ted, zastavka.cas + (spojNaMape?.zpozdeniMin?.toDouble() ?: 0.0).minutes),
                        pristiZastavka = zastavka.zastavkySpoje.map { it.nazev }.getOrNull(indexTyhle + 1),
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
                .remove {
                    info.jenOdjezdy && it.pristiZastavka == null
                }
                .also { filtrovanejSeznam ->
                    if (minulyState == null) return@also
                    if (minulyState.cas == info.cas && minulyState.jenOdjezdy == info.jenOdjezdy && minulyState.filtrZastavky == info.filtrZastavky && minulyState.filtrLinky == info.filtrLinky) return@also
                    println("scrolllllllllllll")
                    if (filtrovanejSeznam.isEmpty()) return@also
                    viewModelScope.launch(Dispatchers.Main) {
                        scrollovat(filtrovanejSeznam.domov(info))
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

    fun onEvent(e: OdjezdyEvent) = when (e) {
        is OdjezdyEvent.KliklNaSpoj -> {
            navigovat(
                SpojDestination(
                    e.spoj.idSpoje
                )
            )
        }

        is OdjezdyEvent.KliklNaZjr -> {
            e.spoj.pristiZastavka?.let {
                navigovat(
                    JizdniRadyDestination(
                        cisloLinky = e.spoj.cisloLinky,
                        zastavka = params.zastavka,
                        pristiZastavka = e.spoj.pristiZastavka,
                    )
                )
            }
        }

        is OdjezdyEvent.ZmenitCas -> {
            viewModelScope.launch(Dispatchers.Main) {
                _info.update { oldState ->
                    oldState.copy(
                        cas = e.cas,
                    )
                }
            }
            Unit
        }

        is OdjezdyEvent.Scrolluje -> {
            viewModelScope.launch(Dispatchers.Main) {
                _info.update {
                    it.copy(
                        indexScrollovani = e.i
                    )
                }
            }
            Unit
        }

        is OdjezdyEvent.Vybral -> {
            viewModelScope.launch(Dispatchers.Main) {
                _info.update { oldState ->
                    when (e.vysledek.typVybiratoru) {
                        TypVybiratoru.LINKA_ZPET -> oldState.copy(filtrLinky = e.vysledek.value.toInt())
                        TypVybiratoru.ZASTAVKA_ZPET -> oldState.copy(filtrZastavky = e.vysledek.value)
                        else -> return@launch
                    }
                }
            }
            Unit
        }

        is OdjezdyEvent.Zrusil -> {
            viewModelScope.launch(Dispatchers.IO) {
                _info.update { oldState ->
                    when (e.typVybiratoru) {
                        TypVybiratoru.LINKA_ZPET -> oldState.copy(filtrLinky = null)
                        TypVybiratoru.ZASTAVKA_ZPET -> oldState.copy(filtrZastavky = null)
                        else -> return@launch
                    }
                }
            }
            Unit
        }

        OdjezdyEvent.ZmenilKompaktniRezim -> {
            _info.update {
                it.copy(
                    kompaktniRezim = !it.kompaktniRezim
                )
            }
        }

        OdjezdyEvent.ZmenilJenOdjezdy -> {
            _info.update {
                it.copy(
                    jenOdjezdy = !it.jenOdjezdy
                )
            }
        }

        OdjezdyEvent.Scrollovat -> {
            viewModelScope.launch(Dispatchers.Main) {
                if (state.value !is OdjezdyState.Jede) return@launch
                scrollovat((state.value as OdjezdyState.Jede).seznam.domov(_info.value))
            }
            Unit
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
                    seznam.domov(OdjezdyInfo(cas = params.cas))
                )
            }
        }
    }
}
