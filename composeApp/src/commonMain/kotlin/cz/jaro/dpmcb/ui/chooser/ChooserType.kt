package cz.jaro.dpmcb.ui.chooser

import kotlinx.serialization.Serializable

@Serializable
enum class ChooserType {
    Stops,
    Lines,
    LineStops,
    EndStop,
    ReturnStop1,
    ReturnStop2,
    ReturnLine,
    ReturnStop,
    ReturnStopVia,
}
