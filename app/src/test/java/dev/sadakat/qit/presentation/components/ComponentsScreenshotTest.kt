package dev.sadakat.qit.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import dev.sadakat.qit.core.designsystem.QItTheme
import dev.sadakat.qit.core.domain.repository.SurahDownloadState
import dev.sadakat.qit.testing.snapshot
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel5)
class ComponentsScreenshotTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun light() = composeRule.snapshot("components_light") { Components() }

    @Test
    fun dark() = composeRule.snapshot("components_dark", darkTheme = true) { Components() }
}

@Composable
private fun Components() {
    Row(
        modifier = Modifier.padding(QItTheme.spacing.lg),
        horizontalArrangement = Arrangement.spacedBy(QItTheme.spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        NumberBadge(1)
        NumberBadge(18)
        NumberBadge(255)
        NumberBadge(114, size = QItTheme.sizes.numberBadgeSmall)
        DownloadIndicator(SurahDownloadState.Downloading(completedFiles = 120, totalFiles = 287))
        DownloadIndicator(SurahDownloadState.Downloaded)
        DownloadIndicator(SurahDownloadState.Failed(completedFiles = 284, totalFiles = 287))
    }
}
