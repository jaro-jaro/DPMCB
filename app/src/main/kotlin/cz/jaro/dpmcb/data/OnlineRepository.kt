package cz.jaro.dpmcb.data

import android.content.Context
import android.util.Log
import com.gitlab.mvysny.konsumexml.KonsumerException
import com.gitlab.mvysny.konsumexml.konsumeXml
import com.gitlab.mvysny.konsumexml.textRecursively
import com.google.firebase.Firebase
import com.google.firebase.crashlytics.crashlytics
import cz.jaro.dpmcb.data.entities.BusName
import cz.jaro.dpmcb.data.entities.bus
import cz.jaro.dpmcb.data.entities.line
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.asRepeatingFlow
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.isOnline
import cz.jaro.dpmcb.data.jikord.MapData
import cz.jaro.dpmcb.data.jikord.OnlineConn
import cz.jaro.dpmcb.data.jikord.OnlineConnDetail
import cz.jaro.dpmcb.data.jikord.OnlineConnStop
import cz.jaro.dpmcb.data.jikord.Transmitter
import cz.jaro.dpmcb.data.jikord.toOnlineConn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.koin.core.annotation.Single
import kotlin.time.Duration.Companion.seconds

@Single
class OnlineRepository(
    private val repo: SpojeRepository,
    private val ctx: Context,
    private val onlineApi: OnlineApi,
) {
    private val scope = MainScope()

    private suspend fun getAllConns() =
        if (repo.isOnlineModeEnabled.value) withContext(Dispatchers.IO) {
            if (!ctx.isOnline) return@withContext null
            val data = """{"w":14.320215289916973,"s":48.88092891115194,"e":14.818033283081036,"n":49.076970164143134,"zoom":12,"showStops":false}"""
            val response = try {
                onlineApi.mapData(
                    headers = headers,
                    body = data.toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull()),
                )
            } catch (e: Exception) {
                e.printStackTrace()
                Firebase.crashlytics.recordException(e)
                return@withContext null
            }

            if (response.code() != 200) return@withContext null

            val text = response.body() ?: return@withContext null
            try {
                json.decodeFromString<MapData>(text.string())
            } catch (e: SerializationException) {
                e.printStackTrace()
                Firebase.crashlytics.recordException(e)
                null
            }
        }
            ?.transmitters
            ?.filter {
                it.cn?.startsWith("325") ?: false
            }
            ?.map(Transmitter::toOnlineConn)
            ?: emptyList()
        else emptyList()

    private val connDeatilFlowMap = mutableMapOf<BusName, SharedFlow<OnlineConnDetail?>>()

    private val json = Json {
        ignoreUnknownKeys = true
    }

    private val headers = mapOf(
        "authority" to "jih.mpvnet.cz",
        "accept" to "application/json, text/javascript, */*; q=0.01",
        "content" to "type: application/json; charset=UTF-8",
        "origin" to "https://jih.mpvnet.cz",
        "referer" to "https://jih.mpvnet.cz/jikord/map",
    )

    private suspend fun getConnDetail(busName: BusName) =
        if (repo.isOnlineModeEnabled.value) withContext(Dispatchers.IO) {

            if (!ctx.isOnline) return@withContext null
            val response = try {
                onlineApi.timetable(
                    headers = headers,
                    body = """{"num1":"${busName.line()}","num2":"${busName.bus()}","cat":"2"}"""
                        .toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull()),
                )
            } catch (e: Exception) {
                e.printStackTrace()
                Firebase.crashlytics.recordException(e)
                return@withContext null
            }

            Log.d("Doprava API", ": ${response.code()} ${response.message()}")

            if (response.code() != 200) return@withContext null

            val text = response.body() ?: return@withContext null

            text.string()
        }
            ?.ifBlank { null }
            //                        ?.replace("<tr class=\"tAlignCentre\"><td>&darr;</td><td><hr></td><td><hr></td><td><hr></td></tr>", "")
            ?.replace("&darr;", "")
            ?.replace("<hr>", "")
            ?.konsumeXml()
            ?.run {
                try {
                    var line = null as Int?
                    OnlineConnDetail(
                        stops = child("div") {
                            child("table") {
                                child("tr") {
                                    childrenText("th")
                                }
                                var i = -1
                                children("tr") {
                                    i++
                                    if (attributes.getValueOrNull("class") == "tAlignCentre") {
                                        line = i
                                        textRecursively()
                                        null
                                    } else
                                        OnlineConnStop()
                                }.filterNotNull()
                            }
                        },
                        nextStopIndex = line,
                    )
                } catch (e: KonsumerException) {
                    e.printStackTrace()
                    Firebase.crashlytics.recordException(e)
                    null
                }
            }
        else null

    private val connsFlow: SharedFlow<List<OnlineConn>> = ::getAllConns
        .asRepeatingFlow(5.seconds)
        .flowOn(Dispatchers.IO)
        .shareIn(
            scope = scope,
            started = SharingStarted.WhileSubscribed(),
            replay = 1
        )

    fun nowRunningBuses() =
        connsFlow

    private fun busDetail(busName: BusName) = connDeatilFlowMap.getOrPut(busName) {
        suspend { getConnDetail(busName) }
            .asRepeatingFlow(5.seconds)
            .flowOn(Dispatchers.IO)
            .shareIn(
                scope = scope,
                started = SharingStarted.WhileSubscribed(),
                replay = 1
            )
    }

    fun busByName(name: BusName) =
        busOnMapByName(name).combine(busDetail(name)) { onlineConn, connDetail ->
            onlineConn to connDetail
        }

    fun busOnMapByName(name: BusName) = nowRunningBuses().map { it.busOnMapByName(name) }
}

fun List<OnlineConn>.busOnMapByName(name: BusName) = find { it.name == name }