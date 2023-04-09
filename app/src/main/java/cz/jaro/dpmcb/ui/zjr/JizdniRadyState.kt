package cz.jaro.dpmcb.ui.zjr

import cz.jaro.dpmcb.data.realtions.OdjezdNizkopodlaznostSpojId

sealed interface JizdniRadyState {
    object Loading : JizdniRadyState
    data class Success(val data: List<OdjezdNizkopodlaznostSpojId>) : JizdniRadyState
}