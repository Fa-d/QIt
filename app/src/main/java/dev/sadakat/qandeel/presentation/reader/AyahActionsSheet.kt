package dev.sadakat.qandeel.presentation.reader

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import dev.sadakat.qandeel.R
import dev.sadakat.qandeel.core.designsystem.QandeelTheme
import dev.sadakat.qandeel.core.domain.player.WordPointer
import dev.sadakat.qandeel.core.ui.kit.QandeelSheet
import dev.sadakat.qandeel.presentation.components.RecitedArabicText
import dev.sadakat.qandeel.presentation.components.WordByWordText

/** The long-pressed ayah's sheet: its words with their meanings, then what to do with it. */
@Composable
fun AyahActionsSheet(
    ayah: AyahActionsUi,
    title: String,
    actions: AyahSheetActions,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    QandeelSheet(onDismissRequest = onDismiss, modifier = modifier) {
        AyahActionsContent(ayah = ayah, title = title, actions = actions)
    }
}

/**
 * The sheet's stateless content: "Al-Baqarah · 2:255", the ayah word by word, each word over its
 * meaning (the glossary, whatever the word-by-word setting), and the actions. Kept separate from the
 * sheet so tests and goldens render it without a window.
 */
@Composable
fun AyahActionsContent(ayah: AyahActionsUi, title: String, actions: AyahSheetActions, modifier: Modifier = Modifier) {
    val gutter = QandeelTheme.spacing.screenGutter
    Column(
        modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier
                .padding(horizontal = gutter, vertical = QandeelTheme.spacing.sm)
                .semantics { heading() },
        )
        Glossary(ayah, Modifier.padding(horizontal = gutter))
        Spacer(Modifier.padding(top = QandeelTheme.spacing.sm))
        ActionRow(Icons.Rounded.PlayArrow, stringResource(R.string.ayah_action_play), actions.onPlay)
        ActionRow(Icons.Rounded.Repeat, stringResource(R.string.ayah_action_repeat), actions.onRepeat)
        ActionRow(Icons.Rounded.ContentCopy, stringResource(R.string.ayah_action_copy), actions.onCopy)
        ActionRow(Icons.Rounded.Share, stringResource(R.string.ayah_action_share), actions.onShare)
        Spacer(Modifier.padding(bottom = QandeelTheme.spacing.lg))
    }
}

/** The ayah word by word over its meanings; the plain ayah when no meanings could be loaded. */
@Composable
private fun Glossary(ayah: AyahActionsUi, modifier: Modifier = Modifier) {
    val colors = QandeelTheme.colors
    if (ayah.words.any { it.meaning != null }) {
        WordByWordText(
            text = ayah.arabic,
            meanings = ayah.words.map { it.meaning.orEmpty() },
            pointer = WordPointer.Off,
            style = QandeelTheme.arabic.body,
            recitedColor = colors.arabicText,
            onHighlight = false,
            keepCurrentWordInView = false,
            modifier = modifier.fillMaxWidth(),
        )
    } else {
        RecitedArabicText(
            text = ayah.arabic,
            pointer = WordPointer.Off,
            style = QandeelTheme.arabic.body,
            recitedColor = colors.arabicText,
            onHighlight = false,
            keepCurrentLineInView = false,
            // Right, not End: the Arabic styles set an RTL text direction, where End is the left edge.
            textAlign = TextAlign.Right,
            modifier = modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun ActionRow(icon: ImageVector, label: String, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = QandeelTheme.sizes.touchTarget)
            .clickable(onClick = onClick)
            .padding(horizontal = QandeelTheme.spacing.screenGutter, vertical = QandeelTheme.spacing.sm),
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(QandeelTheme.spacing.lg))
        Text(label, style = MaterialTheme.typography.bodyLarge)
    }
}
