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
