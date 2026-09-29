package dev.sadakat.qandeel.core.ui.kit

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import dev.sadakat.qandeel.core.ui.kit.glass.LocalQandeelBackdrop
import dev.sadakat.qandeel.core.ui.kit.glass.QandeelGlassEdge
import dev.sadakat.qandeel.core.ui.kit.glass.QandeelSurfaceRole
import dev.sadakat.qandeel.core.ui.kit.glass.glassLook
import dev.sadakat.qandeel.core.ui.kit.glass.qitGlass

/** A small bar with the title in line, or a large one whose title collapses into it. */
enum class QandeelTopBarSize { SMALL, LARGE }

/** How a top bar reacts to its content scrolling. */
enum class QandeelTopBarScrollKind {
    /** Stays put. */
    PINNED,

    /** Slides away while reading down, back on any scroll up. */
    HIDE_ON_SCROLL,

    /** A large title collapses into a small bar, and expands again at the top. */
    COLLAPSE_ON_SCROLL,
}

/** The link between a top bar and the content it scrolls with. */
@Stable
class QandeelTopBarScroll
@OptIn(ExperimentalMaterial3Api::class)
internal constructor(
    internal val behavior: TopAppBarScrollBehavior,
) {
    /** Give this to the scrolling content (`Modifier.nestedScroll`). */
    @OptIn(ExperimentalMaterial3Api::class)
    val nestedScrollConnection: NestedScrollConnection get() = behavior.nestedScrollConnection
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun rememberQandeelTopBarScroll(kind: QandeelTopBarScrollKind): QandeelTopBarScroll {
    val behavior = when (kind) {
        QandeelTopBarScrollKind.PINNED -> TopAppBarDefaults.pinnedScrollBehavior()
        QandeelTopBarScrollKind.HIDE_ON_SCROLL -> TopAppBarDefaults.enterAlwaysScrollBehavior()
        QandeelTopBarScrollKind.COLLAPSE_ON_SCROLL -> TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    }
    return remember(behavior) { QandeelTopBarScroll(behavior) }
}

/**
 * The screen's app bar. It runs under the status bar, and in a glass look it frosts the content
 * scrolling behind it.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QandeelTopBar(
    title: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    size: QandeelTopBarSize = QandeelTopBarSize.SMALL,
    scroll: QandeelTopBarScroll? = null,
) {
    val glass = glassLook(MaterialTheme.colorScheme.surfaceContainer, QandeelSurfaceRole.CHROME)
    val colors = if (glass == null) {
        TopAppBarDefaults.topAppBarColors()
    } else {
        TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Transparent,
            scrolledContainerColor = Color.Transparent,
        )
    }
    val barModifier = if (glass == null) {
        modifier
    } else {
        modifier.qitGlass(glass, RectangleShape, LocalQandeelBackdrop.current, QandeelGlassEdge.BOTTOM)
    }
    when (size) {
        QandeelTopBarSize.SMALL -> TopAppBar(
            title = title,
            modifier = barModifier,
            navigationIcon = navigationIcon,
            actions = actions,
            colors = colors,
            scrollBehavior = scroll?.behavior,
        )

        QandeelTopBarSize.LARGE -> LargeTopAppBar(
            title = title,
            modifier = barModifier,
            navigationIcon = navigationIcon,
            actions = actions,
            colors = colors,
            scrollBehavior = scroll?.behavior,
        )
    }
}
