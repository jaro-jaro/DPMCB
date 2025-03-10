package cz.jaro.dpmcb.data.helperclasses

import cz.jaro.dpmcb.BuildConfig
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.android.awaitFrame
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

actual val Dispatchers.IO: CoroutineDispatcher
    get() = IO

actual val isDebug: Boolean
    get() = BuildConfig.DEBUG

actual suspend fun awaitFrame() { awaitFrame() }

actual fun String.encodeURL(): String = URLEncoder.encode(this, StandardCharsets.UTF_8.toString())

