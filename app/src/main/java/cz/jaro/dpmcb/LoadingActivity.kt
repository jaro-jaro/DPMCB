package cz.jaro.dpmcb

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.database.ktx.database
import com.google.firebase.database.ktx.getValue
import com.google.firebase.ktx.Firebase
import cz.jaro.dpmcb.data.App.Companion.repo
import cz.jaro.dpmcb.data.VsechnoOstatni
import cz.jaro.dpmcb.data.entities.CasKod
import cz.jaro.dpmcb.data.entities.Linka
import cz.jaro.dpmcb.data.entities.Spoj
import cz.jaro.dpmcb.data.entities.Zastavka
import cz.jaro.dpmcb.data.entities.ZastavkaSpoje
import cz.jaro.dpmcb.data.helperclasses.Cas
import cz.jaro.dpmcb.data.helperclasses.Cas.Companion.toCasDivne
import cz.jaro.dpmcb.data.helperclasses.Datum
import cz.jaro.dpmcb.data.helperclasses.Datum.Companion.toDatumDivne
import cz.jaro.dpmcb.data.helperclasses.Quadruple
import cz.jaro.dpmcb.data.helperclasses.Smer
import cz.jaro.dpmcb.data.helperclasses.TypyTabulek
import cz.jaro.dpmcb.data.helperclasses.TypyTabulek.Caskody
import cz.jaro.dpmcb.data.helperclasses.TypyTabulek.Dopravci
import cz.jaro.dpmcb.data.helperclasses.TypyTabulek.LinExt
import cz.jaro.dpmcb.data.helperclasses.TypyTabulek.Linky
import cz.jaro.dpmcb.data.helperclasses.TypyTabulek.Pevnykod
import cz.jaro.dpmcb.data.helperclasses.TypyTabulek.Spoje
import cz.jaro.dpmcb.data.helperclasses.TypyTabulek.Udaje
import cz.jaro.dpmcb.data.helperclasses.TypyTabulek.VerzeJDF
import cz.jaro.dpmcb.data.helperclasses.TypyTabulek.Zaslinky
import cz.jaro.dpmcb.data.helperclasses.TypyTabulek.Zasspoje
import cz.jaro.dpmcb.data.helperclasses.TypyTabulek.Zastavky
import cz.jaro.dpmcb.ui.theme.DPMCBTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlin.system.exitProcess

class LoadingActivity : AppCompatActivity() {

