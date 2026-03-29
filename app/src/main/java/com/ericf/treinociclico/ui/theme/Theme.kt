package com.ericf.treinociclico.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColors = lightColorScheme(
    primary = ClayRed,
    onPrimary = Ivory,
    primaryContainer = Peach,
    onPrimaryContainer = Ink,
    secondary = Forest,
    onSecondary = Ivory,
    surface = Ivory,
    onSurface = Ink,
    surfaceVariant = Sand,
    onSurfaceVariant = Ink,
    outline = Slate,
)

private val DarkColors = darkColorScheme(
    primary = Peach,
    onPrimary = Ink,
    primaryContainer = ClayRed,
    onPrimaryContainer = Ivory,
    secondary = Forest,
    onSecondary = Ivory,
    surface = Deep,
    onSurface = Ivory,
    surfaceVariant = DeepOlive,
    onSurfaceVariant = Ivory,
    outline = Sand,
)

@Composable
fun TrainingTrackerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = AppTypography,
        content = content,
    )
}
