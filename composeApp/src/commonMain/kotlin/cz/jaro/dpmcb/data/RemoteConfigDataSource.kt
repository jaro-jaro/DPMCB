package cz.jaro.dpmcb.data

import cz.jaro.dpmcb.data.entities.LongLine
import cz.jaro.dpmcb.data.entities.RegistrationNumber
import cz.jaro.dpmcb.data.entities.SequenceCode
import cz.jaro.dpmcb.data.entities.SequenceModifiers
import cz.jaro.dpmcb.data.entities.generic
import cz.jaro.dpmcb.data.entities.hasPart
import cz.jaro.dpmcb.data.entities.hasType
import cz.jaro.dpmcb.data.entities.line
import cz.jaro.dpmcb.data.entities.modifiers
import cz.jaro.dpmcb.data.entities.part
import cz.jaro.dpmcb.data.entities.sequenceNumber
import cz.jaro.dpmcb.data.entities.typeChar
import cz.jaro.dpmcb.data.entities.types.VehicleType
import cz.jaro.dpmcb.data.helperclasses.IO
import cz.jaro.dpmcb.data.helperclasses.SequenceType
import cz.jaro.dpmcb.data.helperclasses.Traction
import cz.jaro.dpmcb.data.helperclasses.unaryPlus
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.FirebaseApp
import dev.gitlive.firebase.remoteconfig.FirebaseRemoteConfigException
import dev.gitlive.firebase.remoteconfig.get
import dev.gitlive.firebase.remoteconfig.remoteConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import kotlin.time.Duration.Companion.hours

interface GlobalSettingsDataSource {
    val sequenceConnections: Flow<List<Pair<SequenceCode, SequenceCode>>>
    val dividedSequencesWithMultipleBuses: Flow<List<SequenceCode>>
    val vehiclesTraction: StateFlow<Map<Traction, List<ClosedRange<RegistrationNumber>>>?>
    val linesTraction: StateFlow<Map<Traction, List<LongLine>>?>
    val sequenceTypes: Flow<Map<Char, SequenceType>>
    val vehicleNames: StateFlow<Map<RegistrationNumber, String>?>
}

class RemoteConfigDataSource(
    onlineManager: UserOnlineManager,
    firebase: FirebaseApp,
) : UserOnlineManager by onlineManager, GlobalSettingsDataSource {
    private val scope = CoroutineScope(Dispatchers.IO)

    private val remoteConfig = Firebase.remoteConfig(firebase)

    private suspend fun getConfigActive() = try {
        remoteConfig.settings {
            minimumFetchInterval = 1.hours
        }
        if (!isOnline())
            remoteConfig.activate()
        else
            remoteConfig.fetchAndActivate()
    } catch (e: FirebaseRemoteConfigException) {
        e.printStackTrace()
        recordException(e)
        try {
            remoteConfig.activate()
        } catch (_: FirebaseRemoteConfigException) {
            false
        }
    }

    private val configActive = ::getConfigActive.asFlow()

    override val vehiclesTraction = configActive.map {
        Json.decodeFromString(
            MapSerializer(Traction.serializer(), ListSerializer(RegistrationNumberRangeSerializer)),
            remoteConfig["traction"],
        )
    }.stateIn(scope, SharingStarted.Eagerly, null)

    override val linesTraction = configActive.map {
        Json.decodeFromString<Map<Traction, List<LongLine>>>(remoteConfig["lineTraction"])
    }.stateIn(scope, SharingStarted.Eagerly, null)

    override val vehicleNames = configActive.map {
        Json.decodeFromString<Map<RegistrationNumber, String>>(remoteConfig["vehicleNames"])
    }.stateIn(scope, SharingStarted.Eagerly, null)

    override val sequenceTypes = configActive.map {
        Json.decodeFromString<Map<Char, SequenceType>>(remoteConfig["sequenceTypes"])
            .mapValues { (char, type) -> type.copy(char = char) }
    }

    override val sequenceConnections = configActive.map {
        Json.decodeFromString<List<List<SequenceCode>>>(remoteConfig["sequenceConnections"]).map { Pair(it[0], it[1]) }
    }

    override val dividedSequencesWithMultipleBuses = configActive.map {
        Json.decodeFromString<List<SequenceCode>>(remoteConfig["dividedSequencesWithMultipleBuses"])
    }

    object RegistrationNumberRangeSerializer : KSerializer<ClosedRange<RegistrationNumber>> {
        override val descriptor: SerialDescriptor get() = PrimitiveSerialDescriptor("RegistrationNumberRange", PrimitiveKind.STRING)
        override fun deserialize(decoder: Decoder) =
            decoder.decodeString().split("..").map(String::toInt).map(::RegistrationNumber).let { it[0]..it[1] }

        override fun serialize(encoder: Encoder, value: ClosedRange<RegistrationNumber>) = encoder.encodeString(value.toString())
    }
}

context(m: GlobalSettingsDataSource)
suspend fun SequenceModifiers.type() = typeChar()?.let { m.sequenceTypes.first()[it] }

fun GlobalSettingsDataSource.lineTraction(line: LongLine, type: VehicleType) =
    linesTraction.value?.entries?.find { (_, lines) -> line in lines }?.key
        ?: when (type) {
            VehicleType.TROLEJBUS -> Traction.Trolleybus
            VehicleType.AUTOBUS -> Traction.Diesel
            else -> Traction.Other
        }

fun GlobalSettingsDataSource.vehicleTraction(vehicle: RegistrationNumber) =
    vehiclesTraction.value?.entries?.find { (_, ranges) ->
        ranges.any { range -> vehicle in range }
    }?.key

fun GlobalSettingsDataSource.vehicleName(vehicle: RegistrationNumber) = vehicleNames.value?.get(vehicle)

suspend fun GlobalSettingsDataSource.getSequenceComparator(): Comparator<SequenceCode> {
    val sequenceTypes = sequenceTypes.first()

    return compareBy<SequenceCode> {
        0
    }.thenBy {
        it.modifiers().typeChar()?.let { type ->
            sequenceTypes[type]?.order
        } ?: 0
    }.thenBy {
        it.line().toIntOrNull() ?: 20
    }.thenBy {
        it.sequenceNumber()
    }.thenBy {
        it.modifiers().part()
    }
}

context(m: GlobalSettingsDataSource)
suspend fun SequenceCode.seqName() = let {
    val m = modifiers()
    val (typeNominative, typeGenitive) = m.type()?.let { type ->
        type.nominative to type.genitive
    } ?: ("" to "")
    buildString {
        if (m.hasPart()) +"${m.part()}. část "
        if (m.hasPart() && m.hasType()) +"$typeGenitive "
        if (!m.hasPart() && m.hasType()) +"$typeNominative "
        +generic().value
    }
}

context(m: GlobalSettingsDataSource)
suspend fun SequenceCode.seqConnection() = "Potenciální návaznost na " + let {
    val m = modifiers()
    val (validityAccusative, typeGenitive) = m.type()?.let { type ->
        type.accusative to type.genitive
    } ?: ("" to "")
    buildString {
        if (m.hasPart()) +"${m.part()}. část "
        if (m.hasPart() && m.hasType()) +"$typeGenitive "
        if (!m.hasPart() && m.hasType()) +"$validityAccusative "
        +generic().value
    }
}