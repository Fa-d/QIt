package dev.sadakat.qit.core.data.player

import android.content.Context
import android.content.Intent
import android.os.SystemClock
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.ForwardingPlayer
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import dev.sadakat.qit.core.data.audio.QuranMediaItems
import dev.sadakat.qit.core.domain.audio.QueueItemId
import dev.sadakat.qit.core.domain.audio.QueuePlan
import dev.sadakat.qit.core.domain.audio.SurahTimeline
import dev.sadakat.qit.core.domain.model.AyahRef
import dev.sadakat.qit.core.domain.model.QuranMeta
import dev.sadakat.qit.core.domain.model.RecitationMode
import dev.sadakat.qit.core.domain.player.NowPlaying
import dev.sadakat.qit.core.domain.player.PlaybackProgress
import dev.sadakat.qit.core.domain.player.PlaybackSpeed
import dev.sadakat.qit.core.domain.player.QuranPlayer
import dev.sadakat.qit.core.domain.player.RepeatPolicy
import dev.sadakat.qit.core.domain.player.RepeatProgress
import dev.sadakat.qit.core.domain.player.RepeatSetting
import dev.sadakat.qit.core.domain.player.RepeatStep
import dev.sadakat.qit.core.domain.player.SleepOption
import dev.sadakat.qit.core.domain.player.SleepTimer
import dev.sadakat.qit.core.domain.player.SleepTimerStatus
import dev.sadakat.qit.core.domain.player.WordPointer
import dev.sadakat.qit.core.domain.repository.AudioTimings
import dev.sadakat.qit.core.domain.repository.LastPosition
import dev.sadakat.qit.core.domain.repository.ListeningHistory
import dev.sadakat.qit.core.domain.repository.QuranSettings
import dev.sadakat.qit.core.domain.repository.QuranText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.max

/**
 * [QuranPlayer] on the app-wide [ExoPlayer]
 * (built with [dev.sadakat.qit.core.data.audio.QuranCache.playbackDataSourceFactory]).
 * [scope] runs on the main thread. When playback starts it starts the app's MediaSessionService
 * (resolved through its `androidx.media3.session.MediaSessionService` intent filter), so playback
 * survives the app going to the background.
 *
 * Repeats and the end-of-surah sleep stop act at ayah boundaries: when the current item is the last
 * of an ayah where [RepeatPolicy] intervenes, the player pauses exactly at its end
 * (`pauseAtEndOfMediaItems`) and the policy's step is applied there — no blip of the next ayah.
 * The sleep timer ticks on [clock] (a monotonic clock) and fades the volume out before it stops.
 *
 * The file lengths from [timings] lay the queue end to end ([SurahTimeline]): [progress] and
 * [seekTo] work on the whole surah, and so does the media session ([sessionPlayer]), whose
 * notification then shows and seeks the surah instead of one ayah's file. What is heard is written
 * to [history] by a [ListeningRecorder].
 */
