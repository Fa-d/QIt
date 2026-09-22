package dev.sadakat.qit.watch

import dev.sadakat.qit.core.domain.model.BanglaVoice
import dev.sadakat.qit.core.testing.FakeQuranSettings
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BanglaVoiceSyncTest {

    private val settings = FakeQuranSettings()
    private val channel = RecordingChannel()
    private val sync = BanglaVoiceSync(settings, channel)

    @Test
    fun `the stored voice goes out once at start, and again on every change`() = runTest {
        sync.start(backgroundScope)
        runCurrent()
        assertEquals(listOf(BanglaVoice.DEFAULT), channel.voices)

        settings.banglaVoice.value = BanglaVoice.SAYED_ISMAT_TOHA
        runCurrent()
        assertEquals(listOf(BanglaVoice.DEFAULT, BanglaVoice.SAYED_ISMAT_TOHA), channel.voices)

        settings.banglaVoice.value = BanglaVoice.SAYED_ISMAT_TOHA // The same voice: no second item.
        runCurrent()
        assertEquals(listOf(BanglaVoice.DEFAULT, BanglaVoice.SAYED_ISMAT_TOHA), channel.voices)
    }

    @Test
    fun `a voice picked before the app started still goes out`() = runTest {
        settings.banglaVoice.value = BanglaVoice.SHAREEF_BAEZEED_MAHMOOD

        sync.start(backgroundScope)
        runCurrent()

        assertEquals(listOf(BanglaVoice.SHAREEF_BAEZEED_MAHMOOD), channel.voices)
    }

    /** [BanglaVoiceSyncChannel] test double: records every voice it was told to publish. */
    private class RecordingChannel : BanglaVoiceSyncChannel {

        val voices = mutableListOf<BanglaVoice>()

        override suspend fun publishVoice(voice: BanglaVoice) {
            voices += voice
        }
    }
}
