package cz.jaro.dpmcb.ui.map

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import coil.ImageLoader
import coil.decode.SvgDecoder
import coil.request.ImageRequest
import coil.size.Dimension
import cz.jaro.dpmcb.data.FileStorageManager
import io.ktor.client.statement.bodyAsChannel
import io.ktor.utils.io.jvm.javaio.toInputStream
import me.saket.telephoto.zoomable.ZoomSpec
import me.saket.telephoto.zoomable.coil.ZoomableAsyncImage
import me.saket.telephoto.zoomable.rememberZoomableImageState
import me.saket.telephoto.zoomable.rememberZoomableState
import java.io.File

class AndroidDiagramManager(ctx: Context) : DiagramManager {

    val diagramFile = File(ctx.filesDir, "schema.pdf")

    override suspend fun downloadDiagram(
        path: String,
        progress: (Float) -> Unit,
    ): Unit = FileStorageManager().use { manager ->
        manager
            .getObject(path) { progress(it ?: 0F) }
            .bodyAsChannel().toInputStream().use { input ->
                diagramFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
    }

    override val imageData get() = diagramFile

    override fun checkDiagram() = diagramFile.exists()
}

actual fun supportsLineDiagram() = true

@Composable
actual fun ShowDiagram(diagramData: Any?) {
    val context = LocalContext.current
    val imageLoader = remember {
        ImageLoader.Builder(context)
            .components {
                add(SvgDecoder.Factory(useViewBoundsAsIntrinsicSize = false))
            }
            .build()
    }
    ZoomableAsyncImage(
        model = ImageRequest.Builder(LocalContext.current)
            .data(diagramData)
            .crossfade(true)
            .size(width = Dimension(4000), height = Dimension.Undefined)
            .build(),
        contentDescription = null,
        Modifier
            .fillMaxSize()
            .background(Color.White),
        imageLoader = imageLoader,
        state = rememberZoomableImageState(
            zoomableState = rememberZoomableState(
                zoomSpec = ZoomSpec(
                    maxZoomFactor = 10F,
                )
            )
        )
    )
}