package dev.sadakat.qandeel.wear.presentation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

/**
 * The navigation arguments build (Navigation validates them when the graph is built — an invalid one
 * crashes the app on launch), and the surah route's juz start defaults to "from the start".
 */
class WearNavigationTest {

    @Test
    fun `the surah route's arguments are valid, and from defaults to the start`() {
        val from = SurahArguments.single { it.name == "from" }.argument

        assertFalse(from.isNullable)
        assertEquals(0, from.defaultValue)
    }

    @Test
    fun `the surah list route's arguments are valid`() {
        assertEquals(listOf("downloaded"), SurahsArguments.map { it.name })
    }
}
