package cz.jaro.dpmcb.ui.odjezdy

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Accessible
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.NotAccessible
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.marosseleng.compose.material3.datetimepickers.time.ui.dialog.TimePickerDialog
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import com.ramcosta.composedestinations.result.NavResult
import com.ramcosta.composedestinations.result.ResultRecipient
import cz.jaro.dpmcb.R
import cz.jaro.dpmcb.data.App
import cz.jaro.dpmcb.data.App.Companion.repo
import cz.jaro.dpmcb.data.App.Companion.title
import cz.jaro.dpmcb.data.helperclasses.NavigateFunction
import cz.jaro.dpmcb.data.helperclasses.TypAdapteru
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.IconWithTooltip
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.barvaZpozdeniTextu
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.ted
import cz.jaro.dpmcb.data.helperclasses.Vysledek
import cz.jaro.dpmcb.ui.destinations.VybiratorDestination
import cz.jaro.dpmcb.ui.main.SuplikAkce
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.ParametersHolder
import java.time.Duration
import java.time.LocalTime

@Destination
@Composable
fun Odjezdy(
    zastavka: String,
    cas: LocalTime = ted,
    viewModel: OdjezdyViewModel = koinViewModel {
        ParametersHolder(mutableListOf(zastavka, cas))
    },
    navigator: DestinationsNavigator,
    resultRecipient: ResultRecipient<VybiratorDestination, Vysledek>,
) {
    resultRecipient.onNavResult { result ->
        when (result) {
            is NavResult.Canceled -> {}
            is NavResult.Value -> {
                viewModel.vybral(result.value)
            }
        }
    }

    title = R.string.odjezdy
    App.vybrano = SuplikAkce.Odjezdy

    val state by viewModel.state.collectAsStateWithLifecycle()
    val filtrovanejSeznam by viewModel.filtrovanejSeznam.collectAsStateWithLifecycle()

    val listState = rememberLazyListState(state.indexScrollovani)

    LaunchedEffect(Unit) {
        viewModel.scrollovat = {
            listState.scrollToItem(it)
        }
        viewModel.navigovat = {
            navigator.navigate(it)
        }
    }

    LaunchedEffect(listState) {
        withContext(Dispatchers.IO) {
            snapshotFlow { listState.firstVisibleItemIndex }
                .flowOn(Dispatchers.IO)
                .collect {
                    viewModel.scrolluje(it)
                }
        }
    }

    val jeOnline by repo.maPristupKJihu.collectAsStateWithLifecycle()

    OdjezdyScreen(
        state = state,
        seznam = filtrovanejSeznam,
        zastavka = zastavka,
        zmenitCas = viewModel::zmenitCas,
        zmenilKompaktniRezim = viewModel::zmenilKompaktniRezim,
        listState = listState,
        zrusil = viewModel::zrusil,
        kliklNaDetailSpoje = viewModel::kliklNaDetailSpoje,
        navigate = navigator::navigate,
        jeOnline = jeOnline,
    )
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun OdjezdyScreen(
    state: OdjezdyState,
    seznam: List<KartickaState>?,
    zastavka: String,
    zmenitCas: (LocalTime) -> Unit,
    zmenilKompaktniRezim: () -> Unit,
    listState: LazyListState,
    zrusil: (TypAdapteru) -> Unit,
    kliklNaDetailSpoje: (KartickaState) -> Unit,
    jeOnline: Boolean,
    navigate: NavigateFunction,
) = Column(
    modifier = Modifier
        .fillMaxSize()
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .padding(horizontal = 16.dp)
    ) {
        TextButton(
            onClick = {
                navigate(VybiratorDestination(TypAdapteru.ZASTAVKY))
            }
        ) {
            Text(
                text = zastavka,
                fontSize = 20.sp
            )
        }
        var zobrazitDialog by rememberSaveable { mutableStateOf(false) }
        if (zobrazitDialog) TimePickerDialog(
            onDismissRequest = {
                zobrazitDialog = false
            },
            onTimeChange = {
                zmenitCas(it)
                zobrazitDialog = false
            },
            title = {
                Text("Změnit čas")
            },
            initialTime = state.cas
        )

        TextButton(
            onClick = {
                zobrazitDialog = true
            }
        ) {
            Text(text = state.cas.toString())
        }
        Spacer(modifier = Modifier.weight(1F))

        Text("Zjednodušit")
        Switch(checked = state.kompaktniRezim, onCheckedChange = {
            zmenilKompaktniRezim()
        }, Modifier.padding(all = 8.dp), enabled = jeOnline)
    }

    Column(
        modifier = Modifier
            .padding(horizontal = 16.dp)
    ) {
        val linkaSource = remember { MutableInteractionSource() }
        val containerColor = MaterialTheme.colorScheme.onSurfaceVariant
        TextField(
            value = state.filtrLinky?.toString() ?: "Všechny",
            onValueChange = {},
            Modifier
                .fillMaxWidth(),
            label = {
                Text(text = "Linka:")
            },
            interactionSource = linkaSource,
            readOnly = true,
            trailingIcon = {
                if (state.filtrLinky != null) IconButton(onClick = {
                    zrusil(TypAdapteru.LINKA_ZPET)
                }) {
                    IconWithTooltip(imageVector = Icons.Default.Clear, contentDescription = "Vymazat")
                }
            },
            colors = TextFieldDefaults.colors(
                focusedTextColor = state.filtrZastavky?.let { MaterialTheme.colorScheme.onSurface } ?: MaterialTheme.colorScheme.onSurfaceVariant,
                unfocusedTextColor = state.filtrZastavky?.let { MaterialTheme.colorScheme.onSurface } ?: MaterialTheme.colorScheme.onSurfaceVariant,
                focusedContainerColor = containerColor,
                unfocusedContainerColor = containerColor,
                disabledContainerColor = containerColor,
            ),
        )
        val linkaPressedState by linkaSource.interactions.collectAsStateWithLifecycle(PressInteraction.Cancel(PressInteraction.Press(Offset.Zero)))
        if (linkaPressedState is PressInteraction.Release) {
            navigate(
                VybiratorDestination(
                    typ = TypAdapteru.LINKA_ZPET,
                )
            )
            linkaSource.tryEmit(PressInteraction.Cancel(PressInteraction.Press(Offset.Zero)))
        }
        val zastavkaSource = remember { MutableInteractionSource() }
        TextField(
            value = state.filtrZastavky ?: "Cokoliv",
            onValueChange = {},
            Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),

            label = {
                Text(text = "Jede přes:")
            },
            readOnly = true,
            trailingIcon = {
                if (state.filtrZastavky != null) IconButton(onClick = {
                    zrusil(TypAdapteru.ZASTAVKA_ZPET)
                }) {
                    IconWithTooltip(imageVector = Icons.Default.Clear, contentDescription = "Vymazat")
                }
            },
            interactionSource = zastavkaSource,
            colors = TextFieldDefaults.colors(
                focusedTextColor = state.filtrZastavky?.let { MaterialTheme.colorScheme.onSurface } ?: MaterialTheme.colorScheme.onSurfaceVariant,
                unfocusedTextColor = state.filtrZastavky?.let { MaterialTheme.colorScheme.onSurface } ?: MaterialTheme.colorScheme.onSurfaceVariant,
                focusedContainerColor = containerColor,
                unfocusedContainerColor = containerColor,
                disabledContainerColor = containerColor,
            ),
        )
        val zastavkaPressedState by zastavkaSource.interactions.collectAsStateWithLifecycle(PressInteraction.Cancel(PressInteraction.Press(Offset.Zero)))
        if (zastavkaPressedState is PressInteraction.Release) {
            navigate(
                VybiratorDestination(
                    typ = TypAdapteru.ZASTAVKA_ZPET,
                )
            )
            zastavkaSource.tryEmit(PressInteraction.Cancel(PressInteraction.Press(Offset.Zero)))
        }
    }
    if (seznam == null) Row(
        Modifier
            .padding(top = 8.dp)
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator()
    }
    else if (seznam.isEmpty()) Row(
        Modifier
            .padding(top = 8.dp)
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.Center
    ) {
        Text(
            if (state.filtrZastavky == null && state.filtrLinky == null) "Přes tuto zastávku nic nejede"
            else if (state.filtrLinky == null) "Přes tuto zastávku nejede žádný spoj, který zastavuje na zastávce ${state.filtrZastavky}"
            else if (state.filtrZastavky == null) "Přes tuto zastávku nejede žádný spoj linky ${state.filtrLinky}"
            else "Přes tuto zastávku nejede žádný spoj linky ${state.filtrLinky}, který zastavuje na zastávce ${state.filtrZastavky}",
            Modifier.padding(horizontal = 16.dp)
        )
    }
    else LazyColumn(
        state = listState,
        modifier = Modifier.padding(top = 16.dp)
    ) {
        items(
            items = seznam,
            key = { it.idSpoje to it.cas },
            itemContent = { karticka ->
                Karticka(
                    karticka, kliklNaDetailSpoje, state.kompaktniRezim, modifier = Modifier
                        .animateContentSize()
                        .animateItemPlacement()
                )
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Karticka(
    kartickaState: KartickaState,
    detailSpoje: (KartickaState) -> Unit,
    zjednodusit: Boolean,
    modifier: Modifier = Modifier,
) {
    Divider(modifier)
    Surface(
        onClick = {
            detailSpoje(kartickaState)
        },
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .padding(top = 4.dp, start = 16.dp, end = 16.dp, bottom = 8.dp)
        ) {
            val nasledujiciZastavka = kartickaState.aktualniNasledujiciZastavka
            val zpozdeni = kartickaState.zpozdeni
            val jedeZa = kartickaState.jedeZa

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
            ) {
                IconWithTooltip(
                    imageVector = when {
                        kartickaState.nizkopodlaznost -> Icons.Default.Accessible
                        else -> Icons.Default.NotAccessible
                    },
                    contentDescription = "Invalidní vozík",
                )
                Text(
                    modifier = Modifier.padding(start = 8.dp),
                    text = kartickaState.cisloLinky.toString(),
                    fontSize = 30.sp
                )
                Text(
                    modifier = Modifier,
                    text = " -> ",
                    fontSize = 20.sp
                )
                Text(
                    modifier = Modifier
                        .weight(1F),
                    text = kartickaState.konecna,
                    fontSize = 20.sp
                )
                if (zjednodusit) Text(
                    text = if (jedeZa == null || jedeZa < Duration.ZERO) "${kartickaState.cas}"
                    else jedeZa.asString(),
                    color = if (zpozdeni == null || jedeZa == null || jedeZa < Duration.ZERO) MaterialTheme.colorScheme.onSurface
                    else barvaZpozdeniTextu(zpozdeni),
                ) else {
                    Text(
                        text = "${kartickaState.cas}"
                    )
                    if (zpozdeni != null) Text(
                        text = "${kartickaState.cas.plusMinutes(zpozdeni.toLong())}",
                        color = barvaZpozdeniTextu(zpozdeni),
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }

            if (nasledujiciZastavka != null && zpozdeni != null && !zjednodusit) {
                Text(text = "Následující zastávka:", style = MaterialTheme.typography.labelMedium)
                Row(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        modifier = Modifier
                            .weight(1F),
                        text = nasledujiciZastavka.first,
                        fontSize = 20.sp
                    )
                    Text(
                        text = nasledujiciZastavka.second.toString(),
                        color = barvaZpozdeniTextu(zpozdeni)
                    )
                }
            }
        }
    }
}

fun Duration.asString(): String {
    val hodin = toHours()
    val minut = (toMinutes() % 60).toInt()

    return when {
        hodin == 0L && minut == 0 -> "<1 min"
        hodin == 0L -> "$minut min"
        minut == 0 -> "$hodin hod"
        else -> "$hodin hod $minut min"
    }
}
