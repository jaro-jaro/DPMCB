@file:OptIn(InternalAPI::class)

package cz.jaro.dpmcb.data.entities

import cz.jaro.dpmcb.data.helperclasses.atLeastDigits
import cz.jaro.dpmcb.data.helperclasses.toLastDigits
import io.ktor.utils.io.InternalAPI
import io.ktor.utils.io.JvmSerializable
import kotlinx.serialization.Serializable
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract
import kotlin.jvm.JvmInline

typealias StopNumber = Int
typealias LineStopNumber = Int
typealias SequenceGroup = Int
typealias SequenceGroupCompanion = Int.Companion
typealias StopName = String
typealias Platform = String
typealias RegistrationPlate = String

@Serializable
@JvmInline
value class Table(val value: String) {
    override fun toString() = value
}

@Serializable
@JvmInline
value class LongLine(val value: Int) : Comparable<LongLine> {
    override fun toString() = value.toString()
    override fun compareTo(other: LongLine) = value.compareTo(other.value)
}

@Serializable
@JvmInline
value class ShortLine(val value: Int) : Comparable<ShortLine> {
    override fun toString() = value.toString()
    override fun compareTo(other: ShortLine) = value.compareTo(other.value)
}

typealias BusNumber = Int

@Serializable
@JvmInline
value class BusName(val value: String) : JvmSerializable {
    override fun toString() = value
}

@Serializable
@JvmInline
value class RegistrationNumber(val value: Int) : Comparable<RegistrationNumber> {
    override fun compareTo(other: RegistrationNumber) = value.compareTo(other.value)
    override fun toString() = value.atLeastDigits(2)
}

@Serializable
@JvmInline
value class SequenceCode(val value: String) {
    override fun toString() = value
}

typealias UnknownBusName = BusName

typealias SequenceModifiers = String

@OptIn(ExperimentalContracts::class)
@Suppress("USELESS_IS_CHECK")
fun BusName.isUnknown(): Boolean {
    contract {
        returns(true) implies (this@isUnknown is UnknownBusName)
    }
    return value.substringBefore('/').length <= 3
}

fun SequenceCode.withPart(part: Int) = SequenceCode(
    when {
        !hasModifiers() -> "$value-$part"
        !modifiers().hasPart() -> "$value$part"
        else -> value.dropLast(1) + part
    }
)

fun SequenceCode.withoutPart() = SequenceCode(
    when {
        !hasModifiers() -> value
        !modifiers().hasPart() -> value
        !modifiers().hasType() -> generic().value
        else -> value.dropLast(1)
    }
)

fun SequenceCode.withType(type: Char) = SequenceCode(
    when {
        !hasModifiers() -> "$value-$type"
        !modifiers().hasPart() -> value.dropLast(1) + type
        else -> generic().value + '-' + type + modifiers().part()
    }
)
fun SequenceCode.withoutType() = SequenceCode(
    when {
        !hasModifiers() -> value
        !modifiers().hasType() -> value
        !modifiers().hasPart() -> generic().value
        else -> generic().value + '-' + modifiers().part()
    }
)

val LongLine.Companion.invalid get() = LongLine(-1)
fun LongLine.isInvalid() = this == LongLine.invalid
fun LongLine?.isInvalid() = this == null || isInvalid()
val SequenceGroupCompanion.invalid: SequenceGroup get() = -1
fun SequenceGroup.isInvalid() = this == SequenceGroup.invalid
val SequenceCode.Companion.invalid get() = SequenceCode("0/0")
fun SequenceCode.isInvalid() = this == SequenceCode.invalid

fun SequenceCode.modifiers(): SequenceModifiers = value.substringAfter('-', "")
fun SequenceCode.hasModifiers() = '-' in value
fun SequenceModifiers.hasPart() = isNotEmpty() && last().isDigit()
fun SequenceModifiers.part(): Int? = if (hasPart()) last().digitToInt() else null
fun SequenceModifiers.hasType() = isNotEmpty() && first().isLetter()
fun SequenceModifiers.typeChar() = if (hasType()) first() else null
fun SequenceCode.generic() = SequenceCode(value.substringBefore('-'))
fun SequenceCode.line() = generic().value.substringAfter('/')
fun SequenceCode.sequenceNumber() = generic().value.substringBefore('/').toIntOrNull()
fun Table.line() = value.substringBefore('-').toLongLine()
fun Table.number() = value.substringAfter('-').toInt()
fun BusName.line() = value.substringBefore('/').toLongLine()
fun BusName.shortLine() = value.substringBefore('/').toLastDigits(3).toShortLine()
fun BusName.bus(): BusNumber = value.substringAfter('/').toInt()
fun BusName(line: LongLine, bus: BusNumber) = BusName("${line.value.toLastDigits(6)}/$bus")
fun Table(line: LongLine, number: Int) = Table("${line.value.toLastDigits(6)}-$number")
operator fun LongLine.div(number: BusNumber) = BusName(this, number)
operator fun LongLine.div(number: CharSequence) = this / number.toString().toInt()
operator fun CharSequence.div(number: CharSequence) = toLongLine() / number
fun CharSequence.toRegNum() = RegistrationNumber(toString().toInt())
fun CharSequence.toLongLine() = LongLine(toString().toInt())
fun CharSequence.toShortLine() = ShortLine(toString().toInt())
fun LongLine.toShortLine() = value.toLastDigits(3).toShortLine()