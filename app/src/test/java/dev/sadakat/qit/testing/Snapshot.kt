package dev.sadakat.qit.testing

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
import dev.sadakat.qit.ui.theme.QItAppTheme

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
    darkTheme: Boolean = false,
    arabicScale: Float = 1f,
    content: @Composable () -> Unit,
) {
    setContent {
        QItAppTheme(darkTheme = darkTheme, arabicScale = arabicScale) {
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
