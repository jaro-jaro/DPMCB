package cz.jaro.dpmcb.ui.main

fun interface ShortcutCreator {
    fun createShortcut(includeDate: Boolean, label: String, state: MainState)
}