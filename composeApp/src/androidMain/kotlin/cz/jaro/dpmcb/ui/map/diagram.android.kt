package cz.jaro.dpmcb.ui.map

import android.content.Context
import dev.gitlive.firebase.storage.StorageReference
import io.ktor.client.HttpClient
import io.ktor.client.plugins.onDownload
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsChannel
import io.ktor.utils.io.jvm.javaio.toInputStream
import java.io.File

class AndroidDiagramManager(ctx: Context) : DiagramManager {

    val diagramFile = File(ctx.filesDir, "schema.pdf")

    private val client = HttpClient()

    override suspend fun downloadDiagram(
        reference: StorageReference,
        progress: (Float) -> Unit,
    ) {
        client.get(reference.getDownloadUrl()) {
            onDownload { bytesSentTotal, contentLength ->
                progress(bytesSentTotal.toFloat() / contentLength)
            }
        }.bodyAsChannel().toInputStream().use { input ->
            diagramFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }
    }

    override val imageData get() = diagramFile

    override fun checkDiagram() = diagramFile.exists()
}

actual fun supportsLineDiagram() = true