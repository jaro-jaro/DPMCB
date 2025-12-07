package cz.jaro.dpmcb.data.helperclasses

import cz.jaro.dpmcb.BuildConfig
import dev.gitlive.firebase.database.DatabaseReference
import dev.gitlive.firebase.database.android
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.android.awaitFrame
import kotlinx.coroutines.tasks.await
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

@Suppress("EXTENSION_SHADOWED_BY_MEMBER")
actual val Dispatchers.IO: CoroutineDispatcher
    get() = IO

actual val isDebug: Boolean
    get() = BuildConfig.DEBUG

actual suspend fun awaitFrame() { awaitFrame() }

actual fun String.encodeURL(): String = URLEncoder.encode(this, StandardCharsets.UTF_8.toString())

actual suspend inline fun <reified T> DatabaseReference.getValue(): T =
    android.get().await().value as T

actual const val maxDatabaseInsertBatchSize = Int.MAX_VALUE
actual const val backgroundInfo = "Prosíme, nevypínejte aplikaci."