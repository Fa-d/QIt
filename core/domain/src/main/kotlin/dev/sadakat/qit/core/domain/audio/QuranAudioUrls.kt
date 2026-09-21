package dev.sadakat.qit.core.domain.audio

import dev.sadakat.qit.core.domain.model.QuranMeta
import dev.sadakat.qit.core.domain.model.Track
import java.util.Locale

/**
 * Where each verse's audio lives.
 * - Arabic (Alafasy) and English (Saheeh Intl, Ibrahim Walk): the islamic.network CDN.
 * - Bangla: the Hugging Face dataset [HF_DATASET], which mirrors the local `quran_audio/` folder
 *   (`bangla/bangla-translation-verses/00001.mp3`, `…/intro/002.mp3`). The dataset also mirrors the
 *   Arabic and English sets, so moving those off islamic.network is a URL change here.
 */
object QuranAudioUrls {
    private const val ISLAMIC_NETWORK = "https://cdn.islamic.network/quran/audio"
    const val HF_DATASET = "https://huggingface.co/datasets/Fa-d/qit-quran-audio/resolve/main"
    private const val BANGLA_VERSES = "$HF_DATASET/bangla/bangla-translation-verses"

    /** [id] is the download id and is unique per file, e.g. "ar/255", "bn/intro/2". */
    data class AudioFile(val id: String, val url: String)

    fun verse(track: Track, globalAyah: Int): AudioFile {
        require(globalAyah in 1..QuranMeta.TOTAL_AYAHS) { "Invalid global ayah $globalAyah" }
        val url = when (track) {
            Track.ARABIC -> "$ISLAMIC_NETWORK/128/ar.alafasy/$globalAyah.mp3"
            Track.ENGLISH -> "$ISLAMIC_NETWORK/192/en.walk/$globalAyah.mp3"
            // Locale.ROOT: a Bangla-locale device would otherwise format Bengali digits.
            Track.BANGLA -> "$BANGLA_VERSES/" + String.format(Locale.ROOT, "%05d", globalAyah) + ".mp3"
        }
        return AudioFile("${track.code}/$globalAyah", url)
    }

    /**
     * The basmala played before verse 1 of [surah] on [track], or null for surahs 1 and 9.
     * Arabic and English reuse 1:1 (which is the basmala). The Bangla intro already contains the
     * Arabic basmala followed by its Bangla translation.
     */
    fun basmala(track: Track, surah: Int): AudioFile? {
        if (!QuranMeta.hasBasmalaPrefix(surah)) return null
        return when (track) {
            Track.ARABIC, Track.ENGLISH -> verse(track, 1)
            Track.BANGLA -> AudioFile(
                "bn/intro/$surah",
                "$BANGLA_VERSES/intro/" + String.format(Locale.ROOT, "%03d", surah) + ".mp3"
            )
        }
    }

    /** Every file needed to play [surah] on [track] offline: the basmala (if any), then each verse. */
    fun surahFiles(surah: Int, track: Track): List<AudioFile> =
        listOfNotNull(basmala(track, surah)) +
            (1..QuranMeta.ayahCount(surah)).map { verse(track, QuranMeta.globalAyah(surah, it)) }
}
