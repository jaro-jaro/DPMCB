package cz.jaro.dpmcb.ui.now_running

import cz.jaro.dpmcb.data.entities.ShortLine

sealed interface NowRunningState {
    val type: NowRunningType

    data class LoadingLines(
        override val type: NowRunningType,
    ) : NowRunningState

    data object NoLines : NowRunningState {
        override val type: NowRunningType = NowRunningType.Line
    }

    sealed interface LinesLoaded : NowRunningState {
        val lineNumbers: List<ShortLine>
        val filters: List<ShortLine>
    }

    data class Loading(
        override val lineNumbers: List<ShortLine>,
        override val filters: List<ShortLine>,
        override val type: NowRunningType,
    ) : LinesLoaded

    data class OK(
        override val lineNumbers: List<ShortLine>,
        override val filters: List<ShortLine>,
        override val type: NowRunningType,
        val result: NowRunningResults<*>,
        val isOnline: Boolean,
    ) : LinesLoaded
}