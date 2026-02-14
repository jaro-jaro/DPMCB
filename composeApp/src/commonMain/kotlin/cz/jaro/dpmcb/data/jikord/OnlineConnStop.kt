package cz.jaro.dpmcb.data.jikord

import com.fleeksoft.ksoup.nodes.Element
import cz.jaro.dpmcb.data.entities.Platform
import cz.jaro.dpmcb.data.entities.StopName
import cz.jaro.dpmcb.data.helperclasses.toTime
import kotlinx.datetime.LocalTime
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

data class OnlineConnStop(
    val name: StopName,
    val platform: Platform,
    val scheduledTime: LocalTime,
    val delay: Duration,
)

fun OnlineConnStop(row: Element): OnlineConnStop {
    val children = row.getElementsByTag("td").map { it.text() }
    return OnlineConnStop(
        name = StopName("", "", children[0]), // TODO!!!
        platform = children[1],
        scheduledTime = children[2].toTime(),
        delay = children[3].toInt().minutes,
    )
}