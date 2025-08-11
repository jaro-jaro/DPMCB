package cz.jaro.dpmcb.ui.main

import androidx.navigation.NavHostController

@Suppress("EXTENSION_SHADOWED_BY_MEMBER")
actual fun NavHostController.enableOnBackPressed(enabled: Boolean) =
    enableOnBackPressed(enabled)