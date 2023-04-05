package cz.jaro.dpmcb.data

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.room.Room
import cz.jaro.datum_cas.Datum
import cz.jaro.dpmcb.data.database.AppDatabase
import cz.jaro.dpmcb.data.entities.CasKod
import cz.jaro.dpmcb.data.entities.Linka
import cz.jaro.dpmcb.data.entities.Spoj
import cz.jaro.dpmcb.data.entities.Zastavka
import cz.jaro.dpmcb.data.entities.ZastavkaSpoje
import cz.jaro.dpmcb.data.helperclasses.Quadruple
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.funguj
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.isOnline
import cz.jaro.dpmcb.data.realtions.CasNazevSpojId
import cz.jaro.dpmcb.data.realtions.JedeOdDo
import cz.jaro.dpmcb.data.realtions.LinkaNizkopodlaznostSpojId
import cz.jaro.dpmcb.data.realtions.NazevACas
import cz.jaro.dpmcb.data.realtions.ZastavkaSpojeSeSpojemAJehoZastavky
import kotlinx.coroutines.MainScope
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
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.Calendar.DAY_OF_WEEK

class SpojeRepository(ctx: Application) {

    private val scope = MainScope()

    private val coroutineScope = MainScope()
    private val sharedPref: SharedPreferences =
        ctx.getSharedPreferences("PREFS_DPMCB_JARO", Context.MODE_PRIVATE)
    private lateinit var ostatniField: VsechnoOstatni

    private var ostatni: VsechnoOstatni
        get() {
            return if (::ostatniField.isInitialized) ostatniField
            else sharedPref.getString("ostatni", "{}").let { it ?: "{}" }.let {
                try {
                    Json.decodeFromString(it)
                } catch (e: RuntimeException) {
                    e.printStackTrace()
                    return@let VsechnoOstatni()
                }
            }
        }
        set(value) {
            val json = Json.encodeToString(value)
            sharedPref.edit {
                putString("ostatni", json)
            }
            ostatniField = value
        }

    private val db = Room.databaseBuilder(ctx, AppDatabase::class.java, "databaaaaze").fallbackToDestructiveMigration().build()

    private val _datum = MutableStateFlow(Datum.dnes)
    val datum = _datum.asStateFlow()

    private val _onlineMod = MutableStateFlow(ostatni.nastaveni.autoOnline)
    val onlineMod = _onlineMod.asStateFlow()

    private val _nastaveni = MutableStateFlow(ostatni.nastaveni)
    val nastaveni = _nastaveni.asStateFlow()

    private val _zobrazitNizkopodlaznost = MutableStateFlow(ostatni.zobrazitNizkopodlaznost)
    val zobrazitNizkopodlaznost = _zobrazitNizkopodlaznost.asStateFlow()

    private val _oblibene = MutableStateFlow(ostatni.oblibene)
    val oblibene = _oblibene.asStateFlow()

    private val dao get() = db.dao()
    val verze get() = ostatni.verze

    suspend fun zastavky() = dao.nazvyZastavek()
    suspend fun cislaLinek() = dao.cislaLinek()

    suspend fun spojSeZastavkySpojeNaKterychStaviACaskody(spojId: String) =
        dao.spojSeZastavkySpojeNaKterychStavi(spojId).run {
            Quadruple(
                first().let { LinkaNizkopodlaznostSpojId(it.nizkopodlaznost, it.linka - 325_000, it.spojId) },
                map { CasNazevSpojId(it.cas, it.nazev, it.spojId) }.distinct(),
                map { JedeOdDo(it.jede, it.od..it.`do`) }.distinctBy { it.jede to it.v.toString() },
                zcitelnitPevneKody(first().pevneKody),
            )
        }

    suspend fun spojSeZastavkySpojeNaKterychStaviAJedeV(spojId: String) =
        dao.spojSeZastavkySpojeNaKterychStavi(spojId).run {
            val caskody = map { JedeOdDo(it.jede, it.od..it.`do`) }.distinctBy { it.jede to it.v.toString() }
            Triple(
                first().let { LinkaNizkopodlaznostSpojId(it.nizkopodlaznost, it.linka - 325_000, it.spojId) },
                map { CasNazevSpojId(it.cas, it.nazev, it.spojId) }.distinct(),
            ) { datum: Datum ->
                listOf(
                    (caskody.filter { it.jede }.ifEmpty { null }?.any { datum in it.v } ?: true),
                    caskody.filter { !it.jede }.none { datum in it.v },
                    datum.jedeDnes(first().pevneKody),
                ).all { it }
            }
        }

