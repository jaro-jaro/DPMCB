package cz.jaro.dpmcb.ui.sequence

import cz.jaro.dpmcb.data.entities.RegistrationNumber
import cz.jaro.dpmcb.data.entities.SequenceCode
import kotlinx.datetime.LocalDate

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
        val runsToday: Boolean,
        val height: Float,
        val traveledSegments: Int,
        val date: LocalDate,
        val vehicleNumber: RegistrationNumber?,
        val vehicleName: String?,
        val online: OnlineState? = null,
    ) : SequenceState

    data class OnlineState(
        val delayMin: Float,
        val confirmedLowFloor: Boolean?,
    )
}