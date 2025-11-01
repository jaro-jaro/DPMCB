package cz.jaro.dpmcb.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color

enum class Theme(
    val lightColorScheme: ColorScheme,
    val darkColorScheme: ColorScheme,
    val seed: Color,
    val label: String,
) {
    Red(
        lightColorScheme = LightColorsRed,
        darkColorScheme = DarkColorsRed,
        seed = seed_red,
        label = "Červené",
    ),
    Green(
        lightColorScheme = LightColorsGreen,
        darkColorScheme = DarkColorsGreen,
        seed = seed_green,
        label = "Zelené",
    ),
    Blue(
        lightColorScheme = LightColorsBlue,
        darkColorScheme = DarkColorsBlue,
        seed = seed_blue,
        label = "Modré",
    ),
    Yellow(
        lightColorScheme = LightColorsYellow,
        darkColorScheme = DarkColorsYellow,
        seed = seed_yellow,
        label = "Žluté",
    ),
    Cyan(
        lightColorScheme = LightColorsCyan,
        darkColorScheme = DarkColorsCyan,
        seed = seed_cyan,
        label = "Tyrkysové",
    ),
    Magenta(
        lightColorScheme = LightColorsMagenta,
        darkColorScheme = DarkColorsMagenta,
        seed = seed_magenta,
        label = "Maǧentové",
    ),
    Orange(
        lightColorScheme = LightColorsOrange,
        darkColorScheme = DarkColorsOrange,
        seed = seed_orange,
        label = "Oranǧové",
    ),
    Pink(
        lightColorScheme = LightColorsPink,
        darkColorScheme = DarkColorsPink,
        seed = seed_pink,
        label = "Růžové",
    );
    companion object {
        val Default = Yellow
    }
}