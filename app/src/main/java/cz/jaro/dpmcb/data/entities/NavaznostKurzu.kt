package cz.jaro.dpmcb.data.entities

import androidx.room.Entity
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("NavaznostKurzu")
@Entity(primaryKeys = ["kurzPredtim", "kurzPotom"])
data class NavaznostKurzu(
    val kurzPredtim: String,
    val kurzPotom: String,
)