package cz.jaro.dpmcb.ui.spojeni

import cz.jaro.dpmcb.data.App.Companion.repo
import cz.jaro.dpmcb.data.Spojeni
import cz.jaro.dpmcb.data.entities.Spoj
import cz.jaro.dpmcb.data.helperclasses.Cas
import cz.jaro.dpmcb.data.helperclasses.Smer
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.VDP
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.funguj
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.pristiZastavka
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.reversedIf
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.toInt
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.vsechnyIndexy
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.zastavkySpoje
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlin.math.abs
import kotlin.system.measureTimeMillis

private typealias CestaDoCile = List<String>

class VyhledavacSpojeni {

    data class CastSpojeni(
        val minulaZastavka: String,
        val odjezd: Cas,
        val spoj: Spoj,
        val prijezd: Cas,
        val pristiZastavka: String,
    )

    fun vyhledatSpojeni(
        nastaveniVyhledavani: NastaveniVyhledavani,
        n: Int = 10,
    ): Flow<Spojeni> {
        println("Vyhledávám")
        funguj("Vyhledávám", nastaveniVyhledavani.start, nastaveniVyhledavani.cil, nastaveniVyhledavani.cas)
        return flow {
            funguj("Nalezeno všech $n spojení za " + measureTimeMillis {

                val mozneCesty = najitMozneCesty(nastaveniVyhledavani.start, nastaveniVyhledavani.cil)
//                funguj(slepyUlicky.sorted().joinToString("\n"))
                funguj(mozneCesty.joinToString("\n"))
                funguj(mozneCesty.size)

                val seznam = mutableListOf<Spojeni>()
                repeat(n) { i ->
                    funguj(i)
                    funguj("Spojení č. $i nalezeno za " + measureTimeMillis {
//                        funguj(i, repo.typDne.value)
                        val tabulkaNejSpojeni = najdiNejSpojeni(
                            start = nastaveniVyhledavani.start,
                            vyhledavaciCas = nastaveniVyhledavani.cas,
                            jenNizkopodlazni = nastaveniVyhledavani.jenNizkopodlazni,
                            jenPrima = nastaveniVyhledavani.jenPrima,
                            ignorovatPrvnichSpoju = i,
                            seznamCest = mozneCesty,
                            typDne = repo.typDne.value
                        )
//                        funguj(tabulkaNejSpojeni)
                        if (tabulkaNejSpojeni[nastaveniVyhledavani.cil]!!.casPrijezdu == Cas.nikdy) {
                            return@repeat
                        }

                        val nejSpojeni = upravitVysledek(
                            tabulka = tabulkaNejSpojeni,
                            start = nastaveniVyhledavani.start,
                            cil = nastaveniVyhledavani.cil
                        )
                        funguj(nejSpojeni.map {
                            listOf(it.minulaZastavka, it.odjezd.toString(), "${it.spoj.cisloLinky} (${it.spoj.id})", it.prijezd.toString(), it.pristiZastavka)
                        })
                        seznam += nejSpojeni

                        emit(nejSpojeni)

                    } + " ms")
                }
            } + " ms")
        }
    }

    private fun najitMozneCesty(
        start: String,
        cil: String,
    ): List<CestaDoCile> {
        slepyUlicky.clear()

        return cestaRekurze(start, cil).sortedBy { it.size }.let { moznosti ->
            moznosti.takeWhile { abs(it.size - moznosti.first().size) <= 5 }
        }/*.also { cesty ->
            cesty
                .flatMap { cesta ->
                    cesta.windowed(2)
                }
                .map {
                    it[0] to it[1]
                }
                .groupBy({ it.first }, { it.second })
                .map { (od, doMoznosti) ->
                    od to doMoznosti.toSet()
                }
                .toMap()
                .flatMap { (k, v) ->
                    v.map { k to it }
                }
                .joinToString("\n") {
                    it.toList().joinToString("&&&")
                }
                .replace(" ", "")
                .replace("&&&", " ")
                .also {
                    funguj(it)
                }
        }*/
    }

    private val slepyUlicky: MutableList<String> = mutableListOf()

    private fun cestaRekurze(
        ja: String,
        cil: String,
        minule: List<String> = emptyList(),
    ): List<CestaDoCile> {
//        funguj(ja, "přes", minule.reversed())
        if (ja in slepyUlicky)
            return emptyList()

        if (ja == cil) return listOf(listOf(cil))

        val vsechynSmery = repo.graphZastavek[ja]!!.filter { minule.firstOrNull() != it } // Vsechny smery krome zpet

        val smery = vsechynSmery.filterNot { minule.contains(it) } // Vsechny smery kde jsem jeste nebyl

        val moznosti = smery.flatMap { soused ->
            cestaRekurze(soused, cil, minule + listOf(ja))/*.also {
                if (ja in listOf("U Koníčka", "Senovážné nám. - pošta", "Poliklinika Sever"))
                    funguj("$ja -> $soused", it.joinToString())
            }*/
        }

//        if (ja in listOf("U Koníčka", "Senovážné nám. - pošta", "Poliklinika Sever"))
//            funguj(ja, moznosti)

        if (moznosti.isEmpty() && vsechynSmery.size == smery.size)
            slepyUlicky += ja
//        funguj(ja, moznosti)
        return moznosti.map { cesta -> listOf(ja) + cesta }
    }

