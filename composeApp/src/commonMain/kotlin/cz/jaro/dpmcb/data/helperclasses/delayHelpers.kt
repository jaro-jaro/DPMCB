package cz.jaro.dpmcb.data.helperclasses

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Badge
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import cz.jaro.dpmcb.ui.theme.Colors
import cz.jaro.dpmcb.ui.theme.DPMCBTheme
import cz.jaro.dpmcb.ui.theme.Theme
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

@Preview
@Composable
fun ColorsPreview() = Column {
    DPMCBTheme(useDarkTheme = true, useDynamicColor = false, theme = Theme.Yellow) {
        Surface {
            Column(Modifier.padding(8.dp)) {
                Text("content")
                Text("dimmed", color = Colors.dimmedContent)
                Text("10:00", color = Colors.delayEarlyText)
                Text("10:01", color = Colors.delaySmallText)
                Text("10:03", color = Colors.delayMediumText)
                Text("10:09", color = Colors.delayLargeText)
                Badge(Modifier, Colors.delayEarlyBubble, Colors.delayEarlyBubbleText) { Text("10 s") }
                Badge(Modifier, Colors.delaySmallBubble, Colors.delaySmallBubbleText) { Text("včas") }
                Badge(Modifier, Colors.delayMediumBubble, Colors.delayMediumBubbleText) { Text("2 min") }
                Badge(Modifier, Colors.delayLargeBubble, Colors.delayLargeBubbleText) { Text("8 min") }
            }
        }
    }
    DPMCBTheme(useDarkTheme = false, useDynamicColor = false, theme = Theme.Yellow) {
        Surface {
            Column(Modifier.padding(8.dp)) {
                Text("content")
                Text("dimmed", color = Colors.dimmedContent)
                Text("10:00", color = Colors.delayEarlyText)
                Text("10:01", color = Colors.delaySmallText)
                Text("10:03", color = Colors.delayMediumText)
                Text("10:09", color = Colors.delayLargeText)
                Badge(Modifier, Colors.delayEarlyBubble, Colors.delayEarlyBubbleText) { Text("10 s") }
                Badge(Modifier, Colors.delaySmallBubble, Colors.delaySmallBubbleText) { Text("včas") }
                Badge(Modifier, Colors.delayMediumBubble, Colors.delayMediumBubbleText) { Text("2 min") }
                Badge(Modifier, Colors.delayLargeBubble, Colors.delayLargeBubbleText) { Text("8 min") }
            }
        }
    }
}

@ReadOnlyComposable
@Composable
fun colorOfDelayText(delay: Duration) = when {
    delay < 0.minutes -> Colors.delayEarlyText
    delay >= 4.5.minutes -> Colors.delayLargeText
    delay >= 1.5.minutes -> Colors.delayMediumText
    else -> Colors.delaySmallText
}

@ReadOnlyComposable
@Composable
fun colorOfDelayBubbleText(delay: Duration) = when {
    delay < 0.minutes -> Colors.delayEarlyBubbleText
    delay >= 4.5.minutes -> Colors.delayLargeBubbleText
    delay >= 1.5.minutes -> Colors.delayMediumBubbleText
    else -> Colors.delaySmallBubbleText
}

@ReadOnlyComposable
@Composable
fun colorOfDelayBubbleContainer(delay: Duration) = when {
    delay < 0.minutes -> Colors.delayEarlyBubble
    delay >= 4.5.minutes -> Colors.delayLargeBubble
    delay >= 1.5.minutes -> Colors.delayMediumBubble
    else -> Colors.delaySmallBubble
}
