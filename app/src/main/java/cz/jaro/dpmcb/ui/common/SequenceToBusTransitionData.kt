package cz.jaro.dpmcb.ui.common

import cz.jaro.dpmcb.ui.sequence.BusInSequence
import cz.jaro.dpmcb.ui.sequence.SequenceState

data class SequenceToBusTransitionData(val bus: BusInSequence, val state: SequenceState.OK)
