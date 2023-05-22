package cz.jaro.dpmcb.data.realtions

import java.time.LocalDate

data class Platnost(
    val platnostOd: LocalDate,
    val platnostDo: LocalDate,
)
