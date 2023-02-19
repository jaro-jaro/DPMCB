package cz.jaro.dpmcb.ui.odjezdy

import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TimePicker
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Accessible
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.NotAccessible
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusEvent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.updateLayoutParams
import androidx.core.view.updateMargins
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import com.ramcosta.composedestinations.result.NavResult
import com.ramcosta.composedestinations.result.ResultRecipient
import cz.jaro.dpmcb.R
import cz.jaro.dpmcb.data.App
import cz.jaro.dpmcb.data.helperclasses.Cas
import cz.jaro.dpmcb.data.helperclasses.Trvani.Companion.min
import cz.jaro.dpmcb.data.helperclasses.TypAdapteru
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.IconWithTooltip
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.barvaZpozdeniTextu
import cz.jaro.dpmcb.ui.destinations.VybiratorScreenDestination
import cz.jaro.dpmcb.ui.odjezdy.OdjezdyViewModel.KartickaState
import cz.jaro.dpmcb.ui.vybirator.Vysledek
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.ParametersHolder

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Destination
@Composable
fun OdjezdyScreen(
    zastavka: String,
    cas: Cas = Cas.ted,
    viewModel: OdjezdyViewModel = koinViewModel {
        ParametersHolder(mutableListOf(zastavka, cas))
    },
    navigator: DestinationsNavigator,
    resultRecipient: ResultRecipient<VybiratorScreenDestination, Vysledek>,
) {
    resultRecipient.onNavResult { result ->
        when (result) {
            is NavResult.Canceled -> {}
            is NavResult.Value -> {
                viewModel.vybral(result.value)
            }
        }
    }

    App.title = R.string.odjezdy

    val state by viewModel.state.collectAsState()

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

    if (state.nacitaSe) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())

    Column(
        modifier = Modifier
            .fillMaxSize()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .padding(horizontal = 16.dp)
        ) {
            Text(
                text = zastavka,
                fontSize = 20.sp
            )
            val ctx = LocalContext.current
            TextButton(
                onClick = {
                    MaterialAlertDialogBuilder(ctx).apply {
                        setTitle("Vybrat čas")

                        setView(LinearLayout(context).apply {
                            addView(TimePicker(context).apply {
                                layoutParams = LinearLayout.LayoutParams(
                                    ViewGroup.LayoutParams.WRAP_CONTENT,
                                    ViewGroup.LayoutParams.WRAP_CONTENT
                                )
                                updateLayoutParams<LinearLayout.LayoutParams> {
                                    updateMargins(top = 16)
                                }

                                setIs24HourView(true)

                                hour = state.cas.h
                                minute = state.cas.min

                                setPositiveButton("Zvolit") { dialog, _ ->
                                    dialog.cancel()

                                    val novejCas = Cas(hour, minute)
                                    viewModel.zmenitCas(novejCas)
                                }
                            })
                        })

                        show()
                    }
                }
            ) {
                Text(text = state.cas.toString())
            }
        }

        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp)
        ) {
            val focusRequester = LocalFocusManager.current

            Text(text = "Filtry")
            TextField(
                value = state.filtrLinky?.toString() ?: "Všechny",
                onValueChange = {},
                Modifier
                    .onFocusEvent {
                        if (!it.hasFocus) return@onFocusEvent
                        navigator.navigate(
                            VybiratorScreenDestination(
                                typ = TypAdapteru.LINKA_ZPET,
                            )
                        )
                        focusRequester.clearFocus()
                    }
                    .fillMaxWidth(),
                label = {
                    Text(text = "Linka:")
                },
                readOnly = true,
                trailingIcon = {
                    if (state.filtrLinky != null) IconButton(onClick = {
                        viewModel.zrusil(TypAdapteru.LINKA_ZPET)
                    }) {
                        IconWithTooltip(imageVector = Icons.Default.Clear, contentDescription = "Vymazat")
                    }
                },
                colors = TextFieldDefaults.textFieldColors(
                    textColor = state.filtrLinky?.let { MaterialTheme.colorScheme.onSurface } ?: MaterialTheme.colorScheme.onSurfaceVariant
                ),
            )
            TextField(
                value = state.filtrZastavky ?: "Cokoliv",
                onValueChange = {},
                Modifier
                    .onFocusEvent {
                        if (!it.hasFocus) return@onFocusEvent
                        navigator.navigate(
                            VybiratorScreenDestination(
                                typ = TypAdapteru.ZASTAVKA_ZPET,
                            )
                        )
                        focusRequester.clearFocus()
                    }
                    .fillMaxWidth(),

                label = {
                    Text(text = "Jede přes:")
                },
                readOnly = true,
                trailingIcon = {
                    if (state.filtrZastavky != null) IconButton(onClick = {
                        viewModel.zrusil(TypAdapteru.ZASTAVKA_ZPET)
                    }) {
                        IconWithTooltip(imageVector = Icons.Default.Clear, contentDescription = "Vymazat")
                    }
                },
                colors = TextFieldDefaults.textFieldColors(
                    textColor = state.filtrZastavky?.let { MaterialTheme.colorScheme.onSurface } ?: MaterialTheme.colorScheme.onSurfaceVariant
                ),
            )
        }
        if (state.nacitaSe || state.filtrovanejSeznam.isEmpty()) Row(
            Modifier
                .padding(top = 8.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            CircularProgressIndicator()
        }
        else LazyColumn(
            state = listState,
            modifier = Modifier.padding(top = 16.dp)
        ) {
            items(
                count = Int.MAX_VALUE,
                itemContent = { i ->
                    val karticka by remember(state) {
                        derivedStateOf {
                            state.filtrovanejSeznam[i % state.filtrovanejSeznam.size]
                        }
                    }
                    Karticka(
                        karticka, viewModel::kliklNaDetailSpoje, modifier = Modifier
                            .animateContentSize()
                            .animateItemPlacement()
                    )
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Karticka(
    kartickaState: KartickaState,
    detailSpoje: (KartickaState) -> Unit,
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
            val nasledujiciZastavka by kartickaState.aktualniNasledujiciZastavka
            val zpozdeni by kartickaState.zpozdeni

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
                Text(
                    text = "${kartickaState.cas}"
                )
                if (zpozdeni != null) Text(
                    text = "${kartickaState.cas + zpozdeni!!.min}",
                    color = barvaZpozdeniTextu(zpozdeni!!),
                    modifier = Modifier.padding(start = 8.dp)
                )
            }

            if (nasledujiciZastavka != null && zpozdeni != null) {
                Text(text = "Následující zastávka:", style = MaterialTheme.typography.labelMedium)
                Row(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        modifier = Modifier
                            .weight(1F),
                        text = nasledujiciZastavka!!.nazevZastavky,
                        fontSize = 20.sp
                    )
                    Text(
                        text = nasledujiciZastavka!!.cas.toString(),
                        color = barvaZpozdeniTextu(zpozdeni!!)
                    )
                }
            }
        }
    }
}