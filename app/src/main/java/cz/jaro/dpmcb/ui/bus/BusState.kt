package cz.jaro.dpmcb.ui.bus

import cz.jaro.dpmcb.data.helperclasses.PartOfConn
import cz.jaro.dpmcb.data.jikord.OnlineConnStop
import cz.jaro.dpmcb.data.realtions.LineTimeNameConnIdNextStop
import java.time.LocalDate
import java.time.LocalTime

sealed interface BusState {

    sealed interface Exists : BusState {
        val busId: String
        val timeCodes: List<String>
        val fixedCodes: List<String>
        val lineCode: String
        val busName: String
        val deeplink: String
    }

    sealed interface OK : Exists {

        val stops: List<LineTimeNameConnIdNextStop>
        val lineNumber: Int
        val lowFloor: Boolean
        val sequence: String?
        val sequenceName: String
        val nextBus: Pair<String, Boolean>?
        val previousBus: Pair<String, Boolean>?
        val restriction: Boolean
        val traveledSegments: Int
        val lineHeight: Float
        val favourite: PartOfConn?
        val error: Boolean

        data class Offline(
            override val busId: String,
            override val stops: List<LineTimeNameConnIdNextStop>,
            override val lineNumber: Int,
            override val lowFloor: Boolean,
            override val timeCodes: List<String>,
            override val fixedCodes: List<String>,
            override val lineCode: String,
            override val busName: String,
            override val sequence: String?,
            override val sequenceName: String,
            override val nextBus: Pair<String, Boolean>?,
            override val previousBus: Pair<String, Boolean>?,
            override val deeplink: String,
            override val restriction: Boolean,
            override val traveledSegments: Int,
            override val lineHeight: Float,
            override val favourite: PartOfConn?,
            override val error: Boolean,
        ) : OK

        data class Online(
            override val busId: String,
            override val stops: List<LineTimeNameConnIdNextStop>,
            override val lineNumber: Int,
            override val lowFloor: Boolean,
            override val timeCodes: List<String>,
            override val fixedCodes: List<String>,
            override val lineCode: String,
            override val busName: String,
            override val sequence: String?,
            override val sequenceName: String,
            override val nextBus: Pair<String, Boolean>?,
            override val previousBus: Pair<String, Boolean>?,
            override val deeplink: String,
            override val restriction: Boolean,
            override val traveledSegments: Int,
            override val lineHeight: Float,
            override val favourite: PartOfConn?,
            override val error: Boolean,
            val onlineConnStops: List<OnlineConnStop>,
            val delayMin: Float,
            val vehicle: Int?,
            val confirmedLowFloor: Boolean?,
            val nextStop: LocalTime,
        ) : OK {
            companion object {
                operator fun invoke(
                    state: Offline,
                    onlineConnStops: List<OnlineConnStop>,
                    delayMin: Float,
                    vehicle: Int?,
                    confirmedLowFloor: Boolean?,
                    nextStop: LocalTime,
                ) = with(state) {
                    Online(
                        busId, stops, lineNumber, lowFloor, timeCodes, fixedCodes, lineCode, busName, sequence, sequenceName, nextBus, previousBus,
                        deeplink, restriction, traveledSegments, lineHeight, favourite, error, onlineConnStops, delayMin, vehicle, confirmedLowFloor, nextStop
                    )
                }
            }
        }
    }

    data object Loading : BusState

    data class DoesNotExist(
        val busId: String,
    ) : BusState

    data class DoesNotRun(
        override val busId: String,
        val date: LocalDate,
        override val timeCodes: List<String>,
        override val fixedCodes: List<String>,
        override val lineCode: String,
        override val busName: String,
        override val deeplink: String,
        val runsNextTimeAfterToday: LocalDate?,
        val runsNextTimeAfterDate: LocalDate?,
    ) : Exists
}