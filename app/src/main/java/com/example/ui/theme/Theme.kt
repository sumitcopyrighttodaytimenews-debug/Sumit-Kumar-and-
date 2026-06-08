package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = DarkGold,
    onPrimary = DarkBg,
    secondary = DarkGoldHover,
    onSecondary = DarkBg,
    tertiary = AccentBlue,
    background = DarkBg,
    onBackground = TextWhite,
    surface = DarkCard,
    onSurface = TextWhite,
    surfaceVariant = DarkSurface,
    onSurfaceVariant = TextLightGray,
    outline = DarkBorder
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Force premium dark mode standard
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
