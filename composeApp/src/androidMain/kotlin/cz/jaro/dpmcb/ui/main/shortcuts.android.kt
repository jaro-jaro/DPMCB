package cz.jaro.dpmcb.ui.main

import android.content.Intent
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.drawable.Icon.createWithResource
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import cz.jaro.dpmcb.BuildConfig
import cz.jaro.dpmcb.R
import cz.jaro.dpmcb.data.AppState
import cz.jaro.dpmcb.data.AppState.APP_URL

actual fun supportsShortcuts() = true

actual val shortcutCreator: ShortcutCreator
    @Composable
    get() {
        val ctx = LocalContext.current
        val shortcutManager = ctx.getSystemService(ShortcutManager::class.java)!!
        return ShortcutCreator { includeDate: Boolean, label: String, state: MainState ->
            if (shortcutManager.isRequestPinShortcutSupported) {

                val route = if (includeDate) AppState.route else AppState.route.replace(localDateTypePair.second.serializeAsValue(state.date), "T")

                val pinShortcutInfo = ShortcutInfo
                    .Builder(ctx, "$route-$label")
                    .setShortLabel(label)
                    .setLongLabel(label)
                    .setIcon(
                        createWithResource(
                            ctx, if (BuildConfig.DEBUG) R.mipmap.logo_jaro else R.mipmap.logo_chytra_cesta
                        )
                    )
                    .setIntent(Intent(Intent.ACTION_VIEW, "${APP_URL}$route".toUri()))
                    .build()

                shortcutManager.requestPinShortcut(pinShortcutInfo, null)
            }
        }
    }