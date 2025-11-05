package cz.jaro.dpmcb.data

import io.ktor.client.HttpClient
import io.ktor.client.plugins.onDownload
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText

class FileStorageManager() : AutoCloseable {
    private val client = HttpClient()
    private val proxyUrl = "https://ygbqqztfvcnqxxbqvxwb.supabase.co/functions/v1/storage-proxy"

    suspend fun getObject(
        path: String,
        progress: (Float?) -> Unit,
    ) = client.get("$proxyUrl?path=$path") {
        onDownload { bytesSentTotal, contentLength ->
            progress(contentLength?.let { bytesSentTotal.toFloat() / it })
        }
    }

    override fun close() = client.close()
}

suspend fun FileStorageManager.getText(
    path: String,
    progress: (Float?) -> Unit,
) = getObject(path, progress).bodyAsText()