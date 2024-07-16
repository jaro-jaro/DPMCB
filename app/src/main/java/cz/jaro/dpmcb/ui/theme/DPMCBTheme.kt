package cz.jaro.dpmcb.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

@Composable
fun DPMCBTheme(
    useDarkTheme: Boolean,
    useDynamicColor: Boolean,
    theme: Theme,
    doTheThing: Boolean = true,
    content: @Composable () -> Unit,
) {
    val dynamicColor = useDynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    val colorScheme = when {
        dynamicColor -> when {
            useDarkTheme -> dynamicDarkColorScheme(LocalContext.current)
            else -> dynamicLightColorScheme(LocalContext.current)
        }

        else -> when {
            useDarkTheme -> theme.darkColorScheme
            else -> theme.lightColorScheme
        }
    }

    val view = LocalView.current
    if (doTheThing && !view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !useDarkTheme
        }
    }

    CompositionLocalProvider(
        LocalIsDynamicThemeUsed provides dynamicColor,
        LocalIsDarkThemeUsed provides useDarkTheme,
        LocalTheme provides theme.takeUnless { dynamicColor }
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            content = content,
        )
    }
}

val LocalTheme = staticCompositionLocalOf<Theme?> { error("CompositionLocal LocalTheme not present") }
val LocalIsDynamicThemeUsed = staticCompositionLocalOf<Boolean> { error("CompositionLocal LocalTheme not present") }
val LocalIsDarkThemeUsed = staticCompositionLocalOf<Boolean> { error("CompositionLocal LocalIsDarkThemeUsed not present") }
