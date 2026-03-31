package com.rudy.stoic.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val StoicFontFamily = FontFamily.Monospace

val Typography = Typography(
    // Ring item labels
    displaySmall = TextStyle(
        fontFamily = StoicFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 1.sp,
        color = TextPrimary
    ),
    // Section headers (alphabet headers in drawer)
    titleLarge = TextStyle(
        fontFamily = StoicFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        lineHeight = 28.sp,
        letterSpacing = 1.5.sp,
        color = CyanPrimary
    ),
    // Screen titles
    titleMedium = TextStyle(
        fontFamily = StoicFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 1.sp,
        color = TextPrimary
    ),
    // App names in drawer
    bodyLarge = TextStyle(
        fontFamily = StoicFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.5.sp,
        color = TextPrimary
    ),
    // Quick settings labels
    bodyMedium = TextStyle(
        fontFamily = StoicFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp,
        color = TextSecondary
    ),
    // Small captions / hints
    labelSmall = TextStyle(
        fontFamily = StoicFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 10.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.5.sp,
        color = TextSecondary
    )
)
