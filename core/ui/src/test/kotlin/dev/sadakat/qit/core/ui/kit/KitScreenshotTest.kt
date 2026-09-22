package dev.sadakat.qit.core.ui.kit

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.RoborazziOptions
import com.github.takahirom.roborazzi.captureRoboImage
import dev.sadakat.qit.core.designsystem.scale.QItSurfaces
import dev.sadakat.qit.core.designsystem.skin.QItSkin
import dev.sadakat.qit.core.designsystem.skin.QItSkins
import dev.sadakat.qit.core.designsystem.skin.QItStyle
import dev.sadakat.qit.core.designsystem.skin.QItTone
import dev.sadakat.qit.core.ui.kit.glass.QItSurfaceMode
import dev.sadakat.qit.core.ui.theme.QItMaterialTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w393dp-h851dp-xxhdpi")
class KitScreenshotTest {

    @get:Rule
    val rule = createComposeRule()

    @Test
    fun mushaf() = snapshot("kit_mushaf_light", QItSkins.of(QItStyle.MUSHAF, QItTone.LIGHT))

    @Test
    fun glassFrosted() = snapshot("kit_glass_frosted", glass, QItSurfaceMode.FROSTED)

    @Test
    fun glassTinted() = snapshot("kit_glass_tinted", glass, QItSurfaceMode.TINTED)

    private fun snapshot(name: String, skin: QItSkin, mode: QItSurfaceMode? = null) {
        rule.setContent { QItMaterialTheme(skin = skin, surfaceMode = mode) { KitSpecimen() } }
        rule.onRoot().captureRoboImage("src/test/screenshots/$name.png", roborazziOptions = Options)
    }

    private companion object {
        val glass = QItSkins.of(QItStyle.GLASS, QItTone.LIGHT).copy(
            surfaces = QItSurfaces(
                chromeAlpha = 0.72f,
                chromeFallbackAlpha = 0.9f,
                sheetAlpha = 0.9f,
                cardAlpha = 0.85f,
                blurRadius = 24.dp,
                noise = 0.05f,
                hairline = 1.dp,
                hairlineAlpha = 0.35f,
                floatingInset = 12.dp,
                backdropWash = 0.35f,
            ),
        )
        val Options = RoborazziOptions(compareOptions = RoborazziOptions.CompareOptions(changeThreshold = 0.01f))
    }
}
