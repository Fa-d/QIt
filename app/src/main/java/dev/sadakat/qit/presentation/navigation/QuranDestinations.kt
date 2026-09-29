package dev.sadakat.qit.presentation.navigation

import kotlinx.serialization.Serializable

/** The home screen: continue card, search, surahs and juz. */
@Serializable
data object HomeDestination

/**
 * One surah in the reader, opened at [ayah] (0 = the top). The reader's ViewModel reads these from
 * its SavedStateHandle under the property names, "surah" and "ayah".
 */
@Serializable
data class ReaderDestination(val surah: Int, val ayah: Int = 0)

/** The Progress screen: which surahs and ayahs you've heard, and how often. */
@Serializable
data object ProgressDestination

/** Appearance: the style, the page tone and wallpaper colors. */
@Serializable
data object AppearanceDestination

/** About: the version, the credits for every source, and the privacy note. */
@Serializable
data object AboutDestination
