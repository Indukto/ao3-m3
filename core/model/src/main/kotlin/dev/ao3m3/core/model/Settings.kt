package dev.ao3m3.core.model

import kotlinx.serialization.Serializable

@Serializable
enum class ReadingFont(val label: String) {
    SERIF("Serif"),
    SANS("Sans"),
}

@Serializable
enum class ReadingTheme(val label: String) {
    SYSTEM("Match system"),
    LIGHT("Light"),
    SEPIA("Sepia"),
    DARK("Dark"),
}

/**
 * Persisted reader preferences. Defaults follow the design contract: 17 sp body text at 1.6 line
 * height, comfortable side margins, and no justification (ragged right reads better on phones).
 */
@Serializable
data class ReadingSettings(
    val font: ReadingFont = ReadingFont.SERIF,
    val fontSizeSp: Int = 18,
    val lineHeight: Float = 1.6f,
    val marginDp: Int = 20,
    val justify: Boolean = false,
    val keepScreenOn: Boolean = false,
    val theme: ReadingTheme = ReadingTheme.SYSTEM,
) {
    companion object {
        val FONT_SIZES = listOf(14, 16, 18, 20, 22, 24, 28)
        val LINE_HEIGHTS = listOf(1.3f, 1.45f, 1.6f, 1.8f)
        val MARGINS = listOf(12, 16, 20, 28, 36)
    }
}

/** App-wide appearance preferences. Colour always follows the wallpaper (Material You). */
@Serializable
data class AppearanceSettings(
    val darkTheme: Boolean? = null, // null = follow system
)
