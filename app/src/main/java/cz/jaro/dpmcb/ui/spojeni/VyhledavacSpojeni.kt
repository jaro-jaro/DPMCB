package cz.jaro.dpmcb.ui.spojeni

import cz.jaro.dpmcb.data.App.Companion.repo
import cz.jaro.dpmcb.data.Spojeni
import cz.jaro.dpmcb.data.helperclasses.Cas
import cz.jaro.dpmcb.data.helperclasses.Smer
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.funguj
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.pristiZastavka
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.reversedIf
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.toInt
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.vsechnyIndexy
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlin.system.measureTimeMillis

private typealias CestaDoCile = List<String>

object VyhledavacSpojeni {

    data class CastSpojeni(
        val minulaZastavka: String,
        val odjezd: Cas,
        val spojId: Long,
        val prijezd: Cas,
        val pristiZastavka: String,
    )

    suspend fun vyhledatSpojeni(
        start: String,
        cil: String,
        cas: Cas = Cas.ted,
        n: Int = 20,
    ): Flow<Spojeni> {
        funguj("Vyhledávám", start, cil, cas)
        return flow {
            funguj("Nalezeno všechn $n spojení za", measureTimeMillis {

                val mozneCesty = najitMozneCesty(start, cil)
                funguj(mozneCesty)

                repeat(n) {

                    val tabulkaNejSpojeni = najdiNejSpojeni(start, cas, mozneCesty)
                    funguj(tabulkaNejSpojeni)

                    val nejSpojeni = upravitVysledek(tabulkaNejSpojeni, start, cil)
                    funguj(nejSpojeni.map {
                        val spoj = repo.spoj(it.spojId)
                        listOf(it.minulaZastavka, it.odjezd.toString(), "${spoj.cisloLinky} (${it.spojId})", it.prijezd.toString(), it.pristiZastavka)
                    })

                    emit(nejSpojeni)
                }
            })
        }
    }

    private suspend fun najitMozneCesty(
        start: String,
        cil: String,
    ): List<CestaDoCile> {
        slepyUlicky.clear()

        return cestaRekurze(start, cil)
            /*.flatMap { it.windowed(2) }
            .map { it.toPair() }
            .groupBy { it.first }
            .map { (od, odDoMoznosti) ->
                od to odDoMoznosti.map { it.second }.toSet()
            }
            .toMap()*/
    }

    private val slepyUlicky: MutableList<String> = mutableListOf()

