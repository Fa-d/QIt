package dev.sadakat.qit.core.ui.kit.glass

import android.os.Build
import androidx.compose.ui.unit.dp
import dev.sadakat.qit.core.designsystem.scale.QItSurfaces
import org.junit.Assert.assertEquals
import org.junit.Test

class SurfaceModeTest {

    private val glass = QItSurfaces(chromeAlpha = 0.72f, chromeFallbackAlpha = 0.9f, blurRadius = 24.dp)
    private val modern = SurfaceSignals(sdkInt = Build.VERSION_CODES.BAKLAVA)

    @Test
    fun `an opaque look is always opaque`() {
        assertEquals(QItSurfaceMode.OPAQUE, resolveSurfaceMode(QItSurfaces(), modern))
    }

    @Test
    fun `glass is frosted where blur works`() {
        assertEquals(QItSurfaceMode.FROSTED, resolveSurfaceMode(glass, modern))
        assertEquals(QItSurfaceMode.FROSTED, resolveSurfaceMode(glass, SurfaceSignals(Build.VERSION_CODES.S_V2)))
    }

    @Test
    fun `without blur glass is a stronger tint`() {
        assertEquals(QItSurfaceMode.TINTED, resolveSurfaceMode(glass, SurfaceSignals(Build.VERSION_CODES.R)))
        // Android 12.0 invalidates blurred layers unreliably.
        assertEquals(QItSurfaceMode.TINTED, resolveSurfaceMode(glass, SurfaceSignals(Build.VERSION_CODES.S)))
        assertEquals(QItSurfaceMode.TINTED, resolveSurfaceMode(glass.copy(blurRadius = 0.dp), modern))
    }

    @Test
    fun `battery saver and more contrast make glass solid`() {
        assertEquals(QItSurfaceMode.OPAQUE, resolveSurfaceMode(glass, modern.copy(powerSave = true)))
        assertEquals(QItSurfaceMode.OPAQUE, resolveSurfaceMode(glass, modern.copy(highContrastText = true)))
        assertEquals(QItSurfaceMode.OPAQUE, resolveSurfaceMode(glass, modern.copy(contrastLevel = 0.5f)))
        // Even where blur isn't supported: solid beats tinted.
        assertEquals(
            QItSurfaceMode.OPAQUE,
            resolveSurfaceMode(glass, SurfaceSignals(Build.VERSION_CODES.R, powerSave = true)),
        )
    }
}
