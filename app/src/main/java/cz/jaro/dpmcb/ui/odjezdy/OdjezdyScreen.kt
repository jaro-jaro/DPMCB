package cz.jaro.dpmcb.ui.odjezdy

import android.annotation.SuppressLint
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Accessible
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.NotAccessible
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.style.TextAlign
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
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.hezky6p
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.navigateFunction
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.ted
import cz.jaro.dpmcb.data.helperclasses.Vysledek
import cz.jaro.dpmcb.ui.destinations.VybiratorDestination
import cz.jaro.dpmcb.ui.main.SuplikAkce
import cz.jaro.dpmcb.ui.odjezdy.OdjezdyEvent.Vybral
import cz.jaro.dpmcb.ui.odjezdy.OdjezdyEvent.ZmenilJenOdjezdy
import cz.jaro.dpmcb.ui.odjezdy.OdjezdyEvent.ZmenilKompaktniRezim
import cz.jaro.dpmcb.ui.odjezdy.OdjezdyEvent.ZmenitCas
import cz.jaro.dpmcb.ui.odjezdy.OdjezdyEvent.Zrusil
import cz.jaro.dpmcb.ui.vybirator.TypVybiratoru
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.time.temporal.ChronoUnit
import kotlin.math.abs
import kotlin.math.roundToLong

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
                viewModel.onEvent(Vybral(result.value))
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
            delay(500)
            listState.scrollToItem(it)
        }
        viewModel.navigovat = navigator.navigateFunction
    }

    LaunchedEffect(listState) {
        withContext(Dispatchers.IO) {
            snapshotFlow { listState.firstVisibleItemIndex }
                .flowOn(Dispatchers.IO)
                .collect {
                    viewModel.onEvent(OdjezdyEvent.Scrolluje(it))
                }
        }
    }

    val jeOnline by viewModel.maPristupKJihu.collectAsStateWithLifecycle()
    val datum by viewModel.datum.collectAsStateWithLifecycle()

    OdjezdyScreen(
        info = info,
        state = state,
        zastavka = zastavka,
        onEvent = viewModel::onEvent,
        listState = listState,
        datum = datum,
        navigate = navigator.navigateFunction,
        jeOnline = jeOnline,
    )
}

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OdjezdyScreen(
    state: OdjezdyState,
    info: OdjezdyInfo,
    zastavka: String,
    listState: LazyListState,
    jeOnline: Boolean,
    datum: LocalDate,
    onEvent: (OdjezdyEvent) -> Unit,
    navigate: NavigateFunction,
) = Scaffold(
    floatingActionButton = {
        if (state is OdjezdyState.Jede) {
            val scope = rememberCoroutineScope()
            val i by remember { derivedStateOf { listState.firstVisibleItemIndex } }
            val jeNahore = !listState.canScrollBackward
            val jeDole = !listState.canScrollForward

            val schovatSipkuProtozeJeMocDole = jeDole && i < state.seznam.domov(info)
            val schovatSipkuProtozeJePobliz = abs(i - state.seznam.domov(info)) <= 1

            AnimatedVisibility(
                visible = !schovatSipkuProtozeJePobliz && !schovatSipkuProtozeJeMocDole,
                enter = fadeIn(spring(stiffness = Spring.StiffnessVeryLow)),
                exit = fadeOut(spring(stiffness = Spring.StiffnessVeryLow)),
            ) {
                SmallFloatingActionButton(
                    onClick = {
                        scope.launch(Dispatchers.Main) {
                            listState.animateScrollToItem(state.seznam.domov(info))
                        }
                    },
                ) {
                    IconWithTooltip(
                        Icons.Default.ArrowUpward,
                        "Scrollovat",
                        Modifier.rotate(
                            animateFloatAsState(
                                when {
                                    i > state.seznam.domov(info) -> 0F
                                    i < state.seznam.domov(info) -> 180F
                                    else -> 90F
                                }, label = "TOČENÍ"
                            ).value
                        )
                    )
                }
            }
        }
    }
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding()
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
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
                    onEvent(ZmenitCas(it))
                    zobrazitDialog = false
                },
                title = {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Změnit čas")
                        TextButton(
                            onClick = {
                                onEvent(ZmenitCas(LocalTime.now().truncatedTo(ChronoUnit.MINUTES)))
                                zobrazitDialog = false
                            }
                        ) {
                            Text("Teď")
                        }
                    }
                },
                initialTime = info.cas,
            )

            TextButton(
                onClick = {
                    zobrazitDialog = true
                }
            ) {
                Text(text = info.cas.toString())
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (jeOnline) Surface(
                checked = info.kompaktniRezim,
                onCheckedChange = {
                    onEvent(ZmenilKompaktniRezim)
                },
                Modifier.padding(all = 8.dp),
                shape = CircleShape,
            ) {
                Row(
                    Modifier.padding(all = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(checked = info.kompaktniRezim, onCheckedChange = {
                        onEvent(ZmenilKompaktniRezim)
                    })
                    Text("Zjednodušit")
                }
            }

            Spacer(modifier = Modifier.weight(1F))

            Surface(
                checked = info.jenOdjezdy,
                onCheckedChange = {
                    onEvent(ZmenilJenOdjezdy)
                },
                Modifier.padding(all = 8.dp),
                shape = CircleShape,
            ) {
                Row(
                    Modifier.padding(all = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(checked = info.jenOdjezdy, onCheckedChange = {
                        onEvent(ZmenilJenOdjezdy)
                    })
                    Text("Pouze odjezdy")
                }
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
                        onEvent(Zrusil(TypVybiratoru.LINKA_ZPET))
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
                        onEvent(Zrusil(TypVybiratoru.ZASTAVKA_ZPET))
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
                        OdjezdyState.VubecNicNejede -> "Přes tuto zastávku ${datum.hezky6p()} nic nejede"
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
                    count = state.seznam.size + 2,
                    key = { i ->
                        when (i) {
                            0 -> 0
                            state.seznam.lastIndex + 2 -> Int.MAX_VALUE
                            else -> state.seznam[i - 1].idSpoje to state.seznam[i - 1].cas
                        }
                    },
                    itemContent = { i ->
                        when (i) {
                            0 -> {
                                Surface(
                                    modifier = Modifier.clickable {
                                        onEvent(OdjezdyEvent.PredchoziDen)
                                    },
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center,
                                        modifier = Modifier
                                            .padding(top = 4.dp, start = 16.dp, end = 16.dp, bottom = 8.dp)
                                    ) {
                                        Text(
                                            modifier = Modifier
                                                .weight(1F),
                                            text = "Předchozí den…",
                                            fontSize = 20.sp,
                                            textAlign = TextAlign.Center,
                                        )
                                    }
                                }
                            }

                            state.seznam.lastIndex + 2 -> {
                                HorizontalDivider()
                                Surface(
                                    modifier = Modifier.clickable {
                                        onEvent(OdjezdyEvent.DalsiDen)
                                    },
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center,
                                        modifier = Modifier
                                            .padding(top = 4.dp, start = 16.dp, end = 16.dp, bottom = 8.dp)
                                    ) {
                                        Text(
                                            modifier = Modifier
                                                .weight(1F),
                                            text = "Následující den…",
                                            fontSize = 20.sp,
                                            textAlign = TextAlign.Center,
                                        )
                                    }
                                }
                            }

                            else -> Karticka(
                                state.seznam[i - 1], onEvent, info.kompaktniRezim, modifier = Modifier
                                    .animateContentSize()
//                                    .animateItemPlacement(spring(stiffness = Spring.StiffnessMediumLow))
                            )
                        }
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun Karticka(
    kartickaState: KartickaState,
    onEvent: (OdjezdyEvent) -> Unit,
    zjednodusit: Boolean,
    modifier: Modifier = Modifier,
) = Column(modifier) {
    var showDropDown by rememberSaveable { mutableStateOf(false) }
    Divider()
    Surface(
        modifier = Modifier.combinedClickable(
            onClick = {
                onEvent(OdjezdyEvent.KliklNaSpoj(kartickaState))
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
                        onEvent(OdjezdyEvent.KliklNaZjr(kartickaState))
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
                    when {
                        kartickaState.potvrzenaNizkopodlaznost == true -> Icons.AutoMirrored.Filled.Accessible
                        kartickaState.potvrzenaNizkopodlaznost == false -> Icons.Default.NotAccessible
                        kartickaState.nizkopodlaznost -> Icons.AutoMirrored.Filled.Accessible
                        else -> Icons.Default.NotAccessible
                    },
                    when {
                        kartickaState.potvrzenaNizkopodlaznost == true -> "Potvrzený nízkopodlažní vůz"
                        kartickaState.potvrzenaNizkopodlaznost == false -> "Potvrzený vysokopodlažní vůz"
                        kartickaState.nizkopodlaznost -> "Plánovaný nízkopodlažní vůz"
                        else -> "Nezaručený nízkopodlažní vůz"
                    },
                    tint = when {
                        kartickaState.potvrzenaNizkopodlaznost == false && kartickaState.nizkopodlaznost -> MaterialTheme.colorScheme.error
                        kartickaState.potvrzenaNizkopodlaznost != null -> MaterialTheme.colorScheme.primary
                        else -> MaterialTheme.colorScheme.onSurface
                    }
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

                if (zjednodusit) {
                    Text(
                        text = if (jedeZa < Duration.ZERO || (jedeZa > Duration.ofHours(1L) && zpozdeni == null)) "${kartickaState.cas}" else jedeZa.asString(),
                        color = if (zpozdeni == null || jedeZa < Duration.ZERO) MaterialTheme.colorScheme.onSurface else barvaZpozdeniTextu(zpozdeni),
                    )
                } else {
                    Text(
                        text = "${kartickaState.cas}"
                    )
                    if (zpozdeni != null && jedeZa > Duration.ZERO) Text(
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
                        text = nasledujiciZastavka.second.plusMinutes(zpozdeni.roundToLong()).toString(),
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
