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

    sealed interface OK : SequenceState {
        val sequence: SequenceCode
        val sequenceName: String
        val before: List<Pair<SequenceCode, String>>
        val after: List<Pair<SequenceCode, String>>
        val buses: List<BusInSequence>
        val timeCodes: List<String>
        val fixedCodes: List<String>
        val runsToday: Boolean
        val height: Float
        val traveledSegments: Int
        val date: LocalDate
        val vehicle: RegistrationNumber?
    }

    data class Offline(
        override val sequence: SequenceCode,
        override val sequenceName: String,
        override val before: List<Pair<SequenceCode, String>>,
        override val after: List<Pair<SequenceCode, String>>,
        override val buses: List<BusInSequence>,
        override val timeCodes: List<String>,
        override val fixedCodes: List<String>,
        override val runsToday: Boolean,
        override val height: Float,
        override val traveledSegments: Int,
        override val date: LocalDate,
        override val vehicle: RegistrationNumber?,
    ) : OK

    data class Online(
        override val sequence: SequenceCode,
        override val sequenceName: String,
        override val before: List<Pair<SequenceCode, String>>,
        override val after: List<Pair<SequenceCode, String>>,
        override val buses: List<BusInSequence>,
        override val timeCodes: List<String>,
        override val fixedCodes: List<String>,
        override val runsToday: Boolean,
        override val height: Float,
        override val traveledSegments: Int,
        override val date: LocalDate,
        override val vehicle: RegistrationNumber?,
        val delayMin: Float,
        val confirmedLowFloor: Boolean?,
    ) : OK

    companion object {
        fun Online(
            state: OK,
            delayMin: Float,
            vehicle: RegistrationNumber?,
            confirmedLowFloor: Boolean?,
        ) = with(state) {
            Online(
                sequence, sequenceName, before, after, buses, timeCodes, fixedCodes, runsToday, height, traveledSegments, date,
                vehicle ?: this.vehicle, delayMin, confirmedLowFloor
            )
        }
    }
}