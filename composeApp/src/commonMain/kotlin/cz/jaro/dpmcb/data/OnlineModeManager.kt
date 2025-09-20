package cz.jaro.dpmcb.data

import cz.jaro.dpmcb.data.helperclasses.IO
import cz.jaro.dpmcb.data.helperclasses.combineStates
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.update

class OnlineModeManager(
    localSettingsDataSource: LocalSettingsDataSource,
    onlineManager: UserOnlineManager,
) : LocalSettingsDataSource by localSettingsDataSource, UserOnlineManager by onlineManager {
    private val scope = CoroutineScope(Dispatchers.IO)

    private val _onlineMode = MutableStateFlow(Settings().autoOnline)
    val isOnlineModeEnabled = _onlineMode.asStateFlow()

    val hasAccessToMap = combineStates(
        scope, isOnline, isOnlineModeEnabled,
    ) { isOnline, onlineMode ->
        isOnline && onlineMode
    }

    fun editOnlineMode(mode: Boolean) {
        _onlineMode.update { mode }
    }

    init {
        settings.take(1).onEach { settings ->
            _onlineMode.value = settings.autoOnline
        }.launchIn(scope)
    }
}
