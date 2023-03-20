package cz.jaro.dpmcb.data

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.room.Room
import cz.jaro.datum_cas.Cas
import cz.jaro.datum_cas.Datum
import cz.jaro.dpmcb.data.database.AppDatabase
import cz.jaro.dpmcb.data.entities.CasKod
import cz.jaro.dpmcb.data.entities.Linka
import cz.jaro.dpmcb.data.entities.Spoj
import cz.jaro.dpmcb.data.entities.Zastavka
import cz.jaro.dpmcb.data.entities.ZastavkaSpoje
import cz.jaro.dpmcb.data.helperclasses.Quadruple
import cz.jaro.dpmcb.data.realtions.CasNazevSpojId
import cz.jaro.dpmcb.data.realtions.JedeOdDo
import cz.jaro.dpmcb.data.realtions.LinkaNizkopodlaznostSpojId
import cz.jaro.dpmcb.data.realtions.NazevACas
import cz.jaro.dpmcb.data.realtions.ZastavkaSpojeSeSpojemAJehoZastavky
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.Calendar.DAY_OF_WEEK

class SpojeRepository(ctx: Application) {

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

    private val _onlineMod = MutableStateFlow(true)
    val onlineMod = _onlineMod.asStateFlow()

    private val _oblibene = MutableStateFlow(ostatni.oblibene)
    val oblibene = _oblibene.asStateFlow()

    private val dao get() = db.dao()
    val verze get() = ostatni.verze

    suspend fun zastavky() = dao.nazvyZastavek()
    suspend fun cislaLinek() = dao.cislaLinek()

    //    suspend fun spojSeZastavkamiPodleJihu(spojNaMape: SpojNaMape) = zastavkySpojeDao.findByLinkaFrstZastavkaZastZastavka(
//        cisloLinky = spojNaMape.lineNumber!! - 325_000,
//        zastavka1 = spojNaMape.dep.upravit(),
//        cas1 = spojNaMape.depTime.toCas(),
//        index1 = zastavkyLinky(spojNaMape.lineNumber - 325_000).indexOfFirst { it.upravit() == spojNaMape.dep.upravit() },
//        zastavka2 = spojNaMape.dest.upravit(),
//        cas2 = spojNaMape.destTime.toCas(),
//        index2 = zastavkyLinky(spojNaMape.lineNumber - 325_000).indexOfFirst { it.upravit() == spojNaMape.dest.upravit() },
//        kurzLike = "${Datum.dnes.typDne.toChar()}%"
//    )
//        .toList()
//        .groupBy({ it.second }, { it.first })
//        .toList()
//        .firstOrNull()
//
//    suspend fun spojeJedouciVTypDne(typDne: VDP) = dao.findByKurzInExact("${typDne.toChar()}%")
//    suspend fun spojeJedouciVTypDneSeZastavkySpoju(typDne: VDP) =
//        zastavkySpojeDao.findByKurzInExactJoinSpoj("${typDne.toChar()}%")
//            .toList()
//            .groupBy({ it.second }, { it.first })
//
//    suspend fun spojeJedouciVTypDneZastavujiciNaZastavceSeZastavkySpoje(typDne: VDP, zastavka: String) =
//        zastavkySpojeDao.findByKurzInExactAndIsCasNotJoinSpoj("${typDne.toChar()}%", zastavka, Cas.nikdy)
//            .toList()
//            .groupBy({ it.second }, { it.first })
//
//    suspend fun spoj(spojId: Long) = dao.findById(spojId)
//    suspend fun spojSeZastavkySpoje(spojId: Long) =
//        zastavkySpojeDao.findBySpojIdJoinSpoj(spojId)
//            .let { it.values.first() to it.keys.toList() }

    private val isNikdy: (Cas?) -> Boolean = { it == Cas.nikdy }

    suspend fun spojSeZastavkySpojeNaKterychStaviACaskody(spojId: String) =
        dao.spojSeZastavkySpojeNaKterychStavi(spojId).run {
            Quadruple(
                first().let { LinkaNizkopodlaznostSpojId(it.nizkopodlaznost, it.linka - 325_000, it.spojId) },
                map { CasNazevSpojId(it.cas, it.nazev, it.spojId) }.distinct(),
                map { JedeOdDo(it.jede, it.od..it.do_) }.distinctBy { it.jede to it.v.toString() },
                zcitelnitPevneKody(first().pevneKody),
            )
        }

