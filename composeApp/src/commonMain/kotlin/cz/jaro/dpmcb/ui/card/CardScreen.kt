package cz.jaro.dpmcb.ui.card

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContent
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import cz.jaro.dpmcb.data.AppState
import cz.jaro.dpmcb.data.viewModel
import cz.jaro.dpmcb.ui.main.DrawerAction
import cz.jaro.dpmcb.ui.main.Route
import cz.jaro.dpmcb.ui.theme.dpmcb

@Suppress("unused")
@Composable
fun Card(
    args: Route.Card,
    navController: NavHostController,
    superNavController: NavHostController,
    viewModel: CardViewModel = viewModel(),
) {
    AppState.title = ""
    AppState.selected = DrawerAction.TransportCard

    val hasCard by viewModel.hasCard.collectAsStateWithLifecycle()
    val card by viewModel.card.collectAsStateWithLifecycle()

    if (hasCard != null) CardScreen(
        card = card,
        addCard = viewModel::addCard,
    )
}

@Composable
fun CardScreen(
    card: ImageBitmap?,
    addCard: () -> Unit,
) {
    Box(
        Modifier
            .background(dpmcb)
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeContent.only(WindowInsetsSides.Bottom))
    ) {
        if (card == null) Button(
            onClick = {
                addCard()
            },
            Modifier.padding(16.dp)
        ) {
            Text("Přidat průkazku")
        } else
            Image(
                bitmap = card,
                contentDescription = null,
                Modifier
                    .fillMaxWidth()
                    .padding(all = 8.dp),
                contentScale = ContentScale.Fit,
            )
    }
}
