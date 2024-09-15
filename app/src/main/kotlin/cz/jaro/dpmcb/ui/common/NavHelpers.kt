@file:OptIn(InternalSerializationApi::class, ExperimentalSerializationApi::class)
@file:SuppressLint("RestrictedApi")

package cz.jaro.dpmcb.ui.common

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.runtime.Composable
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDestination
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.navDeepLink
import androidx.navigation.serialization.decodeArguments
import androidx.navigation.serialization.generateRouteWithArgs
import androidx.navigation.toRoute
import com.google.firebase.Firebase
import com.google.firebase.crashlytics.crashlytics
import cz.jaro.dpmcb.ui.main.Route
import cz.jaro.dpmcb.ui.main.typeMap
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.SerializationException
import kotlinx.serialization.serializer
import kotlin.reflect.KClass

inline fun <reified T : Route> NavGraphBuilder.route(
    crossinline content: @Composable AnimatedVisibilityScope.(T) -> Unit,
) =
    composable<T>(
        typeMap = typeMap<T>(),
        deepLinks = listOf(
            navDeepLink<T>(
                basePath = "https://jaro-jaro.github.io/DPMCB/${T::class.serializer().descriptor.serialName}",
                typeMap = typeMap<T>(),
            )
        ),
    ) {
        val args = try {
            it.toRoute<T>()
        } catch (e: SerializationException) {
            e.printStackTrace()
            Firebase.crashlytics.recordException(e)
            null
        }
        if (args != null) content(args)
    }

fun NavBackStackEntry.generateRouteWithArgs() = route?.generateRouteWithArgs(destination)
fun NavBackStackEntry.getRoute() = route

@get:JvmName("getPrivateRoute")
private val NavBackStackEntry.route
    get() = try {
        Route.routes.find {
            it.serializer().descriptor.serialName == destination.route?.split("/", "?", limit = 2)?.first()
        }?.serializer()?.decodeArguments(arguments ?: Bundle(), destination.typeMap())
    } catch (e: SerializationException) {
        e.printStackTrace()
        Firebase.crashlytics.recordException(e)
        null
    }

@PublishedApi
internal fun NavDestination.typeMap() = arguments.mapValues { it.value.type }

inline fun <reified T : Route> T.generateRouteWithArgs(thisDestination: NavDestination) = generateRouteWithArgs(
    this,
    thisDestination.typeMap()
)