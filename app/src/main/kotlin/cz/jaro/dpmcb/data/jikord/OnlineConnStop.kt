package cz.jaro.dpmcb.data.jikord

import com.gitlab.mvysny.konsumexml.Konsumer
import com.google.firebase.crashlytics.crashlytics
import com.google.firebase.Firebase
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.toTime
import kotlinx.datetime.LocalTime

data class OnlineConnStop(
    val name: String,
    val platform: String,
    val scheduledTime: LocalTime,
    val delay: Int,
)

context(Konsumer)
fun OnlineConnStop(): OnlineConnStop? {
    try {
        checkCurrent("tr")
        val children = childrenText("td", 4, 4)
        return OnlineConnStop(
            name = children[0],
            platform = children[1],
            scheduledTime = children[2].toTime(),
            delay = children[3].toInt(),
        )
    } catch (e: RuntimeException) {
        Firebase.crashlytics.recordException(e)
        return null
    }
}