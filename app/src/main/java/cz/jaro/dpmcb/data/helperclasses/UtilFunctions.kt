package cz.jaro.dpmcb.data.helperclasses

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.navigation.NavHostController
import cz.jaro.dpmcb.BuildConfig
import cz.jaro.dpmcb.data.Settings
import cz.jaro.dpmcb.ui.main.Route
import cz.jaro.dpmcb.ui.theme.DPMCBTheme
import cz.jaro.dpmcb.ui.theme.Theme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import java.io.File
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.temporal.ChronoUnit
import kotlin.experimental.ExperimentalTypeInference
import kotlin.math.absoluteValue
import kotlin.time.toJavaDuration

object UtilFunctions {

    fun LocalDate.toCzechLocative() = LocalDate.now().until(this, ChronoUnit.DAYS).let { za ->
        when (za) {
            0L -> "dnes"
            1L -> "zítra"
            2L -> "pozítří"
            in 3L..6L -> when (dayOfWeek!!) {
                DayOfWeek.MONDAY -> "v pondělí"
                DayOfWeek.TUESDAY -> "v úterý"
                DayOfWeek.WEDNESDAY -> "ve středu"
                DayOfWeek.THURSDAY -> "ve čtvrtek"
                DayOfWeek.FRIDAY -> "v pátek"
                DayOfWeek.SATURDAY -> "v sobotu"
                DayOfWeek.SUNDAY -> "v neděli"
            }

            else -> asString()
        }
    }

    fun LocalDate.toCzechAccusative() = LocalDate.now().until(this, ChronoUnit.DAYS).let { za ->
        when (za) {
            0L -> "dnešek"
            1L -> "zítřek"
            2L -> "pozítří"
            in 3L..6L -> when (dayOfWeek!!) {
                DayOfWeek.MONDAY -> "pondělí"
                DayOfWeek.TUESDAY -> "úterý"
                DayOfWeek.WEDNESDAY -> "středu"
                DayOfWeek.THURSDAY -> "čtvrtek"
                DayOfWeek.FRIDAY -> "pátek"
                DayOfWeek.SATURDAY -> "sobotu"
                DayOfWeek.SUNDAY -> "neděli"
            }

            else -> asString()
        }
    }

    inline fun <T> List<T>.reversedIf(predicate: (List<T>) -> Boolean): List<T> = if (predicate(this)) this.reversed() else this

    /**
     * Returns a [Flow] whose values are generated with [transform] function by combining
     * the most recently emitted values by each flow.
     */
    @Suppress("UNCHECKED_CAST")
    fun <T1, T2, T3, T4, T5, T6, R : Any> combine(
        flow: Flow<T1>,
        flow2: Flow<T2>,
        flow3: Flow<T3>,
        flow4: Flow<T4>,
        flow5: Flow<T5>,
        flow6: Flow<T6>,
        transform: suspend (T1, T2, T3, T4, T5, T6) -> R
    ): Flow<R> = combine(flow, flow2, flow3, flow4, flow5, flow6) { args: Array<*> ->
        transform(
            args[0] as T1,
            args[1] as T2,
            args[2] as T3,
            args[3] as T4,
            args[4] as T5,
            args[5] as T6,
        )
    }
    /**
     * Returns a [Flow] whose values are generated with [transform] function by combining
     * the most recently emitted values by each flow.
     */
    @Suppress("UNCHECKED_CAST")
    fun <T1, T2, T3, T4, T5, T6, T7, R : Any> combine(
        flow: Flow<T1>,
        flow2: Flow<T2>,
        flow3: Flow<T3>,
        flow4: Flow<T4>,
        flow5: Flow<T5>,
        flow6: Flow<T6>,
        flow7: Flow<T7>,
        transform: suspend (T1, T2, T3, T4, T5, T6, T7) -> R
    ): Flow<R> = combine(flow, flow2, flow3, flow4, flow5, flow6, flow7) { args: Array<*> ->
        transform(
            args[0] as T1,
            args[1] as T2,
            args[2] as T3,
            args[3] as T4,
            args[4] as T5,
            args[5] as T6,
            args[6] as T7,
        )
    }
    /**
     * Returns a [Flow] whose values are generated with [transform] function by combining
     * the most recently emitted values by each flow.
     */
    @Suppress("UNCHECKED_CAST")
    fun <T1, T2, T3, T4, T5, T6, T7, T8, R : Any> combine(
        flow: Flow<T1>,
        flow2: Flow<T2>,
        flow3: Flow<T3>,
        flow4: Flow<T4>,
        flow5: Flow<T5>,
        flow6: Flow<T6>,
        flow7: Flow<T7>,
        flow8: Flow<T8>,
        transform: suspend (T1, T2, T3, T4, T5, T6, T7, T8) -> R
    ): Flow<R> = combine(flow, flow2, flow3, flow4, flow5, flow6, flow7, flow8) { args: Array<*> ->
        transform(
            args[0] as T1,
            args[1] as T2,
            args[2] as T3,
            args[3] as T4,
            args[4] as T5,
            args[5] as T6,
            args[6] as T7,
            args[7] as T8,
        )
    }

