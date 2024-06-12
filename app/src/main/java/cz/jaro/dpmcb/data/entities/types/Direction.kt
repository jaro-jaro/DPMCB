package cz.jaro.dpmcb.data.entities.types

import cz.jaro.dpmcb.data.entities.types.Direction.NEGATIVE
import cz.jaro.dpmcb.data.entities.types.Direction.POSITIVE

enum class Direction {
    POSITIVE, NEGATIVE;

    companion object
}

operator fun Direction.Companion.invoke(bool: Boolean) = if (bool) POSITIVE else NEGATIVE
