package cz.jaro.dpmcb.data.helperclasses

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

actual val Dispatchers.IO: CoroutineDispatcher
    get() = Main