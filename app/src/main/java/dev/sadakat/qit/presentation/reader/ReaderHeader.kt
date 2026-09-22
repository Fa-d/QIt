package dev.sadakat.qit.presentation.reader

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import dev.sadakat.qit.R
import dev.sadakat.qit.core.designsystem.QItTheme
import dev.sadakat.qit.core.domain.model.QuranMeta
import dev.sadakat.qit.core.domain.model.RecitationMode
import dev.sadakat.qit.core.domain.model.Revelation
import dev.sadakat.qit.core.domain.model.Surah
import dev.sadakat.qit.presentation.components.NumberBadge

/**
 * The reader's first item: the surah's identity (number, Arabic and English names), the basmala,
 * the labelled recitation-mode selector and the play action — everything one needs before the ayahs.
 */
@Composable
fun ReaderHeader(
    surah: Surah,
    mode: RecitationMode,
    onPlaySurah: () -> Unit,
    onModeChange: (RecitationMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                top = QItTheme.spacing.sm,
                start = QItTheme.spacing.screenGutter,
                end = QItTheme.spacing.screenGutter,
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            NumberBadge(number = surah.number)
            Spacer(Modifier.width(QItTheme.spacing.md))
            Column(Modifier.weight(1f)) {
                Text(
                    text = surah.nameEnglish,
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.semantics { heading() },
                )
                Text(
                    text = stringResource(
                        R.string.surah_meta_line,
                        surah.meaningEnglish,
                        pluralStringResource(R.plurals.ayah_count, surah.ayahCount, surah.ayahCount),
                        stringResource(surah.revelation.labelRes()),
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.width(QItTheme.spacing.md))
            Text(
                text = surah.nameArabic,
                style = QItTheme.arabic.title,
                color = QItTheme.colors.arabicText,
            )
        }
        if (QuranMeta.hasBasmalaPrefix(surah.number)) {
            Text(
                text = stringResource(R.string.basmala),
                style = QItTheme.arabic.title,
                color = QItTheme.colors.arabicText,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = QItTheme.spacing.xl),
            )
        }
        ModeSelector(
            mode = mode,
            onModeChange = onModeChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = QItTheme.spacing.lg),
        )
        Button(
            onClick = onPlaySurah,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = QItTheme.spacing.lg),
        ) {
            Icon(imageVector = Icons.Rounded.PlayArrow, contentDescription = null)
            Spacer(Modifier.width(QItTheme.spacing.xs))
            Text(stringResource(R.string.play_surah))
        }
        // Gold rule: where the front matter ends and the recited text begins.
        HorizontalDivider(
            modifier = Modifier.padding(top = QItTheme.spacing.xl),
            thickness = QItTheme.sizes.ornamentStroke,
            color = QItTheme.colors.ornament,
        )
    }
}

/** The labelled recitation-mode selector: Arabic, with or without a translation after it. */
@Composable
private fun ModeSelector(mode: RecitationMode, onModeChange: (RecitationMode) -> Unit, modifier: Modifier = Modifier) {
    SingleChoiceSegmentedButtonRow(modifier = modifier) {
        RecitationMode.entries.forEachIndexed { index, option ->
            SegmentedButton(
                selected = option == mode,
                onClick = { onModeChange(option) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = RecitationMode.entries.size),
                label = {
                    Text(
                        text = stringResource(option.labelRes()),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
            )
        }
    }
}

private fun RecitationMode.labelRes(): Int = when (this) {
    RecitationMode.ARABIC_ONLY -> R.string.mode_arabic
    RecitationMode.ARABIC_ENGLISH -> R.string.mode_english
    RecitationMode.ARABIC_BANGLA -> R.string.mode_bangla
}

private fun Revelation.labelRes(): Int = when (this) {
    Revelation.MECCAN -> R.string.revelation_meccan
    Revelation.MEDINAN -> R.string.revelation_medinan
}
