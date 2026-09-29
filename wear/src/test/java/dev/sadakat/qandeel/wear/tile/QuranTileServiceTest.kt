package dev.sadakat.qandeel.wear.tile

import android.os.Looper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.wear.protolayout.DeviceParametersBuilders
import androidx.wear.protolayout.expression.VersionBuilders.VersionInfo
import androidx.wear.tiles.RequestBuilders.TileRequest
import androidx.wear.tiles.testing.TestTileClient
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.util.concurrent.TimeUnit

/**
 * The real, Hilt-injected tile service: it's created (found on a Galaxy Watch: a constructor quirk
 * crashed its injection) and answers a tile request with the "nothing yet" layout on a fresh install.
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
@Config(application = HiltTestApplication::class)
class QuranTileServiceTest {

    @get:Rule
    val hilt = HiltAndroidRule(this)

    @Test
    fun `the service is created and answers a tile request`() {
        // The client creates and binds the service, which runs Hilt's injection in onCreate.
        val client = TestTileClient(QuranTileService()) { it.run() }

        val request = TileRequest.Builder().setDeviceConfiguration(device()).build()
        val future = client.requestTile(request)
        // The tile is built on the main thread; let it run until the future completes.
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(TIMEOUT_S)
        while (!future.isDone && System.nanoTime() < deadline) {
            shadowOf(Looper.getMainLooper()).idle()
            Thread.sleep(POLL_MS)
        }

        val layout = future.get().tileTimeline.toString()
        assertTrue(layout, layout.contains("Choose a surah to start listening"))
    }

    private fun device() = DeviceParametersBuilders.DeviceParameters.Builder()
        .setScreenWidthDp(WIDTH_DP)
        .setScreenHeightDp(WIDTH_DP)
        .setScreenDensity(2f)
        .setScreenShape(DeviceParametersBuilders.SCREEN_SHAPE_ROUND)
        .setDevicePlatform(DeviceParametersBuilders.DEVICE_PLATFORM_WEAR_OS)
        .setRendererSchemaVersion(VersionInfo.Builder().setMajor(1).setMinor(SCHEMA_MINOR).build())
        .build()

    private companion object {
        const val WIDTH_DP = 225
        const val SCHEMA_MINOR = 500
        const val TIMEOUT_S = 10L
        const val POLL_MS = 10L
    }
}
