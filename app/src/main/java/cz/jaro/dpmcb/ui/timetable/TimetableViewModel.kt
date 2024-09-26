package cz.jaro.dpmcb.ui.timetable

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cz.jaro.dpmcb.data.OnlineRepository
import cz.jaro.dpmcb.data.SpojeRepository
import cz.jaro.dpmcb.data.entities.ShortLine
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.WhileSubscribed
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.koin.android.annotation.KoinViewModel
import org.koin.core.annotation.InjectedParam
import kotlin.time.Duration.Companion.seconds

@KoinViewModel
class TimetableViewModel(
    private val repo: SpojeRepository,
    onlineRepo: OnlineRepository,
    @InjectedParam params: Parameters,
) : ViewModel() {

    data class Parameters(
        val lineNumber: ShortLine,
        val stop: String,
        val nextStop: String,
    )

    val state = repo.date.map { datum ->
        TimetableState.Success(
            repo.timetable(params.lineNumber, params.stop, params.nextStop, datum).sortedBy { it.departure }
        )
    }.combine(onlineRepo.nowRunningBuses()) { tt, onlineConns ->
        tt.copy(
            data = tt.data.map { bus ->
                bus.copy(
                    delay = onlineConns.find { it.name == bus.busName }?.delayMin
                )
            }
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = TimetableState.Loading
    )

    val showLowFloorFromLastTime = repo.showLowFloor
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5.seconds), false)

    fun editShowLowFloor(value: Boolean) {
        viewModelScope.launch {
            repo.changeLowFloor(value)
        }
    }
}
