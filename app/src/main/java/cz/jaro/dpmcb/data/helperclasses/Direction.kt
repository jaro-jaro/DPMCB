package cz.jaro.dpmcb.data.helperclasses

enum class Direction {
    POSITIVE, NEGATIVE;

    companion object {
        operator fun invoke(bool: Boolean) = if (bool) POSITIVE else NEGATIVE
    }
}
