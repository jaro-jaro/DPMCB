package cz.jaro.dpmcb.ui.sequence

import cz.jaro.dpmcb.data.entities.RegistrationNumber
import cz.jaro.dpmcb.data.entities.SequenceCode
import cz.jaro.dpmcb.data.helperclasses.Traction
import kotlinx.datetime.LocalDate
import kotlin.time.Duration

sealed interface SequenceState {

    data object Loading : SequenceState

    data class DoesNotExist(
        val sequence: SequenceCode,
        val sequenceName: String,
        val date: LocalDate,
    ) : SequenceState

    data class OK(
        val sequence: SequenceCode,
        val sequenceName: String,
        val before: List<Pair<SequenceCode, String>>,
        val after: List<Pair<SequenceCode, String>>,
        val buses: List<BusInSequence>,
        val timeCodes: List<String>,
        val fixedCodes: List<String>,
        val lineCode: String,
        val lineTraction: Traction?,
        val runsToday: Boolean,
        val height: Float,
        val traveledSegments: Int,
        val date: LocalDate,
        val vehicleNumber: RegistrationNumber?,
        val vehicleName: String?,
        val vehicleTraction: Traction?,
        val online: OnlineState? = null,
    ) : SequenceState

    data class OnlineState(
        val delay: Duration,
        val confirmedLowFloor: Boolean?,
    )
}