package cz.jaro.dpmcb.data

import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.VDP
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.emptyGraphZastavek
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.toChar
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.toVDP
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@kotlinx.serialization.Serializable
data class VsechnoOstatni(
    val verze: Int = -1,
//    @kotlinx.serialization.Serializable(with = FlowSerializer::class)
    val typDne: VDP = VDP.DNY,

    val linkyAJejichZastavky: Map<Int, List<String>> = emptyMap(),
    val zastavky: List<String> = emptyList(),

    val graphZastavek: GraphZastavek = emptyGraphZastavek(),
) {
    companion object FlowSerializer : KSerializer<Flow<VDP>> {
        override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("Flow", PrimitiveKind.CHAR)

        override fun serialize(encoder: Encoder, value: Flow<VDP>) {
            MainScope().launch {
                val vdp = value.first().toChar()
                encoder.encodeChar(vdp)
            }
        }

        override fun deserialize(decoder: Decoder): Flow<VDP> {
            val vdp = decoder.decodeChar()
            return flowOf(vdp.toVDP())
        }
    }
}

//@kotlinx.serialization.Serializable
//data class StareUplneVsechno(
//    val verze: Int = -1,
//    val datum: Datum = Datum.dnes,
//
//    val linky: List<Linka> = emptyList(),
//    val zastavky: List<String> = emptyList(),
//
//    val graphZastavek: GraphZastavek = emptyMap()
//) {
//    val spoje get() = linky.flatMap { it.spoje }
//    val zastavkySpoju get() = linky.flatMap { it.spoje }.flatMap { it.zastavkySpoje }
//}