    suspend fun nazvyZastavekLinky(linka: Int) = dao.nazvyZastavekLinky(linka + 325_000)

    suspend fun pristiZastavky(linka: Int, tahleZastavka: String) = dao.pristiZastavky(linka + 325_000, tahleZastavka)

    suspend fun zastavkyJedouciVDatumSPristiZastavkou(linka: Int, zastavka: String, pristiZastavka: String, datum: Datum) =
        dao.zastavkyJedouciVDatumSPristiZastavkou(linka + 325_000, zastavka, pristiZastavka, datum)
            .filter { (_, _, _, pevneKody) ->
                datum.jedeDnes(pevneKody)
            }

    suspend fun zapsat(
        zastavkySpoje: Array<ZastavkaSpoje>,
        zastavky: Array<Zastavka>,
        casKody: Array<CasKod>,
        linky: Array<Linka>,
        spoje: Array<Spoj>,
        ostatni: VsechnoOstatni,
    ) {
        this.ostatni = ostatni

        dao.vlozitZastavkySpoje(*zastavkySpoje)
        dao.vlozitZastavky(*zastavky)
        dao.vlozitCasKody(*casKody)
        dao.vlozitLinky(*linky)
        dao.vlozitSpoje(*spoje)
    }

    fun odstranitVse() {
        db.clearAllTables()
    }

    fun upravitDatum(datum: Datum) {
        _datum.update { datum }
    }

    fun upravitOnlineMod(mod: Boolean) {
        _onlineMod.update { mod }
    }

    fun upravitNastaveni(update: (Nastaveni) -> Nastaveni) {
        _nastaveni.update(update)
        ostatni = ostatni.copy(nastaveni = _nastaveni.value)
    }

    fun zmenitNizkopodlaznost(value: Boolean) {
        _zobrazitNizkopodlaznost.update { value }
        ostatni = ostatni.copy(zobrazitNizkopodlaznost = _zobrazitNizkopodlaznost.value)
    }

    fun pridatOblibeny(id: String) {
        _oblibene.update { it + id }
        ostatni = ostatni.copy(oblibene = _oblibene.value)
    }

    fun odebratOblibeny(id: String) {
        _oblibene.update { it - id }
        ostatni = ostatni.copy(oblibene = _oblibene.value)
    }

    suspend fun spojeJedouciVdatumZastavujiciNaIndexechZastavkySeZastavkySpoje(datum: Datum, zastavka: String): List<ZastavkaSpojeSeSpojemAJehoZastavky> =
        dao.spojeZastavujiciNaIndexechZastavky(zastavka).funguj { 0 }
            .groupBy { "S-${it.linka}-${it.cisloSpoje}" to it.indexZastavkyNaLince }
            .map { Triple(it.key.first, it.key.second, it.value) }.funguj { 2 }
            .filter { (_, _, seznam) ->
                val caskody = seznam.map { JedeOdDo(it.jede, it.od..it.`do`) }.distinctBy { it.jede to it.v.toString() }
                listOf(
                    (caskody.filter { it.jede }.ifEmpty { null }?.any { datum in it.v } ?: true),
                    caskody.filter { !it.jede }.none { datum in it.v },
                    datum.jedeDnes(seznam.first().pevneKody),
                ).all { it }
            }.funguj { 3 }
            .map { Triple(it.first, it.second, it.third.first()) }
            .let { seznam ->
                val zastavkySpoju = dao.zastavkySpoju(seznam.map { it.first })
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
                    zastavkySpoje = zastavky.map { it.nazev to it.cas }
                )
            }.funguj { 4 }

    suspend fun spojSeZastavkamiPodleId(spojId: String): Pair<Spoj, List<NazevACas>> = dao.spojSeZastavkamiPodleId(spojId).toList().first()

    val isOnline = flow {
        while (currentCoroutineContext().isActive) {
            emit(ctx.isOnline)
            delay(5000)
        }
    }.stateIn(scope, SharingStarted.WhileSubscribed(5_000), false)

    val maPristupKJihu = isOnline.combine(onlineMod) { jeOnline, onlineMod ->
        jeOnline && onlineMod
    }.stateIn(scope, SharingStarted.WhileSubscribed(5_000), false)
}

