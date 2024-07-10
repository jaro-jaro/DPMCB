package cz.jaro.dpmcb.data.entities

import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.toLastDigits
import kotlinx.serialization.Serializable
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

@Serializable
@JvmInline
value class StopNumber(val value: Int) { override fun toString() = value.toString() }
@Serializable
@JvmInline
value class Table(val value: String)
@Serializable
@JvmInline
value class LongLine(val value: Int) { override fun toString() = value.toString() }

@Serializable
@JvmInline
value class ShortLine(val value: Int): Comparable<ShortLine> {
    companion object {
        val invalid get() = ShortLine(-1)
    }

    fun isInvalid() = this == invalid
    override fun toString() = value.toString()
    override fun compareTo(other: ShortLine) = value.compareTo(other.value)
}

fun ShortLine?.isInvalid() = this == null || isInvalid()

@Serializable
@JvmInline
value class BusNumber(val value: Int) { override fun toString() = value.toString() }
@Serializable
@JvmInline
value class BusName(val value: String)
@Serializable
@JvmInline
value class RegistrationNumber(val value: Int): Comparable<RegistrationNumber> {
    override fun compareTo(other: RegistrationNumber) = value.compareTo(other.value)
}
@Serializable
@JvmInline
value class SequenceCode(val value: String)

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
fun SequenceCode.modifiers(): SequenceModifiers = value.substringAfter('-', "")
fun SequenceCode.hasModifiers() = '-' in value
fun SequenceModifiers.hasPart() = isNotEmpty() && last().isDigit()
fun SequenceModifiers.part() = if (hasPart()) last().digitToInt() else null
fun SequenceModifiers.hasType() = isNotEmpty() && first().isLetter()
fun SequenceModifiers.typeChar() = if (hasType()) first() else null
fun SequenceCode.generic() = SequenceCode(value.substringBefore('-'))
fun SequenceCode.line() = generic().value.substringAfter('/')
fun SequenceCode.sequenceNumber() = generic().value.substringBefore('/')
fun Table.line() = value.substringBefore('-').toLongLine()
fun Table.number() = value.substringAfter('-').toInt()
fun BusName.line() = value.substringBefore('/').toLongLine()
fun BusName.shortLine() = value.substringBefore('/').toLastDigits(3).toShortLine()
fun BusName.bus() = BusNumber(value.substringAfter('/').toInt())
fun BusName(line: LongLine, bus: BusNumber) = BusName("${line.value.toLastDigits(6)}/$bus")
fun Table(line: LongLine, number: Int) = Table("${line.value.toLastDigits(6)}-$number")
infix fun LongLine.slash(number: BusNumber) = BusName(this, number)
infix fun LongLine.slash(number: String) = slash(number.toBusNumber())
infix fun String.slash(number: String) = this.toLongLine() slash number.toBusNumber()
//inline fun UnknownBusName(line: ShortLine, bus: BusNumber) = UnknownBusName("${line.value.toLastDigits(3)}/$bus")
fun String.toStopNumber() = StopNumber(toInt())
fun String.toBusNumber() = BusNumber(toInt())
fun String.toLongLine() = LongLine(toInt())
fun String.toShortLine() = ShortLine(toInt())
fun LongLine.toShortLine() = value.toLastDigits(3).toShortLine()
