package cz.jaro.dpmcb.ui.jedouci

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import cz.jaro.dpmcb.R
import cz.jaro.dpmcb.data.App
import cz.jaro.dpmcb.data.helperclasses.MutateListFunction
import cz.jaro.dpmcb.data.helperclasses.NavigateFunction
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.barvaZpozdeniTextu
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.plus
import cz.jaro.dpmcb.ui.destinations.SpojDestination
import cz.jaro.dpmcb.ui.main.SuplikAkce
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf
import java.time.LocalDate
import kotlin.time.Duration.Companion.minutes

@Composable
@Destination
fun PraveJedouci(
    filtry: IntArray = intArrayOf(),
    viewModel: PraveJedouciViewModel = koinViewModel {
        parametersOf(PraveJedouciViewModel.Parameters(filtry = filtry.toList()))
    },
    navigator: DestinationsNavigator,
) {
    App.title = R.string.doprava_na_jihu
    App.vybrano = SuplikAkce.PraveJedouci

    val state by viewModel.state.collectAsStateWithLifecycle()

    PraveJedouciScreen(
        state = state,
        upravitFiltry = viewModel::upravitFiltry,
        navigate = navigator::navigate,
        zmenitDatum = viewModel.upravitDatum,
    )
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
fun PraveJedouciScreen(
    state: PraveJedouciState,
    upravitFiltry: MutateListFunction<Int>,
    navigate: NavigateFunction,
    zmenitDatum: (LocalDate) -> Unit,
) {
    when (state) {
        PraveJedouciState.Offline -> Text(
            text = "Jste offline :(",
            modifier = Modifier
                .padding(all = 16.dp)
                .fillMaxWidth(),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )

        PraveJedouciState.NacitaniLinek -> Text(
            text = "Načítání...",
            modifier = Modifier
                .padding(all = 16.dp)
                .fillMaxWidth(),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )

        PraveJedouciState.ZadneLinky -> Text(
            text = "Bohužel, zdá se že právě nejede žádná linka. Toto může také nastat pokud má Dopravní podnik výpad svých informačních serverů. V takovém případě nefungují aktuální informace o spojích ani kdekoliv jinde, včetně zastávkových označníků ve městě.",
            modifier = Modifier
                .padding(all = 16.dp)
                .fillMaxWidth(),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )

        is PraveJedouciState.LinkyNacteny -> Column {
            Text(
                "Vyberte linku:", modifier = Modifier
                    .padding(all = 16.dp)
                    .fillMaxWidth()
            )
            FlowRow(
                modifier = Modifier.padding(horizontal = 4.dp),
            ) {
                state.cislaLinek.forEach { cislo ->
                    Chip(
                        seznam = state.filtry,
                        cisloLinky = cislo,
                        poKliknuti = {
                            upravitFiltry {
                                if (it) add(cislo) else remove(cislo)
                            }
                        }
                    )
                }
            }

            when (state) {
                is PraveJedouciState.PraveNicNejede -> Text("Od vybraných linek právě nic nejede")
                is PraveJedouciState.Nacitani -> Text("Načítání...")
                is PraveJedouciState.NeniNicVybrano -> Text("Není vybraná žádná linka")
                is PraveJedouciState.OK -> LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 16.dp)
                        .padding(horizontal = 8.dp),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    state.seznam.forEach { linka ->
                        stickyHeader(key = linka.cisloLinky to linka.cilovaZastavka) {
                            Column(
                                Modifier.background(MaterialTheme.colorScheme.surface)
                            ) {
                                Text(
                                    text = "${linka.cisloLinky} -> ${linka.cilovaZastavka}",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 8.dp)
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
                            }
                        }
                        items(linka.spoje, key = { it.spojId }) { spoj ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        zmenitDatum(LocalDate.now())
                                        navigate(SpojDestination(spojId = spoj.spojId))
                                    }
                            ) {
                                Text(text = spoj.pristiZastavkaNazev, modifier = Modifier.weight(1F))
                                Text(text = spoj.pristiZastavkaCas.toString())
                                Text(
                                    text = spoj.pristiZastavkaCas.plus(spoj.zpozdeni.minutes).toString(),
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