    fun <R> work(vararg msg: R?) = run { if (BuildConfig.DEBUG) Log.d("funguj", msg.joinToString()) }
    inline fun <reified T : Any?, reified R : Any?, reified S : Any?> T.work(vararg msg: R, transform: T.() -> S): T =
        also { UtilFunctions.work(this.transform(), *msg) }

    inline fun <reified T : Any?, reified R : Any?> T.work(vararg msg: R): T = also { work(*msg, transform = { this }) }
    inline fun <reified T : Any?, reified S : Any?> T.work(transform: T.() -> S = { this as S }): T =
        also { work(*emptyArray<Any?>(), transform = transform) }

    inline fun <reified T : Any?> T.work(): T = also { work(*emptyArray<Any?>(), transform = { this }) }

    fun kotlin.time.Duration.toDelay() = run {
        val sign = when {
            inWholeSeconds < 0 -> "-"
            inWholeSeconds > 0 -> "+"
            else -> ""
        }
        val min = inWholeMinutes.absoluteValue
        val s = inWholeSeconds.absoluteValue % 60
        "$sign$min min $s s"
    }

    @Composable
    fun colorOfDelayText(delay: Float) = when {
        delay < 0 -> Color(0xFF343DFF)
        delay >= 4.5 -> Color.Red
        delay >= 1.5 -> Color(0xFFCC6600)
        else -> Color.Green
    }

    @Composable
    fun colorOfDelayBubbleText(delay: Float) = when {
        delay < 0 -> Color(0xFF0000EF)
        delay >= 4.5 -> MaterialTheme.colorScheme.onErrorContainer
        delay >= 1.5 -> Color(0xFFffddaf)
        else -> Color(0xFFADF0D8)
    }

    @Composable
    fun colorOfDelayBubbleContainer(delay: Float) = when {
        delay < 0 -> Color(0xFFE0E0FF)
        delay >= 4.5 -> MaterialTheme.colorScheme.errorContainer
        delay >= 1.5 -> Color(0xFF614000)
        else -> Color(0xFF015140)
    }

    fun Offset(x: Float = 0F, y: Float = 0F) = androidx.compose.ui.geometry.Offset(x, y)

    @ExperimentalMaterial3Api
    @Composable
    fun IconWithTooltip(
        imageVector: ImageVector,
        contentDescription: String?,
        modifier: Modifier = Modifier,
        tooltipText: String? = contentDescription,
        tint: Color = LocalContentColor.current,
    ) = if (tooltipText != null) TooltipBox(
        tooltip = {
            DPMCBTheme(
                useDarkTheme = isSystemInDarkTheme(),
                useDynamicColor = true,
                theme = Theme.Yellow,
                doTheThing = false,
            ) {
                PlainTooltip {
                    Text(text = tooltipText)
                }
            }
        },
        state = rememberTooltipState(),
        positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider()
    ) {
        Icon(
            imageVector = imageVector,
            contentDescription = contentDescription,
            modifier = modifier,
            tint = tint
        )
    }
    else
        Icon(
            imageVector = imageVector,
            contentDescription = contentDescription,
            modifier = modifier,
            tint = tint
        )

    inline fun <reified T, R> Iterable<Flow<T>>.combine(crossinline transform: suspend (List<T>) -> R) =
        combine(this) { transform(it.toList()) }

    fun String?.toTimeWeirdly() = (this?.run {
        LocalTime.of(slice(0..1).toInt(), slice(2..3).toInt())!!
    } ?: now)

    fun String?.toTime() = (this?.run {
        val list = split(":").map(String::toInt)
        LocalTime.of(list[0], list[1])!!
    } ?: now)

    fun String.toTimeOrNull() = this.run {
        val list = split(":").map(String::toIntOrNull)
        LocalTime.of(list.getOrNull(0) ?: return@run null, list.getOrNull(1) ?: return@run null)
    }

    fun String.toDateWeirdly() = LocalDate.of(slice(4..7).toInt(), slice(2..3).toInt(), slice(0..1).toInt())!!

    val Context.diagramFile get() = File(filesDir, "schema.pdf")

    val Context.isOnline: Boolean
        get() {
            val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val networkCapabilities = connectivityManager.activeNetwork ?: return false
            val activeNetwork = connectivityManager.getNetworkCapabilities(networkCapabilities) ?: return false

            return activeNetwork.hasTransport(
                NetworkCapabilities.TRANSPORT_WIFI
            ) || activeNetwork.hasTransport(
                NetworkCapabilities.TRANSPORT_CELLULAR
            ) || activeNetwork.hasTransport(
                NetworkCapabilities.TRANSPORT_ETHERNET
            )
        }

    @OptIn(ExperimentalTypeInference::class)
    fun <T> Flow<T>.compare(initial: T, @BuilderInference comparation: suspend (oldValue: T, newValue: T) -> T): Flow<T> {
        var oldValue: T = initial
        return flow {
            emit(oldValue)
            collect { newValue ->
                oldValue = comparation(oldValue, newValue)
                emit(oldValue)
            }
        }
    }

