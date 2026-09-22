package dev.sadakat.qit.core.data.settings

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.sadakat.qit.core.domain.model.ArabicTextSize
import dev.sadakat.qit.core.domain.model.AyahRef
import dev.sadakat.qit.core.domain.model.ReadingPrefs
import dev.sadakat.qit.core.domain.model.RecitationMode
import dev.sadakat.qit.core.domain.model.ThemeMode
import dev.sadakat.qit.core.domain.model.WordByWord
import dev.sadakat.qit.core.domain.player.PlaybackSpeed
import dev.sadakat.qit.core.domain.repository.LastPosition
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters

/**
 * The `preferencesDataStore` delegate is process-wide and keeps its state in memory between test
 * methods, so the tests that read unwritten state must run before the ones that write.
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@RunWith(AndroidJUnit4::class)
class DataStoreQuranSettingsTest {

    private lateinit var settings: DataStoreQuranSettings

    @Before
    fun setUp() {
        settings = DataStoreQuranSettings(ApplicationProvider.getApplicationContext<Context>())
    }

    @Test
    fun `lastPosition is null before anything has played`() = runTest {
        assertNull(settings.lastPosition.first())
    }

    @Test
    fun `mode defaults to arabic and bangla`() = runTest {
        assertEquals(RecitationMode.ARABIC_BANGLA, settings.mode.first())
    }

    @Test
    fun `saveLastPosition round trips the last position`() = runTest {
        val position = LastPosition(AyahRef(surah = 2, ayah = 255), RecitationMode.ARABIC_ENGLISH)

        settings.saveLastPosition(position)

        assertEquals(position, settings.lastPosition.first())
    }

    @Test
    fun `setMode round trips the chosen mode`() = runTest {
        settings.setMode(RecitationMode.ARABIC_ONLY)

        assertEquals(RecitationMode.ARABIC_ONLY, settings.mode.first())
    }

    @Test
    fun `playbackSpeed defaults to normal speed`() = runTest {
        assertEquals(PlaybackSpeed.X1, settings.playbackSpeed.first())
    }

    @Test
    fun `readingPrefs default to the comfortable middle`() = runTest {
        assertEquals(ReadingPrefs(), settings.readingPrefs.first())
    }

    @Test
    fun `setPlaybackSpeed round trips the chosen speed`() = runTest {
        settings.setPlaybackSpeed(PlaybackSpeed.X1_25)

        assertEquals(PlaybackSpeed.X1_25, settings.playbackSpeed.first())
    }

    @Test
    fun `updateReadingPrefs round trips every field and builds on the stored prefs`() = runTest {
        val changed = ReadingPrefs(
            arabicTextSize = ArabicTextSize.XLARGE,
            showTranslation = false,
            followAlong = false,
            wordByWord = WordByWord.BANGLA,
            themeMode = ThemeMode.DARK,
            dynamicColor = true,
        )
        settings.updateReadingPrefs { changed }
        settings.updateReadingPrefs { it.copy(showTranslation = true) }

        assertEquals(changed.copy(showTranslation = true), settings.readingPrefs.first())
    }
}
