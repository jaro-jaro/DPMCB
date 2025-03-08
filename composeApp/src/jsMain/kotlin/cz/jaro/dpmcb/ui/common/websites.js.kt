package cz.jaro.dpmcb.ui.common

import kotlinx.browser.window

actual val openWebsiteLauncher: (String) -> Unit
    get() = { window.open(it, "_blank"); }