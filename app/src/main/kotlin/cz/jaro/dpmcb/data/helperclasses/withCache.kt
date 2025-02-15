package cz.jaro.dpmcb.data.helperclasses


@JvmName("withCache1")
inline fun <I, O> withCache(crossinline function: (I) -> O): (I) -> O {
    val cache: MutableMap<I, O> = HashMap()
    return {
        cache.getOrPut(it, { function(it) })
    }
}

@JvmName("withCache0")
inline fun <O> withCache(crossinline function: () -> O): () -> O {
    var cache: O? = null
    return {
        cache ?: function().also { cache = it }
    }
}

@JvmName("withCacheSuspend1")
inline fun <I, O> withCache(crossinline function: suspend (I) -> O): suspend (I) -> O {
    val cache: MutableMap<I, O> = HashMap()
    return {
        cache.getOrPut(it, { function(it) })
    }
}

@JvmName("withCacheSuspend0")
inline fun <O> withCache(crossinline function: suspend () -> O): suspend () -> O {
    var cache: O? = null
    return {
        cache ?: function().also { cache = it }
    }
}