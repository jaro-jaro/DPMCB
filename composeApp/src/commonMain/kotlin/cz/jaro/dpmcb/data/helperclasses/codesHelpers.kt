package cz.jaro.dpmcb.data.helperclasses

import cz.jaro.dpmcb.data.entities.types.TimeCodeType
import cz.jaro.dpmcb.data.entities.types.TimeCodeType.DoesNotRun
import cz.jaro.dpmcb.data.entities.types.TimeCodeType.Runs
import cz.jaro.dpmcb.data.entities.types.TimeCodeType.RunsAlso
import cz.jaro.dpmcb.data.entities.types.TimeCodeType.RunsOnly
import cz.jaro.dpmcb.data.generated.Validity
import cz.jaro.dpmcb.data.realtions.RunsFromTo
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.Month
import kotlinx.datetime.plus

val noCode = LocalDate(1970, 1, 1)

fun isNoCode(it: RunsFromTo) = it.`in`.start == noCode && it.`in`.endInclusive == noCode
fun List<RunsFromTo>.removeNoCodes() = filterNot(::isNoCode)

fun filterFixedCodesAndMakeReadable(fixedCodes: String, timeCodes: List<RunsFromTo>) = fixedCodes
    .split(" ")
    .mapNotNull {
        when (it) {
            "X" -> "Jede v pracovních dnech"
            "+" -> "Jede v neděli a ve státem uznané svátky"
            "1" -> "Jede v pondělí"
            "2" -> "Jede v úterý"
            "3" -> "Jede ve středu"
            "4" -> "Jede ve čtvrtek"
            "5" -> "Jede v pátek"
            "6" -> "Jede v sobotu"
            "7" -> "Jede v neděli"
            "24" -> "Spoj s částečně bezbariérově přístupným vozidlem, nutná dopomoc průvodce"
            else -> null
        }
    }
    .takeUnless { timeCodes.any { it.type == RunsOnly } }
    .orEmpty()

fun filterTimeCodesAndMakeReadable(timeCodes: List<RunsFromTo>) = timeCodes.removeNoCodes()
    .groupBy({
        it.type
    }, {
        if (it.`in`.start != it.`in`.endInclusive) "od ${it.`in`.start.asString()} do ${it.`in`.endInclusive.asString()}" else it.`in`.start.asString()
    })
    .let {
        if (it.containsKey(RunsOnly)) mapOf(RunsOnly to it[RunsOnly]!!) else it
    }
    .map { (type, dates) ->
        when (type) {
            Runs -> "Jede "
            RunsAlso -> "Jede také "
            RunsOnly -> "Jede pouze "
            DoesNotRun -> "Nejede "
        } + dates.joinToString()
    }

fun validityString(validity: Validity) = "JŘ linky platí od ${validity.validFrom.asString()} do ${validity.validTo.asString()}"

fun LocalDate.runsToday(fixedCodes: String) = fixedCodes
    .split(" ")
    .mapNotNull {
        when (it) {
            "X" -> dayOfWeek in DayOfWeek.MONDAY..DayOfWeek.FRIDAY && !isPublicHoliday(this) // jede v pracovních dnech
            "+" -> dayOfWeek == DayOfWeek.SUNDAY || isPublicHoliday(this) // jede v neděli a ve státem uznané svátky
            "1" -> dayOfWeek == DayOfWeek.MONDAY // jede v pondělí
            "2" -> dayOfWeek == DayOfWeek.TUESDAY // jede v úterý
            "3" -> dayOfWeek == DayOfWeek.WEDNESDAY // jede ve středu
            "4" -> dayOfWeek == DayOfWeek.THURSDAY // jede ve čtvrtek
            "5" -> dayOfWeek == DayOfWeek.FRIDAY // jede v pátek
            "6" -> dayOfWeek == DayOfWeek.SATURDAY // jede v sobotu
            "7" -> dayOfWeek == DayOfWeek.SUNDAY // jede v neděli
            else -> null
        }
    }
    .ifEmpty { listOf(true) }
    .anyTrue()

