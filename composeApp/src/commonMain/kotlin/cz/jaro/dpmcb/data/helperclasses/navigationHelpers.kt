@file:Suppress("MemberVisibilityCanBePrivate", "unused")

package cz.jaro.dpmcb.data.helperclasses

import androidx.navigation.NavHostController
import androidx.navigation.NavOptions
import androidx.navigation.navOptions
import cz.jaro.dpmcb.ui.main.SuperRoute

inline val NavHostController.navigateToRouteFunction get() = { it: String -> this.navigate(it.work()) }
inline val NavHostController.superNavigateFunction: SuperNavigateFunction
    get() = { route: SuperRoute, navOptions: NavOptions? ->
        navigate(route, navOptions)
    }

typealias SuperNavigateFunction = (SuperRoute, NavOptions?) -> Unit

operator fun <T> ((T, NavOptions?) -> Unit).invoke(route: T) = invoke(route, null)

inline fun <reified T : Any> popUpTo() = navOptions { popUpTo<T> { inclusive = true } }