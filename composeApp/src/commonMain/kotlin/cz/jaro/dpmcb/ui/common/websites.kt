package cz.jaro.dpmcb.ui.common

import androidx.compose.runtime.Composable

expect val openWebsiteLauncher: (url: String) -> Unit
    @Composable get

expect suspend fun fetch(url: String, progress: (Float?) -> Unit): String