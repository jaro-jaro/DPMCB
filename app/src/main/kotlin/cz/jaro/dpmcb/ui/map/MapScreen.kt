package cz.jaro.dpmcb.ui.map

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeContent
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import coil.ImageLoader
import coil.decode.SvgDecoder
import coil.request.ImageRequest
import coil.size.Dimension
import cz.jaro.dpmcb.R
import cz.jaro.dpmcb.data.App
import cz.jaro.dpmcb.data.helperclasses.diagramFile
import cz.jaro.dpmcb.ui.main.DrawerAction
import cz.jaro.dpmcb.ui.main.Route
import me.saket.telephoto.zoomable.ZoomSpec
import me.saket.telephoto.zoomable.coil.ZoomableAsyncImage
import me.saket.telephoto.zoomable.rememberZoomableImageState
import me.saket.telephoto.zoomable.rememberZoomableState

@Suppress("unused")
@Composable
fun Map(
    args: Route.Map,
    navController: NavHostController,
    superNavController: NavHostController,
) {
    App.title = R.string.lines_map
    App.selected = DrawerAction.LinesMap

    MapScreen()
}

@Composable
fun MapScreen() {
    val context = LocalContext.current
    val imageLoader = remember {
        ImageLoader.Builder(context)
            .components {
                add(SvgDecoder.Factory(useViewBoundsAsIntrinsicSize = false))
            }
            .build()
    }

    Box(
        Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeContent.only(WindowInsetsSides.Bottom)),
        contentAlignment = Alignment.Center
    ) {
        ZoomableAsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(context.diagramFile)
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
}