package com.garfiec.librechat.core.common.brand

/**
 * Identity constants of the Lideri Chat white-label build. Values that name the app or the
 * server it is bound to live here, so a rebrand touches one file.
 */
object Brand {
    const val APP_NAME = "Lideri Chat"

    /** The only LibreChat server this build talks to. See [ServerLock]. */
    const val LOCKED_SERVER_URL = "https://chat.lideri.cloud"

    const val DEEP_LINK_SCHEME = "liderichat"

    /** Product token for the User-Agent header. */
    const val USER_AGENT_PRODUCT = "LideriChat"

    /** Folder name used when saving media to the device gallery or files. */
    const val MEDIA_FOLDER = "Lideri Chat"

    // PENDENTE-PUBLICACAO: set to the published privacy policy URL before store release.
    val PRIVACY_POLICY_URL: String? = null
}
