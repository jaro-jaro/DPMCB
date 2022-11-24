package cz.jaro.dpmcb.ui.odjezdy

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Accessible
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.NotAccessible
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import cz.jaro.dpmcb.R
import cz.jaro.dpmcb.data.App
import cz.jaro.dpmcb.data.App.Companion.repo
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.VDP
import cz.jaro.dpmcb.ui.UiEvent
import cz.jaro.dpmcb.ui.theme.DPMCBTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.withContext
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.ParametersHolder

@Destination
@Composable
fun OdjezdyScreen(
    zastavka: String,
    cas: String? = null,
    doba: Int = 30,
    viewModel: OdjezdyViewModel = koinViewModel {
        ParametersHolder(mutableListOf(zastavka, cas, doba))
    },
    navigator: DestinationsNavigator,
) {
    App.title = R.string.odjezdy

    val state by viewModel.state.collectAsState()
//    val viewModel = rememberSaveable() { OdjezdyViewModel(repo, zastavka, cas, doba) }

    LaunchedEffect(Unit) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                is UiEvent.Navigovat -> {
                    navigator.navigate(event.kam)
                }

                else -> {}
            }
        }
    }

    val typDne by repo.typDne.collectAsState(VDP.DNY)

    var job: Job? = null

    LaunchedEffect(state.konec, state.zacatek, typDne) {
        job?.cancel()
        job = withContext(Dispatchers.IO) {
            viewModel.nacistVsechny(typDne)
        }
    }

    if (state.nacitaSe) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = state.zastavka,
                fontSize = 20.sp
            )
            IconButton(
                onClick = {
                    viewModel.poslatEvent(OdjezdyEvent.ZmensitCas)
                },
            ) {
                Icon(
                    imageVector = Icons.Default.Remove,
                    contentDescription = ""
                )
            }
            Text(
                text = state.zacatek.toString()
            )
            IconButton(
                onClick = {
                    viewModel.poslatEvent(OdjezdyEvent.ZvetsitCas)
                },
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = ""
                )
            }
        }
        LazyColumn {
            items(state.seznam) { spoj ->
                Karticka(spoj, viewModel::poslatEvent)
            }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Načítání")
                    if (!state.nacitaSe)
                        viewModel.poslatEvent(OdjezdyEvent.NacistDalsi)
                }
            }
        }
    }
}

@Preview
@Composable
fun KartickaPreview() {
    DPMCBTheme {
        Column {
            Karticka(OdjezdyViewModel.KartickaState(
                konecna = "Ahoj",
                pristiZastavka = "Čau",
                cisloLinky = 9,
                cas = "12:38",
                JePosledniZastavka = false,
                idSpoje = 612L,
                nizkopodlaznost = true
            )) {}
            Karticka(OdjezdyViewModel.KartickaState(
                konecna = "Ne",
                pristiZastavka = "Nechci se zdravit",
                cisloLinky = 29,
                cas = "14:88",
                JePosledniZastavka = true,
                idSpoje = 1415926535L,
                nizkopodlaznost = false
            )) {}
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Karticka(
    kartickaState: OdjezdyViewModel.KartickaState,
    poslatEvent: (OdjezdyEvent) -> Unit,
) {
    OutlinedCard(
        onClick = {
            poslatEvent(OdjezdyEvent.KliklNaDetailSpoje(kartickaState.idSpoje))
        },
        modifier = Modifier
            .padding(bottom = 8.dp)

    ) {
        Column(
            modifier = Modifier
                .padding(all = 8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    modifier = Modifier
                        .weight(1F),
                    text = buildAnnotatedString {
                        append(
                            AnnotatedString(
                                text = kartickaState.cisloLinky.toString(),
                                spanStyle = SpanStyle(
                                    fontSize = 30.sp
                                )
                            )
                        )
                        append(
                            AnnotatedString(
                                text = " -> ",
                                spanStyle = SpanStyle(
                                    fontSize = 20.sp
                                )
                            )
                        )
                        append(
                            AnnotatedString(
                                text = kartickaState.konecna,
                                spanStyle = SpanStyle(
                                    fontSize = 20.sp
                                )
                            )
                        )
                    }
                )
                Text(
                    text = kartickaState.cas
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Icon(
                    when {
                        kartickaState.nizkopodlaznost -> Icons.Default.Accessible
                        else -> Icons.Default.NotAccessible
                    }, "Invalidní vozík"
                )
                TextButton(
                    onClick = {
                        poslatEvent(OdjezdyEvent.KliklNaZjr(kartickaState))
                    },
                    enabled = !kartickaState.JePosledniZastavka
                ) {
                    Text(text = "Zastávkové jízdní řády")
                }
            }
        }
    }
}
