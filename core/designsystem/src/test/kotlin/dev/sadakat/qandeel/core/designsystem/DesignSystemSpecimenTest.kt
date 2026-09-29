package dev.sadakat.qandeel.core.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import dev.sadakat.qandeel.core.designsystem.color.QandeelColors
import dev.sadakat.qandeel.core.designsystem.color.watchQandeelColors
import dev.sadakat.qandeel.core.designsystem.scale.QandeelRadius
import dev.sadakat.qandeel.core.designsystem.shape.shape
import dev.sadakat.qandeel.core.designsystem.skin.QandeelSkin
import dev.sadakat.qandeel.core.designsystem.skin.QandeelSkins
import dev.sadakat.qandeel.core.designsystem.skin.QandeelStyle
import dev.sadakat.qandeel.core.designsystem.skin.QandeelTone
import dev.sadakat.qandeel.core.designsystem.type.QandeelUiType
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * A visual specimen of every skin: the roles of its scheme, its badge, type and radius, and the
 * page's Arabic. The lead reviews the images; ContrastTest holds the numbers.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w400dp-h900dp-xhdpi")
class DesignSystemSpecimenTest {

    @Test
    fun skins() {
        QandeelStyle.entries.forEach { style ->
            QandeelTone.entries.forEach { tone ->
                val skin = QandeelSkins.of(style, tone)
                // The mushaf light and dark specimens keep their original file names.
                val name = if (style == QandeelStyle.MUSHAF && tone != QandeelTone.SEPIA) {
                    tone.name.lowercase()
                } else {
                    "${style.name.lowercase()}_${tone.name.lowercase()}"
                }
                capture(name, skin)
            }
        }
    }

    @Test
    fun watch() = captureColors("watch", watchQandeelColors())

    private fun capture(name: String, skin: QandeelSkin) {
        captureRoboImage("src/test/screenshots/designsystem_specimen_$name.png") {
            ProvideQandeelTokens(skin) { Specimen() }
        }
    }

    private fun captureColors(name: String, colors: QandeelColors) {
        captureRoboImage("src/test/screenshots/designsystem_specimen_$name.png") {
            ProvideQandeelTokens(colors) { Specimen() }
        }
    }
}

@Composable
private fun Specimen() {
    val colors = QandeelTheme.colors
    val surfaces = QandeelTheme.surfaces
    val spacing = QandeelTheme.spacing
    val radius = QandeelTheme.radius
    val ui = QandeelTheme.type
    Column(
        modifier = Modifier
            .background(colors.background)
            .padding(spacing.lg),
        verticalArrangement = Arrangement.spacedBy(spacing.md),
    ) {
        ChromeBar(ui, radius)
        BasicText("Al-Baqarah", style = ui.headlineMedium.copy(color = colors.onSurface))
        BasicText("The Cow · 286 ayahs · Medinan", style = ui.bodyMedium.copy(color = colors.onSurfaceVariant))
        Row(horizontalArrangement = Arrangement.spacedBy(spacing.sm), verticalAlignment = Alignment.CenterVertically) {
            NumberBadge(ui)
            Swatch("primary", colors.primary, colors.onPrimary, ui.labelMedium, radius)
            Swatch("tertiary", colors.tertiary, colors.onTertiary, ui.labelMedium, radius)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
            Swatch("container", colors.primaryContainer, colors.onPrimaryContainer, ui.labelMedium, radius)
            Swatch("secondary", colors.secondaryContainer, colors.onSecondaryContainer, ui.labelMedium, radius)
            Swatch("gold", colors.tertiaryContainer, colors.onTertiaryContainer, ui.labelMedium, radius)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
            Swatch("low", colors.surfaceContainerLow, colors.onSurface, ui.labelMedium, radius)
            Swatch("mid", colors.surfaceContainer, colors.onSurface, ui.labelMedium, radius)
            Swatch("high", colors.surfaceContainerHigh, colors.onSurface, ui.labelMedium, radius)
        }
        Column(
            modifier = Modifier
                .background(colors.playingAyahHighlight, RoundedCornerShape(radius.lg))
                .padding(spacing.md),
        ) {
            BasicText(
                "ٱللَّهُ لَآ إِلَـٰهَ إِلَّا هُوَ ٱلْحَىُّ ٱلْقَيُّومُ",
                style = QandeelTheme.arabic.body.copy(color = colors.onPlayingAyahHighlight),
            )
            BasicText(
                "Allah — there is no deity except Him, the Ever-Living, the Sustainer of existence.",
                style = ui.bodyMedium.copy(color = colors.onPlayingAyahHighlight),
            )
        }
        BasicText(
            "بِسْمِ ٱللَّهِ ٱلرَّحْمَـٰنِ ٱلرَّحِيمِ",
            style = QandeelTheme.arabic.title.copy(color = colors.arabicText),
        )
        BasicText("مَـٰلِكِ يَوْمِ ٱلدِّينِ", style = QandeelTheme.arabic.label.copy(color = colors.arabicText))
    }
}

/** A bar of chrome as the look makes it: frosted over the page when the look is glass. */
@Composable
private fun ChromeBar(ui: QandeelUiType, radius: QandeelRadius) {
    val colors = QandeelTheme.colors
    val surfaces = QandeelTheme.surfaces
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                colors.surfaceContainer.compositeOver(colors.background, surfaces.chromeAlpha),
                RoundedCornerShape(radius.xl),
            )
            .border(
                surfaces.hairline,
                colors.outline.copy(alpha = surfaces.hairlineAlpha),
                RoundedCornerShape(radius.xl),
            )
            .padding(QandeelTheme.spacing.sm),
    ) {
        BasicText("Qandeel · Al-Baqarah", style = ui.titleMedium.copy(color = colors.onSurface))
    }
}

/** The look's number badge: an ornament outline, or a filled tonal container. */
@Composable
private fun NumberBadge(ui: QandeelUiType) {
    val colors = QandeelTheme.colors
    val badge = QandeelTheme.badge
    Box(
        modifier = Modifier
            .size(QandeelTheme.sizes.numberBadge)
            .then(
                if (badge.filled) {
                    Modifier.background(colors.primaryContainer, badge.shape.shape)
                } else {
                    Modifier.border(QandeelTheme.sizes.ornamentStroke, colors.ornament, badge.shape.shape)
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        BasicText(
            "255",
            style = ui.labelMedium.copy(color = if (badge.filled) colors.onPrimaryContainer else colors.onSurface),
        )
    }
}

@Composable
private fun Swatch(label: String, container: Color, content: Color, style: TextStyle, radius: QandeelRadius) {
    Box(
        modifier = Modifier
            .width(96.dp)
            .background(container, RoundedCornerShape(radius.md))
            .padding(QandeelTheme.spacing.sm),
    ) {
        BasicText(label, style = style.copy(color = content))
    }
}

/** [surface] as a translucent layer at [alpha] over [backdrop], blended per sRGB channel. */
private fun Color.compositeOver(backdrop: Color, alpha: Float): Color = Color(
    red = red * alpha + backdrop.red * (1 - alpha),
    green = green * alpha + backdrop.green * (1 - alpha),
    blue = blue * alpha + backdrop.blue * (1 - alpha),
)
