package cz.jaro.dpmcb.ui.odjezdy

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Accessible
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.updateLayoutParams
import androidx.core.view.updateMargins
import com.google.accompanist.placeholder.PlaceholderHighlight
import com.google.accompanist.placeholder.placeholder
import com.google.accompanist.placeholder.shimmer
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import cz.jaro.dpmcb.R
import cz.jaro.dpmcb.data.App
import cz.jaro.dpmcb.data.helperclasses.Cas
import cz.jaro.dpmcb.data.helperclasses.Trvani.Companion.min
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.IconWithTooltip
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.barvaZpozdeniTextu
import cz.jaro.dpmcb.ui.UiEvent
import cz.jaro.dpmcb.ui.odjezdy.OdjezdyViewModel.KartickaState
import cz.jaro.dpmcb.ui.theme.DPMCBTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.ParametersHolder

@OptIn(ExperimentalCoroutinesApi::class)
@Destination
@Composable
fun OdjezdyScreen(
    zastavka: String,
    cas: Cas = Cas.ted,
    viewModel: OdjezdyViewModel = koinViewModel {
        ParametersHolder(mutableListOf(zastavka, cas))
    },
    navigator: DestinationsNavigator,
) {
    App.title = R.string.odjezdy

    val state by viewModel.state.collectAsState()

    val listState = rememberLazyListState(state.indexScrollovani)

    LaunchedEffect(Unit) {
        viewModel.scrollovat = {
            UtilFunctions.funguj("SKRÓLUJU")
            listState.scrollToItem(state.indexScrollovani)
        }
        viewModel.uiEvent.collect { event ->
            when (event) {
                is UiEvent.Navigovat -> {
                    navigator.navigate(event.kam)
                }

                else -> {}
            }
        }
    }

    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemIndex }
            .flowOn(Dispatchers.IO)
            .collect {
                viewModel.scrolluje(it)
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
                text = zastavka,
                fontSize = 20.sp
            )
            val ctx = LocalContext.current
            TextButton(
                onClick = {
                    MaterialAlertDialogBuilder(ctx).apply {
                        setTitle("Vybrat čas")

                        val ll = LinearLayout(context)

                        val tp = android.widget.TimePicker(context)
                        //dp.maxDate = Calendar.getInstance().apply { set(3000, 12, 30) }.timeInMillis
                        tp.layoutParams = LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.WRAP_CONTENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                        )
                        tp.updateLayoutParams<LinearLayout.LayoutParams> {
                            updateMargins(top = 16)
                        }

                        tp.setIs24HourView(true)

                        tp.hour = state.cas.h
                        tp.minute = state.cas.min

                        ll.addView(tp)

                        setView(ll)

                        setPositiveButton("Zvolit") { dialog, _ ->
                            dialog.cancel()

                            val novejCas = Cas(tp.hour, tp.minute)
                            viewModel.zmenitCas(novejCas)
                        }
                        show()
                    }
                }
            ) {
                Text(text = cas.toString())
            }
        }

        LazyColumn(
            state = listState,
        ) {
            items(
                count = Int.MAX_VALUE,
                itemContent = { i ->
                    val karticka by remember(state) {
                        derivedStateOf {
                            if (state.nacitaSe || state.seznam.isEmpty()) null else state.seznam[i % state.seznam.size]
                        }
                    }
                    Karticka(karticka, viewModel::kliklNaDetailSpoje, viewModel::kliklNaZjr)
                }
            )
        }
    }
}

