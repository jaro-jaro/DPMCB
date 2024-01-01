package cz.jaro.dpmcb.ui.spoj

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.material.icons.automirrored.filled.Accessible
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.GpsOff
import androidx.compose.material.icons.filled.NotAccessible
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import cz.jaro.dpmcb.BuildConfig
import cz.jaro.dpmcb.R
import cz.jaro.dpmcb.data.App
import cz.jaro.dpmcb.data.App.Companion.title
import cz.jaro.dpmcb.data.helperclasses.CastSpoje
import cz.jaro.dpmcb.data.helperclasses.NavigateFunction
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.IconWithTooltip
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.Offset
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.barvaZpozdeniBublinyKontejner
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.barvaZpozdeniBublinyText
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.barvaZpozdeniTextu
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.hezky4p
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.hezky6p
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.navigateFunction
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.toSign
import cz.jaro.dpmcb.ui.destinations.JizdniRadyDestination
import cz.jaro.dpmcb.ui.destinations.OdjezdyDestination
import cz.jaro.dpmcb.ui.main.SuplikAkce
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.ParametersHolder
import java.time.LocalDate
import java.time.LocalTime
import kotlin.random.Random
import kotlin.time.Duration.Companion.minutes

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
        upravitOblibene = viewModel::upravitOblibeny,
        odstranitOblibene = viewModel::odebratOblibeny,
        zmenitdatum = viewModel::zmenitdatum,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpojScreen(
    state: SpojState,
    navigate: NavigateFunction,
    upravitOblibene: (CastSpoje) -> Unit,
    odstranitOblibene: () -> Unit,
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
                            Text("Změnit datum na ${state.pristeJedePoDnesku.hezky4p()}")
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
                            Text("Změnit datum na ${state.pristeJedePoDatu.hezky4p()}")
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
                            Text("Změnit datum na ${state.pristeJedePoDatu.hezky4p()}")
                        }
                        Button(
                            onClick = {
                                zmenitdatum(state.pristeJedePoDnesku)
                            },
                            Modifier.padding(top = 16.dp)
                        ) {
                            Text("Změnit datum na ${state.pristeJedePoDnesku.hezky4p()}")
                        }
                    }
                }
            }

            is SpojState.OK -> {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(8.dp), verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "Linka ${state.cisloLinky}", fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)
                    IconWithTooltip(
                        remember(state.nizkopodlaznost) {
                            when {
                                Random.nextFloat() < .01F -> Icons.Default.ShoppingCart
                                state is SpojState.OK.Online && state.potvrzenaNizkopodlaznost == true -> Icons.AutoMirrored.Filled.Accessible
                                state is SpojState.OK.Online && state.potvrzenaNizkopodlaznost == false -> Icons.Default.NotAccessible
                                state.nizkopodlaznost -> Icons.AutoMirrored.Filled.Accessible
                                else -> Icons.Default.NotAccessible
                            }
                        },
                        when {
                            state is SpojState.OK.Online && state.potvrzenaNizkopodlaznost == true -> "Potvrzený nízkopodlažní vůz"
                            state is SpojState.OK.Online && state.potvrzenaNizkopodlaznost == false -> "Potvrzený nenízkopodlažní vůz"
                            state.nizkopodlaznost -> "Plánovaný nízkopodlažní vůz"
                            else -> "Nezaručený nízkopodlažní vůz"
                        },
                        Modifier.padding(start = 8.dp),
                        tint = when {
                            state is SpojState.OK.Online && state.potvrzenaNizkopodlaznost == false && state.nizkopodlaznost -> MaterialTheme.colorScheme.error
                            state is SpojState.OK.Online && state.potvrzenaNizkopodlaznost != null -> MaterialTheme.colorScheme.primary
                            else -> MaterialTheme.colorScheme.onSurface
                        }
                    )
                    if (state is SpojState.OK.Online) Badge(
                        containerColor = barvaZpozdeniBublinyKontejner(state.zpozdeniMin),
                        contentColor = barvaZpozdeniBublinyText(state.zpozdeniMin),
                    ) {
                        Text(
                            text = state.zpozdeniMin.toDouble().minutes.run {
                                "${inWholeSeconds.toSign()}$inWholeMinutes min ${inWholeSeconds % 60} s"
                            },
                        )
                    }
                    if (state is SpojState.OK.Online && state.vuz != null) {
                        Text(
                            text = "ev. č. ${state.vuz}",
                            Modifier.padding(horizontal = 8.dp)
                        )
                    }

                    Spacer(Modifier.weight(1F))

                    var show by remember { mutableStateOf(false) }
                    var cast by remember { mutableStateOf(CastSpoje(state.spojId, -1, -1)) }

                    FilledIconToggleButton(checked = state.oblibeny != null, onCheckedChange = {
                        cast = state.oblibeny ?: CastSpoje(state.spojId, -1, -1)
                        show = true
                    }) {
                        IconWithTooltip(Icons.Default.Star, "Oblíbené")
                    }

                    if (show) AlertDialog(
                        onDismissRequest = {
                            show = false
                        },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    upravitOblibene(cast)
                                    show = false
                                },
                                enabled = cast.start != -1 && cast.end != -1
                            ) {
                                Text("Potvrdit")
                            }
                        },
                        dismissButton = {
                            if (state.oblibeny == null) TextButton(
                                onClick = {
                                    show = false
                                }
                            ) {
                                Text("Zrušit")
                            }
                            else TextButton(
                                onClick = {
                                    odstranitOblibene()
                                    show = false
                                }
                            ) {
                                Text("Odstranit")
                            }
                        },
                        title = {
                            Text("Upravit oblíbený spoj")
                        },
                        icon = {
                            Icon(Icons.Default.Star, null)
                        },
                        text = {
                            Column(
                                Modifier
                                    .fillMaxWidth()
//                                    .verticalScroll(rememberScrollState())
                            ) {
                                Text("Vyberte Váš oblíbený úsek tohoto spoje:")
                                Row(
                                    Modifier
                                        .fillMaxWidth()
                                        .height(IntrinsicSize.Max)
                                        .padding(8.dp)
                                ) {
                                    val start = remember { Animatable(cast.start.toFloat()) }
                                    val end = remember { Animatable(cast.end.toFloat()) }
                                    val alpha by animateFloatAsState(if (cast.start == -1) 0F else 1F, label = "AlphaAnimation")

                                    val scope = rememberCoroutineScope()
                                    fun click(i: Int) = scope.launch {
                                        when {
                                            cast.start == -1 && i == state.zastavky.lastIndex -> {}
                                            cast.start == -1 -> {
                                                start.snapTo(i.toFloat())
                                                end.snapTo(i.toFloat())
                                                cast = cast.copy(start = i)
                                            }

                                            cast.start == i -> {
                                                cast = cast.copy(start = -1, end = -1)
                                                end.snapTo(i.toFloat())
                                            }

                                            cast.end == i -> {
                                                cast = cast.copy(end = -1)
                                                end.animateTo(cast.start.toFloat())
                                            }

                                            i < cast.start -> {
                                                cast = cast.copy(start = i)
                                                if (cast.end == -1) launch { end.animateTo(cast.start.toFloat()) }
                                                start.animateTo(cast.start.toFloat())
                                            }

                                            else /*cast.start < i*/ -> {
                                                cast = cast.copy(end = i)
                                                end.animateTo(cast.end.toFloat())
                                            }
                                        }
                                    }

                                    val barvaVybrano = MaterialTheme.colorScheme.secondary
                                    val baravCary = MaterialTheme.colorScheme.onSurfaceVariant
                                    val zastavek = state.zastavky.count()

                                    var canvasHeight by remember { mutableFloatStateOf(0F) }
                                    Canvas(
                                        Modifier
                                            .fillMaxHeight()
                                            .width(24.dp)
                                            .padding(horizontal = 8.dp)
                                            .pointerInput(Unit) {
                                                detectTapGestures { (_, y) ->
                                                    val rowHeight = canvasHeight / zastavek
                                                    val i = (y / rowHeight).toInt()
                                                    click(i)
                                                }
                                            }
                                    ) {
                                        canvasHeight = size.height
                                        val rowHeight = canvasHeight / zastavek
                                        val lineWidth = 4.5.dp.toPx()
                                        val lineXOffset = 0F
                                        val smallCircleRadius = 6.5.dp.toPx()
                                        val bigCircleRadius = 9.5.dp.toPx()

                                        translate(left = lineXOffset, top = rowHeight * .5F) {
                                            drawLine(
                                                color = baravCary,
                                                start = Offset(),
                                                end = Offset(y = canvasHeight - rowHeight),
                                                strokeWidth = lineWidth,
                                            )

                                            repeat(zastavek) { i ->
                                                translate(top = i * rowHeight) {
                                                    if (i.toFloat() <= start.value || end.value <= i.toFloat()) drawCircle(
                                                        color = baravCary,
                                                        radius = smallCircleRadius,
                                                        center = Offset(),
                                                        style = Fill,
                                                    )
                                                }
                                            }

                                            drawCircle(
                                                color = barvaVybrano,
                                                radius = bigCircleRadius,
                                                center = Offset(y = rowHeight * end.value),
                                                style = Fill,
                                                alpha = alpha,
                                            )
                                            drawCircle(
                                                color = barvaVybrano,
                                                radius = bigCircleRadius,
                                                center = Offset(y = rowHeight * start.value),
                                                style = Fill,
                                                alpha = alpha,
                                            )

                                            if (cast.start != -1) drawLine(
                                                color = barvaVybrano,
                                                start = Offset(y = start.value * rowHeight),
                                                end = Offset(y = end.value * rowHeight),
                                                strokeWidth = lineWidth,
                                            )
                                        }
                                    }
                                    Column(
                                        Modifier
                                            .weight(1F)
                                    ) {
                                        state.zastavky.forEachIndexed { i, zast ->
                                            Box(
                                                Modifier
                                                    .weight(1F)
                                                    .defaultMinSize(32.dp, 32.dp),
                                                contentAlignment = Alignment.CenterStart
                                            ) {
                                                Text(
                                                    text = zast.nazev,
                                                    Modifier
                                                        .clickable {
                                                            click(i)
                                                        },
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    color = if (i == cast.start || i == cast.end) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurface
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    )
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
                    if (state !is SpojState.OK.Online && state.chyba) Card(
                        Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        )
                    ) {
                        Row(
                            Modifier.padding(top = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.GpsOff, null, Modifier.padding(horizontal = 8.dp))
                            Text(text = "Offline", Modifier.padding(horizontal = 8.dp), style = MaterialTheme.typography.headlineSmall)
                        }
                        Text(text = "Pravděpodobně máte slabé připojení k internetu, nebo tento spoj neodesílá data o své poloze. Také je možné, že spoj má zpoždění a ještě nevyjel ze své výchozí zastávky.", Modifier.padding(all = 8.dp))
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
                                Modifier.weight(1F)
                            ) {
                                state.zastavky.forEachIndexed { i, it ->
                                    MujText(
                                        text = it.nazev,
                                        navigate = navigate,
                                        cas = it.cas,
                                        zastavka = it.nazev,
                                        pristiZastavka = it.pristiZastavka,
                                        linka = it.linka,
                                        stanoviste = if (state is SpojState.OK.Online) state.zastavkyNaJihu[i].stanoviste else "",
                                        Modifier.fillMaxWidth(1F),
                                        color = if (state is SpojState.OK.Online && it.cas == state.pristiZastavka)
                                            MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurface,
                                    )
                                }
                            }
                            Column(Modifier.padding(start = 8.dp)) {
                                state.zastavky.forEachIndexed { i, it ->
                                    MujText(
                                        text = it.cas.toString(),
                                        navigate = navigate,
                                        cas = it.cas,
                                        zastavka = it.nazev,
                                        pristiZastavka = it.pristiZastavka,
                                        linka = it.linka,
                                        stanoviste = if (state is SpojState.OK.Online) state.zastavkyNaJihu[i].stanoviste else "",
                                        color = if (state is SpojState.OK.Online && it.cas == state.pristiZastavka)
                                            MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurface,
                                    )
                                }
                            }
                            if (state is SpojState.OK.Online) Column(Modifier.padding(start = 8.dp)) {
                                state.zastavky
                                    .zip(state.zastavkyNaJihu)
                                    .forEach { (zastavka, zastavkaNaJihu) ->
                                        MujText(
                                            text = zastavka.cas.plusMinutes(zastavkaNaJihu.zpozdeni.toLong()).toString(),
                                            navigate = navigate,
                                            cas = zastavka.cas.plusMinutes(zastavkaNaJihu.zpozdeni.toLong()),
                                            zastavka = zastavka.nazev,
                                            pristiZastavka = zastavka.pristiZastavka,
                                            linka = zastavka.linka,
                                            stanoviste = zastavkaNaJihu.stanoviste,
                                            color = barvaZpozdeniTextu(zastavkaNaJihu.zpozdeni.toFloat()),
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
                                    .width(20.dp)
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
                            Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        state.caskody.forEach {
                            Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text(state.linkaKod, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
    navigate: NavigateFunction,
    cas: LocalTime,
    zastavka: String,
    pristiZastavka: String?,
    linka: Int,
    stanoviste: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    style: TextStyle = LocalTextStyle.current,
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
                    Text("$zastavka $stanoviste")
                },
                onClick = {},
                enabled = false
            )
            DropdownMenuItem(
                text = {
                    Text("Zobrazit odjezdy")
                },
                onClick = {
                    navigate(
                        OdjezdyDestination(
                            cas = cas,
                            zastavka = zastavka,
                        )
                    )
                    showDropDown = false
                },
            )
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
        modifier = modifier
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
        overflow = TextOverflow.Ellipsis,
        style = style,
    )
}