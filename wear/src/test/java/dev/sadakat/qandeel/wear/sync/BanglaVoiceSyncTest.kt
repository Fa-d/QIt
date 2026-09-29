package dev.sadakat.qandeel.wear.sync

import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.sadakat.qandeel.core.domain.model.BanglaVoice
import dev.sadakat.qandeel.core.testing.FakeQuranSettings
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class) // An unknown code is logged through android.util.Log.
class BanglaVoiceSyncTest {

    private val settings = FakeQuranSettings()
    private val source = RecordingSource()
    private val sync = BanglaVoiceSync(settings, source)

    @Test
    fun `the phone's voice item is applied once at start`() = runTest {
        source.code = "baezeed"

        sync.start(backgroundScope)
        runCurrent()

        assertEquals(BanglaVoice.SHAREEF_BAEZEED_MAHMOOD, settings.banglaVoice.first())
    }

    @Test
    fun `an unknown code in the item leaves the watch's own choice`() = runTest {
        source.code = "someone-else"

        sync.start(backgroundScope)
        runCurrent()

        assertEquals(BanglaVoice.DEFAULT, settings.banglaVoice.first())
    }

    @Test
    fun `no item leaves the watch's own choice`() = runTest {
        source.code = null

        sync.start(backgroundScope)
        runCurrent()

        assertEquals(BanglaVoice.DEFAULT, settings.banglaVoice.first())
    }

    /** [BanglaVoiceSource] test double: the code scripted for the phone's item. */
    private class RecordingSource : BanglaVoiceSource {

        var code: String? = null

        override suspend fun voiceCode(): String? = code
    }
}
