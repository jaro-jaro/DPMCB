package cz.jaro.dpmcb.data

import androidx.compose.material3.DrawerValue
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import cz.jaro.dpmcb.ui.main.DrawerAction
import cz.jaro.dpmcb.ui.main.Route

object AppState {
    var route by mutableStateOf(Route.initialRoute)
    var title by mutableStateOf("Lepší DPMCB")
    var selected by mutableStateOf(null as DrawerAction?)
    var menuState by mutableStateOf(DrawerValue.Closed)

    const val APP_URL = "https://dpmcb-jaro.web.app/#"
}