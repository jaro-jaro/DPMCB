package cz.jaro.dpmcb.ui.kurz

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Accessible
import androidx.compose.material.icons.filled.NotAccessible
import androidx.compose.material3.Badge
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.rowItem
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.toSign
import cz.jaro.dpmcb.ui.destinations.KurzDestination
import cz.jaro.dpmcb.ui.destinations.SpojDestination
import cz.jaro.dpmcb.ui.main.SuplikAkce
import cz.jaro.dpmcb.ui.spoj.MujText
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.ParametersHolder
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
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun KurzScreen(
    state: KurzState,
    navigate: NavigateFunction,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp)
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
                Text("Tento kurz (${state.kurz}) bohužel neexistuje :(\nZkontrolujte, zda jste zadali správně ID.")
            }

            is KurzState.OK -> {
                item {
                    Text(state.kurz, fontSize = 20.sp)
                }
                item {
                    state.pevneKody.forEach {
                        Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    state.caskody.forEach {
                        Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    Text("POZOR! Ne všechny spoje kurzu musí dnes jet! Vždy si rozklikněte detail kurzu pro konkrétní informace o výpravnosti!",
                        Modifier.padding(vertical = 8.dp), fontSize = 20.sp,  color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                rowItem(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (state is KurzState.OK.Online && state.potvrzenaNizkopodlaznost != null) IconWithTooltip(
                        remember(state.potvrzenaNizkopodlaznost) {
                            when (state.potvrzenaNizkopodlaznost) {
                                true -> Icons.AutoMirrored.Filled.Accessible
                                false -> Icons.Default.NotAccessible
                            }
                        },
                        when (state.potvrzenaNizkopodlaznost) {
                            true -> "Potvrzený nízkopodlažní vůz"
                            false -> "Potvrzený vysokopodlažní vůz"
                        },
                        Modifier.padding(start = 8.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )

                    if (state is KurzState.OK.Online) Badge(
                        containerColor = barvaZpozdeniBublinyKontejner(state.zpozdeniMin),
                        contentColor = barvaZpozdeniBublinyText(state.zpozdeniMin),
                    ) {
                        Text(
                            text = state.zpozdeniMin.toDouble().minutes.run {
                                "${inWholeSeconds.toSign()}$inWholeMinutes min ${inWholeSeconds % 60} s"
                            },
                        )
                    }
                    if (state is KurzState.OK.Online && state.vuz != null) {
                        Text(
                            text = "ev. č. ${state.vuz.evC()}",
                            Modifier.padding(horizontal = 8.dp)
                        )
                    }
                }

                item {
                    HorizontalDivider(Modifier.fillMaxWidth())
                }

                state.navaznostiPredtim.forEach {
                    item {
                        TextButton(
                            onClick = {
                                navigate(KurzDestination(it))
                            }
                        ) {
                            Text("Potenciální návaznost na kurz $it")
                        }
                    }
                    item {
                        HorizontalDivider(Modifier.fillMaxWidth())
                    }
                }

                state.spoje.forEach { spoj ->
                    stickyHeader {
                        val jede = spoj.jede && state is KurzState.OK.Online
                        Surface {
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(text = "${spoj.cisloLinky}", fontSize = 20.sp, color = MaterialTheme.colorScheme.primary)
                                IconWithTooltip(
                                    remember(spoj.nizkopodlaznost) {
                                        when {
                                            jede && (state as KurzState.OK.Online).potvrzenaNizkopodlaznost == true -> Icons.AutoMirrored.Filled.Accessible
                                            jede && (state as KurzState.OK.Online).potvrzenaNizkopodlaznost == false -> Icons.Default.NotAccessible
                                            spoj.nizkopodlaznost -> Icons.AutoMirrored.Filled.Accessible
                                            else -> Icons.Default.NotAccessible
                                        }
                                    },
                                    when {
                                        jede && (state as KurzState.OK.Online).potvrzenaNizkopodlaznost == true -> "Potvrzený nízkopodlažní vůz"
                                        jede && (state as KurzState.OK.Online).potvrzenaNizkopodlaznost == false -> "Potvrzený vysokopodlažní vůz"
                                        spoj.nizkopodlaznost -> "Plánovaný nízkopodlažní vůz"
                                        else -> "Nezaručený nízkopodlažní vůz"
                                    },
                                    Modifier.padding(start = 8.dp),
                                    tint = when {
                                        jede && (state as KurzState.OK.Online).potvrzenaNizkopodlaznost == false && spoj.nizkopodlaznost -> MaterialTheme.colorScheme.error
                                        jede && (state as KurzState.OK.Online).potvrzenaNizkopodlaznost != null -> MaterialTheme.colorScheme.primary
                                        else -> MaterialTheme.colorScheme.onSurface
                                    }
                                )

                                Spacer(Modifier.weight(1F))

                                TextButton(
                                    onClick = {
                                        navigate(SpojDestination(spojId = spoj.spojId))
                                    }
                                ) {
                                    Text("Detail spoje")
                                }
                            }
                        }

                        if (jede) Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            state as KurzState.OK.Online
                            Badge(
                                containerColor = barvaZpozdeniBublinyKontejner(state.zpozdeniMin),
                                contentColor = barvaZpozdeniBublinyText(state.zpozdeniMin),
                            ) {
                                Text(
                                    text = state.zpozdeniMin.toDouble().minutes.run {
                                        "${inWholeSeconds.toSign()}$inWholeMinutes min ${inWholeSeconds % 60} s"
                                    },
                                )
                            }
                            if (state.vuz != null) {
                                Text(
                                    text = "ev. č. ${state.vuz.evC()}",
                                    Modifier.padding(horizontal = 8.dp)
                                )
                            }
                        }
                    }
                    item {
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
                    item {
                        HorizontalDivider(Modifier.fillMaxWidth().padding(top = 16.dp))
                    }
                }

                stickyHeader {  }

                state.navaznostiPotom.forEach {
                    item {
                        TextButton(
                            onClick = {
                                navigate(KurzDestination(it))
                            }
                        ) {
                            Text("Potenciální návaznost na kurz $it")
                        }
                    }
                    item {
                        HorizontalDivider(Modifier.fillMaxWidth())
                    }
                }
            }
        }
    }
}