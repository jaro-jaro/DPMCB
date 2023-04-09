package cz.jaro.dpmcb.ui.spoj

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Badge
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconToggleButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import cz.jaro.datum_cas.min
import cz.jaro.dpmcb.BuildConfig
import cz.jaro.dpmcb.R
import cz.jaro.dpmcb.data.App
import cz.jaro.dpmcb.data.App.Companion.repo
import cz.jaro.dpmcb.data.App.Companion.title
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.IconWithTooltip
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.Offset
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.barvaZpozdeniBublinyKontejner
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.barvaZpozdeniBublinyText
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.barvaZpozdeniTextu
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.toSign
import cz.jaro.dpmcb.ui.UiEvent
import cz.jaro.dpmcb.ui.destinations.OdjezdyScreenDestination
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.ParametersHolder

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Destination
@Composable
fun DetailSpojeScreen(
    spojId: String,
    viewModel: DetailSpojeViewModel = koinViewModel {
        ParametersHolder(mutableListOf(spojId))
    },
    navigator: DestinationsNavigator,
) {

    title = R.string.detail_spoje
    App.vybrano = null

    val state by viewModel.state.collectAsStateWithLifecycle()

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
                IconWithTooltip(
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
                val oblibene by repo.oblibene.collectAsStateWithLifecycle()
                FilledIconToggleButton(checked = spojId in oblibene, onCheckedChange = {
                    if (it) {
                        repo.pridatOblibeny(spojId)
                    } else {
                        repo.odebratOblibeny(spojId)
                    }
                }) {
                    IconWithTooltip(Icons.Default.Star, "Oblíbené")
                }
//                Button(onClick = {
//                    viewModel.detailKurzu()
//                }) {
//                    Text("Detail kurzu")
//                }
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
                                    text = it.nazev,
                                    modifier = Modifier
                                        .clickable {
                                            navigator.navigate(
                                                OdjezdyScreenDestination(
                                                    cas = it.cas,
                                                    zastavka = it.nazev,
                                                )
                                            )
                                        }
                                        .height(24.dp)
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
                                                    cas = it.cas,
                                                    zastavka = it.nazev,
                                                )
                                            )
                                        }
                                        .height(24.dp)
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
                                                        cas = zastavka.cas,
                                                        zastavka = zastavka.nazev,
                                                    )
                                                )
                                            }
                                            .height(24.dp)
                                    )
                                }
                        }
                        val neNaMape = state.zpozdeni != null && state.zastavkyNaJihu != null

                        val projetaBarva = if (neNaMape) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        val barvaBusu = if (neNaMape) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurface
                        val barvaPozadi = MaterialTheme.colorScheme.surface
                        val baravCary = MaterialTheme.colorScheme.surfaceVariant
                        val zastavek = state.zastavky.count()

                        val vyska by viewModel.vyska.collectAsStateWithLifecycle(0F)
                        val projetychUseku by viewModel.projetychUseku.collectAsStateWithLifecycle(0)
                        val animovanaVyska by animateFloatAsState(vyska, label = "HeightAnimation")

                        Canvas(
                            modifier = Modifier
                                .fillMaxHeight()
                                .weight(1F)
                                .padding(start = 8.dp),
                            contentDescription = "Poloha spoje"
                        ) {
                            val canvasHeight = size.height
                            val lineWidth = 3.dp.toPx()
                            val lineXOffset = 7.dp.toPx()
                            val rowHeight = canvasHeight / zastavek
                            val circleRadius = 5.5.dp.toPx()
                            val circleStrokeWidth = 3.dp.toPx()

                            translate(left = lineXOffset, top = rowHeight * .5F) {
                                drawLine(
                                    color = baravCary,
                                    start = Offset(),
                                    end = Offset(y = canvasHeight - rowHeight),
                                    strokeWidth = lineWidth,
                                )

                                repeat(zastavek) { i ->
                                    translate(top = i * rowHeight) {
                                        val projel = projetychUseku >= i

                                        drawCircle(
                                            color = if (projel) projetaBarva else barvaPozadi,
                                            radius = circleRadius,
                                            center = Offset(),
                                            style = Fill
                                        )
                                        drawCircle(
                                            color = if (projel) projetaBarva else baravCary,
                                            radius = circleRadius,
                                            center = Offset(),
                                            style = Stroke(
                                                width = circleStrokeWidth
                                            )
                                        )
                                    }
                                }

                                drawLine(
                                    color = projetaBarva,
                                    start = Offset(),
                                    end = Offset(y = rowHeight * animovanaVyska),
                                    strokeWidth = lineWidth,
                                )

                                if (vyska > 0F) drawCircle(
                                    color = barvaBusu,
                                    radius = circleRadius - circleStrokeWidth * .5F,
                                    center = Offset(y = rowHeight * animovanaVyska)
                                )
                            }
                        }
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    var zobrazitMenu by remember { mutableStateOf(false) }
                    val context = LocalContext.current

                    TextButton(onClick = {
                        zobrazitMenu = true
                    }) {
                        Text("id: $spojId")
                        DropdownMenu(
                            expanded = zobrazitMenu,
                            onDismissRequest = {
                                zobrazitMenu = false
                            }
                        ) {
                            DropdownMenuItem(
                                text = {
                                    Text("Zobrazit v mapě")
                                },
                                onClick = {},
                                enabled = false
                            )
                            DropdownMenuItem(
                                text = {
                                    Text("ID: $spojId")
                                },
                                onClick = {
                                    context.startActivity(Intent.createChooser(Intent().apply {
                                        action = Intent.ACTION_SEND
                                        putExtra(Intent.EXTRA_TEXT, spojId)
                                        type = "text/plain"
                                    }, "Sdílet ID spoje"))
                                    zobrazitMenu = false
                                },
                                trailingIcon = {
                                    IconWithTooltip(Icons.Default.Share, null)
                                }
                            )
                            if (BuildConfig.DEBUG) DropdownMenuItem(
                                text = {
                                    Text("Detail spoje v api na jihu")
                                },
                                onClick = {
                                    context.startActivity(Intent.createChooser(Intent().apply {
                                        action = Intent.ACTION_VIEW
                                        data = Uri.parse("https://dopravanajihu.cz/idspublicservices/api/servicedetail?id=$spojId")
                                    }, "Sdílet ID spoje"))
                                    zobrazitMenu = false
                                },
                                trailingIcon = {
                                    IconWithTooltip(Icons.Default.Public, null)
                                }
                            )
                            DropdownMenuItem(
                                text = {
                                    Text("Název: ${state.nazevSpoje}")
                                },
                                onClick = {
                                    context.startActivity(Intent.createChooser(Intent().apply {
                                        action = Intent.ACTION_SEND
                                        putExtra(Intent.EXTRA_TEXT, state.nazevSpoje)
                                        type = "text/plain"
                                    }, "Sdílet název spoje"))
                                    zobrazitMenu = false
                                },
                                trailingIcon = {
                                    IconWithTooltip(Icons.Default.Share, null)
                                }
                            )
                            DropdownMenuItem(
                                text = {
                                    Text("Sdílet deeplink")
                                },
                                onClick = {
                                    context.startActivity(Intent.createChooser(Intent().apply {
                                        action = Intent.ACTION_SEND
                                        putExtra(Intent.EXTRA_TEXT, state.deeplink)
                                        type = "text/uri-list"
                                    }, "Sdílet deeplink"))
                                    zobrazitMenu = false
                                },
                                trailingIcon = {
                                    IconWithTooltip(Icons.Default.Share, null)
                                }
                            )
                        }
                    }

                }
                Column {
                    state.pevneKody.forEach {
                        Text(it)
                    }
                    state.caskody.forEach {
                        Text(it)
                    }
                }
            }
        }
    }
}
