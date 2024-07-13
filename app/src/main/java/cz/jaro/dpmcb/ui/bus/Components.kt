package cz.jaro.dpmcb.ui.bus

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.Icon.createWithResource
import android.net.Uri
import android.os.Build
import android.service.chooser.ChooserAction
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.GpsOff
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import cz.jaro.dpmcb.R
import cz.jaro.dpmcb.data.entities.BusName
import cz.jaro.dpmcb.data.entities.bus
import cz.jaro.dpmcb.data.helperclasses.MutateFunction
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.toCzechAccusative
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.toCzechLocative
import cz.jaro.dpmcb.data.realtions.BusStop
import cz.jaro.dpmcb.data.realtions.StopType
import cz.jaro.dpmcb.data.realtions.favourites.PartOfConn
import cz.jaro.dpmcb.ui.common.DelayBubble
import cz.jaro.dpmcb.ui.common.IconWithTooltip
import cz.jaro.dpmcb.ui.common.Name
import cz.jaro.dpmcb.ui.common.StopTypeIcon
import cz.jaro.dpmcb.ui.common.Timetable
import cz.jaro.dpmcb.ui.common.Vehicle
import cz.jaro.dpmcb.ui.common.Wheelchair
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDate
import java.io.File

@Composable
fun BusDoesNotExist(
    busName: BusName,
) = ErrorMessage("Tento spoj ($busName) bohužel neexistuje :(\n Asi jste zadali špatně název.")

context(ColumnScope)
@Composable
fun BusDoesNotRun(
    runsNextTimeAfterToday: LocalDate?,
    onEvent: (BusEvent) -> Unit,
    runsNextTimeAfterDate: LocalDate?,
    busName: BusName,
    date: LocalDate,
) {
    ErrorMessage(
        when {
            runsNextTimeAfterDate == null && runsNextTimeAfterToday == null ->
                "Tento spoj ($busName) bohužel ${date.toCzechLocative()} nejede :(\nZkontrolujte, zda jste zadali správné datum."

            runsNextTimeAfterDate != null && runsNextTimeAfterToday != null && runsNextTimeAfterDate != runsNextTimeAfterToday ->
                "Tento spoj ($busName) ${date.toCzechLocative()} nejede. Jede mimo jiné ${runsNextTimeAfterDate.toCzechLocative()} nebo ${runsNextTimeAfterToday.toCzechLocative()}"

            runsNextTimeAfterDate == null && runsNextTimeAfterToday != null ->
                "Tento spoj ($busName) ${date.toCzechLocative()} nejede, ale pojede ${runsNextTimeAfterToday.toCzechLocative()}."

            runsNextTimeAfterDate != null ->
                "Tento spoj ($busName) ${date.toCzechLocative()} nejede, ale pojede ${runsNextTimeAfterDate.toCzechLocative()}."

            else -> throw IllegalArgumentException()
        }
    )

    if (runsNextTimeAfterToday != null) {
        ChangeDateButton(onEvent, runsNextTimeAfterToday)
    }
    if (runsNextTimeAfterDate != null && runsNextTimeAfterToday != runsNextTimeAfterDate) {
        ChangeDateButton(onEvent, runsNextTimeAfterDate)
    }
}

@Composable
fun ChangeDateButton(
    onEvent: (BusEvent) -> Unit,
    date: LocalDate,
) {
    Button(
        onClick = {
            onEvent(BusEvent.ChangeDate(date))
        },
        Modifier.padding(top = 16.dp)
    ) {
        Text("Změnit datum na ${date.toCzechAccusative()}")
    }
}

@Composable
fun ErrorMessage(
    text: String,
) = Row(
    Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.Center
) {
    Text(text)
}

