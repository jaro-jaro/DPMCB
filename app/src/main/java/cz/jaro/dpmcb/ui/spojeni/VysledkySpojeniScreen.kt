package cz.jaro.dpmcb.ui.spojeni

import androidx.compose.runtime.Composable
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import cz.jaro.dpmcb.R
import cz.jaro.dpmcb.data.App

@Destination
@Composable
fun VysledkySpojeniScreen(
    navigator: DestinationsNavigator,
) {
    App.title = R.string.vysledky_vyhledavani

}