    fun <T> T.nullable(): T? = this

    fun LocalDate.asString() = "$dayOfMonth. $monthValue. $year"

    val now get() = LocalTime.now().truncatedTo(ChronoUnit.MINUTES)!!
    private val exactlyNow get() = LocalTime.now().truncatedTo(ChronoUnit.SECONDS)!!

    val nowFlow = flow {
        while (currentCoroutineContext().isActive) {
            delay(500)
            emit(exactlyNow)
        }
    }
        .flowOn(Dispatchers.IO)
        .stateIn(MainScope(), SharingStarted.WhileSubscribed(5_000), now)

    operator fun LocalTime.plus(duration: kotlin.time.Duration) = plus(duration.toJavaDuration())!!
    operator fun LocalDate.plus(duration: kotlin.time.Duration) = plusDays(duration.inWholeDays)!!

    inline val NavHostController.navigateFunction get() = { it: Route -> this.navigate(it.work()) }
    inline val NavHostController.navigateToRouteFunction get() = { it: String -> this.navigate(it.work()) }
//    inline val DestinationsNavigator.navigateFunction: (Direction) -> Unit
//        @Composable get() {
//            val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
//            return navigate@{ it: Direction ->
//                if (!lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.INITIALIZED)) return@navigate
//                this.navigate(it.work { route })
//            }
//        }

    fun List<Boolean>.allTrue() = all { it }
    fun List<Boolean>.anyTrue() = any { it }

    @Composable
    fun Settings.darkMode(): Boolean {
        return if (dmAsSystem) isSystemInDarkTheme() else dm
    }

    context(LazyListScope)
    fun rowItem(
        modifier: Modifier = Modifier,
        key: Any? = null,
        contentType: Any? = null,
        horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
        verticalAlignment: Alignment.Vertical = Alignment.Top,
        content: @Composable context(LazyItemScope) RowScope.() -> Unit,
    ) {
        item(
            key = key,
            contentType = contentType
        ) {
            Row(
                modifier = modifier,
                horizontalArrangement = horizontalArrangement,
                verticalAlignment = verticalAlignment
            ) {
                content(this)
            }
        }
    }

    context(LazyListScope)
    fun columnItem(
        modifier: Modifier = Modifier,
        key: Any? = null,
        contentType: Any? = null,
        verticalArrangement: Arrangement.Vertical = Arrangement.Top,
        horizontalAlignment: Alignment.Horizontal = Alignment.Start,
        content: @Composable context(LazyItemScope) ColumnScope.() -> Unit,
    ) {
        item(
            key = key,
            contentType = contentType
        ) {
            Column(
                modifier = modifier,
                verticalArrangement = verticalArrangement,
                horizontalAlignment = horizontalAlignment
            ) {
                content(this)
            }
        }
    }

    context(LazyListScope)
    fun textItem(
        text: String,
        modifier: Modifier = Modifier,
        key: Any? = null,
        contentType: Any? = null,
        color: Color = Color.Unspecified,
        fontSize: TextUnit = TextUnit.Unspecified,
        fontStyle: FontStyle? = null,
        fontWeight: FontWeight? = null,
        fontFamily: FontFamily? = null,
        letterSpacing: TextUnit = TextUnit.Unspecified,
        textDecoration: TextDecoration? = null,
        textAlign: TextAlign? = null,
        lineHeight: TextUnit = TextUnit.Unspecified,
        overflow: TextOverflow = TextOverflow.Clip,
        softWrap: Boolean = true,
        maxLines: Int = Int.MAX_VALUE,
        minLines: Int = 1,
        onTextLayout: (TextLayoutResult) -> Unit = {},
        style: TextStyle? = null,
    ) {
        item(
            key = key,
            contentType = contentType
        ) {
            Text(
                text = text,
                modifier = modifier,
                color = color,
                fontSize = fontSize,
                fontStyle = fontStyle,
                fontWeight = fontWeight,
                fontFamily = fontFamily,
                letterSpacing = letterSpacing,
                textDecoration = textDecoration,
                textAlign = textAlign,
                lineHeight = lineHeight,
                overflow = overflow,
                softWrap = softWrap,
                maxLines = maxLines,
                minLines = minLines,
                onTextLayout = onTextLayout,
                style = style ?: LocalTextStyle.current
            )
        }
    }

    fun Int.regN() = if ("$this".length == 1) "0$this" else "$this"

    val noCode = LocalDate.of(1970, 1, 1)!!

    fun Int.two() = plus(100).toString().takeLast(2)
}

typealias NavigateFunction = (Route) -> Unit
typealias NavigateBackFunction<R> = (R) -> Unit

typealias MutateListFunction<T> = (MutateListLambda<T>) -> Unit
typealias MutateListLambda<T> = MutableList<T>.() -> Unit

typealias MutateFunction<T> = (MutateLambda<T>) -> Unit
typealias MutateLambda<T> = (T) -> T