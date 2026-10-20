package com.garfiec.librechat.core.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import kotlin.math.max
import kotlin.math.min
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue

class LideriColorsTest {

    private val schemes = listOf("light" to LideriLightColorScheme, "dark" to LideriDarkColorScheme)

    /** WCAG 2.2 contrast ratio from relative luminance. */
    private fun contrast(a: Color, b: Color): Double {
        val la = a.luminance().toDouble()
        val lb = b.luminance().toDouble()
        return (max(la, lb) + 0.05) / (min(la, lb) + 0.05)
    }

    private fun spread(c: Color): Float = max(c.red, max(c.green, c.blue)) - min(c.red, min(c.green, c.blue))

    @Test
    fun lightPrimaryIsTheBrandBlueWithWhiteText() {
        assertEquals(Color(0xFF0424D9), LideriLightColorScheme.primary)
        assertEquals(Color(0xFFFFFFFF), LideriLightColorScheme.onPrimary)
    }

    @Test
    fun darkPrimaryIsTheSecondaryBlueWithInkText() {
        assertEquals(Color(0xFF0797EF), LideriDarkColorScheme.primary)
        assertEquals(Color(0xFF1F2937), LideriDarkColorScheme.onPrimary)
    }

    @Test
    fun orangeIsNeverAnActionColour() {
        val orange = setOf(Color(0xFFFF6022), Color(0xFFFF7C24))
        for ((name, s) in schemes) {
            val actions = mapOf(
                "primary" to s.primary,
                "secondary" to s.secondary,
                "primaryContainer" to s.primaryContainer,
                "secondaryContainer" to s.secondaryContainer,
            )
            for ((role, color) in actions) {
                assertTrue(color !in orange, "$role in the $name scheme must not be the brand orange")
            }
        }
        assertEquals(Color(0xFFFF6022), LideriLightColorScheme.tertiary)
        assertEquals(Color(0xFFFF7C24), LideriDarkColorScheme.tertiary)
    }

    @Test
    fun surfacesAreNeutral() {
        val failures = mutableListOf<String>()
        for ((name, s) in schemes) {
            val roles: Map<String, Color> = mapOf(
                "background" to s.background,
                "surface" to s.surface,
                "surfaceVariant" to s.surfaceVariant,
                "surfaceContainerLowest" to s.surfaceContainerLowest,
                "surfaceContainerLow" to s.surfaceContainerLow,
                "surfaceContainer" to s.surfaceContainer,
                "surfaceContainerHigh" to s.surfaceContainerHigh,
                "surfaceContainerHighest" to s.surfaceContainerHighest,
                "surfaceDim" to s.surfaceDim,
                "surfaceBright" to s.surfaceBright,
            )
            for ((role, color) in roles) {
                if (spread(color) > 0.04f) failures += "$role in the $name scheme is tinted (spread ${spread(color)})"
            }
        }
        assertTrue(failures.isEmpty(), failures.joinToString("\n"))
    }

    @Test
    fun schemeIgnoresAccentAndDynamicColour() {
        assertSame(LideriLightColorScheme, lideriColorScheme(darkTheme = false))
        assertSame(LideriDarkColorScheme, lideriColorScheme(darkTheme = true))
    }

    @Test
    fun brandPairsMeetWcagAa() {
        val failures = mutableListOf<String>()
        for ((name, s) in schemes) {
            val pairs: Map<String, Pair<Color, Color>> = mapOf(
                "primary/onPrimary" to (s.primary to s.onPrimary),
                "onSurface/surface" to (s.onSurface to s.surface),
                "onSurfaceVariant/surface" to (s.onSurfaceVariant to s.surface),
            )
            for ((label, p) in pairs) {
                val ratio = contrast(p.first, p.second)
                if (ratio < 4.5) failures += "$label in the $name scheme is $ratio:1"
            }
        }
        assertTrue(failures.isEmpty(), failures.joinToString("\n"))
    }
}
