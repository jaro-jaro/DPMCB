package cz.jaro.dpmcb.ui.common

import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri

val openWebsiteLauncher: (url: String) -> Unit
    @Composable
    get() {
        val intent =
            CustomTabsIntent.Builder()
                .setShowTitle(true)
                .build()
        val ctx = LocalContext.current
        return {
            intent.launchUrl(ctx, it.toUri())
        }
    }