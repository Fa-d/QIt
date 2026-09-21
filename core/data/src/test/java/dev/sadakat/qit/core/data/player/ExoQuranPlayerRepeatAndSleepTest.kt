package dev.sadakat.qit.core.data.player

import android.content.Context
import android.os.Looper
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.test.utils.FakeMediaSource
import androidx.media3.test.utils.FakeTimeline
import androidx.media3.test.utils.TestExoPlayerBuilder
import androidx.media3.test.utils.robolectric.RobolectricUtil.runMainLooperUntil
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.sadakat.qit.core.domain.model.RecitationMode
import dev.sadakat.qit.core.domain.player.PlaybackSpeed
import dev.sadakat.qit.core.domain.player.RepeatSetting
import dev.sadakat.qit.core.domain.player.SleepOption
import dev.sadakat.qit.core.domain.player.SleepTimerStatus
import dev.sadakat.qit.core.testing.FakeQuranSettings
import dev.sadakat.qit.core.testing.FakeQuranText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import java.time.Duration

/**
 * Repeat and the sleep timer against a real ExoPlayer on a fake clock: every queue item is a 10 s
 * fake source, so "let the ayah finish" means seeking near its end and playing.
 */
@androidx.annotation.OptIn(UnstableApi::class)
@RunWith(AndroidJUnit4::class)
class ExoQuranPlayerRepeatAndSleepTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val settings = FakeQuranSettings()
    private var now = 0L
    private lateinit var exoPlayer: ExoPlayer
    private lateinit var player: ExoQuranPlayer

    @Before
    fun setUp() {
        exoPlayer = TestExoPlayerBuilder(context)
            .setMediaSourceFactory(TenSecondSources())
            .build()
        player =
            ExoQuranPlayer(context, exoPlayer, FakeQuranText(), settings, CoroutineScope(Dispatchers.Main), clock = {
                now
            })
    }

    @After
    fun tearDown() {
        exoPlayer.release()
    }

    private fun start(surah: Int, ayah: Int, mode: RecitationMode = RecitationMode.ARABIC_ONLY) {
        player.play(surah, fromAyah = ayah, mode = mode)
        runMainLooperUntil { player.nowPlaying.value?.isPlaying == true }
    }

    /** Lets the current item play out from 0.5 s before its end: it moves on, pauses at its end, or ends the queue. */
    private fun finishCurrentItem() {
        exoPlayer.seekTo(exoPlayer.currentMediaItemIndex, 9_500)
        runMainLooperUntil {
            exoPlayer.currentPosition < 9_500 || !exoPlayer.playWhenReady ||
                exoPlayer.playbackState == Player.STATE_ENDED
        }
    }

    private fun currentMediaId() = exoPlayer.currentMediaItem?.mediaId

    @Test
    fun `an ayah repeat replays the ayah, then moves on`() {
        start(2, 5)
        player.setRepeat(RepeatSetting.Ayah(times = 2))

        finishCurrentItem()
        runMainLooperUntil { exoPlayer.isPlaying }
        assertEquals("2:5:ar", currentMediaId())
        assertTrue(player.nowPlaying.value!!.isPlaying)

        finishCurrentItem()
        runMainLooperUntil { currentMediaId() == "2:6:ar" }
    }

    @Test
    fun `with a translation the repeat waits for the translation to finish`() {
        start(2, 5, RecitationMode.ARABIC_ENGLISH)
        player.setRepeat(RepeatSetting.Ayah(times = null))

        finishCurrentItem()
        runMainLooperUntil { currentMediaId() == "2:5:en" }

        finishCurrentItem()
        runMainLooperUntil { currentMediaId() == "2:5:ar" }
    }

    @Test
    fun `a range loops back and pauses after its last round`() {
        start(2, 3)
        player.setRepeat(RepeatSetting.Range(from = 3, to = 4, times = 2))

        finishCurrentItem()
        runMainLooperUntil { currentMediaId() == "2:4:ar" }
        finishCurrentItem()
        runMainLooperUntil { currentMediaId() == "2:3:ar" && exoPlayer.isPlaying }
        finishCurrentItem()
        runMainLooperUntil { currentMediaId() == "2:4:ar" }
        finishCurrentItem()

        runMainLooperUntil { !exoPlayer.playWhenReady }
        assertEquals("2:4:ar", currentMediaId())
        assertEquals(RepeatSetting.Off, player.nowPlaying.value?.repeat)
        assertFalse(player.nowPlaying.value!!.isPlaying)
    }

    @Test
    fun `choosing a range elsewhere in the surah jumps to it`() {
        start(2, 20)

        player.setRepeat(RepeatSetting.Range(from = 3, to = 4, times = null))

        runMainLooperUntil { currentMediaId() == "2:3:ar" }
    }

    @Test
    fun `moving out of the range by hand drops it`() {
        start(2, 4)
        player.setRepeat(RepeatSetting.Range(from = 3, to = 4, times = null))

        player.nextAyah()

        runMainLooperUntil { currentMediaId() == "2:5:ar" }
        assertEquals(RepeatSetting.Off, player.nowPlaying.value?.repeat)
        assertFalse(exoPlayer.pauseAtEndOfMediaItems)
    }

    @Test
    fun `playing another surah drops the repeat`() {
        start(2, 4)
        player.setRepeat(RepeatSetting.Ayah(times = 3))

        player.play(3, fromAyah = 1, mode = RecitationMode.ARABIC_ONLY)

        runMainLooperUntil { player.nowPlaying.value?.surah == 3 }
        assertEquals(RepeatSetting.Off, player.nowPlaying.value?.repeat)
    }

    @Test
    fun `the sleep timer counts down, fades the volume and pauses`() {
        start(2, 1)
        player.startSleepTimer(SleepOption.Minutes(1))
        shadowOf(Looper.getMainLooper()).idle()
        assertEquals(SleepTimerStatus.Counting(60_000L), player.sleepTimer.value)

        now = 50_000L
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(1))
        assertEquals(SleepTimerStatus.FadingOut(10_000L), player.sleepTimer.value)
        assertEquals(0.707f, exoPlayer.volume, 0.01f)

        now = 60_000L
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(1))
        assertEquals(SleepTimerStatus.Off, player.sleepTimer.value)
        assertFalse(exoPlayer.playWhenReady)
        assertEquals(1f, exoPlayer.volume)
    }

    @Test
    fun `cancelling the sleep timer restores the volume`() {
        start(2, 1)
        player.startSleepTimer(SleepOption.Minutes(1))
        now = 55_000L
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(1))

        player.cancelSleepTimer()

        assertEquals(SleepTimerStatus.Off, player.sleepTimer.value)
        assertEquals(1f, exoPlayer.volume)
        assertTrue(exoPlayer.playWhenReady)
    }

    @Test
    fun `the end-of-surah sleep stop fades over the last ayah and ends with the surah`() {
        start(1, 6)
        player.startSleepTimer(SleepOption.EndOfSurah)
        shadowOf(Looper.getMainLooper()).idle()
        assertEquals(SleepTimerStatus.EndOfSurah, player.sleepTimer.value)

        // The last ayah (10 s) is within the 20 s fade: its remaining time is known, so it fades.
        player.nextAyah()
        runMainLooperUntil { player.sleepTimer.value is SleepTimerStatus.FadingOut }
        assertTrue(exoPlayer.volume < 1f)

        finishCurrentItem()

        runMainLooperUntil { player.sleepTimer.value == SleepTimerStatus.Off }
        assertEquals(1f, exoPlayer.volume)
    }

    @Test
    fun `the end-of-surah sleep stop wins over a range that loops the last ayah`() {
        start(1, 6)
        player.setRepeat(RepeatSetting.Range(from = 6, to = 7, times = null))
        player.startSleepTimer(SleepOption.EndOfSurah)

        finishCurrentItem()
        runMainLooperUntil { currentMediaId() == "1:7:ar" }
        finishCurrentItem()

        runMainLooperUntil { !exoPlayer.playWhenReady }
        assertEquals("1:7:ar", currentMediaId())
        assertEquals(SleepTimerStatus.Off, player.sleepTimer.value)
    }

    @Test
    fun `stop ends the sleep timer and the repeat`() {
        start(2, 1)
        player.setRepeat(RepeatSetting.Ayah(times = 3))
        player.startSleepTimer(SleepOption.Minutes(5))

        player.stop()

        assertEquals(SleepTimerStatus.Off, player.sleepTimer.value)
        assertFalse(exoPlayer.pauseAtEndOfMediaItems)
    }

    @Test
    fun `a speed set by another controller shows up in now playing`() {
        start(2, 1)

        exoPlayer.setPlaybackSpeed(1.25f)

        runMainLooperUntil { player.nowPlaying.value?.speed == PlaybackSpeed.X1_25 }
    }

    @Test
    fun `system media controls get no repeat or shuffle`() {
        assertFalse(player.sessionPlayer.isCommandAvailable(Player.COMMAND_SET_REPEAT_MODE))
        assertFalse(player.sessionPlayer.availableCommands.contains(Player.COMMAND_SET_SHUFFLE_MODE))
        assertTrue(player.sessionPlayer.availableCommands.contains(Player.COMMAND_PLAY_PAUSE))
    }

    /** Every item is a seekable 10 s source. */
    private class TenSecondSources : androidx.media3.exoplayer.source.MediaSource.Factory {
        override fun createMediaSource(mediaItem: androidx.media3.common.MediaItem) = FakeMediaSource(
            FakeTimeline(
                FakeTimeline.TimelineWindowDefinition.Builder()
                    .setUid(mediaItem.mediaId)
                    .setSeekable(true)
                    .setDurationUs(10_000_000L)
                    .setMediaItem(mediaItem)
                    .build(),
            ),
        )

        override fun getSupportedTypes(): IntArray = intArrayOf(androidx.media3.common.C.CONTENT_TYPE_OTHER)

        override fun setDrmSessionManagerProvider(
            drmSessionManagerProvider: androidx.media3.exoplayer.drm.DrmSessionManagerProvider,
        ) = this

        override fun setLoadErrorHandlingPolicy(
            loadErrorHandlingPolicy: androidx.media3.exoplayer.upstream.LoadErrorHandlingPolicy,
        ) = this
    }
}
