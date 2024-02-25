package cz.jaro.dpmcb.ui.spoj

import android.content.Intent
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
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.material.icons.filled.GpsOff
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import cz.jaro.dpmcb.R
import cz.jaro.dpmcb.data.App
import cz.jaro.dpmcb.data.App.Companion.title
import cz.jaro.dpmcb.data.helperclasses.CastSpoje
import cz.jaro.dpmcb.data.helperclasses.NavigateFunction
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.IconWithTooltip
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.Offset
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.barvaZpozdeniTextu
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.funguj
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.hezky4p
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.hezky6p
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.navigateFunction
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.nazevKurzu
import cz.jaro.dpmcb.data.jikord.ZastavkaOnlineSpoje
import cz.jaro.dpmcb.data.realtions.CasNazevSpojIdLinkaPristi
import cz.jaro.dpmcb.ui.destinations.JizdniRadyDestination
import cz.jaro.dpmcb.ui.destinations.KurzDestination
import cz.jaro.dpmcb.ui.destinations.OdjezdyDestination
import cz.jaro.dpmcb.ui.kurz.BublinaZpozdeni
import cz.jaro.dpmcb.ui.kurz.Nazev
import cz.jaro.dpmcb.ui.kurz.Vozickar
import cz.jaro.dpmcb.ui.kurz.Vuz
import cz.jaro.dpmcb.ui.main.SuplikAkce
import kotlinx.coroutines.launch
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
    App.vybrano = SuplikAkce.NajitSpoj

    val state by viewModel.state.collectAsStateWithLifecycle()

    SpojScreen(
        state = state,
        navigate = navigator.navigateFunction,
        upravitOblibene = viewModel::upravitOblibeny,
        odstranitOblibene = viewModel::odebratOblibeny,
        zmenitdatum = viewModel::zmenitdatum,
    )
}

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
            is SpojState.Loading -> Loading()

            is SpojState.Neexistuje -> Neexistuje(state.spojId)

            is SpojState.Nejede -> {
                Chyby(state.pristeJedePoDnesku, zmenitdatum, state.pristeJedePoDatu, state.spojId, state.datum)

                KodyASdileni(state)
            }

            is SpojState.OK -> {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Nazev("${state.cisloLinky}")
                    Vozickar(
                        nizkopodlaznost = state.nizkopodlaznost,
                        potvrzenaNizkopodlaznost = (state as? SpojState.OK.Online)?.potvrzenaNizkopodlaznost,
                        Modifier.padding(start = 8.dp),
                        povolitVozik = true,
                    )

                    Spacer(Modifier.weight(1F))

                    TlacitkoKurzu(navigate, state.kurz)

                    Oblibenovac(
                        upravitOblibene = upravitOblibene,
                        odstranitOblibene = odstranitOblibene,
                        spojId = state.spojId,
                        oblibenaCastSpoje = state.oblibeny,
                        zastavky = state.zastavky
                    )
                }

                if (state is SpojState.OK.Online) Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    BublinaZpozdeni(state.zpozdeniMin)
                    Vuz(state.vuz)
                }


                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    if (state.vyluka) Vyluka()
                    if (state !is SpojState.OK.Online && state.chyba) Chyba()
                    OutlinedCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                    ) {
                        JizdniRad(
                            navigate = navigate,
                            zastavkyNaJihu = (state as? SpojState.OK.Online)?.zastavkyNaJihu,
                            pristiZastavka = (state as? SpojState.OK.Online)?.pristiZastavka,
                            zastavky = state.zastavky,
                            projetychUseku = state.projetychUseku,
                            vyska = state.vyska,
                            jeOnline = state is SpojState.OK.Online
                        )
                    }

                    KodyASdileni(state)
                }
            }
        }
    }
}

