@file:Suppress("MemberVisibilityCanBePrivate", "unused")

package cz.jaro.dpmcb.data.helperclasses

import androidx.navigation.NavHostController
import androidx.navigation.NavOptions
import com.google.firebase.Firebase
import com.google.firebase.crashlytics.crashlytics
import cz.jaro.dpmcb.ui.main.Route

inline val NavHostController.navigateToRouteFunction get() = { it: String -> this.navigate(it.work()) }
inline val NavHostController.navigateFunction: NavigateFunction
    get() {
        val f = navigateWithOptionsFunction
        return { route: Route ->
            f(route, null)
        }
    }
inline val NavHostController.navigateWithOptionsFunction: NavigateWithOptionsFunction
    get() {
        return navigate@{ route: Route, navOptions: NavOptions? ->
            try {
                this.navigate(route.work(), navOptions)
            } catch (e: IllegalStateException) {
                e.printStackTrace()
                Firebase.crashlytics.log("Pokus o navigaci na $route")
                Firebase.crashlytics.recordException(e)
            }
        }
    }

typealias NavigateFunction = (Route) -> Unit
typealias NavigateWithOptionsFunction = (Route, NavOptions?) -> Unit
typealias NavigateBackFunction<R> = (R) -> Unit

operator fun NavigateWithOptionsFunction.invoke(route: Route) = invoke(route, null)