private fun Datum.jedeDnes(pevneKody: String) = pevneKody
    .split(" ")
    .mapNotNull {
        when (it) {
            "1" -> toCalendar()[DAY_OF_WEEK] in 2..6 // jede v pracovních dnech
            "2" -> toCalendar()[DAY_OF_WEEK] == 1 || jeStatniSvatekNeboDenPracovnihoKlidu(this) // jede v neděli a ve státem uznané svátky
            "3" -> toCalendar()[DAY_OF_WEEK] == 2 // jede v pondělí
            "4" -> toCalendar()[DAY_OF_WEEK] == 3 // jede v úterý
            "5" -> toCalendar()[DAY_OF_WEEK] == 4 // jede ve středu
            "6" -> toCalendar()[DAY_OF_WEEK] == 5 // jede ve čtvrtek
            "7" -> toCalendar()[DAY_OF_WEEK] == 6 // jede v pátek
            "8" -> toCalendar()[DAY_OF_WEEK] == 7 // jede v sobotu
            "14" -> null // bezbariérově přístupná zastávka
            "19" -> null // ???
            "24" -> null // spoj s částečně bezbariérově přístupným vozidlem, nutná dopomoc průvodce
            "28" -> null // zastávka s možností přestupu na železniční dopravu
            else -> null
        }
    }.any { it }

private fun jeStatniSvatekNeboDenPracovnihoKlidu(datum: Datum) = listOf(
    Datum(1, 1, 1), // Den obnovy samostatného českého státu
    Datum(1, 1, 1), // Nový rok
    Datum(1, 5, 1), // Svátek práce
    Datum(8, 5, 1), // Den vítězství
    Datum(5, 7, 1), // Den slovanských věrozvěstů Cyrila a Metoděje,
    Datum(6, 7, 1), // Den upálení mistra Jana Husa
    Datum(28, 9, 1), // Den české státnosti
    Datum(28, 10, 1), // Den vzniku samostatného československého státu
    Datum(17, 11, 1), // Den boje za svobodu a demokracii
    Datum(24, 12, 1), // Štědrý den
    Datum(25, 12, 1), // 1. svátek vánoční
    Datum(26, 12, 1), // 2. svátek vánoční
).any {
    it.den == datum.den && it.mesic == datum.mesic
} || jeVelkyPatekNeboVelikonocniPondeli(datum)

private fun jeVelkyPatekNeboVelikonocniPondeli(datum: Datum): Boolean {
    val (velkyPatek, velikonocniPondeli) = polohaVelkehoPatkuAVelikonocnihoPondeliVRoce(datum.rok) ?: return false

    return datum == velikonocniPondeli || datum == velkyPatek
}

fun polohaVelkehoPatkuAVelikonocnihoPondeliVRoce(rok: Int): Pair<Datum, Datum>? {

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
    val velkyPatekOdZacatkuDubna = velkyPatekOdZacatkuBrezna - 31
    val velkyPatekJeVDubnu = velkyPatekOdZacatkuDubna > 0
    val velkyPatek =
        if (velkyPatekJeVDubnu) Datum(velkyPatekOdZacatkuDubna, 4, rok) else Datum(velkyPatekOdZacatkuBrezna, 3, rok)

    val velikonocniPondeliOdZacatkuBrezna = velikonocniNedeleOdZacatkuBrezna + 1
    val velikonocniPondeliOdZacatkuDubna = velikonocniPondeliOdZacatkuBrezna - 31
    val velikonocniPondeliJeVDubnu = velikonocniPondeliOdZacatkuDubna > 0
    val velikonocniPondeli =
        if (velikonocniPondeliJeVDubnu) Datum(velikonocniPondeliOdZacatkuDubna, 4, rok) else Datum(velikonocniPondeliOdZacatkuBrezna, 3, rok)

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
