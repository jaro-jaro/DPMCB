package cz.jaro.dpmcb.ui.loading

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.database.ktx.database
import com.google.firebase.database.ktx.getValue
import com.google.firebase.ktx.Firebase
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
import cz.jaro.dpmcb.data.helperclasses.Smer
import cz.jaro.dpmcb.data.helperclasses.TypyTabulek
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.toCasDivne
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.toDatumDivne
import io.github.z4kn4fein.semver.toVersion
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
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
        val mainActivityIntent: Intent,
        val loadingActivityIntent: Intent,
        val startActivity: (Intent) -> Unit,
        val packageName: String,
        val exit: () -> Nothing,
    )

    companion object {
        const val META_VERZE_DAT = 4
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
                e.printStackTrace()
                ukazatChybaDialog()
            }

            if (params.update || repo.verze.first() == -1) {
                stahnoutNoveJizdniRady()
            }

            try {
                fungujeVse()
            } catch (e: Exception) {
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

    private suspend fun jePotrebaAktualizovatData(): Boolean {
        val mistniVerze = repo.verze.first()

        val database = Firebase.database("https://dpmcb-jaro-default-rtdb.europe-west1.firebasedatabase.app/")
        val reference = database.getReference("data${META_VERZE_DAT}/verze")

        val onlineVerze = withTimeoutOrNull(3_000) {
            reference.get().await().getValue<Int>()
        } ?: -2

        return mistniVerze < onlineVerze
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
            return false
        }

        if (response.statusCode() != 200) return false

        val mistniVerze = BuildConfig.VERSION_NAME.toVersion(false)
        val nejnovejsiVerze = response.body().toVersion(false)

        return mistniVerze < nejnovejsiVerze
    }

    private suspend fun stahnoutNoveJizdniRady() {

        if (!repo.isOnline.value) {
            withContext(Dispatchers.Main) {
                params.potrebaInternet()
            }
            params.exit()
        }

        _state.update {
            "Aktualizování jízdních řádů.\nTato akce může trvat několik minut.\nProsíme, nevypínejte aplikaci.\nOdstraňování starých dat (1/4)" to null
        }

        db.clearAllTables()

        _state.update {
            "Aktualizování jízdních řádů.\nTato akce může trvat několik minut.\nProsíme, nevypínejte aplikaci.\nStahování dat (2/4)" to 0F
        }

        val database = Firebase.database("https://dpmcb-jaro-default-rtdb.europe-west1.firebasedatabase.app/")
        val storage = Firebase.storage
        val schemaRef = storage.reference.child("schema.pdf")
        val jrRef = storage.reference.child("data${META_VERZE_DAT}/jr-dpmcb.json")
        val referenceVerze = database.getReference("data${META_VERZE_DAT}/verze")

        _state.update {
            "Aktualizování jízdních řádů.\nTato akce může trvat několik minut.\nProsíme, nevypínejte aplikaci.\nStahování dat (2/4)" to 0F
        }

        val jrFile = params.jrFile

        val jrTask = jrRef.getFile(jrFile)

        jrTask.addOnFailureListener {
            throw it
        }

        jrTask.addOnProgressListener { snapshot ->
            _state.update {
                it.first to snapshot.bytesTransferred.toFloat() / snapshot.totalByteCount
            }
        }

        jrTask.await()

        _state.update {
            "Aktualizování jízdních řádů.\nTato akce může trvat několik minut.\nProsíme, nevypínejte aplikaci.\nZpracovávání dat (3/4)" to 0F
        }

        val json = jrFile.readText()

        val data: Map<String, Map<String, List<List<String>>>> = Json.decodeFromString(json)

        val zastavkySpoje: MutableList<ZastavkaSpoje> = mutableListOf()
        val zastavky: MutableList<Zastavka> = mutableListOf()
        val casKody: MutableList<CasKod> = mutableListOf()
        val linky: MutableList<Linka> = mutableListOf()
        val spoje: MutableList<Spoj> = mutableListOf()

        val pocetRadku = data
            .toList()
            .flatMap { it0 ->
                it0.second.flatMap {
                    it.value
                }
            }
            .count()
        var indexRadku = 0F

        _state.update {
            "Aktualizování jízdních řádů.\nTato akce může trvat několik minut.\nProsíme, nevypínejte aplikaci.\nZpracovávání dat (3/4)" to 0F
        }

        data
            .map { it.key.split("-").let { arr -> "${arr[0].toInt().plus(325_000)}-${arr[1]}" } to it.value }
            .forEach { (tab, dataLinky) ->
                val zastavkySpojeTabulky: MutableList<ZastavkaSpoje> = mutableListOf()
                val casKodyTabulky: MutableList<CasKod> = mutableListOf()
                dataLinky
                    .toList()
                    .sortedBy { (typTabulky, _) ->
                        TypyTabulek.values().indexOf(TypyTabulek.valueOf(typTabulky))
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

                                TypyTabulek.Zastavky -> zastavky += Zastavka(
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

                                TypyTabulek.Linky -> linky += Linka(
                                    cislo = radek[0].toInt(),
                                    trasa = radek[1],
                                    typVozidla = Json.decodeFromString("\"${radek[4]}\""),
                                    typLinky = Json.decodeFromString("\"${radek[3]}\""),
                                    maVyluku = radek[5] != "0",
                                    platnostOd = radek[13].toDatumDivne(),
                                    platnostDo = radek[14].toDatumDivne(),
                                    tab = tab,
                                )

                                TypyTabulek.Spoje -> spoje += Spoj(
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

                zastavkySpoje += zastavkySpojeTabulky
                casKody += casKodyTabulky
            }

        _state.update {
            "Aktualizování jízdních řádů.\nTato akce může trvat několik minut.\nProsíme, nevypínejte aplikaci.\nStahování schématu MHD (4/4)" to 0F
        }

        val schemaTask = schemaRef.getFile(params.schemaFile)

        schemaTask.addOnFailureListener {
            throw it
        }

        schemaTask.addOnProgressListener { snapshot ->
            _state.update {
                it.first to snapshot.bytesTransferred.toFloat() / snapshot.totalByteCount
            }
        }

        schemaTask.await()

        _state.update {
            "Aktualizování jízdních řádů.\nTato akce může trvat několik minut.\nProsíme, nevypínejte aplikaci.\nUkládání" to null
        }

        val verze = referenceVerze.get().await().getValue<Int>() ?: -1

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
                    verze = verze,
                )
            }.join()
        }
    }
}