package cz.jaro.dpmcb.ui.detail.spoje

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Accessible
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.NotAccessible
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.WheelchairPickup
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconToggleButton
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import cz.jaro.dpmcb.R
import cz.jaro.dpmcb.data.App
import cz.jaro.dpmcb.data.App.Companion.dopravaRepo
import cz.jaro.dpmcb.data.App.Companion.repo
import cz.jaro.dpmcb.data.entities.Spoj
import cz.jaro.dpmcb.data.entities.ZastavkaSpoje
import cz.jaro.dpmcb.data.helperclasses.Smer
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.reversedIf
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.toSign
import cz.jaro.dpmcb.ui.destinations.DetailKurzuScreenDestination
import cz.jaro.dpmcb.ui.destinations.OdjezdyScreenDestination
import kotlin.random.Random

@Destination
@Composable
fun DetailSpojeScreen(
    navigator: DestinationsNavigator,
    spojId: Long,
) {

    App.title = R.string.detail_spoje

    val a by produceState<Pair<Spoj?, List<ZastavkaSpoje>>>((null to emptyList())) {
        value = repo.spojSeZastavkySpojeNaKterychStavi(spojId)
    }
    val spoj = a.first
    val zastavky = a.second
    val b by dopravaRepo.spojPodleSpojeNeboUlozenehoId(spoj, zastavky).collectAsState(initial = null to null)
    val spojNaMape = b.first
    val detailSpoje = b.second

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp)
    ) {
        if (spoj == null) CircularProgressIndicator(modifier = Modifier.fillMaxWidth())
        else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp), verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Linka ${spoj.cisloLinky}")
                Icon(
                    when {
                        Random.nextFloat() < .01F -> Icons.Default.ShoppingCart
                        spoj.nizkopodlaznost -> Icons.Default.Accessible
                        Random.nextFloat() < .33F -> Icons.Default.WheelchairPickup
                        else -> Icons.Default.NotAccessible
                    }, "Invalidní vozík", modifier = Modifier.padding(start = 8.dp)
                )
                if (spojNaMape != null) Text(
                    text = spojNaMape.delay.run {
                        "${toSign()}$this"
                    },
                    color = if (spojNaMape.delay > 0) Color.Red else Color.Green
                )
                Spacer(Modifier.weight(1F))
                FilledIconToggleButton(checked = false, onCheckedChange = {}) {
                    Icon(Icons.Default.Favorite, "Oblíbené")
                }
                Button(onClick = {
                    navigator.navigate(
                        DetailKurzuScreenDestination(
                            kurz = spoj.nazevKurzu
                        )
                    )
                }) {
                    Text("Detail kurzu")
                }
            }
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                    ) {
                        Column {
                            zastavky.reversedIf { spoj.smer == Smer.NEGATIVNI }.forEach {
                                Text(
                                    text = it.nazevZastavky,
                                    modifier = Modifier
                                        .clickable {
                                            navigator.navigate(
                                                OdjezdyScreenDestination(
                                                    cas = it.cas.toString(),
                                                    zastavka = it.nazevZastavky,
                                                )
                                            )
                                        }
                                )
                            }
                        }
                        Column(Modifier.padding(start = 8.dp)) {
                            zastavky.reversedIf { spoj.smer == Smer.NEGATIVNI }.forEach {
                                Text(
                                    text = it.cas.toString(),
                                    modifier = Modifier
                                        .clickable {
                                            navigator.navigate(
                                                OdjezdyScreenDestination(
                                                    cas = it.cas.toString(),
                                                    zastavka = it.nazevZastavky,
                                                )
                                            )
                                        }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
