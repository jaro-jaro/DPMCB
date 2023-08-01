package cz.jaro.dpmcb.data

import android.app.Application
import com.google.firebase.crashlytics.ktx.crashlytics
import com.google.firebase.ktx.Firebase
import cz.jaro.dpmcb.data.database.Dao
import cz.jaro.dpmcb.data.entities.CasKod
import cz.jaro.dpmcb.data.entities.Linka
import cz.jaro.dpmcb.data.entities.Spoj
import cz.jaro.dpmcb.data.entities.Zastavka
import cz.jaro.dpmcb.data.entities.ZastavkaSpoje
import cz.jaro.dpmcb.data.helperclasses.Quadruple
import cz.jaro.dpmcb.data.helperclasses.Quintuple
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.isOnline
import cz.jaro.dpmcb.data.realtions.CasNazevSpojId
import cz.jaro.dpmcb.data.realtions.CasNazevSpojIdLInkaPristi
import cz.jaro.dpmcb.data.realtions.JedeOdDo
import cz.jaro.dpmcb.data.realtions.LinkaNizkopodlaznostSpojId
import cz.jaro.dpmcb.data.realtions.NazevACas
import cz.jaro.dpmcb.data.realtions.ZastavkaSpojeSeSpojemAJehoZastavky
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.koin.core.annotation.Single
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.Month

