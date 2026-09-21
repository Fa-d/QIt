package dev.sadakat.qit.core.data.player

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.ForwardingPlayer
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import dev.sadakat.qit.core.data.audio.QuranMediaItems
import dev.sadakat.qit.core.domain.audio.QueueItemId
import dev.sadakat.qit.core.domain.audio.QueuePlan
import dev.sadakat.qit.core.domain.model.AyahRef
import dev.sadakat.qit.core.domain.model.RecitationMode
import dev.sadakat.qit.core.domain.player.NowPlaying
import dev.sadakat.qit.core.domain.player.QuranPlayer
import dev.sadakat.qit.core.domain.repository.LastPosition
import dev.sadakat.qit.core.domain.repository.QuranSettings
import dev.sadakat.qit.core.domain.repository.QuranText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.math.max

/**
 * [QuranPlayer] on the app-wide [ExoPlayer]
 * (built with [dev.sadakat.qit.core.data.audio.QuranCache.playbackDataSourceFactory]).
 * [scope] runs on the main thread. When playback starts it starts the app's MediaSessionService
 * (resolved through its `androidx.media3.session.MediaSessionService` intent filter), so playback
 * survives the app going to the background.
 */
class ExoQuranPlayer(
    private val context: Context,
    private val exoPlayer: ExoPlayer,
    private val quranText: QuranText,
    private val settings: QuranSettings,
    private val scope: CoroutineScope,
) : QuranPlayer {

    private val _nowPlaying = MutableStateFlow<NowPlaying?>(null)
    override val nowPlaying: StateFlow<NowPlaying?> = _nowPlaying.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    override val error: StateFlow<String?> = _error.asStateFlow()

    /** Mode of the queued surah; the mode in [NowPlaying] comes from here, not from the items. */
    private var mode: RecitationMode = RecitationMode.ARABIC_BANGLA

    /** Last position already persisted for the current queue, to save once per ayah, not per item. */
    private var savedPosition: AyahRef? = null

    private var session: Player? = null

    /** The player to hand to the MediaSession: next/previous from the notification move by ayah, not by item. */
    val sessionPlayer: Player
        get() = session ?: AyahAwarePlayer().also { session = it }

    private val listener = object : Player.Listener {

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) = publish()

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            publish()
            if (isPlaying) _error.value = null
        }

        override fun onPlaybackStateChanged(playbackState: Int) = publish()

        override fun onPlayerError(error: PlaybackException) {
            _error.value = errorMessage(error.errorCode)
        }
    }

    init {
        exoPlayer.addListener(listener)
    }

    /** Replaces the queue with [surah] in [mode] and starts at [fromAyah] (0 = basmala). */
    override fun play(surah: Int, fromAyah: Int, mode: RecitationMode) {
        _error.value = null
        scope.launch {
            this@ExoQuranPlayer.mode = mode
            savedPosition = null
            val items = QuranMediaItems.build(quranText.surah(surah), mode)
            exoPlayer.setMediaItems(items, QueuePlan.indexOfAyah(ids(items), fromAyah), 0)
            exoPlayer.prepare()
            exoPlayer.play()
            startPlaybackService()
        }
    }

    override fun togglePlayPause() {
        val idleWithQueue = exoPlayer.playbackState == Player.STATE_IDLE && exoPlayer.mediaItemCount > 0
        val endedWithQueue = exoPlayer.playbackState == Player.STATE_ENDED && exoPlayer.mediaItemCount > 0
        if (idleWithQueue) {
            // After an error the player sits idle with the queue intact; prepare() re-arms it.
            exoPlayer.prepare()
        } else if (endedWithQueue) {
            // play() alone never leaves STATE_ENDED, so a finished surah would not replay;
            // start the current (last played) ayah from its beginning instead.
            exoPlayer.seekTo(exoPlayer.currentMediaItemIndex, 0)
        }
        // An idle or ended player has nothing to pause: the useful action there is to play.
        if (!idleWithQueue && !endedWithQueue && exoPlayer.playWhenReady) {
            exoPlayer.pause()
        } else {
            exoPlayer.play()
        }
    }

    /** Jumps to the first item of the next ayah (stops at the end of the surah). */
    override fun nextAyah() = seekByAyah { ids, index -> QueuePlan.nextAyahIndex(ids, index) }

    /** Restarts the current ayah if it has played for more than 3 s, else jumps to the previous ayah. */
    override fun previousAyah() =
        seekByAyah { ids, index -> QueuePlan.previousAyahIndex(ids, index, exoPlayer.currentPosition) }

    override fun stop() {
        exoPlayer.stop()
        exoPlayer.clearMediaItems()
        _nowPlaying.value = null
    }

    /**
     * Queues the last saved position, paused unless [playWhenReady]. No-op if nothing is saved or
     * something is queued.
     */
    override fun restoreLast(playWhenReady: Boolean) {
        if (exoPlayer.mediaItemCount > 0) return
        scope.launch {
            val last = settings.lastPosition.first() ?: return@launch
            // play() may have raced in while we were reading the settings.
            if (exoPlayer.mediaItemCount > 0) return@launch
            mode = last.mode
            savedPosition = AyahRef(last.ref.surah, max(last.ref.ayah, 1))
            val items = QuranMediaItems.build(quranText.surah(last.ref.surah), last.mode)
            exoPlayer.setMediaItems(items, QueuePlan.indexOfAyah(ids(items), last.ref.ayah), 0)
            exoPlayer.prepare()
            exoPlayer.playWhenReady = playWhenReady
        }
    }

    /** Updates [nowPlaying] (and the saved position) from the player's current item and state. */
    private fun publish() {
        val id = exoPlayer.currentMediaItem?.mediaId?.let(QueueItemId::parse)
        if (id == null) {
            _nowPlaying.value = null
            return
        }
        _nowPlaying.value = NowPlaying(
            surah = id.surah,
            ayah = id.ayah,
            track = id.track,
            mode = mode,
            isPlaying = exoPlayer.isPlaying,
            isBuffering = exoPlayer.playbackState == Player.STATE_BUFFERING,
        )
        savePosition(id)
    }

    /** Persists the position whenever the ayah changes — moving to an item of the same ayah does not. */
    private fun savePosition(id: QueueItemId) {
        val ref = AyahRef(id.surah, max(id.ayah, 1))
        if (ref == savedPosition) return
        savedPosition = ref
        val position = LastPosition(ref, mode)
        scope.launch { settings.saveLastPosition(position) }
    }

    private fun seekByAyah(targetIndex: (List<QueueItemId>, Int) -> Int?) {
        val ids = queueIds()
        if (ids.isEmpty()) return
        val target = targetIndex(ids, exoPlayer.currentMediaItemIndex) ?: return
        exoPlayer.seekTo(target, 0)
    }

    private fun queueIds(): List<QueueItemId> = ids((0 until exoPlayer.mediaItemCount).map(exoPlayer::getMediaItemAt))

    private fun ids(items: List<MediaItem>): List<QueueItemId> = items.mapNotNull { QueueItemId.parse(it.mediaId) }

    /**
     * Starts the app's MediaSessionService so playback and its notification outlive the activity.
     * Resolved by intent filter because the service lives in the app module, not here.
     */
    private fun startPlaybackService() {
        try {
            val intent = Intent(ACTION_MEDIA_SESSION_SERVICE).setPackage(context.packageName)
            if (context.packageManager.queryIntentServices(intent, 0).isEmpty()) return
            context.startService(intent)
        } catch (e: IllegalStateException) {
            Log.w(TAG, "Could not start the media session service", e)
        } catch (e: SecurityException) {
            Log.w(TAG, "Could not start the media session service", e)
        }
    }

    /** Forwards the session's next/previous buttons to the ayah-wise seeks. */
    @OptIn(UnstableApi::class) // ForwardingPlayer's constructor; stable in practice since Media3 1.0.
    private inner class AyahAwarePlayer : ForwardingPlayer(exoPlayer) {
        override fun seekToNext() = this@ExoQuranPlayer.nextAyah()

        override fun seekToNextMediaItem() = this@ExoQuranPlayer.nextAyah()

        override fun seekToPrevious() = this@ExoQuranPlayer.previousAyah()

        override fun seekToPreviousMediaItem() = this@ExoQuranPlayer.previousAyah()
    }

    companion object {
        private const val TAG = "ExoQuranPlayer"
        private const val ACTION_MEDIA_SESSION_SERVICE = "androidx.media3.session.MediaSessionService"

        /** What to tell the user about [errorCode]; internal so the mapping can be tested. */
        internal fun errorMessage(errorCode: Int): String = when (errorCode) {
            PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
            PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT,
            PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS,
            ->
                "Can't reach the audio. Check your connection or download this surah."
            else -> "Playback failed. Please try again."
        }
    }
}
