package cz.jaro.dpmcb.data

import cz.jaro.dpmcb.data.helperclasses.asRepeatingStateFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow

private val coroutineScope = CoroutineScope(Dispatchers.IO)

fun interface UserOnlineManager {
    fun isOnline(): Boolean

    val isOnline: StateFlow<Boolean>
        get() = ({ isOnline() })
        .asRepeatingStateFlow(coroutineScope, started = SharingStarted.Companion.Lazily)
}