@Single
class SpojeRepository(
    ctx: Application,
    private val localDataSource: Dao,
    private val preferenceDataSource: PreferenceDataSource,
) {

    private val scope = CoroutineScope(Dispatchers.IO)

    private val _datum = MutableStateFlow(LocalDate.now())
    val datum = _datum.asStateFlow()

    private val _onlineMod = MutableStateFlow(Nastaveni().autoOnline)
    val onlineMod = _onlineMod.asStateFlow()

    val nastaveni = preferenceDataSource.nastaveni

    val zobrazitNizkopodlaznost = preferenceDataSource.nizkopodlaznost

    val oblibene = preferenceDataSource.oblibene

    val verze = preferenceDataSource.verze

    init {
        scope.launch {
            preferenceDataSource.nastaveni.collect { nastaveni ->
                _onlineMod.value = nastaveni.autoOnline
            }
        }
    }

    private val tabulkyMap = mutableMapOf<LocalDate, MutableMap<Int, String?>>()

    private suspend fun pravePouzivanaTabulka(datum: LocalDate, cisloLinky: Int) = tabulkyMap.getOrPut(datum) { mutableMapOf() }.getOrPut(cisloLinky) {
        val tabulky = localDataSource.tabulkyLinky(cisloLinky)

        val tabulky2 = tabulky.filter {
            it.platnostOd <= datum && datum <= it.platnostDo
        }

        if (tabulky2.isEmpty()) return@getOrPut null
        if (tabulky2.size == 1) return@getOrPut tabulky2.first().tab

        val tabulky3 = tabulky2.sortedByDescending { it.platnostOd }

        val tabulky4 =
            if (tabulky3.none { it.maVyluku })
                tabulky3
            else
                tabulky3.filter { it.maVyluku }

        return@getOrPut tabulky4.first().tab
    }

    private suspend fun LocalDate.jeTatoTabulkaPravePouzivana(tab: String): Boolean {
        val cisloLinky = tab.split("-").first().toInt()
        return pravePouzivanaTabulka(this, cisloLinky) == tab
    }

    private suspend fun vsechnyTabulky(datum: LocalDate) =
        localDataSource.vsechnyLinky().mapNotNull { cisloLinky ->
            pravePouzivanaTabulka(datum, cisloLinky)
        }

    suspend fun zastavky(datum: LocalDate) = localDataSource.nazvyZastavek(vsechnyTabulky(datum))
    suspend fun cislaLinek(datum: LocalDate) = localDataSource.cislaLinek(vsechnyTabulky(datum))

    suspend fun spojSeZastavkySpojeNaKterychStaviACaskody(spojId: String, datum: LocalDate) =
        localDataSource.spojSeZastavkySpojeNaKterychStavi(spojId, pravePouzivanaTabulka(datum, extrahovatCisloLinky(spojId))!!).run {
            val bezkodu = distinctBy {
                it.copy(pevneKody = "", jede = false, od = LocalDate.now(), `do` = LocalDate.now())
            }
            val caskody = map {
                JedeOdDo(
                    jede = it.jede,
                    v = it.od..it.`do`
                )
            }.distinctBy {
                it.jede to it.v.toString()
            }
            Quintuple(
                first().let {
                    LinkaNizkopodlaznostSpojId(
                        nizkopodlaznost = it.nizkopodlaznost,
                        linka = it.linka - 325_000,
                        spojId = it.spojId
                    )
                },
                bezkodu.mapIndexed { i, it ->
                    CasNazevSpojIdLInkaPristi(
                        cas = it.cas,
                        nazev = it.nazev,
                        linka = it.linka - 325_000,
                        pristiZastavka = bezkodu.getOrNull(i + 1)?.nazev,
                        spojId = it.spojId
                    )
                }.distinct(),
                caskody,
                zcitelnitPevneKody(first().pevneKody),
                listOf(
                    (caskody.filter { it.jede }.ifEmpty { null }?.any { datum in it.v } ?: true),
                    caskody.filter { !it.jede }.none { datum in it.v },
                    datum.jedeDnes(first().pevneKody),
                ).all { it }
            )
        }

    private fun extrahovatCisloLinky(spojId: String) = spojId.split("-")[1].toInt()

    suspend fun spojSeZastavkySpojeNaKterychStaviAJedeV(spojId: String, datum: LocalDate) =
        localDataSource.spojSeZastavkySpojeNaKterychStavi(spojId, pravePouzivanaTabulka(datum, extrahovatCisloLinky(spojId))!!)
            .run {
                val caskody = map { JedeOdDo(it.jede, it.od..it.`do`) }.distinctBy { it.jede to it.v.toString() }
                Triple(
                    first().let { LinkaNizkopodlaznostSpojId(it.nizkopodlaznost, it.linka - 325_000, it.spojId) },
                    map { CasNazevSpojId(it.cas, it.nazev, it.spojId) }.distinct(),
                ) { datum: LocalDate ->
                    listOf(
                        (caskody.filter { it.jede }.ifEmpty { null }?.any { datum in it.v } ?: true),
                        caskody.filter { !it.jede }.none { datum in it.v },
                        datum.jedeDnes(first().pevneKody),
                    ).all { it }
                }
            }

    suspend fun nazvyZastavekLinky(linka: Int, datum: LocalDate) =
        localDataSource.nazvyZastavekLinky(linka + 325_000, pravePouzivanaTabulka(datum, linka + 325_000)!!)

    suspend fun pristiZastavky(linka: Int, tahleZastavka: String, datum: LocalDate) =
        localDataSource.pristiZastavky(linka + 325_000, tahleZastavka, pravePouzivanaTabulka(datum, linka + 325_000)!!)

    suspend fun zastavkyJedouciVDatumSPristiZastavkou(linka: Int, zastavka: String, pristiZastavka: String, datum: LocalDate) =
        localDataSource.zastavkyJedouciVDatumSPristiZastavkou(
            linka = linka + 325_000,
            zastavka = zastavka,
            pristiZastavka = pristiZastavka,
            datum = datum,
            tab = pravePouzivanaTabulka(datum, linka + 325_000)!!
        ).filter { (_, _, _, pevneKody) ->
            datum.jedeDnes(pevneKody)
        }

    suspend fun zapsat(
        zastavkySpoje: Array<ZastavkaSpoje>,
        zastavky: Array<Zastavka>,
        casKody: Array<CasKod>,
        linky: Array<Linka>,
        spoje: Array<Spoj>,
        verze: Int,
    ) {
        preferenceDataSource.zmenitVerzi(verze)

        localDataSource.vlozitZastavkySpoje(*zastavkySpoje)
        localDataSource.vlozitZastavky(*zastavky)
        localDataSource.vlozitCasKody(*casKody)
        localDataSource.vlozitLinky(*linky)
        localDataSource.vlozitSpoje(*spoje)
    }

    fun upravitDatum(datum: LocalDate) {
        _datum.update { datum }
    }

    fun upravitOnlineMod(mod: Boolean) {
        _onlineMod.update { mod }
    }

    suspend fun upravitNastaveni(update: (Nastaveni) -> Nastaveni) {
        preferenceDataSource.zmenitNastaveni(update)
    }

    suspend fun zmenitNizkopodlaznost(value: Boolean) {
        preferenceDataSource.zmenitNizkopodlaznost(value)
    }

    suspend fun pridatOblibeny(id: String) {
        preferenceDataSource.zmenitOblibene {
            it + id
        }
    }

    suspend fun odebratOblibeny(id: String) {
        preferenceDataSource.zmenitOblibene {
            it - id
        }
    }

    suspend fun spojeJedouciVdatumZastavujiciNaIndexechZastavkySeZastavkySpoje(datum: LocalDate, zastavka: String): List<ZastavkaSpojeSeSpojemAJehoZastavky> =
        localDataSource.spojeZastavujiciNaIndexechZastavky(zastavka, vsechnyTabulky(datum))
            .groupBy { "S-${it.linka}-${it.cisloSpoje}" to it.indexZastavkyNaLince }
            .map { Triple(it.key.first, it.key.second, it.value) }
            .filter { (_, _, seznam) ->
                val caskody = seznam.map { JedeOdDo(it.jede, it.od..it.`do`) }.distinctBy { it.jede to it.v.toString() }
                listOf(
                    (caskody.filter { it.jede }.ifEmpty { null }?.any { datum in it.v } ?: true),
                    caskody.filter { !it.jede }.none { datum in it.v },
                    datum.jedeDnes(seznam.first().pevneKody),
                ).all { it }
            }
            .map { Triple(it.first, it.second, it.third.first()) }
            .let { seznam ->
                val zastavkySpoju = localDataSource.zastavkySpoju(seznam.map { it.first }, vsechnyTabulky(datum))
                seznam.map { Quadruple(it.first, it.second, it.third, zastavkySpoju[it.first]!!) }
            }
            .map { (spojId, indexZastavkyNaLince, info, zastavky) ->
                ZastavkaSpojeSeSpojemAJehoZastavky(
                    nazev = info.nazev,
                    cas = info.cas,
                    indexZastavkyNaLince = indexZastavkyNaLince,
                    spojId = spojId,
                    linka = info.linka - 325_000,
                    nizkopodlaznost = info.nizkopodlaznost,
                    zastavkySpoje = zastavky
                )
            }

    suspend fun spojSeZastavkamiPodleId(spojId: String, datum: LocalDate): Pair<Spoj, List<NazevACas>> =
        localDataSource.spojSeZastavkamiPodleId(spojId, pravePouzivanaTabulka(datum, extrahovatCisloLinky(spojId))!!)
            .toList()
            .first { datum.jeTatoTabulkaPravePouzivana(it.first.tab) }

    val isOnline = flow {
        while (currentCoroutineContext().isActive) {
            emit(ctx.isOnline)
            delay(5000)
        }
    }.stateIn(scope, SharingStarted.WhileSubscribed(5_000), ctx.isOnline)

    val maPristupKJihu = isOnline.combine(onlineMod) { jeOnline, onlineMod ->
        jeOnline && onlineMod
    }.stateIn(scope, SharingStarted.WhileSubscribed(5_000), ctx.isOnline && nastaveni.value.autoOnline)

    suspend fun maVyluku(spojId: String, datum: LocalDate) =
        localDataSource.vyluka(pravePouzivanaTabulka(datum, extrahovatCisloLinky(spojId))!!)

    suspend fun platnostLinky(spojId: String, datum: LocalDate) =
        localDataSource.platnost(pravePouzivanaTabulka(datum, extrahovatCisloLinky(spojId))!!)

    suspend fun existujeSpoj(spojId: String, datum: LocalDate): Boolean {
        val cisloLinky = try {
            extrahovatCisloLinky(spojId)
        } catch (e: Exception) {
            Firebase.crashlytics.recordException(e)
            return false
        }
        val tabulka = pravePouzivanaTabulka(datum, cisloLinky) ?: return false
        return localDataSource.existujeSpoj(spojId, tabulka) != null
    }

    fun spojJedeV(spojId: String): suspend (LocalDate) -> Boolean = { datum ->
        val seznam = localDataSource.pevneKodyCaskody(spojId, pravePouzivanaTabulka(datum, extrahovatCisloLinky(spojId))!!)
            .map { JedeOdDo(it.jede, it.od..it.`do`) to it.pevneKody }

        listOf(
            (seznam.map { it.first }.filter { it.jede }.ifEmpty { null }?.any { datum in it.v } ?: true),
            seznam.map { it.first }.filter { !it.jede }.none { datum in it.v },
            datum.jedeDnes(seznam.first().second),
        ).all { it }
    }
}

