package com.garfiec.librechat.core.ui.brand

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LideriLogoTest {

    @Test
    fun lightBackgroundUsesThePrincipalSignature() {
        assertEquals(LideriLogoAsset.Principal, lideriLogoAsset(dark = false))
    }

    @Test
    fun darkBackgroundUsesTheNegativeSignature() {
        assertEquals(LideriLogoAsset.Negativa, lideriLogoAsset(dark = true))
    }

    @Test
    fun brandGeometryMatchesTheKit() {
        assertEquals(220, LIDERI_LOGO_MIN_WIDTH_DP)
        assertTrue(abs(LIDERI_LOGO_ASPECT - 2.5763f) < 0.01f)
        assertTrue(abs(LIDERI_LOGO_CLEAR_SPACE_FRACTION - 0.0729f) < 0.001f)
    }
}
