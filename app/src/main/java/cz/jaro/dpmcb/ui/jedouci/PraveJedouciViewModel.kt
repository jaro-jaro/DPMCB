package cz.jaro.dpmcb.ui.jedouci

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cz.jaro.dpmcb.data.DopravaRepository
import cz.jaro.dpmcb.data.SpojeRepository
import cz.jaro.dpmcb.data.helperclasses.NavigateFunction
import cz.jaro.dpmcb.data.helperclasses.Smer
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.combine
import cz.jaro.dpmcb.ui.destinations.SpojDestination
import cz.jaro.dpmcb.ui.jedouci.PraveJedouciViewModel.JedouciSpojADalsiVeci.Companion.jenJedouciSpoje
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.toList
import org.koin.android.annotation.KoinViewModel
import org.koin.core.annotation.InjectedParam
import java.time.LocalDate
import java.time.LocalTime

@KoinViewModel
class PraveJedouciViewModel(
    private val repo: SpojeRepository,
    private val dopravaRepo: DopravaRepository,
    @InjectedParam private val params: Parameters,
) : ViewModel() {

    data class Parameters(
        val filtry: List<Int>,
        val typ: TypPraveJedoucich,
        val navigate: NavigateFunction,
    )

    private val typ = MutableStateFlow(params.typ)
    private val filtry = MutableStateFlow(params.filtry)
    private val nacitaSe = MutableStateFlow(true)

    fun onEvent(e: PraveJedouciEvent) = when (e) {
        is PraveJedouciEvent.ZmenitFiltr -> {
            if (e.cisloLinky in filtry.value) filtry.value -= (e.cisloLinky) else filtry.value += (e.cisloLinky)
        }

        is PraveJedouciEvent.ZmenitTyp -> {
            typ.value = e.typ
        }

        is PraveJedouciEvent.KliklNaSpoj -> {
            params.navigate(SpojDestination(spojId = e.spojId))
        }
    }

    private val vysledek = filtry.map {
        it.ifEmpty { null }
    }
        .combine(dopravaRepo.seznamSpojuKterePraveJedou()) { filtry, spojeNaMape ->
            nacitaSe.value = true
            filtry?.let {
                spojeNaMape
                    .filter {
                        it.linka in filtry
                    }
                    .asSequence()
            } ?: spojeNaMape.asSequence()
        }
        .combine(typ) { spoje, typ ->
            spoje
                .asFlow()
                .mapNotNull { spojNaMape ->
                    repo.spojSeZastavkamiPodleId(spojNaMape.id, LocalDate.now()).let { (spoj, zastavky) ->
                        JedouciSpojADalsiVeci(
                            spojId = spoj.id,
                            pristiZastavkaNazev = zastavky.lastOrNull { it.cas == spojNaMape.pristiZastavka }?.nazev ?: return@mapNotNull null,
                            pristiZastavkaCas = zastavky.lastOrNull { it.cas == spojNaMape.pristiZastavka }?.cas ?: return@mapNotNull null,
                            zpozdeni = spojNaMape.zpozdeniMin ?: return@mapNotNull null,
                            indexNaLince = zastavky.indexOfLast { it.cas == spojNaMape.pristiZastavka },
                            smer = spoj.smer,
                            cisloLinky = spoj.linka - 325_000,
                            cil = zastavky.last().nazev,
                            vuz = spojNaMape.vuz ?: return@mapNotNull null,
                        )
                    }
                }
                .toList()
                .let { it ->
                    when (typ) {
                        TypPraveJedoucich.EvC -> it
                            .sortedWith(
                                compareBy { it.vuz }
                            )
                            .map(JedouciSpojADalsiVeci::toJedouciVuz)
                            .toVysledek()
                        TypPraveJedoucich.Poloha -> it
                            .sortedWith(
                                compareBy<JedouciSpojADalsiVeci> { it.cisloLinky }
                                    .thenBy { it.smer }
                                    .thenBy { it.cil }
                                    .thenBy { it.indexNaLince }
                                    .thenByDescending { it.pristiZastavkaCas }
                            )
                            .groupBy { it.cisloLinky to it.cil }
                            .map { (_, seznam) ->
                                JedouciLinkaVeSmeru(
                                    cisloLinky = seznam.first().cisloLinky,
                                    cilovaZastavka = seznam.first().cil,
                                    spoje = seznam.jenJedouciSpoje()
                                )
                            }
                            .toVysledek()

                        TypPraveJedoucich.Zpozdeni -> it
                            .sortedWith(
                                compareByDescending<JedouciSpojADalsiVeci> { it.zpozdeni }
                                    .thenBy { it.cisloLinky }
                                    .thenBy { it.smer }
                                    .thenBy { it.cil }
                                    .thenBy { it.indexNaLince }
                                    .thenByDescending { it.pristiZastavkaCas }
                            )
                            .map(JedouciSpojADalsiVeci::toJedouciZpozdenySpoj)
                            .toVysledek()
                    }
                }
                .also { nacitaSe.value = false }
        }

    private val cislaLinek = flow {
        emit(repo.cislaLinek(LocalDate.now()))
    }

    val state = combine(repo.datum, cislaLinek, vysledek, nacitaSe, repo.maPristupKJihu, filtry, typ) { datum, cislaLinek, vysledek, nacitaSeSeznam, jeOnline, filtry, typ ->
        if (datum != LocalDate.now()) return@combine PraveJedouciState.NeniDneska

        if (!jeOnline) return@combine PraveJedouciState.Offline

        if (cislaLinek.isEmpty()) return@combine PraveJedouciState.ZadneLinky

        if (vysledek.seznam.isNotEmpty()) return@combine PraveJedouciState.OK(cislaLinek, filtry, typ, vysledek)

        if (nacitaSeSeznam) return@combine PraveJedouciState.Nacitani(cislaLinek, filtry, typ)

        return@combine PraveJedouciState.PraveNicNejede(cislaLinek, filtry, typ)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PraveJedouciState.NacitaniLinek(params.typ))

    data class JedouciSpojADalsiVeci(
        val spojId: String,
        val pristiZastavkaNazev: String,
        val pristiZastavkaCas: LocalTime,
        val zpozdeni: Float,
        val indexNaLince: Int,
        val smer: Smer,
        val cisloLinky: Int,
        val cil: String,
        val vuz: Int,
    ) {
        fun toJedouciZpozdenySpoj() = JedouciZpozdenySpoj(
            spojId = spojId,
            zpozdeni = zpozdeni,
            cisloLinky = cisloLinky,
            cilovaZastavka = cil,
        )
        fun toJedouciVuz() = JedouciVuz(
            spojId = spojId,
            cisloLinky = cisloLinky,
            cilovaZastavka = cil,
            vuz = vuz,
        )
        fun toJedouciSpoj() = JedouciSpoj(
            spojId = spojId,
            pristiZastavkaNazev = pristiZastavkaNazev,
            pristiZastavkaCas = pristiZastavkaCas,
            zpozdeni = zpozdeni,
            vuz = vuz,
        )

        companion object {
            fun List<JedouciSpojADalsiVeci>.jenJedouciSpoje() = map(JedouciSpojADalsiVeci::toJedouciSpoj)
        }
    }
}
