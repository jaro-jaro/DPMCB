package cz.jaro.dpmcb.ui.theme

import androidx.compose.material3.ColorScheme

enum class Theme(
    val lightColorScheme: ColorScheme,
    val darkColorScheme: ColorScheme,
    val jmeno: String,
) {
    Red(
        lightColorScheme = LightColorsRed,
        darkColorScheme = DarkColorsRed,
        jmeno = "Červené",
    ),
    Green(
        lightColorScheme = LightColorsGreen,
        darkColorScheme = DarkColorsGreen,
        jmeno = "Zelené",
    ),
    Blue(
        lightColorScheme = LightColorsBlue,
        darkColorScheme = DarkColorsBlue,
        jmeno = "Modré",
    ),
    Yellow(
        lightColorScheme = LightColorsYellow,
        darkColorScheme = DarkColorsYellow,
        jmeno = "Žluté",
    ),
    Cyan(
        lightColorScheme = LightColorsCyan,
        darkColorScheme = DarkColorsCyan,
        jmeno = "Tyrkysové",
    ),
    Magenta(
        lightColorScheme = LightColorsMagenta,
        darkColorScheme = DarkColorsMagenta,
        jmeno = "Maǧentové",
    ),
}