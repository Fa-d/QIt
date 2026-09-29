package dev.sadakat.qandeel.wear.testing

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.onRoot
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.TimeSource
import androidx.wear.compose.material3.TimeText
import com.github.takahirom.roborazzi.RoborazziATFAccessibilityCheckOptions
import com.github.takahirom.roborazzi.RoborazziATFAccessibilityChecker
import com.github.takahirom.roborazzi.RoborazziOptions
import com.github.takahirom.roborazzi.captureRoboImage
import com.github.takahirom.roborazzi.checkRoboAccessibility
import dev.sadakat.qandeel.wear.presentation.theme.QandeelWearTheme

/**
 * Renders [content] full-screen in the watch theme, compares it with the golden
 * `src/test/screenshots/<name>.png` and runs the accessibility checks on it. Record goldens with
 * `./gradlew :wear:recordRoborazziDebug`.
 *
 * Test classes need `@GraphicsMode(GraphicsMode.Mode.NATIVE)` and a round watch, e.g.
 * `@Config(qualifiers = RobolectricDeviceQualifiers.WearOSLargeRound)`.
 */
fun ComposeContentTestRule.wearSnapshot(name: String, content: @Composable () -> Unit) {
    setContent {
        QandeelWearTheme {
            // The time at the top is the clock's: fix it, or every golden would change by the minute.
            AppScaffold(timeText = { TimeText(timeSource = FixedTime) }) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background),
                ) { content() }
            }
        }
    }
    onRoot().captureRoboImage("src/test/screenshots/$name.png", roborazziOptions = SnapshotOptions)
    onRoot().checkRoboAccessibility(
        RoborazziATFAccessibilityCheckOptions(failureLevel = RoborazziATFAccessibilityChecker.CheckLevel.Error),
    )
}

/** The time the goldens show. */
private object FixedTime : TimeSource {
    @Composable
    override fun currentTime(): String = "10:09"
}

/** Tolerates sub-percent anti-aliasing differences between machines. */
private val SnapshotOptions = RoborazziOptions(
    compareOptions = RoborazziOptions.CompareOptions(changeThreshold = 0.01f),
)