@Composable
private fun Loading() {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun Neexistuje(
    spojId: String,
) = Row(
    Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.Center
) {
    Text("Tento spoj (ID $spojId) bohužel neexistuje :(\nZkontrolujte, zda jste zadali správně ID.")
}

context(ColumnScope)
@Composable
private fun Chyby(
    pristeJedePoDnesku: LocalDate?,
    zmenitdatum: (LocalDate) -> Unit,
    pristeJedePoDatu: LocalDate?,
    spojId: String,
    datum: LocalDate,
) {
    pristeJedePoDatu.funguj(pristeJedePoDnesku)
    TextChyby(
        when {
            pristeJedePoDatu == null && pristeJedePoDnesku == null ->
                "Tento spoj (ID $spojId) bohužel ${datum.hezky6p()} nejede :(\nZkontrolujte, zda jste zadali správné datum."

            pristeJedePoDatu != null && pristeJedePoDnesku != null && pristeJedePoDatu != pristeJedePoDnesku ->
                "Tento spoj (ID $spojId) ${datum.hezky6p()} nejede. Jede mimo jiné ${pristeJedePoDatu.hezky6p()} nebo ${pristeJedePoDnesku.hezky6p()}"

            pristeJedePoDatu == null && pristeJedePoDnesku != null ->
                "Tento spoj (ID $spojId) ${datum.hezky6p()} nejede, ale pojede ${pristeJedePoDnesku.hezky6p()}."

            pristeJedePoDatu != null ->
                "Tento spoj (ID $spojId) ${datum.hezky6p()} nejede, ale pojede ${pristeJedePoDatu.hezky6p()}."

            else -> throw IllegalArgumentException()
        }
    )

    if (pristeJedePoDnesku != null) {
        ZmenitDatum(zmenitdatum, pristeJedePoDnesku)
    }
    if (pristeJedePoDatu != null && pristeJedePoDnesku != pristeJedePoDatu) {
        ZmenitDatum(zmenitdatum, pristeJedePoDatu)
    }
}

@Composable
private fun ZmenitDatum(zmenitdatum: (LocalDate) -> Unit, datum: LocalDate) {
    Button(
        onClick = {
            zmenitdatum(datum)
        },
        Modifier.padding(top = 16.dp)
    ) {
        Text("Změnit datum na ${datum.hezky4p()}")
    }
}

@Composable
private fun TextChyby(
    text: String,
) = Row(
    Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.Center
) {
    Text(text)
}

@Composable
fun JizdniRad(
    zastavky: List<CasNazevSpojIdLinkaPristi>,
    navigate: NavigateFunction,
    zastavkyNaJihu: List<ZastavkaOnlineSpoje>?,
    pristiZastavka: LocalTime?,
    zobrazitCaru: Boolean = true,
    projetychUseku: Int = 0,
    vyska: Float = 0F,
    jeOnline: Boolean = false,
) = Row(
    modifier = Modifier
        .fillMaxWidth()
        .height(IntrinsicSize.Max)
        .padding(12.dp)
) {
    Column(
        Modifier.weight(1F)
    ) {
        zastavky.forEachIndexed { i, it ->
            MujText(
                text = it.nazev,
                navigate = navigate,
                cas = it.cas,
                zastavka = it.nazev,
                pristiZastavka = it.pristiZastavka,
                linka = it.linka,
                stanoviste = if (zastavkyNaJihu != null) zastavkyNaJihu[i].stanoviste else "",
                Modifier.fillMaxWidth(1F),
                color = if (pristiZastavka != null && it.cas == pristiZastavka)
                    MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurface,
            )
        }
    }
    Column(Modifier.padding(start = 8.dp)) {
        zastavky.forEachIndexed { i, it ->
            MujText(
                text = it.cas.toString(),
                navigate = navigate,
                cas = it.cas,
                zastavka = it.nazev,
                pristiZastavka = it.pristiZastavka,
                linka = it.linka,
                stanoviste = if (zastavkyNaJihu != null) zastavkyNaJihu[i].stanoviste else "",
                color = if (pristiZastavka != null && it.cas == pristiZastavka)
                    MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurface,
            )
        }
    }
    if (zastavkyNaJihu != null) Column(Modifier.padding(start = 8.dp)) {
        zastavky
            .zip(zastavkyNaJihu)
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

    if (zobrazitCaru) Cara(
        zastavky = zastavky,
        projetychUseku = projetychUseku,
        vyska = vyska,
        jeOnline = jeOnline
    )
}

@Composable
private fun Cara(
    zastavky: List<CasNazevSpojIdLinkaPristi>,
    projetychUseku: Int,
    vyska: Float,
    jeOnline: Boolean,
) {
    val projetaBarva = if (jeOnline) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
    val barvaBusu = if (jeOnline) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurface
    val barvaPozadi = MaterialTheme.colorScheme.surface
    val baravCary = MaterialTheme.colorScheme.surfaceVariant
    val zastavek = zastavky.count()

    val animovanaVyska by animateFloatAsState(vyska, label = "HeightAnimation")

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

@Composable
private fun Chyba(
) = Card(
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
    Text(
        text = "Pravděpodobně spoj neodesílá data o své poloze, nebo má zpoždění a ještě nevyjel z výchozí zastávky. Často se také stává, že spoj je přibližně první tři minuty své jízdy offline a až poté začne odesílat aktuální data",
        Modifier.padding(all = 8.dp)
    )
}

@Composable
private fun Vyluka(
) = Card(
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

@Composable
private fun TlacitkoKurzu(
    navigate: NavigateFunction,
    kurz: String?,
) {
    if (kurz != null) TextButton(
        onClick = {
            navigate(KurzDestination(kurz))
        }
    ) {
        Text("Kurz: ${kurz.nazevKurzu()}")
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun Oblibenovac(
    upravitOblibene: (CastSpoje) -> Unit,
    odstranitOblibene: () -> Unit,
    spojId: String,
    oblibenaCastSpoje: CastSpoje?,
    zastavky: List<CasNazevSpojIdLinkaPristi>,
) {
    var show by remember { mutableStateOf(false) }
    var cast by remember { mutableStateOf(CastSpoje(spojId, -1, -1)) }

    FilledIconToggleButton(checked = oblibenaCastSpoje != null, onCheckedChange = {
        cast = oblibenaCastSpoje ?: CastSpoje(spojId, -1, -1)
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
            if (oblibenaCastSpoje == null) TextButton(
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
                        .verticalScroll(rememberScrollState())
                        .height(IntrinsicSize.Max)
                        .padding(8.dp)
                ) {
                    val start = remember { Animatable(cast.start.toFloat()) }
                    val end = remember { Animatable(cast.end.toFloat()) }
                    val alpha by animateFloatAsState(if (cast.start == -1) 0F else 1F, label = "AlphaAnimation")

                    val scope = rememberCoroutineScope()
                    fun click(i: Int) = scope.launch {
                        when {
                            cast.start == -1 && i == zastavky.lastIndex -> {}
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
                    val zastavek = zastavky.count()

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
                        zastavky.forEachIndexed { i, zast ->
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

context(ColumnScope)
@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun KodyASdileni(
    state: SpojState.Existuje,
) {
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