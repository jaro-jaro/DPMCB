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
import cz.jaro.datum_cas.min
import cz.jaro.dpmcb.R
import cz.jaro.dpmcb.SuplikAkce
import cz.jaro.dpmcb.data.App.Companion.repo
import cz.jaro.dpmcb.data.App.Companion.title
import cz.jaro.dpmcb.data.App.Companion.vybrano
import cz.jaro.dpmcb.data.helperclasses.NavigateFunction
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.asString
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.barvaZpozdeniTextu
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.toSign
import cz.jaro.dpmcb.ui.destinations.DetailSpojeDestination
import org.koin.androidx.compose.koinViewModel
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit

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

    val datum by repo.datum.collectAsStateWithLifecycle()

    OblibeneScreen(
        oblibene = oblibene,
        navigate = navigator::navigate,
        vybranyDatum = datum.toLocalDate(),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OblibeneScreen(
    oblibene: OblibeneState,
    navigate: NavigateFunction,
    vybranyDatum: LocalDate,
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
                text = "Zatím nemáte žádné oblíbené spoje. Přidejte si je kliknutím na ikonu hvězdičky v detailu spoje",
                modifier = Modifier.padding(all = 16.dp),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center
            )
        }
        else if (oblibene.dnes.isEmpty()) item {
            Text(
                text = "${vybranyDatum.hezky().replaceFirstChar { it.titlecase() }} nejede žádný z vašich oblíbených spojů",
                modifier = Modifier.padding(all = 16.dp),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center
            )
        }
        else item {
            Text(
                text = "Jede ${vybranyDatum.hezky()}",
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
                        text = "Další pojede ${if (vybranyDatum != LocalDate.now()) it.dalsiPojede.asString() else it.dalsiPojede.hezky()}", Modifier
                            .fillMaxWidth()
                            .padding(start = 8.dp, bottom = 8.dp, end = 8.dp)
                    )
                }
            }
        }
    }
}

private fun LocalDate.hezky() = Datum.dnes.toLocalDate().until(this, ChronoUnit.DAYS).let { za ->
    when (za) {
        0L -> "dnes"
        1L -> "zítra"
        2L -> "pozítří"
        in 3L..7L -> when (dayOfWeek!!) {
            DayOfWeek.MONDAY -> "v pondělí"
            DayOfWeek.TUESDAY -> "v úterý"
            DayOfWeek.WEDNESDAY -> "ve středu"
            DayOfWeek.THURSDAY -> "ve čtvrtek"
            DayOfWeek.FRIDAY -> "v pátek"
            DayOfWeek.SATURDAY -> "v sobotu"
            DayOfWeek.SUNDAY -> "v neděli"
        }

        else -> asString()
    }
}