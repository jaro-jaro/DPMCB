package cz.jaro.dpmcb.ui.odjezdy

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Accessible
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.NotAccessible
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
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
import cz.jaro.dpmcb.data.App.Companion.title
import cz.jaro.dpmcb.data.helperclasses.NavigateFunction
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.IconWithTooltip
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.barvaZpozdeniTextu
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.navigateFunction
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.ted
import cz.jaro.dpmcb.data.helperclasses.Vysledek
import cz.jaro.dpmcb.ui.destinations.VybiratorDestination
import cz.jaro.dpmcb.ui.main.SuplikAkce
import cz.jaro.dpmcb.ui.vybirator.TypVybiratoru
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf
import java.time.Duration
import java.time.LocalTime

@Destination
@Composable
fun Odjezdy(
    zastavka: String,
    cas: LocalTime? = null,
    linka: Int? = null,
    pres: String? = null,
    viewModel: OdjezdyViewModel = koinViewModel {
        parametersOf(
            OdjezdyViewModel.Parameters(
                zastavka = zastavka,
                cas = cas ?: ted,
                linka = linka,
                pres = pres
            )
        )
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

    val info by viewModel.info.collectAsStateWithLifecycle()
    val state by viewModel.state.collectAsStateWithLifecycle()

    val listState = rememberLazyListState(info.indexScrollovani)

    LaunchedEffect(Unit) {
        viewModel.scrollovat = {
            listState.scrollToItem(it)
        }
        viewModel.navigovat = navigator.navigateFunction
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

    val jeOnline by viewModel.maPristupKJihu.collectAsStateWithLifecycle()

    OdjezdyScreen(
        info = info,
        state = state,
        zastavka = zastavka,
        zmenitCas = viewModel::zmenitCas,
        zmenilKompaktniRezim = viewModel::zmenilKompaktniRezim,
        listState = listState,
        zrusil = viewModel::zrusil,
        kliklNaSpoj = viewModel::kliklNaSpoj,
        kliklNaZjr = viewModel::kliklNaZjr,
        navigate = navigator.navigateFunction,
        jeOnline = jeOnline,
    )
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun OdjezdyScreen(
    state: OdjezdyState,
    info: OdjezdyInfo,
    zastavka: String,
    listState: LazyListState,
    jeOnline: Boolean,
    zmenitCas: (LocalTime) -> Unit,
    zmenilKompaktniRezim: () -> Unit,
    zrusil: (TypVybiratoru) -> Unit,
    kliklNaSpoj: (KartickaState) -> Unit,
    kliklNaZjr: (KartickaState) -> Unit,
    navigate: NavigateFunction,
) = Column(
    modifier = Modifier
        .fillMaxSize()
) {
    FlowRow(
        modifier = Modifier
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.Bottom
    ) {
        TextButton(
            onClick = {
                navigate(VybiratorDestination(TypVybiratoru.ZASTAVKY))
            },
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
            initialTime = info.cas
        )

        TextButton(
            onClick = {
                zobrazitDialog = true
            }
        ) {
            Text(text = info.cas.toString())
        }
        Spacer(modifier = Modifier.weight(1F))

        if (jeOnline) Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Zjednodušit")
            Switch(checked = info.kompaktniRezim, onCheckedChange = {
                zmenilKompaktniRezim()
            }, Modifier.padding(all = 8.dp))
        }
    }

    Column(
        modifier = Modifier
            .padding(horizontal = 16.dp)
    ) {
        val linkaSource = remember { MutableInteractionSource() }
        val containerColor = MaterialTheme.colorScheme.surfaceVariant
        TextField(
            value = info.filtrLinky?.toString() ?: "Všechny",
            onValueChange = {},
            Modifier
                .fillMaxWidth(),
            label = {
                Text(text = "Linka:")
            },
            interactionSource = linkaSource,
            readOnly = true,
            trailingIcon = {
                if (info.filtrLinky != null) IconButton(onClick = {
                    zrusil(TypVybiratoru.LINKA_ZPET)
                }) {
                    IconWithTooltip(imageVector = Icons.Default.Clear, contentDescription = "Vymazat")
                }
            },
            colors = TextFieldDefaults.colors(
                focusedTextColor = info.filtrLinky?.let { MaterialTheme.colorScheme.onSurface } ?: MaterialTheme.colorScheme.onSurfaceVariant,
                unfocusedTextColor = info.filtrLinky?.let { MaterialTheme.colorScheme.onSurface } ?: MaterialTheme.colorScheme.onSurfaceVariant,
                focusedContainerColor = containerColor,
                unfocusedContainerColor = containerColor,
                disabledContainerColor = containerColor,
            ),
        )
        val linkaPressedState by linkaSource.interactions.collectAsStateWithLifecycle(PressInteraction.Cancel(PressInteraction.Press(Offset.Zero)))
        if (linkaPressedState is PressInteraction.Release) {
            navigate(
                VybiratorDestination(
                    typ = TypVybiratoru.LINKA_ZPET,
                )
            )
            linkaSource.tryEmit(PressInteraction.Cancel(PressInteraction.Press(Offset.Zero)))
        }
        val zastavkaSource = remember { MutableInteractionSource() }
        TextField(
            value = info.filtrZastavky ?: "Cokoliv",
            onValueChange = {},
            Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),

            label = {
                Text(text = "Pojede přes:")
            },
            readOnly = true,
            trailingIcon = {
                if (info.filtrZastavky != null) IconButton(onClick = {
                    zrusil(TypVybiratoru.ZASTAVKA_ZPET)
                }) {
                    IconWithTooltip(imageVector = Icons.Default.Clear, contentDescription = "Vymazat")
                }
            },
            interactionSource = zastavkaSource,
            colors = TextFieldDefaults.colors(
                focusedTextColor = info.filtrZastavky?.let { MaterialTheme.colorScheme.onSurface } ?: MaterialTheme.colorScheme.onSurfaceVariant,
                unfocusedTextColor = info.filtrZastavky?.let { MaterialTheme.colorScheme.onSurface } ?: MaterialTheme.colorScheme.onSurfaceVariant,
                focusedContainerColor = containerColor,
                unfocusedContainerColor = containerColor,
                disabledContainerColor = containerColor,
            ),
        )
        val zastavkaPressedState by zastavkaSource.interactions.collectAsStateWithLifecycle(PressInteraction.Cancel(PressInteraction.Press(Offset.Zero)))
        if (zastavkaPressedState is PressInteraction.Release) {
            navigate(
                VybiratorDestination(
                    typ = TypVybiratoru.ZASTAVKA_ZPET,
                )
            )
            zastavkaSource.tryEmit(PressInteraction.Cancel(PressInteraction.Press(Offset.Zero)))
        }
    }
    when (state) {
        OdjezdyState.Loading -> Row(
            Modifier
                .padding(top = 8.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            CircularProgressIndicator()
        }

        is OdjezdyState.NicNejede -> Row(
            Modifier
                .padding(top = 8.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                when (state) {
                    OdjezdyState.VubecNicNejede -> "Přes tuto zastávku nic nejede"
                    OdjezdyState.SemNicNejede -> "Přes tuto zastávku nejede žádný spoj, který bude zastavovat na zastávce ${info.filtrZastavky}"
                    OdjezdyState.LinkaNejede -> "Přes tuto zastávku nejede žádný spoj linky ${info.filtrLinky}"
                    OdjezdyState.LinkaSemNejede -> "Přes tuto zastávku nejede žádný spoj linky ${info.filtrLinky}, který bude zastavovat na zastávce ${info.filtrZastavky}"
                },
                Modifier.padding(horizontal = 16.dp)
            )
        }

        is OdjezdyState.Jede -> LazyColumn(
            state = listState,
            modifier = Modifier.padding(top = 16.dp)
        ) {
            items(
                items = state.seznam,
                key = { it.idSpoje to it.cas },
                itemContent = { karticka ->
                    Karticka(
                        karticka, kliklNaSpoj, kliklNaZjr, info.kompaktniRezim, modifier = Modifier
                            .animateContentSize()
                            .animateItemPlacement()
                    )
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun Karticka(
    kartickaState: KartickaState,
    detailSpoje: (KartickaState) -> Unit,
    zjr: (KartickaState) -> Unit,
    zjednodusit: Boolean,
    modifier: Modifier = Modifier,
) {
    Divider(modifier)
    var showDropDown by rememberSaveable { mutableStateOf(false) }
    Surface(
        modifier = modifier.combinedClickable(
            onClick = {
                detailSpoje(kartickaState)
            },
            onLongClick = {
                showDropDown = true
            }
        )
    ) {
        kartickaState.pristiZastavka?.let {
            DropdownMenu(
                expanded = showDropDown,
                onDismissRequest = {
                    showDropDown = false
                }
            ) {
                DropdownMenuItem(
                    text = {
                        Text("Zobrazit zastávkové JŘ")
                    },
                    onClick = {
                        zjr(kartickaState)
                        showDropDown = false
                    },
                )
            }
        }
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
                    contentDescription = if (kartickaState.nizkopodlaznost) "Nízkopodlažní vůz" else "Nenízkopodlažní vůz",
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
