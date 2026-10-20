package com.garfiec.librechat.core.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/**
 * The Lideri brand colour schemes, picked from `design/lideri/kit/brand-tokens.json`.
 *
 * Primary is the brand blue (light) or the secondary brand blue (dark); the orange is `tertiary`
 * only, a spot accent that is never an action colour. Surfaces are neutral in both modes so
 * Liquid Glass (which derives everything from this scheme) does not pick up a blue tint.
 */
val LideriLightColorScheme: ColorScheme = lightColorScheme(
    primary = Color(0xFF0424D9),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFDDE3FF),
    onPrimaryContainer = Color(0xFF02127A),
    secondary = Color(0xFF0797EF),
    onSecondary = Color(0xFF1F2937),
    secondaryContainer = Color(0xFFE1F2FD),
    onSecondaryContainer = Color(0xFF063A5C),
    tertiary = Color(0xFFFF6022),
    onTertiary = Color(0xFF1F2937),
    background = Color(0xFFFFFFFF),
    onBackground = Color(0xFF1F2937),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1F2937),
    surfaceVariant = Color(0xFFF5F7FA),
    onSurfaceVariant = Color(0xFF526071),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF8F9FB),
    surfaceContainer = Color(0xFFF5F7FA),
    surfaceContainerHigh = Color(0xFFEEF1F5),
    surfaceContainerHighest = Color(0xFFE8ECF1),
    surfaceDim = Color(0xFFE2E6EB),
    surfaceBright = Color(0xFFFFFFFF),
    outline = Color(0xFF8E9399),
    outlineVariant = Color(0xFFD9E0EA),
    inverseSurface = Color(0xFF1F2937),
    inverseOnSurface = Color(0xFFF5F7FA),
    inversePrimary = Color(0xFF0797EF),
    surfaceTint = Color(0xFF0424D9),
)

val LideriDarkColorScheme: ColorScheme = darkColorScheme(
    primary = Color(0xFF0797EF),
    onPrimary = Color(0xFF1F2937),
    primaryContainer = Color(0xFF0B2A6B),
    onPrimaryContainer = Color(0xFFDDE3FF),
    secondary = Color(0xFF07C4EF),
    onSecondary = Color(0xFF1F2937),
    secondaryContainer = Color(0xFF0A3550),
    onSecondaryContainer = Color(0xFFD6EEFC),
    tertiary = Color(0xFFFF7C24),
    onTertiary = Color(0xFF1F2937),
    background = Color(0xFF121417),
    onBackground = Color(0xFFE8EAED),
    surface = Color(0xFF121417),
    onSurface = Color(0xFFE8EAED),
    surfaceVariant = Color(0xFF2A2D32),
    onSurfaceVariant = Color(0xFFB4BAC2),
    surfaceContainerLowest = Color(0xFF0D0F11),
    surfaceContainerLow = Color(0xFF181A1D),
    surfaceContainer = Color(0xFF1C1F22),
    surfaceContainerHigh = Color(0xFF24272B),
    surfaceContainerHighest = Color(0xFF2E3135),
    surfaceDim = Color(0xFF121417),
    surfaceBright = Color(0xFF36393E),
    outline = Color(0xFF8A919B),
    outlineVariant = Color(0xFF3A3E44),
    inverseSurface = Color(0xFFE8EAED),
    inverseOnSurface = Color(0xFF1F2937),
    inversePrimary = Color(0xFF0424D9),
    surfaceTint = Color(0xFF0797EF),
)

/** The Lideri scheme for the given mode. Never depends on the accent or dynamic-colour settings. */
fun lideriColorScheme(darkTheme: Boolean): ColorScheme =
    if (darkTheme) LideriDarkColorScheme else LideriLightColorScheme
