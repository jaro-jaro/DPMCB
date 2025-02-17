package cz.jaro.dpmcb.ui.timetable

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cz.jaro.dpmcb.data.SpojeRepository
import cz.jaro.dpmcb.data.entities.ShortLine
import cz.jaro.dpmcb.data.helperclasses.NavigateFunction
import cz.jaro.dpmcb.ui.main.Route
import cz.jaro.dpmcb.ui.main.Route.Favourites.date
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import org.koin.android.annotation.KoinViewModel
import org.koin.core.annotation.InjectedParam

@KoinViewModel
class TimetableViewModel(
    private val repo: SpojeRepository,
    @InjectedParam private val params: Parameters,
) : ViewModel() {

    data class Parameters(
        val lineNumber: ShortLine,
        val stop: String,
        val nextStop: String,
        val date: LocalDate,
        val navigate: NavigateFunction,
    )

    private val list = suspend {
        repo.timetable(params.lineNumber, params.stop, params.nextStop, date).sortedBy { it.departure }
    }.asFlow()

    val state = list.combine(repo.showLowFloor) { list, showLowFloor ->
        TimetableState.Success(
            data = list,
            date = date,
            lineNumber = params.lineNumber,
            stop = params.stop,
            nextStop = params.nextStop,
            showLowFloorFromLastTime = showLowFloor,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = TimetableState.Loading(
            date = date,
            lineNumber = params.lineNumber,
            stop = params.stop,
            nextStop = params.nextStop,
            showLowFloorFromLastTime = false,
        )
    )

    fun onEvent(e: TimetableEvent) = when(e) {
        is TimetableEvent.ChangeDate -> params.navigate(
            Route.Timetable(
                lineNumber = params.lineNumber,
                stop = params.stop,
                nextStop = params.nextStop,
                date = e.date,
            )
        )
        is TimetableEvent.EditShowLowFloor -> {
            viewModelScope.launch {
                repo.changeLowFloor(e.value)
            }
            Unit
        }
        is TimetableEvent.GoToBus -> params.navigate(
            Route.Bus(
                date = date,
                busName = e.bus,
            )
        )
    }
}
