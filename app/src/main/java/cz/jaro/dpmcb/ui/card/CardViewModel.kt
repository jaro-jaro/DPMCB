package cz.jaro.dpmcb.ui.card

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.ManagedActivityResultLauncher
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
import org.koin.android.annotation.KoinViewModel
import org.koin.core.annotation.InjectedParam
import kotlin.time.Duration.Companion.seconds

@KoinViewModel
class CardViewModel(
    private val repo: SpojeRepository,
    @InjectedParam private val params: Parameters,
) : ViewModel() {

    data class Parameters(
        val getPickMultipleMedia: (callback: (Uri?) -> Unit) -> ManagedActivityResultLauncher<PickVisualMediaRequest, Uri?>
    )

    val hasCard = repo.hasCard
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5.seconds), null)

    fun addCard() {
        params.getPickMultipleMedia {
            it?.let {
                viewModelScope.launch {
                    repo.copyFile(it, repo.cardFile)
                    repo.changeCard(true)
                }
            }
        }.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }

    val card = hasCard.filterNotNull().map {
        if (it) BitmapFactory.decodeFile(repo.cardFile.path).asImageBitmap() else null
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5.seconds), null)
}
