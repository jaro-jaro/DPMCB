package cz.jaro.dpmcb.ui.bus

import android.app.PendingIntent
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.core.content.FileProvider
import cz.jaro.dpmcb.R
import cz.jaro.dpmcb.data.helperclasses.MutateFunction
import cz.jaro.dpmcb.data.helperclasses.unaryPlus
import cz.jaro.dpmcb.data.realtions.favourites.PartOfConn
import cz.jaro.dpmcb.ui.common.IconWithTooltip
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun busShareManager(
    state: BusState.Exists,
    graphicsLayerWhole: GraphicsLayer,
    graphicsLayerPart: GraphicsLayer,
    part: PartOfConn,
    editPart: MutateFunction<PartOfConn>,
): BusShareManager {
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

    return BusShareManager {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
            share(context, state)
        else
            dropdown = true
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
                BroadcastReceiver.createIntent(context, BroadcastReceiver.TYPE_COPY),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            ),
        ).build()
        if (state is BusState.OK) this += ChooserAction.Builder(
            createWithResource(context, R.drawable.baseline_image_24),
            "Přidat obrázek",
            PendingIntent.getBroadcast(
                context,
                1,
                BroadcastReceiver.createIntent(context, BroadcastReceiver.TYPE_ADD_IMAGE),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            ),
        ).build()
        if (state is BusState.OK) this += ChooserAction.Builder(
            createWithResource(context, R.drawable.baseline_timeline_24),
            "Pouze část spoje",
            PendingIntent.getBroadcast(
                context,
                2,
                BroadcastReceiver.createIntent(context, BroadcastReceiver.TYPE_SHARE_PART),
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

    val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", f)

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