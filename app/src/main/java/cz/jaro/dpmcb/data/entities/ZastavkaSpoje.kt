package cz.jaro.dpmcb.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import cz.jaro.dpmcb.data.DopravaRepository.Companion.upravit
import cz.jaro.dpmcb.data.helperclasses.Cas

@kotlinx.serialization.Serializable
@Entity
data class ZastavkaSpoje(
    @PrimaryKey val id: Long,

    val nazevZastavky: String,
    val idSpoje: Long,
    val cisloLinky: Int,
    val nazevKurzu: String,
    val nizkopodlaznost: Boolean,

    val cas: Cas,
    val indexNaLince: Int,

    val upravene: String = nazevZastavky.upravit(),
)