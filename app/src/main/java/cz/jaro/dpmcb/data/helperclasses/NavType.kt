package cz.jaro.dpmcb.data.helperclasses

import android.os.Bundle
import androidx.navigation.NavType
import kotlinx.serialization.encodeToString
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

    override fun get(bundle: Bundle, key: String): T? =
        bundle.getString(key)?.let(::parseValue)

    override fun put(bundle: Bundle, key: String, value: T) =
        bundle.putString(key, serializeAsValue(value))

    override fun parseValue(value: String) = parseValue(value)

    override fun serializeAsValue(value: T) = serializeAsValue(value)

    override val name: String = T::class.simpleName ?: ""
}

