package dev.sadakat.qit.core.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import dev.sadakat.qit.core.designsystem.color.QItColors
import dev.sadakat.qit.core.designsystem.color.darkQItColors
import dev.sadakat.qit.core.designsystem.color.lightQItColors
import dev.sadakat.qit.core.designsystem.color.watchQItColors
import dev.sadakat.qit.core.designsystem.shape.OctagramShape
import dev.sadakat.qit.core.designsystem.type.QItUiType
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** A visual specimen of the tokens: the roles of each scheme, the octagram and the Arabic scale. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w400dp-h900dp-xhdpi")
class DesignSystemSpecimenTest {

    @Test
    fun light() = capture("light", lightQItColors())

    @Test
    fun dark() = capture("dark", darkQItColors())

    @Test
    fun watch() = capture("watch", watchQItColors())

    private fun capture(name: String, colors: QItColors) {
        captureRoboImage("src/test/screenshots/designsystem_specimen_$name.png") {
            ProvideQItTokens(colors) { Specimen() }
        }
    }
}

@Composable
private fun Specimen() {
    val colors = QItTheme.colors
    val spacing = QItTheme.spacing
    val ui = QItUiType()
    Column(
        modifier = Modifier
            .background(colors.background)
            .padding(spacing.lg),
        verticalArrangement = Arrangement.spacedBy(spacing.md),
    ) {
        BasicText("Al-Baqarah", style = ui.headlineMedium.copy(color = colors.onSurface))
        BasicText("The Cow · 286 ayahs · Medinan", style = ui.bodyMedium.copy(color = colors.onSurfaceVariant))
        Row(horizontalArrangement = Arrangement.spacedBy(spacing.sm), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(QItTheme.sizes.numberBadge)
                    .border(QItTheme.sizes.ornamentStroke, colors.ornament, OctagramShape),
                contentAlignment = Alignment.Center,
            ) {
                BasicText("255", style = ui.labelMedium.copy(color = colors.onSurface))
            }
            Swatch("primary", colors.primary, colors.onPrimary, ui.labelMedium)
            Swatch("tertiary", colors.tertiary, colors.onTertiary, ui.labelMedium)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
            Swatch("container", colors.primaryContainer, colors.onPrimaryContainer, ui.labelMedium)
            Swatch("secondary", colors.secondaryContainer, colors.onSecondaryContainer, ui.labelMedium)
            Swatch("gold", colors.tertiaryContainer, colors.onTertiaryContainer, ui.labelMedium)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
            Swatch("low", colors.surfaceContainerLow, colors.onSurface, ui.labelMedium)
            Swatch("mid", colors.surfaceContainer, colors.onSurface, ui.labelMedium)
            Swatch("high", colors.surfaceContainerHigh, colors.onSurface, ui.labelMedium)
        }
        Column(
            modifier = Modifier
                .background(colors.playingAyahHighlight)
                .padding(spacing.md),
        ) {
            BasicText(
                "ٱللَّهُ لَآ إِلَـٰهَ إِلَّا هُوَ ٱلْحَىُّ ٱلْقَيُّومُ",
                style = QItTheme.arabic.body.copy(color = colors.onPlayingAyahHighlight),
            )
            BasicText(
                "Allah — there is no deity except Him, the Ever-Living, the Sustainer of existence.",
                style = ui.bodyMedium.copy(color = colors.onPlayingAyahHighlight),
            )
        }
        BasicText(
            "بِسْمِ ٱللَّهِ ٱلرَّحْمَـٰنِ ٱلرَّحِيمِ",
            style = QItTheme.arabic.title.copy(color = colors.arabicText),
        )
        BasicText("مَـٰلِكِ يَوْمِ ٱلدِّينِ", style = QItTheme.arabic.label.copy(color = colors.arabicText))
    }
}

@Composable
private fun Swatch(label: String, container: Color, content: Color, style: TextStyle) {
    Box(
        modifier = Modifier
            .width(96.dp)
            .background(container)
            .padding(QItTheme.spacing.sm),
    ) {
        BasicText(label, style = style.copy(color = content))
    }
}
