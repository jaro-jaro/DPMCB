package cz.jaro.dpmcb.ui.card

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.contract.ActivityResultContract
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

fun interface GenericActivityResultLauncher<I, O> {
    fun launch(input: I, callback: ActivityResultCallback<O>)
}

@Composable
fun <I, O> rememberResultLauncher(contract: ActivityResultContract<I, O>): GenericActivityResultLauncher<I, O> {
    var cb by remember { mutableStateOf(ActivityResultCallback<O> {}) }

    val launcher = rememberLauncherForActivityResult(contract) {
        cb.onActivityResult(it)
    }

    return remember {
        GenericActivityResultLauncher { input, callback ->
            cb = callback
            launcher.launch(input)
        }
    }
}