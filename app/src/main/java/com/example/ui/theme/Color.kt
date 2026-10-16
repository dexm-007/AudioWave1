package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// Shared / Accent colors
val PureRed = Color(0xFFFF0033) // Vibrant Pure True Red for liked tracks (zero pink)
val PureRedDark = Color(0xFFD50000)

// 1. Emerald Green Palette (from user logo & palette image)
val EmeraldDarkBg = Color(0xFF09140E)
val EmeraldSurface = Color(0xFF112219)
val EmeraldSurfaceVariant = Color(0xFF193225)
val EmeraldSurfaceElevated = Color(0xFF224232)
val EmeraldPrimary = Color(0xFF25D366) // Vibrant Mint Emerald
val EmeraldSecondary = Color(0xFF60936C) // Sage Green (from image)
val EmeraldTertiary = Color(0xFFD1C766) // Gold (from image)
val EmeraldBorder = Color(0xFF244735)

// 2. Velvet Blue/Purple/Pink Palette (from user image #2)
val VelvetDarkBg = Color(0xFF0E0C1C)
val VelvetSurface = Color(0xFF1A1730)
val VelvetSurfaceVariant = Color(0xFF262044)
val VelvetSurfaceElevated = Color(0xFF332B5B)
val VelvetPrimary = Color(0xFF964EC2) // Violet (from image)
val VelvetSecondary = Color(0xFF50409A) // Deep Purple (from image)
val VelvetTertiary = Color(0xFFFF7BBF) // Pink (from image)
val VelvetBorder = Color(0xFF3B3363)

// 3. Embers Palette (from user image #3)
val EmbersDarkBg = Color(0xFF130E17)
val EmbersSurface = Color(0xFF211827)
val EmbersSurfaceVariant = Color(0xFF2F2137)
val EmbersSurfaceElevated = Color(0xFF3F2B49)
val EmbersPrimary = Color(0xFFF64668) // Coral Red (from image)
val EmbersSecondary = Color(0xFF984063) // Wine/Berry (from image)
val EmbersTertiary = Color(0xFFFE9677) // Peach (from image)
val EmbersBorder = Color(0xFF493355)

// 4. Cyber Neon Palette
val CyberDarkBg = Color(0xFF0A0C10)
val CyberSurface = Color(0xFF131722)
val CyberSurfaceVariant = Color(0xFF1B2130)
val CyberSurfaceElevated = Color(0xFF242C3F)
val CyberPrimary = Color(0xFF00E5FF)
val CyberSecondary = Color(0xFFB388FF)
val CyberTertiary = Color(0xFFFF4081)
val CyberBorder = Color(0xFF2B364D)

// Text tokens
val TextPrimary = Color(0xFFF8FAFC)
val TextSecondary = Color(0xFF94A3B8)
val TextMuted = Color(0xFF64748B)

enum class AppThemeMode(val displayName: String, val primaryHex: String) {
    EMERALD("Emerald Audiowave", "#1A5F3F"),
    VELVET("Velvet Sunset", "#964EC2"),
    EMBERS("Embers Glow", "#F64668"),
    CYBER("Cyber Neon", "#00E5FF")
}
