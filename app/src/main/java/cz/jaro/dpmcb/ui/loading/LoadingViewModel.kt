package cz.jaro.dpmcb.ui.loading

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.annotation.Keep
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.crashlytics.ktx.crashlytics
import com.google.firebase.database.GenericTypeIndicator
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.ktx.storage
import cz.jaro.dpmcb.BuildConfig
import cz.jaro.dpmcb.data.SpojeRepository
import cz.jaro.dpmcb.data.database.AppDatabase
import cz.jaro.dpmcb.data.entities.CasKod
import cz.jaro.dpmcb.data.entities.Linka
import cz.jaro.dpmcb.data.entities.Spoj
import cz.jaro.dpmcb.data.entities.Zastavka
import cz.jaro.dpmcb.data.entities.ZastavkaSpoje
import cz.jaro.dpmcb.data.helperclasses.Quadruple
import cz.jaro.dpmcb.data.helperclasses.Quintuple
import cz.jaro.dpmcb.data.helperclasses.Smer
import cz.jaro.dpmcb.data.helperclasses.TypyTabulek
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.toCasDivne
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.toDatumDivne
import io.github.z4kn4fein.semver.toVersion
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.jsoup.Jsoup
import org.koin.android.annotation.KoinViewModel
import org.koin.core.annotation.InjectedParam
import java.io.File
import java.net.SocketTimeoutException
import java.time.LocalDate

