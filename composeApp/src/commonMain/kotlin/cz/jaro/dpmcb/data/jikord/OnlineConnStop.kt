package cz.jaro.dpmcb.data.jikord

import com.fleeksoft.ksoup.nodes.Element
import cz.jaro.dpmcb.data.helperclasses.toTime
import kotlinx.datetime.LocalTime
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

data class OnlineConnStop(
    val name: String,
    val platform: String,
    val scheduledTime: LocalTime,
    val delay: Duration,
)

fun OnlineConnStop(row: Element): OnlineConnStop {
    val children = row.getElementsByTag("td").map { it.text() }
    return OnlineConnStop(
        name = children[0],
        platform = children[1],
        scheduledTime = children[2].toTime(),
        delay = children[3].toInt().minutes,
    )
}