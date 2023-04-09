package cz.jaro.dpmcb.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import cz.jaro.datum_cas.Datum
import cz.jaro.dpmcb.data.helperclasses.TypLinky
import cz.jaro.dpmcb.data.helperclasses.TypVozidla

@Entity
data class Linka(
    @PrimaryKey val cislo: Int,
    val trasa: String,
    val typVozidla: TypVozidla,
    val typLinky: TypLinky,
    val maVyluku: Boolean,
    val platnostOd: Datum,
    val platnostDo: Datum,

    val kratkeCislo: Int = cislo - 325_000,
)
