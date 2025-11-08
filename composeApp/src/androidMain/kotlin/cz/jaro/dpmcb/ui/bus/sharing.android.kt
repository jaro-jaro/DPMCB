package cz.jaro.dpmcb.ui.bus

import android.app.PendingIntent
import android.content.ClipData
import android.content.ClipDescription
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.Icon.createWithResource
import android.os.Build
import android.service.chooser.ChooserAction
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.platform.Clipboard
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.toClipEntry
import androidx.core.content.FileProvider
import cz.jaro.dpmcb.R
import cz.jaro.dpmcb.data.helperclasses.MutateFunction
import cz.jaro.dpmcb.data.realtions.Empty
import cz.jaro.dpmcb.data.realtions.PartOfConn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.number
import java.io.File

@Suppress("AssignedValueIsNeverRead", "KotlinConstantConditions")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
actual fun busShareManager(
    state: BusState.Exists,
    graphicsLayerWhole: GraphicsLayer,
    graphicsLayerPart: GraphicsLayer,
    part: State<PartOfConn>,
    editPart: MutateFunction<PartOfConn>,
): BusShareManager {
    val clipboard = LocalClipboard.current
    val context = LocalContext.current
    var show by remember { mutableStateOf(false) }
    var dropdown by remember { mutableStateOf(false) }
    var showDateRemoval by remember { mutableStateOf(true) }
    var deeplink by remember { mutableStateOf(state.deeplink) }

    LaunchedEffect(Unit) {
        BroadcastReceiver.clickedFlow.collect { e ->
            when (e) {
                BroadcastReceiver.ActionType.REMOVE_DATE_FROM_BUS -> {
                    deeplink = state.deeplink2
                    showDateRemoval = false
                    share(context, state, showDateRemoval, deeplink)
                }

                BroadcastReceiver.ActionType.REMOVE_DATE_FROM_IMAGE -> {
                    deeplink = state.deeplink2
                    showDateRemoval = false
                    shareImage(context, state, graphicsLayerWhole.toImageBitmap(), null, showDateRemoval, deeplink)
                }

                BroadcastReceiver.ActionType.REMOVE_DATE_FROM_IMAGE_PART -> {
                    deeplink = state.deeplink2
                    showDateRemoval = false
                    shareImage(context, state, graphicsLayerPart.toImageBitmap(), part.value, showDateRemoval, deeplink)
                }

                BroadcastReceiver.ActionType.COPY -> copy(clipboard, deeplink)
                BroadcastReceiver.ActionType.ADD_IMAGE ->
                    shareImage(context, state, graphicsLayerWhole.toImageBitmap(), null, showDateRemoval, deeplink)

                BroadcastReceiver.ActionType.SHARE_PART -> {
                    editPart { PartOfConn.Empty(state.busName) }
                    show = true
                }

                else -> {}
            }
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
                        shareImage(context, state, graphicsLayerPart.toImageBitmap(), part.value, showDateRemoval, deeplink)
                    }
                },
                enabled = part.value.start != -1 && part.value.end != -1
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
                PartOfBusChooser(part.value, editPart, state.stops, Modifier.verticalScroll(rememberScrollState()))
            }
        }
    )

    DropdownMenu(
        expanded = dropdown,
        onDismissRequest = { dropdown = false }
    ) {
        DropdownMenuItem(
            text = { Text("Kopírovat odkaz") },
            onClick = { scope.launch { BroadcastReceiver.send(BroadcastReceiver.ActionType.COPY) } },
            trailingIcon = { Icon(Icons.Default.ContentCopy, null) }
        )
        if (state is BusState.OK) DropdownMenuItem(
            text = { Text("Sdílet s obrázkem") },
            onClick = { scope.launch { BroadcastReceiver.send(BroadcastReceiver.ActionType.ADD_IMAGE) } },
            trailingIcon = { Icon(Icons.Default.Image, null) }
        )
        if (state is BusState.OK) DropdownMenuItem(
            text = { Text("Sdílet obrázek části spoje") },
            onClick = { scope.launch { BroadcastReceiver.send(BroadcastReceiver.ActionType.SHARE_PART) } },
            trailingIcon = { Icon(Icons.Default.Timeline, null) }
        )
        if (showDateRemoval) DropdownMenuItem(
            text = { Text("Odstranit z odkazu datum") },
            onClick = { scope.launch { BroadcastReceiver.send(BroadcastReceiver.ActionType.REMOVE_DATE_FROM_BUS) } },
            trailingIcon = { Icon(Icons.Default.Today, null) }
        )
        DropdownMenuItem(
            text = { Text("Sdílet odkaz") },
            onClick = { share(context, state, showDateRemoval, deeplink) },
            trailingIcon = { Icon(Icons.Default.Share, null) }
        )
    }

    return BusShareManager {
        deeplink = state.deeplink
        showDateRemoval = true
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
            share(context, state, showDateRemoval, deeplink)
        else
            dropdown = true
    }
}

