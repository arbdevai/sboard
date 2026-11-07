package com.sahalos.sboard.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = OledBlackAccent,
    secondary = OledBlackSecondaryText,
    tertiary = Color(0xFF10B981),
    background = OledBlackBackground,
    surface = OledBlackSurface,
    surfaceVariant = OledBlackSurfaceVariant,
    onPrimary = Color.Black,
    onSecondary = Color.Black,
    onTertiary = Color.Black,
    onBackground = OledBlackText,
    onSurface = OledBlackText,
    onSurfaceVariant = OledBlackSecondaryText
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
