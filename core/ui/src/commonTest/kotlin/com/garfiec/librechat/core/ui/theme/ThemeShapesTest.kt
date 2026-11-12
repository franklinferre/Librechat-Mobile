package com.garfiec.librechat.core.ui.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.garfiec.librechat.core.model.config.theme.GenericFontFamily
import com.garfiec.librechat.core.model.config.theme.ServerThemeAppearance
import com.garfiec.librechat.core.model.config.theme.ThemeCorner
import com.garfiec.librechat.core.model.config.theme.ThemeFont
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

class ThemeShapesTest {

    @Test
    fun switchboardKeepsTheShapesTheAppAlwaysDrew() {
        with(ThemeShapes.SWITCHBOARD) {
            assertEquals(RoundedCornerShape(28.dp), composer)
            assertSame(CircleShape, composerAction)
            assertEquals(RoundedCornerShape(16.dp), userBubble)
            assertEquals(RoundedCornerShape(16.dp), bubble)
            assertEquals(RoundedCornerShape(16.dp), card)
            assertEquals(RoundedCornerShape(8.dp), codeBlock)
            assertEquals(RoundedCornerShape(8.dp), table)
            assertEquals(RoundedCornerShape(8.dp), diagram)
            assertEquals(RoundedCornerShape(12.dp), fileChip)
            assertEquals(RoundedCornerShape(16.dp), menu)
            assertEquals(RoundedCornerShape(28.dp), dialog)
        }
    }

    @Test
    fun eachElementTakesTheCornerTheWebRoundsItWith() {
        val corners = ThemeCorner.entries.associateWith { it.ordinal.toFloat() + 1 }
        fun r(corner: ThemeCorner) = RoundedCornerShape(corners.getValue(corner).dp)
        with(ThemeShapes.of(corners)) {
            assertEquals(r(ThemeCorner.LARGE_SURFACE), composer)
            assertEquals(r(ThemeCorner.COMPOSER_ACTION), composerAction)
            assertEquals(r(ThemeCorner.SURFACE), bubble)
            assertEquals(r(ThemeCorner.SURFACE), card)
            assertEquals(r(ThemeCorner.RADIUS_XL), codeBlock)
            assertEquals(r(ThemeCorner.CONTROL), table)
            assertEquals(r(ThemeCorner.RADIUS_LG), diagram)
            assertEquals(r(ThemeCorner.RADIUS_XL), fileChip)
            assertEquals(r(ThemeCorner.MENU), menu)
            assertEquals(r(ThemeCorner.RADIUS_2XL), dialog)
            val surface = corners.getValue(ThemeCorner.SURFACE).dp
            assertEquals(
                RoundedCornerShape(surface, surface, corners.getValue(ThemeCorner.CONTROL).dp, surface),
                userBubble,
            )
        }
    }

    @Test
    fun anUnnamedCornerKeepsSwitchboards() {
        val shapes = ThemeShapes.of(mapOf(ThemeCorner.SURFACE to 4f))
        assertEquals(RoundedCornerShape(4.dp), shapes.userBubble)
        assertEquals(ThemeShapes.SWITCHBOARD.composer, shapes.composer)
        assertEquals(ThemeShapes.SWITCHBOARD.codeBlock, shapes.codeBlock)
    }

    @Test
    fun headingsTakeTheDisplayFamilyAndTheRestTheBody() {
        // Lideri: libreChatTypography() is composable (brand fonts from resources), so this pins
        // withFonts on a plain Typography; the server-font path is not used by the locked theme anyway.
        val base = Typography()
        val typography = base.withFonts(
            ServerThemeAppearance(fonts = mapOf(ThemeFont.BODY to GenericFontFamily.SERIF, ThemeFont.DISPLAY to GenericFontFamily.MONO)),
        )
        assertEquals(FontFamily.Monospace, typography.headlineLarge.fontFamily)
        assertEquals(FontFamily.Monospace, typography.displaySmall.fontFamily)
        assertEquals(FontFamily.Serif, typography.bodyLarge.fontFamily)
        assertEquals(FontFamily.Serif, typography.titleMedium.fontFamily)
        assertEquals(FontFamily.Serif, typography.labelSmall.fontFamily)
        val sans = ServerThemeAppearance(fonts = mapOf(ThemeFont.BODY to GenericFontFamily.SANS))
        assertSame(base, base.withFonts(sans))
    }
}
