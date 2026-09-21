package dev.sadakat.qit.core.testing

import dev.sadakat.qit.core.domain.model.Ayah
import dev.sadakat.qit.core.domain.model.QuranMeta
import dev.sadakat.qit.core.domain.model.Revelation
import dev.sadakat.qit.core.domain.model.Surah

/** Realistic sample data for tests: real surah structure (from [QuranMeta]) with placeholder text. */
object TestQuran {

    private val names = mapOf(
        1 to Triple("Al-Faatiha", "The Opening", "الفاتحة"),
        2 to Triple("Al-Baqara", "The Cow", "البقرة"),
        9 to Triple("At-Tawba", "The Repentance", "التوبة"),
        112 to Triple("Al-Ikhlaas", "Sincerity", "الإخلاص"),
        114 to Triple("An-Naas", "Mankind", "الناس"),
    )

    private val medinan =
        setOf(2, 3, 4, 5, 8, 9, 22, 24, 33, 47, 48, 49, 55, 57, 58, 59, 60, 61, 62, 63, 64, 65, 66, 76, 98, 99, 110)

    fun surah(number: Int): Surah {
        val (english, meaning, arabic) = names[number] ?: Triple("Surah $number", "Meaning $number", "سورة $number")
        return Surah(
            number = number,
            nameArabic = arabic,
            nameEnglish = english,
            meaningEnglish = meaning,
            ayahCount = QuranMeta.ayahCount(number),
            revelation = if (number in medinan) Revelation.MEDINAN else Revelation.MECCAN,
        )
    }

    val allSurahs: List<Surah> by lazy { (1..QuranMeta.SURAH_COUNT).map(::surah) }

    fun ayahs(surah: Int): List<Ayah> = (1..QuranMeta.ayahCount(surah)).map { n ->
        Ayah(
            surah = surah,
            number = n,
            globalNumber = QuranMeta.globalAyah(surah, n),
            arabic = "آية $surah:$n",
            english = "English $surah:$n",
            bangla = "বাংলা $surah:$n",
        )
    }
}
