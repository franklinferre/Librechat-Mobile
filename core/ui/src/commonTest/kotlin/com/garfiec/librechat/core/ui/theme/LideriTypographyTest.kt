package com.garfiec.librechat.core.ui.theme

import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LideriTypographyTest {

    private val typography = lideriTypography(heading = FontFamily.Serif, body = FontFamily.Monospace)

    @Test
    fun headingRolesUseHeadingFamilyAtSemiBoldOrHeavier() {
        val styles = with(typography) {
            listOf(
                displayLarge, displayMedium, displaySmall,
                headlineLarge, headlineMedium, headlineSmall,
                titleLarge, titleMedium, titleSmall,
            )
        }
        styles.forEach {
            assertEquals(FontFamily.Serif, it.fontFamily)
            assertTrue(it.fontWeight!!.weight >= 600)
        }
    }

    @Test
    fun bodyRolesUseBodyFamilyAtRegular() {
        with(typography) { listOf(bodyLarge, bodyMedium, bodySmall) }.forEach {
            assertEquals(FontFamily.Monospace, it.fontFamily)
            assertEquals(FontWeight.Normal, it.fontWeight)
        }
    }

    @Test
    fun labelRolesUseBodyFamilyAtMediumOrSemiBold() {
        with(typography) { listOf(labelLarge, labelMedium, labelSmall) }.forEach {
            assertEquals(FontFamily.Monospace, it.fontFamily)
            assertTrue(it.fontWeight == FontWeight.Medium || it.fontWeight == FontWeight.SemiBold)
        }
    }

    @Test
    fun sizesOfRolesTheUpstreamDefinedAreUnchanged() {
        assertEquals(16.sp, typography.bodyLarge.fontSize)
        assertEquals(14.sp, typography.bodyMedium.fontSize)
        assertEquals(24.sp, typography.headlineMedium.fontSize)
        assertEquals(30.sp, typography.headlineLarge.fontSize)
        assertEquals(16.sp, typography.titleMedium.fontSize)
        assertEquals(14.sp, typography.titleSmall.fontSize)
        assertEquals(12.sp, typography.labelSmall.fontSize)
    }
}
