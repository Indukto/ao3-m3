package dev.ao3m3.core.designsystem.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/**
 * The app's one accent is AO3's crimson. Everything else stays in a warm, very low-chroma
 * neutral range so that the many tags and stats on a work page never compete with it.
 */
private val Crimson = Color(0xFF8C1D18)
private val CrimsonLight = Color(0xFFFFB4AB)
private val CrimsonContainer = Color(0xFFFFDAD5)
private val CrimsonContainerDark = Color(0xFF93000A)

internal val ao3LightScheme = lightColorScheme(
    primary = Crimson,
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = CrimsonContainer,
    onPrimaryContainer = Color(0xFF410002),
    inversePrimary = CrimsonLight,
    secondary = Color(0xFF775652),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFF6DDD8),
    onSecondaryContainer = Color(0xFF2C1512),
    tertiary = Color(0xFF6C5D2F),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFF5E3A7),
    onTertiaryContainer = Color(0xFF231B00),
    error = Color(0xFFB3261E),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFF9DEDC),
    onErrorContainer = Color(0xFF410E0B),
    background = Color(0xFFFCF8F7),
    onBackground = Color(0xFF1C1B1B),
    surface = Color(0xFFFCF8F7),
    onSurface = Color(0xFF1C1B1B),
    surfaceVariant = Color(0xFFF0DEDB),
    onSurfaceVariant = Color(0xFF524341),
    surfaceTint = Crimson,
    surfaceDim = Color(0xFFDCD9D8),
    surfaceBright = Color(0xFFFCF8F7),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF7F2F1),
    surfaceContainer = Color(0xFFF1EBEA),
    surfaceContainerHigh = Color(0xFFEBE5E4),
    surfaceContainerHighest = Color(0xFFE5DEDD),
    outline = Color(0xFF847371),
    outlineVariant = Color(0xFFD5C2BF),
    inverseSurface = Color(0xFF313030),
    inverseOnSurface = Color(0xFFF4F0EF),
    scrim = Color(0xFF000000),
)

internal val ao3DarkScheme = darkColorScheme(
    primary = CrimsonLight,
    onPrimary = Color(0xFF690005),
    primaryContainer = CrimsonContainerDark,
    onPrimaryContainer = Color(0xFFFFDAD6),
    inversePrimary = Crimson,
    secondary = Color(0xFFE6BEBA),
    onSecondary = Color(0xFF412A27),
    secondaryContainer = Color(0xFF59403D),
    onSecondaryContainer = Color(0xFFFFDAD6),
    tertiary = Color(0xFFD8C58C),
    onTertiary = Color(0xFF3B2F05),
    tertiaryContainer = Color(0xFF534619),
    onTertiaryContainer = Color(0xFFF5E3A7),
    error = Color(0xFFF2B8B5),
    onError = Color(0xFF601410),
    errorContainer = Color(0xFF8C1D18),
    onErrorContainer = Color(0xFFF9DEDC),
    background = Color(0xFF131110),
    onBackground = Color(0xFFE8E1E0),
    surface = Color(0xFF131110),
    onSurface = Color(0xFFE8E1E0),
    surfaceVariant = Color(0xFF4F4443),
    onSurfaceVariant = Color(0xFFD3C4C2),
    surfaceTint = CrimsonLight,
    surfaceDim = Color(0xFF131110),
    surfaceBright = Color(0xFF3A3534),
    surfaceContainerLowest = Color(0xFF0D0B0B),
    surfaceContainerLow = Color(0xFF1B1918),
    surfaceContainer = Color(0xFF1F1D1C),
    surfaceContainerHigh = Color(0xFF2A2726),
    surfaceContainerHighest = Color(0xFF352F2E),
    outline = Color(0xFF9C8C8A),
    outlineVariant = Color(0xFF4F4443),
    inverseSurface = Color(0xFFE8E1E0),
    inverseOnSurface = Color(0xFF313030),
    scrim = Color(0xFF000000),
)

/** Warm paper colours used by the reader's sepia theme. */
val SepiaBackground = Color(0xFFF4ECD8)
val SepiaOnBackground = Color(0xFF3B3125)

/** Reader-only dark paper, used when the app is light but the reader is dark. */
val ReaderDarkBackground = Color(0xFF151312)
val ReaderDarkOnBackground = Color(0xFFE6DEDC)
