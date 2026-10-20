package com.garfiec.librechat.core.ui.brand

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.garfiec.librechat.core.ui.resources.Res
import com.garfiec.librechat.core.ui.resources.lideri_negativa
import com.garfiec.librechat.core.ui.resources.lideri_principal
import com.garfiec.librechat.core.ui.theme.LocalDarkTheme
import org.jetbrains.compose.resources.painterResource

/** Which kit PNG to draw: the blue signature on light surfaces, the negative one on dark. */
enum class LideriLogoAsset { Principal, Negativa }

fun lideriLogoAsset(dark: Boolean): LideriLogoAsset = if (dark) LideriLogoAsset.Negativa else LideriLogoAsset.Principal

/** The brand kit forbids rendering the full signature narrower than this. */
const val LIDERI_LOGO_MIN_WIDTH_DP = 220

/** Clear space around the signature, as a fraction of its total width (kit: H/4 = 0.0729). */
const val LIDERI_LOGO_CLEAR_SPACE_FRACTION = 0.073f

/** Kit PNG is 2400x932; the aspect ratio is locked. */
const val LIDERI_LOGO_ASPECT = 2400f / 932f

/**
 * The full Lideri signature (wordmark plus the five icons), unaltered kit artwork.
 * The artwork is never narrower than [LIDERI_LOGO_MIN_WIDTH_DP]; the clear space is added around it.
 */
@Composable
fun LideriLogo(
    modifier: Modifier = Modifier,
    width: Dp = 240.dp,
    dark: Boolean = LocalDarkTheme.current,
) {
    val logoWidth = maxOf(width, LIDERI_LOGO_MIN_WIDTH_DP.dp)
    val painter = painterResource(
        when (lideriLogoAsset(dark)) {
            LideriLogoAsset.Principal -> Res.drawable.lideri_principal
            LideriLogoAsset.Negativa -> Res.drawable.lideri_negativa
        },
    )
    Image(
        painter = painter,
        // The brand name; intentionally not localized.
        contentDescription = "Lideri",
        contentScale = ContentScale.Fit,
        modifier = modifier
            .testTag("lideri_logo")
            // Clear space sits outside the artwork so the signature itself keeps the full width.
            .padding(logoWidth * LIDERI_LOGO_CLEAR_SPACE_FRACTION)
            .width(logoWidth)
            .aspectRatio(LIDERI_LOGO_ASPECT),
    )
}
