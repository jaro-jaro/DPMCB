package cz.jaro.dpmcb.ui.oblibene

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Badge
import androidx.compose.material3.CircularProgressIndicator
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
import cz.jaro.datum_cas.Datum
import cz.jaro.datum_cas.dni
import cz.jaro.datum_cas.min
import cz.jaro.dpmcb.R
import cz.jaro.dpmcb.SuplikAkce
import cz.jaro.dpmcb.data.App.Companion.title
import cz.jaro.dpmcb.data.App.Companion.vybrano
import cz.jaro.dpmcb.data.helperclasses.NavigateFunction
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.barvaZpozdeniTextu
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.toSign
import cz.jaro.dpmcb.ui.destinations.DetailSpojeDestination
import org.koin.androidx.compose.koinViewModel
import java.util.Calendar.DAY_OF_WEEK
import java.util.Calendar.FRIDAY
import java.util.Calendar.MONDAY
import java.util.Calendar.SATURDAY
import java.util.Calendar.SUNDAY
import java.util.Calendar.THURSDAY
import java.util.Calendar.TUESDAY
import java.util.Calendar.WEDNESDAY

@Destination
@RootNavGraph(start = true)
@Composable
fun Oblibene(
    navigator: DestinationsNavigator,
    viewModel: OblibeneViewModel = koinViewModel(),
) {
    title = R.string.app_name
    vybrano = SuplikAkce.Oblibene

    val oblibene by viewModel.state.collectAsStateWithLifecycle()

    OblibeneScreen(
        oblibene = oblibene,
        navigate = navigator::navigate
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OblibeneScreen(
    oblibene: OblibeneViewModel.OblibeneState,
    navigate: NavigateFunction,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize()
    ) {
        if (oblibene.nacitaSe) item {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(all = 16.dp),
                horizontalArrangement = Arrangement.Center,
            ) {
                CircularProgressIndicator()
            }
        }
        else if (!oblibene.nejake) item {
            Text(
                text = "Zatím nemáte žádná oblíbená spojení. Přidejte si je kliknutím na ikonu hvězdičky v detailu spoje",
                modifier = Modifier.padding(all = 16.dp),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center
            )
        }
        else if (oblibene.dnes.isEmpty()) item {
            Text(
                text = "Dnes nejede žádný z vašich oblíbených spojů",
                modifier = Modifier.padding(all = 16.dp),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center
            )
        }
        else item {
            Text(
                text = "Jede dnes",
                modifier = Modifier.padding(all = 16.dp),
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center
            )
        }

        items(oblibene.dnes) {

            OutlinedCard(
                onClick = {
                    navigate(DetailSpojeDestination(it.spojId))
                },
                Modifier
                    .fillMaxWidth()
                    .padding(all = 8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 8.dp, top = 8.dp, end = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "${it.linka}")
                    if (it.zpozdeni != null) Badge(
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
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 8.dp, end = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = it.vychoziZastavka)
                    Text(text = it.vychoziZastavkaCas.toString())
                }
                if (it.aktualniZastavka != null && it.aktualniZastavkaCas != null && it.zpozdeni != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 8.dp, end = 8.dp),
                    ) {
                        Text(text = it.aktualniZastavka)
                        Spacer(modifier = Modifier.weight(1F))
                        Text(
                            text = "${it.aktualniZastavkaCas + it.zpozdeni.min}",
                            color = barvaZpozdeniTextu(it.zpozdeni),
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 8.dp, bottom = 8.dp, end = 8.dp),
                ) {
                    Text(text = it.cilovaZastavka)
                    Spacer(modifier = Modifier.weight(1F))
                    if (it.zpozdeni != null) Text(
                        text = "${it.cilovaZastavkaCas + it.zpozdeni.min}",
                        color = barvaZpozdeniTextu(it.zpozdeni),
                        modifier = Modifier.padding(start = 8.dp)
                    ) else Text(text = "${it.cilovaZastavkaCas}")
                }
            }
        }

        if (oblibene.jindy.isNotEmpty()) item {
            Text(
                text = "Jede jindy",
                modifier = Modifier.padding(all = 16.dp),
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center
            )
        }

        items(oblibene.jindy, key = { it.spojId }) {

            OutlinedCard(
                onClick = {
                    navigate(DetailSpojeDestination(it.spojId))
                },
                Modifier
                    .fillMaxWidth()
                    .padding(all = 8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 8.dp, top = 8.dp, end = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "${it.linka}")
                    if (it.zpozdeni != null) Badge(
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
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 8.dp, end = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = it.vychoziZastavka)
                    Text(text = it.vychoziZastavkaCas.toString())
                }
                if (it.aktualniZastavka != null && it.aktualniZastavkaCas != null && it.zpozdeni != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 8.dp, end = 8.dp),
                    ) {
                        Text(text = it.aktualniZastavka)
                        Spacer(modifier = Modifier.weight(1F))
                        Text(
                            text = "${it.aktualniZastavkaCas + it.zpozdeni.min}",
                            color = barvaZpozdeniTextu(it.zpozdeni),
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 8.dp, bottom = 8.dp, end = 8.dp),
                ) {
                    Text(text = it.cilovaZastavka)
                    Spacer(modifier = Modifier.weight(1F))
                    if (it.zpozdeni != null) Text(
                        text = "${it.cilovaZastavkaCas + it.zpozdeni.min}",
                        color = barvaZpozdeniTextu(it.zpozdeni),
                        modifier = Modifier.padding(start = 8.dp)
                    ) else Text(text = "${it.cilovaZastavkaCas}")
                }
                if (it.dalsiPojede != null) {
                    Text(
                        text = "Další pojede ${it.dalsiPojede.hezky()}", Modifier
                            .fillMaxWidth()
                            .padding(start = 8.dp, bottom = 8.dp, end = 8.dp)
                    )
                }
            }
        }
    }
}

private fun Datum.hezky() = (this - Datum.dnes).let { za ->
    when {
        za == 1.dni -> "zítra"
        za == 2.dni -> "pozítří"
        za < 7.dni -> when (toCalendar()[DAY_OF_WEEK]) {
            MONDAY -> "v pondělí"
            TUESDAY -> "v úterý"
            WEDNESDAY -> "ve středu"
            THURSDAY -> "ve čtvrtek"
            FRIDAY -> "v pátek"
            SATURDAY -> "v sobotu"
            SUNDAY -> "v neděli"
            else -> throw IllegalArgumentException("WTF")
        }

        else -> toString()
    }
}