private fun LocalDate.jedeDnes(pevneKody: String) = pevneKody
    .split(" ")
    .mapNotNull {
        when (it) {
            "1" -> dayOfWeek in DayOfWeek.MONDAY..DayOfWeek.FRIDAY && !jeStatniSvatekNeboDenPracovnihoKlidu(this) // jede v pracovních dnech
            "2" -> dayOfWeek == DayOfWeek.SUNDAY || jeStatniSvatekNeboDenPracovnihoKlidu(this) // jede v neděli a ve státem uznané svátky
            "3" -> dayOfWeek == DayOfWeek.MONDAY // jede v pondělí
            "4" -> dayOfWeek == DayOfWeek.TUESDAY // jede v úterý
            "5" -> dayOfWeek == DayOfWeek.WEDNESDAY // jede ve středu
            "6" -> dayOfWeek == DayOfWeek.THURSDAY // jede ve čtvrtek
            "7" -> dayOfWeek == DayOfWeek.FRIDAY // jede v pátek
            "8" -> dayOfWeek == DayOfWeek.SATURDAY // jede v sobotu
            "14" -> null // bezbariérově přístupná zastávka
            "19" -> null // ???
            "24" -> null // spoj s částečně bezbariérově přístupným vozidlem, nutná dopomoc průvodce
            "28" -> null // zastávka s možností přestupu na železniční dopravu
            else -> null
        }
    }
    .ifEmpty { listOf(true) }
    .any { it }

