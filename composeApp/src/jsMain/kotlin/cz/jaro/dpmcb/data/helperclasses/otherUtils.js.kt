package cz.jaro.dpmcb.data.helperclasses

import cz.jaro.dpmcb.BuildKonfig
import io.github.z4kn4fein.semver.toVersion
import io.ktor.http.encodeURLPath
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

actual val Dispatchers.IO: CoroutineDispatcher
    get() = Main

actual val isDebug = BuildKonfig.versionName.toVersion(strict = false).isPreRelease

actual suspend fun awaitFrame() = Unit

actual fun String.encodeURL() = encodeURLPath()