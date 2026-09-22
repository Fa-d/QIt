package dev.sadakat.qit.wear.audio

import android.app.Application
import android.content.Context
import android.media.AudioManager
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(application = Application::class)
class AudioManagerStreamVolumeTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    private fun audioManager() = context.getSystemService(AudioManager::class.java)!!

    @Test
    fun `the level starts at the stream's current fraction`() {
        val manager = audioManager()
        val max = manager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val half = max / 2
        manager.setStreamVolume(AudioManager.STREAM_MUSIC, half, 0)

        assertEquals(half.toFloat() / max, AudioManagerStreamVolume(context).level.value, 0.0001f)
    }

    @Test
    fun `adjust moves the stream and the level by whole steps`() {
        val volume = AudioManagerStreamVolume(context)
        val manager = audioManager()
        val max = manager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val before = manager.getStreamVolume(AudioManager.STREAM_MUSIC)

        volume.adjust(2)
        assertEquals(before + 2, manager.getStreamVolume(AudioManager.STREAM_MUSIC))
        assertEquals((before + 2).toFloat() / max, volume.level.value, 0.0001f)

        volume.adjust(-1)
        assertEquals(before + 1, manager.getStreamVolume(AudioManager.STREAM_MUSIC))
        assertEquals((before + 1).toFloat() / max, volume.level.value, 0.0001f)
    }

    @Test
    fun `adjusting past the range clamps instead of throwing`() {
        val volume = AudioManagerStreamVolume(context)
        val manager = audioManager()
        val max = manager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)

        volume.adjust(max * 2)
        assertEquals(max, manager.getStreamVolume(AudioManager.STREAM_MUSIC))
        assertEquals(1f, volume.level.value, 0.0001f)

        volume.adjust(-max * 4)
        assertEquals(0, manager.getStreamVolume(AudioManager.STREAM_MUSIC))
        assertEquals(0f, volume.level.value, 0.0001f)
    }
}
