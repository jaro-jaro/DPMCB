package cz.jaro.dpmcb.data.entities.types

enum class Direction {
    POSITIVE, NEGATIVE;
}

fun Boolean.toDirection() = if (this) Direction.POSITIVE else Direction.NEGATIVE
