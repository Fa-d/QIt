package dev.sadakat.qit.core.domain.player

import dev.sadakat.qit.core.domain.player.RepeatPolicy.Decision
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RepeatPolicyTest {

    private val first = RepeatProgress()

    @Test
    fun `without a repeat every ayah advances`() {
        assertEquals(Decision(RepeatStep.Advance, first), RepeatPolicy.afterAyah(RepeatSetting.Off, first, 5))
    }

    @Test
    fun `an ayah repeat replays each ayah until it has played the chosen number of times`() {
        val thrice = RepeatSetting.Ayah(times = 3)

        val second = RepeatPolicy.afterAyah(thrice, first, 5)
        assertEquals(Decision(RepeatStep.JumpTo(5), RepeatProgress(2)), second)

        val third = RepeatPolicy.afterAyah(thrice, second.progress, 5)
        assertEquals(Decision(RepeatStep.JumpTo(5), RepeatProgress(3)), third)

        // Played three times: move on, and the next ayah starts counting from one.
        assertEquals(Decision(RepeatStep.Advance, first), RepeatPolicy.afterAyah(thrice, third.progress, 5))
    }

    @Test
    fun `an endless ayah repeat never advances`() {
        val forever = RepeatSetting.Ayah(times = null)
        assertEquals(RepeatStep.JumpTo(7), RepeatPolicy.afterAyah(forever, RepeatProgress(99), 7).step)
    }

    @Test
    fun `a range plays through and restarts from its first ayah`() {
        val range = RepeatSetting.Range(from = 3, to = 5, times = 2)

        assertEquals(RepeatStep.Advance, RepeatPolicy.afterAyah(range, first, 3).step)
        assertEquals(RepeatStep.Advance, RepeatPolicy.afterAyah(range, first, 4).step)
        assertEquals(Decision(RepeatStep.JumpTo(3), RepeatProgress(2)), RepeatPolicy.afterAyah(range, first, 5))
    }

    @Test
    fun `a counted range stops after its last round`() {
        val range = RepeatSetting.Range(from = 3, to = 5, times = 2)
        assertEquals(Decision(RepeatStep.Finish, first), RepeatPolicy.afterAyah(range, RepeatProgress(2), 5))
    }

    @Test
    fun `an endless range loops for ever`() {
        val range = RepeatSetting.Range(from = 1, to = 1, times = null)
        assertEquals(RepeatStep.JumpTo(1), RepeatPolicy.afterAyah(range, RepeatProgress(50), 1).step)
    }

    @Test
    fun `ayahs outside a range and the basmala are left alone`() {
        val range = RepeatSetting.Range(from = 3, to = 5, times = null)
        assertEquals(RepeatStep.Advance, RepeatPolicy.afterAyah(range, first, 9).step)
        assertEquals(RepeatStep.Advance, RepeatPolicy.afterAyah(RepeatSetting.Ayah(null), first, 0).step)
    }

    @Test
    fun `the player needs to intervene exactly where the step isn't advance`() {
        val range = RepeatSetting.Range(from = 3, to = 5, times = null)
        assertTrue(RepeatPolicy.intervenesAfter(range, first, 5))
        assertFalse(RepeatPolicy.intervenesAfter(range, first, 4))
        assertFalse(RepeatPolicy.intervenesAfter(RepeatSetting.Off, first, 4))
    }

    @Test
    fun `moving by hand restarts an ayah repeat's count`() {
        val (setting, progress) = RepeatPolicy.onManualMove(RepeatSetting.Ayah(3), RepeatProgress(2), targetAyah = 8)
        assertEquals(RepeatSetting.Ayah(3), setting)
        assertEquals(first, progress)
    }

    @Test
    fun `moving inside a range keeps it and its count, moving out drops it`() {
        val range = RepeatSetting.Range(from = 3, to = 5, times = 3)

        assertEquals(range to RepeatProgress(2), RepeatPolicy.onManualMove(range, RepeatProgress(2), targetAyah = 4))
        assertEquals(RepeatSetting.Off to first, RepeatPolicy.onManualMove(range, RepeatProgress(2), targetAyah = 6))
        assertEquals(RepeatSetting.Off to first, RepeatPolicy.onManualMove(RepeatSetting.Off, RepeatProgress(2), 6))
    }

    @Test
    fun `choosing a range away from the playing ayah jumps to the range`() {
        val range = RepeatSetting.Range(from = 3, to = 5, times = null)
        assertEquals(3, RepeatPolicy.entryAyah(range, currentAyah = 9))
        assertNull(RepeatPolicy.entryAyah(range, currentAyah = 4))
        assertNull(RepeatPolicy.entryAyah(RepeatSetting.Ayah(3), currentAyah = 9))
    }
}