@Composable
fun Error(
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
fun Restriction(
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


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SequenceRow(
    onEvent: (BusEvent) -> Unit,
    sequenceName: String?,
    hasNextBus: Boolean,
    hasPreviousBus: Boolean,
) {
    if (sequenceName != null) Row(
        modifier = Modifier.fillMaxWidth(),
    ) {
        IconButton(
            onClick = {
                if (hasPreviousBus) onEvent(BusEvent.PreviousBus)
            },
            enabled = hasPreviousBus,
        ) {
            IconWithTooltip(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Předchozí spoj kurzu"
            )
        }
        Box(
            Modifier.weight(1F),
            contentAlignment = Alignment.Center,
        ) {
            TextButton(
                onClick = {
                    onEvent(BusEvent.ShowSequence)
                }
            ) {
                Text("Kurz: $sequenceName")
            }
        }
        IconButton(
            onClick = {
                if (hasNextBus) onEvent(BusEvent.NextBus)
            },
            enabled = hasNextBus,
        ) {
            IconWithTooltip(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = "Následující spoj kurzu"
            )
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun Favouritificator(
    onEvent: (BusEvent) -> Unit,
    busName: BusName,
    favouritePartOfConn: PartOfConn?,
    stops: List<BusStop>,
) {
    var show by remember { mutableStateOf(false) }
    var part by remember { mutableStateOf(PartOfConn.Empty(busName)) }

    FilledIconToggleButton(checked = favouritePartOfConn != null, onCheckedChange = {
        part = favouritePartOfConn ?: PartOfConn.Empty(busName)
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
                    onEvent(BusEvent.ChangeFavourite(part))
                    show = false
                },
                enabled = part.start != -1 && part.end != -1
            ) {
                Text("Potvrdit")
            }
        },
        dismissButton = {
            if (favouritePartOfConn == null) TextButton(
                onClick = {
                    show = false
                }
            ) {
                Text("Zrušit")
            }
            else TextButton(
                onClick = {
                    onEvent(BusEvent.RemoveFavourite)
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
            ) {
                Text("Vyberte Váš oblíbený úsek tohoto spoje:")
                PartOfBusChooser(part, { part = it(part) }, stops, Modifier.verticalScroll(rememberScrollState()))
            }
        }
    )
}

context(ColumnScope)
@Composable
private fun PartOfBusChooser(
    part: PartOfConn,
    editPart: MutateFunction<PartOfConn>,
    stops: List<BusStop>,
    modifier: Modifier = Modifier,
) {
    fun isDisabled(i: Int) = when {
        part.start == -1 && i == stops.lastIndex -> true
        part.start == -1 && stops[i].type == StopType.GetOffOnly -> true

        part.start == -1 -> false
        part.start == i -> false
        part.end == i -> false

        i < part.start -> true
        stops[i].type == StopType.GetOnOnly -> true
        else -> false
    }

    Box(
        modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Max)
        ) {
            val start = remember { Animatable(part.start.toFloat()) }
            val end = remember { Animatable(part.end.toFloat()) }
            val alpha by animateFloatAsState(if (part.start == -1) 0F else 1F, label = "AlphaAnimation")

            val scope = rememberCoroutineScope()
            fun click(i: Int) = scope.launch {
                when {
                    part.start == -1 && stops[i].type == StopType.GetOffOnly -> {}
                    part.start == -1 && i == stops.lastIndex -> {}

                    part.start == -1 -> {
                        start.snapTo(i.toFloat())
                        end.snapTo(i.toFloat())
                        editPart { it.copy(start = i) }
                    }

                    part.start == i -> {
                        editPart { it.copy(start = -1, end = -1) }
                        end.snapTo(i.toFloat())
                    }

                    part.end == i -> {
                        editPart { it.copy(end = -1) }
                        end.animateTo(part.start.toFloat())
                    }

                    i < part.start -> {}
                    stops[i].type == StopType.GetOnOnly -> {}

                    else -> {
                        editPart { it.copy(end = i) }
                        end.animateTo(i.toFloat())
                    }
                }
            }

            val selectedColor = MaterialTheme.colorScheme.tertiary
            val lineColor = MaterialTheme.colorScheme.onSurfaceVariant
            val disabledColor1 = MaterialTheme.colorScheme.onSurface
            val disabledColor = MaterialTheme.colorScheme.onSurface.copy(alpha = .38F)
            val stopCount = stops.count()

            var canvasHeight by remember { mutableFloatStateOf(0F) }
            Box(
                Modifier
                    .fillMaxHeight()
                    .width(32.dp)
                    .padding(end = 8.dp)
                    .pointerInput(Unit) {
                        detectTapGestures { (_, y) ->
                            val rowHeight = canvasHeight / stopCount
                            val i = (y / rowHeight).toInt()
                            click(i)
                        }
                    }
            ) {
                Canvas(
                    Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            this.alpha = .38F
                        }
                ) {
                    canvasHeight = size.height
                    val rowHeight = canvasHeight / stopCount
                    val lineWidth = 4.5.dp.toPx()
                    val smallCircleRadius = 6.5.dp.toPx()

                    translate(left = 8.dp.toPx(), top = rowHeight * .5F) {

                        drawLine(
                            color = disabledColor1,
                            start = UtilFunctions.Offset(),
                            end = UtilFunctions.Offset(y = canvasHeight - rowHeight),
                            strokeWidth = lineWidth,
                        )

                        repeat(stopCount) { i ->
                            val isInFavouritePart = end.value <= i.toFloat() || i.toFloat() <= start.value
                            translate(top = i * rowHeight) {
                                if (isInFavouritePart && isDisabled(i)) drawCircle(
                                    color = disabledColor1,
                                    radius = smallCircleRadius,
                                    center = UtilFunctions.Offset(),
                                    style = Fill,
                                )
                            }
                        }
                    }
                }
                Canvas(
                    Modifier
                        .fillMaxSize()
                ) {
                    canvasHeight = size.height
                    val rowHeight = canvasHeight / stopCount
                    val lineWidth = 4.5.dp.toPx()
                    val smallCircleRadius = 6.5.dp.toPx()
                    val bigCircleRadius = 9.5.dp.toPx()

                    translate(left = 8.dp.toPx(), top = rowHeight * .5F) {

                        val iMin = stops.indices.first { !isDisabled(it) }
                        val iMax = stops.indices.last { !isDisabled(it) }
                        drawLine(
                            color = lineColor,
                            start = UtilFunctions.Offset(y = iMin * rowHeight),
                            end = UtilFunctions.Offset(y = iMax * rowHeight),
                            strokeWidth = lineWidth,
                        )

                        repeat(stopCount) { i ->
                            val isInFavouritePart = end.value <= i.toFloat() || i.toFloat() <= start.value
                            translate(top = i * rowHeight) {
                                if (isInFavouritePart && !isDisabled(i)) drawCircle(
                                    color = lineColor,
                                    radius = smallCircleRadius,
                                    center = UtilFunctions.Offset(),
                                    style = Fill,
                                )
                            }
                        }

                        drawCircle(
                            color = selectedColor,
                            radius = bigCircleRadius,
                            center = UtilFunctions.Offset(y = rowHeight * end.value),
                            style = Fill,
                            alpha = alpha,
                        )
                        drawCircle(
                            color = selectedColor,
                            radius = bigCircleRadius,
                            center = UtilFunctions.Offset(y = rowHeight * start.value),
                            style = Fill,
                            alpha = alpha,
                        )

                        if (part.start != -1) drawLine(
                            color = selectedColor,
                            start = UtilFunctions.Offset(y = start.value * rowHeight),
                            end = UtilFunctions.Offset(y = end.value * rowHeight),
                            strokeWidth = lineWidth,
                        )
                    }
                }
            }
            Column(
                Modifier
                    .weight(1F)
            ) {
                stops.forEachIndexed { i, stop ->
                    Row(
                        Modifier
                            .weight(1F)
                            .defaultMinSize(32.dp, 32.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Start,
                    ) {
                        Text(
                            text = stop.name,
                            Modifier
                                .clickable(enabled = !isDisabled(i)) {
                                    click(i)
                                }
                                .weight(1F)
                                .defaultMinSize(24.dp, 24.dp),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = when {
                                isDisabled(i) -> disabledColor
                                i == part.start || i == part.end -> selectedColor
                                else -> MaterialTheme.colorScheme.onSurface
                            }
                        )
                        if (stop.type != StopType.Normal) StopTypeIcon(
                            stop.type, color = when {
                                isDisabled(i) -> disabledColor
                                i == part.start || i == part.end -> selectedColor
                                else -> MaterialTheme.colorScheme.onSurface
                            }
                        )
                    }
                }
            }
        }
    }
}

fun share(context: Context, state: BusState.Exists) {
    val isAtLeast34 = Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE
    val actions = if (isAtLeast34) buildList {
        this += ChooserAction.Builder(
            createWithResource(context, R.drawable.baseline_content_copy_24),
            "Kopírovat",
            PendingIntent.getBroadcast(
                context,
                0,
                Intent(context, BroadcastReceiver::class.java).apply {
                    putExtra("type", BroadcastReceiver.TYPE_COPY)
                },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            ),
        ).build()
        if (state is BusState.OK) this += ChooserAction.Builder(
            createWithResource(context, R.drawable.baseline_image_24),
            "Přidat obrázek",
            PendingIntent.getBroadcast(
                context,
                1,
                Intent(context, BroadcastReceiver::class.java).apply {
                    putExtra("type", BroadcastReceiver.TYPE_ADD_IMAGE)
                },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            ),
        ).build()
        if (state is BusState.OK) this += ChooserAction.Builder(
            createWithResource(context, R.drawable.baseline_timeline_24),
            "Pouze část spoje",
            PendingIntent.getBroadcast(
                context,
                2,
                Intent(context, BroadcastReceiver::class.java).apply {
                    putExtra("type", BroadcastReceiver.TYPE_SHARE_PART)
                },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            ),
        ).build()
    }.toTypedArray() else null

    context.startActivity(Intent.createChooser(Intent().apply {
        action = Intent.ACTION_SEND

        putExtra(
            Intent.EXTRA_TEXT,
            if (state is BusState.OK) buildString {
                +"Linka ${state.lineNumber}: ${state.stops.first().name} (${state.stops.first().time}) -> ${state.stops.last().name} (${state.stops.last().time})\n"
                +state.deeplink
            }
            else state.deeplink
        )
        putExtra(Intent.EXTRA_TITLE, "Sdílet spoj")

        type = "text/plain"
        flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
    }, "Sdílet spoj").apply {
        if (isAtLeast34)
            putExtra(Intent.EXTRA_CHOOSER_CUSTOM_ACTIONS, actions!!)
    })
}

suspend fun shareImage(context: Context, state: BusState.Exists, bitmap: ImageBitmap) = withContext(Dispatchers.IO) {
    val d = context.filesDir
    val f = File(d, "spoj_${state.busName.value.replace('/', '_')}.png")
    f.createNewFile()

    f.outputStream().use { os ->
        bitmap.asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, os)
        os.flush()
    }

    val uri = FileProvider.getUriForFile(context, context.applicationContext.packageName + ".provider", f)

    context.startActivity(Intent.createChooser(Intent().apply {
        action = Intent.ACTION_SEND

        putExtra(Intent.EXTRA_STREAM, uri)

        putExtra(Intent.EXTRA_TEXT, state.deeplink)
        putExtra(Intent.EXTRA_TITLE, "Sdílet spoj")

        setDataAndType(uri, "image/png")
        flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
    }, "Sdílet spoj"))
}

fun copy(clipboardManager: ClipboardManager, state: BusState.Exists) {
    clipboardManager.setText(AnnotatedString(state.deeplink))
}

context(ColumnScope)
@Composable
fun CodesAndShare(
    state: BusState.Exists,
) = CodesAndShare(
    state = state,
    graphicsLayerWhole = rememberGraphicsLayer(),
    graphicsLayerPart = rememberGraphicsLayer(),
    part = PartOfConn.Empty(state.busName),
    editPart = { },
)

context(ColumnScope)
@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun CodesAndShare(
    state: BusState.Exists,
    graphicsLayerWhole: GraphicsLayer,
    graphicsLayerPart: GraphicsLayer,
    part: PartOfConn,
    editPart: MutateFunction<PartOfConn>,
) {
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    var show by remember { mutableStateOf(false) }
    var dropdown by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        for (e in BroadcastReceiver.clicked) when (e) {
            BroadcastReceiver.TYPE_COPY -> copy(clipboardManager, state)
            BroadcastReceiver.TYPE_ADD_IMAGE -> shareImage(context, state, graphicsLayerWhole.toImageBitmap())
            BroadcastReceiver.TYPE_SHARE_PART -> show = true
        }
    }

    val scope = rememberCoroutineScope()

    if (show && state is BusState.OK) AlertDialog(
        onDismissRequest = {
            show = false
        },
        confirmButton = {
            TextButton(
                onClick = {
                    scope.launch {
                        show = false
                        shareImage(context, state, graphicsLayerPart.toImageBitmap())
                        editPart { PartOfConn.Empty(state.busName) }
                    }
                },
                enabled = part.start != -1 && part.end != -1
            ) {
                Text("Potvrdit")
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    show = false
                }
            ) {
                Text("Zrušit")
            }
        },
        title = {
            Text("Sdílet část spoje")
        },
        icon = {
            Icon(Icons.Default.Star, null)
        },
        text = {
            Column(
                Modifier
                    .fillMaxWidth()
            ) {
                Text("Vyberte úsek spoje, který chcete sdílet:")
                PartOfBusChooser(part, editPart, state.stops, Modifier.verticalScroll(rememberScrollState()))
            }
        }
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
    ) {
        TextButton(onClick = {
            CustomTabsIntent.Builder()
                .setShowTitle(true)
                .build()
                .launchUrl(context, Uri.parse("https://jih.mpvnet.cz/Jikord/map/Route?mode=0,0,2,0,${state.busName.value.replace('/', ',')},0"))
        }) {
            IconWithTooltip(Icons.Filled.Language, null, Modifier.size(ButtonDefaults.IconSize))
            Spacer(Modifier.width(ButtonDefaults.IconSpacing))
            Text("Zobrazit na mapě")
        }
        Spacer(Modifier.weight(1F))
        TextButton(
            onClick = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
                    share(context, state)
                else
                    dropdown = true
            },
            contentPadding = ButtonDefaults.TextButtonWithIconContentPadding
        ) {
            DropdownMenu(
                expanded = dropdown,
                onDismissRequest = { dropdown = false }
            ) {
                DropdownMenuItem(
                    text = { Text("Sdílet odkaz") },
                    onClick = { share(context, state) },
                    trailingIcon = { IconWithTooltip(Icons.Default.Share, null) }
                )
                DropdownMenuItem(
                    text = { Text("Kopírovat odkaz") },
                    onClick = { scope.launch { BroadcastReceiver.clicked.send(BroadcastReceiver.TYPE_COPY) } },
                    trailingIcon = { IconWithTooltip(Icons.Default.ContentCopy, null) }
                )
                if (state is BusState.OK) DropdownMenuItem(
                    text = { Text("Sdílet s obrázkem") },
                    onClick = { scope.launch { BroadcastReceiver.clicked.send(BroadcastReceiver.TYPE_ADD_IMAGE) } },
                    trailingIcon = { IconWithTooltip(Icons.Default.Image, null) }
                )
                if (state is BusState.OK) DropdownMenuItem(
                    text = { Text("Sdílet obrázek části spoje") },
                    onClick = { scope.launch { BroadcastReceiver.clicked.send(BroadcastReceiver.TYPE_SHARE_PART) } },
                    trailingIcon = { IconWithTooltip(Icons.Default.Timeline, null) }
                )
            }
            Text("Sdílet")
            Spacer(Modifier.width(ButtonDefaults.IconSpacing))
            IconWithTooltip(Icons.Filled.Share, null, Modifier.size(ButtonDefaults.IconSize))
        }
    }

    Column {
        state.fixedCodes.forEach {
            Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        state.timeCodes.forEach {
            Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(state.lineCode, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun ShareLayout(graphicsLayer: GraphicsLayer, state: BusState.OK, part: PartOfConn?) = Column(
    modifier = Modifier
        .fillMaxWidth()
        .drawWithContent {
            graphicsLayer.record {
                this@drawWithContent.drawContent()
            }
        }
        .clip(MaterialTheme.shapes.medium)
        .background(MaterialTheme.colorScheme.background)
        .padding(bottom = 8.dp, start = 8.dp, end = 8.dp, top = 8.dp)
) {
    Row(
        Modifier
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Name("${state.lineNumber}", subName = "/${state.busName.bus()}")
        Wheelchair(
            lowFloor = state.lowFloor,
            confirmedLowFloor = (state as? BusState.OnlineRunning)?.confirmedLowFloor,
            Modifier.padding(start = 8.dp),
            enableCart = true,
        )

        if (state is BusState.OnlineRunning) DelayBubble(state.delayMin)
        if (state is BusState.OnlineRunning) Vehicle(state.vehicle, showInfoButton = false)
    }
    OutlinedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
    ) {
        Timetable(
            navigate = {},
            onlineConnStops = (state as? BusState.Online)?.onlineConnStops,
            nextStopIndex = (state as? BusState.OnlineRunning)?.nextStopIndex,
            stops = state.stops,
            traveledSegments = state.traveledSegments,
            height = state.lineHeight,
            isOnline = state is BusState.OnlineRunning,
            part = part
        )
    }
}

context(StringBuilder) operator fun String.unaryPlus(): StringBuilder = append(this)