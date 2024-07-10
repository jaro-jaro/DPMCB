package cz.jaro.dpmcb.ui.bus

import cz.jaro.dpmcb.data.entities.BusName
import cz.jaro.dpmcb.data.entities.RegistrationNumber
import cz.jaro.dpmcb.data.entities.SequenceCode
import cz.jaro.dpmcb.data.entities.ShortLine
import cz.jaro.dpmcb.data.jikord.OnlineConnStop
import cz.jaro.dpmcb.data.realtions.BusStop
import cz.jaro.dpmcb.data.realtions.favourites.PartOfConn
import kotlinx.datetime.LocalDate

sealed interface BusState {

    sealed interface Exists : BusState {
        val busName: BusName
        val timeCodes: List<String>
        val fixedCodes: List<String>
        val lineCode: String
        val deeplink: String
    }

    sealed interface OK : Exists {

        val stops: List<BusStop>
        val lineNumber: ShortLine
        val lowFloor: Boolean
        val sequence: SequenceCode?
        val sequenceName: String?
        val nextBus: Pair<BusName, Boolean>?
        val previousBus: Pair<BusName, Boolean>?
        val restriction: Boolean
        val traveledSegments: Int
        val lineHeight: Float
        val favourite: PartOfConn?
        val error: Boolean

    }

    data class Offline(
        override val busName: BusName,
        override val stops: List<BusStop>,
        override val lineNumber: ShortLine,
        override val lowFloor: Boolean,
        override val timeCodes: List<String>,
        override val fixedCodes: List<String>,
        override val lineCode: String,
        override val sequence: SequenceCode?,
        override val sequenceName: String?,
        override val nextBus: Pair<BusName, Boolean>?,
        override val previousBus: Pair<BusName, Boolean>?,
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
        override val busName: BusName,
        override val stops: List<BusStop>,
        override val lineNumber: ShortLine,
        override val lowFloor: Boolean,
        override val timeCodes: List<String>,
        override val fixedCodes: List<String>,
        override val lineCode: String,
        override val sequence: SequenceCode?,
        override val sequenceName: String?,
        override val nextBus: Pair<BusName, Boolean>?,
        override val previousBus: Pair<BusName, Boolean>?,
        override val deeplink: String,
        override val restriction: Boolean,
        override val traveledSegments: Int,
        override val lineHeight: Float,
        override val favourite: PartOfConn?,
        override val error: Boolean,
        override val onlineConnStops: List<OnlineConnStop>,
    ) : Online

    data class OnlineRunning(
        override val busName: BusName,
        override val stops: List<BusStop>,
        override val lineNumber: ShortLine,
        override val lowFloor: Boolean,
        override val timeCodes: List<String>,
        override val fixedCodes: List<String>,
        override val lineCode: String,
        override val sequence: SequenceCode?,
        override val sequenceName: String?,
        override val nextBus: Pair<BusName, Boolean>?,
        override val previousBus: Pair<BusName, Boolean>?,
        override val deeplink: String,
        override val restriction: Boolean,
        override val traveledSegments: Int,
        override val lineHeight: Float,
        override val favourite: PartOfConn?,
        override val error: Boolean,
        override val onlineConnStops: List<OnlineConnStop>,
        val delayMin: Float,
        val vehicle: RegistrationNumber?,
        val confirmedLowFloor: Boolean?,
        val nextStopIndex: Int,
    ) : Online

    companion object {
        fun OnlineRunning(
            state: Offline,
            onlineConnStops: List<OnlineConnStop>,
            delayMin: Float,
            vehicle: RegistrationNumber?,
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
        val busName: BusName,
    ) : BusState

    data class DoesNotRun(
        override val busName: BusName,
        val date: LocalDate,
        override val timeCodes: List<String>,
        override val fixedCodes: List<String>,
        override val lineCode: String,
        override val deeplink: String,
        val runsNextTimeAfterToday: LocalDate?,
        val runsNextTimeAfterDate: LocalDate?,
    ) : Exists
}