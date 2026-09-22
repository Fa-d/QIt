package dev.sadakat.qit.core.data.audio

import dev.sadakat.qit.core.domain.audio.WordTimings
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive

/** Lengths (ms) of every audio file, by track code; verse files by global ayah - 1, intros by surah - 1. */
@Serializable
internal class AudioDurations(private val verses: Map<String, List<Int>>, private val intros: Map<String, List<Int>>) {
    /** The length of the file with download id [fileId] ("ar/255", "bn.toha/262", "bn/intro/2"), or null if unknown. */
    fun durationMs(fileId: String): Long? {
        val intro = INTRO_ID.matchEntire(fileId)
        val verse = VERSE_ID.matchEntire(fileId)
        val (durations, code, number) = when {
            intro != null -> Triple(intros, intro.groupValues[1], intro.groupValues[2])
            verse != null -> Triple(verses, verse.groupValues[1], verse.groupValues[2])
            else -> return null
        }
        return durations[code]?.getOrNull(number.toInt() - 1)?.takeIf { it > 0 }?.toLong()
    }
}

// Top level, not in a companion: the serialization plugin puts the class's serializer there.
// A track code is letters and dots ("ar", "bn", "bn.toha").
private val VERSE_ID = Regex("([a-z.]+)/(\\d{1,5})")
private val INTRO_ID = Regex("([a-z.]+)/intro/(\\d{1,3})")

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
