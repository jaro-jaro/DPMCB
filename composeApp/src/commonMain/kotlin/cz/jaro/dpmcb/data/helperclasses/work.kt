@file:Suppress("MemberVisibilityCanBePrivate", "unused", "NOTHING_TO_INLINE")
@file:OptIn(ExperimentalTime::class)

package cz.jaro.dpmcb.data.helperclasses

import kotlin.time.ExperimentalTime
import kotlin.time.measureTimedValue

fun <R> work(vararg msg: R?, tag: String? = null) = run { if (isDebug) println(msg.joinToString()) }
inline fun <reified T : Any?, reified R : Any?, reified S : Any?> T.work(vararg msg: R, tag: String? = null, transform: T.() -> S, ): T =
    also { cz.jaro.dpmcb.data.helperclasses.work(*msg, transform(), tag = tag) }

inline fun <reified T : Any?, reified R : Any?> T.work(vararg msg: R, tag: String? = null): T = also { work(*msg, tag = tag, transform = { this }) }
inline fun <reified T : Any?, reified S : Any?> T.work(tag: String? = null, transform: T.() -> S = { this as S }): T =
    also { work(*emptyArray<Any?>(), tag = tag, transform = transform) }

inline fun <reified T : Any?> T.work(tag: String? = null): T = also { work(*emptyArray<Any?>(), tag = tag, transform = { this }) }

inline fun <T, reified R : Any?> measure(vararg msg: R, tag: String? = null, block: () -> T) =
    measureTimedValue(block).work(*msg, tag = tag) { duration }.value

inline fun <T> measure(tag: String? = null, block: () -> T) =
    measureTimedValue(block).work(tag = tag) { duration }.value