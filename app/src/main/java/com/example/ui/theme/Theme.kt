package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = NeonCyan,
    onPrimary = VaultObsidianDark,
    primaryContainer = VaultSurfaceVariant,
    onPrimaryContainer = NeonCyan,
    secondary = NeonAmber,
    onSecondary = VaultObsidianDark,
    secondaryContainer = VaultSurfaceVariant,
    onSecondaryContainer = NeonAmber,
    tertiary = NeonPlasmaPink,
    onTertiary = VaultObsidianDark,
    background = VaultObsidianDark,
    onBackground = TextPrimary,
    surface = VaultSurfaceDark,
    onSurface = TextPrimary,
    surfaceVariant = VaultSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    outline = VaultCardBorder,
    error = ErrorRed,
    onError = TextPrimary
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    // XP Music Vault Ultra is strictly an audiophile dark studio environment
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
