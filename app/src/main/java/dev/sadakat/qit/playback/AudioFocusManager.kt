package dev.sadakat.qit.playback

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages audio focus for the music player.
 * Handles audio interruptions, ducking, and focus loss.
 */
@Singleton
class AudioFocusManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "AudioFocusManager"
    }

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    private val _hasAudioFocus = MutableStateFlow(false)
    val hasAudioFocus: StateFlow<Boolean> = _hasAudioFocus.asStateFlow()

    private val _isDucked = MutableStateFlow(false)
    val isDucked: StateFlow<Boolean> = _isDucked.asStateFlow()

    private var audioFocusRequest: AudioFocusRequest? = null
    private var onAudioFocusChangeListener: AudioManager.OnAudioFocusChangeListener? = null

    init {
        setupAudioFocusRequest()
    }

    /**
     * Request audio focus for music playback
     */
    fun requestAudioFocus(): Boolean {
        val result = audioManager.requestAudioFocus(
            audioFocusRequest ?: return false
        )

        val granted = result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        _hasAudioFocus.value = granted

        return granted
    }

    /**
     * Abandon audio focus
     */
    fun abandonAudioFocus() {
        audioFocusRequest?.let { request ->
            audioManager.abandonAudioFocusRequest(request)
        }
        _hasAudioFocus.value = false
        _isDucked.value = false
    }

    /**
     * Check if we currently have audio focus
     */
    fun hasFocus(): Boolean = _hasAudioFocus.value

    /**
     * Get the current volume multiplier for ducking
     */
    fun getVolumeMultiplier(): Float {
        return if (_isDucked.value) 0.3f else 1.0f
    }

    /**
     * Setup the audio focus request with proper attributes
     */
    private fun setupAudioFocusRequest() {
        onAudioFocusChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
            handleAudioFocusChange(focusChange)
        }

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build()

        audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setAudioAttributes(audioAttributes)
            .setWillPauseWhenDucked(false) // We'll handle ducking ourselves
            .setOnAudioFocusChangeListener(onAudioFocusChangeListener!!)
            .build()
    }

    /**
     * Handle different audio focus changes
     */
    private fun handleAudioFocusChange(focusChange: Int) {
        when (focusChange) {
            AudioManager.AUDIOFOCUS_GAIN -> {
                // We have full audio focus
                _hasAudioFocus.value = true
                _isDucked.value = false
            }

            AudioManager.AUDIOFOCUS_LOSS -> {
                // Permanent loss of audio focus
                _hasAudioFocus.value = false
                _isDucked.value = false
                // PlaybackManager should handle pausing
            }

            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                // Temporary loss of audio focus
                _hasAudioFocus.value = false
                _isDucked.value = false
                // PlaybackManager should handle pausing
            }

            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                // Another app wants to play audio, we can duck our volume
                _hasAudioFocus.value = true
                _isDucked.value = true
            }
        }
    }
}