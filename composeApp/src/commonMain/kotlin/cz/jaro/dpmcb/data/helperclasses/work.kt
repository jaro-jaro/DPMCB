@file:Suppress("MemberVisibilityCanBePrivate", "unused", "NOTHING_TO_INLINE")
@file:OptIn(ExperimentalTime::class)

package cz.jaro.dpmcb.data.helperclasses

import kotlin.time.ExperimentalTime
import kotlin.time.measureTimedValue

fun work(vararg msg: Any?, tag: String? = null) = run { if (isDebug) println(msg.joinToString()) }
inline fun <reified T : Any?, reified S : Any?> T.work(vararg msg: Any?, tag: String? = null, transform: T.() -> S, ): T =
    also { cz.jaro.dpmcb.data.helperclasses.work(*msg, transform(), tag = tag) }

inline fun <reified T : Any?> T.work(vararg msg: Any?, tag: String? = null): T = also { work(*msg, tag = tag, transform = { this }) }

inline fun <T> measure(vararg msg: Any?, tag: String? = null, block: () -> T) =
    measureTimedValue(block).work(*msg, tag = tag) { duration }.value