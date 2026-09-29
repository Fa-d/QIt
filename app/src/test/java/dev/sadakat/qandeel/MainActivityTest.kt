package dev.sadakat.qandeel

import android.graphics.drawable.ColorDrawable
import androidx.compose.ui.graphics.toArgb
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import dev.sadakat.qandeel.core.data.settings.DataStoreQuranSettings
import dev.sadakat.qandeel.core.designsystem.skin.QandeelSkins
import dev.sadakat.qandeel.core.designsystem.skin.QandeelStyle
import dev.sadakat.qandeel.core.designsystem.skin.QandeelTone
import dev.sadakat.qandeel.core.domain.model.ThemeMode
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
@Config(application = HiltTestApplication::class)
class MainActivityTest {

    @get:Rule
    val hilt = HiltAndroidRule(this)

    @Test
    fun `the cold-start window paints the app's tone on a light system`() = runBlocking {
        // Robolectric's day mode is the light system: with the app set to dark, the window must
        // paint the dark page in onCreate, not the bright paper the system's day would pick.
        val settings = DataStoreQuranSettings(ApplicationProvider.getApplicationContext())
        settings.updateReadingPrefs { it.copy(themeMode = ThemeMode.DARK) }

        val activity = Robolectric.buildActivity(MainActivity::class.java).create().get()

        val page = (shadowOf(activity.window).backgroundDrawable as ColorDrawable).color
        assertEquals(QandeelSkins.of(QandeelStyle.MUSHAF, QandeelTone.DARK).colors.background.toArgb(), page)
    }

    @Test
    fun `the cold-start window paints a sepia app's paper on a light system`() = runBlocking {
        val settings = DataStoreQuranSettings(ApplicationProvider.getApplicationContext())
        settings.updateReadingPrefs { it.copy(themeMode = ThemeMode.SEPIA) }

        val activity = Robolectric.buildActivity(MainActivity::class.java).create().get()

        val page = (shadowOf(activity.window).backgroundDrawable as ColorDrawable).color
        assertEquals(QandeelSkins.of(QandeelStyle.MUSHAF, QandeelTone.SEPIA).colors.background.toArgb(), page)
    }
}
