package cz.jaro.dpmcb.data.entities

enum class Direction {
    POSITIVE, NEGATIVE;

    companion object {
        operator fun Companion.invoke(bool: Boolean) = if (bool) POSITIVE else NEGATIVE
    }
}
