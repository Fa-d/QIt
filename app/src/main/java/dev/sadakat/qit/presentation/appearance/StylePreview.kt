package dev.sadakat.qit.presentation.appearance

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import dev.sadakat.qit.R
import dev.sadakat.qit.core.designsystem.QItTheme
import dev.sadakat.qit.core.designsystem.skin.QItStyle
import dev.sadakat.qit.core.designsystem.skin.QItTone
import dev.sadakat.qit.core.ui.kit.QItAppShell
import dev.sadakat.qit.core.ui.kit.QItBottomBar
import dev.sadakat.qit.core.ui.kit.QItScaffold
import dev.sadakat.qit.core.ui.kit.QItTopBar
import dev.sadakat.qit.presentation.components.NumberBadge
import dev.sadakat.qit.ui.theme.QItAppTheme

/**
 * A live miniature of the app in [style]: the real theme and kit, rendered at a fraction of their
 * size, so what the card shows is exactly what choosing it gives. Decorative for screen readers:
 * the card around it says what it is.
 */
@Composable
fun StylePreview(style: QItStyle, tone: QItTone, dynamicColor: Boolean, modifier: Modifier = Modifier) {
    val density = LocalDensity.current
    Box(
        modifier
            .clearAndSetSemantics {}
            // The miniature isn't a window: no status or navigation bar to keep clear of.
            .consumeWindowInsets(WindowInsets.safeDrawing),
    ) {
        // Text keeps its size relation to the frame whatever the user's font scale: it's a picture.
        CompositionLocalProvider(LocalDensity provides Density(density.density * PREVIEW_SCALE, fontScale = 1f)) {
            QItAppTheme(dynamicColor = dynamicColor, style = style, tone = tone) {
                MiniApp()
            }
        }
    }
}

@Composable
private fun MiniApp() {
    QItAppShell(bottomBar = { MiniPlayerBar() }) { shellPadding ->
        QItScaffold(
            topBar = { QItTopBar(title = { Text(stringResource(R.string.preview_title)) }) },
            contentPadding = shellPadding,
        ) { padding ->
            Column(
                verticalArrangement = Arrangement.spacedBy(QItTheme.spacing.md),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = QItTheme.spacing.lg, vertical = QItTheme.spacing.sm),
            ) {
                MiniRow(1, stringResource(R.string.preview_surah_1))
                MiniRow(2, stringResource(R.string.preview_surah_2))
                MiniAyah()
            }
        }
    }
}

@Composable
private fun MiniRow(number: Int, name: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(QItTheme.spacing.md),
    ) {
        NumberBadge(number)
        Text(name, style = MaterialTheme.typography.titleMedium, maxLines = 1)
    }
}

/** An ayah being recited, on its highlight, with the word pointer's pill: the page every look keeps. */
@Composable
private fun MiniAyah() {
    val colors = QItTheme.colors
    Box(
        Modifier
            .fillMaxWidth()
            .background(colors.playingAyahHighlight, RoundedCornerShape(QItTheme.radius.md))
            .padding(QItTheme.spacing.md),
    ) {
        Text(
            text = stringResource(R.string.arabic_size_preview),
            style = QItTheme.arabic.body,
            color = colors.onPlayingAyahHighlight,
            textAlign = TextAlign.Center,
            maxLines = 1,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun MiniPlayerBar() {
    QItBottomBar {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = QItTheme.spacing.lg, vertical = QItTheme.spacing.md),
        ) {
            Text(
                stringResource(R.string.preview_now_playing),
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.weight(1f),
            )
            Icon(Icons.Rounded.Pause, contentDescription = null)
        }
    }
}

/** The miniature's size relative to the real app. */
private const val PREVIEW_SCALE = 0.5f