fun share(context: Context, state: BusState.Exists, showDateRemoval: Boolean, deeplink: String) {
    val isAtLeast34 = Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE
    val actions = if (isAtLeast34) buildList {
        if (showDateRemoval) this += ChooserAction.Builder(
            createWithResource(context, R.drawable.baseline_today_24),
            "Odstranit z odkazu datum",
            PendingIntent.getBroadcast(
                context,
                4,
                BroadcastReceiver.createIntent(context, BroadcastReceiver.ActionType.REMOVE_DATE_FROM_BUS),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            ),
        ).build()
        if (state is BusState.OK) this += ChooserAction.Builder(
            createWithResource(context, R.drawable.baseline_timeline_24),
            "Pouze část spoje",
            PendingIntent.getBroadcast(
                context,
                2,
                BroadcastReceiver.createIntent(context, BroadcastReceiver.ActionType.SHARE_PART),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            ),
        ).build()
        if (state is BusState.OK) this += ChooserAction.Builder(
            createWithResource(context, R.drawable.baseline_image_24),
            "Přidat obrázek",
            PendingIntent.getBroadcast(
                context,
                1,
                BroadcastReceiver.createIntent(context, BroadcastReceiver.ActionType.ADD_IMAGE),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            ),
        ).build()
        this += ChooserAction.Builder(
            createWithResource(context, R.drawable.baseline_content_copy_24),
            "Kopírovat",
            PendingIntent.getBroadcast(
                context,
                0,
                BroadcastReceiver.createIntent(context, BroadcastReceiver.ActionType.COPY),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            ),
        ).build()
    }.toTypedArray() else null

    context.startActivity(Intent.createChooser(Intent().apply {
        action = Intent.ACTION_SEND

        putExtra(Intent.EXTRA_TEXT, createDescription(state, null, deeplink))
        putExtra(Intent.EXTRA_TITLE, "Sdílet spoj")

        type = "text/plain"
        flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
    }, "Sdílet spoj").apply {
        if (isAtLeast34)
            putExtra(Intent.EXTRA_CHOOSER_CUSTOM_ACTIONS, actions!!)
    })
}

suspend fun shareImage(
    context: Context, state: BusState.Exists,
    bitmap: ImageBitmap, part: PartOfConn?,
    showDateRemoval: Boolean, deeplink: String,
) = withContext(Dispatchers.IO) {
    val d = context.filesDir
    val f = File(d, "spoj_${state.busName.value.replace('/', '_')}.png")
    f.createNewFile()

    f.outputStream().use { os ->
        bitmap.asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, os)
        os.flush()
    }

    val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", f)

    context.startActivity(Intent.createChooser(Intent().apply {
        action = Intent.ACTION_SEND

        putExtra(Intent.EXTRA_STREAM, uri)

        putExtra(Intent.EXTRA_TEXT, createDescription(state, part, deeplink))
        putExtra(Intent.EXTRA_TITLE, "Sdílet spoj")

        setDataAndType(uri, "image/png")
        flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
    }, "Sdílet spoj").apply {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE && showDateRemoval)
            putExtra(
                Intent.EXTRA_CHOOSER_CUSTOM_ACTIONS, arrayOf(
                    ChooserAction.Builder(
                        createWithResource(context, R.drawable.baseline_today_24),
                        "Odstranit z odkazu datum",
                        PendingIntent.getBroadcast(
                            context,
                            if (part == null) 6 else 7,
                            BroadcastReceiver.createIntent(
                                context,
                                if (part == null) BroadcastReceiver.ActionType.REMOVE_DATE_FROM_IMAGE else BroadcastReceiver.ActionType.REMOVE_DATE_FROM_IMAGE_PART
                            ),
                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                        ),
                    ).build()
                )
            )
    })
}

private fun createDescription(
    state: BusState.Exists,
    part: PartOfConn?,
    deeplink: String,
): String = if (state is BusState.OK) {
    val date = if ("/T/" in deeplink) "" else " (${state.date.day}. ${state.date.month.number}.)"
    val start = part?.let { state.stops[part.start] } ?: state.stops.first()
    val end = part?.let { state.stops[part.end] } ?: state.stops.last()
    "Linka ${state.lineNumber}$date: ${start.name} (${start.time}) -> ${end.name} (${end.time})\n$deeplink"
} else deeplink

suspend fun copy(clipboard: Clipboard, deeplink: String) {
    val description = ClipDescription("Odkaz na spoj", arrayOf("text/plain"))
    val item = ClipData.Item(deeplink)
    val clipData = ClipData(description, item)
    clipboard.setClipEntry(clipData.toClipEntry())
}