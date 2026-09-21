package dev.sadakat.qit.presentation.reader

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.DownloadDone
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Watch
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.semantics.SemanticsPropertyReceiver
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.sadakat.qit.R
import dev.sadakat.qit.core.domain.model.Ayah
import dev.sadakat.qit.core.domain.model.QuranMeta
import dev.sadakat.qit.core.domain.model.RecitationMode
import dev.sadakat.qit.core.domain.model.Surah
import dev.sadakat.qit.core.domain.repository.SurahDownloadState
import dev.sadakat.qit.presentation.components.NumberBadge
import dev.sadakat.qit.ui.theme.AmiriQuran

/** Semantics flag marking the ayah that is currently playing. */
val AyahIsPlaying = SemanticsPropertyKey<Boolean>("AyahIsPlaying")

var SemanticsPropertyReceiver.ayahIsPlaying by AyahIsPlaying

/** Connects [SurahReaderScreen] to its [SurahReaderViewModel]. */
@Composable
fun SurahReaderRoute(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SurahReaderViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    SurahReaderScreen(
        state = state,
        onBack = onBack,
        onAyahClick = viewModel::playAyah,
        onPlaySurah = viewModel::playSurah,
        onDownload = viewModel::download,
        onRemove = viewModel::remove,
        onSendToWatch = viewModel::sendToWatch,
        onModeChange = viewModel::setMode,
        onConsumeMessage = viewModel::consumeMessage,
        modifier = modifier,
    )
}

/** One surah, ayah by ayah: Arabic text, the mode's translation and the reading actions. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SurahReaderScreen(
    state: SurahReaderUiState,
    onBack: () -> Unit,
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
    LaunchedEffect(messageText) {
        messageText?.let {
            snackbarHostState.showSnackbar(it)
            onConsumeMessage()
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

    Box(modifier = modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = state.surah?.nameEnglish.orEmpty(),
                            style = MaterialTheme.typography.titleLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        state.surah?.let {
                            Text(
                                text = it.meaningEnglish,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = stringResource(R.string.cd_back),
                        )
                    }
                },
                actions = {
                    DownloadAction(
                        state = state.downloadState,
                        onDownload = onDownload,
                        onRemoveClick = { removeDialogPending = true },
                    )
                    IconButton(onClick = onSendToWatch) {
                        Icon(
                            imageVector = Icons.Rounded.Watch,
                            contentDescription = stringResource(R.string.cd_send_to_watch),
                        )
                    }
                    ModeMenu(mode = state.mode, onModeChange = onModeChange)
                },
                // Inset paddings come from the app scaffold; don't apply them twice.
                windowInsets = WindowInsets(0, 0, 0, 0),
            )

            when {
                state.loadFailed -> Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text(
                        text = stringResource(R.string.load_error),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(16.dp),
                    )
                }
                state.surah == null -> Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                else -> AyahList(
                    state = state,
                    onAyahClick = onAyahClick,
                    onPlaySurah = onPlaySurah,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        SnackbarHost(hostState = snackbarHostState, modifier = Modifier.align(Alignment.BottomCenter))
    }
}

/** The reader's download action: offer, show progress, or offer removal (after confirmation). */
@Composable
private fun DownloadAction(
    state: SurahDownloadState,
    onDownload: () -> Unit,
    onRemoveClick: () -> Unit,
) {
    when (state) {
        is SurahDownloadState.Downloading -> {
            val downloadingText = stringResource(R.string.cd_downloading)
            CircularProgressIndicator(
                progress = { state.progress },
                strokeWidth = 2.dp,
                modifier = Modifier
                    .size(24.dp)
                    .semantics { contentDescription = downloadingText },
            )
        }
        is SurahDownloadState.Downloaded -> IconButton(onClick = onRemoveClick) {
            Icon(
                imageVector = Icons.Rounded.DownloadDone,
                contentDescription = stringResource(R.string.cd_remove_download),
            )
        }
        else -> IconButton(onClick = onDownload) {
            Icon(imageVector = Icons.Rounded.Download, contentDescription = stringResource(R.string.cd_download))
        }
    }
}

/** Menu of the three recitation modes with a checkmark on the active one. */
@Composable
private fun ModeMenu(mode: RecitationMode, onModeChange: (RecitationMode) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(
                imageVector = Icons.Rounded.Tune,
                contentDescription = stringResource(R.string.cd_recitation_mode),
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            RecitationMode.entries.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.label) },
                    onClick = {
                        expanded = false
                        onModeChange(option)
                    },
                    trailingIcon = {
                        if (option == mode) {
                            Icon(imageVector = Icons.Rounded.Check, contentDescription = null)
                        }
                    },
                )
            }
        }
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

/** Basmala header, play button and the ayahs, scrolled to the playing/initial ayah. */
@Composable
private fun AyahList(
    state: SurahReaderUiState,
    onAyahClick: (Int) -> Unit,
    onPlaySurah: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val surah: Surah = state.surah ?: return
    val listState = rememberLazyListState()
    val headerCount = 1

    // Keep the playing ayah in view as it advances.
    LaunchedEffect(state.playingAyah) {
        state.playingAyah?.let { playing ->
            listState.animateScrollToItem(headerCount + playing - 1)
        }
    }
    // Open deep-linked to an ayah: land there without animation.
    LaunchedEffect(state.initialAyah) {
        if (state.initialAyah > 0) listState.scrollToItem(headerCount + state.initialAyah - 1)
    }

    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxWidth(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item(key = "header") {
            ReaderHeader(surah = surah, onPlaySurah = onPlaySurah)
        }
        items(state.ayahs, key = { it.number }) { ayah ->
            AyahRow(
                ayah = ayah,
                translationTrack = state.mode.translation,
                isPlaying = state.playingAyah == ayah.number,
                onClick = { onAyahClick(ayah.number) },
            )
        }
    }
}

@Composable
private fun ReaderHeader(surah: Surah, onPlaySurah: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (QuranMeta.hasBasmalaPrefix(surah.number)) {
            Text(
                text = stringResource(R.string.basmala),
                fontFamily = AmiriQuran,
                fontSize = 22.sp,
                lineHeight = 36.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                textAlign = TextAlign.Center,
            )
        }
        Button(onClick = onPlaySurah) {
            Text(stringResource(R.string.play_surah))
        }
        Spacer(Modifier.height(8.dp))
    }
}

/** One ayah: number badge, Arabic (right-aligned, Amiri) and the mode's translation below. */
@Composable
private fun AyahRow(
    ayah: Ayah,
    translationTrack: dev.sadakat.qit.core.domain.model.Track?,
    isPlaying: Boolean,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("ayah_${ayah.number}")
            .semantics { ayahIsPlaying = isPlaying }
            .clickable(onClick = onClick)
            .background(
                if (isPlaying) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent,
                RoundedCornerShape(12.dp),
            )
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            NumberBadge(number = ayah.number, size = 28.dp)
        }
        Text(
            text = ayah.arabic,
            fontFamily = AmiriQuran,
            fontSize = 26.sp,
            lineHeight = 46.sp,
            textAlign = TextAlign.End,
            modifier = Modifier.fillMaxWidth(),
        )
        translationTrack?.let { track ->
            ayah.translation(track)?.let { translation ->
                Text(
                    text = translation,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                )
            }
        }
    }
}
