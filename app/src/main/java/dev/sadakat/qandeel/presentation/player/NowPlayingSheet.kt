package dev.sadakat.qandeel.presentation.player

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import dev.sadakat.qandeel.R
import dev.sadakat.qandeel.core.designsystem.QandeelTheme
import dev.sadakat.qandeel.core.designsystem.component.PlayerTokens
import dev.sadakat.qandeel.core.domain.model.RecitationMode
import dev.sadakat.qandeel.core.domain.player.NowPlaying
import dev.sadakat.qandeel.core.domain.player.PlaybackProgress
import dev.sadakat.qandeel.core.domain.player.WordPointer
import dev.sadakat.qandeel.core.ui.kit.QandeelMenu
import dev.sadakat.qandeel.core.ui.kit.QandeelSheet
import dev.sadakat.qandeel.core.ui.kit.rememberQandeelSheetState
import dev.sadakat.qandeel.presentation.components.RecitedArabicText
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.roundToInt

/** The full player, slid up over the app from the mini player. */
@Composable
fun NowPlayingSheet(
    state: PlayerUiState,
    actions: NowPlayingActions,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    pointer: WordPointer = WordPointer.Off,
    progress: () -> PlaybackProgress = { PlaybackProgress.START },
) {
    val sheetState = rememberQandeelSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    QandeelSheet(
        onDismissRequest = onDismiss,
        state = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        // The sheet is its own window: expose its test tags as resource ids too (baseline profile).
        modifier = modifier.semantics { testTagsAsResourceId = true },
    ) {
        NowPlayingContent(
            state = state,
            actions = actions,
            pointer = pointer,
            progress = progress,
            onCollapse = { scope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() } },
        )
    }
}

/**
 * The full player's content, kept to what listening needs: the reciting ayah large, with the word
 * pointer and its translation; the whole surah as one time bar; the transport, with repeat and speed
 * at its sides; and the recitation mode and sleep timer in one quiet row.
 */
@Composable
fun NowPlayingContent(
    state: PlayerUiState,
    actions: NowPlayingActions,
    modifier: Modifier = Modifier,
    pointer: WordPointer = WordPointer.Off,
    progress: () -> PlaybackProgress = { PlaybackProgress.START },
    onCollapse: () -> Unit = {},
) {
    val nowPlaying = state.nowPlaying ?: return
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = QandeelTheme.spacing.screenGutter)
            .padding(bottom = QandeelTheme.spacing.lg),
    ) {
        Header(state, nowPlaying, actions, onCollapse)
        HorizontalDivider(
            thickness = QandeelTheme.sizes.ornamentStroke,
            color = QandeelTheme.colors.ornament,
            modifier = Modifier.padding(vertical = QandeelTheme.spacing.md),
        )
        AyahText(
            arabic = state.ayahArabic ?: stringResource(R.string.basmala),
            translation = state.ayahTranslation,
            pointer = pointer,
            meanings = state.ayahMeanings,
            modifier = Modifier.weight(1f),
        )
        SurahTimeBar(nowPlaying, progress, ayahAt = state::ayahAt, onSeek = actions.onSeek)
        TransportRow(nowPlaying, actions, Modifier.padding(top = QandeelTheme.spacing.sm))
        ModeAndSleepRow(
            mode = nowPlaying.mode,
            voice = state.voice,
            sleepTimer = state.sleepTimer,
            actions = actions,
            modifier = Modifier.padding(top = QandeelTheme.spacing.md),
        )
    }
}

@Composable
private fun Header(state: PlayerUiState, nowPlaying: NowPlaying, actions: NowPlayingActions, onCollapse: () -> Unit) {
    var menuOpen by remember { mutableStateOf(false) }
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onCollapse) {
            Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = stringResource(R.string.player_cd_close))
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = QandeelTheme.spacing.xs),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = state.surahName ?: stringResource(R.string.surah_fallback_name, nowPlaying.surah),
                style = MaterialTheme.typography.titleLarge,
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
                // Announced as the recitation moves on, without interrupting what's being read out.
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            )
        }
        Box {
            IconButton(onClick = { menuOpen = true }) {
                Icon(Icons.Rounded.MoreVert, contentDescription = stringResource(R.string.player_cd_more))
            }
            QandeelMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
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

/**
 * The ayah itself — the heart of the screen, with the word pointer. Long ayahs scroll inside their
 * space, following the recited line. With word by word on, the meaning of the word being recited is
 * shown under the Arabic. While the translation is read, it comes forward and the Arabic steps back.
 */
@Composable
private fun AyahText(
    arabic: String,
    translation: String?,
    pointer: WordPointer,
    meanings: List<String>,
    modifier: Modifier = Modifier,
) {
    val translating = pointer == WordPointer.Translating
    Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .testTag("player_ayah"),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            RecitedArabicText(
                text = arabic,
                pointer = pointer,
                style = QandeelTheme.arabic.display,
                textAlign = TextAlign.Center,
                keepCurrentLineInView = true,
                modifier = Modifier.fillMaxWidth(),
            )
            if (meanings.isNotEmpty()) CurrentWordMeaning(meanings, pointer)
            translation?.let {
                Spacer(Modifier.height(QandeelTheme.spacing.md))
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (translating) QandeelTheme.colors.currentWord else QandeelTheme.colors.translationText,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

/**
 * The meaning of the word being recited, one line under the Arabic. The line keeps its place while
 * no word is (the translation playing, a pause), so the ayah never shifts under the reader.
 */
@Composable
private fun CurrentWordMeaning(meanings: List<String>, pointer: WordPointer) {
    val meaning = (pointer as? WordPointer.Reciting)?.let { meanings.getOrNull(it.word) }
    val motion = QandeelTheme.motion
    AnimatedContent(
        targetState = meaning,
        transitionSpec = { fadeIn(motion.enter()) togetherWith fadeOut(motion.exit()) },
        contentAlignment = Alignment.Center,
        label = "currentWordMeaning",
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = QandeelTheme.spacing.sm)
            .testTag("player_word_meaning"),
    ) { shown ->
        Text(
            text = shown.orEmpty(),
            style = MaterialTheme.typography.titleMedium,
            color = QandeelTheme.colors.primary,
            textAlign = TextAlign.Center,
            maxLines = 1,
            minLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
