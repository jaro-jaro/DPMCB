package cz.jaro.dpmcb.ui.card

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.graphics.asImageBitmap
import cz.jaro.dpmcb.data.PreferenceDataSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.WhileSubscribed
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import kotlin.time.Duration.Companion.seconds

class AndroidCardManager(
    ctx: ComponentActivity,
    private val preferenceDataSource: PreferenceDataSource,
) : CardManager {
    val scope = CoroutineScope(Dispatchers.IO)

    val hasCard = preferenceDataSource.hasCard

    val cardFile = File(ctx.filesDir, "prukazka.jpg")

    private lateinit var callback: (Uri?) -> Unit
    private val launcher = ctx.registerForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { callback(it) }

    private val contentResolver = ctx.contentResolver

    override fun loadCard() {
        callback = callback@{
            if (it == null) return@callback
            scope.launch {
                contentResolver.openInputStream(it)!!.use { input ->
                    cardFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                preferenceDataSource.changeCard(true)
            }
        }
        launcher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }

    override fun removeCard() {
        scope.launch {
            preferenceDataSource.changeCard(false)
        }
    }

    override val card = hasCard.filterNotNull().map {
        if (it) BitmapFactory.decodeFile(cardFile.path).asImageBitmap() else null
    }.stateIn(scope, SharingStarted.WhileSubscribed(5.seconds), null)
}

actual fun supportsCard() = true