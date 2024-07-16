package cz.jaro.dpmcb.ui.now_running

import cz.jaro.dpmcb.data.entities.ShortLine

sealed interface NowRunningState {

    data object IsNotToday : NowRunningState

    data object Offline : NowRunningState

    data class LoadingLines(
        override val type: NowRunningType,
    ) : HasType

    data object NoLines : NowRunningState

    sealed interface HasFilters : HasType {
        val lineNumbers: List<ShortLine>
        val filters: List<ShortLine>
    }

    sealed interface HasType : NowRunningState {
        val type: NowRunningType
    }

    sealed interface HasNotRunning : NowRunningState {
        val nowNotRunning: NowRunningResults<*>
    }

    data class Loading(
        override val lineNumbers: List<ShortLine>,
        override val filters: List<ShortLine>,
        override val type: NowRunningType,
    ) : HasFilters, HasType

    data class NothingRunsToday(
        override val lineNumbers: List<ShortLine>,
        override val filters: List<ShortLine>,
        override val type: NowRunningType,
    ) : HasFilters, HasType

    data class NothingRunningNow(
        override val lineNumbers: List<ShortLine>,
        override val filters: List<ShortLine>,
        override val type: NowRunningType,
        override val nowNotRunning: NowRunningResults<*>,
    ) : HasFilters, HasType, HasNotRunning

    data class OK(
        override val lineNumbers: List<ShortLine>,
        override val filters: List<ShortLine>,
        override val type: NowRunningType,
        override val nowNotRunning: NowRunningResults<*>,
        val result: NowRunningResults<*>,
    ) : HasFilters, HasType, HasNotRunning
}