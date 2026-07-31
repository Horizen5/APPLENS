package com.applens.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color

private val AppColorScheme = lightColorScheme(
    primary = AccentBlue,
    onPrimary = Color.White,
    secondary = AccentPurple,
    onSecondary = Color.White,
    background = BgColor,
    onBackground = TextPrimary,
    surface = CardColor,
    onSurface = TextPrimary,
    surfaceVariant = CardColor,
    onSurfaceVariant = TextSecondary,
    outline = DividerColor,
    error = StatusRed,
)

@Composable
fun AppLensTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = AppColorScheme,
        content = content
    )
}
