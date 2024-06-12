package cz.jaro.dpmcb.ui.card

import android.net.Uri
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import cz.jaro.dpmcb.R
import cz.jaro.dpmcb.data.App
import cz.jaro.dpmcb.ui.main.DrawerAction
import cz.jaro.dpmcb.ui.main.Route
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

var callback = { _: Uri? -> }

@Suppress("UNUSED_PARAMETER")
@Composable
fun Card(
    args: Route.Card,
    navController: NavHostController,
) {
    App.title = R.string.empty
    App.selected = DrawerAction.TransportCard


    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) {
        callback(it)
    }

    fun getPickMultipleMedia(cb: (Uri?) -> Unit): ManagedActivityResultLauncher<PickVisualMediaRequest, Uri?> {
        callback = cb
        return launcher
    }

    val viewModel: CardViewModel = koinViewModel {
        parametersOf(CardViewModel.Parameters(
            getPickMultipleMedia = ::getPickMultipleMedia
        ))
    }

    val hasCard by viewModel.hasCard.collectAsStateWithLifecycle()
    val card by viewModel.card.collectAsStateWithLifecycle()

    if (hasCard != null) CardScreen(
        card = card,
        hasCard = hasCard!!,
        addCard = viewModel::addCard,
    )
}

@Composable
fun CardScreen(
    card: ImageBitmap?,
    hasCard: Boolean,
    addCard: () -> Unit,
) {
    Box(
        Modifier.background(Color(0xFFD73139)).fillMaxSize()
    ) {
        if (!hasCard || card == null) {
            Button(
                onClick = {
                    addCard()
                },
                Modifier.padding(16.dp)
            ) {
                Text("Přidat průkazku")
            }
        } else {
            Image(
                bitmap = card,
                contentDescription = null,
                Modifier.fillMaxWidth().padding(all = 8.dp),
                contentScale = ContentScale.Fit,
            )
        }
    }
}
