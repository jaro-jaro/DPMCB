package cz.jaro.dpmcb.ui.bus

import cz.jaro.dpmcb.data.realtions.favourites.PartOfConn
import cz.jaro.dpmcb.data.jikord.OnlineConnStop
import cz.jaro.dpmcb.data.realtions.BusStop
import java.time.LocalDate

sealed interface BusState {

    sealed interface Exists : BusState {
        val busName: String
        val timeCodes: List<String>
        val fixedCodes: List<String>
        val lineCode: String
        val deeplink: String
    }

    sealed interface OK : Exists {

        val stops: List<BusStop>
        val lineNumber: Int
        val lowFloor: Boolean
        val sequence: String?
        val sequenceName: String?
        val nextBus: Pair<String, Boolean>?
        val previousBus: Pair<String, Boolean>?
        val restriction: Boolean
        val traveledSegments: Int
        val lineHeight: Float
        val favourite: PartOfConn?
        val error: Boolean

    }

    data class Offline(
        override val busName: String,
        override val stops: List<BusStop>,
        override val lineNumber: Int,
        override val lowFloor: Boolean,
        override val timeCodes: List<String>,
        override val fixedCodes: List<String>,
        override val lineCode: String,
        override val sequence: String?,
        override val sequenceName: String?,
        override val nextBus: Pair<String, Boolean>?,
        override val previousBus: Pair<String, Boolean>?,
        override val deeplink: String,
        override val restriction: Boolean,
        override val traveledSegments: Int,
        override val lineHeight: Float,
        override val favourite: PartOfConn?,
        override val error: Boolean,
    ) : OK

    sealed interface Online : OK {
        val onlineConnStops: List<OnlineConnStop>
    }

    data class OnlineNotRunning(
        override val busName: String,
        override val stops: List<BusStop>,
        override val lineNumber: Int,
        override val lowFloor: Boolean,
        override val timeCodes: List<String>,
        override val fixedCodes: List<String>,
        override val lineCode: String,
        override val sequence: String?,
        override val sequenceName: String?,
        override val nextBus: Pair<String, Boolean>?,
        override val previousBus: Pair<String, Boolean>?,
        override val deeplink: String,
        override val restriction: Boolean,
        override val traveledSegments: Int,
        override val lineHeight: Float,
        override val favourite: PartOfConn?,
        override val error: Boolean,
        override val onlineConnStops: List<OnlineConnStop>,
    ) : Online

    data class OnlineRunning(
        override val busName: String,
        override val stops: List<BusStop>,
        override val lineNumber: Int,
        override val lowFloor: Boolean,
        override val timeCodes: List<String>,
        override val fixedCodes: List<String>,
        override val lineCode: String,
        override val sequence: String?,
        override val sequenceName: String?,
        override val nextBus: Pair<String, Boolean>?,
        override val previousBus: Pair<String, Boolean>?,
        override val deeplink: String,
        override val restriction: Boolean,
        override val traveledSegments: Int,
        override val lineHeight: Float,
        override val favourite: PartOfConn?,
        override val error: Boolean,
        override val onlineConnStops: List<OnlineConnStop>,
        val delayMin: Float,
        val vehicle: Int?,
        val confirmedLowFloor: Boolean?,
        val nextStopIndex: Int,
    ) : Online

    companion object {
        fun OnlineRunning(
            state: Offline,
            onlineConnStops: List<OnlineConnStop>,
            delayMin: Float,
            vehicle: Int?,
            confirmedLowFloor: Boolean?,
            nextStopIndex: Int,
        ) = with(state) {
            OnlineRunning(
                busName, stops, lineNumber, lowFloor, timeCodes, fixedCodes, lineCode, sequence, sequenceName, nextBus, previousBus,
                deeplink, restriction, traveledSegments, lineHeight, favourite, error, onlineConnStops, delayMin, vehicle, confirmedLowFloor, nextStopIndex
            )
        }

        fun OnlineNotRunning(
            state: Offline,
            onlineConnStops: List<OnlineConnStop>,
        ) = with(state) {
            OnlineNotRunning(
                busName, stops, lineNumber, lowFloor, timeCodes, fixedCodes, lineCode, sequence, sequenceName, nextBus, previousBus,
                deeplink, restriction, traveledSegments, lineHeight, favourite, error, onlineConnStops
            )
        }
    }

    data object Loading : BusState

    data class DoesNotExist(
        val busName: String,
    ) : BusState

    data class DoesNotRun(
        override val busName: String,
        val date: LocalDate,
        override val timeCodes: List<String>,
        override val fixedCodes: List<String>,
        override val lineCode: String,
        override val deeplink: String,
        val runsNextTimeAfterToday: LocalDate?,
        val runsNextTimeAfterDate: LocalDate?,
    ) : Exists
}