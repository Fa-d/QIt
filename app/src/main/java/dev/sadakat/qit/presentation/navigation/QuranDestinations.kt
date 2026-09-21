package dev.sadakat.qit.presentation.navigation

import androidx.navigation.NavType

/** Navigation destinations of the phone UI. */
object QuranDestinations {
    const val SURAH_LIST = "surahs"
    const val ARG_SURAH = "surah"
    const val ARG_AYAH = "ayah"
    const val SURAH_READER_PATTERN = "surah/{$ARG_SURAH}?$ARG_AYAH={$ARG_AYAH}"

    val surahReaderArguments = listOf(
        androidx.navigation.navArgument(ARG_SURAH) { type = NavType.IntType },
        androidx.navigation.navArgument(ARG_AYAH) {
            type = NavType.IntType
            defaultValue = 0
        },
    )

    /** Route of the reader for [surah], optionally scrolling to [ayah]. */
    fun surahReader(surah: Int, ayah: Int = 0): String = "surah/$surah?$ARG_AYAH=$ayah"
}