    private var infoText by mutableStateOf("Načítání dat")
    private var progress by mutableStateOf(null as Float?)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            DPMCBTheme {
                Surface(
                    modifier = Modifier.fillMaxSize()
                ) {
                    Column(
                        Modifier
                            .fillMaxSize()
                            .padding(all = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Image(
                            painter = painterResource(R.drawable.logo_transparent),
                            contentDescription = "Logo JARO",
                            contentScale = ContentScale.FillWidth,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text(infoText, textAlign = TextAlign.Center)
                        if (progress == null) {
                            LinearProgressIndicator(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp)
                            )
                        } else {
                            LinearProgressIndicator(
                                progress = progress ?: 0F, modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp)
                            )
                        }
                    }
                }
            }
        }

        lifecycleScope.launch(Dispatchers.IO) {

            try {
                if (intent.getBooleanExtra("update", false) || repo.verze == -1) {
                    stahnoutNoveJizdniRady()
                }
                repo.cislaLinek()
            } catch (e: Exception) {
                e.printStackTrace()
                var lock = true
                MaterialAlertDialogBuilder(this@LoadingActivity).apply {
                    setTitle("Chyba!")
                    setMessage("Zdá se, ža vaše jizdní řády uložené v zařízení jsou poškozené! Chcete stáhnout nové?")
                    setNegativeButton("Ne, zavřít aplikaci") { _, _ ->
                        exitProcess(1)
                    }
                    setPositiveButton("Ano") { _, _ ->
                        launch(Dispatchers.IO) {
                            stahnoutNoveJizdniRady()
                            lock = false
                        }
                    }

                    withContext(Dispatchers.Main) {
                        show()
                    }
                }
                while (lock) Unit
            }

            val intent = Intent(this@LoadingActivity, MainActivity::class.java)

            if (!jeOnline()) {
                finish()
                startActivity(intent)
                return@launch
            }

            val mistniVerze = repo.verze

            infoText = "Kontrola dostupnosti aktualizací"

            val database = Firebase.database("https://dpmcb-jaro-default-rtdb.europe-west1.firebasedatabase.app/")
            val reference = database.getReference("verze")

            val onlineVerze = reference.get().await().getValue<Int>() ?: -2

            intent.putExtra("update", mistniVerze < onlineVerze)

            finish()
            startActivity(intent)
        }
    }

    private suspend fun stahnoutNoveJizdniRady() {

        if (!jeOnline()) {
            Toast.makeText(this, "Na stažení jizdních řádů je potřeba připojení k internetu!", Toast.LENGTH_LONG).show()
            exitProcess(-1)
        }

        infoText = "Aktualizování jízdních řádů.\nTato akce může trvat několik minut.\nProsíme, nevypínejte aplikaci.\nOdstraňování starých dat..."

        repo.odstranitVse()

        infoText = "Aktualizování jízdních řádů.\nTato akce může trvat několik minut.\nProsíme, nevypínejte aplikaci.\nStahování..."

        val database = Firebase.database("https://dpmcb-jaro-default-rtdb.europe-west1.firebasedatabase.app/")
        val referenceData = database.getReference("data2/data")
        val referenceVerze = database.getReference("data2/verze")

        val data = referenceData.get().await()
            .getValue<Map<String, Map<String, List<List<String>>>>>() ?: mapOf()

        val zastavkySpoje: MutableList<ZastavkaSpoje> = mutableListOf()
        val zastavky: MutableList<Zastavka> = mutableListOf()
        val casKody: MutableList<CasKod> = mutableListOf()
        val linky: MutableList<Linka> = mutableListOf()
        val spoje: MutableList<Spoj> = mutableListOf()

        progress = 0F

        val pocetRadku = data
            .toList()
            .flatMap { it0 ->
                it0.second.flatMap {
                    it.value
                }
            }
            .count()
        var indexRadku = 0F

        data
            .filter { it.key.split("-")[1] == "0" }
            .map { it.key.split("-")[0].toInt() to it.value }
            .sortedBy { it.first }
            .forEach { (cisloLinky, dataLinky) ->
                dataLinky.forEach { (typTabulky, tabulka) ->
                    tabulka.forEach radek@{ radek ->
                        indexRadku++

                        infoText = "Aktualizování jízdních řádů.\nTato akce může trvat několik minut.\nProsíme, nevypínejte aplikaci.\nUkládání linky $cisloLinky..."
                        progress = indexRadku / pocetRadku

                        when (TypyTabulek.valueOf(typTabulky)) {
                            Zasspoje -> zastavkySpoje += ZastavkaSpoje(
                                linka = radek[0].toInt(),
                                cisloSpoje = radek[1].toInt(),
                                indexZastavkyNaLince = radek[2].toInt(),
                                cisloZastavky = radek[3].toInt(),
                                kmOdStartu = radek[9].ifEmpty { null }?.toInt() ?: return@radek,
                                prijezd = radek[10].takeIf { it != "<" }?.takeIf { it != "|" }?.ifEmpty { null }?.toCasDivne() ?: Cas.nikdy,
                                odjezd = radek[11].takeIf { it != "<" }?.takeIf { it != "|" }?.ifEmpty { null }?.toCasDivne() ?: Cas.nikdy,
                            )

                            Zastavky -> zastavky += Zastavka(
                                linka = radek[0].toInt(),
                                cisloZastavky = radek[1].toInt(),
                                nazevZastavky = radek[2],
                                pevneKody = radek.slice(7..12).filter { it.isNotEmpty() }.joinToString(" "),
                            )

                            Caskody -> casKody += CasKod(
                                linka = radek[0].toInt(),
                                cisloSpoje = radek[1].toInt(),
                                kod = radek[3].toInt(),
                                indexTerminu = radek[2].toInt(),
                                jede = radek[4] == "1",
                                platiOd = radek[5].toDatumDivne(),
                                platiDo = radek[6].ifEmpty { radek[5] }.toDatumDivne(),
                            )

                            Linky -> linky += Linka(
                                cislo = radek[0].toInt(),
                                trasa = radek[1],
                                typVozidla = Json.decodeFromString("\"${radek[4]}\""),
                                typLinky = Json.decodeFromString("\"${radek[3]}\""),
                                maVyluku = radek[5] != "0",
                                platnostOd = radek[13].toDatumDivne(),
                                platnostDo = radek[14].toDatumDivne(),
                            )

                            Spoje -> spoje += Spoj(
                                linka = radek[0].toInt(),
                                cisloSpoje = radek[1].toInt(),
                                pevneKody = radek.slice(2..12).filter { it.isNotEmpty() }.joinToString(" "),
                                smer = Smer.POZITIVNI // POZOR!!! DOČASNÁ HODNOTA!!!
                            )

                            Pevnykod -> Unit
                            Zaslinky -> Unit
                            VerzeJDF -> Unit
                            Dopravci -> Unit
                            LinExt -> Unit
                            Udaje -> Unit
                        }
                    }
                }
            }

        infoText = "Aktualizování jízdních řádů.\nTato akce může trvat několik minut.\nProsíme, nevypínejte aplikaci.\nDokončování..."

        spoje.forEachIndexed { index, spoj ->
            progress = index.toFloat() / spoje.count()

            val zast = zastavkySpoje.filter { it.cisloSpoje == spoj.cisloSpoje }.sortedBy { it.indexZastavkyNaLince }
            spoje[index] =
                spoj.copy(smer = if (zast.first().cas <= zast.last().cas && zast.first().kmOdStartu <= zast.last().kmOdStartu) Smer.POZITIVNI else Smer.NEGATIVNI)

            if (casKody.none { it.cisloSpoje == spoj.cisloSpoje && it.linka == spoj.linka })
                casKody += CasKod(
                    linka = spoj.linka,
                    cisloSpoje = spoj.cisloSpoje,
                    kod = 0,
                    indexTerminu = 0,
                    jede = false,
                    platiOd = Datum(0, 0, 0),
                    platiDo = Datum(0, 0, 0)
                )
        }

        progress = null

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

    private fun jeOnline(): Boolean {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        val networkCapabilities = connectivityManager.activeNetwork ?: return false
        val activeNetwork = connectivityManager.getNetworkCapabilities(networkCapabilities) ?: return false

        return activeNetwork.hasTransport(
            NetworkCapabilities.TRANSPORT_WIFI
        ) || activeNetwork.hasTransport(
            NetworkCapabilities.TRANSPORT_CELLULAR
        ) || activeNetwork.hasTransport(
            NetworkCapabilities.TRANSPORT_ETHERNET
        )
    }
}
