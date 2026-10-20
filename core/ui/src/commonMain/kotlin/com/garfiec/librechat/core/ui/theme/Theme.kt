package com.garfiec.librechat.core.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import com.garfiec.librechat.core.model.ui.GlassCapability
import com.garfiec.librechat.core.model.ui.UiStyle
import com.garfiec.librechat.core.ui.components.topbar.LocalNativeOverlayGate
import com.garfiec.librechat.core.ui.components.topbar.NativeOverlayGate

/**
 * Returns a platform-specific dynamic color scheme if available, or null to fall back
 * to the LibreChat color scheme. On Android 12+, returns wallpaper-based Material You colors.
 */
@Composable
expect fun platformColorScheme(darkTheme: Boolean, dynamicColor: Boolean): ColorScheme?

/**
 * Whether the platform supports wallpaper-based Material You dynamic color.
 * Android 12+ returns true; iOS returns false. Used to gate the "use wallpaper
 * colors" setting and the [LibreChatTheme] precedence branch.
 */
expect fun supportsDynamicColor(): Boolean

/** The platform's own idiom, used when the user has not picked a [UiStyle]. */
expect fun platformDefaultUiStyle(): UiStyle

/** How faithfully this device can render [UiStyle.LIQUID_GLASS]. */
expect fun glassCapability(): GlassCapability

/**
 * Applies the LibreChat Material 3 theme.
 *
 * The colour scheme is always the Lideri brand one ([lideriColorScheme], `LideriColors.kt`).
 * [accentColor] and [useDynamicColor] are kept so existing call sites compile, but they are
 * ignored: a value a user saved earlier can no longer change the app's colours.
 *
 * [uiStyle] is the user's stored choice; null (nothing stored) means [platformDefaultUiStyle].
 */
@Composable
fun LibreChatTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    accentColor: Color = DefaultAccentSeed,
    useDynamicColor: Boolean = false,
    uiStyle: UiStyle? = null,
    content: @Composable () -> Unit,
) {
    val colorScheme = rememberLibreChatColorScheme(darkTheme, accentColor, useDynamicColor)
    val capability = remember { glassCapability() }
    CompositionLocalProvider(
        LocalGlassLevel provides capability.takeIf { (uiStyle ?: platformDefaultUiStyle()) == UiStyle.LIQUID_GLASS },
        LocalDarkTheme provides darkTheme,
        // One gate for the whole app: covering surfaces and native bars must see the same instance.
        LocalNativeOverlayGate provides remember { NativeOverlayGate() },
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = libreChatTypography(),
            shapes = libreChatShapes,
            content = content,
        )
    }
}

/**
 * Draws [content] in the light or dark theme the app would have with these settings, whatever theme
 * the app is in now: the colour scheme [LibreChatTheme] would build, and the dark flag glass colours
 * read. For a theme picker's previews only; the interface style is left as it is.
 */
@Composable
fun ThemePreview(
    darkTheme: Boolean,
    accentColor: Color,
    useDynamicColor: Boolean,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(LocalDarkTheme provides darkTheme) {
        MaterialTheme(
            colorScheme = rememberLibreChatColorScheme(darkTheme, accentColor, useDynamicColor),
            typography = libreChatTypography(),
            shapes = libreChatShapes,
            content = content,
        )
    }
}

/** The scheme for [darkTheme]: always the Lideri one; the accent and dynamic-colour inputs are ignored. */
@Suppress("UNUSED_PARAMETER")
@Composable
private fun rememberLibreChatColorScheme(darkTheme: Boolean, accentColor: Color, useDynamicColor: Boolean): ColorScheme =
    lideriColorScheme(darkTheme)
