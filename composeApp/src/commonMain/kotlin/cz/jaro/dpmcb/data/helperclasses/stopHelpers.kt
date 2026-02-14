package cz.jaro.dpmcb.data.helperclasses

import cz.jaro.dpmcb.data.SpojeRepository
import cz.jaro.dpmcb.data.entities.LongLine
import cz.jaro.dpmcb.data.entities.StopName
import cz.jaro.dpmcb.data.realtions.MiddleStop
import kotlin.math.roundToInt

fun middleDestination(
    isOneWay: Boolean,
    stops: List<StopName>,
    thisStopIndex: Int,
): StopName? {
    val middleStop = if (isOneWay) findMiddleStop(stops) else null
    return if (middleStop != null && thisStopIndex < (middleStop.index - 1))
        middleStop.name
    else null
}

fun findMiddleStop(stops: List<StopName>): MiddleStop? {
    val wi = stops.withIndex().toList()

    val lastCommonStop = wi.indexOfLast { (i, name) ->
        i < wi.indexOfLast { it.value == name }
    }

    val firstReCommonStop = wi.indexOfFirst { (i, name) ->
        wi.indexOfFirst { it.value == name } < i
    }

    if (firstReCommonStop == -1 || lastCommonStop == -1) return null

    val last = wi[(lastCommonStop + firstReCommonStop).div(2F).roundToInt()]
    return MiddleStop(
        last.value,
        last.index,
    )
}

suspend fun SpojeRepository.middleDestination(
    line: LongLine,
    stops: List<StopName>,
    thisStopIndex: Int,
) = middleDestination(isOneWay(line), stops, thisStopIndex)