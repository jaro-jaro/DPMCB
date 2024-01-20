package cz.jaro.dpmcb.ui.spoj

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Accessible
import androidx.compose.material.icons.filled.NotAccessible
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Badge
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.navigateFunction
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.toSign
import cz.jaro.dpmcb.ui.destinations.SpojDestination
import cz.jaro.dpmcb.ui.kurz.KurzState
import cz.jaro.dpmcb.ui.kurz.KurzViewModel
import cz.jaro.dpmcb.ui.kurz.SpojKurzuState
import cz.jaro.dpmcb.ui.main.SuplikAkce
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.ParametersHolder
import kotlin.random.Random
import kotlin.time.Duration.Companion.minutes

@Destination
@Composable
fun Kurz(
    spojIds: Array<String>,
    viewModel: KurzViewModel = koinViewModel {
        ParametersHolder(mutableListOf(spojIds.toList()))
    },
    navigator: DestinationsNavigator,
) {
    title = R.string.detail_kurzu
    App.vybrano = SuplikAkce.Kurz

    val state by viewModel.state.collectAsStateWithLifecycle()

    KurzScreen(
        state = state,
        navigate = navigator.navigateFunction,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KurzScreen(
    state: KurzState,
    navigate: NavigateFunction,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp)
            .verticalScroll(rememberScrollState())
    ) {
        when (state) {
            is KurzState.Loading -> Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator()
            }

            is KurzState.Neexistuje -> Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                Text("Tento kurz (ID ${state.kurzId}) bohužel neexistuje :(\nZkontrolujte, zda jste zadali správně ID.")
            }

            is KurzState.OK -> {
                state.spoje.forEach { spoj ->
                    if (spoj.spojId.startsWith("S-")) Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(8.dp), verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "Linka ${spoj.cisloLinky}", fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)
                        IconWithTooltip(
                            remember(spoj.nizkopodlaznost) {
                                when {
                                    Random.nextFloat() < .01F -> Icons.Default.ShoppingCart
                                    spoj is SpojKurzuState.Online && spoj.potvrzenaNizkopodlaznost == true -> Icons.Default.Accessible
                                    spoj is SpojKurzuState.Online && spoj.potvrzenaNizkopodlaznost == false -> Icons.Default.NotAccessible
                                    spoj.nizkopodlaznost -> Icons.Default.Accessible
                                    else -> Icons.Default.NotAccessible
                                }
                            },
                            when {
                                spoj is SpojKurzuState.Online && spoj.potvrzenaNizkopodlaznost == true -> "Potvrzený nízkopodlažní vůz"
                                spoj is SpojKurzuState.Online && spoj.potvrzenaNizkopodlaznost == false -> "Potvrzený vysokopodlažní vůz"
                                spoj.nizkopodlaznost -> "Plánovaný nízkopodlažní vůz"
                                else -> "Nezaručený nízkopodlažní vůz"
                            },
                            Modifier.padding(start = 8.dp),
                            tint = when {
                                spoj is SpojKurzuState.Online && spoj.potvrzenaNizkopodlaznost == false && spoj.nizkopodlaznost -> MaterialTheme.colorScheme.error
                                spoj is SpojKurzuState.Online && spoj.potvrzenaNizkopodlaznost != null -> MaterialTheme.colorScheme.primary
                                else -> MaterialTheme.colorScheme.onSurface
                            }
                        )
                        if (spoj is SpojKurzuState.Online) Badge(
                            containerColor = barvaZpozdeniBublinyKontejner(spoj.zpozdeniMin),
                            contentColor = barvaZpozdeniBublinyText(spoj.zpozdeniMin),
                        ) {
                            Text(
                                text = spoj.zpozdeniMin.toDouble().minutes.run {
                                    "${inWholeSeconds.toSign()}$inWholeMinutes min ${inWholeSeconds % 60} s"
                                },
                            )
                        }
                        if (spoj is SpojKurzuState.Online && spoj.vuz != null) {
                            Text(
                                text = "ev. č. ${spoj.vuz.evC()}",
                                Modifier.padding(horizontal = 8.dp)
                            )
                        }
                        FilledTonalButton(
                            onClick = {
                                navigate(SpojDestination(spojId = spoj.spojId))
                            }
                        ) {
                            Text(spoj.spojId)
                        }
                    }
                    OutlinedCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()

                                .height(IntrinsicSize.Max)
                                .padding(12.dp)
                        ) {
                            Column(
                                Modifier.weight(1F)
                            ) {
                                spoj.zastavky.forEachIndexed { i, it ->
                                    MujText(
                                        text = it.nazev,
                                        navigate = navigate,
                                        cas = it.cas,
                                        zastavka = it.nazev,
                                        pristiZastavka = spoj.zastavky.getOrNull(i + 1)?.nazev,
                                        linka = spoj.cisloLinky,
                                        stanoviste = "",
                                        Modifier.fillMaxWidth(1F),
                                    )
                                }
                            }
                            Column(
                                Modifier.padding(start = 8.dp)
                            ) {
                                spoj.zastavky.forEachIndexed { i, it ->
                                    MujText(
                                        text = it.cas.toString(),
                                        navigate = navigate,
                                        cas = it.cas,
                                        zastavka = it.nazev,
                                        pristiZastavka = spoj.zastavky.getOrNull(i + 1)?.nazev,
                                        linka = spoj.cisloLinky,
                                        stanoviste = "",
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