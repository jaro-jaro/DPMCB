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
import cz.jaro.dpmcb.data.App.Companion.repo
import cz.jaro.dpmcb.data.VsechnoOstatni
import cz.jaro.dpmcb.data.entities.CasKod
import cz.jaro.dpmcb.data.entities.Linka
import cz.jaro.dpmcb.data.entities.Spoj
import cz.jaro.dpmcb.data.entities.Zastavka
import cz.jaro.dpmcb.data.entities.ZastavkaSpoje
import cz.jaro.dpmcb.data.helperclasses.Quadruple
import cz.jaro.dpmcb.data.helperclasses.Smer
import cz.jaro.dpmcb.data.helperclasses.TypyTabulek
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.funguj
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.toCasDivne
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.toDatumDivne
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
import java.io.File
import java.time.LocalDate

class LoadingViewModel(
    private val uri: String?,
    private val update: Boolean,
    private val chyba: (() -> Unit) -> Unit,
    private val potrebaInternet: () -> Unit,
    private val finish: () -> Unit,
    private val schemaFile: File,
    private val jrFile: File,
    private val mainActivityIntent: Intent,
    private val loadingActivityIntent: Intent,
    private val startActivity: (Intent) -> Unit,
    private val packageName: String,
    private val exit: () -> Nothing,
) : ViewModel() {

    private val _state = MutableStateFlow("" to (null as Float?))
    val state = _state.asStateFlow()

    val nastaveni = repo.nastaveni

    init {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                update || repo.verze == -1
            } catch (e: Exception) {
                e.printStackTrace()
                ukazatChybaDialog()
            }

            if (update || repo.verze == -1) {
                stahnoutNoveJizdniRady()
            }

            try {
                fungujeVse()
            } catch (e: Exception) {
                e.printStackTrace()
                e.funguj()
                ukazatChybaDialog()
                5.funguj()
            }
            1.funguj()

            if (uri?.removePrefix("/DPMCB").equals("/app-details")) {
                otevriDetailAplikace()
            }

            val intent = mainActivityIntent

            uri?.let {
                intent.putExtra("link", it.removePrefix("/DPMCB"))
            }

            if (!repo.isOnline.value || !repo.nastaveni.value.kontrolaAktualizaci) {
                finish()
                startActivity(intent)
                return@launch
            }

            val mistniVerze = repo.verze

            _state.update {
                "Kontrola dostupnosti aktualizací" to null
            }

            val database = Firebase.database("https://dpmcb-jaro-default-rtdb.europe-west1.firebasedatabase.app/")
            val reference = database.getReference("data3/verze")

            val onlineVerze = withTimeoutOrNull(3_000) {
                reference.get().await().getValue<Int>()
            } ?: -2

            intent.putExtra("update", mistniVerze < onlineVerze)

            finish()
            startActivity(intent)
        }
    }

    private suspend fun fungujeVse(): Nothing? {
        repo.cislaLinek().ifEmpty {
            throw Exception()
        }
        if (!schemaFile.exists()) {
            throw Exception()
        }
        return null
    }

    private suspend fun ukazatChybaDialog(): Nothing {
        4.funguj()
        coroutineScope {
            withContext(Dispatchers.Main) {
                chyba {
                    startActivity(loadingActivityIntent.apply {
                        flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NO_HISTORY
                        putExtra("update", true)
                    })
                    finish()
                }
            }
        }
        2.funguj()
        while (true) Unit
        3.funguj()
    }

    private fun otevriDetailAplikace(): Nothing {
        finish()
        startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", packageName, null)
            addCategory(Intent.CATEGORY_DEFAULT)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_HISTORY or Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
        })
        exit()
    }

    private fun fungujdeVse() {

    }

    private suspend fun stahnoutNoveJizdniRady() {

        repo.isOnline.first()

        if (repo.isOnline.value) {
            withContext(Dispatchers.Main) {
                potrebaInternet()
            }
            exit()
        }

        _state.update {
            "Aktualizování jízdních řádů.\nTato akce může trvat několik minut.\nProsíme, nevypínejte aplikaci.\nOdstraňování starých dat (1/6)" to null
        }

        repo.odstranitVse()

        _state.update {
            "Aktualizování jízdních řádů.\nTato akce může trvat několik minut.\nProsíme, nevypínejte aplikaci.\nStahování dat (2/6)" to null
        }

        val database = Firebase.database("https://dpmcb-jaro-default-rtdb.europe-west1.firebasedatabase.app/")
        val storage = Firebase.storage
        val schemaRef = storage.reference.child("schema.pdf")
        val jrRef = storage.reference.child("jr-dpmcb.json")
        val referenceVerze = database.getReference("data3/verze")

        _state.update {
            "Aktualizování jízdních řádů.\nTato akce může trvat několik minut.\nProsíme, nevypínejte aplikaci.\nStahování dat (2/6)" to 0F
        }

        val jrFile = jrFile

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
            "Aktualizování jízdních řádů.\nTato akce může trvat několik minut.\nProsíme, nevypínejte aplikaci.\nZpracovávání dat (3/6)" to null
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
            "Aktualizování jízdních řádů.\nTato akce může trvat několik minut.\nProsíme, nevypínejte aplikaci.\nZpracovávání dat (4/6)" to 0F
        }

        data
            .map { it.key.toInt() to it.value }
            .sortedBy { it.first }
            .forEach { (_, dataLinky) ->
                dataLinky.forEach { (typTabulky, tabulka) ->
                    tabulka.forEach radek@{ radek ->
                        indexRadku++

                        _state.update {
                            "Aktualizování jízdních řádů.\nTato akce může trvat několik minut.\nProsíme, nevypínejte aplikaci.\nZpracovávání dat (4/6)" to indexRadku / pocetRadku
                        }

                        when (TypyTabulek.valueOf(typTabulky)) {
                            TypyTabulek.Zasspoje -> zastavkySpoje += ZastavkaSpoje(
                                linka = radek[0].toInt(),
                                cisloSpoje = radek[1].toInt(),
                                indexZastavkyNaLince = radek[2].toInt(),
                                cisloZastavky = radek[3].toInt(),
                                kmOdStartu = radek[9].ifEmpty { null }?.toInt() ?: return@radek,
                                prijezd = radek[10].takeIf { it != "<" }?.takeIf { it != "|" }?.ifEmpty { null }?.toCasDivne(),
                                odjezd = radek[11].takeIf { it != "<" }?.takeIf { it != "|" }?.ifEmpty { null }?.toCasDivne(),
                            )

                            TypyTabulek.Zastavky -> zastavky += Zastavka(
                                linka = radek[0].toInt(),
                                cisloZastavky = radek[1].toInt(),
                                nazevZastavky = radek[2],
                                pevneKody = radek.slice(7..12).filter { it.isNotEmpty() }.joinToString(" "),
                            )

                            TypyTabulek.Caskody -> casKody += CasKod(
                                linka = radek[0].toInt(),
                                cisloSpoje = radek[1].toInt(),
                                kod = radek[3].toInt(),
                                indexTerminu = radek[2].toInt(),
                                jede = radek[4] == "1",
                                platiOd = radek[5].toDatumDivne(),
                                platiDo = radek[6].ifEmpty { radek[5] }.toDatumDivne(),
                            )

                            TypyTabulek.Linky -> linky += Linka(
                                cislo = radek[0].toInt(),
                                trasa = radek[1],
                                typVozidla = Json.decodeFromString("\"${radek[4]}\""),
                                typLinky = Json.decodeFromString("\"${radek[3]}\""),
                                maVyluku = radek[5] != "0",
                                platnostOd = radek[13].toDatumDivne(),
                                platnostDo = radek[14].toDatumDivne(),
                            )

                            TypyTabulek.Spoje -> spoje += Spoj(
                                linka = radek[0].toInt(),
                                cisloSpoje = radek[1].toInt(),
                                pevneKody = radek.slice(2..12).filter { it.isNotEmpty() }.joinToString(" "),
                                smer = Smer.POZITIVNI // POZOR!!! DOČASNÁ HODNOTA!!!
                            )

                            TypyTabulek.Pevnykod -> Unit
                            TypyTabulek.Zaslinky -> Unit
                            TypyTabulek.VerzeJDF -> Unit
                            TypyTabulek.Dopravci -> Unit
                            TypyTabulek.LinExt -> Unit
                            TypyTabulek.Udaje -> Unit
                        }
                    }
                }
            }

        _state.update {
            "Aktualizování jízdních řádů.\nTato akce může trvat několik minut.\nProsíme, nevypínejte aplikaci.\nZpracovávání dat (5/6)" to 0F
        }

        spoje.forEachIndexed { index, spoj ->
            _state.update {
                it.first to index.toFloat() / spoje.count()
            }

            val zast = zastavkySpoje
                .filter { it.cisloSpoje == spoj.cisloSpoje && it.linka == spoj.linka }
                .sortedBy { it.indexZastavkyNaLince }
                .filter { it.cas != null }

            spoje[index] =
                spoj.copy(smer = if (zast.first().cas!! <= zast.last().cas && zast.first().kmOdStartu <= zast.last().kmOdStartu) Smer.POZITIVNI else Smer.NEGATIVNI)

            if (casKody.none { it.cisloSpoje == spoj.cisloSpoje && it.linka == spoj.linka })
                casKody += CasKod(
                    linka = spoj.linka,
                    cisloSpoje = spoj.cisloSpoje,
                    kod = 0,
                    indexTerminu = 0,
                    jede = false,
                    platiOd = LocalDate.of(0, 1, 1),
                    platiDo = LocalDate.of(0, 1, 1),
                )
        }

        _state.update {
            "Aktualizování jízdních řádů.\nTato akce může trvat několik minut.\nProsíme, nevypínejte aplikaci.\nStahování schématu MHD (6/6)" to 0F
        }

        val schemaTask = schemaRef.getFile(schemaFile)

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
                    zastavkySpoje = zastavkySpoje.distinctBy { Triple(it.linka, it.cisloSpoje, it.indexZastavkyNaLince) }.toTypedArray(),
                    zastavky = zastavky.distinctBy { it.linka to it.cisloZastavky }.toTypedArray(),
                    casKody = casKody.distinctBy { Quadruple(it.linka, it.kod, it.cisloSpoje, it.indexTerminu) }.toTypedArray(),
                    linky = linky.distinctBy { it.cislo }.toTypedArray(),
                    spoje = spoje.distinctBy { it.linka to it.cisloSpoje }.toTypedArray(),
                    ostatni = VsechnoOstatni(
                        verze = verze,
                        oblibene = repo.oblibene.value
                    )
                )
            }.join()
        }
    }
}