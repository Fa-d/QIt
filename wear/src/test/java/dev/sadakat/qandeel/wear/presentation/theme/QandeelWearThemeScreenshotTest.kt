package dev.sadakat.qandeel.wear.presentation.theme

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.text.style.TextAlign
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import dev.sadakat.qandeel.core.designsystem.QandeelTheme
import dev.sadakat.qandeel.wear.testing.wearSnapshot
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** The watch theme on a round screen: brand roles on black, Arabic at the watch size. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.WearOSLargeRound)
class QandeelWearThemeScreenshotTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun theme() = composeRule.wearSnapshot("wear_theme_large_round") { Specimen() }
}

@Composable
private fun Specimen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(QandeelTheme.spacing.xl),
        verticalArrangement = Arrangement.spacedBy(QandeelTheme.spacing.sm, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Al-Kahf 18:10", style = MaterialTheme.typography.titleMedium)
        Text(
            text = "رَبَّنَآ ءَاتِنَا مِن لَّدُنكَ رَحْمَةً",
            style = QandeelTheme.arabic.watchBody,
            color = QandeelTheme.colors.arabicText,
            textAlign = TextAlign.Center,
        )
        Button(onClick = {}) { Text("Continue") }
    }
}
