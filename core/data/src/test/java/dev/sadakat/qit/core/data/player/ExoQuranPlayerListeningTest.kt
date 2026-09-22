package dev.sadakat.qit.core.data.player

import android.content.Context
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.drm.DrmSessionManagerProvider
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.upstream.LoadErrorHandlingPolicy
import androidx.media3.test.utils.FakeMediaSource
import androidx.media3.test.utils.FakeTimeline
import androidx.media3.test.utils.TestExoPlayerBuilder
import androidx.media3.test.utils.robolectric.RobolectricUtil.runMainLooperUntil
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.sadakat.qit.core.domain.audio.WordTimings
import dev.sadakat.qit.core.domain.model.RecitationMode
import dev.sadakat.qit.core.domain.player.PlaybackProgress
import dev.sadakat.qit.core.domain.player.RepeatSetting
import dev.sadakat.qit.core.domain.player.WordPointer
import dev.sadakat.qit.core.testing.FakeAudioTimings
import dev.sadakat.qit.core.testing.FakeListeningHistory
import dev.sadakat.qit.core.testing.FakeQuranSettings
import dev.sadakat.qit.core.testing.FakeQuranText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** What [ExoQuranPlayer] records as heard, and how it lays the surah out as one timeline. */
@androidx.annotation.OptIn(UnstableApi::class)
@RunWith(AndroidJUnit4::class)
class ExoQuranPlayerListeningTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val history = FakeListeningHistory()
    private val timings = FakeAudioTimings(defaultDurationMs = ITEM_MS)
    private var now = 0L
    private lateinit var exoPlayer: ExoPlayer
    private lateinit var player: ExoQuranPlayer

    @Before
    fun setUp() {
        exoPlayer = TestExoPlayerBuilder(context).setMediaSourceFactory(TwoSecondSources()).build()
        player = ExoQuranPlayer(
            context = context,
            exoPlayer = exoPlayer,
            quranText = FakeQuranText(),
            settings = FakeQuranSettings(),
            timings = timings,
            history = history,
            scope = CoroutineScope(Dispatchers.Main),
            clock = { now },
            wallClock = { WALL_CLOCK_MS },
        )
    }

    @After
    fun tearDown() {
        exoPlayer.release()
    }

    private fun heard(ayah: Int) = history.counts.value.count(112, ayah)

    private fun playToTheEnd(fromAyah: Int, mode: RecitationMode) {
        player.play(112, fromAyah, mode)
        runMainLooperUntil { exoPlayer.playbackState == Player.STATE_ENDED }
        runMainLooperUntil { heard(4) > 0 }
    }

    private fun startPaused(fromAyah: Int) {
        player.play(112, fromAyah, RecitationMode.ARABIC_ONLY)
        runMainLooperUntil { exoPlayer.isPlaying }
        exoPlayer.pause()
        runMainLooperUntil { !exoPlayer.isPlaying }
    }

    @Test
    fun `every ayah heard to its end counts once, the basmala never`() {
        playToTheEnd(fromAyah = 0, mode = RecitationMode.ARABIC_ONLY)

        assertEquals(listOf(1, 1, 1, 1), (1..4).map(::heard))
        assertEquals(WALL_CLOCK_MS, history.counts.value.lastHeardAt[112])
        // 1:1 is the basmala's recording, but the basmala is not Al-Fatiha's first ayah.
        assertEquals(0, history.counts.value.count(1, 1))
    }

    @Test
    fun `the arabic counts, its translation doesn't`() {
        playToTheEnd(fromAyah = 1, mode = RecitationMode.ARABIC_ENGLISH)

        assertEquals(listOf(1, 1, 1, 1), (1..4).map(::heard))
    }

    @Test
    fun `an ayah skipped with next doesn't count`() {
        startPaused(fromAyah = 1)

        player.nextAyah()
        player.togglePlayPause()
        runMainLooperUntil { exoPlayer.playbackState == Player.STATE_ENDED }
        runMainLooperUntil { heard(4) > 0 }

        assertEquals(listOf(0, 1, 1, 1), (1..4).map(::heard))
    }

    /** Also a regression test: only the first ayah used to repeat, the count wasn't reset for the next. */
    @Test
    fun `every memorizing repeat counts`() {
        startPaused(fromAyah = 1)
        player.setRepeat(RepeatSetting.Ayah(times = 3))
        player.togglePlayPause()

        runMainLooperUntil { exoPlayer.playbackState == Player.STATE_ENDED }
        runMainLooperUntil { heard(4) == 3 }

        assertEquals(listOf(3, 3, 3, 3), (1..4).map(::heard))
    }

    @Test
    fun `the time spent listening is added to the surah`() {
        now = 1_000
        player.play(112, 1, RecitationMode.ARABIC_ONLY)
        runMainLooperUntil { exoPlayer.isPlaying }

        now = 6_000
        exoPlayer.pause()

        runMainLooperUntil { history.counts.value.listenedMs[112] == 5_000L }
    }

    @Test
    fun `the session shows and seeks the whole surah`() {
        startPaused(fromAyah = 2) // basmala, 112:1 … 112:4: five 2 s items; 112:2 is the third

        val session = player.sessionPlayer
        assertEquals(5 * ITEM_MS, session.duration)
        assertEquals(5 * ITEM_MS, session.contentDuration)
        exoPlayer.seekTo(2, 500)
        runMainLooperUntil { exoPlayer.currentPosition == 500L }
        assertEquals(2 * ITEM_MS + 500, session.currentPosition)
        assertEquals(2 * ITEM_MS + 500, session.contentPosition)

        session.seekTo(7_000)

        runMainLooperUntil { exoPlayer.currentMediaItem?.mediaId == "112:3:ar" }
        assertEquals(1_000L, exoPlayer.currentPosition)
    }

    @Test
    fun `seeking the surah moves to the ayah at that time`() {
        startPaused(fromAyah = 1)

        player.seekTo(9_000)

        runMainLooperUntil { exoPlayer.currentMediaItem?.mediaId == "112:4:ar" }
        assertEquals(1_000L, exoPlayer.currentPosition)
        assertEquals(4, player.nowPlaying.value?.ayah)
    }

    @Test
    fun `progress reports the position in the item and the surah`() {
        startPaused(fromAyah = 3)
        exoPlayer.seekTo(3, 250)
        var latest: PlaybackProgress? = null
        val collecting = CoroutineScope(Dispatchers.Main).launch { player.progress.collect { latest = it } }

        runMainLooperUntil { latest?.itemPositionMs == 250L }
        assertEquals(PlaybackProgress(250, 3 * ITEM_MS + 250, 5 * ITEM_MS), latest)
        collecting.cancel()
    }

    @Test
    fun `the pointer follows the recited word`() {
        timings.words = mapOf(112 to mapOf(3 to WordTimings(intArrayOf(0, 400, 400, 1_200, 1_200, 2_000))))
        startPaused(fromAyah = 3)
        exoPlayer.seekTo(3, 500)
        var latest: WordPointer? = null
        val collecting = CoroutineScope(Dispatchers.Main).launch { player.pointer.collect { latest = it } }

        runMainLooperUntil { latest == WordPointer.Reciting(1) }
        collecting.cancel()
    }

    @Test
    fun `without the files' lengths the session shows the current file`() {
        timings.defaultDurationMs = null
        startPaused(fromAyah = 1)

        assertEquals(ITEM_MS, player.sessionPlayer.duration)
        player.seekTo(5_000) // no timeline: ignored
        assertEquals("112:1:ar", exoPlayer.currentMediaItem?.mediaId)
    }

    /** Every item is a seekable 2 s source. */
    private class TwoSecondSources : MediaSource.Factory {
        override fun createMediaSource(mediaItem: MediaItem): MediaSource = FakeMediaSource(
            FakeTimeline(
                FakeTimeline.TimelineWindowDefinition.Builder()
                    .setUid(mediaItem.mediaId)
                    .setSeekable(true)
                    .setDurationUs(ITEM_MS * C.MICROS_PER_SECOND / 1_000)
                    .setDefaultPositionUs(0)
                    .setWindowPositionInFirstPeriodUs(0)
                    .setMediaItem(mediaItem)
                    .build(),
            ),
        )

        override fun getSupportedTypes(): IntArray = intArrayOf(C.CONTENT_TYPE_OTHER)

        override fun setDrmSessionManagerProvider(provider: DrmSessionManagerProvider): MediaSource.Factory = this

        override fun setLoadErrorHandlingPolicy(policy: LoadErrorHandlingPolicy): MediaSource.Factory = this
    }

    private companion object {
        const val ITEM_MS = 2_000L
        const val WALL_CLOCK_MS = 1_700_000_000_000L
    }
}
