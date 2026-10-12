package com.dtox.app.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/** Forest is the free default; Midnight and Paper are shop unlocks. */
enum class AppTheme(val id: String, val label: String, val light: Boolean) {
    FOREST("forest", "Forest", false),
    MIDNIGHT("midnight", "Midnight", false),
    PAPER("paper", "Paper", true);

    companion object {
        fun from(id: String): AppTheme = entries.firstOrNull { it.id == id } ?: FOREST
    }
}

private val ForestScheme = darkColorScheme(
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

private val MidnightScheme = darkColorScheme(
    primary = Color(0xFF93B4FF),
    onPrimary = Color(0xFF0B1633),
    primaryContainer = Color(0xFF1E2A44),
    onPrimaryContainer = Color(0xFFD6E2FF),
    secondary = Color(0xFFB0BEDC),
    onSecondary = Color(0xFF141B2C),
    secondaryContainer = Color(0xFF232C42),
    onSecondaryContainer = Color(0xFFDDE4F7),
    background = Color(0xFF0A0E1A),
    onBackground = Color(0xFFE4E9F7),
    surface = Color(0xFF121826),
    onSurface = Color(0xFFE4E9F7),
    surfaceVariant = Color(0xFF1B2434),
    onSurfaceVariant = Color(0xFFA9B4CC),
    outline = Color(0xFF3B4763),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
)

private val PaperScheme = lightColorScheme(
    primary = Color(0xFF39694F),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFCDEADB),
    onPrimaryContainer = Color(0xFF0B2B1D),
    secondary = Color(0xFF4E6355),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFD2E8DA),
    onSecondaryContainer = Color(0xFF0D1F16),
    background = Color(0xFFF7F4EE),
    onBackground = Color(0xFF2A2724),
    surface = Color(0xFFFFFDF8),
    onSurface = Color(0xFF2A2724),
    surfaceVariant = Color(0xFFEFEAE0),
    onSurfaceVariant = Color(0xFF6B645C),
    outline = Color(0xFFB3AA9C),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
)

val AppTheme.scheme: ColorScheme
    get() = when (this) {
        AppTheme.FOREST -> ForestScheme
        AppTheme.MIDNIGHT -> MidnightScheme
        AppTheme.PAPER -> PaperScheme
    }

/** Calm palettes, no dynamic color — quiet by design. */
@Composable
fun DToxTheme(theme: AppTheme, content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = theme.scheme, content = content)
}
