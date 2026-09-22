package dev.sadakat.qit.wear.tile

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.wear.protolayout.DeviceParametersBuilders
import androidx.wear.protolayout.LayoutElementBuilders.LayoutElement
import androidx.wear.protolayout.ModifiersBuilders.Clickable
import androidx.wear.protolayout.expression.VersionBuilders.VersionInfo
import androidx.wear.protolayout.material3.materialScope
import androidx.wear.protolayout.modifiers.clickable
import dev.sadakat.qit.core.domain.model.AyahRef
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** The tile's layout for each state: which words it shows and where its edge button goes. */
@RunWith(AndroidJUnit4::class)
@Config(application = Application::class)
class QuranTileLayoutTest {

    private val labels = TileLabels(
        appName = "QIt",
        nothingYet = "Choose a surah to start listening",
        open = "Open",
        continueListening = "Continue",
        pause = "Pause",
        resume = "Resume",
        position = { surah, ayah -> "$surah:$ayah" },
        progress = { ayah, count -> "Ayah $ayah of $count" },
    )
    private val clicks = TileClicks(
        open = clickable(id = "open"),
        nowPlaying = clickable(id = "now_playing"),
        resume = clickable(id = "resume"),
        pause = clickable(id = "pause"),
    )

    private fun render(state: TileState): String = layout(state).toString()

    private fun layout(state: TileState): LayoutElement {
        val device = DeviceParametersBuilders.DeviceParameters.Builder()
            .setScreenWidthDp(WIDTH_DP)
            .setScreenHeightDp(WIDTH_DP)
            .setScreenDensity(2f)
            .setScreenShape(DeviceParametersBuilders.SCREEN_SHAPE_ROUND)
            .setDevicePlatform(DeviceParametersBuilders.DEVICE_PLATFORM_WEAR_OS)
            .setRendererSchemaVersion(VersionInfo.Builder().setMajor(1).setMinor(SCHEMA_MINOR).build())
            .build()
        return materialScope(ApplicationProvider.getApplicationContext(), device, allowDynamicTheme = false) {
            quranTileLayout(state, labels, clicks)
        }
    }

    private fun String.clicks(id: Clickable) = contains("id=${id.id},")

    @Test
    fun `before anything played it invites to open the app`() {
        val tile = render(TileState.NothingYet)

        assertTrue(tile.contains("Choose a surah to start listening"))
        assertTrue(tile.contains("Open"))
        assertTrue(tile.clicks(clicks.open))
    }

    @Test
    fun `a saved position offers to continue through the app`() {
        val tile = render(TileState.Continue("Al-Kahf", AyahRef(18, 23), ayahCount = 110))

        assertTrue(tile.contains("Al-Kahf"))
        assertTrue(tile.contains("18:23"))
        assertTrue(tile.contains("Ayah 23 of 110"))
        assertTrue(tile.contains("Continue"))
        assertTrue(tile.clicks(clicks.resume))
        assertTrue(tile.clicks(clicks.nowPlaying))
    }

    @Test
    fun `while playing the edge button pauses in place`() {
        val tile = render(TileState.Queued("Al-Kahf", AyahRef(18, 23), ayahCount = 110, isPlaying = true))

        assertTrue(tile.contains("Pause"))
        assertTrue(tile.clicks(clicks.pause))
        assertFalse(tile.clicks(clicks.resume))
    }

    @Test
    fun `while paused the edge button resumes through the app`() {
        val tile = render(TileState.Queued("Al-Kahf", AyahRef(18, 23), ayahCount = 110, isPlaying = false))

        assertTrue(tile.contains("Resume"))
        assertTrue(tile.clicks(clicks.resume))
        assertFalse(tile.clicks(clicks.pause))
    }

    private companion object {
        const val WIDTH_DP = 225
        const val SCHEMA_MINOR = 500
    }
}
