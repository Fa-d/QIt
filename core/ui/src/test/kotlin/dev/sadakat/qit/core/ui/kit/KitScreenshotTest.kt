package dev.sadakat.qit.core.ui.kit

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.RoborazziOptions
import com.github.takahirom.roborazzi.captureRoboImage
import dev.sadakat.qit.core.designsystem.skin.QItSkins
import dev.sadakat.qit.core.designsystem.skin.QItStyle
import dev.sadakat.qit.core.designsystem.skin.QItTone
import dev.sadakat.qit.core.ui.kit.glass.QItSurfaceMode
import dev.sadakat.qit.core.ui.theme.QItMaterialTheme
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
class KitScreenshotTest(private val style: QItStyle, private val tone: QItTone, private val mode: QItSurfaceMode?) {

    @get:Rule
    val rule = createComposeRule()

    // Built inside the sandbox: Roborazzi reads Robolectric's configuration when it's created.
    private val options = RoborazziOptions(compareOptions = RoborazziOptions.CompareOptions(changeThreshold = 0.01f))

    @Test
    fun specimen() {
        val skin = QItSkins.of(style, tone)
        rule.setContent {
            QItMaterialTheme(skin = skin, surfaceMode = mode ?: defaultMode(skin.surfaces.isTranslucent)) {
                KitSpecimen()
            }
        }
        val suffix = mode?.let { "_${it.name.lowercase()}" }.orEmpty()
        val name = "kit_${style.name.lowercase()}_${tone.name.lowercase()}$suffix"
        rule.onRoot().captureRoboImage("src/test/screenshots/$name.png", roborazziOptions = options)
    }

    private fun defaultMode(translucent: Boolean) = if (translucent) QItSurfaceMode.FROSTED else QItSurfaceMode.OPAQUE

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0} {1} {2}")
        fun looks(): List<Array<Any?>> = QItStyle.entries.flatMap { style ->
            QItTone.entries.map { tone -> arrayOf<Any?>(style, tone, null) }
        } + listOf(
            arrayOf(QItStyle.GLASS, QItTone.LIGHT, QItSurfaceMode.TINTED),
            arrayOf(QItStyle.GLASS, QItTone.LIGHT, QItSurfaceMode.OPAQUE),
        )
    }
}
