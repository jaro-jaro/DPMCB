package cz.jaro.dpmcb.data

import android.content.Context
import android.util.Log
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.isOnline
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.jsoup.Jsoup


class DopravaApi(
    @PublishedApi internal val ctx: Context,
    @PublishedApi internal val baseUrl: String,
) {
    suspend inline fun <reified T : Any> ziskatData(url: String): T? = withContext(Dispatchers.IO) {

        if (!ctx.isOnline) return@withContext null

        val response = try {
            withContext(Dispatchers.IO) {
                Jsoup
                    .connect("$baseUrl$url")
                    .ignoreContentType(true)
                    .maxBodySize(0)
                    .execute()
            }
        } catch (e: Exception) {
            return@withContext null
        }

        Log.d("Doprava API", "$baseUrl$url: ${response.statusCode()} ${response.statusMessage()}")

        if (response.statusCode() == 200) {
            val text = response.body()

            return@withContext try {
                Json.decodeFromString<T>(text)
            } catch (e: SerializationException) {
                null
            }
        }

        return@withContext null
    }
}
