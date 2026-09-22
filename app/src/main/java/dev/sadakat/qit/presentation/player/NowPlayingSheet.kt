package dev.sadakat.qit.presentation.player

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import dev.sadakat.qit.R
import dev.sadakat.qit.core.designsystem.QItTheme
import dev.sadakat.qit.core.designsystem.component.PlayerTokens
import dev.sadakat.qit.core.domain.model.RecitationMode
import dev.sadakat.qit.core.domain.player.NowPlaying
import kotlin.math.max
import kotlin.math.roundToInt

/** The full player, slid up over the app from the mini player. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NowPlayingSheet(
    state: PlayerUiState,
    actions: NowPlayingActions,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = modifier,
    ) {
        NowPlayingContent(state = state, actions = actions)
    }
}

/**
 * The full player's content: the reciting ayah large, with its translation; where in the surah
 * (drag to move by ayah); the transport; the recitation mode; and repeat, speed and sleep timer.
 */
@Composable
fun NowPlayingContent(state: PlayerUiState, actions: NowPlayingActions, modifier: Modifier = Modifier) {
    val nowPlaying = state.nowPlaying ?: return
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = QItTheme.spacing.screenGutter)
            .padding(bottom = QItTheme.spacing.lg),
    ) {
        Header(state, nowPlaying, actions)
        HorizontalDivider(
            thickness = QItTheme.sizes.ornamentStroke,
            color = QItTheme.colors.ornament,
            modifier = Modifier.padding(vertical = QItTheme.spacing.md),
        )
        AyahText(
            arabic = state.ayahArabic ?: stringResource(R.string.basmala),
            translation = state.ayahTranslation,
            modifier = Modifier.weight(1f),
        )
        AyahSeekBar(nowPlaying, onSeekToAyah = actions.onSeekToAyah)
        Transport(nowPlaying, actions)
        Spacer(Modifier.height(QItTheme.spacing.lg))
        ModeSelector(nowPlaying.mode, onModeChange = actions.onModeChange)
        Spacer(Modifier.height(QItTheme.spacing.md))
        PlayerOptions(
            nowPlaying = nowPlaying,
            sleepTimer = state.sleepTimer,
            onRepeatChange = actions.onRepeatChange,
            onSpeedChange = actions.onSpeedChange,
            onSleepTimerChange = actions.onSleepTimerChange,
        )
    }
}

@Composable
private fun Header(state: PlayerUiState, nowPlaying: NowPlaying, actions: NowPlayingActions) {
    var menuOpen by remember { mutableStateOf(false) }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(
                text = state.surahName ?: stringResource(R.string.surah_fallback_name, nowPlaying.surah),
                style = MaterialTheme.typography.headlineSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = if (nowPlaying.ayah == 0) {
                    stringResource(R.string.player_basmala)
                } else {
                    stringResource(R.string.player_ayah_of, nowPlaying.ayah, nowPlaying.ayahCount)
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Box {
            IconButton(onClick = { menuOpen = true }) {
                Icon(Icons.Rounded.MoreVert, contentDescription = stringResource(R.string.player_cd_more))
            }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.player_open_in_reader)) },
                    leadingIcon = { Icon(Icons.AutoMirrored.Rounded.MenuBook, contentDescription = null) },
                    onClick = {
                        menuOpen = false
                        actions.onOpenReader(nowPlaying.surah, nowPlaying.ayah)
                    },
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.player_stop)) },
                    leadingIcon = { Icon(Icons.Rounded.Stop, contentDescription = null) },
                    onClick = {
                        menuOpen = false
                        actions.onStop()
                    },
                )
            }
        }
    }
}

/** The ayah itself — the heart of the screen. Long ayahs scroll inside their space. */
@Composable
private fun AyahText(arabic: String, translation: String?, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .testTag("player_ayah"),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = arabic,
                style = QItTheme.arabic.display,
                color = QItTheme.colors.arabicText,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            translation?.let {
                Spacer(Modifier.height(QItTheme.spacing.md))
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyLarge,
                    color = QItTheme.colors.translationText,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

/** Where in the surah: drag to any ayah; the position label follows the thumb while dragging. */
@Composable
private fun AyahSeekBar(nowPlaying: NowPlaying, onSeekToAyah: (Int) -> Unit) {
    var dragged by remember { mutableStateOf<Float?>(null) }
    val shownAyah = dragged?.roundToInt() ?: max(nowPlaying.ayah, 1)
    val seekDescription = stringResource(R.string.player_cd_seek)
    Column(Modifier.padding(top = QItTheme.spacing.md)) {
        Slider(
            value = dragged ?: max(nowPlaying.ayah, 1).toFloat(),
            onValueChange = { dragged = it },
            onValueChangeFinished = {
                dragged?.let { onSeekToAyah(it.roundToInt()) }
                dragged = null
            },
            valueRange = 1f..nowPlaying.ayahCount.toFloat(),
            modifier = Modifier
                .fillMaxWidth()
                .semantics { contentDescription = seekDescription },
        )
        Row {
            Text(shownAyah.toString(), style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(1f))
            Text(nowPlaying.ayahCount.toString(), style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
private fun Transport(nowPlaying: NowPlaying, actions: NowPlayingActions) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = QItTheme.spacing.sm),
        horizontalArrangement = Arrangement.spacedBy(QItTheme.spacing.xl, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = actions.onPrevious, modifier = Modifier.size(PlayerTokens.CardPlayButtonSize)) {
            Icon(
                Icons.Rounded.SkipPrevious,
                contentDescription = stringResource(R.string.cd_previous_ayah),
                modifier = Modifier.size(QItTheme.sizes.iconLarge),
            )
        }
        Box(contentAlignment = Alignment.Center) {
            FilledIconButton(
                onClick = actions.onTogglePlayPause,
                modifier = Modifier.size(PlayerTokens.PlayButtonSize),
            ) {
                Icon(
                    imageVector = if (nowPlaying.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    contentDescription = stringResource(
                        if (nowPlaying.isPlaying) R.string.cd_pause else R.string.cd_play,
                    ),
                    modifier = Modifier.size(PlayerTokens.PlayButtonIconSize),
                )
            }
            if (nowPlaying.isBuffering) {
                CircularProgressIndicator(
                    strokeWidth = QItTheme.sizes.strokeThin,
                    modifier = Modifier
                        .size(PlayerTokens.PlayButtonSize)
                        .testTag("player_buffering"),
                )
            }
        }
        IconButton(onClick = actions.onNext, modifier = Modifier.size(PlayerTokens.CardPlayButtonSize)) {
            Icon(
                Icons.Rounded.SkipNext,
                contentDescription = stringResource(R.string.cd_next_ayah),
                modifier = Modifier.size(QItTheme.sizes.iconLarge),
            )
        }
    }
}

@Composable
private fun ModeSelector(mode: RecitationMode, onModeChange: (RecitationMode) -> Unit) {
    val modes = RecitationMode.entries
    val modeDescription = stringResource(R.string.player_cd_mode)
    SingleChoiceSegmentedButtonRow(
        modifier = Modifier
            .fillMaxWidth()
            .semantics { contentDescription = modeDescription },
    ) {
        modes.forEachIndexed { index, option ->
            SegmentedButton(
                selected = option == mode,
                onClick = { if (option != mode) onModeChange(option) },
                shape = SegmentedButtonDefaults.itemShape(index, modes.size),
            ) {
                Text(modeLabel(option), maxLines = 1)
            }
        }
    }
}
