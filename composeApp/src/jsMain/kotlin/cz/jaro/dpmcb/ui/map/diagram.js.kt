package cz.jaro.dpmcb.ui.map

import androidx.compose.runtime.Composable

actual fun supportsLineDiagram() = false

@Composable
actual fun ShowDiagram(diagramData: Any?): Unit = error("Not supported")