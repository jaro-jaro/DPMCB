package cz.jaro.dpmcb.ui.spoj

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconToggleButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import cz.jaro.dpmcb.BuildConfig
import cz.jaro.dpmcb.R
import cz.jaro.dpmcb.data.App
import cz.jaro.dpmcb.data.App.Companion.title
import cz.jaro.dpmcb.data.helperclasses.NavigateFunction
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.IconWithTooltip
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.Offset
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.barvaZpozdeniBublinyKontejner
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.barvaZpozdeniBublinyText
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.barvaZpozdeniTextu
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.hezky6p
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.hezky7p
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.navigateFunction
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.toSign
import cz.jaro.dpmcb.ui.destinations.JizdniRadyDestination
import cz.jaro.dpmcb.ui.destinations.OdjezdyDestination
import cz.jaro.dpmcb.ui.main.SuplikAkce
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.ParametersHolder
import java.time.LocalDate
import java.time.LocalTime

@Destination
@Composable
fun Spoj(
    spojId: String,
    viewModel: SpojViewModel = koinViewModel {
        ParametersHolder(mutableListOf(spojId))
    },
    navigator: DestinationsNavigator,
) {
    title = R.string.detail_spoje
    App.vybrano = SuplikAkce.SpojPodleId

    val state by viewModel.state.collectAsStateWithLifecycle()

    SpojScreen(
        state = state,
        navigate = navigator.navigateFunction,
        toggleOblibeny = viewModel::toggleOblibeny,
        zmenitdatum = viewModel::zmenitdatum,
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun SpojScreen(
    state: SpojState,
    navigate: NavigateFunction,
    toggleOblibeny: () -> Unit,
    zmenitdatum: (LocalDate) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp)
    ) {
        when (state) {
            is SpojState.Loading -> Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator()
            }

            is SpojState.Neexistuje -> Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                Text("Tento spoj (ID ${state.spojId}) bohužel neexistuje :(\nZkontrolujte, zda jste zadali správně ID.")
            }

            is SpojState.Nejede -> {
                when {
                    state.pristeJedePoDatu == null && state.pristeJedePoDnesku == null -> {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text("Tento spoj (ID ${state.spojId}) bohužel ${state.datum.hezky6p()} nejede :(\nZkontrolujte, zda jste zadali správné datum.")
                        }
                    }

                    state.pristeJedePoDatu == null && state.pristeJedePoDnesku != null -> {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text("Tento spoj (ID ${state.spojId}) ${state.datum.hezky6p()} nejede, ale pojede ${state.pristeJedePoDnesku.hezky6p()}.")
                        }
                        Button(
                            onClick = {
                                zmenitdatum(state.pristeJedePoDnesku)
                            },
                            Modifier.padding(top = 16.dp)
                        ) {
                            Text("Změnit datum na ${state.pristeJedePoDnesku.hezky7p()}")
                        }
                    }

                    state.pristeJedePoDatu != null && (state.pristeJedePoDnesku == null || state.pristeJedePoDatu == state.pristeJedePoDnesku) -> {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text("Tento spoj (ID ${state.spojId}) ${state.datum.hezky6p()} nejede, ale pojede ${state.pristeJedePoDatu.hezky6p()}.")
                        }
                        Button(
                            onClick = {
                                zmenitdatum(state.pristeJedePoDatu)
                            },
                            Modifier.padding(top = 16.dp)
                        ) {
                            Text("Změnit datum na ${state.pristeJedePoDatu.hezky7p()}")
                        }
                    }

                    state.pristeJedePoDatu != null && state.pristeJedePoDnesku != null -> {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text("Tento spoj (ID ${state.spojId}) ${state.datum.hezky6p()} nejede. Jede mimo jiné ${state.pristeJedePoDatu.hezky6p()} nebo ${state.pristeJedePoDnesku.hezky6p()}")
                        }
                        Button(
                            onClick = {
                                zmenitdatum(state.pristeJedePoDatu)
                            },
                            Modifier.padding(top = 16.dp)
                        ) {
                            Text("Změnit datum na ${state.pristeJedePoDatu.hezky7p()}")
                        }
                        Button(
                            onClick = {
                                zmenitdatum(state.pristeJedePoDnesku)
                            },
                            Modifier.padding(top = 16.dp)
                        ) {
                            Text("Změnit datum na ${state.pristeJedePoDnesku.hezky7p()}")
                        }
                    }
                }
            }

            is SpojState.OK -> {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp), verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Linka ${state.cisloLinky}")
                    IconWithTooltip(
                        state.nizkopodlaznost.first, state.nizkopodlaznost.second, modifier = Modifier.padding(start = 8.dp)
                    )
                    if (state is SpojState.OK.Online) Badge(
                        containerColor = barvaZpozdeniBublinyKontejner(state.zpozdeni),
                        contentColor = barvaZpozdeniBublinyText(state.zpozdeni),
                    ) {
                        Text(
                            text = state.zpozdeni.run {
                                "${toSign()}$this min"
                            },
                        )
                    }
                    Spacer(Modifier.weight(1F))
                    FilledIconToggleButton(checked = state.jeOblibeny, onCheckedChange = {
                        toggleOblibeny()
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
                        .verticalScroll(rememberScrollState())
                ) {
                    if (state.vyluka) Card(
                        Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Row(
                            Modifier.padding(top = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.WarningAmber, null, Modifier.padding(horizontal = 8.dp))
                            Text(text = "Výluka", Modifier.padding(horizontal = 8.dp), style = MaterialTheme.typography.headlineSmall)
                        }
                        Text(text = "Tento spoj jede podle výlukového jízdního řádu!", Modifier.padding(all = 8.dp))
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
                                Modifier.weight(1F, false)
                            ) {
                                state.zastavky.forEach {
                                    MujText(
                                        text = it.nazev,
                                        navigate = navigate,
                                        cas = it.cas,
                                        zastavka = it.nazev,
                                        pristiZastavka = it.pristiZastavka,
                                        linka = it.linka,
                                    )
                                }
                            }
                            Column(Modifier.padding(start = 8.dp)) {
                                state.zastavky.forEach {
                                    MujText(
                                        text = it.cas.toString(),
                                        navigate = navigate,
                                        cas = it.cas,
                                        zastavka = it.nazev,
                                        pristiZastavka = it.pristiZastavka,
                                        linka = it.linka,
                                    )
                                }
                            }
                            if (state is SpojState.OK.Online) Column(Modifier.padding(start = 8.dp)) {
                                state.zastavky
                                    .zip(state.zastavkyNaJihu)
                                    .forEach { (zastavka, zastavkaNaJihu) ->
                                        MujText(
                                            text = if (!zastavkaNaJihu.passed) (zastavka.cas.plusMinutes(state.zpozdeni.toLong())).toString() else "",
                                            color = barvaZpozdeniTextu(state.zpozdeni),
                                            navigate = navigate,
                                            cas = zastavka.cas,
                                            zastavka = zastavka.nazev,
                                            pristiZastavka = zastavka.pristiZastavka,
                                            linka = zastavka.linka,
                                        )
                                    }
                            }

                            val projetaBarva = if (state is SpojState.OK.Online) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            val barvaBusu = if (state is SpojState.OK.Online) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurface
                            val barvaPozadi = MaterialTheme.colorScheme.surface
                            val baravCary = MaterialTheme.colorScheme.surfaceVariant
                            val zastavek = state.zastavky.count()

                            val animovanaVyska by animateFloatAsState(state.vyska, label = "HeightAnimation")

                            Canvas(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .width(14.dp)
                                    .padding(horizontal = 8.dp),
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
                                            val projel = state.projetychUseku >= i

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

                                    if (state.vyska > 0F) drawCircle(
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
                            Text("ID: ${state.spojId}")
                            DropdownMenu(
                                expanded = zobrazitMenu,
                                onDismissRequest = {
                                    zobrazitMenu = false
                                }
                            ) {
                                val clipboardManager = LocalClipboardManager.current
                                DropdownMenuItem(
                                    text = {
                                        Text("Zobrazit v mapě")
                                    },
                                    onClick = {},
                                    enabled = false
                                )
                                DropdownMenuItem(
                                    text = {
                                        Text("ID: ${state.spojId}")
                                    },
                                    onClick = {},
                                    trailingIcon = {
                                        Row {
                                            IconButton(
                                                onClick = {
                                                    context.startActivity(Intent.createChooser(Intent().apply {
                                                        action = Intent.ACTION_SEND
                                                        putExtra(Intent.EXTRA_TEXT, state.spojId)
                                                        type = "text/plain"
                                                    }, "Sdílet ID spoje"))
                                                    zobrazitMenu = false
                                                }
                                            ) {
                                                IconWithTooltip(Icons.Default.Share, "Sdílet")
                                            }
                                            IconButton(
                                                onClick = {
                                                    clipboardManager.setText(AnnotatedString(state.spojId))
                                                    zobrazitMenu = false
                                                }
                                            ) {
                                                IconWithTooltip(Icons.Default.ContentCopy, "Kopírovat")
                                            }
                                        }
                                    }
                                )
                                if (BuildConfig.DEBUG) DropdownMenuItem(
                                    text = {
                                        Text("Detail spoje v api na jihu")
                                    },
                                    onClick = {
                                        context.startActivity(Intent.createChooser(Intent().apply {
                                            action = Intent.ACTION_VIEW
                                            data = Uri.parse("https://dopravanajihu.cz/idspublicservices/api/servicedetail?id=${state.spojId}")
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
                                    onClick = {},
                                    trailingIcon = {
                                        Row {
                                            IconButton(
                                                onClick = {
                                                    context.startActivity(Intent.createChooser(Intent().apply {
                                                        action = Intent.ACTION_SEND
                                                        putExtra(Intent.EXTRA_TEXT, state.nazevSpoje)
                                                        type = "text/plain"
                                                    }, "Sdílet název spoje"))
                                                    zobrazitMenu = false
                                                }
                                            ) {
                                                IconWithTooltip(Icons.Default.Share, "Sdílet")
                                            }
                                            IconButton(
                                                onClick = {
                                                    clipboardManager.setText(AnnotatedString(state.nazevSpoje))
                                                    zobrazitMenu = false
                                                }
                                            ) {
                                                IconWithTooltip(Icons.Default.ContentCopy, "Kopírovat")
                                            }
                                        }
                                    }
                                )
                                DropdownMenuItem(
                                    text = {
                                        Text("Link: ${state.deeplink.removePrefix("https://jaro-jaro.github.io/DPMCB")}")
                                    },
                                    onClick = {},
                                    trailingIcon = {
                                        Row {
                                            IconButton(
                                                onClick = {
                                                    context.startActivity(Intent.createChooser(Intent().apply {
                                                        action = Intent.ACTION_SEND
                                                        putExtra(Intent.EXTRA_TEXT, state.deeplink)
                                                        type = "text/uri-list"
                                                    }, "Sdílet deeplink"))
                                                    zobrazitMenu = false
                                                }
                                            ) {
                                                IconWithTooltip(Icons.Default.Share, "Sdílet")
                                            }
                                            IconButton(
                                                onClick = {
                                                    clipboardManager.setText(AnnotatedString(state.deeplink))
                                                    zobrazitMenu = false
                                                }
                                            ) {
                                                IconWithTooltip(Icons.Default.ContentCopy, "Kopírovat")
                                            }
                                        }
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
                        Text(state.linkaKod)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MujText(
    text: String,
    color: Color = Color.Unspecified,
    navigate: NavigateFunction,
    cas: LocalTime,
    zastavka: String,
    pristiZastavka: String?,
    linka: Int,
) = Box {
    var showDropDown by rememberSaveable { mutableStateOf(false) }
    pristiZastavka?.let {
        DropdownMenu(
            expanded = showDropDown,
            onDismissRequest = {
                showDropDown = false
            }
        ) {
            DropdownMenuItem(
                text = {
                    Text("Zobrazit zastávkové JŘ")
                },
                onClick = {
                    navigate(
                        JizdniRadyDestination(
                            cisloLinky = linka,
                            zastavka = zastavka,
                            pristiZastavka = pristiZastavka,
                        )
                    )
                    showDropDown = false
                },
            )
        }
    }
    Text(
        text = text,
        color = color,
        modifier = Modifier
            .combinedClickable(
                onClick = {
                    navigate(
                        OdjezdyDestination(
                            cas = cas,
                            zastavka = zastavka,
                        )
                    )
                },
                onLongClick = {
                    showDropDown = true
                },
            )
            .defaultMinSize(24.dp, 24.dp),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}