package cz.jaro.dpmcb.ui.main

import android.annotation.SuppressLint
import android.os.Bundle
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
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.serializer

@OptIn(InternalSerializationApi::class, ExperimentalSerializationApi::class)
inline fun <reified T : Route> NavGraphBuilder.route(
    crossinline content: @Composable (T) -> Unit,
) =
    composable<T>(
        typeMap = typeMap<T>(),
        deepLinks = listOf(
            navDeepLink<T>(
                basePath = "https://jaro-jaro.github.io/DPMCB/${T::class.serializer().descriptor.serialName}",
                typeMap = typeMap<T>().also {
                    Firebase.crashlytics.log("Type map: $it")
                },
            )
        ),
    ) {
        val args = it.toRoute<T>()
        content(args)
    }

@SuppressLint("RestrictedApi")
fun NavBackStackEntry.generateRouteWithArgs() = route?.generateRouteWithArgs(destination)

@OptIn(ExperimentalSerializationApi::class, InternalSerializationApi::class)
private val NavBackStackEntry.route
    @SuppressLint("RestrictedApi")
    get() = Route.routes.find {
        it.serializer().descriptor.serialName == destination.route?.split("/", "?", limit = 2)?.first()
    }?.serializer()?.decodeArguments(arguments ?: Bundle(), destination.typeMap())

@PublishedApi
internal fun NavDestination.typeMap() = arguments.mapValues { it.value.type }

@SuppressLint("RestrictedApi")
inline fun <reified T : Route> T.generateRouteWithArgs(thisDestination: NavDestination) = generateRouteWithArgs(
    thisDestination.typeMap()
)