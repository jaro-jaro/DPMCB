package cz.jaro.dpmcb.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable

actual fun areDynamicColorsSupported() = false

@Composable
actual fun dynamicDarkColorScheme(): ColorScheme = error("Not supported")

@Composable
actual fun dynamicLightColorScheme(): ColorScheme = error("Not supported")