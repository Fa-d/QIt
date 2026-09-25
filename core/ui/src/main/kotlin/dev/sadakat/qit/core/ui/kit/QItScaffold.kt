package dev.sadakat.qit.core.ui.kit

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.BringIntoViewSpec
import androidx.compose.foundation.gestures.LocalBringIntoViewSpec
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import dev.sadakat.qit.core.designsystem.QItTheme
import dev.sadakat.qit.core.ui.kit.glass.LocalQItBackdrop
import dev.sadakat.qit.core.ui.kit.glass.qitBackdropSource
import dev.sadakat.qit.core.ui.kit.glass.rememberQItBackdrop
import kotlin.math.abs
import kotlin.math.max

/** What a screen shows, which decides what may be drawn behind its content. */
enum class QItPage {
    /** Lists and settings: the look's backdrop wash may color the page. */
    CHROME,

    /** The Quran's text: always the plain page, never a wash. */
    READING,
}

/**
 * The app's frame: the page color, a [bottomBar] (the mini player) and [snackbarHost]. Screens get
 * the space the bottom bar takes as padding, not as a smaller box, so their lists scroll on behind
 * it (and behind glass, blurred). Provides the window's backdrop for frosted chrome.
 */
@Composable
fun QItAppShell(
    modifier: Modifier = Modifier,
    bottomBar: @Composable () -> Unit = {},
    snackbarHost: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    val backdrop = rememberQItBackdrop()
    CompositionLocalProvider(LocalQItBackdrop provides backdrop) {
        Scaffold(
            modifier = modifier,
            bottomBar = bottomBar,
            snackbarHost = snackbarHost,
            containerColor = MaterialTheme.colorScheme.background,
            // Screens run under the status bar (their top bars pad it); the bottom is the bar's or
            // the navigation bar's.
            contentWindowInsets = WindowInsets.navigationBars,
        ) { padding ->
            content(PaddingValues(bottom = padding.calculateBottomPadding()))
        }
    }
}

/**
 * A screen: [topBar] over [content], which runs behind the bar and behind the app's bottom bar.
 * [content] gets the padding to keep clear of both, for its lists' content padding; [overlay] (a
 * snackbar, a floating chip) is placed over the content with the same padding.
 *
 * @param contentPadding the app shell's padding for its bottom bar.
 * @param page [QItPage.READING] for the Quran's text, which never gets the look's backdrop wash.
 */
@Composable
fun QItScaffold(
    topBar: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
    page: QItPage = QItPage.CHROME,
    overlay: @Composable BoxScope.(PaddingValues) -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    val backdrop = LocalQItBackdrop.current
    val wash = if (page == QItPage.CHROME) QItTheme.surfaces.backdropWash else 0f
    val washStart = MaterialTheme.colorScheme.primaryContainer
    val washEnd = MaterialTheme.colorScheme.tertiaryContainer
    Scaffold(
        topBar = topBar,
        modifier = modifier,
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0.dp, 0.dp, 0.dp, 0.dp),
    ) { inner ->
        val padding = PaddingValues(top = inner.calculateTopPadding(), bottom = contentPadding.calculateBottomPadding())
        Box(Modifier.fillMaxSize().consumeWindowInsets(padding)) {
            Box(
                Modifier
                    .fillMaxSize()
                    .qitBackdropSource(backdrop)
                    .then(if (wash > 0f) Modifier.backdropWash(washStart, washEnd, wash) else Modifier),
            ) {
                KeepClearOf(padding) { content(padding) }
            }
            overlay(padding)
        }
    }
}

/** Two soft glows, top-start and bottom-end, for frosted chrome to catch. */
private fun Modifier.backdropWash(start: Color, end: Color, strength: Float): Modifier = drawBehind {
    val radius = max(size.width, size.height)
    drawRect(Brush.radialGradient(listOf(start.copy(alpha = strength), Color.Transparent), Offset.Zero, radius))
    drawRect(
        Brush.radialGradient(
            listOf(end.copy(alpha = strength), Color.Transparent),
            Offset(size.width, size.height),
            radius,
        ),
    )
}

/**
 * Scrolling something into view (the recited line) stops short of [padding], so it doesn't land
 * behind a bar. Lists already keep their content padding clear; bring-into-view doesn't.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun KeepClearOf(padding: PaddingValues, content: @Composable () -> Unit) {
    val density = LocalDensity.current
    val top = with(density) { padding.calculateTopPadding().toPx() }
    val bottom = with(density) { padding.calculateBottomPadding().toPx() }
    val spec = remember(top, bottom) { PaddedBringIntoViewSpec(top, bottom) }
    CompositionLocalProvider(LocalBringIntoViewSpec provides spec, content = content)
}

/**
 * The platform's bring-into-view rule (scroll the least that shows the item; leave items bigger
 * than the view alone) applied to the part of the container between [top] and [bottom] padding.
 */
@OptIn(ExperimentalFoundationApi::class)
internal class PaddedBringIntoViewSpec(private val top: Float, private val bottom: Float) : BringIntoViewSpec {
    override fun calculateScrollDistance(offset: Float, size: Float, containerSize: Float): Float {
        val visible = containerSize - top - bottom
        val leading = offset - top
        val trailing = leading + size
        return when {
            leading >= 0f && trailing <= visible -> 0f
            leading < 0f && trailing > visible -> 0f
            abs(leading) < abs(trailing - visible) -> leading
            else -> trailing - visible
        }
    }
}
