package com.garfiec.librechat.core.common.di

import com.garfiec.librechat.core.common.brand.ServerLock
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.koin.dsl.koinApplication

/**
 * Pins the shipped graph: the app's [ServerLock] comes from [commonModule] and must be locked to the
 * Lideri server. Lives in androidUnitTest (not commonTest) because [commonModule] includes the platform
 * module, whose Android actuals need a Context; eager instances are off so nothing platform is built.
 */
class CommonModuleServerLockTest {
    @Test
    fun shippedGraphIsLockedToTheLideriServer() {
        val koin = koinApplication(createEagerInstances = false) { modules(commonModule) }
        try {
            val lock = koin.koin.get<ServerLock>()
            assertTrue(lock.isLocked)
            assertEquals("https://chat.lideri.cloud", lock.url)
        } finally {
            koin.close()
        }
    }
}
