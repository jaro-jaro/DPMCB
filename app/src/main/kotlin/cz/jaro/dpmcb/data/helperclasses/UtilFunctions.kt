@file:Suppress("MemberVisibilityCanBePrivate", "unused")

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
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
import androidx.navigation.NavOptions
import com.google.firebase.Firebase
import com.google.firebase.crashlytics.crashlytics
import cz.jaro.dpmcb.BuildConfig
import cz.jaro.dpmcb.data.Settings
import cz.jaro.dpmcb.ui.main.Route
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.datetime.Clock
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.DateTimePeriod
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atDate
import kotlinx.datetime.atTime
import kotlinx.datetime.periodUntil
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.todayIn
import java.io.File
import kotlin.experimental.ExperimentalTypeInference
import kotlin.math.absoluteValue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.milliseconds

object UtilFunctions {

    fun LocalDate.toCzechLocative() = when (durationUntil(SystemClock.todayHere()).inWholeDays) {
        0L -> "dnes"
        1L -> "zítra"
        2L -> "pozítří"
        in 3L..6L -> when (dayOfWeek) {
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

    fun LocalDate.toCzechAccusative() = when (durationUntil(SystemClock.todayHere()).inWholeDays) {
        0L -> "dnešek"
        1L -> "zítřek"
        2L -> "pozítří"
        in 3L..6L -> when (dayOfWeek) {
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

    fun Duration.toDelay() = run {
        val sign = when {
            inWholeSeconds < 0 -> "-"
            inWholeSeconds > 0 -> "+"
            else -> ""
        }
        val min = inWholeMinutes.absoluteValue
        val s = inWholeSeconds.absoluteValue % 60
        "$sign$min min $s s"
    }

    @ReadOnlyComposable
    @Composable
    fun colorOfDelayText(delay: Float) = when {
        delay < 0 -> Color(0xFF343DFF)
        delay >= 4.5 -> Color.Red
        delay >= 1.5 -> Color(0xFFCC6600)
        else -> Color.Green
    }

    @ReadOnlyComposable
    @Composable
    fun colorOfDelayBubbleText(delay: Float) = when {
        delay < 0 -> Color(0xFF0000EF)
        delay >= 4.5 -> MaterialTheme.colorScheme.onErrorContainer
        delay >= 1.5 -> Color(0xFFffddaf)
        else -> Color(0xFFADF0D8)
    }

    @ReadOnlyComposable
    @Composable
    fun colorOfDelayBubbleContainer(delay: Float) = when {
        delay < 0 -> Color(0xFFE0E0FF)
        delay >= 4.5 -> MaterialTheme.colorScheme.errorContainer
        delay >= 1.5 -> Color(0xFF614000)
        else -> Color(0xFF015140)
    }

    fun Offset(x: Float = 0F, y: Float = 0F) = androidx.compose.ui.geometry.Offset(x, y)

    inline fun <reified T, R> Iterable<Flow<T>>.combine(crossinline transform: suspend (List<T>) -> R) =
        combine(this) { transform(it.toList()) }

    fun String?.toTimeWeirdly() = (this?.run {
        LocalTime(slice(0..1).toInt(), slice(2..3).toInt())
    } ?: now)

    fun String?.toTime() = (this?.run {
        val list = split(":").map(String::toInt)
        LocalTime(list[0], list[1])
    } ?: now)

    fun String.toTimeOrNull() = this.run {
        val list = split(":").map(String::toIntOrNull)
        LocalTime(list.getOrNull(0) ?: return@run null, list.getOrNull(1) ?: return@run null)
    }

    fun String.toDateWeirdly() = LocalDate(slice(4..7).toInt(), slice(2..3).toInt(), slice(0..1).toInt())

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

    fun LocalDate.asString() = "$dayOfMonth. $monthNumber. $year"

    val now get() = SystemClock.timeHere().let { LocalTime(it.hour, it.minute) }
    val exactlyNow get() = SystemClock.timeHere().let { LocalTime(it.hour, it.minute, it.second) }

    val nowFlow = ::exactlyNow
        .asRepeatingFlow()
        .flowOn(Dispatchers.IO)
        .stateIn(MainScope(), SharingStarted.WhileSubscribed(5_000), exactlyNow)

    fun <T> (() -> T).asRepeatingFlow(duration: Duration = 500.milliseconds): Flow<T> = flow {
        while (currentCoroutineContext().isActive) {
            emit(invoke())
            delay(duration)
        }
    }
    fun <T> (suspend () -> T).asRepeatingFlow(duration: Duration = 500.milliseconds): Flow<T> = flow {
        while (currentCoroutineContext().isActive) {
            emit(invoke())
            delay(duration)
        }
    }

    fun LocalDateTime.plus(duration: Duration, timeZone: TimeZone = DefaultTimeZone) = toInstant(timeZone).plus(duration).toLocalDateTime(timeZone)
    fun LocalDateTime.plus(period: DateTimePeriod, timeZone: TimeZone = DefaultTimeZone) = toInstant(timeZone).plus(period, timeZone).toLocalDateTime(timeZone)
    operator fun LocalDateTime.plus(duration: Duration) = plus(duration, DefaultTimeZone)
    operator fun LocalDateTime.plus(period: DateTimePeriod) = plus(period, DefaultTimeZone)

    fun LocalTime.plus(duration: Duration, date: LocalDate, timeZone: TimeZone = DefaultTimeZone) = atDate(date).plus(duration, timeZone).time
    operator fun LocalTime.plus(duration: Duration) = atDate(SystemClock.todayHere()).plus(duration).time
    fun LocalTime.plus(period: DateTimePeriod, date: LocalDate, timeZone: TimeZone = DefaultTimeZone) = atDate(date).plus(period, timeZone).time
    operator fun LocalTime.plus(period: DateTimePeriod) = atDate(SystemClock.todayHere()).plus(period).time

    fun LocalDate.plus(period: DatePeriod, timeZone: TimeZone = DefaultTimeZone) = atTime(LocalTime.Noon).plus(period, timeZone).date
    operator fun LocalDate.plus(period: DatePeriod) = plus(period, DefaultTimeZone)
    fun LocalDate.plus(duration: Duration, timeZone: TimeZone = DefaultTimeZone) = plus(duration.toDatePeriod(), timeZone)
    operator fun LocalDate.plus(duration: Duration) = plus(duration.toDatePeriod())

    fun LocalDateTime.minus(duration: Duration, timeZone: TimeZone = DefaultTimeZone) = toInstant(timeZone).minus(duration).toLocalDateTime(timeZone)
    operator fun LocalDateTime.minus(duration: Duration) = minus(duration, DefaultTimeZone)

    fun LocalTime.minus(duration: Duration, date: LocalDate, timeZone: TimeZone = DefaultTimeZone) = atDate(date).minus(duration, timeZone).time
    operator fun LocalTime.minus(duration: Duration) = atDate(SystemClock.todayHere()).minus(duration).time

    fun LocalDate.minus(duration: Duration, timeZone: TimeZone = DefaultTimeZone) = atTime(LocalTime.Noon).minus(duration, timeZone).date
    operator fun LocalDate.minus(duration: Duration) = minus(duration, DefaultTimeZone)

    fun LocalDateTime.until(other: LocalDateTime, timeZone: TimeZone = DefaultTimeZone) = other.toInstant(timeZone).minus(toInstant(timeZone))
    operator fun LocalDateTime.minus(other: LocalDateTime) = other.until(this, DefaultTimeZone)

    fun LocalTime.until(other: LocalTime, date: LocalDate, timeZone: TimeZone = DefaultTimeZone) = atDate(date).until(other.atDate(date), timeZone)
    operator fun LocalTime.minus(other: LocalTime) = other.until(this, SystemClock.todayHere())

    fun LocalDate.durationUntil(other: LocalDate, timeZone: TimeZone = DefaultTimeZone) = atTime(LocalTime.Noon).until(other.atTime(LocalTime.Noon), timeZone)
    operator fun LocalDate.minus(other: LocalDate) = other.periodUntil(this)

    private val LocalTime.Companion.Noon get() = LocalTime(0, 0)

    inline fun <T, K1, K2> Iterable<T>.groupByPair(keySelector: (T) -> Pair<K1, K2>): List<Triple<K1, K2, List<T>>> =
        groupBy(keySelector)
            .map { Triple(it.key.first, it.key.second, it.value) }

    inline fun <T, K1, K2, V> Iterable<T>.groupByPair(keySelector: (T) -> Pair<K1, K2>, valueTransform: (T) -> V) =
        groupBy(keySelector, valueTransform)
            .map { Triple(it.key.first, it.key.second, it.value) }

//    inline val NavHostController.navigateFunction get() = { it: Route -> this.navigate(it.work()) }
    inline val NavHostController.navigateToRouteFunction get() = { it: String -> this.navigate(it.work()) }
    inline val NavHostController.navigateFunction: NavigateFunction
        get() {
            val f = navigateWithOptionsFunction
            return { route: Route ->
                f(route, null)
            }
        }
    inline val NavHostController.navigateWithOptionsFunction: NavigateWithOptionsFunction
        get() {
            return navigate@{ route: Route, navOptions: NavOptions? ->
                try {
                    this.navigate(route.work(), navOptions)
                } catch (e: IllegalStateException) {
                    e.printStackTrace()
                    Firebase.crashlytics.log("Pokus o navigaci na $route")
                    Firebase.crashlytics.recordException(e)
                }
            }
        }

    fun List<Boolean>.allTrue() = all { it }
    fun List<Boolean>.anyTrue() = any { it }

    @Composable
    @ReadOnlyComposable
    fun Settings.darkMode(): Boolean {
        return if (dmAsSystem) isSystemInDarkTheme() else dm
    }

    /**
     * Performs the given [action] on each element.
     */
    suspend inline fun <T> ReceiveChannel<T>.forEach(action: (T) -> Unit) {
        for (element in this) action(element)
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

    val noCode = LocalDate(1970, 1, 1)

    fun Int.two() = toLastDigits(2)

    fun Int.atLeastDigits(amount: Int) = toString().atLeastDigits(amount)
    fun String.atLeastDigits(amount: Int) = "0" * (amount - length) + this
    fun Int.toLastDigits(amount: Int) = toString().toLastDigits(amount)
    fun String.toLastDigits(amount: Int) = atLeastDigits(amount).takeLast(amount)
    operator fun String.times(times: Int) = buildString {
        if (times <= 0) return@buildString
        repeat(times) {
            append(this@times)
        }
    }

    fun <T, U: Comparable<T>> List<U>.sorted() = sortedWith(compareBy { it })

//    private fun LocalTime.copy(
//        hour: Int = this.hour,
//        minute: Int = this.minute,
//        second: Int = this.second,
//        nanosecond: Int = this.nanosecond,
//    ) = LocalTime(hour, minute, second, nanosecond)
    private fun Duration.truncatedToDays() = inWholeDays.days
    private fun Duration.toDatePeriod() = DatePeriod(days = truncatedToDays().inWholeDays.toInt())

    inline fun <T> Iterable<T>.indexOfFirstOrNull(predicate: (T) -> Boolean): Int? {
        for ((index, item) in this.withIndex()) {
            if (predicate(item)) return index
        }
        return null
    }
}

typealias NavigateFunction = (Route) -> Unit
typealias NavigateWithOptionsFunction = (Route, NavOptions?) -> Unit
typealias NavigateBackFunction<R> = (R) -> Unit

typealias MutateListFunction<T> = (MutateListLambda<T>) -> Unit
typealias MutateListLambda<T> = MutableList<T>.() -> Unit

typealias MutateFunction<T> = (MutateLambda<T>) -> Unit
typealias MutateLambda<T> = (T) -> T

operator fun NavigateWithOptionsFunction.invoke(route: Route) = invoke(route, null)

fun Clock.timeIn(timeZone: TimeZone) = nowIn(timeZone).time
fun Clock.nowIn(timeZone: TimeZone) = now().toLocalDateTime(timeZone)
fun Clock.nowHere() = nowIn(DefaultTimeZone)
val SystemClock get() = Clock.System
fun Clock.todayHere() = todayIn(DefaultTimeZone)
fun Clock.timeHere() = timeIn(DefaultTimeZone)
val DefaultTimeZone get() = TimeZone.currentSystemDefault()
