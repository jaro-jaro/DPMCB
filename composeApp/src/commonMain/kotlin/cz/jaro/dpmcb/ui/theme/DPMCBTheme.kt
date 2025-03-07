package cz.jaro.dpmcb.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import cz.jaro.dpmcb.data.Settings

@Composable
fun DPMCBTheme(
    useDarkTheme: Boolean,
    useDynamicColor: Boolean,
    theme: Theme,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        useDynamicColor && areDynamicColorsSupported() -> when {
            useDarkTheme -> dynamicDarkColorScheme()
            else -> dynamicLightColorScheme()
        }

        else -> when {
            useDarkTheme -> theme.darkColorScheme
            else -> theme.lightColorScheme
        }
    }

    CompositionLocalProvider(
        LocalIsDynamicThemeUsed provides (useDynamicColor && areDynamicColorsSupported()),
        LocalIsDarkThemeUsed provides useDarkTheme,
        LocalTheme provides theme.takeUnless { useDynamicColor && areDynamicColorsSupported() }
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

@Composable
fun DPMCBTheme(
    settings: Settings,
    doTheThing: Boolean = true,
    content: @Composable () -> Unit,
) = DPMCBTheme(
    useDarkTheme = settings.darkMode(),
    useDynamicColor = settings.dynamicColors,
    theme = settings.theme,
    content = content,
)

@Composable
@ReadOnlyComposable
fun Settings.darkMode() = if (dmAsSystem) isSystemInDarkTheme() else dm