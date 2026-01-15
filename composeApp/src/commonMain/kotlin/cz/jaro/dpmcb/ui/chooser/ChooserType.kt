package cz.jaro.dpmcb.ui.chooser

import kotlinx.serialization.Serializable

@Serializable
enum class ChooserType {
    Stops,
    Lines,
    LineStops,
    Platforms,
    ReturnStop1,
    ReturnStop2,
    ReturnLine,
    ReturnStop,
    ReturnStopVia,
}
