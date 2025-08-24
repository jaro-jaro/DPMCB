package cz.jaro.dpmcb.ui.settings

import cz.jaro.dpmcb.data.Settings
import cz.jaro.dpmcb.data.entities.ShortLine
import cz.jaro.dpmcb.data.entities.Table
import cz.jaro.dpmcb.data.helperclasses.Traction
import kotlinx.datetime.LocalDate

data class SettingsState(
    val settings: Settings?,
    val version: String,
    val dataVersion: Int,
    val dataMetaVersion: Int,
    val isOnline: Boolean,
    val tables: List<LineTable>,
)

data class LineTable(
    val shortNumber: ShortLine,
    val tab: Table,
    val validFrom: LocalDate,
    val validTo: LocalDate,
    val route: String,
    val traction: Traction,
    val hasRestriction: Boolean,
)