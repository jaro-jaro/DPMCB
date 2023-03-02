package cz.jaro.dpmcb.data.realtions

import cz.jaro.dpmcb.data.helperclasses.Datum

data class JedeOdDo(
    val jede: Boolean,
    val v: Datum.Companion.DatumRange,
)