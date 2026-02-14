package cz.jaro.dpmcb.data.entities.types

enum class TimeCodeType {
    Runs, RunsAlso, RunsOnly, DoesNotRun;

    companion object {
        val TimeCodeType.runs get() = this != DoesNotRun
    }
}