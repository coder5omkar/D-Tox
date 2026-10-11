package com.dtox.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DToxScheme = darkColorScheme(
    primary = Color(0xFF7FD1AE),
    onPrimary = Color(0xFF06231A),
    primaryContainer = Color(0xFF17372B),
    onPrimaryContainer = Color(0xFFC6EEDC),
    secondary = Color(0xFF9FC7B7),
    onSecondary = Color(0xFF10221C),
    secondaryContainer = Color(0xFF1D332B),
    onSecondaryContainer = Color(0xFFD5E9E0),
    background = Color(0xFF0A1210),
    onBackground = Color(0xFFE2EFE9),
    surface = Color(0xFF0F1A16),
    onSurface = Color(0xFFE2EFE9),
    surfaceVariant = Color(0xFF182722),
    onSurfaceVariant = Color(0xFFA9C4B8),
    outline = Color(0xFF43584F),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
)

/** Calm, dark, single theme. No dynamic color — quiet by design. */
@Composable
fun DToxTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = DToxScheme, content = content)
}
