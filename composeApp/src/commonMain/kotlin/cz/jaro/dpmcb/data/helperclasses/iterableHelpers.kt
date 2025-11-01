@file:Suppress("MemberVisibilityCanBePrivate", "unused")

package cz.jaro.dpmcb.data.helperclasses

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.supervisorScope

inline fun <T> List<T>.reversedIf(predicate: (List<T>) -> Boolean): List<T> = if (predicate(this)) this.reversed() else this

inline fun <T, K1, K2> Iterable<T>.groupByPair(keySelector: (T) -> Pair<K1, K2>): List<Triple<K1, K2, List<T>>> =
    groupBy(keySelector)
        .map { Triple(it.key.first, it.key.second, it.value) }

inline fun <T, K1, K2, V> Iterable<T>.groupByPair(keySelector: (T) -> Pair<K1, K2>, valueTransform: (T) -> V) =
    groupBy(keySelector, valueTransform)
        .map { Triple(it.key.first, it.key.second, it.value) }

fun Iterable<Boolean>.allTrue() = all { it }
fun Iterable<Boolean>.anyTrue() = any { it }

fun <T, U: Comparable<T>> List<U>.sorted() = sortedWith(compareBy { it })

inline fun <T> Iterable<T>.indexOfFirstOrNull(predicate: (T) -> Boolean): Int? {
    for ((index, item) in this.withIndex()) {
        if (predicate(item)) return index
    }
    return null
}

typealias MutateListFunction<T> = (MutateListLambda<T>) -> Unit
typealias MutateListLambda<T> = MutableList<T>.() -> Unit

typealias MutateFunction<T> = (MutateLambda<T>) -> Unit
typealias MutateLambda<T> = (T) -> T

fun <T> T.nullable(): T? = this

suspend inline fun <T, R> Iterable<T>.asyncMap(crossinline transform: suspend (T) -> R): List<R> = supervisorScope {
    map {
        async { transform(it) }
    }.awaitAll()
}

fun <K, V> Collection<Map.Entry<K, V>>.toMap(): Map<K, V> = toMap(LinkedHashMap(size))

fun <K, V, M : MutableMap<in K, in V>> Iterable<Map.Entry<K, V>>.toMap(destination: M): M {
    for (element in this) {
        destination[element.key] = element.value
    }
    return destination
}

inline fun <K, V> Map<K, V>.mutate(block: MutableMap<K, V>.() -> Unit): Map<K, V> =
    (this as? MutableMap<K, V> ?: toMutableMap()).apply(block)
inline fun <T> Iterable<T>.mutate(block: MutableList<T>.() -> Unit): List<T> =
    (this as? MutableList<T> ?: toMutableList()).apply(block)

fun <K, V> Map<K, V>.with(key: K, mutateValue: (V?) -> V): Map<K, V> = mutate {
    this[key] = mutateValue(this[key])
}
fun <T> Iterable<T>.with(index: Int, mutateValue: (T) -> T): List<T> = mutate {
    this[index] = mutateValue(this[index])
}

fun <T> Iterable<T>.with(index: Int, value: T) = with(index) { value }
fun <K, V> Map<K, V>.with(key: K, value: V) = with(key) { value }

fun <T> Iterable<T>.countMembers(): Map<T, Int> = fold(mapOf()) { result, element ->
    result.with(element, result.getOrElse(element) { 0 } + 1)
}
