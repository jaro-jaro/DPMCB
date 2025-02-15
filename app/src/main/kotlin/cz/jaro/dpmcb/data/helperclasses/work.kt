@file:Suppress("MemberVisibilityCanBePrivate", "unused")

package cz.jaro.dpmcb.data.helperclasses

import android.util.Log
import cz.jaro.dpmcb.BuildConfig

fun <R> work(vararg msg: R?) = run { if (BuildConfig.DEBUG) Log.d("funguj", msg.joinToString()) }
inline fun <reified T : Any?, reified R : Any?, reified S : Any?> T.work(vararg msg: R, transform: T.() -> S): T =
    also { cz.jaro.dpmcb.data.helperclasses.work(transform(), *msg) }

inline fun <reified T : Any?, reified R : Any?> T.work(vararg msg: R): T = also { work(*msg, transform = { this }) }
inline fun <reified T : Any?, reified S : Any?> T.work(transform: T.() -> S = { this as S }): T =
    also { work(*emptyArray<Any?>(), transform = transform) }

inline fun <reified T : Any?> T.work(): T = also { work(*emptyArray<Any?>(), transform = { this }) }