    data class RadekTabulky(
        val nejVzdalenostOdStartu: Int,
        val minulaZastavka: String,
        val casPrijezdu: Cas,
        val casOdjezduZMinulyZastvky: Cas,
        val minulejSpoj: Long,
    )

    private suspend fun najdiNejSpojeni(
        start: String,
        vyhledavaciCas: Cas,
        jenNizkopodlazni: Boolean,
        jenPrima: Boolean,
        ignorovatPrvnichSpoju: Int,
        seznamCest: List<CestaDoCile>,
        typDne: VDP,
    ): MutableMap<String, RadekTabulky> {
//        funguj(seznamCest)
        val vsechnyZastavky = seznamCest.flatten().distinct()
        val tabulka = mutableMapOf(
            *arrayOf(
                start to RadekTabulky(
                    nejVzdalenostOdStartu = 0,
                    minulaZastavka = "",
                    casPrijezdu = Cas.nikdy,
                    casOdjezduZMinulyZastvky = Cas.nikdy,
                    minulejSpoj = -1L
                )
            ) + vsechnyZastavky.map {
                it to RadekTabulky(
                    nejVzdalenostOdStartu = Int.MAX_VALUE,
                    minulaZastavka = "",
                    casPrijezdu = Cas.nikdy,
                    casOdjezduZMinulyZastvky = Cas.nikdy,
                    minulejSpoj = -1L
                )
            }
        )

//        funguj(vsechnyZastavky)
        val queue: MutableList<Triple<Cas, CestaDoCile, Int>> = mutableListOf()

//        funguj(queue)
        queue.addAll(seznamCest.map { Triple(vyhledavaciCas, it, Int.MAX_VALUE) })
//        funguj(queue)

        while (queue.isNotEmpty()) {
            val (aktualniCas, cesta) = queue.first()
            val nazev = cesta.first()
            val cil = cesta.last()
            if (nazev == cil) {
                queue.removeAt(0)
                continue
            }

            val soused = cesta[1]
//            funguj("rekurzuju", nazev, soused, aktualniCas, cil, cesta)
            val zastavkyMinulyhoSpoje = if (tabulka[nazev]!!.minulejSpoj != -1L) repo.zastavkySpoje(tabulka[nazev]!!.minulejSpoj) else null
            val nazvyZastavekMinulyhoSpoje = zastavkyMinulyhoSpoje?.map { it.nazevZastavky }

            val preskocit = if (nazev === start) ignorovatPrvnichSpoju else 0
            var pocetPreskocenych = -1
            funguj(nazev, preskocit)

            val a = if (nazvyZastavekMinulyhoSpoje != null && (nazvyZastavekMinulyhoSpoje.contains(soused) || jenPrima)) {
                val b = zastavkyMinulyhoSpoje.vsechnyIndexy(soused) // Najdeme zastávku v minulým spoji
                    .map { Triple(tabulka[nazev]!!.minulejSpoj, null, zastavkyMinulyhoSpoje[it]) }
                    .find { it.third.cas >= aktualniCas && it.third.cas != Cas.nikdy }
                if (jenPrima && (b == null)) {
                    queue.removeAt(0)
                    continue
                }
                b
            } else null

            val (prvniSpojId, tahleZast, pristiZast) =
                a // Najdeme zastávku všude
                    ?: repo.spojeJedouciVTypDneSeZastavkySpoju(typDne)
                        .filter { (spoj, _) ->
                            !jenNizkopodlazni || spoj.nizkopodlaznost
                        }
                        .map { (spoj, zastavky) ->
                            Triple(spoj, zastavky, zastavky.vsechnyIndexy(nazev))
                        }
                        .flatMap { (spoj, zastavky, indexy) ->
                            indexy.map { Triple(spoj, zastavky, it) }
                        }
                        .filter { (spoj, zasatvky, index) ->
                            zasatvky.pristiZastavka(spoj.smer, index)?.nazevZastavky == soused
                        }
                        .map { (spoj, zastavky, index) ->
                            Triple(spoj.id, zastavky[index], zastavky[index + spoj.smer.toInt()])
                        }
                        .find { (_, zast, pristiZast) ->
////                            funguj(_, zast, pristiZast)
                            val zastavkaBySla = zast.run { cas != Cas.nikdy && aktualniCas <= cas }
                                    && pristiZast.run { cas != Cas.nikdy && aktualniCas <= cas }
                            zastavkaBySla && run {
                                pocetPreskocenych++
                                pocetPreskocenych >= preskocit
                            }
                        }.also {
                            if (it == null) {
//                                funguj(nazev, soused, aktualniCas, "NENALEZENO")
                                queue.removeAt(0)
                            }
                        } ?: continue

//            funguj(pristiZast.cas, aktualniCas)
            val zlost = (pristiZast.cas - aktualniCas)

            val minulaVzdalenostOdStartu = tabulka[nazev]!!.nejVzdalenostOdStartu
            val novaVzdalenostOdStartu = if (minulaVzdalenostOdStartu == Int.MAX_VALUE) zlost else minulaVzdalenostOdStartu + zlost

////            if (!tabulka.containsKey(soused) || tabulka[soused]!!.nejVzdalenostOdStartu > novaVzdalenostOdStartu) funguj(nazev, aktualniCas, tahleZast, pristiZast)

            if (!tabulka.containsKey(soused) || tabulka[soused]!!.nejVzdalenostOdStartu > novaVzdalenostOdStartu) tabulka[soused] = RadekTabulky(
                nejVzdalenostOdStartu = novaVzdalenostOdStartu,
                minulaZastavka = nazev,
                casPrijezdu = pristiZast.cas,
                casOdjezduZMinulyZastvky = tahleZast?.cas ?: aktualniCas,
                minulejSpoj = prvniSpojId
            )

//            funguj(aktualniCas, zlost)
            queue.removeAt(0)
            if (novaVzdalenostOdStartu > tabulka[cil]!!.nejVzdalenostOdStartu) {
//                funguj("Už je to zbytečný")
            } else {
                queue += Triple(aktualniCas + zlost, cesta.drop(1), zlost)
            }
            queue.sortBy { it.third }
        }

//        funguj(tabulka)
        return tabulka
    }

