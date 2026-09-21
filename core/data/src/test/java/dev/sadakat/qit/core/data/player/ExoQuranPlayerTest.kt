package dev.sadakat.qit.core.data.player

import android.content.Context
import android.os.Looper
import androidx.media3.common.AdPlaybackState
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.TransferListener
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.drm.DrmSessionManagerProvider
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.upstream.LoadErrorHandlingPolicy
import androidx.media3.test.utils.FakeMediaSource
import androidx.media3.test.utils.FakeTimeline
import androidx.media3.test.utils.TestExoPlayerBuilder
import androidx.media3.test.utils.robolectric.RobolectricUtil.runMainLooperUntil
import androidx.media3.test.utils.robolectric.TestPlayerRunHelper
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.collect.ImmutableList
import dev.sadakat.qit.core.domain.model.AyahRef
import dev.sadakat.qit.core.domain.model.RecitationMode
import dev.sadakat.qit.core.domain.model.Track
import dev.sadakat.qit.core.domain.player.NowPlaying
import dev.sadakat.qit.core.domain.repository.LastPosition
import dev.sadakat.qit.core.testing.FakeQuranSettings
import dev.sadakat.qit.core.testing.FakeQuranText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import java.util.concurrent.atomic.AtomicBoolean

@androidx.annotation.OptIn(UnstableApi::class)
@RunWith(AndroidJUnit4::class)
class ExoQuranPlayerTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val settings = FakeQuranSettings()
    private val text = FakeQuranText()
    private val players = mutableListOf<ExoPlayer>()
    private lateinit var exoPlayer: ExoPlayer
    private lateinit var player: ExoQuranPlayer

    @Before
    fun setUp() {
        exoPlayer = newPlayer(SeekableFakeMediaSourceFactory())
        player = ExoQuranPlayer(context, exoPlayer, text, settings, CoroutineScope(Dispatchers.Main))
    }

    @After
    fun tearDown() {
        players.forEach(ExoPlayer::release)
    }

    private fun newPlayer(factory: MediaSource.Factory): ExoPlayer =
        TestExoPlayerBuilder(context)
            .setMediaSourceFactory(factory)
            .build()
            .also(players::add)

    private fun nowPlaying(): NowPlaying = player.nowPlaying.value ?: error("Nothing playing")

    /**
     * Pumps both the main and the player's playback looper until [condition] holds: surfacing a
     * playback error needs work on both loopers, and pumping only the main one is not enough.
     */
    private fun runUntil(player: ExoPlayer, condition: () -> Boolean) {
        val playback = shadowOf(player.playbackLooper)
        // Robolectric shadows SystemClock, so measure the deadline with the real JVM clock.
        val deadline = System.nanoTime() + 10_000_000_000L
        while (!condition()) {
            if (System.nanoTime() > deadline) throw AssertionError("Timed out waiting for the player")
            shadowOf(Looper.getMainLooper()).runOneTask()
            playback.idle()
        }
    }

    @Test
    fun `play starts at the requested ayah and publishes NowPlaying`() {
        player.play(2, fromAyah = 255, mode = RecitationMode.ARABIC_ONLY)

        runMainLooperUntil { player.nowPlaying.value?.isPlaying == true }

        assertEquals(
            NowPlaying(2, 255, Track.ARABIC, RecitationMode.ARABIC_ONLY, isPlaying = true, isBuffering = false),
            player.nowPlaying.value,
        )
        assertEquals(255, exoPlayer.currentMediaItemIndex)
        assertEquals("2:255:ar", exoPlayer.currentMediaItem?.mediaId)
    }

    @Test
    fun `nextAyah jumps to the next ayah skipping the translation item`() {
        player.play(2, fromAyah = 255, mode = RecitationMode.ARABIC_ENGLISH)
        runMainLooperUntil { player.nowPlaying.value?.ayah == 255 }

        player.nextAyah()

        runMainLooperUntil { exoPlayer.currentMediaItem?.mediaId == "2:256:ar" }
        assertEquals(256, player.nowPlaying.value?.ayah)
    }

    @Test
    fun `nextAyah stops at the end of the surah`() {
        player.play(2, fromAyah = 286, mode = RecitationMode.ARABIC_ONLY)
        runMainLooperUntil { player.nowPlaying.value?.ayah == 286 }

        player.nextAyah()
        shadowOf(Looper.getMainLooper()).idle()

        assertEquals(286, exoPlayer.currentMediaItemIndex)
        assertEquals(286, player.nowPlaying.value?.ayah)
    }

    @Test
    fun `previousAyah goes back to the previous ayah at its start`() {
        player.play(2, fromAyah = 6, mode = RecitationMode.ARABIC_ONLY)
        runMainLooperUntil { player.nowPlaying.value?.isPlaying == true }
        exoPlayer.pause()
        runMainLooperUntil { !exoPlayer.playWhenReady }

        player.previousAyah()

        runMainLooperUntil { exoPlayer.currentMediaItemIndex == 5 }
        assertEquals("2:5:ar", exoPlayer.currentMediaItem?.mediaId)
        assertEquals(0L, exoPlayer.currentPosition)
    }

    @Test
    fun `previousAyah restarts the current ayah after more than 3 seconds`() {
        player.play(2, fromAyah = 6, mode = RecitationMode.ARABIC_ONLY)
        runMainLooperUntil { player.nowPlaying.value?.isPlaying == true }
        exoPlayer.pause()
        runMainLooperUntil { !exoPlayer.playWhenReady }
        exoPlayer.seekTo(6, 3_500)
        runMainLooperUntil { exoPlayer.currentPosition == 3_500L }

        player.previousAyah()

        runMainLooperUntil { exoPlayer.currentPosition == 0L }
        assertEquals(6, exoPlayer.currentMediaItemIndex)
    }

    @Test
    fun `the last position is saved once per ayah, not once per item`() {
        player.play(1, fromAyah = 1, mode = RecitationMode.ARABIC_ENGLISH)

        runMainLooperUntil { settings.lastPosition.value != null }
        assertEquals(
            LastPosition(AyahRef(1, 1), RecitationMode.ARABIC_ENGLISH),
            settings.lastPosition.value,
        )

        player.nextAyah()
        runMainLooperUntil { settings.lastPosition.value?.ref?.ayah == 2 }

        // Moving to ayah 2's translation does not save ayah 2 again.
        exoPlayer.seekTo(exoPlayer.currentMediaItemIndex + 1, 0)
        runMainLooperUntil { player.nowPlaying.value?.track == Track.ENGLISH }
        assertEquals(
            LastPosition(AyahRef(1, 2), RecitationMode.ARABIC_ENGLISH),
            settings.lastPosition.value,
        )
    }

    @Test
    fun `restoreLast queues the last position without playing`() {
        settings.lastPosition.value = LastPosition(AyahRef(2, 255), RecitationMode.ARABIC_ONLY)

        player.restoreLast(playWhenReady = false)

        runMainLooperUntil { player.nowPlaying.value?.isBuffering == false }
        assertEquals(
            NowPlaying(2, 255, Track.ARABIC, RecitationMode.ARABIC_ONLY, isPlaying = false, isBuffering = false),
            nowPlaying(),
        )
        assertFalse(exoPlayer.playWhenReady)
        assertEquals(255, exoPlayer.currentMediaItemIndex)
    }

    @Test
    fun `restoreLast is a no-op when something is already queued`() {
        player.play(1, fromAyah = 1, mode = RecitationMode.ARABIC_ONLY)
        runMainLooperUntil { player.nowPlaying.value?.isPlaying == true }
        exoPlayer.pause()
        runMainLooperUntil { !exoPlayer.playWhenReady }

        settings.lastPosition.value = LastPosition(AyahRef(2, 255), RecitationMode.ARABIC_ENGLISH)
        player.restoreLast(playWhenReady = true)
        shadowOf(Looper.getMainLooper()).idle()

        assertEquals(7, exoPlayer.mediaItemCount)
        assertEquals("1:1:ar", exoPlayer.currentMediaItem?.mediaId)
        assertFalse(exoPlayer.playWhenReady)
    }

    @Test
    fun `restoreLast with nothing saved leaves the player empty`() {
        player.restoreLast(playWhenReady = true)
        shadowOf(Looper.getMainLooper()).idle()

        assertEquals(0, exoPlayer.mediaItemCount)
        assertNull(player.nowPlaying.value)
    }

    @Test
    fun `stop clears the queue and nowPlaying`() {
        player.play(2, fromAyah = 1, mode = RecitationMode.ARABIC_ONLY)
        runMainLooperUntil { player.nowPlaying.value != null }

        player.stop()

        runMainLooperUntil { exoPlayer.mediaItemCount == 0 }
        assertNull(player.nowPlaying.value)
        assertEquals(Player.STATE_IDLE, exoPlayer.playbackState)
    }

    @Test
    fun `the session player is one instance whose next and previous move by ayah`() {
        assertSame(player.sessionPlayer, player.sessionPlayer)
        player.play(2, fromAyah = 255, mode = RecitationMode.ARABIC_ENGLISH)
        runMainLooperUntil { player.nowPlaying.value?.ayah == 255 }

        player.sessionPlayer.seekToNext()
        runMainLooperUntil { exoPlayer.currentMediaItem?.mediaId == "2:256:ar" }

        player.sessionPlayer.seekToPrevious()
        runMainLooperUntil { exoPlayer.currentMediaItem?.mediaId == "2:255:ar" }
    }

    @Test
    fun `togglePlayPause pauses and resumes`() {
        player.play(2, fromAyah = 1, mode = RecitationMode.ARABIC_ONLY)
        runMainLooperUntil { player.nowPlaying.value?.isPlaying == true }

        player.togglePlayPause()
        runMainLooperUntil { player.nowPlaying.value?.isPlaying == false }

        player.togglePlayPause()
        runMainLooperUntil { player.nowPlaying.value?.isPlaying == true }
    }

    @Test
    fun `togglePlayPause recovers a player left idle with a queue, as after an error`() {
        player.play(2, fromAyah = 1, mode = RecitationMode.ARABIC_ONLY)
        runMainLooperUntil { player.nowPlaying.value != null }
        exoPlayer.stop() // the state an error leaves behind: idle with the queue intact
        runMainLooperUntil { exoPlayer.playbackState == Player.STATE_IDLE }

        player.togglePlayPause()

        // The re-prepare happens on the playback thread; let it complete before waiting for play.
        TestPlayerRunHelper.run(exoPlayer).untilPendingCommandsAreFullyHandled()
        runMainLooperUntil { player.nowPlaying.value?.isPlaying == true }
    }

    @Test
    fun `a playback error shows a short message that the next play clears`() {
        val failing = newPlayer(FailingOnceMediaSourceFactory())
        val failingPlayer = ExoQuranPlayer(context, failing, text, settings, CoroutineScope(Dispatchers.Main))

        failingPlayer.play(2, fromAyah = 1, mode = RecitationMode.ARABIC_ONLY)

        runUntil(failing) { failingPlayer.error.value != null }
        assertEquals("Playback failed. Please try again.", failingPlayer.error.value)
        assertEquals(Player.STATE_IDLE, failing.playbackState)

        failingPlayer.play(2, fromAyah = 1, mode = RecitationMode.ARABIC_ONLY)

        assertNull(failingPlayer.error.value)
    }

    @Test
    fun `network io errors ask to check the connection or download the surah`() {
        val expected = "Can't reach the audio. Check your connection or download this surah."
        assertEquals(expected, ExoQuranPlayer.errorMessage(PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED))
        assertEquals(expected, ExoQuranPlayer.errorMessage(PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT))
        assertEquals(expected, ExoQuranPlayer.errorMessage(PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS))
    }

    @Test
    fun `other errors get a short generic message`() {
        val expected = "Playback failed. Please try again."
        assertEquals(expected, ExoQuranPlayer.errorMessage(PlaybackException.ERROR_CODE_UNSPECIFIED))
        assertEquals(expected, ExoQuranPlayer.errorMessage(PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND))
    }

    /** Media sources whose windows are seekable, 10 s long and start at 0, so seeks up to 10 s work. */
    private class SeekableFakeMediaSourceFactory : MediaSource.Factory {
        override fun createMediaSource(mediaItem: MediaItem): MediaSource = FakeMediaSource(
            FakeTimeline(
                FakeTimeline.TimelineWindowDefinition(
                    /* periodCount = */ 1,
                    mediaItem.mediaId,
                    /* isSeekable = */ true,
                    /* isDynamic = */ false,
                    /* isLive = */ false,
                    /* isPlaceholder = */ false,
                    /* durationUs = */ 10 * C.MICROS_PER_SECOND,
                    /* defaultPositionUs = */ 0,
                    /* windowOffsetInFirstPeriodUs = */ 0,
                    ImmutableList.of(AdPlaybackState.NONE),
                    mediaItem,
                ),
            ),
        )

        override fun getSupportedTypes(): IntArray = intArrayOf(C.CONTENT_TYPE_OTHER)

        override fun setDrmSessionManagerProvider(
            drmSessionManagerProvider: DrmSessionManagerProvider,
        ): MediaSource.Factory = this

        override fun setLoadErrorHandlingPolicy(loadErrorHandlingPolicy: LoadErrorHandlingPolicy): MediaSource.Factory =
            this
    }

    /** Fails the very first preparation (driving the player into an error), then behaves. */
    private class FailingOnceMediaSource(private val failedOnce: AtomicBoolean) : FakeMediaSource() {
        override fun prepareSourceInternal(transferListener: TransferListener?) {
            // Prepare properly first so a later release passes FakeMediaSource's own assertions.
            super.prepareSourceInternal(transferListener)
            if (failedOnce.compareAndSet(false, true)) {
                throw IllegalStateException("source failed")
            }
        }
    }

    private class FailingOnceMediaSourceFactory : MediaSource.Factory {
        // Shared: the queue has one source per item, and only the first preparation may fail.
        private val failedOnce = AtomicBoolean(false)

        override fun createMediaSource(mediaItem: MediaItem): MediaSource = FailingOnceMediaSource(failedOnce)

        override fun getSupportedTypes(): IntArray = intArrayOf(C.CONTENT_TYPE_OTHER)

        override fun setDrmSessionManagerProvider(
            drmSessionManagerProvider: DrmSessionManagerProvider,
        ): MediaSource.Factory = this

        override fun setLoadErrorHandlingPolicy(loadErrorHandlingPolicy: LoadErrorHandlingPolicy): MediaSource.Factory =
            this
    }
}
