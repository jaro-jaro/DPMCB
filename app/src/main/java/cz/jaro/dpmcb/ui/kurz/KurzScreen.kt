package cz.jaro.dpmcb.ui.kurz

import android.annotation.SuppressLint
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Accessible
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.NotAccessible
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Badge
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import cz.jaro.dpmcb.R
import cz.jaro.dpmcb.data.App
import cz.jaro.dpmcb.data.App.Companion.title
import cz.jaro.dpmcb.data.helperclasses.NavigateFunction
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.IconWithTooltip
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.barvaZpozdeniBublinyKontejner
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.barvaZpozdeniBublinyText
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.evC
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.navaznostKurzu
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.navigateFunction
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.nazevKurzu
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.rowItem
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.toSign
import cz.jaro.dpmcb.ui.destinations.KurzDestination
import cz.jaro.dpmcb.ui.destinations.SpojDestination
import cz.jaro.dpmcb.ui.main.SuplikAkce
import cz.jaro.dpmcb.ui.spoj.JizdniRad
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.ParametersHolder
import java.time.LocalTime
import kotlin.random.Random
import kotlin.time.Duration.Companion.minutes

@Destination
@Composable
fun Kurz(
    kurz: String,
    viewModel: KurzViewModel = koinViewModel {
        ParametersHolder(mutableListOf(kurz))
    },
    navigator: DestinationsNavigator,
) {
    title = R.string.detail_kurzu
    App.vybrano = SuplikAkce.NajitSpoj

    val state by viewModel.state.collectAsStateWithLifecycle()

    KurzScreen(
        state = state,
        navigate = navigator.navigateFunction,
        lazyListState = rememberLazyListState(),
    )
}

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
fun KurzScreen(
    state: KurzState,
    navigate: NavigateFunction,
    lazyListState: LazyListState,
) = Scaffold(
    floatingActionButton = {
        if (state is KurzState.OK) FABy(state, lazyListState)
    }
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp),
        state = lazyListState
    ) {
        when (state) {
            is KurzState.Loading -> rowItem(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator()
            }

            is KurzState.Neexistuje -> rowItem(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                Text("Tento kurz (${state.kurz.nazevKurzu()}) bohužel neexistuje :(\nZkontrolujte, zda jste zadali správně ID.")
            }

            is KurzState.OK -> {
                item {
                    FlowRow(
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Nazev(state.kurz.nazevKurzu())

                        if (state is KurzState.OK.Online && state.potvrzenaNizkopodlaznost != null) Vozickar(
                            nizkopodlaznost = state.potvrzenaNizkopodlaznost,
                            potvrzenaNizkopodlaznost = state.potvrzenaNizkopodlaznost,
                            modifier = Modifier.padding(start = 8.dp),
                        )

                        if (state is KurzState.OK.Online) BublinaZpozdeni(state.zpozdeniMin)
                        if (state is KurzState.OK.Online) Vuz(state.vuz)
                    }
                }

                item {
                    state.pevneKody.forEach {
                        Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    state.caskody.forEach {
                        Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                item {
                    HorizontalDivider(Modifier.fillMaxWidth())
                }

                state.navaznostiPredtim.forEach {
                    item {
                        Navaznost(navigate, it)
                    }
                    item {
                        HorizontalDivider(Modifier.fillMaxWidth())
                    }
                }

                state.spoje.forEach { spoj ->
                    stickyHeader {
                        Surface {
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Nazev("${spoj.cisloLinky}")
                                Vozickar(
                                    nizkopodlaznost = spoj.nizkopodlaznost,
                                    potvrzenaNizkopodlaznost = (state as? KurzState.OK.Online)?.potvrzenaNizkopodlaznost?.takeIf { spoj.jede },
                                    modifier = Modifier.padding(start = 8.dp)
                                )

                                Spacer(Modifier.weight(1F))

                                TlacitkoSpoje(navigate, spoj)
                            }
                        }
                        Surface {
                            if (spoj.jede && state is KurzState.OK.Online) Row(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp, horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                BublinaZpozdeni(state.zpozdeniMin)
                                Vuz(state.vuz)
                            }
                        }

                    }
                    item {
                        OutlinedCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp)
                        ) {
                            JizdniRad(
                                zastavky = spoj.zastavky,
                                navigate = navigate,
                                zastavkyNaJihu = null,
                                pristiZastavka = null,
                                zobrazitCaru = spoj.jede
                            )
                        }
                    }
                    item {
                        HorizontalDivider(
                            Modifier
                                .fillMaxWidth()
                                .padding(top = 16.dp)
                        )
                    }
                }

                stickyHeader { }

                state.navaznostiPotom.forEach {
                    item {
                        Navaznost(navigate, it)
                    }
                    item {
                        HorizontalDivider(Modifier.fillMaxWidth())
                    }
                }
            }
        }
    }
}

