package cz.jaro.dpmcb.data

import cz.jaro.dpmcb.data.helperclasses.IO
import cz.jaro.dpmcb.data.helperclasses.combineStates
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.update

class OnlineModeManager(
    localSettingsDataSource: LocalSettingsDataSource,
    onlineManager: UserOnlineManager,
) : LocalSettingsDataSource by localSettingsDataSource, UserOnlineManager by onlineManager {
    private val scope = CoroutineScope(Dispatchers.IO)

    val isOnlineModeEnabled: StateFlow<Boolean>
        field = MutableStateFlow(Settings().autoOnline)

    val hasAccessToMap = combineStates(
        scope, isOnline, isOnlineModeEnabled,
    ) { isOnline, onlineMode ->
        isOnline && onlineMode
    }

    fun editOnlineMode(mode: Boolean) {
        isOnlineModeEnabled.update { mode }
    }

    init {
        settings.take(1).onEach { settings ->
            isOnlineModeEnabled.value = settings.autoOnline
        }.launchIn(scope)
    }
}
