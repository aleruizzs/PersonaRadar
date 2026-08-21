package com.personaradar.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val PersonaColorScheme = darkColorScheme(
    primary = PersonaRed,
    onPrimary = PersonaWhite,
    primaryContainer = PersonaCrimson,
    onPrimaryContainer = PersonaYellow,
    secondary = PersonaYellow,
    onSecondary = PersonaBlack,
    secondaryContainer = PersonaDarkGray,
    onSecondaryContainer = PersonaWhite,
    background = PersonaBlack,
    onBackground = PersonaWhite,
    surface = PersonaGraphite,
    onSurface = PersonaWhite,
    surfaceVariant = PersonaDarkGray,
    onSurfaceVariant = PersonaLightGray,
    error = PersonaRed,
    onError = PersonaWhite
)

@Composable
fun PersonaRadarTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = PersonaColorScheme,
        typography = Typography,
        content = content
    )
}
