package cz.jaro.dpmcb.ui.bus

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.graphics.layer.GraphicsLayer
import cz.jaro.dpmcb.data.helperclasses.MutateFunction
import cz.jaro.dpmcb.data.realtions.PartOfConn

fun interface BusShareManager {
    fun shareBus()
}

@Composable
expect fun busShareManager(
    state: BusState.Exists,
    graphicsLayerWhole: GraphicsLayer,
    graphicsLayerPart: GraphicsLayer,
    part: State<PartOfConn>,
    editPart: MutateFunction<PartOfConn>,
): BusShareManager