package cz.jaro.dpmcb.ui.sequence

import cz.jaro.dpmcb.data.entities.RegistrationNumber
import cz.jaro.dpmcb.data.entities.SequenceCode

sealed interface SequenceState {

    data object Loading : SequenceState

    data class DoesNotExist(
        val sequence: SequenceCode,
        val sequenceName: String,
        val date: String,
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
        val delayMin: Float,
        val vehicle: RegistrationNumber?,
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
                sequence, sequenceName, before, after, buses, timeCodes, fixedCodes, runsToday, height, traveledSegments,
                delayMin, vehicle, confirmedLowFloor
            )
        }
    }
}