    private suspend fun upravitVysledek(
        tabulka: Map<String, RadekTabulky>,
        start: String,
        cil: String,
    ): Spojeni = upravitRekurze(
        nazev = cil,
        cil = start,
        tabulka = tabulka
    ).groupBy { it.spoj }.map { (spoj, castiSpojeni) ->
        val prvni = castiSpojeni.first()
        val posledni = castiSpojeni.last()
        CastSpojeni(
            minulaZastavka = prvni.minulaZastavka,
            odjezd = prvni.odjezd,
            spoj = spoj,
            prijezd = posledni.prijezd,
            pristiZastavka = posledni.pristiZastavka,
        )
    }.let { seznam ->

        val novejSeznam = seznam.toMutableList()
        while (true) {
            val res = run {
                val docasnejSeznam = novejSeznam.toList().reversed()
                docasnejSeznam.forEachIndexed { i, castSpojeni ->
                    if (i == docasnejSeznam.lastIndex) return@run true
                    val minulaCastSpojeni = docasnejSeznam[i + 1]
                    val spoj = castSpojeni.spoj
                    val zastavkySpoje = spoj.zastavkySpoje()

                    val zastavkaVyjezduMinulyCastiNaTomtoSpoji = zastavkySpoje
                        .reversedIf { spoj.smer == Smer.NEGATIVNI }
                        .dropLastWhile { it.nazevZastavky != castSpojeni.minulaZastavka && it.cas != castSpojeni.odjezd }
                        .findLast { it.nazevZastavky == minulaCastSpojeni.minulaZastavka && it.cas >= minulaCastSpojeni.odjezd }

                    if (zastavkaVyjezduMinulyCastiNaTomtoSpoji != null) {
                        novejSeznam[novejSeznam.lastIndex - i] = castSpojeni.copy(
                            minulaZastavka = zastavkaVyjezduMinulyCastiNaTomtoSpoji.nazevZastavky,
                            odjezd = zastavkaVyjezduMinulyCastiNaTomtoSpoji.cas
                        )
                        novejSeznam.remove(minulaCastSpojeni)
                        return@run false
                    }
                }
                return@run true
            }
            if (res) break
        }

        novejSeznam
    }

    private suspend fun upravitRekurze(
        nazev: String,
        cil: String,
        tabulka: Map<String, RadekTabulky>,
    ): Spojeni {
        if (nazev == cil) return listOf()
        val radekTabulky = tabulka[nazev]!!
        return upravitRekurze(radekTabulky.minulaZastavka, cil, tabulka) + CastSpojeni(
            minulaZastavka = radekTabulky.minulaZastavka,
            odjezd = radekTabulky.casOdjezduZMinulyZastvky,
            spoj = repo.spoj(radekTabulky.minulejSpoj),
            prijezd = radekTabulky.casPrijezdu,
            pristiZastavka = nazev
        )
    }
}
