package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

@Composable
fun AudiowaveTheme(
    themeMode: AppThemeMode = AppThemeMode.EMERALD,
    content: @Composable () -> Unit
) {
    val colorScheme = when (themeMode) {
        AppThemeMode.EMERALD -> darkColorScheme(
            primary = EmeraldPrimary,
            onPrimary = Color.Black,
            primaryContainer = EmeraldSurfaceVariant,
            onPrimaryContainer = EmeraldPrimary,
            secondary = EmeraldSecondary,
            onSecondary = Color.White,
            secondaryContainer = EmeraldSurfaceElevated,
            onSecondaryContainer = EmeraldTertiary,
            tertiary = EmeraldTertiary,
            background = EmeraldDarkBg,
            onBackground = TextPrimary,
            surface = EmeraldSurface,
            onSurface = TextPrimary,
            surfaceVariant = EmeraldSurfaceVariant,
            onSurfaceVariant = TextSecondary,
            outline = EmeraldBorder
        )
        AppThemeMode.VELVET -> darkColorScheme(
            primary = VelvetPrimary,
            onPrimary = Color.White,
            primaryContainer = VelvetSurfaceVariant,
            onPrimaryContainer = VelvetTertiary,
            secondary = VelvetSecondary,
            onSecondary = Color.White,
            secondaryContainer = VelvetSurfaceElevated,
            onSecondaryContainer = VelvetPrimary,
            tertiary = VelvetTertiary,
            background = VelvetDarkBg,
            onBackground = TextPrimary,
            surface = VelvetSurface,
            onSurface = TextPrimary,
            surfaceVariant = VelvetSurfaceVariant,
            onSurfaceVariant = TextSecondary,
            outline = VelvetBorder
        )
        AppThemeMode.EMBERS -> darkColorScheme(
            primary = EmbersPrimary,
            onPrimary = Color.White,
            primaryContainer = EmbersSurfaceVariant,
            onPrimaryContainer = EmbersTertiary,
            secondary = EmbersSecondary,
            onSecondary = Color.White,
            secondaryContainer = EmbersSurfaceElevated,
            onSecondaryContainer = EmbersPrimary,
            tertiary = EmbersTertiary,
            background = EmbersDarkBg,
            onBackground = TextPrimary,
            surface = EmbersSurface,
            onSurface = TextPrimary,
            surfaceVariant = EmbersSurfaceVariant,
            onSurfaceVariant = TextSecondary,
            outline = EmbersBorder
        )
        AppThemeMode.CYBER -> darkColorScheme(
            primary = CyberPrimary,
            onPrimary = Color(0xFF00363D),
            primaryContainer = CyberSurfaceVariant,
            onPrimaryContainer = CyberPrimary,
            secondary = CyberSecondary,
            onSecondary = Color(0xFF2C0B56),
            secondaryContainer = CyberSurfaceElevated,
            onSecondaryContainer = CyberSecondary,
            tertiary = CyberTertiary,
            background = CyberDarkBg,
            onBackground = TextPrimary,
            surface = CyberSurface,
            onSurface = TextPrimary,
            surfaceVariant = CyberSurfaceVariant,
            onSurfaceVariant = TextSecondary,
            outline = CyberBorder
        )
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
