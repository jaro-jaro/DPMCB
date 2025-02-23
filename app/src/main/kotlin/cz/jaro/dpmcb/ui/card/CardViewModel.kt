package cz.jaro.dpmcb.ui.card

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.graphics.asImageBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cz.jaro.dpmcb.data.SpojeRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.WhileSubscribed
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds

class CardViewModel(
    private val repo: SpojeRepository,
    private val params: Parameters,
) : ViewModel() {

    data class Parameters(
        val pickMediaLauncher: GenericActivityResultLauncher<PickVisualMediaRequest, Uri?>
    )

    val hasCard = repo.hasCard
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5.seconds), null)

    fun addCard() {
        params.pickMediaLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) {
            it?.let {
                viewModelScope.launch {
                    repo.copyFile(it, repo.cardFile)
                    repo.changeCard(true)
                }
            }
        }
    }

    val card = hasCard.filterNotNull().map {
        if (it) BitmapFactory.decodeFile(repo.cardFile.path).asImageBitmap() else null
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5.seconds), null)
}
