package cz.jaro.dpmcb.ui.detail.spoje

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconToggleButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.unit.dp
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import cz.jaro.dpmcb.R
import cz.jaro.dpmcb.data.App
import cz.jaro.dpmcb.data.App.Companion.repo
import cz.jaro.dpmcb.data.helperclasses.Cas
import cz.jaro.dpmcb.data.helperclasses.Cas.Companion.toCas
import cz.jaro.dpmcb.data.helperclasses.Trvani.Companion.min
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.Offset
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.barvaZpozdeniBublinyKontejner
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.barvaZpozdeniBublinyText
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.barvaZpozdeniTextu
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.toSign
import cz.jaro.dpmcb.ui.UiEvent
import cz.jaro.dpmcb.ui.destinations.OdjezdyScreenDestination
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.ParametersHolder

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Destination
@Composable
fun DetailSpojeScreen(
    spojId: Long,
    viewModel: DetailSpojeViewModel = koinViewModel {
        ParametersHolder(mutableListOf(spojId))
    },
    navigator: DestinationsNavigator,
) {

    App.title = R.string.detail_spoje

    val state by viewModel.state.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                is UiEvent.Navigovat -> {
                    navigator.navigate(event.kam)
                }

                else -> {}
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp)
    ) {
        if (state.nacitaSe) CircularProgressIndicator(modifier = Modifier.fillMaxWidth())
        else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp), verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Linka ${state.cisloLinky}")
                Icon(
                    state.nizkopodlaznost, "Invalidní vozík", modifier = Modifier.padding(start = 8.dp)
                )
                if (state.zpozdeni != null) Badge(
                    containerColor = barvaZpozdeniBublinyKontejner(state.zpozdeni!!),
                    contentColor = barvaZpozdeniBublinyText(state.zpozdeni!!),
                ) {
                    Text(
                        text = state.zpozdeni!!.run {
                            "${toSign()}$this min"
                        },
                    )
                }
                Spacer(Modifier.weight(1F))
                val oblibene by repo.oblibene.collectAsState()
                FilledIconToggleButton(checked = spojId in oblibene, onCheckedChange = {
                    if (it) {
                        repo.pridatOblibeny(spojId)
                    } else {
                        repo.odebratOblibeny(spojId)
                    }
                }) {
                    Icon(Icons.Default.Star, "Oblíbené")
                }
                Button(onClick = {
                    viewModel.detailKurzu()
                }) {
                    Text("Detail kurzu")
                }
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(ScrollState(0))
            ) {
                OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(IntrinsicSize.Max)
                            .padding(12.dp)
                    ) {
                        Column {
                            state.zastavky.forEach {
                                Text(
                                    text = it.nazevZastavky,
                                    modifier = Modifier
                                        .clickable {
                                            navigator.navigate(
                                                OdjezdyScreenDestination(
                                                    cas = it.cas.toString(),
                                                    zastavka = it.nazevZastavky,
                                                )
                                            )
                                        }
                                )
                            }
                        }
                        Column(Modifier.padding(start = 8.dp)) {
                            state.zastavky.forEach {
                                Text(
                                    text = it.cas.toString(),
                                    modifier = Modifier
                                        .clickable {
                                            navigator.navigate(
                                                OdjezdyScreenDestination(
                                                    cas = it.cas.toString(),
                                                    zastavka = it.nazevZastavky,
                                                )
                                            )
                                        }
                                )
                            }
                        }
                        if (state.zpozdeni != null && state.zastavkyNaJihu != null) Column(Modifier.padding(start = 8.dp)) {
                            state.zastavky
                                .zip(state.zastavkyNaJihu!!)
                                .forEach { (zastavka, zastavkaNaJihu) ->
                                    Text(
                                        text = if (!zastavkaNaJihu.passed) (zastavka.cas + state.zpozdeni!!.min).toString() else "",
                                        color = barvaZpozdeniTextu(state.zpozdeni!!),
                                        modifier = Modifier
                                            .clickable {
                                                navigator.navigate(
                                                    OdjezdyScreenDestination(
                                                        cas = zastavka.cas.toString(),
                                                        zastavka = zastavka.nazevZastavky,
                                                    )
                                                )
                                            }
                                    )
                                }
                        }
                        val primary = MaterialTheme.colorScheme.primary
                        val tertiary = MaterialTheme.colorScheme.tertiary
                        val surface = MaterialTheme.colorScheme.surface
                        val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant
                        val zastavek = state.zastavky.count()
                        val ted by Cas.presneTed.collectAsState(Cas.nikdy)

                        val scope = rememberCoroutineScope()
                        val vyska = remember { Animatable(0F) }

                        Canvas(
                            modifier = Modifier
                                .fillMaxHeight()
                                .weight(1F)
                                .padding(start = 8.dp),
                            contentDescription = "Poloha spoje"
                        ) {
                            while (System.currentTimeMillis() % 1_000 > 5L) Unit
                            val canvasHeight = size.height
                            val lineWidth = 6.3.dp.toPx()
                            val lineXOffset = 7.dp.toPx()
                            val rowHeight = canvasHeight / zastavek
                            val circleRadius = 5.2.dp.toPx()
                            val circleStrokeWidth = 3.1.dp.toPx()

                            translate(left = lineXOffset, top = rowHeight * .5F) {
                                drawLine(
                                    color = surfaceVariant,
                                    start = Offset(),
                                    end = Offset(y = canvasHeight - rowHeight),
                                    strokeWidth = lineWidth,
                                )

                                if (state.zpozdeni != null && state.zastavkyNaJihu != null) state.zastavkyNaJihu!!.windowed(2)
                                    .forEachIndexed { i, (posledniZastavka, pristiZastavka) ->
                                        translate(top = i * rowHeight) {

                                            if (posledniZastavka.passed && pristiZastavka.passed) {
                                                drawLine(
                                                    color = primary,
                                                    start = Offset(),
                                                    end = Offset(y = rowHeight),
                                                    strokeWidth = lineWidth,
                                                )
                                            }

                                            if (posledniZastavka.passed && !pristiZastavka.passed) {
                                                val prijezd = pristiZastavka.arrivalTime.toCas() + state.zpozdeni!!.min
                                                val odjezd = posledniZastavka.departureTime.toCas() + state.zpozdeni!!.min
                                                val dobaJizdy = prijezd - odjezd
                                                val ubehlo = ted - odjezd
                                                scope.launch {
                                                    vyska.animateTo((ubehlo / dobaJizdy).toFloat().coerceAtMost(1F))
                                                }

                                                drawLine(
                                                    color = primary,
                                                    start = Offset(),
                                                    end = Offset(y = rowHeight * vyska.value),
                                                    strokeWidth = lineWidth,
                                                )
                                            }
                                        }
                                    }

                                repeat(zastavek) { i ->
                                    translate(top = i * rowHeight) {
                                        val projel = state.zastavkyNaJihu?.get(i)?.passed ?: false
                                        drawCircle(
                                            color = if (projel) primary else surface,
                                            radius = circleRadius,
                                            center = Offset(),
                                            style = Fill
                                        )
                                        drawCircle(
                                            color = if (projel) primary else surfaceVariant,
                                            radius = circleRadius,
                                            center = Offset(),
                                            style = Stroke(
                                                width = circleStrokeWidth
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                if (state.idNaJihu != null) Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Text("id: ${state.idNaJihu!!}")
                }
            }
        }
    }
}
