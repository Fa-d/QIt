package dev.sadakat.qit.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import dev.sadakat.qit.core.designsystem.QItTheme
import dev.sadakat.qit.core.designsystem.shape.shape

/**
 * A surah or ayah number in the look's badge: the rub el hizb in the mushaf look, the way printed
 * mushafs mark their verses, drawn as an ornament line or as a filled tonal shape.
 *
 * @param label what a screen reader says for it ("Ayah 5"); the bare number when null.
 */
@Composable
fun NumberBadge(
    number: Int,
    modifier: Modifier = Modifier,
    size: Dp = QItTheme.sizes.numberBadge,
    label: String? = null,
) {
    val badge = QItTheme.badge
    val shape = badge.shape.shape
    val colors = QItTheme.colors
    Box(
        modifier = modifier
            .size(size)
            .then(
                if (badge.filled) {
                    Modifier.background(colors.secondaryContainer, shape)
                } else {
                    Modifier.border(QItTheme.sizes.ornamentStroke, colors.ornament, shape)
                },
            )
            .then(if (label != null) Modifier.clearAndSetSemantics { contentDescription = label } else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = number.toString(),
            style = MaterialTheme.typography.labelMedium.copy(
                fontSize = numberSize(size, digits = number.toString().length),
                letterSpacing = TextUnit.Unspecified,
            ),
            color = if (badge.filled) colors.onSecondaryContainer else MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
        )
    }
}

/**
 * The number is sized to the star, not to the user's font scale: the star is a fixed ornament and the
 * number must stay inside its notches. Three digits get a smaller share of the width.
 */
@Composable
private fun numberSize(badge: Dp, digits: Int): TextUnit =
    with(LocalDensity.current) { (badge * if (digits >= THREE) SHARE_THREE_DIGITS else SHARE).toSp() }

private const val THREE = 3
private const val SHARE = 0.34f
private const val SHARE_THREE_DIGITS = 0.27f
