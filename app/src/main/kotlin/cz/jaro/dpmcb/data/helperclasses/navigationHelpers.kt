@file:Suppress("MemberVisibilityCanBePrivate", "unused")

package cz.jaro.dpmcb.data.helperclasses

import androidx.navigation.NavHostController
import androidx.navigation.NavOptions
import androidx.navigation.navOptions
import cz.jaro.dpmcb.data.log
import cz.jaro.dpmcb.data.recordException
import cz.jaro.dpmcb.ui.main.Route
import cz.jaro.dpmcb.ui.main.SuperRoute

inline val NavHostController.navigateToRouteFunction get() = { it: String -> this.navigate(it.work()) }
inline val NavHostController.superNavigateFunction: SuperNavigateFunction
    get() = { route: SuperRoute, navOptions: NavOptions? ->
        navigate(route, navOptions)
    }
inline val NavHostController.navigateFunction: NavigateFunction
    get() = navigateWithOptionsFunction.let { f ->
        { route: Route ->
            f(route, null)
        }
    }
inline val NavHostController.navigateWithOptionsFunction: NavigateWithOptionsFunction
    get() = navigate@{ route: Route, navOptions: NavOptions? ->
        try {
            this.navigate(route.work(), navOptions)
        } catch (e: IllegalStateException) {
            e.printStackTrace()
            log("Pokus o navigaci na $route")
            recordException(e)
        }
    }

typealias NavigateFunction = (Route) -> Unit
typealias SuperNavigateFunction = (SuperRoute, NavOptions?) -> Unit
typealias NavigateWithOptionsFunction = (Route, NavOptions?) -> Unit
typealias NavigateBackFunction<R> = (R) -> Unit

operator fun <T> ((T, NavOptions?) -> Unit).invoke(route: T) = invoke(route, null)

inline fun <reified T : Any> popUpTo() = navOptions { popUpTo<T> { inclusive = true } }