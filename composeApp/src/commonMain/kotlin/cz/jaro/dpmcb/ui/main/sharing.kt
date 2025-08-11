package cz.jaro.dpmcb.ui.main

import androidx.compose.runtime.Composable

fun interface ScreenShareManager {
    fun shareScreen(state: MainState)
}

expect fun supportsSharing(): Boolean

expect val screenShareManager: ScreenShareManager
    @Composable get