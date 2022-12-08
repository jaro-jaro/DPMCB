package cz.jaro.dpmcb.ui.spojeni

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import cz.jaro.dpmcb.R
import cz.jaro.dpmcb.data.App
import cz.jaro.dpmcb.ui.destinations.DetailSpojeScreenDestination
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.ParametersHolder

@Destination
@Composable
fun VysledkySpojeniScreen(
    nastaveniVyhledavani: NastaveniVyhledavani,
    viewModel: VysledkySpojeniViewModel = koinViewModel {
        ParametersHolder(mutableListOf(nastaveniVyhledavani))
    },
    navigator: DestinationsNavigator,
) {
    App.title = R.string.vysledky_vyhledavani

    LinearProgressIndicator(Modifier.fillMaxWidth())

    val vysledky by viewModel.vysledky.collectAsState(emptyList())

    Column(
        modifier = Modifier
            .padding(all = 16.dp)
            .fillMaxWidth()
    ) {
        Text(text = "${nastaveniVyhledavani.start} -> ${nastaveniVyhledavani.cil}")
        Text(text = "Čas: ${nastaveniVyhledavani.cas}")

        LazyColumn(
            modifier = Modifier
                .weight(1F)
                .fillMaxWidth()
        ) {
            if (nastaveniVyhledavani.jenNizkopodlazni) item { Text(text = "Zobrazují se pouze nízkopodlažní spojení") }
            if (nastaveniVyhledavani.jenPrima) item { Text(text = "Zobrazují se pouze přímá spojení") }

            items(vysledky) { spojeni ->
                OutlinedCard(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    spojeni.forEach {
                        Column(
                            Modifier
                                .clickable {
                                    navigator.navigate(DetailSpojeScreenDestination(spojId = it.spoj.id))
                                }
                                .padding(all = 8.dp)
                        ) {
                            Text(text = "${it.spoj.cisloLinky}")
                            Text(text = "${it.minulaZastavka} (${it.odjezd}) -> ${it.pristiZastavka} (${it.prijezd})")
                        }
                    }
                }
            }
        }
    }
}
