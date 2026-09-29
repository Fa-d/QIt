package dev.sadakat.qandeel.core.data.audio

import org.junit.Assert.assertEquals
import org.junit.Test

class SurahFinishesTest {

    private val finishes = SurahFinishes()

    @Test
    fun `a surah leaving the running downloads has finished`() {
        assertEquals(emptyMap<Int, Boolean>(), finishes.update(setOf(18, 36)))
        assertEquals(mapOf(18 to false), finishes.update(setOf(36)))
        assertEquals(mapOf(36 to false), finishes.update(emptySet()))
    }

    @Test
    fun `a surah with a failed file finishes as failed`() {
        finishes.update(setOf(18))
        finishes.onFailed(listOf(18))
        assertEquals(emptyMap<Int, Boolean>(), finishes.update(setOf(18))) // other files still running
        assertEquals(mapOf(18 to true), finishes.update(emptySet()))
    }

    @Test
    fun `a failure is forgotten once its surah is done`() {
        finishes.update(setOf(18))
        finishes.onFailed(listOf(18))
        finishes.update(emptySet())

        finishes.update(setOf(18)) // retried
        assertEquals(mapOf(18 to false), finishes.update(emptySet()))
    }

    @Test
    fun `a cancelled surah hasn't finished`() {
        finishes.update(setOf(18, 36))
        finishes.onRemoving(listOf(18))
        assertEquals(emptyMap<Int, Boolean>(), finishes.update(setOf(36)))
    }

    @Test
    fun `removing a downloaded surah doesn't hide a later download of it`() {
        finishes.onRemoving(listOf(18)) // not running: a finished download being deleted
        finishes.update(emptySet())
        finishes.update(setOf(18))
        assertEquals(mapOf(18 to false), finishes.update(emptySet()))
    }
}
