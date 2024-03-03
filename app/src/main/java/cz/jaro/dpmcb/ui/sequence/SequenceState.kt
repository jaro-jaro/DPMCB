package cz.jaro.dpmcb.ui.sequence

sealed interface SequenceState {

    data object Loading : SequenceState

    data class DoesNotExist(
        val kurz: String,
    ) : SequenceState

    sealed interface OK : SequenceState {
        val sequence: String
        val before: List<String>
        val after: List<String>
        val buses: List<BusInSequence>
        val timeCodes: List<String>
        val fixedCodes: List<String>
        val runsToday: Boolean

        data class Offline(
            override val sequence: String,
            override val before: List<String>,
            override val after: List<String>,
            override val buses: List<BusInSequence>,
            override val timeCodes: List<String>,
            override val fixedCodes: List<String>,
            override val runsToday: Boolean,
        ) : OK

        data class Online(
            override val sequence: String,
            override val before: List<String>,
            override val after: List<String>,
            override val buses: List<BusInSequence>,
            override val timeCodes: List<String>,
            override val fixedCodes: List<String>,
            override val runsToday: Boolean,
            val delayMin: Float,
            val vehicle: Int?,
            val confirmedLowFloor: Boolean?,
        ) : OK {
            companion object {
                operator fun invoke(
                    state: OK,
                    delayMin: Float,
                    vehicle: Int?,
                    confirmedLowFloor: Boolean?,
                ) = with(state) {
                    Online(
                        sequence, before, after, buses, timeCodes, fixedCodes, runsToday, delayMin, vehicle, confirmedLowFloor
                    )
                }
            }
        }
    }
}