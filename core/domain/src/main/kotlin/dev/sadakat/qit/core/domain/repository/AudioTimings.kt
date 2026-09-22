package dev.sadakat.qit.core.domain.repository

import dev.sadakat.qit.core.domain.audio.WordTimings

/** What is known ahead of time about the audio files: their lengths, and when each Arabic word is recited. */
interface AudioTimings {

    /**
     * When each word of [surah]'s ayahs is recited in the Arabic audio, by ayah. Ayah 0 — the
     * basmala played before verse 1 — is recited from 1:1's file, so it has 1:1's timings.
     */
    suspend fun wordTimings(surah: Int): Map<Int, WordTimings>

    /**
     * Length of the audio file whose download id is [fileId]
     * ([dev.sadakat.qit.core.domain.audio.QuranAudioUrls.AudioFile.id]), or null if unknown.
     */
    suspend fun durationMs(fileId: String): Long?
}
