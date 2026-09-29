package dev.sadakat.qandeel.testing

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.RoborazziATFAccessibilityCheckOptions
import com.github.takahirom.roborazzi.RoborazziATFAccessibilityChecker
import com.github.takahirom.roborazzi.RoborazziOptions
import com.github.takahirom.roborazzi.captureRoboImage
import com.github.takahirom.roborazzi.checkRoboAccessibility
import dev.sadakat.qandeel.core.designsystem.skin.QandeelStyle
import dev.sadakat.qandeel.core.designsystem.skin.QandeelTone
import dev.sadakat.qandeel.core.ui.kit.glass.QandeelSurfaceMode
import dev.sadakat.qandeel.ui.theme.QandeelAppTheme

/**
 * Renders [content] in the app theme, compares it with the golden `src/test/screenshots/<name>.png`
 * and runs the accessibility checks (touch targets, contrast, labels) on it. Record goldens with
 * `./gradlew :app:recordRoborazziDebug`.
 *
 * Test classes need `@GraphicsMode(GraphicsMode.Mode.NATIVE)` and a device, e.g.
 * `@Config(qualifiers = RobolectricDeviceQualifiers.Pixel5)`.
 */
fun ComposeContentTestRule.snapshot(
    name: String,
    arabicScale: Float = 1f,
    style: QandeelStyle = QandeelStyle.MUSHAF,
    tone: QandeelTone = QandeelTone.LIGHT,
    content: @Composable () -> Unit,
) {
    setContent {
        // Glass is pinned to its unblurred tint: goldens stay the same on every machine (the kit's
        // own goldens cover real blur).
        QandeelAppTheme(
            arabicScale = arabicScale,
            style = style,
            tone = tone,
            surfaceMode = QandeelSurfaceMode.TINTED,
        ) {
            Surface(color = MaterialTheme.colorScheme.background, content = content)
        }
    }
    onRoot().captureRoboImage("src/test/screenshots/$name.png", roborazziOptions = SnapshotOptions)
    onRoot().checkRoboAccessibility(
        RoborazziATFAccessibilityCheckOptions(failureLevel = RoborazziATFAccessibilityChecker.CheckLevel.Error),
    )
}

/** Tolerates sub-percent anti-aliasing differences between machines. */
private val SnapshotOptions = RoborazziOptions(
    compareOptions = RoborazziOptions.CompareOptions(changeThreshold = 0.01f),
)
