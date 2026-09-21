package dev.sadakat.qit.core.domain.model

/** How big the Quran's Arabic is drawn, relative to the default. */
enum class ArabicTextSize(val scale: Float) {
    SMALL(0.85f),
    MEDIUM(1f),
    LARGE(1.2f),
    XLARGE(1.45f),
    XXLARGE(1.75f),
}

enum class ThemeMode { SYSTEM, LIGHT, DARK }

/** The reader's comfort settings. */
data class ReadingPrefs(
    val arabicTextSize: ArabicTextSize = ArabicTextSize.MEDIUM,
    /** Show the recitation mode's translation under each ayah. */
    val showTranslation: Boolean = true,
    /** Keep the reciting ayah in view while audio plays. */
    val followAlong: Boolean = true,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    /** Wallpaper colors (Android 12+) instead of the brand's. */
    val dynamicColor: Boolean = false,
)
