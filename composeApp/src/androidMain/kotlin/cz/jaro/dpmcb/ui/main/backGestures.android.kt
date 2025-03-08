package cz.jaro.dpmcb.ui.main

import androidx.navigation.NavHostController

actual fun NavHostController.enableOnBackPressed(enabled: Boolean) =
    enableOnBackPressed(enabled)