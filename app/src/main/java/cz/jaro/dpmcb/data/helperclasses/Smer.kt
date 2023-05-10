package cz.jaro.dpmcb.data.helperclasses

enum class Smer {
    POZITIVNI, NEGATIVNI;

    companion object {
        fun fromBoolean(bool: Boolean) = if (bool) POZITIVNI else NEGATIVNI
    }
}