    suspend fun spojSeZastavkySpojeNaKterychStaviAJedeV(spojId: String) =
        dao.spojSeZastavkySpojeNaKterychStavi(spojId).run {
            val caskody = map { JedeOdDo(it.jede, it.od..it.do_) }.distinctBy { it.jede to it.v.toString() }
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

//
//    suspend fun spojeKurzu(kurz: String) = dao.findByKurz(kurz)
//    suspend fun spojeKurzuSeZastavkySpojeNaKterychStavi(kurz: String) =
//        zastavkySpojeDao.findByKurzAndNotCasJoinSpoj(kurz, Cas.nikdy)
//            .toList()
//            .groupBy({ it.second }, { it.first })
//
//    suspend fun spojeLinky(cisloLinky: Int) = dao.findByLinka(cisloLinky)
//    suspend fun spojeLinkyZastavujiciVZastavceSeZastavkamiSpoju(cisloLinky: Int, indexZastavky: Int) =
//        zastavkySpojeDao.findByLinkaAndIndexAndNotCasJoinSpoj(cisloLinky, indexZastavky, Cas.nikdy)
//            .toList()
//            .groupBy({ it.second }, { it.first })
//
//    suspend fun spojeLinkyJedouciVTypDneSeZastavkamiSpoju(cisloLinky: Int, typDne: VDP) =
//        zastavkySpojeDao.findByLinkaAndKurzInExactJoinSpoj(cisloLinky, "${typDne.toChar()}%")
//            .toList()
//            .groupBy({ it.second }, { it.first })
//
//    suspend fun zastavkaSpoje(idZastavky: Long) = zastavkySpojeDao.findById(idZastavky)
//    suspend fun zastavkySpoje(spojId: Long) = zastavkySpojeDao.findBySpoj(spojId)
//    suspend fun nazvyZastavekSpoje(spojId: Long) = zastavkySpojeDao.findNazvyBySpoj(spojId)
//
//    fun zastavkyLinky(cisloLinky: Int) = ostatni.linkyAJejichZastavky[cisloLinky]!!

    suspend fun nazvyZastavekLinky(linka: Int) = dao.nazvyZastavekLinky(linka + 325_000)

    suspend fun pristiZastavky(linka: Int, tahleZastavka: String) = dao.pristiZastavky(linka + 325_000, tahleZastavka)

    suspend fun zastavkyJedouciVDatumSPristiZastavkou(linka: Int, zastavka: String, pristiZastavka: String, datum: Datum) =
        dao.zastavkyJedouciVDatumSPristiZastavkou(linka + 325_000, zastavka, pristiZastavka, datum)
            .filter { (_, _, _, pevneKody) ->
                datum.jedeDnes(pevneKody)
            }

    private fun Datum.jedeDnes(pevneKody: String) = pevneKody
        .split(" ")
        .mapNotNull {
            when (it) {
                "1" -> toCalendar()[DAY_OF_WEEK] in 2..6 // jede v pracovních dnech
                "2" -> toCalendar()[DAY_OF_WEEK] == 1 // jede v neděli a ve státem uznané svátky
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

    fun pridatOblibeny(id: String) {
        _oblibene.update { it + id }
        ostatni = ostatni.copy(oblibene = _oblibene.value)
    }

    fun odebratOblibeny(id: String) {
        _oblibene.update { it - id }
        ostatni = ostatni.copy(oblibene = _oblibene.value)
    }

    suspend fun spojeJedouciVdatumZastavujiciNaIndexechZastavkySeZastavkySpoje(datum: Datum, zastavka: String): List<ZastavkaSpojeSeSpojemAJehoZastavky> =
        dao.spojeJedouciVdatumZastavujiciNaIndexechZastavkySeZastavkySpoje(datum, zastavka)
            .filter {
                datum.jedeDnes(it.pevneKody)
            }
            .groupBy { "S-${it.linka}-${it.cisloSpoje}" to it.indexZastavkyNaLince }
            .map { Triple(it.key.first, it.key.second, it.value) }
            .map { (spojId, indexZastavkyNaLince, seznam) ->
                ZastavkaSpojeSeSpojemAJehoZastavky(
                    nazev = seznam.first().nazev,
                    cas = seznam.first().cas,
                    indexZastavkyNaLince = indexZastavkyNaLince,
                    spojId = spojId,
                    linka = seznam.first().linka - 325_000,
                    nizkopodlaznost = seznam.first().nizkopodlaznost,
                    zastavkySpoje = seznam.map { it.jinaZastavkaSpojeNazev to it.jinaZastavkaSpojeCas }
                )
            }

    suspend fun spojSeZastavkamiPodleId(spojId: String): Pair<Spoj, List<NazevACas>> = dao.spojSeZastavkamiPodleId(spojId).toList().first()
}
