package cz.jaro.dpmcb.ui.main

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigation.NavController
import androidx.navigation.NavDestination

@Composable
fun rememberNavigator(navController: NavController) = remember(navController) { Navigator(navController) }

expect fun Navigator(
    navController: NavController,
): Navigator

interface Navigator {
    fun navigate(route: Route, replaceCurrentRoute: Boolean = false)
    fun <T> navigateBackWithResult(value: T)
    fun <T> getResult(): T?
    fun <T> clearResult()
    fun navigateUp()
    fun getNavDestination(): NavDestination?
}

expect fun setAppTitle(title: String)