@Composable
private fun FABy(state: KurzState.OK, lazyListState: LazyListState) {
    fun Int.indexSpojeNaIndex() = 3 + state.navaznostiPredtim.count() * 2 + this * 3

    val ted = remember(state.spoje) {
        state.spoje.indexOfFirst {
            it.jede
        }.takeUnless {
            it == -1
        } ?: state.spoje.indexOfFirst {
            LocalTime.now() < it.zastavky.first().cas
        }.takeIf {
            state.jedeDnes && state.spoje.first().zastavky.first().cas < LocalTime.now() && LocalTime.now() < state.spoje.last().zastavky.last().cas
        }
    }

    val scope = rememberCoroutineScope()
    Column {
        SmallFloatingActionButton(
            onClick = {
                scope.launch {
                    lazyListState.animateScrollToItem(0)
                }
            },
        ) {
            Icon(
                imageVector = Icons.Default.ArrowUpward,
                contentDescription = null
            )
        }
        if (ted != null) SmallFloatingActionButton(
            onClick = {
                scope.launch {
                    lazyListState.animateScrollToItem(ted.indexSpojeNaIndex())
                }
            },
        ) {
            Icon(
                imageVector = Icons.Default.GpsFixed,
                contentDescription = null
            )
        }
        SmallFloatingActionButton(
            onClick = {
                scope.launch {
                    lazyListState.animateScrollToItem(Int.MAX_VALUE)
                }
            },
        ) {
            Icon(
                imageVector = Icons.Default.ArrowDownward,
                contentDescription = null
            )
        }
    }
}

@Composable
private fun TlacitkoSpoje(
    navigate: NavigateFunction,
    spoj: SpojKurzuState,
) = TextButton(
    onClick = {
        navigate(SpojDestination(spojId = spoj.spojId))
    }
) {
    Text("Detail spoje")
}

@Composable
private fun Navaznost(
    navigate: NavigateFunction,
    kurz: String,
) = TextButton(
    onClick = {
        navigate(KurzDestination(kurz))
    }
) {
    Text(kurz.navaznostKurzu())
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun Vuz(vuz: Int?) {
    if (vuz != null) {
        Text(
            text = "ev. č. ${vuz.evC()}",
            Modifier.padding(horizontal = 8.dp)
        )
        val context = LocalContext.current
        IconWithTooltip(
            Icons.Default.Info,
            "Zobrazit informace o voze",
            Modifier.clickable {
                CustomTabsIntent.Builder()
                    .setShowTitle(true)
                    .build()
                    .launchUrl(context, Uri.parse("https://seznam-autobusu.cz/seznam?operatorName=DP+města+České+Budějovice&prov=1&evc=$vuz"))
            },
        )
    }
}

@Composable
fun BublinaZpozdeni(zpozdeniMinut: Float) {
    Badge(
        containerColor = barvaZpozdeniBublinyKontejner(zpozdeniMinut),
        contentColor = barvaZpozdeniBublinyText(zpozdeniMinut),
    ) {
        Text(
            text = zpozdeniMinut.toDouble().minutes.run {
                "${inWholeSeconds.toSign()}$inWholeMinutes min ${inWholeSeconds % 60} s"
            },
        )
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun Vozickar(
    nizkopodlaznost: Boolean,
    potvrzenaNizkopodlaznost: Boolean?,
    modifier: Modifier = Modifier,
    povolitVozik: Boolean = false,
) {
    IconWithTooltip(
        imageVector = remember(nizkopodlaznost, potvrzenaNizkopodlaznost) {
            when {
                povolitVozik && Random.nextFloat() < .01F -> Icons.Default.ShoppingCart
                potvrzenaNizkopodlaznost == true -> Icons.AutoMirrored.Filled.Accessible
                potvrzenaNizkopodlaznost == false -> Icons.Default.NotAccessible
                nizkopodlaznost -> Icons.AutoMirrored.Filled.Accessible
                else -> Icons.Default.NotAccessible
            }
        },
        contentDescription = when {
            potvrzenaNizkopodlaznost == true -> "Potvrzený nízkopodlažní vůz"
            potvrzenaNizkopodlaznost == false -> "Potvrzený vysokopodlažní vůz"
            nizkopodlaznost -> "Plánovaný nízkopodlažní vůz"
            else -> "Nezaručený nízkopodlažní vůz"
        },
        modifier,
        tint = when {
            potvrzenaNizkopodlaznost == false && nizkopodlaznost -> MaterialTheme.colorScheme.error
            potvrzenaNizkopodlaznost != null -> MaterialTheme.colorScheme.primary
            else -> MaterialTheme.colorScheme.onSurface
        }
    )
}

@Composable
fun Nazev(nazev: String) {
    Text(nazev, fontSize = 24.sp, color = MaterialTheme.colorScheme.primary)
}