@KoinViewModel
class LoadingViewModel(
    private val repo: SpojeRepository,
    private val db: AppDatabase,
    @InjectedParam private val params: Parameters,
) : ViewModel() {

    data class Parameters(
        val uri: String?,
        val update: Boolean,
        val chyba: (() -> Unit) -> Unit,
        val potrebaInternet: () -> Unit,
        val finish: () -> Unit,
        val schemaFile: File,
        val jrFile: File,
        val kurzyFile: File,
        val mainActivityIntent: Intent,
        val loadingActivityIntent: Intent,
        val startActivity: (Intent) -> Unit,
        val packageName: String,
        val exit: () -> Nothing,
    )

    companion object {
        const val META_VERZE_DAT = 5
        const val EXTRA_KEY_AKTUALIZOVAT_DATA = "aktualizovat-data"
        const val EXTRA_KEY_AKTUALIZOVAT_APLIKACI = "aktualizovat-aplikaci"
        const val EXTRA_KEY_DEEPLINK = "link"
    }

    private val _state = MutableStateFlow("" to (null as Float?))
    val state = _state.asStateFlow()

    val nastaveni = repo.nastaveni

    init {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                params.update || repo.verze.first() == -1
            } catch (e: Exception) {
                Firebase.crashlytics.recordException(e)
                e.printStackTrace()
                ukazatChybaDialog()
            }

            if (params.update || repo.verze.first() == -1) {
                stahnoutNoveJizdniRady()
            }

            try {
                fungujeVse()
            } catch (e: Exception) {
                Firebase.crashlytics.recordException(e)
                e.printStackTrace()
                ukazatChybaDialog()
            }

            val intent = vyresitOdkaz(params.mainActivityIntent)

            if (!repo.isOnline.value || !repo.nastaveni.value.kontrolaAktualizaci) {
                params.finish()
                params.startActivity(intent)
                return@launch
            }

            _state.update {
                "Kontrola dostupnosti aktualizací" to null
            }

            intent.putExtra(EXTRA_KEY_AKTUALIZOVAT_DATA, jePotrebaAktualizovatData())
            intent.putExtra(EXTRA_KEY_AKTUALIZOVAT_APLIKACI, jePotrebaAktualizovatAplikaci())

            params.finish()
            params.startActivity(intent)
        }
    }

    private suspend fun fungujeVse(): Nothing? {
        repo.cislaLinek(LocalDate.now()).ifEmpty {
            throw Exception()
        }
        if (!params.schemaFile.exists()) {
            throw Exception()
        }
        return null
    }

    private suspend fun ukazatChybaDialog(): Nothing {
        coroutineScope {
            withContext(Dispatchers.Main) {
                params.chyba {
                    params.startActivity(params.loadingActivityIntent.apply {
                        flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NO_HISTORY
                        putExtra("update", true)
                    })
                    params.finish()
                }
            }
        }
        println() // !!! DO NOT REMOVE !!! DOESN'T WORK WITHOUT IT !!! *** !!! NEODSTRAŇOVAT !!! BEZ TOHO TO NEFUNGUJE !!!
        while (true) Unit
    }

    private fun vyresitOdkaz(baseIntent: Intent): Intent {
        if (params.uri?.removePrefix("/DPMCB").equals("/app-details")) {
            otevriDetailAplikace()
        }

        params.uri?.let {
            baseIntent.putExtra(EXTRA_KEY_DEEPLINK, it.removePrefix("/DPMCB"))
        }

        return baseIntent
    }

    private fun otevriDetailAplikace(): Nothing {
        params.finish()
        params.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", params.packageName, null)
            addCategory(Intent.CATEGORY_DEFAULT)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_HISTORY or Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
        })
        params.exit()
    }

    @Keep
    object TI : GenericTypeIndicator<Int>()

    private suspend fun jePotrebaAktualizovatData(): Boolean {
        val mistniVerze = repo.verze.first()

        val database = Firebase.database("https://dpmcb-jaro-default-rtdb.europe-west1.firebasedatabase.app/")
        val reference = database.getReference("data${META_VERZE_DAT}/verze")

        val onlineVerze = viewModelScope.async {
            withTimeoutOrNull(3_000) {
                reference.get().await().getValue(TI)
            } ?: -2
        }

        return mistniVerze < onlineVerze.await()
    }

    private suspend fun jePotrebaAktualizovatAplikaci(): Boolean {
        val jeDebug = BuildConfig.DEBUG

        if (jeDebug) return false

        val response = try {
            withContext(Dispatchers.IO) {
                Jsoup
                    .connect("https://raw.githubusercontent.com/jaro-jaro/DPMCB/main/app/version.txt")
                    .ignoreContentType(true)
                    .maxBodySize(0)
                    .execute()
            }
        } catch (e: SocketTimeoutException) {
            Firebase.crashlytics.recordException(e)
            return false
        }

        if (response.statusCode() != 200) return false

        val mistniVerze = BuildConfig.VERSION_NAME.toVersion(false)
        val nejnovejsiVerze = response.body().toVersion(false)

        return mistniVerze < nejnovejsiVerze
    }

    private suspend fun stahnoutSoubor(ref: StorageReference, file: File): File {

        val task = ref.getFile(file)

        task.addOnFailureListener {
            throw it
        }

        task.addOnProgressListener { snapshot ->
            _state.update {
                it.first to snapshot.bytesTransferred.toFloat() / snapshot.totalByteCount
            }
        }

        task.await()

        return file
    }

    private suspend fun stahnoutNoveJizdniRady() {

        if (!repo.isOnline.value) {
            withContext(Dispatchers.Main) {
                params.potrebaInternet()
            }
            params.exit()
        }

        _state.update {
            "Aktualizování jízdních řádů.\nTato akce může trvat několik minut.\nProsíme, nevypínejte aplikaci.\nAnalyzování nové verze (0/?)" to null
        }

        val database = Firebase.database("https://dpmcb-jaro-default-rtdb.europe-west1.firebasedatabase.app/")
        val verzeRef = database.getReference("data${META_VERZE_DAT}/verze")
        val novaVerze = verzeRef.get().await().getValue(TI) ?: -1
        val aktualniVerze = repo.verze.first()

        val udelameUplnouAktualizaci = aktualniVerze + 1 != novaVerze

        val zastavkySpoje: MutableList<ZastavkaSpoje> = mutableListOf()
        val zastavky: MutableList<Zastavka> = mutableListOf()
        val casKody: MutableList<CasKod> = mutableListOf()
        val linky: MutableList<Linka> = mutableListOf()
        val spoje: MutableList<Spoj> = mutableListOf()

        if (udelameUplnouAktualizaci) {

            _state.update {
                "Aktualizování jízdních řádů.\nTato akce může trvat několik minut.\nProsíme, nevypínejte aplikaci.\nOdstraňování starých dat (1/5)" to null
            }

            db.clearAllTables()

            _state.update {
                "Aktualizování jízdních řádů.\nTato akce může trvat několik minut.\nProsíme, nevypínejte aplikaci.\nStahování dat (2/5)" to 0F
            }

            val storage = Firebase.storage
            val kurzyRef = storage.reference.child("kurzy.json")
            val navaznostiRef = storage.reference.child("navaznosti.json")
            val schemaRef = storage.reference.child("schema.pdf")
            val jrRef = storage.reference.child("data${META_VERZE_DAT}/data${novaVerze}.json")

            val jrFile = params.jrFile

            val json = stahnoutSoubor(
                ref = jrRef,
                file = jrFile,
            ).readText()

            _state.update {
                "Aktualizování jízdních řádů.\nTato akce může trvat několik minut.\nProsíme, nevypínejte aplikaci.\nStahování kurzů (3/5)" to 0F
            }

            val kurzyFile = params.kurzyFile

            val json2 = stahnoutSoubor(
                ref = kurzyRef,
                file = kurzyFile,
            ).readText()

            val kurzy = json2.fromJson<Map<String, List<String>>>()

            _state.update {
                "Aktualizování jízdních řádů.\nTato akce může trvat několik minut.\nProsíme, nevypínejte aplikaci.\nZpracovávání dat (4/5)" to 0F
            }

            val data: Map<String, Map<String, List<List<String>>>> = Json.decodeFromString(json)

            data.extrahovatData(kurzy)
                .forEach { (zastavkySpojeTabulky, casKodyTabulky, zastavkyTabulky, linkyTabulky, spojeTabulky) ->
                    zastavkySpoje += zastavkySpojeTabulky
                    casKody += casKodyTabulky
                    zastavky += zastavkyTabulky
                    linky += linkyTabulky
                    spoje += spojeTabulky
                }

            _state.update {
                "Aktualizování jízdních řádů.\nTato akce může trvat několik minut.\nProsíme, nevypínejte aplikaci.\nStahování schématu MHD (5/5)" to 0F
            }

            stahnoutSchema(schemaRef)
        } else {
            _state.update {
                "Aktualizování jízdních řádů.\nTato akce může trvat několik minut.\nProsíme, nevypínejte aplikaci.\nStahování aktualizačního balíčku (1/?)" to 0F
            }

            val storage = Firebase.storage
            val kurzyRef = storage.reference.child("kurzy.json")
            val schemaRef = storage.reference.child("schema.pdf")
            val zmenyRef = storage.reference.child("data${META_VERZE_DAT}/zmeny$aktualniVerze..$novaVerze.json")

            val jrFile = params.jrFile

            val json = stahnoutSoubor(
                ref = zmenyRef,
                file = jrFile,
            ).readText()

            val hodneZmen = Json.parseToJsonElement(json).jsonObject

            val plus = hodneZmen["+"]!!.jsonObject.toString().fromJson<Map<String, Map<String, List<List<String>>>>>()
            val minus = hodneZmen["-"]!!.jsonArray.toString().fromJson<List<String>>()
            val zmeny = hodneZmen["Δ"]!!.jsonObject.toString().fromJson<Map<String, Map<String, List<List<String>>>>>()
            val menitSchema = hodneZmen["Δ\uD83D\uDDFA"]!!.jsonPrimitive.boolean

            val minusTabulky = minus
                .map { it.split("-").let { arr -> "${arr[0].toInt().plus(325_000)}-${arr[1]}" } }
            val zmenyTabulky = zmeny
                .map { it.key.split("-").let { arr -> "${arr[0].toInt().plus(325_000)}-${arr[1]}" } }

            zastavkySpoje.addAll(repo.zastavkySpoje())
            zastavky.addAll(repo.zastavky())
            casKody.addAll(repo.casKody())
            linky.addAll(repo.linky())
            spoje.addAll(repo.spoje())

            val N = when {
                menitSchema -> 6
                else -> 5
            }

            _state.update {
                "Aktualizování jízdních řádů.\nTato akce může trvat několik minut.\nProsíme, nevypínejte aplikaci.\nZpracovávání odstraněných jízdních řádů (2/$N)" to 0F
            }

            zastavkySpoje.removeAll { it.tab in minusTabulky || it.tab in zmenyTabulky }
            _state.update { it.first to 1 / 5F }
            zastavky.removeAll { it.tab in minusTabulky || it.tab in zmenyTabulky }
            _state.update { it.first to 2 / 5F }
            casKody.removeAll { it.tab in minusTabulky || it.tab in zmenyTabulky }
            _state.update { it.first to 3 / 5F }
            linky.removeAll { it.tab in minusTabulky || it.tab in zmenyTabulky }
            _state.update { it.first to 4 / 5F }
            spoje.removeAll { it.tab in minusTabulky || it.tab in zmenyTabulky }
            _state.update { it.first to 5 / 5F }

            _state.update {
                "Aktualizování jízdních řádů.\nTato akce může trvat několik minut.\nProsíme, nevypínejte aplikaci.\nStahování kurzů (3/$N)" to 0F
            }

            val kurzyFile = params.kurzyFile

            val json2 = stahnoutSoubor(
                ref = kurzyRef,
                file = kurzyFile,
            ).readText()

            val kurzy = json2.fromJson<Map<String, List<String>>>()

            _state.update {
                "Aktualizování jízdních řádů.\nTato akce může trvat několik minut.\nProsíme, nevypínejte aplikaci.\nZpracovávání nových jízdních řádů (4/$N)" to 0F
            }

            plus.extrahovatData(kurzy)
                .forEach { (zastavkySpojeTabulky, casKodyTabulky, zastavkyTabulky, linkyTabulky, spojeTabulky) ->
                    zastavkySpoje += zastavkySpojeTabulky
                    casKody += casKodyTabulky
                    zastavky += zastavkyTabulky
                    linky += linkyTabulky
                    spoje += spojeTabulky
                }

            _state.update {
                "Aktualizování jízdních řádů.\nTato akce může trvat několik minut.\nProsíme, nevypínejte aplikaci.\nZpracovávání změněných jízdních řádů (5/$N)" to 0F
            }

            zmeny.extrahovatData(kurzy)
                .forEach { (zastavkySpojeTabulky, casKodyTabulky, zastavkyTabulky, linkyTabulky, spojeTabulky) ->
                    zastavkySpoje += zastavkySpojeTabulky
                    casKody += casKodyTabulky
                    zastavky += zastavkyTabulky
                    linky += linkyTabulky
                    spoje += spojeTabulky
                }

            if (menitSchema) {
                _state.update {
                    "Aktualizování jízdních řádů.\nTato akce může trvat několik minut.\nProsíme, nevypínejte aplikaci.\nStahování schématu MHD (6/6)" to 0F
                }

                stahnoutSchema(schemaRef)
            }
        }

        _state.update {
            "Aktualizování jízdních řádů.\nTato akce může trvat několik minut.\nProsíme, nevypínejte aplikaci.\nUkládání" to null
        }

        println(spoje)
        println(linky)
        println(zastavky)
        println(zastavkySpoje)
        println(casKody)

        coroutineScope {
            launch {
                repo.zapsat(
                    zastavkySpoje = zastavkySpoje.distinctBy { Triple(it.tab, it.cisloSpoje, it.indexZastavkyNaLince) }.toTypedArray(),
                    zastavky = zastavky.distinctBy { it.tab to it.cisloZastavky }.toTypedArray(),
                    casKody = casKody.distinctBy { Quadruple(it.tab, it.kod, it.cisloSpoje, it.indexTerminu) }.toTypedArray(),
                    linky = linky.distinctBy { it.tab }.toTypedArray(),
                    spoje = spoje.distinctBy { it.tab to it.cisloSpoje }.toTypedArray(),
                    verze = novaVerze,
                )
            }.join()
        }
    }

    private suspend fun stahnoutSchema(schemaRef: StorageReference) = stahnoutSoubor(schemaRef, params.schemaFile)

    private fun Map<String, Map<String, List<List<String>>>>.extrahovatData(
        kurzy: Map<String, List<String>>,
    ): List<Quintuple<MutableList<ZastavkaSpoje>, MutableList<CasKod>, MutableList<Zastavka>, MutableList<Linka>, MutableList<Spoj>>> {

        val pocetRadku = this
            .toList()
            .flatMap { it0 ->
                it0.second.flatMap {
                    it.value
                }
            }
            .count()

        var indexRadku = 0F

        return this
            .map { it.key.split("-").let { arr -> "${arr[0].toInt().plus(325_000)}-${arr[1]}" } to it.value }
            .map { (tab, dataLinky) ->
                val zastavkySpojeTabulky: MutableList<ZastavkaSpoje> = mutableListOf()
                val casKodyTabulky: MutableList<CasKod> = mutableListOf()
                val zastavkyTabulky: MutableList<Zastavka> = mutableListOf()
                val linkyTabulky: MutableList<Linka> = mutableListOf()
                val spojeTabulky: MutableList<Spoj> = mutableListOf()
                dataLinky
                    .toList()
                    .sortedBy { (typTabulky, _) ->
                        TypyTabulek.entries.indexOf(TypyTabulek.valueOf(typTabulky))
                    }
                    .forEach { (typTabulky, tabulka) ->
                        tabulka.forEach radek@{ radek ->
                            indexRadku++

                            _state.update {
                                it.first to indexRadku / pocetRadku
                            }

                            when (TypyTabulek.valueOf(typTabulky)) {
                                TypyTabulek.Zasspoje -> zastavkySpojeTabulky += ZastavkaSpoje(
                                    linka = radek[0].toInt(),
                                    cisloSpoje = radek[1].toInt(),
                                    indexZastavkyNaLince = radek[2].toInt(),
                                    cisloZastavky = radek[3].toInt(),
                                    kmOdStartu = radek[9].ifEmpty { null }?.toInt() ?: return@radek,
                                    prijezd = radek[10].takeIf { it != "<" }?.takeIf { it != "|" }?.ifEmpty { null }?.toCasDivne(),
                                    odjezd = radek[11].takeIf { it != "<" }?.takeIf { it != "|" }?.ifEmpty { null }?.toCasDivne(),
                                    tab = tab,
                                )

                                TypyTabulek.Zastavky -> zastavkyTabulky += Zastavka(
                                    linka = radek[0].toInt(),
                                    cisloZastavky = radek[1].toInt(),
                                    nazevZastavky = radek[2],
                                    pevneKody = radek.slice(7..12).filter { it.isNotEmpty() }.joinToString(" "),
                                    tab = tab,
                                )

                                TypyTabulek.Caskody -> casKodyTabulky += CasKod(
                                    linka = radek[0].toInt(),
                                    cisloSpoje = radek[1].toInt(),
                                    kod = radek[3].toInt(),
                                    indexTerminu = radek[2].toInt(),
                                    jede = radek[4] == "1",
                                    platiOd = radek[5].toDatumDivne(),
                                    platiDo = radek[6].ifEmpty { radek[5] }.toDatumDivne(),
                                    tab = tab,
                                )

                                TypyTabulek.Linky -> linkyTabulky += Linka(
                                    cislo = radek[0].toInt(),
                                    trasa = radek[1],
                                    typVozidla = Json.decodeFromString("\"${radek[4]}\""),
                                    typLinky = Json.decodeFromString("\"${radek[3]}\""),
                                    maVyluku = radek[5] != "0",
                                    platnostOd = radek[13].toDatumDivne(),
                                    platnostDo = radek[14].toDatumDivne(),
                                    tab = tab,
                                )

                                TypyTabulek.Spoje -> spojeTabulky += Spoj(
                                    linka = radek[0].toInt(),
                                    cisloSpoje = radek[1].toInt(),
                                    pevneKody = radek.slice(2..12).filter { it.isNotEmpty() }.joinToString(" "),
                                    smer = zastavkySpojeTabulky
                                        .filter { it.cisloSpoje == radek[1].toInt() }
                                        .sortedBy { it.indexZastavkyNaLince }
                                        .filter { it.cas != null }
                                        .let { zast ->
                                            Smer.fromBoolean(zast.first().cas!! <= zast.last().cas && zast.first().kmOdStartu <= zast.last().kmOdStartu)
                                        },
                                    tab = tab,
                                    kurz = kurzy.toList().firstOrNull { (_, spoje) -> "S-${radek[0]}-${radek[1]}" in spoje }?.first,
                                    poradiNaKurzu = kurzy.toList().indexOfFirst { (_, spoje) -> "S-${radek[0]}-${radek[1]}" in spoje }.takeUnless { it == -1 },
                                ).also { spoj ->
                                    if (casKodyTabulky.none { it.cisloSpoje == spoj.cisloSpoje })
                                        casKodyTabulky += CasKod(
                                            linka = spoj.linka,
                                            cisloSpoje = spoj.cisloSpoje,
                                            kod = 0,
                                            indexTerminu = 0,
                                            jede = false,
                                            platiOd = LocalDate.of(0, 1, 1),
                                            platiDo = LocalDate.of(0, 1, 1),
                                            tab = spoj.tab,
                                        )
                                }

                                TypyTabulek.Pevnykod -> Unit
                                TypyTabulek.Zaslinky -> Unit
                                TypyTabulek.VerzeJDF -> Unit
                                TypyTabulek.Dopravci -> Unit
                                TypyTabulek.LinExt -> Unit
                                TypyTabulek.Udaje -> Unit
                            }
                        }
                    }
                Quintuple(zastavkySpojeTabulky, casKodyTabulky, zastavkyTabulky, linkyTabulky, spojeTabulky)
            }
    }

    inline fun <reified T> String.fromJson(): T = Json.decodeFromString<T>(this)
}