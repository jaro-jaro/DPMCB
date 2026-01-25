package cz.jaro.dpmcb.ui.main

import androidx.lifecycle.Lifecycle
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import cz.jaro.dpmcb.data.Logger

context(logger: Logger)
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

    override fun navigateBackWithResult(value: String) {
        navController.navigateUp()
        navController.currentBackStackEntry?.savedStateHandle?.set("result", value)
    }

    override fun getResult(): String? {
        return navController.currentBackStackEntry?.savedStateHandle?.get<String>("result")
    }

    override fun clearResult() {
        if (navController.currentBackStackEntry?.lifecycle?.currentState?.isAtLeast(Lifecycle.State.CREATED) == true)
            navController.currentBackStackEntry?.savedStateHandle?.remove<String>("result")
    }

    override fun navigateUp() {
        navController.navigateUp()
    }

    override fun getNavDestination(): NavDestination? {
        return navController.currentDestination
    }
}

actual fun setAppTitle(title: String) {}