private fun jeStatniSvatekNeboDenPracovnihoKlidu(datum: LocalDate) = listOf(
    LocalDate.of(1, 1, 1), // Den obnovy samostatného českého státu
    LocalDate.of(1, 1, 1), // Nový rok
    LocalDate.of(1, 5, 1), // Svátek práce
    LocalDate.of(1, 5, 8), // Den vítězství
    LocalDate.of(1, 7, 5), // Den slovanských věrozvěstů Cyrila a Metoděje,
    LocalDate.of(1, 7, 6), // Den upálení mistra Jana Husa
    LocalDate.of(1, 9, 28), // Den české státnosti
    LocalDate.of(1, 10, 28), // Den vzniku samostatného československého státu
    LocalDate.of(1, 11, 17), // Den boje za svobodu a demokracii
    LocalDate.of(1, 12, 24), // Štědrý den
    LocalDate.of(1, 12, 25), // 1. svátek vánoční
    LocalDate.of(1, 12, 26), // 2. svátek vánoční
).any {
    it.dayOfMonth == datum.dayOfMonth && it.month == datum.month
} || jeVelkyPatekNeboVelikonocniPondeli(datum)

private fun jeVelkyPatekNeboVelikonocniPondeli(datum: LocalDate): Boolean {
    val (velkyPatek, velikonocniPondeli) = polohaVelkehoPatkuAVelikonocnihoPondeliVRoce(datum.year) ?: return false

    return datum == velikonocniPondeli || datum == velkyPatek
}

fun polohaVelkehoPatkuAVelikonocnihoPondeliVRoce(rok: Int): Pair<LocalDate, LocalDate>? {

    // Zdroj: https://cs.wikipedia.org/wiki/V%C3%BDpo%C4%8Det_data_Velikonoc#Algoritmus_k_v%C3%BDpo%C4%8Dtu_data_Velikonoc

    val (m, n) = listOf(
        1583..1599 to (22 to 2),
        1600..1699 to (22 to 2),
        1700..1799 to (23 to 3),
        1800..1899 to (23 to 4),
        1900..1999 to (24 to 5),
        2000..2099 to (24 to 5),
        2100..2199 to (24 to 6),
        2200..2299 to (25 to 0),
    ).find { (roky, _) ->
        rok in roky
    }?.second ?: return null

    val a = rok % 19
    val b = rok % 4
    val c = rok % 7
    val d = (19 * a + m) % 30
    val e = (n + 2 * b + 4 * c + 6 * d) % 7
    val velikonocniNedeleOdZacatkuBrezna = 22 + d + e

    val velkyPatekOdZacatkuBrezna = velikonocniNedeleOdZacatkuBrezna - 2
    val velkyPatek = LocalDate.of(rok, Month.MARCH, 1).plusDays(velkyPatekOdZacatkuBrezna - 1L)

    val velikonocniPondeliOdZacatkuBrezna = velikonocniNedeleOdZacatkuBrezna + 1
    val velikonocniPondeli = LocalDate.of(rok, Month.MARCH, 1).plusDays(velikonocniPondeliOdZacatkuBrezna - 1L)

    return velkyPatek to velikonocniPondeli
}

private fun zcitelnitPevneKody(pevneKody: String) = pevneKody
    .split(" ")
    .mapNotNull {
        when (it) {
            "1" -> "Jede v pracovních dnech"
            "2" -> "Jede v neděli a ve státem uznané svátky"
            "3" -> "Jede v pondělí"
            "4" -> "Jede v úterý"
            "5" -> "Jede ve středu"
            "6" -> "Jede ve čtvrtek"
            "7" -> "Jede v pátek"
            "8" -> "Jede v sobotu"
            "14" -> "Bezbariérově přístupná zastávka"
            "19" -> null
            "24" -> "Spoj s částečně bezbariérově přístupným vozidlem, nutná dopomoc průvodce"
            "28" -> "Zastávka s možností přestupu na železniční dopravu"
            else -> null
        }
    }