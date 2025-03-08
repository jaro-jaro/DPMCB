package cz.jaro.dpmcb.data.helperclasses

import cz.jaro.dpmcb.BuildConfig
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

actual val Dispatchers.IO: CoroutineDispatcher
    get() = IO
actual val isDebug: Boolean
    get() = BuildConfig.DEBUG