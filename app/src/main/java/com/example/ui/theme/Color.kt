package com.example.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

// Light Mode Palettes matching Image 2 mockup and user requirements
val LightBackground = Color(0xFFF8F9FA)
val LightSurface = Color(0xFFFFFFFF)
val LightSurfaceVariant = Color(0xFFF1F5F9)
val LightTextPrimary = Color(0xFF1A1C1E)
val LightTextSecondary = Color(0xFF64748B)
val LightOutline = Color(0xFFE2E8F0)

val LightPrimary = Color(0xFF0891B2)
val LightOnPrimary = Color(0xFFFFFFFF)
val LightPrimaryContainer = Color(0xFFE0F2FE)
val LightOnPrimaryContainer = Color(0xFF0369A1)

// Dark Mode Palettes matching Image 2 mockup and user requirements
val DarkBackground = Color(0xFF0F141A)
val DarkSurface = Color(0xFF19202A)
val DarkSurfaceVariant = Color(0xFF222B38)
val DarkTextPrimary = Color(0xFFE1E2E5)
val DarkTextSecondary = Color(0xFF94A3B8)
val DarkOutline = Color(0xFF2D3848)

val DarkPrimary = Color(0xFF22D3EE)
val DarkOnPrimary = Color(0xFF082F49)
val DarkPrimaryContainer = Color(0xFF0C4A6E)
val DarkOnPrimaryContainer = Color(0xFFBAE6FD)

// Semantic Status Colors
val StatusGreenLight = Color(0xFF2E7D32)
val StatusGreenDark = Color(0xFF81C784)

val StatusAmberLight = Color(0xFFED6C02)
val StatusAmberDark = Color(0xFFFFB74D)

val StatusRedLight = Color(0xFFD32F2F)
val StatusRedDark = Color(0xFFE57373)

// Alert Banner Specific Colors from Image 2
val AlertBgLight = Color(0xFFFFF7E6)
val AlertBorderLight = Color(0xFFFDE68A)
val AlertAccentLight = Color(0xFFD97706)

val AlertBgDark = Color(0xFF261D13)
val AlertBorderDark = Color(0xFF92400E)
val AlertAccentDark = Color(0xFFFBBF24)

// Chart & Gauge Specific Accents
val Chart30DayLight = Color(0xFF0891B2)
val Chart30DayDark = Color(0xFF22D3EE)

val Chart90DayLight = Color(0xFF7C3AED)
val Chart90DayDark = Color(0xFFA78BFA)

val Chart180DayLight = Color(0xFFF59E0B)
val Chart180DayDark = Color(0xFFFBBF24)

val ChartSpotLight = Color(0xFF94A3B8)
val ChartSpotDark = Color(0xFF64748B)

@Immutable
data class VitalsSemanticColors(
    val statusStable: Color,
    val statusWarning: Color,
    val statusCritical: Color,
    val alertBg: Color,
    val alertBorder: Color,
    val alertAccent: Color,
    val chart30d: Color,
    val chart90d: Color,
    val chart180d: Color,
    val chartSpot: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val cardSurface: Color
)
