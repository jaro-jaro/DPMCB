package cz.jaro.dpmcb.data

import cz.jaro.dpmcb.data.entities.RegistrationNumber
import cz.jaro.dpmcb.data.helperclasses.IO
import cz.jaro.dpmcb.data.helperclasses.Traction
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.FirebaseApp
import dev.gitlive.firebase.remoteconfig.FirebaseRemoteConfigException
import dev.gitlive.firebase.remoteconfig.get
import dev.gitlive.firebase.remoteconfig.remoteConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asFlow
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
    val vehiclesTraction: StateFlow<Map<Traction, List<ClosedRange<RegistrationNumber>>>?>
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

    override val vehicleNames = configActive.map {
        Json.decodeFromString<Map<RegistrationNumber, String>>(remoteConfig["vehicleNames"])
    }.stateIn(scope, SharingStarted.Eagerly, null)

    object RegistrationNumberRangeSerializer : KSerializer<ClosedRange<RegistrationNumber>> {
        override val descriptor: SerialDescriptor get() = PrimitiveSerialDescriptor("RegistrationNumberRange", PrimitiveKind.STRING)
        override fun deserialize(decoder: Decoder) =
            decoder.decodeString().split("..").map(String::toInt).map(::RegistrationNumber).let { it[0]..it[1] }

        override fun serialize(encoder: Encoder, value: ClosedRange<RegistrationNumber>) = encoder.encodeString(value.toString())
    }
}

fun GlobalSettingsDataSource.vehicleTraction(vehicle: RegistrationNumber) =
    vehiclesTraction.value?.entries?.find { (_, ranges) ->
        ranges.any { range -> vehicle in range }
    }?.key

fun GlobalSettingsDataSource.vehicleName(vehicle: RegistrationNumber) = vehicleNames.value?.get(vehicle)
