package dev.sadakat.qit.presentation.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import dev.sadakat.qit.core.designsystem.QItTheme
import dev.sadakat.qit.core.designsystem.shape.OctagramShape

/** A surah or ayah number inside the rub el hizb, the way printed mushafs mark their verses. */
@Composable
fun NumberBadge(number: Int, modifier: Modifier = Modifier, size: Dp = QItTheme.sizes.numberBadge) {
    Box(
        modifier = modifier
            .size(size)
            .border(QItTheme.sizes.ornamentStroke, QItTheme.colors.ornament, OctagramShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = number.toString(),
            style = MaterialTheme.typography.labelMedium.copy(
                fontSize = numberSize(size, digits = number.toString().length),
                letterSpacing = TextUnit.Unspecified,
            ),
            color = MaterialTheme.colorScheme.onSurface,
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
