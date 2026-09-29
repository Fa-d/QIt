package dev.sadakat.qandeel.core.ui.kit.glass

import android.os.Build
import androidx.compose.ui.unit.dp
import dev.sadakat.qandeel.core.designsystem.scale.QandeelSurfaces
import org.junit.Assert.assertEquals
import org.junit.Test

class SurfaceModeTest {

    private val glass = QandeelSurfaces(chromeAlpha = 0.72f, chromeFallbackAlpha = 0.9f, blurRadius = 24.dp)
    private val modern = SurfaceSignals(sdkInt = Build.VERSION_CODES.BAKLAVA)

    @Test
    fun `an opaque look is always opaque`() {
        assertEquals(QandeelSurfaceMode.OPAQUE, resolveSurfaceMode(QandeelSurfaces(), modern))
    }

    @Test
    fun `glass is frosted where blur works`() {
        assertEquals(QandeelSurfaceMode.FROSTED, resolveSurfaceMode(glass, modern))
        assertEquals(QandeelSurfaceMode.FROSTED, resolveSurfaceMode(glass, SurfaceSignals(Build.VERSION_CODES.S_V2)))
    }

    @Test
    fun `without blur glass is a stronger tint`() {
        assertEquals(QandeelSurfaceMode.TINTED, resolveSurfaceMode(glass, SurfaceSignals(Build.VERSION_CODES.R)))
        // Android 12.0 invalidates blurred layers unreliably.
        assertEquals(QandeelSurfaceMode.TINTED, resolveSurfaceMode(glass, SurfaceSignals(Build.VERSION_CODES.S)))
        assertEquals(QandeelSurfaceMode.TINTED, resolveSurfaceMode(glass.copy(blurRadius = 0.dp), modern))
    }

    @Test
    fun `battery saver and more contrast make glass solid`() {
        assertEquals(QandeelSurfaceMode.OPAQUE, resolveSurfaceMode(glass, modern.copy(powerSave = true)))
        assertEquals(QandeelSurfaceMode.OPAQUE, resolveSurfaceMode(glass, modern.copy(highContrastText = true)))
        assertEquals(QandeelSurfaceMode.OPAQUE, resolveSurfaceMode(glass, modern.copy(contrastLevel = 0.5f)))
        // Even where blur isn't supported: solid beats tinted.
        assertEquals(
            QandeelSurfaceMode.OPAQUE,
            resolveSurfaceMode(glass, SurfaceSignals(Build.VERSION_CODES.R, powerSave = true)),
        )
    }
}
