package dev.sadakat.qit.core.ui.kit

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
import dev.sadakat.qit.core.ui.kit.glass.LocalQItBackdrop
import dev.sadakat.qit.core.ui.kit.glass.QItGlassEdge
import dev.sadakat.qit.core.ui.kit.glass.QItSurfaceRole
import dev.sadakat.qit.core.ui.kit.glass.glassLook
import dev.sadakat.qit.core.ui.kit.glass.qitGlass

/** A small bar with the title in line, or a large one whose title collapses into it. */
enum class QItTopBarSize { SMALL, LARGE }

/** How a top bar reacts to its content scrolling. */
enum class QItTopBarScrollKind {
    /** Stays put. */
    PINNED,

    /** Slides away while reading down, back on any scroll up. */
    HIDE_ON_SCROLL,

    /** A large title collapses into a small bar, and expands again at the top. */
    COLLAPSE_ON_SCROLL,
}

/** The link between a top bar and the content it scrolls with. */
@Stable
class QItTopBarScroll
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
fun rememberQItTopBarScroll(kind: QItTopBarScrollKind): QItTopBarScroll {
    val behavior = when (kind) {
        QItTopBarScrollKind.PINNED -> TopAppBarDefaults.pinnedScrollBehavior()
        QItTopBarScrollKind.HIDE_ON_SCROLL -> TopAppBarDefaults.enterAlwaysScrollBehavior()
        QItTopBarScrollKind.COLLAPSE_ON_SCROLL -> TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    }
    return remember(behavior) { QItTopBarScroll(behavior) }
}

/**
 * The screen's app bar. It runs under the status bar, and in a glass look it frosts the content
 * scrolling behind it.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QItTopBar(
    title: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    size: QItTopBarSize = QItTopBarSize.SMALL,
    scroll: QItTopBarScroll? = null,
) {
    val glass = glassLook(MaterialTheme.colorScheme.surfaceContainer, QItSurfaceRole.CHROME)
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
        modifier.qitGlass(glass, RectangleShape, LocalQItBackdrop.current, QItGlassEdge.BOTTOM)
    }
    when (size) {
        QItTopBarSize.SMALL -> TopAppBar(
            title = title,
            modifier = barModifier,
            navigationIcon = navigationIcon,
            actions = actions,
            colors = colors,
            scrollBehavior = scroll?.behavior,
        )

        QItTopBarSize.LARGE -> LargeTopAppBar(
            title = title,
            modifier = barModifier,
            navigationIcon = navigationIcon,
            actions = actions,
            colors = colors,
            scrollBehavior = scroll?.behavior,
        )
    }
}
