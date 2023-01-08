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
import cz.jaro.dpmcb.data.GraphZastavek
import cz.jaro.dpmcb.data.VsechnoOstatni
import cz.jaro.dpmcb.data.entities.Spoj
import cz.jaro.dpmcb.data.entities.ZastavkaSpoje
import cz.jaro.dpmcb.data.helperclasses.Cas
import cz.jaro.dpmcb.data.helperclasses.Cas.Companion.toCas
import cz.jaro.dpmcb.data.helperclasses.Smer.NEGATIVNI
import cz.jaro.dpmcb.data.helperclasses.Smer.POZITIVNI
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.emptyGraphZastavek
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.reversedIf
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.toGraphZastavek
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.toMutableGraphZastavek
import cz.jaro.dpmcb.ui.theme.DPMCBTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.set
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
            } catch (_: Exception) {
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

        repo.odstranitSpojeAJejichZastavky()

        infoText = "Aktualizování jízdních řádů.\nTato akce může trvat několik minut.\nProsíme, nevypínejte aplikaci.\nStahování..."

        val database = Firebase.database("https://dpmcb-jaro-default-rtdb.europe-west1.firebasedatabase.app/")
        val referenceData = database.getReference("data")
        val referenceLinky = database.getReference("linky")
        val referenceVerze = database.getReference("verze")

        val jr = referenceData.get().await()
            .getValue<Map<String, Map<String, Map<String, List<List<String>>>>>>() ?: mapOf()
        val zastavkyLinek = referenceLinky.get().await()
            .getValue<Map<String, List<String>>>() ?: mapOf()

        val budouciData = mutableMapOf<Int, MutableList<List<String>>>()

        progress = 0F

        jr.forEach { (_, linkyDanehoVdp) -> //pro vsechny vdp
            linkyDanehoVdp.forEach { (cisloLinky, linka) ->

                if (!budouciData.containsKey(cisloLinky.toInt())) {
                    budouciData[cisloLinky.toInt()] = mutableListOf()
                }

                linka.forEach { (plusminus, spoje) ->
                    spoje.forEach { spoj ->

                        budouciData[cisloLinky.toInt()]!! +=
                            if (plusminus == "+")
                                listOf("+") +
                                        spoj
                            else
                                listOf("-") +
                                        spoj.filterIndexed { i, _ -> i <= 2 } +
                                        spoj.filterIndexed { i, _ -> i > 2 }.reversed()

                    }
                }
            }
        }

        val pocetZastavekCelkove = budouciData
            .toList()
            .flatMap { (_, spoje) ->
                spoje.flatten()
            }
            .count()
        var indexZastavky = 0F

        var idSpoje = 1L
        var idZastavkySCasem = 1L

        val linky = mutableMapOf<Int, List<String>>()
        val zastavky = mutableSetOf<String>()
        val spoje = mutableListOf<Spoj>()
        val zastavkySpoju = mutableListOf<ZastavkaSpoje>()

        budouciData.toList().sortedBy { it.first }.forEach { (cisloLinky, spojeZDat) ->

            spojeZDat.forEach { spoj ->

                spoje += Spoj(
                    nizkopodlaznost = spoj[1].toBooleanStrict(),
                    nazevKurzu = spoj[2],
                    vyjmecnosti = spoj[3].toInt(),
                    smer = if (spoj[0] == "+") POZITIVNI else NEGATIVNI,
                    cisloLinky = cisloLinky,
                    id = idSpoje
                )

                spoj
                    .filterIndexed { i, _ -> i > 3 }
                    .mapIndexed { i, cas ->
                        //println(cisloLinky)
                        //println(zastavkyLinek[cisloLinky.toString()])
                        //println(i to cas)
                        Triple(zastavkyLinek[cisloLinky.toString()]!![i], cas.toCas(), i)
                    }
                    .forEach { (zastavka, cas, indexZastavkyNaLince) ->
                        indexZastavky++

                        infoText = "Aktualizování jízdních řádů.\nTato akce může trvat několik minut.\nProsíme, nevypínejte aplikaci.\nUkládání linky $cisloLinky..."
                        progress = indexZastavky / pocetZastavekCelkove

                        val novyId = idZastavkySCasem
                        idZastavkySCasem++

                        zastavkySpoju += ZastavkaSpoje(
                            id = novyId,
                            nazevZastavky = zastavka,
                            idSpoje = idSpoje,
                            cisloLinky = cisloLinky,
                            nazevKurzu = spoj[2],
                            cas = cas,
                            indexNaLince = indexZastavkyNaLince,
                            nizkopodlaznost = spoj[1].toBooleanStrict()
                        )

                        zastavky += zastavka
                    }

                idSpoje++
            }

            linky += Pair(
                cisloLinky,
                zastavkyLinek[cisloLinky.toString()]!!,
            )
        }

        progress = null
        infoText = "Aktualizování jízdních řádů.\nTato akce může trvat několik minut.\nProsíme, nevypínejte aplikaci.\nDokončování..."

        val verze = referenceVerze.get().await().getValue<Int>() ?: -1

        val graphZastavek = vytvoritGraf(zastavkySpoju.groupBy({ zs -> spoje.find { it.id == zs.idSpoje }!! }, { it }))

        println(spoje)
        println(linky)
        println(zastavky)
        println(zastavkySpoju)
        println(graphZastavek)

        coroutineScope {
            launch {
                repo.zapsat(
                    zastavkySpoju = zastavkySpoju.toTypedArray(),
                    spoje = spoje.toTypedArray(),
                    ostatni = VsechnoOstatni(
                        verze = verze,
                        linkyAJejichZastavky = linky,
                        zastavky = zastavky.toList(),
                        graphZastavek = graphZastavek,
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

    private fun vytvoritGraf(spoje: Map<Spoj, List<ZastavkaSpoje>>): GraphZastavek {

        val graphZastavek = emptyGraphZastavek().toMutableGraphZastavek()

        spoje.forEach { (spoj, zastavkySpoje) ->
            zastavkySpoje
                .sortedBy { it.indexNaLince }
                .reversedIf { spoj.smer == NEGATIVNI }
                .filter { it.cas != Cas.nikdy }
                .map { it.nazevZastavky }
                .also { zastavky ->
                    zastavky.zipWithNext().forEach { (zastavka, soused) ->
                        graphZastavek.putIfAbsent(zastavka, mutableSetOf())
                        graphZastavek[zastavka]!!.add(soused)
                    }
                }
        }

        graphZastavek.toList().sortedBy { it.first }.toMap().forEach {
            println(it)
        }

        //println(graphZastavek
        //    .flatMap { (k, v) ->
        //        v.map { k to it }
        //    }
        //    .joinToString("\n") {
        //        it.toList().joinToString("&&&")
        //    }
        //    .replace(" ", "")
        //    .replace("&&&", " ")
        //)

        return graphZastavek.toGraphZastavek()
    }
}
