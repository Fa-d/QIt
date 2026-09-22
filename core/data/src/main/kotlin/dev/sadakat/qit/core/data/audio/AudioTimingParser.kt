package dev.sadakat.qit.core.data.audio

import dev.sadakat.qit.core.domain.audio.WordTimings
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive

/** Lengths (ms) of every audio file, by track; verse files by global ayah - 1, Bangla intros by surah - 1. */
@Serializable
internal class AudioDurations(
    private val ar: List<Int>,
    private val en: List<Int>,
    private val bn: List<Int>,
    private val bnIntro: List<Int>,
) {
    /** The length of the file with download id [fileId] ("ar/255", "bn/intro/2", …), or null if unknown. */
    fun durationMs(fileId: String): Long? {
        val intro = INTRO_ID.matchEntire(fileId)
        val verse = VERSE_ID.matchEntire(fileId)
        val (list, number) = when {
            intro != null -> bnIntro to intro.groupValues[1]
            verse != null -> trackList(verse.groupValues[1]) to verse.groupValues[2]
            else -> return null
        }
        return list.getOrNull(number.toInt() - 1)?.takeIf { it > 0 }?.toLong()
    }

    private fun trackList(code: String) = when (code) {
        "ar" -> ar
        "en" -> en
        else -> bn
    }
}

// Top level, not in a companion: the serialization plugin puts the class's serializer there.
private val VERSE_ID = Regex("(ar|en|bn)/(\\d{1,5})")
private val INTRO_ID = Regex("bn/intro/(\\d{1,3})")

/** Parses the timing assets (`scripts/build_audio_timing.py`). Pure Kotlin, so it is cheap to test on the JVM. */
internal object AudioTimingParser {

    private val decoder = Json { ignoreUnknownKeys = true }

    /** `[[ayah, [start0, end0, start1, end1, …]], …]` → word timings by ayah. */
    fun parseWordTimings(json: String): Map<Int, WordTimings> = try {
        decoder.parseToJsonElement(json).jsonArray.associate { element ->
            val row = element.jsonArray
            require(row.size == 2) { "A row is [ayah, [timings]], got $row" }
            row[0].jsonPrimitive.int to WordTimings(row[1].jsonArray.map { it.jsonPrimitive.int }.toIntArray())
        }
    } catch (e: IllegalArgumentException) {
        // SerializationException is an IllegalArgumentException; wrapping adds which asset failed.
        throw IllegalArgumentException("Malformed word timings", e)
    }

    fun parseDurations(json: String): AudioDurations = try {
        decoder.decodeFromString<AudioDurations>(json)
    } catch (e: SerializationException) {
        throw IllegalArgumentException("Malformed audio durations", e)
    }
}
