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
        val date: LocalDate
    }

    data class OK(
        override val busName: BusName,
        val stops: List<BusStop>,
        val lineNumber: ShortLine,
        val lowFloor: Boolean,
        override val timeCodes: List<String>,
        override val fixedCodes: List<String>,
        override val lineCode: String,
        val sequence: SequenceCode?,
        val sequenceName: String?,
        val nextBus: BusName?,
        val previousBus: BusName?,
        override val deeplink: String,
        val restriction: Boolean,
        val traveledSegments: Int,
        val lineHeight: Float,
        val favourite: PartOfConn?,
        val shouldBeOnline: Boolean,
        override val date: LocalDate,
        val online: OnlineState? = null,
    ) : Exists

    data class OnlineState(
        val onlineConnStops: List<OnlineConnStop>,
        val running: RunningState? = null,
    )

    data class RunningState(
        val delayMin: Float?,
        val vehicleNumber: RegistrationNumber?,
        val vehicleName: String?,
        val confirmedLowFloor: Boolean?,
        val nextStopIndex: Int?,
    )

    data object Loading : BusState

    data class DoesNotExist(
        val busName: BusName,
    ) : BusState

    data class DoesNotRun(
        override val busName: BusName,
        override val date: LocalDate,
        override val timeCodes: List<String>,
        override val fixedCodes: List<String>,
        override val lineCode: String,
        override val deeplink: String,
        val runsNextTimeAfterToday: LocalDate?,
        val runsNextTimeAfterDate: LocalDate?,
    ) : Exists
}