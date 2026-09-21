package dev.sadakat.qit.core.designsystem.color

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import dev.sadakat.qit.core.designsystem.ref.QItPalettes

/**
 * Semantic color roles: what a color is *for*. The first block mirrors Material 3's roles so each app
 * can map them onto its own MaterialTheme 1:1; the extended roles at the end name the things a Quran
 * reader needs and Material has no role for.
 */
@Immutable
data class QItColors(
    val primary: Color,
    val onPrimary: Color,
    val primaryContainer: Color,
    val onPrimaryContainer: Color,
    val inversePrimary: Color,
    val secondary: Color,
    val onSecondary: Color,
    val secondaryContainer: Color,
    val onSecondaryContainer: Color,
    val tertiary: Color,
    val onTertiary: Color,
    val tertiaryContainer: Color,
    val onTertiaryContainer: Color,
    val background: Color,
    val onBackground: Color,
    val surface: Color,
    val onSurface: Color,
    val surfaceVariant: Color,
    val onSurfaceVariant: Color,
    val surfaceDim: Color,
    val surfaceBright: Color,
    val surfaceContainerLowest: Color,
    val surfaceContainerLow: Color,
    val surfaceContainer: Color,
    val surfaceContainerHigh: Color,
    val surfaceContainerHighest: Color,
    val inverseSurface: Color,
    val inverseOnSurface: Color,
    val outline: Color,
    val outlineVariant: Color,
    val scrim: Color,
    val error: Color,
    val onError: Color,
    val errorContainer: Color,
    val onErrorContainer: Color,
    /** The Quran's Arabic text: the page's ink. */
    val arabicText: Color,
    /** Translation text under the Arabic; quieter than the Arabic but fully readable. */
    val translationText: Color,
    /** Background of the ayah that is being recited. */
    val playingAyahHighlight: Color,
    /** Text on [playingAyahHighlight]. */
    val onPlayingAyahHighlight: Color,
    /** Gold line work: the octagram around surah and ayah numbers, section rules. */
    val ornament: Color,
    /** The unfilled part of progress bars and rings; the filled part is [primary]. */
    val progressTrack: Color,
    /** Hairlines between list rows. */
    val divider: Color,
    val isDark: Boolean,
)

private val primary = QItPalettes.primary
private val secondary = QItPalettes.secondary
private val tertiary = QItPalettes.tertiary
private val neutral = QItPalettes.neutral
private val neutralVariant = QItPalettes.neutralVariant
private val error = QItPalettes.error

/** Paper and ink: warm light surfaces, deep green primary, gold accents. */
fun lightQItColors(): QItColors = QItColors(
    primary = primary[40],
    onPrimary = primary[100],
    primaryContainer = primary[90],
    onPrimaryContainer = primary[10],
    inversePrimary = primary[80],
    secondary = secondary[40],
    onSecondary = secondary[100],
    secondaryContainer = secondary[90],
    onSecondaryContainer = secondary[10],
    tertiary = tertiary[40],
    onTertiary = tertiary[100],
    tertiaryContainer = tertiary[90],
    onTertiaryContainer = tertiary[10],
    background = neutral[98],
    onBackground = neutral[10],
    surface = neutral[98],
    onSurface = neutral[10],
    surfaceVariant = neutralVariant[90],
    onSurfaceVariant = neutralVariant[30],
    surfaceDim = neutral[87],
    surfaceBright = neutral[98],
    surfaceContainerLowest = neutral[100],
    surfaceContainerLow = neutral[96],
    surfaceContainer = neutral[94],
    surfaceContainerHigh = neutral[92],
    surfaceContainerHighest = neutral[90],
    inverseSurface = neutral[20],
    inverseOnSurface = neutral[95],
    outline = neutralVariant[50],
    outlineVariant = neutralVariant[80],
    scrim = neutral[0],
    error = error[40],
    onError = error[100],
    errorContainer = error[90],
    onErrorContainer = error[10],
    arabicText = neutral[10],
    translationText = neutralVariant[30],
    playingAyahHighlight = tertiary[92],
    onPlayingAyahHighlight = tertiary[10],
    ornament = tertiary[50],
    progressTrack = secondary[90],
    divider = neutralVariant[90],
    isDark = false,
)

/** Night mushaf: warm dark surfaces, light green primary, bright gold accents. */
fun darkQItColors(): QItColors = QItColors(
    primary = primary[80],
    onPrimary = primary[20],
    primaryContainer = primary[30],
    onPrimaryContainer = primary[90],
    inversePrimary = primary[40],
    secondary = secondary[80],
    onSecondary = secondary[20],
    secondaryContainer = secondary[30],
    onSecondaryContainer = secondary[90],
    tertiary = tertiary[80],
    onTertiary = tertiary[20],
    tertiaryContainer = tertiary[30],
    onTertiaryContainer = tertiary[90],
    background = neutral[6],
    onBackground = neutral[90],
    surface = neutral[6],
    onSurface = neutral[90],
    surfaceVariant = neutralVariant[30],
    onSurfaceVariant = neutralVariant[80],
    surfaceDim = neutral[6],
    surfaceBright = neutral[24],
    surfaceContainerLowest = neutral[4],
    surfaceContainerLow = neutral[10],
    surfaceContainer = neutral[12],
    surfaceContainerHigh = neutral[17],
    surfaceContainerHighest = neutral[22],
    inverseSurface = neutral[90],
    inverseOnSurface = neutral[20],
    outline = neutralVariant[60],
    outlineVariant = neutralVariant[30],
    scrim = neutral[0],
    error = error[80],
    onError = error[20],
    errorContainer = error[30],
    onErrorContainer = error[90],
    arabicText = neutral[90],
    translationText = neutralVariant[80],
    playingAyahHighlight = tertiary[20],
    onPlayingAyahHighlight = tertiary[90],
    ornament = tertiary[70],
    progressTrack = secondary[30],
    divider = neutralVariant[20],
    isDark = true,
)

/**
 * The watch: the night scheme on a pure black background (OLED pixels off), with the brighter tones
 * Wear Material 3 uses for its roles.
 */
fun watchQItColors(): QItColors = darkQItColors().copy(
    primary = primary[90],
    onPrimary = primary[10],
    tertiary = tertiary[90],
    onTertiary = tertiary[10],
    background = neutral[0],
    onBackground = neutral[100],
    surface = neutral[0],
    onSurface = neutral[95],
    arabicText = neutral[95],
    ornament = tertiary[80],
)
