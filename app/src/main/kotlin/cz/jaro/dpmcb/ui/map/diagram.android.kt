package cz.jaro.dpmcb.ui.map

import android.content.Context
import com.google.firebase.storage.StorageReference
import kotlinx.coroutines.tasks.await
import java.io.File

class AndroidDiagramManager(ctx: Context) : DiagramManager {

    val diagramFile = File(ctx.filesDir, "schema.pdf")

    override suspend fun downloadDiagram(
        reference: StorageReference,
        progress: (Float) -> Unit,
    ) {
        val task = reference.getFile(diagramFile)

        task.addOnFailureListener {
            throw it
        }

        task.addOnProgressListener { snapshot ->
            progress(snapshot.bytesTransferred.toFloat() / snapshot.totalByteCount)
        }

        task.await()
    }

    override val imageData get() = diagramFile

    override fun checkDiagram() = diagramFile.exists()
}

fun supportsLineDiagram() = true