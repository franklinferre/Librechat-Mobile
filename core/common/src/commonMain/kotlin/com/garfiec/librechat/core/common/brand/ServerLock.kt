package com.garfiec.librechat.core.common.brand

/**
 * Server the app is pinned to. When [url] is set, onboarding skips the server picker and the
 * app connects only to that address. A null [url] leaves the app unlocked, which is the
 * upstream behaviour.
 */
class ServerLock(val url: String?) {
    val isLocked: Boolean
        get() = url != null
}
