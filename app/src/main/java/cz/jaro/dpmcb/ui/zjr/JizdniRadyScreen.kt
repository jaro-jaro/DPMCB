package cz.jaro.dpmcb.ui.zjr

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import cz.jaro.dpmcb.R
import cz.jaro.dpmcb.data.App.Companion.title
import cz.jaro.dpmcb.data.App.Companion.vybrano
import cz.jaro.dpmcb.data.helperclasses.NavigateFunction
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.navigateFunction
import cz.jaro.dpmcb.data.realtions.OdjezdNizkopodlaznostSpojId
import cz.jaro.dpmcb.ui.destinations.SpojDestination
import cz.jaro.dpmcb.ui.main.SuplikAkce
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@Destination
@Composable
fun JizdniRady(
    cisloLinky: Int,
    zastavka: String,
    pristiZastavka: String,
    viewModel: JizdniRadyViewModel = koinViewModel {
        parametersOf(
            JizdniRadyViewModel.Parameters(
                cisloLinky = cisloLinky,
                zastavka = zastavka,
                pristiZastavka = pristiZastavka
            )
        )
    },
    navigator: DestinationsNavigator,
) {
    title = R.string.jizdni_rady
    vybrano = SuplikAkce.JizdniRady

    val state by viewModel.state.collectAsStateWithLifecycle()
    val zobrazitNizkopodlaznostZMinule by viewModel.zobrazitNizkopodlaznostZMinule.collectAsStateWithLifecycle()
    val nastaveni by viewModel.nstaveni.collectAsStateWithLifecycle()

    JizdniRadyScreen(
        cisloLinky = cisloLinky,
        zastavka = zastavka,
        pristiZastavka = pristiZastavka,
        state = state,
        navigate = navigator.navigateFunction,
        zobrazitNizkopodlaznostZMinule = zobrazitNizkopodlaznostZMinule,
        zachovatNizkopodlaznosti = nastaveni.zachovavatNizkopodlaznost,
        upravitZobrazeniNizkopodlaznosti = viewModel.upravitZobrazeniNizkopodlaznosti,
    )
}

@Composable
fun JizdniRadyScreen(
    cisloLinky: Int,
    zastavka: String,
    pristiZastavka: String,
    state: JizdniRadyState,
    navigate: NavigateFunction,
    zobrazitNizkopodlaznostZMinule: Boolean,
    zachovatNizkopodlaznosti: Boolean,
    upravitZobrazeniNizkopodlaznosti: (Boolean) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),

        ) {
        Row(
            modifier = Modifier,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = cisloLinky.toString(),
                fontSize = 30.sp,
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "$zastavka -> $pristiZastavka",
                fontSize = 20.sp,
            )
        }

        var zobrazitNizkopodlaznosti by remember {
            mutableStateOf(if (zachovatNizkopodlaznosti) zobrazitNizkopodlaznostZMinule else false)
        }

        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(checked = zobrazitNizkopodlaznosti, onCheckedChange = {
                zobrazitNizkopodlaznosti = it
                upravitZobrazeniNizkopodlaznosti(zobrazitNizkopodlaznosti)
            })
            Text("Zobrazit nízkopodlažnost", Modifier.clickable {
                zobrazitNizkopodlaznosti = !zobrazitNizkopodlaznosti
                upravitZobrazeniNizkopodlaznosti(zobrazitNizkopodlaznosti)
            })
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (state == JizdniRadyState.Loading) Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.TopCenter
        ) {
            CircularProgressIndicator()
        }

        if (state is JizdniRadyState.Success) Row(
            modifier = Modifier
                .verticalScroll(state = rememberScrollState()),
        ) {
            Column(
                modifier = Modifier
                //.width(32.dp)
            ) {
                repeat(24) { h ->
                    Text(
                        modifier = Modifier
                            .defaultMinSize(minHeight = 32.dp)
                            .padding(4.dp),
                        text = h.toString(),
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                    )

                }
            }
            Column(
                modifier = Modifier
                    .horizontalScroll(state = rememberScrollState())
                    .fillMaxWidth()
            ) {
                repeat(24) { h ->
                    RadekOdjezdu(
                        navigate = navigate,
                        vysledek = state.data.filter { it.odjezd.hour == h },
                        zobrazitNizkopodlaznost = zobrazitNizkopodlaznosti
                    )
                }
            }
        }
    }
}


@Composable
fun RadekOdjezdu(
    navigate: NavigateFunction,
    vysledek: List<OdjezdNizkopodlaznostSpojId>,
    zobrazitNizkopodlaznost: Boolean,
) {

    Row(
        modifier = Modifier
            .defaultMinSize(minHeight = 32.dp),
    ) {
        if (vysledek.isEmpty())
            Text(
                text = "",
                modifier = Modifier
                    .padding(4.dp),
                color = Color.Transparent,
                fontSize = 20.sp,
                fontWeight = FontWeight.Normal
            )
        vysledek.sortedBy { it.odjezd }.forEach { (odjezd, nizkopodlaznost, spojId) ->
            Text(
                text = odjezd.minute.let { if ("$it".length <= 1) "0$it" else "$it" },
                modifier = Modifier
                    .clickable {
                        navigate(SpojDestination(spojId = spojId))
                    }
                    //.width(32.dp)
                    .padding(4.dp),
                color = if (zobrazitNizkopodlaznost && nizkopodlaznost) MaterialTheme.colorScheme.primary else Color.Unspecified,
                fontSize = 20.sp,
                fontWeight = if (zobrazitNizkopodlaznost && nizkopodlaznost) FontWeight.Bold else FontWeight.Normal
            )
        }
    }
}
