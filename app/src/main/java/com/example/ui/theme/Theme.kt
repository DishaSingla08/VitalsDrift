package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf

enum class AppThemeMode(val title: String) {
    SYSTEM("Auto / System"),
    LIGHT("Light Mode"),
    DARK("Dark Mode")
}

private val LightColorScheme = lightColorScheme(
    primary = LightPrimary,
    onPrimary = LightOnPrimary,
    primaryContainer = LightPrimaryContainer,
    onPrimaryContainer = LightOnPrimaryContainer,
    background = LightBackground,
    onBackground = LightTextPrimary,
    surface = LightSurface,
    onSurface = LightTextPrimary,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightTextSecondary,
    outline = LightOutline
)

private val DarkColorScheme = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = DarkOnPrimary,
    primaryContainer = DarkPrimaryContainer,
    onPrimaryContainer = DarkOnPrimaryContainer,
    background = DarkBackground,
    onBackground = DarkTextPrimary,
    surface = DarkSurface,
    onSurface = DarkTextPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkTextSecondary,
    outline = DarkOutline
)

private val LightSemanticColors = VitalsSemanticColors(
    statusStable = StatusGreenLight,
    statusWarning = StatusAmberLight,
    statusCritical = StatusRedLight,
    alertBg = AlertBgLight,
    alertBorder = AlertBorderLight,
    alertAccent = AlertAccentLight,
    chart30d = Chart30DayLight,
    chart90d = Chart90DayLight,
    chart180d = Chart180DayLight,
    chartSpot = ChartSpotLight,
    textPrimary = LightTextPrimary,
    textSecondary = LightTextSecondary,
    cardSurface = LightSurface
)

private val DarkSemanticColors = VitalsSemanticColors(
    statusStable = StatusGreenDark,
    statusWarning = StatusAmberDark,
    statusCritical = StatusRedDark,
    alertBg = AlertBgDark,
    alertBorder = AlertBorderDark,
    alertAccent = AlertAccentDark,
    chart30d = Chart30DayDark,
    chart90d = Chart90DayDark,
    chart180d = Chart180DayDark,
    chartSpot = ChartSpotDark,
    textPrimary = DarkTextPrimary,
    textSecondary = DarkTextSecondary,
    cardSurface = DarkSurface
)

val LocalVitalsSemanticColors = staticCompositionLocalOf { LightSemanticColors }

object VitalsTheme {
    val colors: VitalsSemanticColors
        @Composable
        @ReadOnlyComposable
        get() = LocalVitalsSemanticColors.current
}

@Composable
fun MyApplicationTheme(
    themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    content: @Composable () -> Unit
) {
    val isDark = when (themeMode) {
        AppThemeMode.SYSTEM -> isSystemInDarkTheme()
        AppThemeMode.LIGHT -> false
        AppThemeMode.DARK -> true
    }

    val colorScheme = if (isDark) DarkColorScheme else LightColorScheme
    val semanticColors = if (isDark) DarkSemanticColors else LightSemanticColors

    CompositionLocalProvider(LocalVitalsSemanticColors provides semanticColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
