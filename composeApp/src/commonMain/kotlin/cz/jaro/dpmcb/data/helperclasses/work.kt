@file:Suppress("MemberVisibilityCanBePrivate", "unused", "NOTHING_TO_INLINE")
@file:OptIn(ExperimentalTime::class)

package cz.jaro.dpmcb.data.helperclasses

import kotlin.time.ExperimentalTime
import kotlin.time.Instant

fun <R> work(vararg msg: R?) = run { if (isDebug) println(msg.joinToString()) }
inline fun <reified T : Any?, reified R : Any?, reified S : Any?> T.work(vararg msg: R, transform: T.() -> S): T =
    also { cz.jaro.dpmcb.data.helperclasses.work(*msg, transform()) }

inline fun <reified T : Any?, reified R : Any?> T.work(vararg msg: R): T = also { work(*msg, transform = { this }) }
inline fun <reified T : Any?, reified S : Any?> T.work(transform: T.() -> S = { this as S }): T =
    also { work(*emptyArray<Any?>(), transform = transform) }

inline fun <reified T : Any?> T.work(): T = also { work(*emptyArray<Any?>(), transform = { this }) }

var last: Instant? = null

inline fun <T : Any?, R> T.timestamp(vararg msg: R?): T = also {
    cz.jaro.dpmcb.data.helperclasses.timestamp(*msg)
}

fun <R> timestamp(vararg msg: R?) {
    val now = SystemClock.now()
    now.work(*msg, last?.let { now - it })
    last = now
}
