package dev.ao3m3.core.designsystem.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val Base = Typography()

/**
 * Uses the emphasized variants from Material 3 Expressive as the app's default display sizes,
 * then tightens tracking on large text so titles read as one confident block rather than
 * default-Material headline soup.
 */
val Ao3Typography: Typography = Base.copy(
    displaySmall = Base.displaySmallEmphasized.copy(
        fontWeight = FontWeight.SemiBold,
        letterSpacing = (-0.4).sp,
    ),
    headlineLarge = Base.headlineLargeEmphasized.copy(letterSpacing = (-0.3).sp),
    headlineMedium = Base.headlineMediumEmphasized.copy(letterSpacing = (-0.2).sp),
    headlineSmall = Base.headlineSmallEmphasized,
    titleLarge = Base.titleLargeEmphasized,
    titleMedium = Base.titleMediumEmphasized,
    bodyLarge = Base.bodyLarge.copy(fontSize = 17.sp, lineHeight = 26.sp),
    bodyMedium = Base.bodyMedium.copy(lineHeight = 22.sp),
    labelLarge = Base.labelLarge.copy(fontWeight = FontWeight.Medium),
)

/** Reader typography is computed per preference, not from the theme. */
fun readingFontFamily(serif: Boolean): FontFamily = if (serif) FontFamily.Serif else FontFamily.SansSerif
