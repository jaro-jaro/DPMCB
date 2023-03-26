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

    private val _onlineMod = MutableStateFlow(true)
    val onlineMod = _onlineMod.asStateFlow()

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

    fun upravitOnlineMod(mod: Boolean) {
        _onlineMod.update { mod }
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