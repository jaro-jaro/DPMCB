package cz.jaro.dpmcb.ui.common

import androidx.navigation.NavType
import androidx.savedstate.SavedState
import androidx.savedstate.read
import androidx.savedstate.write
import cz.jaro.dpmcb.data.helperclasses.fromJson
import cz.jaro.dpmcb.data.helperclasses.toJson
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.serialDescriptor
import kotlin.reflect.typeOf

inline fun <reified T : Enum<T>> enumTypePair() = typePair(
    parseValue = { enumValueOf<T>(it) },
    serializeAsValue = { it.name },
)

inline fun <reified T> serializationTypePair() = typePair(
    parseValue = { it.fromJson<T>() },
    serializeAsValue = { it.toJson<T>() },
    name = serialDescriptor<T>().serialName,
)

inline fun <reified T> stringSerializationTypePair() = typePair(
    parseValue = { it.run { "\"$this\"" }.fromJson<T>() },
    serializeAsValue = { it.toJson<T>().removeSurrounding("\"") },
    name = serialDescriptor<T>().serialName,
)

inline fun <reified T> serializationTypePair(serializer: KSerializer<T>) = typePair(
    parseValue = { it.fromJson(serializer) },
    serializeAsValue = { it.toJson(serializer) },
    name = serializer.descriptor.serialName,
)

inline fun <reified T> stringSerializationTypePair(serializer: KSerializer<T>) = typePair(
    parseValue = { it.run { "\"$this\"" }.fromJson(serializer) },
    serializeAsValue = { it.toJson(serializer).removeSurrounding("\"") },
    name = serializer.descriptor.serialName,
)

inline fun <reified T> typePair(
    crossinline parseValue: (String) -> T,
    crossinline serializeAsValue: (T) -> String,
    name: String? = null,
) = typeOf<T>() to NavType(parseValue, serializeAsValue, name)

inline fun <reified T> NavType(
    crossinline parseValue: (String) -> T,
    crossinline serializeAsValue: (T) -> String,
    name: String? = null,
) = object : NavType<T>(isNullableAllowed = true) {

    override fun get(bundle: SavedState, key: String) =
        bundle.read { getStringOrNull(key)?.let { parseValue(it) } }

    override fun put(bundle: SavedState, key: String, value: T) =
        bundle.write { putString(key, serializeAsValue(value)) }

    override fun parseValue(value: String) = parseValue(value)

    override fun serializeAsValue(value: T) = serializeAsValue(value)

    override val name: String = name ?: T::class.simpleName ?: ""
}

