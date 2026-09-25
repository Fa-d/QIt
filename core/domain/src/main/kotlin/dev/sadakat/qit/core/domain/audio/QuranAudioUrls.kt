package dev.sadakat.qit.core.domain.audio

import dev.sadakat.qit.core.domain.model.QuranMeta
import dev.sadakat.qit.core.domain.model.Track
import java.util.Locale

/**
 * Where each verse's audio lives.
 * - Arabic (Alafasy) and English (Saheeh Intl, Ibrahim Walk): the islamic.network CDN.
 * - Arabic of the extra Bangla voices (Abdul Basit mujawwad, Sudais): everyayah.com, by surah and verse
 *   (`001001.mp3`), the recordings quran-align timed.
 * - Bangla: the Hugging Face dataset [HF_DATASET], which mirrors the local `quran_audio/` folder
 *   (`bangla/bangla-translation-verses/00001.mp3`, `…/intro/002.mp3`, `bangla/toha-verses/00001.mp3`, …).
 *   The dataset also mirrors the Arabic and English sets, so moving those off islamic.network is a URL
 *   change here.
 */
object QuranAudioUrls {
    private const val ISLAMIC_NETWORK = "https://cdn.islamic.network/quran/audio"
    private const val EVERYAYAH = "https://everyayah.com/data"
    const val HF_DATASET = "https://huggingface.co/datasets/faddy001/quran_audio/resolve/main"
    private const val BANGLA_VERSES = "$HF_DATASET/bangla/bangla-translation-verses"

    /** [id] is the download id and is unique per file, e.g. "ar/255", "bn/intro/2", "bn.toha/255". */
    data class AudioFile(val id: String, val url: String)

    fun verse(track: Track, globalAyah: Int): AudioFile {
        require(globalAyah in 1..QuranMeta.TOTAL_AYAHS) { "Invalid global ayah $globalAyah" }
        val url = when (track) {
            Track.ARABIC -> "$ISLAMIC_NETWORK/128/ar.alafasy/$globalAyah.mp3"
            Track.ENGLISH -> "$ISLAMIC_NETWORK/192/en.walk/$globalAyah.mp3"
            Track.BANGLA -> "$BANGLA_VERSES/${fiveDigits(globalAyah)}.mp3"
            Track.ARABIC_BASIT_MUJAWWAD -> everyayah("Abdul_Basit_Mujawwad_128kbps", globalAyah)
            Track.BANGLA_TOHA -> "$HF_DATASET/bangla/toha-verses/${fiveDigits(globalAyah)}.mp3"
            Track.ARABIC_SUDAIS -> everyayah("Abdurrahmaan_As-Sudais_192kbps", globalAyah)
            Track.BANGLA_BAEZEED -> "$HF_DATASET/bangla/baezeed-verses/${fiveDigits(globalAyah)}.mp3"
        }
        return AudioFile("${track.code}/$globalAyah", url)
    }

    /**
     * Whether [track] has a file of its own for [globalAyah]. A recording that reads a few verses'
     * translation in one go, where the verses couldn't be told apart, has one file for all of them,
     * under the last ([SharedTranslations]); the others have none.
     */
    fun hasOwnFile(track: Track, globalAyah: Int): Boolean = !SharedTranslations.isShared(track, globalAyah)

    /**
     * The basmala played before verse 1 of [surah] on [track], or null for surahs 1 and 9 (and for a
     * track whose 1:1 shares its file with 1:2). Every track but [Track.BANGLA] reuses 1:1, which is the
     * basmala (and its meaning). The [Track.BANGLA] intro already contains the Arabic basmala followed
     * by its Bangla translation.
     */
    fun basmala(track: Track, surah: Int): AudioFile? {
        if (!QuranMeta.hasBasmalaPrefix(surah)) return null
        return when (track) {
            Track.BANGLA -> AudioFile("bn/intro/$surah", "$BANGLA_VERSES/intro/${threeDigits(surah)}.mp3")
            else -> verse(track, 1).takeIf { hasOwnFile(track, 1) }
        }
    }

    /** Whether [track]'s basmala already contains the Arabic, so no Arabic basmala plays before it. */
    fun basmalaIncludesArabic(track: Track): Boolean = track == Track.BANGLA

    /** Every file needed to play [surah] on [track] offline: the basmala (if any), then each verse's own file. */
    fun surahFiles(surah: Int, track: Track): List<AudioFile> = listOfNotNull(basmala(track, surah)) +
        (1..QuranMeta.ayahCount(surah))
            .map { QuranMeta.globalAyah(surah, it) }
            .filter { hasOwnFile(track, it) }
            .map { verse(track, it) }

    private fun everyayah(folder: String, globalAyah: Int): String {
        val ref = QuranMeta.ayahRef(globalAyah)
        return "$EVERYAYAH/$folder/${threeDigits(ref.surah)}${threeDigits(ref.ayah)}.mp3"
    }

    // Locale.ROOT: a Bangla-locale device would otherwise format Bengali digits.
    private fun fiveDigits(n: Int) = String.format(Locale.ROOT, "%05d", n)

    private fun threeDigits(n: Int) = String.format(Locale.ROOT, "%03d", n)
}
