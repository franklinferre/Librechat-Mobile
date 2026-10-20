package com.garfiec.librechat.core.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.garfiec.librechat.core.ui.resources.Res
import com.garfiec.librechat.core.ui.resources.poppins_bold
import com.garfiec.librechat.core.ui.resources.poppins_semibold
import com.garfiec.librechat.core.ui.resources.raleway_bold
import com.garfiec.librechat.core.ui.resources.raleway_medium
import com.garfiec.librechat.core.ui.resources.raleway_regular
import com.garfiec.librechat.core.ui.resources.raleway_semibold
import org.jetbrains.compose.resources.Font

/**
 * Lideri type scale: Poppins for display/headline/title, Raleway for body and labels.
 * Roles the upstream client already sized keep those sizes; the rest take the M3 defaults.
 * Pure so a test can pass sentinel families.
 */
fun lideriTypography(heading: FontFamily, body: FontFamily): Typography {
    val d = Typography()
    fun TextStyle.heading(weight: FontWeight) = copy(fontFamily = heading, fontWeight = weight)
    fun TextStyle.body(weight: FontWeight) = copy(fontFamily = body, fontWeight = weight)
    return Typography(
        displayLarge = d.displayLarge.heading(FontWeight.SemiBold),
        displayMedium = d.displayMedium.heading(FontWeight.SemiBold),
        displaySmall = d.displaySmall.heading(FontWeight.SemiBold),
        headlineLarge = d.headlineLarge.copy(fontSize = 30.sp, lineHeight = 36.sp).heading(FontWeight.Bold),
        headlineMedium = d.headlineMedium.copy(fontSize = 24.sp, lineHeight = 32.sp).heading(FontWeight.SemiBold),
        headlineSmall = d.headlineSmall.heading(FontWeight.SemiBold),
        titleLarge = d.titleLarge.heading(FontWeight.SemiBold),
        titleMedium = d.titleMedium.copy(fontSize = 16.sp, lineHeight = 24.sp).heading(FontWeight.SemiBold),
        titleSmall = d.titleSmall.copy(fontSize = 14.sp, lineHeight = 20.sp).heading(FontWeight.SemiBold),
        bodyLarge = d.bodyLarge.copy(fontSize = 16.sp, lineHeight = 24.sp).body(FontWeight.Normal),
        bodyMedium = d.bodyMedium.copy(fontSize = 14.sp, lineHeight = 20.sp).body(FontWeight.Normal),
        bodySmall = d.bodySmall.copy(fontSize = 12.sp, lineHeight = 16.sp).body(FontWeight.Normal),
        labelLarge = d.labelLarge.body(FontWeight.SemiBold),
        labelMedium = d.labelMedium.body(FontWeight.Medium),
        labelSmall = d.labelSmall.copy(fontSize = 12.sp, lineHeight = 16.sp).body(FontWeight.Medium),
    )
}

/** The app's [Typography], with the bundled Poppins and Raleway files loaded. */
@Composable
fun libreChatTypography(): Typography {
    val heading = FontFamily(
        Font(Res.font.poppins_semibold, FontWeight.SemiBold),
        Font(Res.font.poppins_bold, FontWeight.Bold),
    )
    val body = FontFamily(
        Font(Res.font.raleway_regular, FontWeight.Normal),
        Font(Res.font.raleway_medium, FontWeight.Medium),
        Font(Res.font.raleway_semibold, FontWeight.SemiBold),
        Font(Res.font.raleway_bold, FontWeight.Bold),
    )
    return remember(heading, body) { lideriTypography(heading, body) }
}
