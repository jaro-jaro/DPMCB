package cz.jaro.dpmcb.ui.zjr

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import cz.jaro.dpmcb.data.App.Companion.repo
import cz.jaro.dpmcb.data.entities.ZastavkaSpoje
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.VDP
import cz.jaro.dpmcb.ui.destinations.DetailSpojeScreenDestination
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.ParametersHolder

@Destination
@Composable
fun JizdniRadyScreen(
    cisloLinky: Int,
    zastavka: String,
    pristiZastavka: String,
    viewModel: JizdniRadyViewModel = koinViewModel {
        ParametersHolder(mutableListOf(cisloLinky, zastavka, pristiZastavka))
    },
    navigator: DestinationsNavigator,
) {
//    val viewModel = remember { JizdniRadyViewModel(cisloLinky, zastavka, pristiZastavka, repo) }

    val typDne by repo.typDne.collectAsState(VDP.DNY)

    LaunchedEffect(typDne) {
        viewModel.nacistJR(typDne)
    }

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
                color = Color.White,
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "$zastavka -> $pristiZastavka",
                fontSize = 20.sp,
                color = Color.White,
            )
        }
        Spacer(modifier = Modifier.height(8.dp))

        if (viewModel.state.nacitaSe) Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.TopCenter
        ) {
            CircularProgressIndicator()
        }

        if (!viewModel.state.nacitaSe) Row(
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
                            .height(32.dp)
                            .padding(4.dp),
                        text = h.toString(),
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = Color.White
                    )

                }
            }
            Column(
                modifier = Modifier
                    .horizontalScroll(state = rememberScrollState())
            ) {
                repeat(24) { h ->
                    RadekOdjezdu(navigator, viewModel.dataProHodinu(h))
                }
            }
        }
    }
}


@Composable
fun RadekOdjezdu(
    navigator: DestinationsNavigator,
    vysledek: List<Pair<ZastavkaSpoje, Long>>
) {

    Row(
        modifier = Modifier
            .height(32.dp)
    ) {
        vysledek.forEach { (zastavka, id) ->
            Text(
                text = zastavka.cas.min.let { if ("$it".length <= 1) "0$it" else "$it" },
                modifier = Modifier
                    .clickable {
                        navigator.navigate(DetailSpojeScreenDestination(spojId = id))
                    }
                    //.width(32.dp)
                    .padding(4.dp),
                color = Color.White,
                fontSize = 20.sp
            )
        }

    }

}
