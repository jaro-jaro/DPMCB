package cz.jaro.dpmcb.data.jikord

import com.gitlab.mvysny.konsumexml.Konsumer
import cz.jaro.dpmcb.data.helperclasses.toTime
import cz.jaro.dpmcb.data.recordException
import kotlinx.datetime.LocalTime

data class OnlineConnStop(
    val name: String,
    val platform: String,
    val scheduledTime: LocalTime,
    val delay: Int,
)

fun Konsumer.OnlineConnStop(): OnlineConnStop? {
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
        recordException(e)
        return null
    }
}