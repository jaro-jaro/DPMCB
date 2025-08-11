package cz.jaro.dpmcb.ui.main

import androidx.compose.runtime.Composable

actual fun supportsShortcuts() = false

actual val shortcutCreator: ShortcutCreator
    @Composable
    get() = error("Not supported")