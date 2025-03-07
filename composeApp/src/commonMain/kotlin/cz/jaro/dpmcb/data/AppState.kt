package cz.jaro.dpmcb.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import cz.jaro.dpmcb.ui.main.DrawerAction

object AppState {
    var route by mutableStateOf("favourites")
    var title by mutableStateOf("Lepší DPMCB")
    var selected by mutableStateOf(null as DrawerAction?)
}