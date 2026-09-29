package dev.sadakat.qandeel.core.ui.kit

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.sadakat.qandeel.core.designsystem.QandeelTheme
import dev.sadakat.qandeel.core.ui.kit.glass.LocalQandeelBackdrop
import dev.sadakat.qandeel.core.ui.kit.glass.QandeelGlassEdge
import dev.sadakat.qandeel.core.ui.kit.glass.QandeelSurfaceRole
import dev.sadakat.qandeel.core.ui.kit.glass.glassLook
import dev.sadakat.qandeel.core.ui.kit.glass.qitGlass

/**
 * A surface of [color] in the current look: solid, or, for a [role] a glass look makes translucent,
 * a frosted tint (no shadow then: it would show through).
 */
@Composable
fun QandeelSurface(
    modifier: Modifier = Modifier,
    role: QandeelSurfaceRole = QandeelSurfaceRole.PAGE,
    shape: Shape = RectangleShape,
    color: Color = MaterialTheme.colorScheme.surface,
    contentColor: Color = contentColorFor(color),
    shadowElevation: Dp = 0.dp,
    edge: QandeelGlassEdge = QandeelGlassEdge.ALL,
    content: @Composable () -> Unit,
) {
    val glass = glassLook(color, role)
    if (glass == null) {
        Surface(
            modifier = modifier,
            shape = shape,
            color = color,
            contentColor = contentColor,
            shadowElevation = shadowElevation,
            content = content,
        )
    } else {
        Surface(
            modifier = modifier.qitGlass(glass, shape, LocalQandeelBackdrop.current, edge),
            shape = shape,
            color = Color.Transparent,
            contentColor = contentColor,
            content = content,
        )
    }
}

/** How much a card stands out: the page's own tone, or the primary or tertiary container. */
enum class QandeelEmphasis { NEUTRAL, PRIMARY, TERTIARY }

/** A card on the page; clickable when [onClick] is given. */
@Composable
fun QandeelCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    emphasis: QandeelEmphasis = QandeelEmphasis.NEUTRAL,
    shape: Shape = MaterialTheme.shapes.extraLarge,
    content: @Composable ColumnScope.() -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val (container, onContainer) = when (emphasis) {
        QandeelEmphasis.NEUTRAL -> scheme.surfaceContainerHigh to scheme.onSurface
        QandeelEmphasis.PRIMARY -> scheme.primaryContainer to scheme.onPrimaryContainer
        QandeelEmphasis.TERTIARY -> scheme.tertiaryContainer to scheme.onTertiaryContainer
    }
    val glass = glassLook(container, QandeelSurfaceRole.CARD)
    val colors = CardDefaults.cardColors(
        containerColor = if (glass == null) container else Color.Transparent,
        contentColor = onContainer,
    )
    val cardModifier = if (glass == null) modifier else modifier.qitGlass(glass, shape, backdrop = null)
    if (onClick == null) {
        Card(modifier = cardModifier, shape = shape, colors = colors, content = content)
    } else {
        Card(onClick = onClick, modifier = cardModifier, shape = shape, colors = colors, content = content)
    }
}

/**
 * The bar at the bottom of the app (the mini player): docked along the bottom edge, or, in looks
 * with a floating inset, a pill floating above it. Either way its content stays clear of the
 * navigation bar.
 */
@Composable
fun QandeelBottomBar(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val surfaces = QandeelTheme.surfaces
    val color = MaterialTheme.colorScheme.surfaceContainerHigh
    if (surfaces.floatingInset > 0.dp) {
        Box(
            modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = surfaces.floatingInset, vertical = surfaces.floatingInset / 2),
        ) {
            QandeelSurface(
                role = QandeelSurfaceRole.CHROME,
                shape = RoundedCornerShape(QandeelTheme.radius.xl),
                color = color,
                shadowElevation = surfaces.shadow,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(content = content)
            }
        }
    } else {
        QandeelSurface(
            role = QandeelSurfaceRole.CHROME,
            color = color,
            edge = QandeelGlassEdge.TOP,
            modifier = modifier.fillMaxWidth(),
        ) {
            // The surface runs behind the navigation bar (edge to edge); the content stays above it.
            Column(Modifier.navigationBarsPadding(), content = content)
        }
    }
}
