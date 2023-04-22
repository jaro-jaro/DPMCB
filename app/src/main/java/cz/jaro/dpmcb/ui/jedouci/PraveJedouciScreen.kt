package cz.jaro.dpmcb.ui.jedouci

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.accompanist.flowlayout.FlowRow
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import cz.jaro.dpmcb.R
import cz.jaro.dpmcb.data.App
import cz.jaro.dpmcb.data.helperclasses.MutateListFunction
import cz.jaro.dpmcb.data.helperclasses.NavigateFunction
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.barvaZpozdeniTextu
import cz.jaro.dpmcb.ui.destinations.DetailSpojeDestination
import cz.jaro.dpmcb.ui.main.SuplikAkce
import org.koin.androidx.compose.koinViewModel
import java.time.LocalDate

@Composable
@Destination
fun PraveJedouci(
    viewModel: PraveJedouciViewModel = koinViewModel(),
    navigator: DestinationsNavigator,
) {
    App.title = R.string.doprava_na_jihu
    App.vybrano = SuplikAkce.PraveJedouci

    val cislaLinek by viewModel.cislaLinek.collectAsStateWithLifecycle()
    val seznam by viewModel.seznam.collectAsStateWithLifecycle(initialValue = emptyList())
    val filtry by viewModel.filtry.collectAsStateWithLifecycle()
    val nacitaSe by viewModel.nacitaSe.collectAsStateWithLifecycle()
    val jeOnline by viewModel.maPristupKJihu.collectAsStateWithLifecycle()

    PraveJedouciScreen(
        cislaLinek = cislaLinek,
        seznam = seznam,
        filtry = filtry,
        upravitFiltry = viewModel::upravitFiltry,
        nacitaSe = nacitaSe,
        jeOnline = jeOnline,
        navigate = navigator::navigate,
        zmenitDatum = viewModel.upravitDatum,
    )
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun PraveJedouciScreen(
    cislaLinek: List<Int>?,
    seznam: List<Triple<Int, String, List<JedouciSpoj>>>,
    filtry: List<Int>,
    upravitFiltry: MutateListFunction<Int>,
    nacitaSe: Boolean,
    jeOnline: Boolean,
    navigate: NavigateFunction,
    zmenitDatum: (LocalDate) -> Unit,
) = if (!jeOnline) Text(
    text = "Jste offline :(",
    modifier = Modifier.padding(all = 16.dp),
    style = MaterialTheme.typography.bodyLarge,
    textAlign = TextAlign.Center
)
else Column {
    Text("Vyberte linku:", modifier = Modifier.padding(bottom = 4.dp, start = 8.dp))
    FlowRow(
        modifier = Modifier
            .padding(start = 4.dp, end = 4.dp)
    ) {
        cislaLinek?.forEach { cislo ->
            Chip(
                seznam = filtry,
                cisloLinky = cislo,
                poKliknuti = {
                    upravitFiltry {
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
        if (seznam.isEmpty() && filtry.isEmpty()) item {
            Text("Není vybraná žádná linka", modifier = Modifier.padding(all = 8.dp))
        }
        if (seznam.isEmpty() && filtry.isNotEmpty()) item {
            Text(
                if (nacitaSe) "Načítání..." else "Od vybraných linek právě nic nejede",
                modifier = Modifier.padding(all = 8.dp)
            )
        }
        items(seznam, key = { it.first to it.second }) { (cislo, cil, spoje) ->
            OutlinedCard(
                modifier = Modifier
                    .animateItemPlacement()
                    .padding(all = 8.dp)
                    .animateItemPlacement(),
                onClick = {
                    if (spoje.size == 1) {
                        navigate(DetailSpojeDestination(spojId = spoje.first().spojId))
                    }
                }
            ) {
                Column(
                    modifier = Modifier
                        .padding(all = 8.dp)
                ) {
                    Text(text = "$cislo -> $cil", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                    ) {
                        Text(text = "Příští zastávka", modifier = Modifier.weight(1F), style = MaterialTheme.typography.labelMedium)
                        Text(text = "odjezd", style = MaterialTheme.typography.bodySmall)
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(MaterialTheme.colorScheme.onSurface)
                    )
                    spoje.forEach { spoj ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    zmenitDatum(LocalDate.now())
                                    navigate(DetailSpojeDestination(spojId = spoj.spojId))
                                }
                        ) {
                            Text(text = spoj.pristiZastavka.first, modifier = Modifier.weight(1F))
                            Text(
                                text = (spoj.pristiZastavka.second).toString(),
                            )
                            Text(
                                text = (spoj.pristiZastavka.second.plusMinutes(spoj.zpozdeni.toLong())).toString(),
                                color = barvaZpozdeniTextu(spoj.zpozdeni),
                                modifier = Modifier.padding(start = 8.dp)
                            )
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