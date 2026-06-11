package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
    primary = PrimaryColor,
    onPrimary = LightSurface,
    secondary = SecondaryAccent,
    onSecondary = LightSurface,
    tertiary = PrimaryHover,
    background = LightBg,
    onBackground = TextDark,
    surface = LightSurface,
    onSurface = TextDark,
    surfaceVariant = LightCard,
    onSurfaceVariant = TextGray,
    outline = LightBorder
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = false, // Force Light theme standard
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = Typography,
        content = content
    )
}
