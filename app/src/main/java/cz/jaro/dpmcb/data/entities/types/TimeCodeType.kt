package cz.jaro.dpmcb.data.entities.types

enum class TimeCodeType(
    val code: Int,
) {
    Runs(1), RunsAlso(2), RunsOnly(3), DoesNotRun(4);
}

val TimeCodeType.runs get() = this != TimeCodeType.DoesNotRun