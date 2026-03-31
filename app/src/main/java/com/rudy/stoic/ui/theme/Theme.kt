package com.rudy.stoic.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val StoicColorScheme = darkColorScheme(
    primary = CyanPrimary,
    secondary = CyanMuted,
    tertiary = AmberAccent,
    background = BackgroundDark,
    surface = SurfaceDark,
    surfaceVariant = SurfaceDarkAlt,
    onPrimary = BackgroundDark,
    onSecondary = BackgroundDark,
    onTertiary = BackgroundDark,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    onSurfaceVariant = TextSecondary,
    outline = CyanBorder
)

@Composable
fun StoicTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = StoicColorScheme,
        typography = Typography,
        content = content
    )
}
