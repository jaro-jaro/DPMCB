package cz.jaro.dpmcb.ui.jedouci

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cz.jaro.dpmcb.data.DopravaRepository
import cz.jaro.dpmcb.data.SpojeRepository
import cz.jaro.dpmcb.data.helperclasses.NavigateFunction
import cz.jaro.dpmcb.data.helperclasses.Smer
import cz.jaro.dpmcb.ui.destinations.KurzDestination
import cz.jaro.dpmcb.ui.destinations.SpojDestination
import cz.jaro.dpmcb.ui.jedouci.PraveJedouciViewModel.JedouciSpojADalsiVeci.Companion.jenJedouciSpoje
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.WhileSubscribed
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import org.koin.android.annotation.KoinViewModel
import org.koin.core.annotation.InjectedParam
import java.time.LocalDate
import java.time.LocalTime
import kotlin.time.Duration.Companion.seconds
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.combine as kombajn

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

        is PraveJedouciEvent.KliklNaKurz -> {
            params.navigate(KurzDestination(kurz = e.kurz))
        }
    }

    private val praveJedouci = combine(filtry, dopravaRepo.seznamSpojuKterePraveJedou(), typ) { filtry, spojeNaMape, typ ->
        nacitaSe.value = true
        spojeNaMape
            .map { spojNaMape ->
                viewModelScope.async {
                    val (spoj, zastavky) = repo.spojSeZastavkamiPodleId(spojNaMape.id, LocalDate.now())
                    JedouciSpojADalsiVeci(
                        spojId = spoj.id,
                        pristiZastavkaNazev = zastavky.lastOrNull { it.cas == spojNaMape.pristiZastavka }?.nazev ?: return@async null,
                        pristiZastavkaCas = zastavky.lastOrNull { it.cas == spojNaMape.pristiZastavka }?.cas ?: return@async null,
                        zpozdeni = spojNaMape.zpozdeniMin ?: return@async null,
                        indexNaLince = zastavky.indexOfLast { it.cas == spojNaMape.pristiZastavka },
                        smer = spoj.smer,
                        cisloLinky = spoj.linka - 325_000,
                        cil = zastavky.last().nazev,
                        vuz = spojNaMape.vuz ?: return@async null,
                        kurz = spoj.kurz,
                    )
                }
            }
            .awaitAll()
            .filterNotNull()
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
                                spoje = seznam.jenJedouciSpoje(),
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

    private val filtrovanyVysledek = praveJedouci.combine(filtry) { vysledek, filtry ->
        when (vysledek) {
            is VysledekPraveJedoucich.EvC -> vysledek.copy(seznam = vysledek.seznam.filter { filtry.isEmpty() || it.cisloLinky in filtry })
            is VysledekPraveJedoucich.Poloha -> vysledek.copy(seznam = vysledek.seznam.filter { filtry.isEmpty() || it.cisloLinky in filtry })
            is VysledekPraveJedoucich.Zpozdeni -> vysledek.copy(seznam = vysledek.seznam.filter { filtry.isEmpty() || it.cisloLinky in filtry })
        }
    }

    private val cislaLinek = flow {
        emit(repo.cislaLinek(LocalDate.now()))
    }

    private val praveNejedouci = combine(repo.praveJedouci, praveJedouci, filtry) { praveJedouci, vysledek, filtry ->
        val opravduJedouci = when (vysledek) {
            is VysledekPraveJedoucich.EvC -> vysledek.seznam.mapNotNull { it.kurz }
            is VysledekPraveJedoucich.Poloha -> vysledek.seznam.flatMap { it.spoje }.mapNotNull { it.kurz }
            is VysledekPraveJedoucich.Zpozdeni -> vysledek.seznam.mapNotNull { it.kurz }
        }

        praveJedouci.filter { (kurz, linky) ->
            kurz !in opravduJedouci && (linky.any { it in  filtry} || filtry.isEmpty())
        }.map { it.first }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5.seconds), emptyList())

    val state =
        kombajn(repo.datum, cislaLinek, filtrovanyVysledek, nacitaSe, repo.maPristupKJihu, filtry, typ, praveNejedouci) { datum, cislaLinek, vysledek, nacitaSeSeznam, jeOnline, filtry, typ, praveNejedouci ->
            if (datum != LocalDate.now()) return@kombajn PraveJedouciState.NeniDneska

            if (!jeOnline) return@kombajn PraveJedouciState.Offline

            if (cislaLinek.isEmpty()) return@kombajn PraveJedouciState.ZadneLinky

            if (vysledek.seznam.isNotEmpty()) return@kombajn PraveJedouciState.OK(cislaLinek, filtry, typ, praveNejedouci, vysledek)

            if (nacitaSeSeznam) return@kombajn PraveJedouciState.Nacitani(cislaLinek, filtry, typ)

            return@kombajn PraveJedouciState.PraveNicNejede(cislaLinek, filtry, typ, praveNejedouci)
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
        val kurz: String?,
    ) {
        fun toJedouciZpozdenySpoj() = JedouciZpozdenySpoj(
            spojId = spojId,
            zpozdeni = zpozdeni,
            cisloLinky = cisloLinky,
            cilovaZastavka = cil,
            kurz = kurz,
        )

        fun toJedouciVuz() = JedouciVuz(
            spojId = spojId,
            cisloLinky = cisloLinky,
            cilovaZastavka = cil,
            vuz = vuz,
            kurz = kurz,
        )

        fun toJedouciSpoj() = JedouciSpoj(
            spojId = spojId,
            pristiZastavkaNazev = pristiZastavkaNazev,
            pristiZastavkaCas = pristiZastavkaCas,
            zpozdeni = zpozdeni,
            vuz = vuz,
            kurz = kurz,
        )

        companion object {
            fun List<JedouciSpojADalsiVeci>.jenJedouciSpoje() = map(JedouciSpojADalsiVeci::toJedouciSpoj)
        }
    }
}
