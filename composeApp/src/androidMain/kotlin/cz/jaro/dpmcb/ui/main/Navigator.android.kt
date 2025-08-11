package cz.jaro.dpmcb.ui.main

import androidx.lifecycle.Lifecycle
import androidx.navigation.NavController
import androidx.navigation.NavDestination

actual fun Navigator(
    navController: NavController,
) = object : Navigator {
    override fun navigate(route: Route, replaceCurrentRoute: Boolean) {
        navController.navigate(route) {
            if (replaceCurrentRoute)
                popUpTo(route = navController.currentDestination?.route ?: "") {
                    inclusive = true
                }
        }
    }

    override fun <T> navigateBackWithResult(value: T) {
        navController.navigateUp()
        navController.currentBackStackEntry?.savedStateHandle?.set("result", value)
    }

    override fun <T> getResult(): T? {
        return navController.currentBackStackEntry?.savedStateHandle?.get<T>("result")
    }

    override fun <T> clearResult() {
        if (navController.currentBackStackEntry?.lifecycle?.currentState?.isAtLeast(Lifecycle.State.CREATED) == true)
            navController.currentBackStackEntry?.savedStateHandle?.remove<T>("result")
    }

    override fun navigateUp() {
        navController.navigateUp()
    }

    override fun getNavDestination(): NavDestination? {
        return navController.currentDestination
    }
}

actual fun setAppTitle(title: String) {}