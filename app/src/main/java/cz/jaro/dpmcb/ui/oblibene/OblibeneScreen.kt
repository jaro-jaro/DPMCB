package cz.jaro.dpmcb.ui.oblibene

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Badge
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootNavGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import cz.jaro.dpmcb.R
import cz.jaro.dpmcb.data.App.Companion.title
import cz.jaro.dpmcb.data.App.Companion.vybrano
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.asString
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.barvaZpozdeniTextu
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.hezky6p
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.navigateFunction
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.rowItem
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.toSign
import cz.jaro.dpmcb.ui.main.SuplikAkce
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf
import java.time.LocalDate

@Destination
@RootNavGraph(start = true)
@Composable
fun Oblibene(
    navigator: DestinationsNavigator,
    viewModel: OblibeneViewModel = koinViewModel {
        parametersOf(
            OblibeneViewModel.Parameters(
                navigate = navigator.navigateFunction
            )
        )
    },
) {
    title = R.string.app_name
    vybrano = SuplikAkce.Oblibene

    val state by viewModel.state.collectAsStateWithLifecycle()

    OblibeneScreen(
        state = state,
        onEvent = viewModel::onEvent
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OblibeneScreen(
    state: OblibeneState,
    onEvent: (OblibeneEvent) -> Unit,
) = LazyColumn(
    modifier = Modifier.fillMaxSize()
) {
    if (state == OblibeneState.NacitaSe) rowItem(
        Modifier
            .fillMaxWidth()
            .padding(all = 16.dp),
        horizontalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator()
    }

    if (state == OblibeneState.ZadneOblibene) item {
        Text(
            text = "Zatím nemáte žádné oblíbené spoje. Přidejte si je kliknutím na ikonu hvězdičky v detailu spoje",
            modifier = Modifier.padding(all = 16.dp),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.primary,
        )
    }

    if (state is OblibeneState.JedeJenJindy) item {
        Text(
            text = "${state.dnes.hezky6p().replaceFirstChar { it.titlecase() }} nejede žádný z vašich oblíbených spojů",
            modifier = Modifier.padding(all = 16.dp),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.primary,
        )
    }

    if (state is OblibeneState.DnesNecoJede) {
        item {
            Text(
                text = "Jede ${state.dnes.hezky6p()}",
                modifier = Modifier.padding(all = 16.dp),
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        items(state.dnesJede) {
            val content: @Composable ColumnScope.() -> Unit = {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 8.dp, top = 8.dp, end = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "${it.linka}")
                    if (it is KartickaState.Online) Badge(
                        containerColor = UtilFunctions.barvaZpozdeniBublinyKontejner(it.zpozdeni),
                        contentColor = UtilFunctions.barvaZpozdeniBublinyText(it.zpozdeni),
                        modifier = Modifier.padding(start = 8.dp)
                    ) {
                        Text(
                            text = it.zpozdeni.run {
                                "${toSign()}$this min"
                            },
                        )
                    }
                }
                @Composable
                fun AktualniZastavka() {
                    if (it is KartickaState.Online) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 8.dp, end = 8.dp),
                        ) {
                            Text(text = it.aktualniZastavka, color = MaterialTheme.colorScheme.secondary)
                            Spacer(modifier = Modifier.weight(1F))
                            Text(
                                text = "${it.aktualniZastavkaCas.plusMinutes(it.zpozdeni.toLong())}",
                                color = barvaZpozdeniTextu(it.zpozdeni),
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    }
                }

                if (it is KartickaState.Online && it.mistoAktualniZastavky == -1) AktualniZastavka()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 8.dp, end = 8.dp),
                ) {
                    Text(text = it.vychoziZastavka)
                    Spacer(modifier = Modifier.weight(1F))
                    if (it is KartickaState.Online && it.mistoAktualniZastavky == -1) Text(
                        text = "${it.vychoziZastavkaCas.plusMinutes(it.zpozdeni.toLong())}",
                        color = barvaZpozdeniTextu(it.zpozdeni),
                        modifier = Modifier.padding(start = 8.dp)
                    ) else Text(text = "${it.vychoziZastavkaCas}")
                }
                if (it is KartickaState.Online && it.mistoAktualniZastavky == 0) AktualniZastavka()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 8.dp, end = 8.dp),
                ) {
                    Text(text = it.cilovaZastavka)
                    Spacer(modifier = Modifier.weight(1F))
                    if (it is KartickaState.Online && it.mistoAktualniZastavky < 1) Text(
                        text = "${it.cilovaZastavkaCas.plusMinutes(it.zpozdeni.toLong())}",
                        color = barvaZpozdeniTextu(it.zpozdeni),
                        modifier = Modifier.padding(start = 8.dp)
                    ) else Text(text = "${it.cilovaZastavkaCas}")
                }
                if (it is KartickaState.Online && it.mistoAktualniZastavky == 1) AktualniZastavka()

                Spacer(Modifier.height(8.dp))
            }

            if (it is KartickaState.Online && it.mistoAktualniZastavky == 0) ElevatedCard(
                onClick = {
                    onEvent(OblibeneEvent.VybralSpojDnes(it.spojId))
                },
                Modifier
                    .fillMaxWidth()
                    .padding(all = 8.dp),
                content = content,
            )
            else OutlinedCard(
                onClick = {
                    onEvent(OblibeneEvent.VybralSpojDnes(it.spojId))
                },
                Modifier
                    .fillMaxWidth()
                    .padding(all = 8.dp),
                content = content,
            )
        }
    }

    if (state is OblibeneState.JindyNecoJede) {
        item {
            Text(
                text = "Jede jindy",
                modifier = Modifier.padding(all = 16.dp),
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        items(state.jindyJede, key = { it.spojId }) {

            OutlinedCard(
                onClick = {
                    onEvent(OblibeneEvent.VybralSpojJindy(it.spojId, it.dalsiPojede))
                },
                Modifier
                    .fillMaxWidth()
                    .padding(all = 8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 8.dp, top = 8.dp, end = 8.dp),
                ) {
                    Text(text = "${it.linka}")
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 8.dp, end = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = it.vychoziZastavka)
                    Text(text = it.vychoziZastavkaCas.toString())
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 8.dp, bottom = 8.dp, end = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = it.cilovaZastavka)
                    Text(text = "${it.cilovaZastavkaCas}")
                }
                if (it.dalsiPojede != null) {
                    Text(
                        text = "Další pojede ${if (state.dnes != LocalDate.now()) it.dalsiPojede.asString() else it.dalsiPojede.hezky6p()}", Modifier
                            .fillMaxWidth()
                            .padding(start = 8.dp, bottom = 8.dp, end = 8.dp)
                    )
                }
            }
        }
    }
}