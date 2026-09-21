package dev.sadakat.qit.core.testing

import dev.sadakat.qit.core.domain.model.Ayah
import dev.sadakat.qit.core.domain.model.Surah
import dev.sadakat.qit.core.domain.repository.QuranText

/** [QuranText] over [TestQuran]. Set [failure] to make every call throw it. */
class FakeQuranText(
    private val surahs: List<Surah> = TestQuran.allSurahs,
    private val ayahs: (Int) -> List<Ayah> = TestQuran::ayahs
) : QuranText {

    var failure: Throwable? = null

    override suspend fun surahs(): List<Surah> {
        failure?.let { throw it }
        return surahs
    }

    override suspend fun surah(number: Int): Surah {
        failure?.let { throw it }
        return surahs.firstOrNull { it.number == number } ?: throw IllegalArgumentException("No surah $number")
    }

    override suspend fun ayahs(surah: Int): List<Ayah> {
        failure?.let { throw it }
        return ayahs.invoke(surah)
    }
}