    private suspend fun cestaRekurze(
        ja: String,
        cil: String,
        minule: List<String> = emptyList(),
    ): List<CestaDoCile> {
//        funguj("Rekurzuju z", minule, "přes", ja, "do", cil)
        if (ja in slepyUlicky)
            return emptyList()

        if (ja == cil) return listOf(listOf(cil))

        val moznosti = repo.graphZastavek[ja]!!.filter { it !in minule }.flatMap { soused ->
            cestaRekurze(soused, cil, minule + listOf(ja))
                .filter { cesta -> ja !in cesta }
        }.map { cesta -> listOf(ja) + cesta }

        if (moznosti.isEmpty())
            slepyUlicky += ja
        funguj(ja, moznosti)
        return moznosti
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
        seznamCest: List<CestaDoCile>,
    ): MutableMap<String, RadekTabulky> {
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

        val queue: MutableList<Triple<Cas, CestaDoCile, Int>> = mutableListOf()

        queue.addAll(seznamCest.map { Triple(vyhledavaciCas, it, Int.MAX_VALUE) })

        while (queue.isNotEmpty()) {
            val (aktualniCas, cesta) = queue.first()
            val nazev = cesta.first()
            val cil = cesta.last()
            if (nazev == cil) {
                queue.removeAt(0)
                continue
            }

            val soused = cesta[1]
            val zastavkyMinulyhoSpoje = if (tabulka[nazev]!!.minulejSpoj != -1L) repo.zastavkySpoje(tabulka[nazev]!!.minulejSpoj) else null
            val nazvyZastavekMinulyhoSpoje = zastavkyMinulyhoSpoje?.map { it.nazevZastavky }

//            funguj("rekurzuju", nazev, soused, aktualniCas, cil, cesta)

            val (prvniSpojId, tahleZast, pristiZast) =
                (if (nazvyZastavekMinulyhoSpoje?.contains(soused) != true) null
                else zastavkyMinulyhoSpoje.vsechnyIndexy(soused)
                    .map { Triple(tabulka[nazev]!!.minulejSpoj, null, zastavkyMinulyhoSpoje[it]) }
                    .find { it.third.cas >= aktualniCas && it.third.cas != Cas.nikdy })
                    ?: repo.spojeJedouciVTypDneSeZastavkySpoju(repo.typDne)
                        .map { (spoj, zastavky) ->
                            Triple(spoj, zastavky, zastavky.vsechnyIndexy(nazev))
                        }
                        .flatMap { (spoj, zastavky, indexy) ->
                            indexy.map { Triple(spoj, zastavky, it) }
                        }
                        .filter { (spoj, zasatvky, index) ->
                            spoj.pristiZastavka(index)?.nazevZastavky == soused
                        }
                        .map { (spoj, zastavky, index) ->
                            Triple(spoj.id, zastavky[index], zastavky[index + spoj.smer.toInt()])
                        }
                        .find { (_, zast, pristiZast) ->
                            /*funguj(_, zast, pristiZast)*/
                            zast.run { cas != Cas.nikdy && aktualniCas <= cas }
                                    && pristiZast.run { cas != Cas.nikdy && aktualniCas <= cas }
                        }.also {
                            if (it == null) {
                                funguj(nazev, soused, aktualniCas, "NENALEZENO")
                                queue.removeAt(0)
                            }
                        } ?: continue

//            funguj(pristiZast.cas, aktualniCas)
            val zlost = (pristiZast.cas - aktualniCas)

            val minulaVzdalenostOdStartu = tabulka[nazev]!!.nejVzdalenostOdStartu
            val novaVzdalenostOdStartu = if (minulaVzdalenostOdStartu == Int.MAX_VALUE) zlost else minulaVzdalenostOdStartu + zlost

//            if (!tabulka.containsKey(soused) || tabulka[soused]!!.nejVzdalenostOdStartu > novaVzdalenostOdStartu) funguj(nazev, aktualniCas, tahleZast, pristiZast)

            if (!tabulka.containsKey(soused) || tabulka[soused]!!.nejVzdalenostOdStartu > novaVzdalenostOdStartu) tabulka[soused] = RadekTabulky(
                nejVzdalenostOdStartu = novaVzdalenostOdStartu,
                minulaZastavka = nazev,
                casPrijezdu = pristiZast.cas,
                casOdjezduZMinulyZastvky = tahleZast?.cas ?: aktualniCas,
                minulejSpoj = prvniSpojId
            )

//            funguj(aktualniCas, zlost)
            queue.removeAt(0)
            queue += Triple(aktualniCas + zlost, cesta.drop(1), zlost)
            queue.sortBy { it.third }
        }

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
    ).groupBy { it.spojId }.map { (spojId, castiSpojeni) ->
        val prvni = castiSpojeni.first()
        val posledni = castiSpojeni.last()
        CastSpojeni(
            minulaZastavka = prvni.minulaZastavka,
            odjezd = prvni.odjezd,
            spojId = spojId,
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
                    val (spoj, zastavkySpoje) = repo.spojSeZastavkySpoje(castSpojeni.spojId)

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

    private fun upravitRekurze(
        nazev: String,
        cil: String,
        tabulka: Map<String, RadekTabulky>,
    ): Spojeni {
        if (nazev == cil) return listOf()
        val radekTabulky = tabulka[nazev]!!
        return upravitRekurze(radekTabulky.minulaZastavka, cil, tabulka) + CastSpojeni(
            minulaZastavka = radekTabulky.minulaZastavka,
            odjezd = radekTabulky.casOdjezduZMinulyZastvky,
            spojId = radekTabulky.minulejSpoj,
            prijezd = radekTabulky.casPrijezdu,
            pristiZastavka = nazev
        )
    }
}
