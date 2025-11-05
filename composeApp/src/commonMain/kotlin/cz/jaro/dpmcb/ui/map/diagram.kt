package cz.jaro.dpmcb.ui.map

import androidx.compose.runtime.Composable

interface DiagramManager {
    suspend fun downloadDiagram(
        path: String,
        progress: (Float) -> Unit,
    )

    val imageData: Any?

    fun checkDiagram(): Boolean
}

expect fun supportsLineDiagram(): Boolean

@Composable
expect fun ShowDiagram(diagramData: Any?)