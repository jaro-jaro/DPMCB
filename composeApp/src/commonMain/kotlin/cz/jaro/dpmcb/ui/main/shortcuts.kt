package cz.jaro.dpmcb.ui.main

import androidx.compose.runtime.Composable

fun interface ShortcutCreator {
    fun createShortcut(includeDate: Boolean, label: String, state: MainState)
}

expect fun supportsShortcuts(): Boolean

expect val shortcutCreator: ShortcutCreator
    @Composable get