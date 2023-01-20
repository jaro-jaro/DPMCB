package cz.jaro.dpmcb.ui.jedouci

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.accompanist.flowlayout.FlowRow
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import cz.jaro.dpmcb.R
import cz.jaro.dpmcb.data.App
import cz.jaro.dpmcb.data.helperclasses.Trvani.Companion.min
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.barvaZpozdeniTextu
import cz.jaro.dpmcb.ui.destinations.DetailSpojeScreenDestination
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalFoundationApi::class)
@Composable
@Destination
fun PraveJedouciScreen(
    viewModel: PraveJedouciViewModel = koinViewModel(),
    navigator: DestinationsNavigator,
) {
    val cislaLinek = viewModel.cislaLinek
    val seznam by viewModel.seznam.collectAsState(initial = emptyList())
    val filtry by viewModel.filtry.collectAsState()

    App.title = R.string.doprava_na_jihu

    Column {
        FlowRow(
            modifier = Modifier
                .padding(start = 4.dp, end = 4.dp)
        ) {
            cislaLinek.forEach { cislo ->
                Chip(
                    seznam = filtry,
                    cisloLinky = cislo,
                    poKliknuti = {
                        viewModel.upravitFiltry {
                            if (it) add(cislo) else remove(cislo)
                        }
                    }
                )
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 16.dp)
        ) {
            items(seznam, key = { it.first to it.second }) { (cislo, cil, spoje) ->
                Card(
                    modifier = Modifier
                        .animateItemPlacement()
                        .padding(all = 8.dp)
                        .animateItemPlacement()
                ) {
                    Column(
                        modifier = Modifier.padding(all = 8.dp)
                    ) {
                        Text(text = "$cislo -> $cil")

                        spoje.forEach { spoj ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        navigator.navigate(DetailSpojeScreenDestination(spojId = spoj.spojId))
                                    }
                            ) {
                                Text(text = spoj.pristiZastavka.first, modifier = Modifier.weight(1F))
                                Text(
                                    text = (spoj.pristiZastavka.second + spoj.zpozdeni.min).toString(),
                                    color = barvaZpozdeniTextu(spoj.zpozdeni)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Chip(
    seznam: List<Int>,
    cisloLinky: Int,
    poKliknuti: (Boolean) -> Unit,
) = FilterChip(
    modifier = Modifier
        .padding(all = 4.dp),
    selected = cisloLinky in seznam,
    onClick = {
        poKliknuti(cisloLinky !in seznam)
    },
    label = { Text("$cisloLinky") }
)