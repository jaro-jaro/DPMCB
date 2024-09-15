package cz.jaro.dpmcb.data.entities

import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.atLeastDigits
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.toLastDigits
import kotlinx.serialization.Serializable
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

typealias SequenceID = Int
typealias StopNumber = Int
typealias SequenceGroup = Int
typealias SequenceGroupCompanion = Int.Companion

@Serializable
@JvmInline
value class Table(val value: String) {
    override fun toString() = value
}

@Serializable
@JvmInline
value class LongLine(val value: Int) {
    override fun toString() = value.toString()
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
value class BusName(val value: String) {
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

fun SequenceCode.changePart(part: Int) = SequenceCode(
    when {
        !hasModifiers() -> "$value-$part"
        !modifiers().hasPart() -> "$value$part"
        else -> value.dropLast(1) + part
    }
)

val ShortLine.Companion.invalid get() = ShortLine(-1)
fun ShortLine.isInvalid() = this == ShortLine.invalid
fun ShortLine?.isInvalid() = this == null || isInvalid()
val SequenceGroupCompanion.invalid get() = -1
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
fun SequenceCode.sequenceNumber() = generic().value.substringBefore('/').toInt()
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