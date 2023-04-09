package cz.jaro.dpmcb.ui.mapa

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.github.barteksc.pdfviewer.PDFView
import com.ramcosta.composedestinations.annotation.Destination
import cz.jaro.dpmcb.R
import cz.jaro.dpmcb.SuplikAkce
import cz.jaro.dpmcb.data.App
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.schemaFile

@Destination
@Composable
fun Mapa() {
    App.title = R.string.mapa_linek
    App.vybrano = SuplikAkce.Mapa

    MapaScreen()
}

@Composable
fun MapaScreen() = AndroidView(
    modifier = Modifier.fillMaxSize(),
    factory = { context ->
        PDFView(context, null).apply {
            fromFile(context.schemaFile).load()
        }
    }
)
