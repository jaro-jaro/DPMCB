package cz.jaro.dpmcb.ui.now_running

sealed interface NowRunningResults<T> {

    val online: List<T>?
    val offlineNotOnline: List<T>?

    data class Lines(
        override val online: List<RunningLineInDirection>?,
        override val offlineNotOnline: List<RunningLineInDirection>?,
    ) : NowRunningResults<RunningLineInDirection> {
        companion object {
            operator fun invoke(online: List<RunningLineInDirection>?, offline: List<RunningLineInDirection>?) = Lines(
                online = online,
                offlineNotOnline = online.orEmpty().flatMap { it.buses.map { it.sequence } }.let { online ->
                    offline?.map { line -> line.copy(buses = line.buses.filterNot { it.sequence in online }) }
                        ?.filter { it.buses.isNotEmpty() }
                },
            )
        }
    }

    data class Delay(
        override val online: List<RunningDelayedBus>?,
        override val offlineNotOnline: List<RunningDelayedBus>?,
    ) : NowRunningResults<RunningDelayedBus> {
        companion object {
            operator fun invoke(online: List<RunningDelayedBus>?, offline: List<RunningDelayedBus>?) = Delay(
                online = online,
                offlineNotOnline = online.orEmpty().map { it.sequence }.let { online -> offline?.filterNot { it.sequence in online } },
            )
        }
    }

    data class RegN(
        override val online: List<RunningVehicle>?,
        override val offlineNotOnline: List<RunningVehicle>?,
    ) : NowRunningResults<RunningVehicle> {
        companion object {
            operator fun invoke(online: List<RunningVehicle>?, offline: List<RunningVehicle>?) = RegN(
                online = online,
                offlineNotOnline = online.orEmpty().map { it.sequence }.let { online -> offline?.filterNot { it.sequence in online } },
            )
        }
    }
}