package com.sahalos.sboard.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = NordDarkAccent,
    secondary = NordDarkSecondaryText,
    tertiary = Color(0xFFA3BE8C),
    background = NordDarkBackground,
    surface = NordDarkSurface,
    onPrimary = Color(0xFF2E3440),
    onSecondary = Color(0xFF2E3440),
    onTertiary = Color(0xFF2E3440),
    onBackground = NordDarkText,
    onSurface = NordDarkText
)

private val LightColorScheme = lightColorScheme(
    primary = MaterialLightAccent,
    secondary = MaterialLightSecondaryText,
    tertiary = Color(0xFF188038),
    background = MaterialLightBackground,
    surface = MaterialLightSurface,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = MaterialLightText,
    onSurface = MaterialLightText
)

@Composable
fun SboardAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
