package cz.jaro.dpmcb.data.entities

enum class TimeCodeType(
    val code: Int,
) {
    Runs(1), RunsAlso(2), RunsOnly(3), DoesNotRun(4);

    companion object {
        val TimeCodeType.runs get() = this != DoesNotRun
    }
}