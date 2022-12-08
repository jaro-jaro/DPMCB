package cz.jaro.dpmcb.data

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.room.Room
import cz.jaro.dpmcb.data.database.AppDatabase
import cz.jaro.dpmcb.data.entities.Spoj
import cz.jaro.dpmcb.data.entities.ZastavkaSpoje
import cz.jaro.dpmcb.data.helperclasses.Cas
import cz.jaro.dpmcb.data.helperclasses.Datum
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.VDP
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.toChar
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.typDne
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class SpojeRepository(ctx: Application) {

    private val coroutineScope = MainScope()
    private val sharedPref: SharedPreferences =
        ctx.getSharedPreferences("PREFS_DPMCB_JARO", Context.MODE_PRIVATE)

    private lateinit var ostatniField: VsechnoOstatni

    private var ostatni: VsechnoOstatni
        get() {
            return if (::ostatniField.isInitialized) ostatniField
            else sharedPref.getString("ostatni", "{}").let { it ?: "{}" }.let { Json.decodeFromString(it) }
        }
        set(value) {
            val json = Json.encodeToString(value)
            sharedPref.edit {
                putString("ostatni", json)
            }
            ostatniField = value
        }

    private lateinit var db: AppDatabase


    private val _typDne = MutableStateFlow(Datum.dnes.typDne)

    val typDne = _typDne.asStateFlow()

    init {
        coroutineScope.launch {
            db = Room.databaseBuilder(ctx, AppDatabase::class.java, "databaaaaze").build()
        }
    }

    private val spojeDao get() = db.spojeDao()

    private val zastavkySpojeDao get() = db.zastavkySpojeDao()
    val verze get() = ostatni.verze

    var idSpoju
        get() = ostatni.idSpoju
        set(value) {
            ostatni = ostatni.copy(idSpoju = value)
        }
    val graphZastavek get() = ostatni.graphZastavek

    suspend fun spoje() = spojeDao.getAll()
    val zastavky get() = ostatni.zastavky
    val historieVyhledavani get() = ostatni.historieVyhledavani
    val cislaLinek get() = ostatni.linkyAJejichZastavky.keys.toList()
    suspend fun zastavkySpoju() = zastavkySpojeDao.getAll()
    suspend fun spojeJedouciVTypDne(typDne: VDP) = spojeDao.findByKurzInExact("${typDne.toChar()}%")
    suspend fun spojeJedouciVTypDneSeZastavkySpoju(typDne: VDP) =
        zastavkySpojeDao.findByKurzInExactJoinSpoj("${typDne.toChar()}%")
            .toList()
            .groupBy({ it.second }, { it.first })

    suspend fun spojeJedouciVTypDneZastavujiciNaZastavceSeZastavkySpoje(typDne: VDP, zastavka: String) =
        zastavkySpojeDao.findByKurzInExactAndIsJoinSpoj("${typDne.toChar()}%", zastavka)
            .toList()
            .groupBy({ it.second }, { it.first })

    suspend fun spoj(spojId: Long) = spojeDao.findById(spojId)
    suspend fun spojSeZastavkySpoje(spojId: Long) =
        zastavkySpojeDao.findBySpojIdJoinSpoj(spojId)
            .let { it.values.first() to it.keys.toList() }

    suspend fun spojSeZastavkySpojeNaKterychStavi(spojId: Long) =
        zastavkySpojeDao.findBySpojIdAndNotCasJoinSpoj(spojId, Cas.nikdy)
            .let { it.values.first() to it.keys.toList() }

    suspend fun spojeKurzu(kurz: String) = spojeDao.findByKurz(kurz)
    suspend fun spojeKurzuSeZastavkySpojeNaKterychStavi(kurz: String) =
        zastavkySpojeDao.findByKurzAndNotCasJoinSpoj(kurz, Cas.nikdy)
            .toList()
            .groupBy({ it.second }, { it.first })

    suspend fun spojeLinky(cisloLinky: Int) = spojeDao.findByLinka(cisloLinky)
    suspend fun spojeLinkyZastavujiciVZastavceSeZastavkamiSpoju(cisloLinky: Int, indexZastavky: Int) =
        zastavkySpojeDao.findByLinkaAndIndexAndNotCasJoinSpoj(cisloLinky, indexZastavky, Cas.nikdy)
            .toList()
            .groupBy({ it.second }, { it.first })

    suspend fun spojeLinkyJedouciVTypDneSeZastavkamiSpoju(cisloLinky: Int, typDne: VDP) =
        zastavkySpojeDao.findByLinkaAndKurzInExactJoinSpoj(cisloLinky, "${typDne.toChar()}%")
            .toList()
            .groupBy({ it.second }, { it.first })

    suspend fun zastavkaSpoje(idZastavky: Long) = zastavkySpojeDao.findById(idZastavky)
    suspend fun zastavkySpoje(spojId: Long) = zastavkySpojeDao.findBySpoj(spojId)
    suspend fun nazvyZastavekSpoje(spojId: Long) = zastavkySpojeDao.findNazvyBySpoj(spojId)

    fun zastavkyLinky(cisloLinky: Int) = ostatni.linkyAJejichZastavky[cisloLinky]!!

    val ostatniData get() = ostatni

    suspend fun zapsat(zastavkySpoju: Array<ZastavkaSpoje>, spoje: Array<Spoj>, ostatni: VsechnoOstatni) {
        this.ostatni = ostatni

        spojeDao.insertAll(*spoje)
        zastavkySpojeDao.insertAll(*zastavkySpoju)
    }

    fun odstranitSpojeAJejichZastavky() {
        while (!::db.isInitialized) Unit
        db.clearAllTables()
    }

    fun upravitTypDne(typ: VDP) {
        _typDne.update { typ }
    }

    fun pridatDoHistorieVyhledavani(start: String, cil: String) {
        val historie = ostatni.historieVyhledavani.reversed().toMutableList()
        historie += (start to cil)
        ostatni = ostatni.copy(historieVyhledavani = historie.reversed().distinct().take(17))
    }
}
