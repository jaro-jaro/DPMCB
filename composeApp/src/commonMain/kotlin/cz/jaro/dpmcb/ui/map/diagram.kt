package cz.jaro.dpmcb.ui.map

import dev.gitlive.firebase.storage.StorageReference

interface DiagramManager {
    suspend fun downloadDiagram(
        reference: StorageReference,
        progress: (Float) -> Unit,
    )

    val imageData: Any?

    fun checkDiagram(): Boolean
}

expect fun supportsLineDiagram(): Boolean