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
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.evC
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.navigateFunction
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.plus
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.toSign
import cz.jaro.dpmcb.ui.main.SuplikAkce
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf
import kotlin.time.Duration.Companion.minutes

@Composable
@Destination
fun PraveJedouci(
    filtry: IntArray = intArrayOf(),
    typ: TypPraveJedoucich = TypPraveJedoucich.Poloha,
    navigator: DestinationsNavigator,
    viewModel: PraveJedouciViewModel = koinViewModel {
        parametersOf(PraveJedouciViewModel.Parameters(filtry = filtry.toList(), typ = typ, navigate = navigator.navigateFunction))
    },
) {
    App.title = R.string.prave_jedouci
    App.vybrano = SuplikAkce.PraveJedouci

    val state by viewModel.state.collectAsStateWithLifecycle()

    PraveJedouciScreen(
        state = state,
        onEvent = viewModel::onEvent,
    )
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun PraveJedouciScreen(
    state: PraveJedouciState,
    onEvent: (PraveJedouciEvent) -> Unit,
) {
    when (state) {
        PraveJedouciState.NeniDneska -> Text(
            text = "Pro zobrazení právě jedoucích spojů si změňte datum na dnešek",
            modifier = Modifier
                .padding(all = 16.dp)
                .fillMaxWidth(),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )

        PraveJedouciState.Offline -> Text(
            text = "Jste offline :(",
            modifier = Modifier
                .padding(all = 16.dp)
                .fillMaxWidth(),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )

        PraveJedouciState.ZadneLinky -> Text(
            text = "Bohužel, zdá se že právě nejede žádná linka. Toto může také nastat pokud má Dopravní podnik výpadek svých informačních serverů. V takovém případě nefungují aktuální informace o spojích ani kdekoliv jinde, včetně zastávkových označníků ve městě.",
            modifier = Modifier
                .padding(all = 16.dp)
                .fillMaxWidth(),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )

        is PraveJedouciState.MaPravoMitTyp -> Column {
            Text(
                "Řadit podle:", modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth()
            )
            FlowRow(
                modifier = Modifier.padding(horizontal = 8.dp),
            ) {
                TypPraveJedoucich.entries.forEach { typ ->
                    FilterChip(
                        modifier = Modifier
                            .padding(all = 4.dp),
                        selected = typ == state.typ,
                        onClick = {
                            onEvent(PraveJedouciEvent.ZmenitTyp(typ))
                        },
                        label = { Text(typ.jmeno) }
                    )
                }
            }
            when (state) {
                is PraveJedouciState.NacitaniLinek -> Text(text = "Načítání...")

                is PraveJedouciState.MaPravoMitFiltry -> Column {
                    Text(
                        "Filtr linek:", modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .fillMaxWidth()
                    )
                    FlowRow(
                        modifier = Modifier.padding(horizontal = 8.dp),
                    ) {
                        state.cislaLinek.forEach { cislo ->
                            Chip(
                                seznam = state.filtry,
                                cisloLinky = cislo,
                                poKliknuti = {
                                    onEvent(PraveJedouciEvent.ZmenitFiltr(cislo))
                                }
                            )
                        }
                    }

                    when (state) {
                        is PraveJedouciState.PraveNicNejede -> Text("Od vybraných linek právě nic nejede")
                        is PraveJedouciState.Nacitani -> Text("Načítání...")
                        is PraveJedouciState.OK -> LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(top = 8.dp)
                                .padding(horizontal = 8.dp),
                            contentPadding = PaddingValues(vertical = 8.dp)
                        ) {
                            when (state.vysledek) {
                                is VysledekPraveJedoucich.Poloha -> state.vysledek.seznam.forEach { linka ->
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
                                                    onEvent(PraveJedouciEvent.KliklNaSpoj(spoj.spojId))
                                                }
                                        ) {
                                            Text(text = "${spoj.vuz.evC()}: ${spoj.pristiZastavkaNazev}", modifier = Modifier.weight(1F))
                                            Text(text = spoj.pristiZastavkaCas.toString())
                                            Text(
                                                text = spoj.pristiZastavkaCas.plus(spoj.zpozdeni.toInt().minutes).toString(),
                                                color = UtilFunctions.barvaZpozdeniTextu(spoj.zpozdeni),
                                                modifier = Modifier.padding(start = 8.dp)
                                            )
                                        }
                                    }
                                }

                                is VysledekPraveJedoucich.EvC -> items(state.vysledek.seznam, key = { it.spojId }) { spoj ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                onEvent(PraveJedouciEvent.KliklNaSpoj(spoj.spojId))
                                            }
                                    ) {
                                        Text(text = "${spoj.cisloLinky} -> ${spoj.cilovaZastavka}", modifier = Modifier.weight(1F))
                                        Text(text = spoj.vuz.evC())
                                    }
                                }

                                is VysledekPraveJedoucich.Zpozdeni -> items(state.vysledek.seznam, key = { it.spojId }) { spoj ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                onEvent(PraveJedouciEvent.KliklNaSpoj(spoj.spojId))
                                            }
                                    ) {
                                        Text(text = "${spoj.cisloLinky} -> ${spoj.cilovaZastavka}", modifier = Modifier.weight(1F))
                                        Text(
                                            text = spoj.zpozdeni.toDouble().minutes.run { "${inWholeSeconds.toSign()}$inWholeMinutes min ${inWholeSeconds % 60} s" },
                                            color = UtilFunctions.barvaZpozdeniTextu(spoj.zpozdeni)
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