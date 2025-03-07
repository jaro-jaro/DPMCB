package cz.jaro.dpmcb.ui.theme

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

actual fun areDynamicColorsSupported() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

@RequiresApi(Build.VERSION_CODES.S)
@Composable
actual fun dynamicDarkColorScheme() = dynamicDarkColorScheme(LocalContext.current)

@RequiresApi(Build.VERSION_CODES.S)
@Composable
actual fun dynamicLightColorScheme() = dynamicLightColorScheme(LocalContext.current)
