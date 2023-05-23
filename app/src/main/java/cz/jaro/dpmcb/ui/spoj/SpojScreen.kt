package cz.jaro.dpmcb.ui.spoj

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Badge
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
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
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
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.hezky
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.navigateFunction
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.toSign
import cz.jaro.dpmcb.data.naJihu.ZastavkaSpojeNaJihu
import cz.jaro.dpmcb.ui.destinations.JizdniRadyDestination
import cz.jaro.dpmcb.ui.destinations.OdjezdyDestination
import cz.jaro.dpmcb.ui.main.SuplikAkce
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.ParametersHolder
import java.time.LocalTime
import kotlin.math.roundToInt

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

    val info by viewModel.info.collectAsStateWithLifecycle()
    val stateZJihu = viewModel.stateZJihu.collectAsStateWithLifecycle()
    val vyska = viewModel.vyska.collectAsStateWithLifecycle(0F)
    val projetychUseku = viewModel.projetychUseku.collectAsStateWithLifecycle(0)
    val oblibene = viewModel.oblibene.collectAsStateWithLifecycle()
    val datum = viewModel.datum.collectAsStateWithLifecycle()

    SpojScreen(
        info = info,
        zpozdeni = remember(stateZJihu.value) { derivedStateOf { stateZJihu.value.zpozdeni?.inWholeSeconds?.div(60F)?.roundToInt() } },
        zastavkyNaJihu = remember(stateZJihu.value) { derivedStateOf { stateZJihu.value.zastavkyNaJihu } },
        projetychUseku = projetychUseku,
        vyska = vyska,
        navigate = navigator.navigateFunction,
        oblibene = oblibene,
        pridatOblibeny = viewModel.pridatOblibeny,
        odebratOblibeny = viewModel.odebratOblibeny,
        datum = remember(datum.value) { derivedStateOf { datum.value.hezky() } },
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun SpojScreen(
    info: SpojInfo,
    zpozdeni: State<Int?>,
    zastavkyNaJihu: State<List<ZastavkaSpojeNaJihu>?>,
    projetychUseku: State<Int>,
    vyska: State<Float>,
    navigate: NavigateFunction,
    oblibene: State<List<String>>,
    pridatOblibeny: (String) -> Unit,
    odebratOblibeny: (String) -> Unit,
    datum: State<String>,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp)
    ) {
        when (info) {
            is SpojInfo.Loading -> Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator()
            }

            is SpojInfo.Neexistuje -> Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                Text("Tento spoj (ID ${info.spojId}) bohužel ${datum.value} nejede, nebo vůbec neexistuje :(\nZkontrolujte, zda jste zadali správně ID, nebo zkuste změnit datum.")
            }

            is SpojInfo.OK -> {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp), verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Linka ${info.cisloLinky}")
                    IconWithTooltip(
                        info.nizkopodlaznost.first, info.nizkopodlaznost.second, modifier = Modifier.padding(start = 8.dp)
                    )
                    if (zpozdeni.value != null) Badge(
                        containerColor = barvaZpozdeniBublinyKontejner(zpozdeni.value!!),
                        contentColor = barvaZpozdeniBublinyText(zpozdeni.value!!),
                    ) {
                        Text(
                            text = zpozdeni.value!!.run {
                                "${toSign()}$this min"
                            },
                        )
                    }
                    Spacer(Modifier.weight(1F))
                    FilledIconToggleButton(checked = info.spojId in oblibene.value, onCheckedChange = {
                        if (it) {
                            pridatOblibeny(info.spojId)
                        } else {
                            odebratOblibeny(info.spojId)
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
                    if (info.vyluka) Card(
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
                            Column {
                                info.zastavky.forEach {
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
                                info.zastavky.forEach {
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
                            if (zpozdeni.value != null && zastavkyNaJihu.value != null) Column(Modifier.padding(start = 8.dp)) {
                                info.zastavky
                                    .zip(zastavkyNaJihu.value!!)
                                    .forEach { (zastavka, zastavkaNaJihu) ->
                                        MujText(
                                            text = if (!zastavkaNaJihu.passed) (zastavka.cas.plusMinutes(zpozdeni.value!!.toLong())).toString() else "",
                                            color = barvaZpozdeniTextu(zpozdeni.value!!),
                                            navigate = navigate,
                                            cas = zastavka.cas,
                                            zastavka = zastavka.nazev,
                                            pristiZastavka = zastavka.pristiZastavka,
                                            linka = zastavka.linka,
                                        )
                                    }
                            }
                            val neNaMape = zpozdeni.value != null && zastavkyNaJihu.value != null

                            val projetaBarva = if (neNaMape) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            val barvaBusu = if (neNaMape) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurface
                            val barvaPozadi = MaterialTheme.colorScheme.surface
                            val baravCary = MaterialTheme.colorScheme.surfaceVariant
                            val zastavek = info.zastavky.count()

                            val animovanaVyska by animateFloatAsState(vyska.value, label = "HeightAnimation")

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
                                            val projel = projetychUseku.value >= i

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

                                    if (vyska.value > 0F) drawCircle(
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
                            Text("ID: ${info.spojId}")
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
                                        Text("ID: ${info.spojId}")
                                    },
                                    onClick = {},
                                    trailingIcon = {
                                        Row {
                                            IconButton(
                                                onClick = {
                                                    context.startActivity(Intent.createChooser(Intent().apply {
                                                        action = Intent.ACTION_SEND
                                                        putExtra(Intent.EXTRA_TEXT, info.spojId)
                                                        type = "text/plain"
                                                    }, "Sdílet ID spoje"))
                                                    zobrazitMenu = false
                                                }
                                            ) {
                                                IconWithTooltip(Icons.Default.Share, "Sdílet")
                                            }
                                            IconButton(
                                                onClick = {
                                                    clipboardManager.setText(AnnotatedString(info.spojId))
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
                                            data = Uri.parse("https://dopravanajihu.cz/idspublicservices/api/servicedetail?id=${info.spojId}")
                                        }, "Sdílet ID spoje"))
                                        zobrazitMenu = false
                                    },
                                    trailingIcon = {
                                        IconWithTooltip(Icons.Default.Public, null)
                                    }
                                )
                                DropdownMenuItem(
                                    text = {
                                        Text("Název: ${info.nazevSpoje}")
                                    },
                                    onClick = {},
                                    trailingIcon = {
                                        Row {
                                            IconButton(
                                                onClick = {
                                                    context.startActivity(Intent.createChooser(Intent().apply {
                                                        action = Intent.ACTION_SEND
                                                        putExtra(Intent.EXTRA_TEXT, info.nazevSpoje)
                                                        type = "text/plain"
                                                    }, "Sdílet název spoje"))
                                                    zobrazitMenu = false
                                                }
                                            ) {
                                                IconWithTooltip(Icons.Default.Share, "Sdílet")
                                            }
                                            IconButton(
                                                onClick = {
                                                    clipboardManager.setText(AnnotatedString(info.nazevSpoje))
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
                                        Text("Link: ${info.deeplink.removePrefix("https://jaro-jaro.github.io/DPMCB")}")
                                    },
                                    onClick = {},
                                    trailingIcon = {
                                        Row {
                                            IconButton(
                                                onClick = {
                                                    context.startActivity(Intent.createChooser(Intent().apply {
                                                        action = Intent.ACTION_SEND
                                                        putExtra(Intent.EXTRA_TEXT, info.deeplink)
                                                        type = "text/uri-list"
                                                    }, "Sdílet deeplink"))
                                                    zobrazitMenu = false
                                                }
                                            ) {
                                                IconWithTooltip(Icons.Default.Share, "Sdílet")
                                            }
                                            IconButton(
                                                onClick = {
                                                    clipboardManager.setText(AnnotatedString(info.deeplink))
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
                        info.pevneKody.forEach {
                            Text(it)
                        }
                        info.caskody.forEach {
                            Text(it)
                        }
                        Text(info.linkaKod)
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
            .height(24.dp),
    )
}