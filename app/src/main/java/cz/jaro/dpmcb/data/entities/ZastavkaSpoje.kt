package cz.jaro.dpmcb.data.entities

import androidx.room.Entity
import java.time.LocalTime

@Entity(primaryKeys = ["tab", "cisloSpoje", "indexZastavkyNaLince"])
data class ZastavkaSpoje(
// Primary keys
    val tab: String,
    val cisloSpoje: Int,
    val indexZastavkyNaLince: Int,
// Other
    val linka: Int,
    val cisloZastavky: Int,
    val kmOdStartu: Int,
    val prijezd: LocalTime?,
    val odjezd: LocalTime?,
) {
    val cas get() = odjezd ?: prijezd
}