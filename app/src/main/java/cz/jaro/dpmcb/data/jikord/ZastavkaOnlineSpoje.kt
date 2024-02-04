package cz.jaro.dpmcb.data.jikord

import com.gitlab.mvysny.konsumexml.Konsumer
import com.google.firebase.crashlytics.ktx.crashlytics
import com.google.firebase.ktx.Firebase
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.toCas
import java.time.LocalTime

data class ZastavkaOnlineSpoje(
    val nazev: String,
    val stanoviste: String,
    val pravidelnyCas: LocalTime,
    val zpozdeni: Int,
)

context(Konsumer)
fun ZastavkaOnlineSpoje(): ZastavkaOnlineSpoje? {
    try {
        checkCurrent("tr")
        val deti = childrenText("td", 4, 4)
        return ZastavkaOnlineSpoje(
            nazev = deti[0],
            stanoviste = deti[1],
            pravidelnyCas = deti[2].toCas(),
            zpozdeni = deti[3].toInt(),
        )
    } catch (e: RuntimeException) {
        Firebase.crashlytics.recordException(e)
        return null
    }
}