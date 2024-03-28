package cz.jaro.dpmcb.ui.sequence

sealed interface SequenceState {

    data object Loading : SequenceState

    data class DoesNotExist(
        val sequence: String,
        val sequenceName: String,
        val date: String
    ) : SequenceState

    sealed interface OK : SequenceState {
        val sequence: String
        val sequenceName: String
        val before: List<Pair<String, String>>
        val after: List<Pair<String, String>>
        val buses: List<BusInSequence>
        val timeCodes: List<String>
        val fixedCodes: List<String>
        val runsToday: Boolean
        val height: Float
        val traveledSegments: Int
    }

    data class Offline(
        override val sequence: String,
        override val sequenceName: String,
        override val before: List<Pair<String, String>>,
        override val after: List<Pair<String, String>>,
        override val buses: List<BusInSequence>,
        override val timeCodes: List<String>,
        override val fixedCodes: List<String>,
        override val runsToday: Boolean,
        override val height: Float,
        override val traveledSegments: Int,
    ) : OK

    data class Online(
        override val sequence: String,
        override val sequenceName: String,
        override val before: List<Pair<String, String>>,
        override val after: List<Pair<String, String>>,
        override val buses: List<BusInSequence>,
        override val timeCodes: List<String>,
        override val fixedCodes: List<String>,
        override val runsToday: Boolean,
        override val height: Float,
        override val traveledSegments: Int,
        val delayMin: Float,
        val vehicle: Int?,
        val confirmedLowFloor: Boolean?,
    ) : OK

    companion object {
        fun Online(
            state: OK,
            delayMin: Float,
            vehicle: Int?,
            confirmedLowFloor: Boolean?,
        ) = with(state) {
            Online(
                sequence, sequenceName, before, after, buses, timeCodes, fixedCodes, runsToday, height, traveledSegments, delayMin, vehicle, confirmedLowFloor
            )
        }
    }
}