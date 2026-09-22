package dev.sadakat.qit.presentation.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import dev.sadakat.qit.R
import dev.sadakat.qit.core.designsystem.QItTheme
import dev.sadakat.qit.core.domain.model.Revelation
import dev.sadakat.qit.core.domain.repository.SurahDownloadState
import dev.sadakat.qit.presentation.components.DownloadIndicator
import dev.sadakat.qit.presentation.components.NumberBadge

/** A surah in the list: its number in the octagram, names, size and offline state. */
@Composable
fun SurahRow(row: SurahRowUi, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val surah = row.surah
    val nameColor = if (row.isPlaying) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
    ListRow(
        onClick = onClick,
        onClickLabel = stringResource(R.string.cd_open_surah),
        modifier = modifier.testTag("surah_${surah.number}"),
    ) {
        NumberBadge(surah.number, label = stringResource(R.string.home_cd_surah_number, surah.number))
        Spacer(Modifier.width(QItTheme.spacing.md))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = surah.nameEnglish,
                    style = MaterialTheme.typography.titleMedium,
                    color = nameColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (row.isPlaying) {
                    Spacer(Modifier.width(QItTheme.spacing.xs))
                    Icon(
                        imageVector = Icons.Rounded.GraphicEq,
                        contentDescription = stringResource(R.string.home_cd_playing),
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(QItTheme.sizes.iconSmall),
                    )
                }
            }
            val ayahs = pluralStringResource(R.plurals.ayah_count, surah.ayahCount, surah.ayahCount)
            val revelation = stringResource(
                if (surah.revelation == Revelation.MECCAN) R.string.revelation_meccan else R.string.revelation_medinan,
            )
            Text(
                text = stringResource(R.string.home_surah_subtitle, surah.meaningEnglish, ayahs, revelation),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.width(QItTheme.spacing.sm))
        Text(
            text = surah.nameArabicShort,
            style = QItTheme.arabic.label,
            color = QItTheme.colors.arabicText,
            maxLines = 1,
        )
        if (row.download !is SurahDownloadState.NotDownloaded) {
            Spacer(Modifier.width(QItTheme.spacing.sm))
            DownloadIndicator(row.download)
        }
    }
}

/** A juz and where it starts. */
@Composable
fun JuzRow(row: JuzRowUi, onClick: () -> Unit, modifier: Modifier = Modifier) {
    ListRow(
        onClick = onClick,
        onClickLabel = stringResource(R.string.home_cd_open_juz),
        modifier = modifier.testTag("juz_${row.juz}"),
    ) {
        NumberBadge(row.juz, label = stringResource(R.string.home_cd_juz_number, row.juz))
        Spacer(Modifier.width(QItTheme.spacing.md))
        Column(Modifier.weight(1f)) {
            Text(stringResource(R.string.home_juz_title, row.juz), style = MaterialTheme.typography.titleMedium)
            Text(
                text = stringResource(R.string.home_juz_starts, row.surahName, row.start.surah, row.start.ayah),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** The search matched a verse reference: go straight to it. */
@Composable
fun JumpRow(jump: AyahJumpUi, onClick: () -> Unit, modifier: Modifier = Modifier) {
    ListRow(
        onClick = onClick,
        onClickLabel = stringResource(R.string.home_cd_open_verse),
        modifier = modifier.testTag("jump"),
    ) {
        Box(
            modifier = Modifier
                .size(QItTheme.sizes.numberBadge)
                .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.MenuBook,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(QItTheme.sizes.icon),
            )
        }
        Spacer(Modifier.width(QItTheme.spacing.md))
        Column(Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.home_jump_to, jump.ref.surah, jump.ref.ayah),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = stringResource(R.string.home_jump_to_surah, jump.surahName),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ListRow(
    onClick: () -> Unit,
    onClickLabel: String,
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = QItTheme.sizes.touchTarget)
            .clickable(onClickLabel = onClickLabel, onClick = onClick)
            .padding(horizontal = QItTheme.spacing.screenGutter, vertical = QItTheme.spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        content = content,
    )
}
