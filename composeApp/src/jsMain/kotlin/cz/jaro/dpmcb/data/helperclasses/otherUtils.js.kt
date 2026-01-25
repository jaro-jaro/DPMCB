package cz.jaro.dpmcb.data.helperclasses

import cz.jaro.dpmcb.data.Logger
import cz.jaro.dpmcb.data.work
import dev.gitlive.firebase.database.DatabaseReference
import dev.gitlive.firebase.database.js
import io.ktor.http.encodeURLPath
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.await

actual val Dispatchers.IO: CoroutineDispatcher
    get() = Main

actual suspend fun awaitFrame() = Unit

actual fun String.encodeURL() = encodeURLPath()

context(logger: Logger)
@OptIn(ExperimentalWasmJsInterop::class)
actual suspend inline fun <reified T> DatabaseReference.getValue(): T =
    dev.gitlive.firebase.database.externals.get(js).work().await().also { Unit.work(it) }.`val`().work() as T

actual const val maxDatabaseInsertBatchSize = 1_000
actual val backgroundInfo = "Proces můžete nechat běžet na pozadí, ale prosíme, nezavírejte tuto kartu."