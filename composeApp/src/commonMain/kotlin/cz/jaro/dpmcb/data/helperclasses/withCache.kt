package cz.jaro.dpmcb.data.helperclasses


inline fun <P, R> withCache(crossinline function: suspend (P) -> R): suspend (P) -> R {
    val cache: MutableMap<P, R> = HashMap()

    return {
        cache.getOrPut(it) { function(it) }
    }
}

inline fun <A, B, R> withCache(crossinline function: suspend (A, B) -> R): suspend (A, B) -> R {
    val cache: MutableMap<Pair<A, B>, R> = HashMap()

    return { a, b ->
        cache.getOrPut(a to b) { function(a, b) }
    }
}

inline fun <A, B, C, R> withCache(crossinline function: suspend (A, B, C) -> R): suspend (A, B, C) -> R {
    val cache: MutableMap<Triple<A, B, C>, R> = HashMap()

    return { a, b, c ->
        cache.getOrPut(Triple(a, b, c)) { function(a, b, c) }
    }
}