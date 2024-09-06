package cz.jaro.dpmcb.data.entities.types

enum class Direction {
    POSITIVE, NEGATIVE;

    companion object {
        operator fun Companion.invoke(bool: Boolean) = if (bool) POSITIVE else NEGATIVE
    }
}
