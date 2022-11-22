package cz.jaro.dpmcb.ui.detail.kurzu

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Accessible
import androidx.compose.material.icons.filled.NotAccessible
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.WheelchairPickup
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.accompanist.flowlayout.FlowRow
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import cz.jaro.dpmcb.R
import cz.jaro.dpmcb.data.App
import cz.jaro.dpmcb.data.App.Companion.repo
import cz.jaro.dpmcb.data.helperclasses.Smer
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.reversedIf
import cz.jaro.dpmcb.ui.destinations.OdjezdyScreenDestination
import kotlin.random.Random

@Destination
@Composable
fun DetailKurzuScreen(
    navigator: DestinationsNavigator,
    kurz: String
) {

    App.title = R.string.detail_spoje

    val spoje by produceState(emptyList()) {
        value = repo.spojeKurzuSeZastavkySpojeNaKterychStavi(kurz).map { (spoj, zastavky) ->
            spoj to zastavky.reversedIf { spoj.smer == Smer.NEGATIVNI }
        }.sortedBy { it.second.first().cas }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp)
    ) {
        if (spoje.isEmpty()) CircularProgressIndicator(modifier = Modifier.fillMaxWidth())
        else {
            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                item {
                    FlowRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp)
                    ) {
                        Text("Linka ${spoje.first().first.cisloLinky}")
                        spoje.forEach { (spoj, _) ->
                            Icon(
                                when {
                                    Random.nextFloat() < .01F -> Icons.Default.ShoppingCart
                                    spoj.nizkopodlaznost -> Icons.Default.Accessible
                                    Random.nextFloat() < .33F -> Icons.Default.WheelchairPickup
                                    else -> Icons.Default.NotAccessible
                                }, "Invalidní vozík", modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    }
                }
                items(spoje) { (_, zastavky) ->
                    OutlinedCard(modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp)
                        ) {
                            Column {
                                zastavky.forEach {
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
                                zastavky.forEach {
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
}
