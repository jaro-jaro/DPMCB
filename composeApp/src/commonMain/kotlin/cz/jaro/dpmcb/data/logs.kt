package cz.jaro.dpmcb.data

import kotlin.time.measureTimedValue

interface Logger : DebugManager {
    fun recordException(throwable: Throwable)
    fun logToCrashlytics(message: Any?)
    fun log(tag: String?, message: String)

    fun work(vararg msg: Any?, tag: String? = null) = run { if (isDebug()) println(msg.joinToString()) }
}

@IgnorableReturnValue
context(logger: Logger)
inline fun <reified T, reified S> T.work(vararg msg: Any?, tag: String? = null, transform: T.() -> S): T =
    also { logger.work(*msg, transform(), tag = tag) }

@IgnorableReturnValue
context(logger: Logger)
inline fun <reified T> T.work(vararg msg: Any?, tag: String? = null): T =
    also { work(*msg, tag = tag, transform = { this }) }

@IgnorableReturnValue
context(logger: Logger)
inline fun <reified T> measure(vararg msg: Any?, tag: String? = null, block: () -> T) =
    measureTimedValue(block).work(*msg, tag = tag) { duration }.value
