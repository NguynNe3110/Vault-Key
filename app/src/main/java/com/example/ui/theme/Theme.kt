package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Light = lightColorScheme(
    primary = Teal, onPrimary = Color.White, primaryContainer = TealLight, onPrimaryContainer = Color(0xFF104F43),
    secondary = Ink, onSecondary = Color.White, secondaryContainer = Color(0xFFE8EBE4), onSecondaryContainer = Ink,
    tertiary = Color(0xFF715014), tertiaryContainer = Color(0xFFFFE7B4), onTertiaryContainer = Color(0xFF513600),
    background = Paper, onBackground = Ink, surface = Color.White, onSurface = Ink,
    surfaceVariant = Color(0xFFEDF1EC), onSurfaceVariant = MutedInk, outline = Color(0xFF73847E),
    outlineVariant = Line, error = Color(0xFFB42330), errorContainer = Color(0xFFFFE3E5))
private val Dark = darkColorScheme(
    primary = Color(0xFF94D7BD), onPrimary = Color(0xFF073C2F), primaryContainer = Color(0xFF174E3F), onPrimaryContainer = Color(0xFFB5EED5),
    secondary = Color(0xFFD8E2DC), onSecondary = Ink, secondaryContainer = Color(0xFF2E3E39), onSecondaryContainer = Color(0xFFE1EAE4),
    tertiary = Amber, tertiaryContainer = Color(0xFF55401A), onTertiaryContainer = Color(0xFFFFE3A7),
    background = Color(0xFF101C1B), onBackground = Color(0xFFE6EFEB), surface = Color(0xFF182724), onSurface = Color(0xFFE6EFEB),
    surfaceVariant = Color(0xFF243530), onSurfaceVariant = Color(0xFFB5C7BF), outline = Color(0xFF849A90),
    outlineVariant = Color(0xFF3E544A), error = Color(0xFFFFB2B7), errorContainer = Color(0xFF68222D))

@Composable
fun MyApplicationTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (darkTheme) Dark else Light, typography = Typography, content = content)
}
