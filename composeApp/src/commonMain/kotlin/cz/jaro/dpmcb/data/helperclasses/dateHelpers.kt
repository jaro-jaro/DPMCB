@file:Suppress("MemberVisibilityCanBePrivate", "unused")
@file:OptIn(ExperimentalTime::class)

package cz.jaro.dpmcb.data.helperclasses

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.DateTimePeriod
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atDate
import kotlinx.datetime.atTime
import kotlinx.datetime.number
import kotlinx.datetime.periodUntil
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.todayIn
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.ExperimentalTime

fun LocalDate.toCzechLocative() = when (SystemClock.todayHere().durationUntil(this).inWholeDays) {
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

fun LocalDate.toCzechAccusative() = when (SystemClock.todayHere().durationUntil(this).inWholeDays) {
    0L -> "dnešek"
    1L -> "zítřek"
    2L -> "pozítří"
    in 3L..6L -> dayOfWeek.cz()
    else -> asString()
}

fun LocalDateTime.plus(duration: Duration, timeZone: TimeZone = DefaultTimeZone) = toInstant(timeZone).plus(duration).toLocalDateTime(timeZone)
fun LocalDateTime.plus(period: DateTimePeriod, timeZone: TimeZone = DefaultTimeZone) = toInstant(timeZone).plus(period, timeZone).toLocalDateTime(timeZone)
operator fun LocalDateTime.plus(duration: Duration) = plus(duration, DefaultTimeZone)
operator fun LocalDateTime.plus(period: DateTimePeriod) = plus(period, DefaultTimeZone)

fun LocalTime.plus(duration: Duration, date: LocalDate, timeZone: TimeZone = DefaultTimeZone) = atDate(date).plus(duration, timeZone).time
operator fun LocalTime.plus(duration: Duration) = atDate(SystemClock.todayHere()).plus(duration).time
fun LocalTime.plus(period: DateTimePeriod, date: LocalDate, timeZone: TimeZone = DefaultTimeZone) = atDate(date).plus(period, timeZone).time
operator fun LocalTime.plus(period: DateTimePeriod) = atDate(SystemClock.todayHere()).plus(period).time

fun LocalDate.plus(period: DatePeriod, timeZone: TimeZone = DefaultTimeZone) = atTime(LocalTime.Companion.Noon).plus(period, timeZone).date
operator fun LocalDate.plus(period: DatePeriod) = plus(period, DefaultTimeZone)
fun LocalDate.plus(duration: Duration, timeZone: TimeZone = DefaultTimeZone) = plus(duration.toDatePeriod(), timeZone)
operator fun LocalDate.plus(duration: Duration) = plus(duration.toDatePeriod())

fun LocalDateTime.minus(duration: Duration, timeZone: TimeZone = DefaultTimeZone) = toInstant(timeZone).minus(duration).toLocalDateTime(timeZone)
operator fun LocalDateTime.minus(duration: Duration) = minus(duration, DefaultTimeZone)

fun LocalTime.minus(duration: Duration, date: LocalDate, timeZone: TimeZone = DefaultTimeZone) = atDate(date).minus(duration, timeZone).time
operator fun LocalTime.minus(duration: Duration) = atDate(SystemClock.todayHere()).minus(duration).time

fun LocalDate.minus(duration: Duration, timeZone: TimeZone = DefaultTimeZone) = atTime(LocalTime.Companion.Noon).minus(duration, timeZone).date
operator fun LocalDate.minus(duration: Duration) = minus(duration, DefaultTimeZone)

fun LocalDateTime.until(other: LocalDateTime, timeZone: TimeZone = DefaultTimeZone) = other.toInstant(timeZone).minus(toInstant(timeZone))
operator fun LocalDateTime.minus(other: LocalDateTime) = other.until(this, DefaultTimeZone)

fun LocalTime.until(other: LocalTime, date: LocalDate, timeZone: TimeZone = DefaultTimeZone) = atDate(date).until(other.atDate(date), timeZone)
operator fun LocalTime.minus(other: LocalTime) = other.until(this, SystemClock.todayHere())

fun LocalDate.durationUntil(other: LocalDate, timeZone: TimeZone = DefaultTimeZone) =
    atTime(LocalTime.Companion.Noon).until(other.atTime(LocalTime.Companion.Noon), timeZone)

operator fun LocalDate.minus(other: LocalDate) = other.periodUntil(this)

private val LocalTime.Companion.Noon get() = LocalTime(0, 0)

private fun Duration.truncatedToDays() = inWholeDays.days
private fun Duration.toDatePeriod() = DatePeriod(days = truncatedToDays().inWholeDays.toInt())

fun LocalDate.asString() = "$day. ${month.number}. $year"

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

val now get() = SystemClock.timeHere().let { LocalTime(it.hour, it.minute) }
val exactlyNow get() = SystemClock.timeHere().let { LocalTime(it.hour, it.minute, it.second) }

val nowFlow = ::exactlyNow
    .asRepeatingFlow()
    .flowOn(Dispatchers.IO)
    .stateIn(MainScope(), SharingStarted.WhileSubscribed(5_000), exactlyNow)

fun Clock.timeIn(timeZone: TimeZone) = nowIn(timeZone).time
fun Clock.nowIn(timeZone: TimeZone) = now().toLocalDateTime(timeZone)
fun Clock.nowHere() = nowIn(DefaultTimeZone)
val SystemClock get() = Clock.System
fun Clock.todayHere() = todayIn(DefaultTimeZone)
fun Clock.timeHere() = timeIn(DefaultTimeZone)
val DefaultTimeZone get() = TimeZone.currentSystemDefault()

val Duration.inSeconds get() = inWholeMilliseconds / 1000F
val Duration.inMinutes get() = inSeconds / 60F
val Duration.inHours get() = inMinutes / 60F
val Duration.inDays get() = inHours / 24F

fun DayOfWeek.cz() = when (this) {
    DayOfWeek.MONDAY -> "pondělí"
    DayOfWeek.TUESDAY -> "úterý"
    DayOfWeek.WEDNESDAY -> "středu"
    DayOfWeek.THURSDAY -> "čtvrtek"
    DayOfWeek.FRIDAY -> "pátek"
    DayOfWeek.SATURDAY -> "sobotu"
    DayOfWeek.SUNDAY -> "neděli"
}