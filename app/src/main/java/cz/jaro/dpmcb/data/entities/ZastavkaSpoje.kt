package cz.jaro.dpmcb.data.entities

import androidx.room.Entity
import java.time.LocalTime

@Entity(primaryKeys = ["linka", "cisloSpoje", "indexZastavkyNaLince"])
data class ZastavkaSpoje(
    val linka: Int,
    val cisloSpoje: Int,
    val indexZastavkyNaLince: Int,
    val cisloZastavky: Int,
    val kmOdStartu: Int,
    val prijezd: LocalTime?,
    val odjezd: LocalTime?,
) {
    val cas get() = odjezd ?: prijezd
}