package cz.jaro.dpmcb.ui.main

import androidx.compose.runtime.Composable

actual fun supportsSharing() = false

actual val screenShareManager: ScreenShareManager
    @Composable
    get() = error("Not supported")