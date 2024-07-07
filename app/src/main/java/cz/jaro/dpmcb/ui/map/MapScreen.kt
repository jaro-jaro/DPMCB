package cz.jaro.dpmcb.ui.map

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavHostController
import com.github.barteksc.pdfviewer.PDFView
import cz.jaro.dpmcb.R
import cz.jaro.dpmcb.data.App
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.diagramFile
import cz.jaro.dpmcb.ui.main.DrawerAction
import cz.jaro.dpmcb.ui.main.Route

@Suppress("UNUSED_PARAMETER")
@Composable
fun Map(
    args: Route.Map,
    navController: NavHostController,
) {
    App.title = R.string.lines_map
    App.selected = DrawerAction.LinesMap

    MapScreen()
}

@Composable
fun MapScreen() = AndroidView(
    modifier = Modifier.fillMaxSize(),
    factory = { context ->
        PDFView(context, null).apply {
            fromFile(context.diagramFile).load()
        }
    }
)
