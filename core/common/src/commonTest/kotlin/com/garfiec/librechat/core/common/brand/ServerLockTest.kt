package com.garfiec.librechat.core.common.brand

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ServerLockTest {

    @Test
    fun nullUrlIsUnlocked() {
        assertFalse(ServerLock(null).isLocked)
    }

    @Test
    fun brandLockIsTheLideriServer() {
        val lock = ServerLock(Brand.LOCKED_SERVER_URL)
        assertTrue(lock.isLocked)
        assertEquals("https://chat.lideri.cloud", lock.url)
    }
}
