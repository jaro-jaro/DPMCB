package cz.jaro.dpmcb.ui.oblibene

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Badge
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootNavGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import cz.jaro.dpmcb.data.App.Companion.dopravaRepo
import cz.jaro.dpmcb.data.App.Companion.repo
import cz.jaro.dpmcb.data.DopravaRepository.Companion.upravit
import cz.jaro.dpmcb.data.entities.Spoj
import cz.jaro.dpmcb.data.entities.ZastavkaSpoje
import cz.jaro.dpmcb.data.helperclasses.Smer
import cz.jaro.dpmcb.data.helperclasses.Trvani.Companion.min
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.barvaZpozdeniTextu
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.reversedIf
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.toSign
import cz.jaro.dpmcb.ui.destinations.DetailSpojeScreenDestination

@OptIn(ExperimentalMaterial3Api::class)
@Destination
@RootNavGraph(start = true)
@Composable
fun OblibeneScreen(
    navigator: DestinationsNavigator,
) {
    val oblibene by repo.oblibene.collectAsState()
    LazyColumn(
        modifier = Modifier.fillMaxSize()
    ) {
        if (oblibene.isEmpty()) item {
            Text(
                text = "Zatím nemáte žádná oblíbená spojení. Přidejte si je kliknutím na ikonu hvězdičky v detailu spoje",
                modifier = Modifier.padding(all = 16.dp),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center
            )
        }

        items(oblibene) {

            val a by produceState<Pair<Spoj?, List<ZastavkaSpoje>>>((null to emptyList())) {
                value = repo.spojSeZastavkySpojeNaKterychStavi(it)
            }
            val spoj = a.first
            val zastavky = a.second
            val spojNaMape by dopravaRepo.spojNaMapePodleSpojeNeboUlozenehoId(spoj, zastavky).collectAsState(initial = null)
            val detailSpoje by dopravaRepo.detailSpojePodleSpojeNeboUlozenehoId(spojNaMape?.let { spoj }, zastavky).collectAsState(initial = null)

            OutlinedCard(
                onClick = {
                    navigator.navigate(DetailSpojeScreenDestination(it))
                },
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                if (spoj != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 8.dp, top = 8.dp, end = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "${spoj.cisloLinky}")
                        if (spojNaMape != null) Badge(
                            containerColor = UtilFunctions.barvaZpozdeniBublinyKontejner(spojNaMape!!.delay),
                            contentColor = UtilFunctions.barvaZpozdeniBublinyText(spojNaMape!!.delay),
                            modifier = Modifier.padding(start = 8.dp)
                        ) {
                            Text(
                                text = spojNaMape!!.delay.run {
                                    "${toSign()}$this min"
                                },
                            )
                        }
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 8.dp, end = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        val z = zastavky.reversedIf { spoj.smer == Smer.NEGATIVNI }.first()
                        Text(text = z.nazevZastavky)
                        Text(text = z.cas.toString())
                    }
                    if (detailSpoje != null && spojNaMape != null) {
                        val zNaMape = detailSpoje!!.stations.find { !it.passed }
                        val z = zastavky.find {
                            it.nazevZastavky.upravit() == zNaMape?.name?.upravit() && it.cas.toString() == zNaMape.departureTime
                        }
                        if (z != null) Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 8.dp, end = 8.dp),
                        ) {
                            Text(text = z.nazevZastavky)
                            Spacer(modifier = Modifier.weight(1F))
                            Text(
                                text = "${z.cas + spojNaMape!!.delay.min}",
                                color = barvaZpozdeniTextu(spojNaMape!!.delay),
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 8.dp, bottom = 8.dp, end = 8.dp),
                    ) {
                        val z = zastavky.reversedIf { spoj.smer == Smer.NEGATIVNI }.last()
                        Text(text = z.nazevZastavky)
                        Spacer(modifier = Modifier.weight(1F))
                        if (spojNaMape != null) Text(
                            text = "${z.cas + spojNaMape!!.delay.min}",
                            color = barvaZpozdeniTextu(spojNaMape!!.delay),
                            modifier = Modifier.padding(start = 8.dp)
                        ) else Text(text = "${z.cas}")
                    }
                }
            }
        }
    }
}