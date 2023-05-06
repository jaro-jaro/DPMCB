package cz.jaro.dpmcb.ui.spoj

import androidx.compose.ui.graphics.vector.ImageVector
import cz.jaro.dpmcb.data.realtions.CasNazevSpojId

data class SpojInfo(
    val spojId: String,
    val zastavky: List<CasNazevSpojId>,
    val cisloLinky: Int,
    val nizkopodlaznost: ImageVector,
    val caskody: List<String>,
    val pevneKody: List<String>,
    val linkaKod: String,
    val nazevSpoje: String,
    val deeplink: String,
    val vyluka: Boolean,
)