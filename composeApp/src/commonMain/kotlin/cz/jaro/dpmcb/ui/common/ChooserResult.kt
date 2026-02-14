package cz.jaro.dpmcb.ui.common

import cz.jaro.dpmcb.ui.chooser.ChooserType
import kotlinx.serialization.Serializable

@Serializable
data class ChooserResult<T>(val value: T, val chooserType: ChooserType)