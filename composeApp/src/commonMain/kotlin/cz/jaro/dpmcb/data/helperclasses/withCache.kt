package cz.jaro.dpmcb.data.helperclasses

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async


inline fun <P, R> withCache(scope: CoroutineScope, crossinline function: suspend (P) -> R): (P) -> Deferred<R> {
    val cache: MutableMap<P, Deferred<R>> = HashMap()

    return {
        cache.getOrPut(it) { scope.async { function(it) } }
    }
}

inline fun <A, B, R> withCache(scope: CoroutineScope, crossinline function: suspend (A, B) -> R): (A, B) -> Deferred<R> {
    val cache: MutableMap<Pair<A, B>, Deferred<R>> = HashMap()

    return { a, b ->
        cache.getOrPut(a to b) { scope.async { function(a, b) } }
    }
}

inline fun <A, B, C, R> withCache(scope: CoroutineScope, crossinline function: suspend (A, B, C) -> R): (A, B, C) -> Deferred<R> {
    val cache: MutableMap<Triple<A, B, C>, Deferred<R>> = HashMap()

    return { a, b, c ->
        cache.getOrPut(Triple(a, b, c)) { scope.async { function(a, b, c) } }
    }
}