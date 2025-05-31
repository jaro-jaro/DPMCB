package cz.jaro.dpmcb.ui.main

import androidx.navigation.NavController
import androidx.navigation.NavDestination
import cz.jaro.dpmcb.ui.common.generateRouteWithArgs
import kotlinx.browser.window
import org.w3c.dom.url.URL

actual fun Navigator(
    navController: NavController,
): Navigator {
    window.onpopstate = {
        navController.navigate(window.location.hash.removePrefix("#"))
    }
    return object : Navigator {
        override fun navigate(route: Route, replaceCurrentRoute: Boolean) {
            navController.navigate(route)
            val destination = navController.currentDestination
            val path = route.generateRouteWithArgs(destination ?: return)
            val url = URL(window.location.protocol + window.location.host + "/$path")
            val pathWithoutSearch = url.pathname.removePrefix("/") + url.search
            if (replaceCurrentRoute)
                window.history.replaceState(null, "", "#$pathWithoutSearch")
            else
                window.history.pushState(null, "", "#$pathWithoutSearch")
        }

        override fun navigateUp() {
            window.history.back()
        }

        override fun <T> navigateBackWithResult(value: T) {
            window.history.back()
            window.history.replaceState(value, "", window.location.hash)
        }

        override fun <T> getResult(): T? {
            @Suppress("UNCHECKED_CAST")
            return window.history.state as? T?
        }

        override fun <T> clearResult() {
            window.history.replaceState(null, "", window.location.hash)
        }

        override fun getNavDestination(): NavDestination? {
            return navController.currentDestination
        }
    }
}

actual fun setAppTitle(title: String) {
    window.document.title = title
}