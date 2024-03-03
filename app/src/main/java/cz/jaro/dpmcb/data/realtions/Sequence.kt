package cz.jaro.dpmcb.data.realtions

data class Sequence(
    val name: String,
    val before: List<String>,
    val buses: List<InfoStops>,
    val after: List<String>,
    val commonTimeCodes: List<RunsFromTo>,
    val commonFixedCodes: String,
)
