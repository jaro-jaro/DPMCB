package cz.jaro.dpmcb.ui.common

import androidx.navigation.NavType
import androidx.savedstate.SavedState
import androidx.savedstate.read
import androidx.savedstate.write
import kotlinx.serialization.json.Json
import kotlin.reflect.typeOf

inline fun <reified T : Enum<T>> enumTypePair() = typePair(
    parseValue = { enumValueOf<T>(it) },
    serializeAsValue = { it.name },
)

inline fun <reified T> serializationTypePair() = typePair(
    parseValue = { Json.decodeFromString<T>(it) },
    serializeAsValue = { Json.encodeToString<T>(it) },
)

inline fun <reified T> typePair(
    crossinline parseValue: (String) -> T,
    crossinline serializeAsValue: (T) -> String,
) = typeOf<T>() to NavType(parseValue, serializeAsValue)

inline fun <reified T> NavType(
    crossinline parseValue: (String) -> T,
    crossinline serializeAsValue: (T) -> String,
) = object : NavType<T>(isNullableAllowed = true) {

    override fun get(bundle: SavedState, key: String) =
        bundle.read { getStringOrNull(key)?.let(::parseValue) }

    override fun put(bundle: SavedState, key: String, value: T) =
        bundle.write { putString(key, serializeAsValue(value)) }

    override fun parseValue(value: String) = parseValue(value)

    override fun serializeAsValue(value: T) = serializeAsValue(value)

    override val name: String = T::class.simpleName ?: ""
}