private fun Modifier.applyIf(apply: Boolean, block: Modifier.() -> Modifier) = if (apply) block() else this

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Karticka(
    kartickaState: KartickaState?,
    detailSpoje: (KartickaState) -> Unit,
    zjr: (KartickaState) -> Unit,
) {

    OutlinedCard(
        onClick = {
            if (kartickaState == null) return@OutlinedCard
            detailSpoje(kartickaState)
        },
        modifier = Modifier
            .padding(bottom = 8.dp)

    ) {
        Column(
            modifier = Modifier
                .padding(all = 8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .applyIf(kartickaState == null) {
                        padding(all = 8.dp)
                    }
                    .placeholder(
                        visible = kartickaState == null,
                        color = Color.Gray,
                        highlight = PlaceholderHighlight.shimmer(
                            highlightColor = Color.DarkGray,
                        ),
                        shape = RoundedCornerShape(6.dp),
                    )
            ) {
                Text(
                    modifier = Modifier,
                    text = kartickaState?.cisloLinky?.toString() ?: "?",
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
                    text = kartickaState?.konecna ?: "???",
                    fontSize = 20.sp
                )
                Text(
                    text = "${kartickaState?.cas ?: "??:??"}"
                )
                val zpozdeni by (kartickaState?.zpozdeni ?: flowOf(null)).collectAsState(null)

                if (zpozdeni != null && kartickaState != null) Text(
                    text = "${kartickaState.cas + zpozdeni!!.min}",
                    color = barvaZpozdeniTextu(zpozdeni!!),
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconWithTooltip(
                    imageVector = when {
                        kartickaState?.nizkopodlaznost ?: false -> Icons.Default.Accessible
                        else -> Icons.Default.NotAccessible
                    },
                    contentDescription = "Invalidní vozík",
                    modifier = Modifier
                        .applyIf(kartickaState == null) {
                            padding(all = 8.dp)
                        }
                        .placeholder(
                            visible = kartickaState == null,
                            color = Color.Gray,
                            highlight = PlaceholderHighlight.shimmer(
                                highlightColor = Color.DarkGray,
                            ),
                            shape = RoundedCornerShape(6.dp),
                        )
                )
                TextButton(
                    onClick = {
                        if (kartickaState == null) return@TextButton
                        zjr(kartickaState)
                    },
                    enabled = kartickaState?.jePosledniZastavka == false,
                    modifier = Modifier
                        .applyIf(kartickaState == null) {
                            padding(all = 8.dp)
                        }
                        .placeholder(
                            visible = kartickaState == null,
                            color = Color.Gray,
                            highlight = PlaceholderHighlight.shimmer(
                                highlightColor = Color.DarkGray,
                            ),
                            shape = RoundedCornerShape(6.dp),
                        )
                ) {
                    Text(text = "Zastávkové jízdní řády")
                }
            }
        }
    }
}


@Preview(uiMode = UI_MODE_NIGHT_YES, device = "spec:width=1080px,height=2340px,dpi=480")
@Composable
fun Prev() {
    DPMCBTheme {
        Surface(
            modifier = Modifier
                .padding(16.dp)
        ) {
            Column {

                OutlinedCard(
                    modifier = Modifier
                        .padding(bottom = 8.dp)
                        .fillMaxWidth()

                ) {
                    Column(
                        modifier = Modifier
                            .padding(top = 4.dp, start = 8.dp, end = 8.dp, bottom = 8.dp)
                            .fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            IconWithTooltip(
                                imageVector = Icons.Default.Accessible,
                                contentDescription = "Invalidní vozík",
                            )
                            Text(
                                modifier = Modifier.padding(start = 8.dp),
                                text = "9",
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
                                text = "Suché Vrbné",
                                fontSize = 20.sp
                            )
                            Text(
                                text = "14:19"
                            )
                            Text(
                                text = "14:29",
                                modifier = Modifier.padding(start = 8.dp),
                                color = barvaZpozdeniTextu(zpozdeni = 10)
                            )
                        }
                        Text(text = "Následující zastávka:", style = MaterialTheme.typography.labelMedium)
                        Row(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                modifier = Modifier
                                    .weight(1F),
                                text = "Poliklinika Sever",
                                fontSize = 20.sp
                            )
                            Text(
                                text = "14:25",
                                color = barvaZpozdeniTextu(zpozdeni = 10)
                            )
                        }
                    }
                }
                OutlinedCard(
                    modifier = Modifier
                        .padding(bottom = 8.dp)
                        .fillMaxWidth()

                ) {
                    Column(
                        modifier = Modifier
                            .padding(top = 4.dp, start = 8.dp, end = 8.dp, bottom = 8.dp)
                            .fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            IconWithTooltip(
                                imageVector = Icons.Default.Accessible,
                                contentDescription = "Invalidní vozík",
                            )
                            Text(
                                modifier = Modifier.padding(start = 8.dp),
                                text = "9",
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
                                text = "Vltava",
                                fontSize = 20.sp
                            )
                            Text(
                                text = "14:10"
                            )
                            Text(
                                text = "14:10",
                                modifier = Modifier.padding(start = 8.dp),
                                color = barvaZpozdeniTextu(zpozdeni = 0)
                            )
                        }
                        Text(text = "Následující zastávka:", style = MaterialTheme.typography.labelMedium)
                        Row(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                modifier = Modifier
                                    .weight(1F),
                                text = "Nádraží",
                                fontSize = 20.sp
                            )
                            Text(
                                text = "14:01",
                                color = barvaZpozdeniTextu(zpozdeni = 0)
                            )
                        }
                    }
                }
                OutlinedCard(
                    modifier = Modifier
                        .padding(bottom = 8.dp)
                        .fillMaxWidth()

                ) {
                    Column(
                        modifier = Modifier
                            .padding(top = 4.dp, start = 8.dp, end = 8.dp, bottom = 8.dp)
                            .fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            IconWithTooltip(
                                imageVector = Icons.Default.Accessible,
                                contentDescription = "Invalidní vozík",
                            )
                            Text(
                                modifier = Modifier.padding(start = 8.dp),
                                text = "10",
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
                                text = "Na děkanských polích",
                                fontSize = 20.sp
                            )
                            Text(
                                text = "15:00"
                            )
                        }
                    }
                }
            }
        }
    }
}