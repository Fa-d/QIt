package dev.sadakat.qandeel.core.ui.kit

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.RoborazziOptions
import com.github.takahirom.roborazzi.captureRoboImage
import dev.sadakat.qandeel.core.designsystem.skin.QandeelSkins
import dev.sadakat.qandeel.core.designsystem.skin.QandeelStyle
import dev.sadakat.qandeel.core.designsystem.skin.QandeelTone
import dev.sadakat.qandeel.core.ui.kit.glass.QandeelSurfaceMode
import dev.sadakat.qandeel.core.ui.theme.QandeelMaterialTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The kit specimen in every look: each style on each tone, and glass also as it falls back (tinted
 * where blur is unavailable, solid under battery saver or more contrast). Rendered through the
 * hardware renderer (see this module's build file), so frosted glass is really blurred.
 */
@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w393dp-h851dp-xxhdpi")
class KitScreenshotTest(
    private val style: QandeelStyle,
    private val tone: QandeelTone,
    private val mode: QandeelSurfaceMode?,
) {

    @get:Rule
    val rule = createComposeRule()

    // Built inside the sandbox: Roborazzi reads Robolectric's configuration when it's created.
    private val options = RoborazziOptions(compareOptions = RoborazziOptions.CompareOptions(changeThreshold = 0.01f))

    @Test
    fun specimen() {
        val skin = QandeelSkins.of(style, tone)
        rule.setContent {
            QandeelMaterialTheme(skin = skin, surfaceMode = mode ?: defaultMode(skin.surfaces.isTranslucent)) {
                KitSpecimen()
            }
        }
        val suffix = mode?.let { "_${it.name.lowercase()}" }.orEmpty()
        val name = "kit_${style.name.lowercase()}_${tone.name.lowercase()}$suffix"
        rule.onRoot().captureRoboImage("src/test/screenshots/$name.png", roborazziOptions = options)
    }

    private fun defaultMode(translucent: Boolean) =
        if (translucent) QandeelSurfaceMode.FROSTED else QandeelSurfaceMode.OPAQUE

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0} {1} {2}")
        fun looks(): List<Array<Any?>> = QandeelStyle.entries.flatMap { style ->
            QandeelTone.entries.map { tone -> arrayOf<Any?>(style, tone, null) }
        } + listOf(
            arrayOf(QandeelStyle.GLASS, QandeelTone.LIGHT, QandeelSurfaceMode.TINTED),
            arrayOf(QandeelStyle.GLASS, QandeelTone.LIGHT, QandeelSurfaceMode.OPAQUE),
        )
    }
}