// pauseAtEndOfMediaItems and ForwardingPlayer are marked unstable, but have been stable in practice since Media3 1.0.
// flatMapLatest: a position change restarts the ticker; mapLatest: a new surah drops a stale lookup.
@OptIn(UnstableApi::class, ExperimentalCoroutinesApi::class)
@Suppress("LongParameterList") // Its collaborators, all injected: splitting them up would only hide that.
class ExoQuranPlayer(
    private val context: Context,
    private val exoPlayer: ExoPlayer,
    private val quranText: QuranText,
    private val settings: QuranSettings,
    private val timings: AudioTimings,
    history: ListeningHistory,
    private val scope: CoroutineScope,
    private val clock: () -> Long = SystemClock::elapsedRealtime,
    wallClock: () -> Long = System::currentTimeMillis,
) : QuranPlayer {

    private val _nowPlaying = MutableStateFlow<NowPlaying?>(null)
    override val nowPlaying: StateFlow<NowPlaying?> = _nowPlaying.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    override val error: StateFlow<String?> = _error.asStateFlow()

    private val _sleepTimer = MutableStateFlow<SleepTimerStatus>(SleepTimerStatus.Off)
    override val sleepTimer: StateFlow<SleepTimerStatus> = _sleepTimer.asStateFlow()

    private var speed: PlaybackSpeed = PlaybackSpeed.X1
    private var repeat: RepeatSetting = RepeatSetting.Off
    private var repeatProgress = RepeatProgress()

    /**
     * True from a boundary pause until playback runs again: the pause is ours, not the listener's,
     * so [NowPlaying.isPlaying] stays true instead of blinking to "paused" for a moment.
     */
    private var crossingBoundary = false

    private var sleep: SleepTimer? = null
    private var sleepTicker: Job? = null

    /** Mode of the queued surah; the mode in [NowPlaying] comes from here, not from the items. */
    private var mode: RecitationMode = RecitationMode.ARABIC_BANGLA

    /** Last position already persisted for the current queue, to save once per ayah, not per item. */
    private var savedPosition: AyahRef? = null

    private var session: Player? = null

    /** The queued surah laid end to end; null while nothing is queued or the file lengths are unknown. */
    private var timeline: SurahTimeline? = null

    /** Bumped whenever the position moves other than by playing on: restarts the [progress] ticker. */
    private val positionMoves = MutableStateFlow(0)

    override val progress: Flow<PlaybackProgress> = positionMoves
        .flatMapLatest {
            flow {
                emit(currentProgress())
                while (exoPlayer.isPlaying) {
                    delay(PROGRESS_TICK_MS)
                    emit(currentProgress())
                }
            }
        }
        .distinctUntilChanged()
        .flowOn(Dispatchers.Main)

    /** The playing surah's word timings, loaded once per surah. */
    private val surahWords = _nowPlaying
        .map { it?.surah }
        .distinctUntilChanged()
        .mapLatest { surah -> surah?.let { timings.wordTimings(it) }.orEmpty() }
        .catch { emit(emptyMap()) }

    override val pointer: Flow<WordPointer> =
        combine(_nowPlaying, progress, surahWords, WordPointer::of).distinctUntilChanged()

    /** The player to hand to the MediaSession: next/previous from the notification move by ayah, not by item. */
    val sessionPlayer: Player
        get() = session ?: AyahAwarePlayer().also { session = it }

    private val listener = object : Player.Listener {

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            updateBoundaryStop()
            publish()
        }

        override fun onPositionDiscontinuity(
            oldPosition: Player.PositionInfo,
            newPosition: Player.PositionInfo,
            reason: Int,
        ) {
            positionMoves.value++
            if (reason == Player.DISCONTINUITY_REASON_AUTO_TRANSITION) {
                onAyahPlayedThrough(
                    QueueItemId.parse(oldPosition.mediaItem?.mediaId),
                    QueueItemId.parse(newPosition.mediaItem?.mediaId),
                )
            }
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            if (isPlaying) {
                crossingBoundary = false
                _error.value = null
            }
            positionMoves.value++
            publish()
        }

        override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
            if (!playWhenReady && reason == Player.PLAY_WHEN_READY_CHANGE_REASON_END_OF_MEDIA_ITEM) onBoundary()
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            // With a boundary pause pending, the end-of-item event follows and onBoundary() decides
            // (a looping range must not outlive the end-of-surah stop); otherwise the surah just ended.
            val boundaryPending = exoPlayer.pauseAtEndOfMediaItems
            if (playbackState == Player.STATE_ENDED && sleep?.option == SleepOption.EndOfSurah && !boundaryPending) {
                finishSleepTimer()
            }
            publish()
        }

        override fun onPlaybackParametersChanged(playbackParameters: PlaybackParameters) {
            // Another controller (the system's media controls) may change the speed too.
            speed = PlaybackSpeed.entries.minBy { abs(it.factor - playbackParameters.speed) }
            publish()
        }

        override fun onPlayerError(error: PlaybackException) {
            _error.value = errorMessage(error.errorCode)
        }
    }

    init {
        exoPlayer.addListener(listener)
        exoPlayer.addListener(ListeningRecorder(exoPlayer, history, scope, clock, wallClock))
    }

    /** Replaces the queue with [surah] in [mode] and starts at [fromAyah] (0 = basmala). */
    override fun play(surah: Int, fromAyah: Int, mode: RecitationMode) {
        _error.value = null
        // A repeat belongs to its surah: moving within it follows the manual-move rules, a new surah drops it.
        if (_nowPlaying.value?.surah == surah) {
            moveRepeat(fromAyah)
        } else {
            repeat = RepeatSetting.Off
            repeatProgress = RepeatProgress()
        }
        scope.launch {
            this@ExoQuranPlayer.mode = mode
            savedPosition = null
            applySpeed(settings.playbackSpeed.first())
            val items = QuranMediaItems.build(quranText.surah(surah), mode)
            timeline = timelineOf(surah, mode)
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
            // After an error the player sits idle with the queue intact; prepare() re-arms it. This is
            // a retry, so the old error goes: a repeat failure must be reported as a new one.
            _error.value = null
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
        finishSleepTimer()
        repeat = RepeatSetting.Off
        repeatProgress = RepeatProgress()
        crossingBoundary = false
        exoPlayer.pauseAtEndOfMediaItems = false
        exoPlayer.stop()
        exoPlayer.clearMediaItems()
        timeline = null
        _nowPlaying.value = null
    }

    /** Moves to [surahPositionMs] into the queued surah; the repeat follows as for any manual move. */
    override fun seekTo(surahPositionMs: Long) {
        val line = timeline?.takeIf { it.itemCount == exoPlayer.mediaItemCount } ?: return
        val point = line.locate(surahPositionMs)
        queueIds().getOrNull(point.index)?.let { moveRepeat(it.ayah) }
        exoPlayer.seekTo(point.index, point.positionInItemMs)
        updateBoundaryStop()
        publish()
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
            applySpeed(settings.playbackSpeed.first())
            val items = QuranMediaItems.build(quranText.surah(last.ref.surah), last.mode)
            timeline = timelineOf(last.ref.surah, last.mode)
            exoPlayer.setMediaItems(items, QueuePlan.indexOfAyah(ids(items), last.ref.ayah), 0)
            exoPlayer.prepare()
            exoPlayer.playWhenReady = playWhenReady
            if (playWhenReady) {
                // Resuming playback must survive the app going to the background just like play().
                startPlaybackService()
            }
        }
    }

    override fun setRepeat(repeat: RepeatSetting) {
        this.repeat = repeat
        repeatProgress = RepeatProgress()
        val current = currentId()
        RepeatPolicy.entryAyah(repeat, current?.ayah ?: 0)?.let { seekToAyah(it) }
        updateBoundaryStop()
        publish()
    }

    override fun setSpeed(speed: PlaybackSpeed) {
        applySpeed(speed)
        scope.launch { settings.setPlaybackSpeed(speed) }
    }

    override fun setSleepTimer(option: SleepOption?) {
        if (option == null) {
            finishSleepTimer()
            return
        }
        sleepTicker?.cancel()
        sleep = SleepTimer(option, startedAtMs = clock())
        sleepTicker = scope.launch {
            while (true) {
                val status = tickSleepTimer() ?: break
                delay(if (status is SleepTimerStatus.FadingOut) FADE_TICK_MS else TICK_MS)
            }
        }
    }

    /**
     * One sleep timer tick: publishes the countdown, sets the fade volume, and pauses when the timer is
     * due. Returns the new status, or null once the timer is over.
     */
    private fun tickSleepTimer(): SleepTimerStatus? {
        val timer = sleep ?: return null
        val now = clock()
        if (timer.isDue(now)) {
            exoPlayer.pause()
            finishSleepTimer()
            return null
        }
        val remainingSurah = remainingSurahMs()
        exoPlayer.volume = timer.volume(now, remainingSurah, speed.factor)
        return timer.status(now, remainingSurah, speed.factor).also { _sleepTimer.value = it }
    }

    private fun finishSleepTimer() {
        sleepTicker?.cancel()
        sleepTicker = null
        sleep = null
        exoPlayer.volume = 1f
        _sleepTimer.value = SleepTimerStatus.Off
    }

    /** Playing time left in the surah, known once its last item plays (earlier items' lengths aren't loaded). */
    private fun remainingSurahMs(): Long? {
        val onLastItem = exoPlayer.mediaItemCount > 0 && exoPlayer.currentMediaItemIndex == exoPlayer.mediaItemCount - 1
        if (!onLastItem || exoPlayer.duration == C.TIME_UNSET) return null
        return (exoPlayer.duration - exoPlayer.currentPosition).coerceAtLeast(0)
    }

    /**
     * The current item ended and the player paused there because a repeat or the sleep timer takes
     * over at this ayah's end (see [updateBoundaryStop]).
     */
    private fun onBoundary() {
        val id = currentId() ?: return
        if (isEndOfSurahStop(id)) {
            finishSleepTimer()
            publish()
            return
        }
        val decision = RepeatPolicy.afterAyah(repeat, repeatProgress, id.ayah)
        repeatProgress = decision.progress
        when (val step = decision.step) {
            RepeatStep.Advance -> resumeAcrossBoundary()

            is RepeatStep.JumpTo -> {
                seekToAyah(step.ayah)
                resumeAcrossBoundary()
            }

            RepeatStep.Finish -> repeat = RepeatSetting.Off
        }
        updateBoundaryStop()
        publish()
    }

    /**
     * Playback ran on from [from] into [to] without a boundary pause. When that leaves an ayah, the
     * repeat policy had nothing to do there (else it would have paused), but the count still moves
     * on: an ayah repeat's next ayah starts counting afresh.
     */
    private fun onAyahPlayedThrough(from: QueueItemId?, to: QueueItemId?) {
        if (from == null || to == null || from.ayah == to.ayah) return
        val decision = RepeatPolicy.afterAyah(repeat, repeatProgress, from.ayah)
        if (decision.step == RepeatStep.Advance) repeatProgress = decision.progress
        updateBoundaryStop()
    }

    private fun resumeAcrossBoundary() {
        crossingBoundary = true
        exoPlayer.play()
    }

    /**
     * Pauses at the end of the current item only when it's the last item of an ayah where a repeat
     * intervenes, or where the end-of-surah sleep stop falls (a repeat would otherwise carry on).
     */
    private fun updateBoundaryStop() {
        val ids = queueIds()
        val index = exoPlayer.currentMediaItemIndex
        val id = ids.getOrNull(index)
        val lastOfAyah = id != null && ids.getOrNull(index + 1)?.ayah != id.ayah
        exoPlayer.pauseAtEndOfMediaItems =
            id != null && lastOfAyah &&
            (RepeatPolicy.intervenesAfter(repeat, repeatProgress, id.ayah) || isEndOfSurahStop(id))
    }

    /** Whether [id] ends the surah while the sleep timer is set to stop there. */
    private fun isEndOfSurahStop(id: QueueItemId) =
        sleep?.option == SleepOption.EndOfSurah && id.ayah == QuranMeta.ayahCount(id.surah)

    private fun moveRepeat(targetAyah: Int) {
        val (setting, progress) = RepeatPolicy.onManualMove(repeat, repeatProgress, targetAyah)
        repeat = setting
        repeatProgress = progress
    }

    private fun seekToAyah(ayah: Int) {
        exoPlayer.seekTo(QueuePlan.indexOfAyah(queueIds(), ayah), 0)
    }

    private fun currentId(): QueueItemId? = exoPlayer.currentMediaItem?.mediaId?.let(QueueItemId::parse)

    /** [surah]'s queue in [mode] laid end to end, or null if any of its files' lengths is unknown. */
    private suspend fun timelineOf(surah: Int, mode: RecitationMode): SurahTimeline? {
        val durations = QueuePlan.plan(surah, mode).map { entry -> timings.durationMs(entry.file.id) ?: return null }
        return SurahTimeline(durations)
    }

    /** The timeline of the queue as it is now; null if it doesn't match the queue. */
    private fun currentTimeline(): SurahTimeline? = timeline?.takeIf { it.itemCount == exoPlayer.mediaItemCount }

    /** [positionInItemMs] into the current item as a position in the surah; unchanged without a timeline. */
    private fun surahPosition(positionInItemMs: Long): Long =
        currentTimeline()?.positionOf(exoPlayer.currentMediaItemIndex, positionInItemMs) ?: positionInItemMs

    private fun currentProgress(): PlaybackProgress {
        val line = currentTimeline()
        val inItem = exoPlayer.currentPosition
        return PlaybackProgress(
            itemPositionMs = inItem,
            surahPositionMs = line?.positionOf(exoPlayer.currentMediaItemIndex, inItem) ?: 0,
            surahDurationMs = line?.durationMs ?: 0,
        )
    }

    /** Sets the playback speed; ExoPlayer keeps the pitch natural. */
    private fun applySpeed(speed: PlaybackSpeed) {
        this.speed = speed
        exoPlayer.setPlaybackSpeed(speed.factor)
        publish()
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
            isPlaying = exoPlayer.isPlaying || crossingBoundary,
            isBuffering = exoPlayer.playbackState == Player.STATE_BUFFERING,
            speed = speed,
            repeat = repeat,
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
        moveRepeat(ids[target].ayah)
        exoPlayer.seekTo(target, 0)
        updateBoundaryStop()
        publish()
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

    /**
     * Forwards the session's next/previous buttons to the ayah-wise seeks, and hides repeat and
     * shuffle from system media controls: Media3's repeat would loop a single *track* (just the
     * Arabic of an ayah, or just its translation) and shuffle has no meaning for a surah. Repeat is
     * QIt's own, in the app.
     *
     * Positions and the duration are the whole surah's (once its file lengths are known), so the
     * notification's seek bar runs through the surah instead of restarting with every file, and
     * dragging it moves across ayahs.
     */
    @Suppress("TooManyFunctions") // One-line overrides of the Player it forwards to.
    private inner class AyahAwarePlayer : ForwardingPlayer(exoPlayer) {
        override fun seekToNext() = this@ExoQuranPlayer.nextAyah()

        override fun seekToNextMediaItem() = this@ExoQuranPlayer.nextAyah()

        override fun seekToPrevious() = this@ExoQuranPlayer.previousAyah()

        override fun seekToPreviousMediaItem() = this@ExoQuranPlayer.previousAyah()

        override fun getDuration(): Long = currentTimeline()?.durationMs ?: super.getDuration()

        override fun getContentDuration(): Long = currentTimeline()?.durationMs ?: super.getContentDuration()

        override fun getCurrentPosition(): Long = surahPosition(super.getCurrentPosition())

        override fun getContentPosition(): Long = surahPosition(super.getContentPosition())

        override fun getBufferedPosition(): Long = surahPosition(super.getBufferedPosition())

        override fun getContentBufferedPosition(): Long = surahPosition(super.getContentBufferedPosition())

        override fun seekTo(positionMs: Long) {
            if (currentTimeline() == null) super.seekTo(positionMs) else this@ExoQuranPlayer.seekTo(positionMs)
        }

        override fun isCommandAvailable(command: Int): Boolean =
            command !in HIDDEN_SESSION_COMMANDS && super.isCommandAvailable(command)

        override fun getAvailableCommands(): Player.Commands = super.getAvailableCommands().buildUpon()
            .remove(Player.COMMAND_SET_REPEAT_MODE)
            .remove(Player.COMMAND_SET_SHUFFLE_MODE)
            .build()
    }

    companion object {
        private const val TAG = "ExoQuranPlayer"
        private const val ACTION_MEDIA_SESSION_SERVICE = "androidx.media3.session.MediaSessionService"
        private const val TICK_MS = 1_000L

        /** How often [progress] samples while playing: fine enough for the word pointer. */
        private const val PROGRESS_TICK_MS = 50L

        /** Fading ticks faster so the volume steps are inaudible. */
        private const val FADE_TICK_MS = 100L

        private val HIDDEN_SESSION_COMMANDS = setOf(Player.COMMAND_SET_REPEAT_MODE, Player.COMMAND_SET_SHUFFLE_MODE)

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
