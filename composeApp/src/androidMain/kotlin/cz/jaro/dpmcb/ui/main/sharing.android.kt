package cz.jaro.dpmcb.ui.main

import android.app.PendingIntent
import android.content.Intent
import android.graphics.drawable.Icon.createWithResource
import android.os.Build
import android.service.chooser.ChooserAction
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import cz.jaro.dpmcb.R
import cz.jaro.dpmcb.data.AppState
import cz.jaro.dpmcb.ui.bus.BroadcastReceiver

actual fun supportsSharing() = true

actual val screenShareManager: ScreenShareManager
    @Composable get() {
        var deeplink2 by rememberSaveable { mutableStateOf("") }

        val ctx = LocalContext.current

        LaunchedEffect(Unit) {
            for (e in BroadcastReceiver.clicked) when (e) {
                BroadcastReceiver.TYPE_REMOVE_DATE -> {
                    ctx.startActivity(Intent.createChooser(Intent().apply {
                        action = Intent.ACTION_SEND
                        putExtra(Intent.EXTRA_TEXT, deeplink2)
                        type = "text/uri-list"
                    }, "Sdílet"))
                }
            }
        }

        return ScreenShareManager { state ->
            val deeplink = "https://jaro-jaro.github.io/DPMCB/${AppState.route}"
            deeplink2 = "https://jaro-jaro.github.io/DPMCB/${AppState.route.replace(localDateTypePair.second.serializeAsValue(state.date), "T")}"
            ctx.startActivity(Intent.createChooser(Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, deeplink)
                type = "text/uri-list"
            }, "Sdílet").apply {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
                    putExtra(
                        Intent.EXTRA_CHOOSER_CUSTOM_ACTIONS, arrayOf(
                            ChooserAction.Builder(
                                createWithResource(ctx, R.drawable.baseline_today_24),
                                "Odstranit z odkazu datum",
                                PendingIntent.getBroadcast(
                                    ctx,
                                    5,
                                    BroadcastReceiver.createIntent(ctx, BroadcastReceiver.TYPE_REMOVE_DATE),
                                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                                ),
                            ).build()
                        )
                    )
            })
        }
    }