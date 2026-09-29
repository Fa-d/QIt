package dev.sadakat.qandeel.core.data.text

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive

/** Parses the word meaning assets (`scripts/build_word_meanings.py`); pure Kotlin, cheap to test on the JVM. */
internal object WordMeaningsParser {

    private val decoder = Json { ignoreUnknownKeys = true }

    /** `[[ayah, ["meaning0", "meaning1", …]], …]` → meanings by ayah, one per word. */
    fun parse(json: String): Map<Int, List<String>> = try {
        decoder.parseToJsonElement(json).jsonArray.associate { element ->
            val row = element.jsonArray
            require(row.size == 2) { "A row is [ayah, [meanings]], got $row" }
            row[0].jsonPrimitive.int to row[1].jsonArray.map { it.jsonPrimitive.content }
        }
    } catch (e: IllegalArgumentException) {
        // SerializationException is an IllegalArgumentException; wrapping adds which asset failed.
        throw IllegalArgumentException("Malformed word meanings", e)
    }
}
