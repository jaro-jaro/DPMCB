package cz.jaro.dpmcb.ui.main

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import cz.jaro.dpmcb.data.Logger
import cz.jaro.dpmcb.data.helperclasses.fromJson
import cz.jaro.dpmcb.data.helperclasses.toJson

context(logger: Logger)
@Composable
fun rememberNavigator(navController: NavController) = remember(navController) { Navigator(navController) }

context(logger: Logger)
expect fun Navigator(
    navController: NavController,
): Navigator

interface Navigator {
    fun navigate(route: Route, replaceCurrentRoute: Boolean = false)
    fun navigateBackWithResult(value: String)
    fun getResult(): String?
    fun clearResult()
    fun navigateUp()
    fun getNavDestination(): NavDestination?
}

@Suppress("EXTENSION_SHADOWED_BY_MEMBER")
inline fun <reified T> Navigator.getResult() =
    getResult()?.fromJson<T>()
inline fun <reified T> Navigator.navigateBackWithResult(value: T) =
    navigateBackWithResult(value.toJson())

expect fun setAppTitle(title: String)