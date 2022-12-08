package cz.jaro.dpmcb.data

import android.content.Context
import android.net.ConnectivityManager
import android.util.Log
import android.widget.Toast
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

    suspend inline fun <reified T : Any> ziskatData(url: String): T? {

        if (!isOnline) withContext(Dispatchers.Main) {
            Toast.makeText(ctx, "Nemáte připojení k internetu", Toast.LENGTH_LONG).show()
        }

        val response = try {
            withContext(Dispatchers.IO) {
                Jsoup
                    .connect(baseUrl + url)
                    .ignoreContentType(true)
                    .maxBodySize(0)
                    .execute()
            }
        } catch (e: SocketTimeoutException) {
            return null
        }

        Log.d("Doprava API", "$url: ${response.statusCode()} ${response.statusMessage()}")

        if (response.statusCode() == 200) {
            val text = response.body()

            return Gson().fromJson<T>(text, object : TypeToken<T>() {}.type)
        }

        return null
    }
}
