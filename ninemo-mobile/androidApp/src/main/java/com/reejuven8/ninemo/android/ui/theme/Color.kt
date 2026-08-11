package com.reejuven8.ninemo.android.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// ─── Berry palette (from the imported Claude Design mockups) ──────────────────
val Berry = Color(0xFF9C4276)
val BerryPressed = Color(0xFF7A2F5B)
val BerryContainer = Color(0xFFFFD8EC)
val OnBerryContainer = Color(0xFF3B0722)
val Surface = Color(0xFFFDF7FB)
val SurfaceVariant = Color(0xFFF0E1E9)
val OnSurface = Color(0xFF211A1E)
val OnSurfaceMuted = Color(0xFF504349)
val Outline = Color(0xFF83737C)
val Canvas = Color(0xFFEFE7EC)

// Severity (identical semantics on every screen; always paired with icon+text)
val SeverityNormal = Color(0xFF2E7D5B)
val SeverityWarning = Color(0xFFB26A00)
val SeverityWarningBg = Color(0xFFFFF1DC)
val SeverityCritical = Color(0xFFB3261E)
val SeverityCriticalBg = Color(0xFFFCE8E6)

val NineMoLightColors = lightColorScheme(
    primary = Berry,
    onPrimary = Color.White,
    primaryContainer = BerryContainer,
    onPrimaryContainer = OnBerryContainer,
    secondary = BerryPressed,
    onSecondary = Color.White,
    background = Surface,
    onBackground = OnSurface,
    surface = Surface,
    onSurface = OnSurface,
    surfaceVariant = SurfaceVariant,
    onSurfaceVariant = OnSurfaceMuted,
    outline = Outline,
    error = SeverityCritical,
    onError = Color.White,
)

val NineMoDarkColors = darkColorScheme(
    primary = Color(0xFFFFB0D4),
    onPrimary = Color(0xFF5A123C),
    primaryContainer = Color(0xFF7A2F5B),
    onPrimaryContainer = BerryContainer,
    background = Color(0xFF191114),
    onBackground = Color(0xFFEFDFE6),
    surface = Color(0xFF191114),
    onSurface = Color(0xFFEFDFE6),
    surfaceVariant = Color(0xFF4E3A45),
    onSurfaceVariant = Color(0xFFD3C0C9),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
)