// Je státní svátek nebo den pracovního klidu
fun isPublicHoliday(date: LocalDate) = listOf(
    LocalDate(1, 1, 1), // Den obnovy samostatného českého státu
    LocalDate(1, 1, 1), // Nový rok
    LocalDate(1, 5, 1), // Svátek práce
    LocalDate(1, 5, 8), // Den vítězství
    LocalDate(1, 7, 5), // Den slovanských věrozvěstů Cyrila a Metoděje,
    LocalDate(1, 7, 6), // Den upálení mistra Jana Husa
    LocalDate(1, 9, 28), // Den české státnosti
    LocalDate(1, 10, 28), // Den vzniku samostatného československého státu
    LocalDate(1, 11, 17), // Den boje za svobodu a demokracii
    LocalDate(1, 12, 24), // Štědrý den
    LocalDate(1, 12, 25), // 1. svátek vánoční
    LocalDate(1, 12, 26), // 2. svátek vánoční
).any {
    it.day == date.day && it.month == date.month
} || isEaster(date)

// Je Velký pátek nebo Velikonoční pondělí
fun isEaster(date: LocalDate): Boolean {
    val (bigFriday, easterMonday) = positionOfEasterInYear(date.year) ?: return false

    return date == easterMonday || date == bigFriday
}

val cahedEaster = mutableMapOf<Int, Pair<LocalDate, LocalDate>>()
// Poloha Velkého pátku a Velikonočního pondělí v roce
// Zdroj: https://cs.wikipedia.org/wiki/V%C3%BDpo%C4%8Det_data_Velikonoc#Algoritmus_k_v%C3%BDpo%C4%8Dtu_data
fun positionOfEasterInYear(year: Int): Pair<LocalDate, LocalDate>? {
    if (year in cahedEaster) return cahedEaster[year]

    val (m, n) = listOf(
        1583..1599 to (22 to 2),
        1600..1699 to (22 to 2),
        1700..1799 to (23 to 3),
        1800..1899 to (23 to 4),
        1900..1999 to (24 to 5),
        2000..2099 to (24 to 5),
        2100..2199 to (24 to 6),
        2200..2299 to (25 to 0),
    ).find { (years, _) ->
        year in years
    }?.second ?: return null

    val a = year % 19
    val b = year % 4
    val c = year % 7
    val d = (19 * a + m) % 30
    val e = (n + 2 * b + 4 * c + 6 * d) % 7

    val isException = (d == 29 && e == 6) || (d == 28 && e == 6 && a > 10)

    val eaterSundayFromTheStartOfMarch = (d + e) + if (isException) 15 else 22

    val bigFridayFromTheStartOfMarch = eaterSundayFromTheStartOfMarch - 2
    val bigFriday = LocalDate(year, Month.MARCH, 1) + (bigFridayFromTheStartOfMarch - 1).dayPeriod

    val easterMondayFromTheStartOfMarch = eaterSundayFromTheStartOfMarch + 1
    val easterMonday = LocalDate(year, Month.MARCH, 1) + (easterMondayFromTheStartOfMarch - 1).dayPeriod

    val result = bigFriday to easterMonday
    cahedEaster[year] = result
    return result
}

infix fun List<RunsFromTo>.anyAre(type: TimeCodeType) = any { it.type == type }
infix fun List<RunsFromTo>.noneAre(type: TimeCodeType) = none { it.type == type }
infix fun List<RunsFromTo>.filter(type: TimeCodeType) = filter { it.type == type }

infix fun List<RunsFromTo>.anySatisfies(date: LocalDate) = any { it satisfies date }
infix fun RunsFromTo.satisfies(date: LocalDate) = date in `in`

fun runsAt(
    timeCodes: List<RunsFromTo>,
    fixedCodes: String,
    date: LocalDate,
): Boolean = timeCodes.removeNoCodes().let { filteredCodes ->
    when {
        filteredCodes anyAre RunsOnly -> filteredCodes filter RunsOnly anySatisfies date
        filteredCodes filter RunsAlso anySatisfies date -> true
        !date.runsToday(fixedCodes) -> false
        filteredCodes filter DoesNotRun anySatisfies date -> false
        filteredCodes noneAre Runs -> true
        filteredCodes filter Runs anySatisfies date -> true
        else -> false
    }
}