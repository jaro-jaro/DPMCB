package cz.jaro.dpmcb.ui.map

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.core.net.toFile
import androidx.core.net.toUri
import coil.compose.rememberAsyncImagePainter
import coil.imageLoader
import coil.memory.MemoryCache
import coil.request.ImageRequest
import com.ramcosta.composedestinations.annotation.Destination
import cz.jaro.dpmcb.R
import cz.jaro.dpmcb.data.App
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.diagramFile
import cz.jaro.dpmcb.ui.main.DrawerAction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.math.sqrt

@Destination
@Composable
fun Map() {
    App.title = R.string.lines_map
    App.selected = DrawerAction.LinesMap

    MapScreen()
}

@Composable
fun MapScreen() {
//    AndroidView(
//        modifier = Modifier.fillMaxSize(),
//        factory = { context ->
//            PDFView(context, null).apply {
//                fromFile(context.diagramFile).load()
//            }
//        }
//    )
//    val lifecycleScope = LocalLifecycleOwner.current.lifecycleScope
//
//    AndroidView(
//        factory = { context ->
//            PdfRendererView(context).apply {
//                initWithFile(context.diagramFile)
//            }
//        },
//        update = { view ->
//        },
//        modifier = Modifier.fillMaxSize()
//    )

    val context = LocalContext.current

    val modifier: Modifier = Modifier
    val uri: Uri = context.diagramFile.toUri()
    val verticalArrangement: Arrangement.Vertical = Arrangement.spacedBy(8.dp)
    val rendererScope = rememberCoroutineScope()
    val mutex = remember { Mutex() }
    val renderer by produceState<PdfRenderer?>(null, uri) {
        rendererScope.launch(Dispatchers.IO) {
            val input = ParcelFileDescriptor.open(uri.toFile(), ParcelFileDescriptor.MODE_READ_ONLY)
            value = PdfRenderer(input)
        }
        awaitDispose {
            val currentRenderer = value
            rendererScope.launch(Dispatchers.IO) {
                mutex.withLock {
                    currentRenderer?.close()
                }
            }
        }
    }
    val imageLoader = LocalContext.current.imageLoader
    val imageLoadingScope = rememberCoroutineScope()
    BoxWithConstraints(modifier = modifier.fillMaxWidth().graphicsLayer {
        scaleX = 10f
        scaleY = 10f
    }) {
        val width = with(LocalDensity.current) { maxWidth.toPx() }.toInt()
        val height = (width / sqrt(2f)).toInt()
        val pageCount by remember(renderer) { derivedStateOf { renderer?.pageCount ?: 0 } }
        LazyColumn(
            verticalArrangement = verticalArrangement
        ) {
            items(
                count = pageCount,
                key = { index -> "$uri-$index" }
            ) { index ->
                val cacheKey = MemoryCache.Key("$uri-$index")
                val cacheValue: Bitmap? = imageLoader.memoryCache?.get(cacheKey)?.bitmap

                var bitmap: Bitmap? by remember { mutableStateOf(cacheValue) }
                if (bitmap == null) {
                    DisposableEffect(uri, index) {
                        val job = imageLoadingScope.launch(Dispatchers.IO) {
                            val destinationBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                            mutex.withLock {
                                Log.d("PdfGenerator", "Loading PDF $uri - page $index/$pageCount")
                                if (!coroutineContext.isActive) return@launch
                                try {
                                    renderer?.let {
                                        it.openPage(index).use { page ->
                                            page.render(
                                                destinationBitmap,
                                                null,
                                                null,
                                                PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY
                                            )
                                        }
                                    }
                                } catch (e: Exception) {
                                    //Just catch and return in case the renderer is being closed
                                    return@launch
                                }
                            }
                            bitmap = destinationBitmap
                        }
                        onDispose {
                            job.cancel()
                        }
                    }
                    Box(modifier = Modifier
                        .background(Color.White)
                        .aspectRatio(sqrt(2f) / 1f)
                        .fillMaxWidth())
                } else { //bitmap != null
                    val request = ImageRequest.Builder(context)
                        .size(width * 10, height * 10)
                        .memoryCacheKey(cacheKey)
                        .data(bitmap)
                        .build()

                    Image(
                        modifier = Modifier
                            .background(Color.White)
                            .aspectRatio(sqrt(2f) / 1f)
                            .fillMaxWidth(),
                        contentScale = ContentScale.Fit,
                        painter = rememberAsyncImagePainter(request),
                        contentDescription = "Page ${index + 1} of $pageCount"
                    )
                }
            }
        }
    }
}
