package cz.jaro.dpmcb.data.helperclasses

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import kotlin.math.absoluteValue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes


fun Duration.toDelay(): String {
    val sign = when {
        inWholeSeconds < 0 -> "-"
        inWholeSeconds > 0 -> "+"
        else -> return "včas"
    }
    val min = inWholeMinutes.absoluteValue
    val s = inWholeSeconds.absoluteValue % 60
    return "$sign$min min $s s"
}

@ReadOnlyComposable
@Composable
fun colorOfDelayText(delay: Duration) = when {
    delay < 0.minutes -> Color(0xFF343DFF)
    delay >= 4.5.minutes -> Color.Red
    delay >= 1.5.minutes -> Color(0xFFCC6600)
    else -> Color.Green
}

@ReadOnlyComposable
@Composable
fun colorOfDelayBubbleText(delay: Duration) = when {
    delay < 0.minutes -> Color(0xFF0000EF)
    delay >= 4.5.minutes -> MaterialTheme.colorScheme.onErrorContainer
    delay >= 1.5.minutes -> Color(0xFFffddaf)
    else -> Color(0xFFADF0D8)
}

@ReadOnlyComposable
@Composable
fun colorOfDelayBubbleContainer(delay: Duration) = when {
    delay < 0.minutes -> Color(0xFFE0E0FF)
    delay >= 4.5.minutes -> MaterialTheme.colorScheme.errorContainer
    delay >= 1.5.minutes -> Color(0xFF614000)
    else -> Color(0xFF015140)
}
