package cz.jaro.dpmcb.data

import android.content.Context
import android.net.ConnectivityManager
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import java.net.SocketTimeoutException


class DopravaApi(
    @PublishedApi internal val ctx: Context,
    @PublishedApi internal val baseUrl: String,
) {

    @PublishedApi
    internal val isOnline: Boolean
        get() {
            val connManager = ctx.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val networkInfo = connManager.activeNetworkInfo

            return networkInfo != null && networkInfo.isConnectedOrConnecting
        }

    suspend inline fun <reified T : Any> ziskatData(url: String): T? = withContext(Dispatchers.IO) {

        /*if (!isOnline) withContext(Dispatchers.Main) {
            Toast.makeText(ctx, "Nemáte připojení k internetu", Toast.LENGTH_LONG).show()
        }*/
        if (!isOnline) return@withContext null

        val response = try {
            withContext(Dispatchers.IO) {
                Jsoup
                    .connect("$baseUrl$url")
                    .ignoreContentType(true)
                    .maxBodySize(0)
                    .execute()
            }
        } catch (e: SocketTimeoutException) {
            return@withContext null
        }

        Log.d("Doprava API", "$baseUrl$url: ${response.statusCode()} ${response.statusMessage()}")

        if (response.statusCode() == 200) {
            val text = response.body()

            return@withContext Gson().fromJson<T>(text, object : TypeToken<T>() {}.type)
        }

        return@withContext null
    }
}
