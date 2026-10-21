package com.garfiec.librechat.core.common.speech

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SttLanguageTest {

    @Test
    fun `Portuguese is offered and maps to Brazilian Portuguese`() {
        assertTrue("Portuguese" in sttLanguageOptions)
        assertEquals("pt-BR", mapSttLanguageToLocale("Portuguese"))
    }

    @Test
    fun `every offered language except Auto-detect maps to a locale`() {
        sttLanguageOptions.drop(1).forEach { assertEquals(true, mapSttLanguageToLocale(it) != null, it) }
    }

    @Test
    fun `Auto-detect and unknown names fall back to the device default`() {
        assertNull(mapSttLanguageToLocale("Auto-detect"))
        assertNull(mapSttLanguageToLocale(""))
        assertNull(mapSttLanguageToLocale("Klingon"))
    }
}
