package cz.jaro.dpmcb.data.entities

import androidx.room.Entity
import cz.jaro.dpmcb.data.helperclasses.Cas

@Entity(primaryKeys = ["linka", "cisloSpoje", "indexZastavkyNaLince"])
data class ZastavkaSpoje(
    val linka: Int,
    val cisloSpoje: Int,
    val indexZastavkyNaLince: Int,
    val cisloZastavky: Int,
    val kmOdStartu: Int,
    val prijezd: Cas?,
    val odjezd: Cas?,
) {
    val cas get() = odjezd ?: prijezd ?: Cas.nikdy
}