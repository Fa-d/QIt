package dev.sadakat.qit.wear.audio

import android.content.Context
import android.media.AudioManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The watch's media volume, turned by the crown on Now playing. An app-local port rather than a
 * domain one: volume is a device concern the phone doesn't share.
 */
interface StreamVolume {

    /** The current volume as a fraction 0..1 of the music stream's range. */
    val level: StateFlow<Float>

    /** Moves the volume by [steps] stream increments; negative steps lower it. */
    fun adjust(steps: Int)
}

/** [StreamVolume] over [AudioManager]'s music stream — what the recitation plays on. */
@Singleton
class AudioManagerStreamVolume @Inject constructor(@ApplicationContext context: Context) : StreamVolume {

    private val audioManager = context.getSystemService(AudioManager::class.java)

    // Zero never happens in practice; guard anyway so the fraction stays defined.
    private val max = (audioManager?.getStreamMaxVolume(AudioManager.STREAM_MUSIC) ?: 0).coerceAtLeast(1)

    private val levelState = MutableStateFlow(currentFraction())
    override val level: StateFlow<Float> = levelState

    override fun adjust(steps: Int) {
        val manager = audioManager ?: return
        if (steps == 0) return
        // One set of the target index rather than N raises/lowers: atomic, and no per-step UI.
        val target = (manager.getStreamVolume(AudioManager.STREAM_MUSIC) + steps).coerceIn(0, max)
        // No flags: silent, without the system volume overlay.
        manager.setStreamVolume(AudioManager.STREAM_MUSIC, target, 0)
        levelState.value = target.toFloat() / max
    }

    private fun currentFraction(): Float =
        (audioManager?.getStreamVolume(AudioManager.STREAM_MUSIC) ?: 0).toFloat() / max
}
