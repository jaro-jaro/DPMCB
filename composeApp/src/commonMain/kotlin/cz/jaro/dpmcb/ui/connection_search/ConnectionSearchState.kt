package cz.jaro.dpmcb.ui.connection_search

import cz.jaro.dpmcb.data.entities.StopName
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

data class ConnectionSearchState(
    val settings: SearchSettings,
    val settingsModified: Boolean,
    val history: List<SearchSettings>,
    val favourites: List<Favourite>,
)

typealias Favourite = List<FavouriteRelation>

@Serializable
data class FavouriteRelation(
    val start: StopName,
    val destination: StopName,
) {
    override fun toString() = "$start -> $destination"
}

infix fun StopName.to(other: StopName) = FavouriteRelation(this, other)

class FavouriteRelationSerializer : KSerializer<FavouriteRelation> {
    override val descriptor = PrimitiveSerialDescriptor("FavouriteRelation", PrimitiveKind.STRING)
    fun toString(value: FavouriteRelation) = "${value.start}~${value.destination}".replace(" ", "%20")
    fun fromString(value: String) = value.replace("%20", " ").split("~").let { it[0] to it[1] }
    override fun serialize(encoder: Encoder, value: FavouriteRelation) = encoder.encodeString(toString(value))
    override fun deserialize(decoder: Decoder) = fromString(decoder.decodeString())
}

@Serializable(with = RelationsSerializer::class)
data class Relations(val value: List<FavouriteRelation>)

class RelationsSerializer : KSerializer<Relations> {
    override val descriptor = PrimitiveSerialDescriptor("Relations", PrimitiveKind.STRING)
    private val child = FavouriteRelationSerializer()
    override fun deserialize(decoder: Decoder) = Relations(decoder.decodeString().split("+").map { child.fromString(it) })
    override fun serialize(encoder: Encoder, value: Relations) =
        encoder.encodeString(value.value.joinToString("+") { child.toString(it) })
}