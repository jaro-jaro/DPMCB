package cz.jaro.dpmcb.ui.chooser

import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.runtime.Composable

@OptIn(ExperimentalLayoutApi::class)
actual val isImeVisible: Boolean
    @Composable get() = WindowInsets.isImeVisible