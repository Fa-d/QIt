// kit-migration: pending (still builds Material containers itself; move it onto the :core:ui kit)
package dev.sadakat.qit.presentation.reader

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.sadakat.qit.R
import dev.sadakat.qit.core.designsystem.QItTheme
import dev.sadakat.qit.core.designsystem.component.ReaderTokens
import dev.sadakat.qit.core.domain.model.RecitationMode
import dev.sadakat.qit.core.domain.model.Surah
import dev.sadakat.qit.core.domain.player.WordPointer
import kotlinx.coroutines.delay

/** Connects [SurahReaderScreen] to its [SurahReaderViewModel]. */
@Composable
fun SurahReaderRoute(
    onBack: () -> Unit,
    onOpenReadingSettings: () -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
    viewModel: SurahReaderViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val pointer by viewModel.pointer.collectAsStateWithLifecycle()
    SurahReaderScreen(
        state = state,
        pointer = pointer,
        onBack = onBack,
        onOpenReadingSettings = onOpenReadingSettings,
        onAyahClick = viewModel::playAyah,
        onPlaySurah = viewModel::playSurah,
        onDownload = viewModel::download,
        onRemove = viewModel::remove,
        onSendToWatch = viewModel::sendToWatch,
        onModeChange = viewModel::setMode,
        onConsumeMessage = viewModel::consumeMessage,
        // Until this screen moves onto the kit: keep clear of the mini player and the status bar.
        modifier = modifier.padding(contentPadding).statusBarsPadding(),
    )
}

/** One surah, ayah by ayah: Arabic text, the mode's translation and the reading actions. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SurahReaderScreen(
    state: SurahReaderUiState,
    pointer: WordPointer,
    onBack: () -> Unit,
    onOpenReadingSettings: () -> Unit,
    onAyahClick: (Int) -> Unit,
    onPlaySurah: () -> Unit,
    onDownload: () -> Unit,
    onRemove: () -> Unit,
    onSendToWatch: () -> Unit,
    onModeChange: (RecitationMode) -> Unit,
    onConsumeMessage: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val messageText = state.message?.let { message ->
        when (message) {
            is ReaderMessage.SentToWatch ->
                pluralStringResource(R.plurals.sent_to_watch, message.watches, message.watches)

            ReaderMessage.NoWatch -> stringResource(R.string.no_watch_found)
        }
    }
    // The effect outlives recompositions; always call the latest callback, not the first one.
    val currentOnConsumeMessage by rememberUpdatedState(onConsumeMessage)
    LaunchedEffect(messageText) {
        messageText?.let {
            snackbarHostState.showSnackbar(it)
            currentOnConsumeMessage()
        }
    }

    var removeDialogPending by remember { mutableStateOf(false) }
    if (removeDialogPending) {
        RemoveDownloadDialog(
            surahName = state.surah?.nameEnglish.orEmpty(),
            onConfirm = {
                removeDialogPending = false
                onRemove()
            },
            onDismiss = { removeDialogPending = false },
        )
    }

    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    Box(modifier = modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .nestedScroll(scrollBehavior.nestedScrollConnection),
        ) {
            ReaderTopBar(
                surah = state.surah,
                scrollBehavior = scrollBehavior,
                onBack = onBack,
                onOpenReadingSettings = onOpenReadingSettings,
                downloadState = state.downloadState,
                onDownload = onDownload,
                onRemoveClick = { removeDialogPending = true },
                onSendToWatch = onSendToWatch,
            )
            when {
                state.loadFailed -> ReaderLoadError(Modifier.weight(1f))

                state.surah == null -> ReaderLoading(Modifier.weight(1f))

                else -> ReaderContent(
                    state = state,
                    pointer = pointer,
                    onAyahClick = onAyahClick,
                    onPlaySurah = onPlaySurah,
                    onModeChange = onModeChange,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        SnackbarHost(hostState = snackbarHostState, modifier = Modifier.align(Alignment.BottomCenter))
    }
}

/** The header and the ayahs, scrolled to the deep-linked ayah and mirroring the reciting one. */
@Composable
private fun ReaderContent(
    state: SurahReaderUiState,
    pointer: WordPointer,
    onAyahClick: (Int) -> Unit,
    onPlaySurah: () -> Unit,
    onModeChange: (RecitationMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    val surah: Surah = state.surah ?: return
    val listState = rememberLazyListState()
    val follow = rememberFollowAlongState(
        listState = listState,
        headerCount = READER_HEADER_COUNT,
        playingAyah = state.playingAyah,
        enabled = state.followAlong,
    )
    var pulsedAyah by remember { mutableStateOf<Int?>(null) }
    val pulseDuration = QItTheme.motion.durationLong

    // A deep link lands on its ayah without animation, then pulses it once so the eye finds it.
    LaunchedEffect(state.initialAyah) {
        if (state.initialAyah > 0) {
            listState.scrollToItem(recitingAyahIndex(READER_HEADER_COUNT, state.initialAyah))
            pulsedAyah = state.initialAyah
            delay(pulseDuration.toLong())
            pulsedAyah = null
        }
    }

    Box(modifier = modifier.fillMaxWidth()) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .testTag("ayah_list")
                .nestedScroll(follow.userDragObserver),
            contentPadding = PaddingValues(
                top = QItTheme.spacing.sm,
                bottom = QItTheme.spacing.xxl,
            ),
            verticalArrangement = Arrangement.spacedBy(QItTheme.spacing.sm),
        ) {
            item(key = HEADER_KEY) {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    ReaderHeader(
                        surah = surah,
                        mode = state.mode,
                        onPlaySurah = onPlaySurah,
                        onModeChange = onModeChange,
                        listening = state.listening,
                        modifier = Modifier.widthIn(max = ReaderTokens.MaxReadingWidth),
                    )
                }
            }
            items(state.ayahs, key = { it.number }) { ayah ->
                // Inset so the reciting ayah's highlight sits on the page rather than touching its edges.
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = QItTheme.spacing.sm),
                    contentAlignment = Alignment.Center,
                ) {
                    AyahItem(
                        ayah = ayah,
                        translationTrack = state.mode.translation.takeIf { state.showTranslation },
                        isPlaying = state.playingAyah == ayah.number,
                        pulsed = pulsedAyah == ayah.number,
                        pointer = pointer,
                        heardTimes = state.heard.getOrElse(ayah.number - 1) { 0 },
                        followWords = state.followAlong && follow.following,
                        wordMeanings = state.wordMeanings[ayah.number],
                        onClick = {
                            // Reading where the recitation is: mirror it again from here.
                            follow.resume()
                            onAyahClick(ayah.number)
                        },
                        modifier = Modifier.widthIn(max = ReaderTokens.MaxReadingWidth),
                    )
                }
            }
        }
        JumpToRecitingChip(
            follow = follow,
            listState = listState,
            playingAyah = state.playingAyah,
            headerCount = READER_HEADER_COUNT,
            enabled = state.followAlong,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

@Composable
private fun ReaderLoading(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
private fun ReaderLoadError(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Text(
            text = stringResource(R.string.load_error),
            style = MaterialTheme.typography.bodyLarge,
            color = QItTheme.colors.error,
            modifier = Modifier.padding(QItTheme.spacing.lg),
        )
    }
}

@Composable
private fun RemoveDownloadDialog(surahName: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.remove_download_title)) },
        text = { Text(stringResource(R.string.remove_download_text, surahName)) },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text(stringResource(R.string.remove)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
}

/** The header is one list item, so ayah n sits at index [READER_HEADER_COUNT] + n − 1. */
private const val READER_HEADER_COUNT = 1

private const val HEADER_KEY = "header"
