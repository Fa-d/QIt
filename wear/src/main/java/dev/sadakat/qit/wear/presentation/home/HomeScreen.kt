package dev.sadakat.qit.wear.presentation.home

// qit:legacy-ui — predates the design tokens; its UX slice replaces it.

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.wear.compose.foundation.ExperimentalWearFoundationApi
import androidx.wear.compose.foundation.lazy.AutoCenteringParams
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.foundation.rotary.RotaryScrollableDefaults
import androidx.wear.compose.foundation.rotary.rotaryScrollable
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.CircularProgressIndicator
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.PositionIndicator
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText
import dev.sadakat.qit.core.domain.model.RecitationMode
import dev.sadakat.qit.core.domain.repository.SurahDownloadState
import dev.sadakat.qit.wear.R
import kotlin.math.roundToInt

@Composable
fun HomeRoute(
    onSurahClick: (Int) -> Unit,
    onNowPlayingClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: WearHomeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    HomeScreen(
        uiState = uiState,
        onSurahClick = onSurahClick,
        onNowPlayingClick = onNowPlayingClick,
        onContinueClick = viewModel::continuePlaying,
        onCycleMode = viewModel::cycleMode,
        modifier = modifier,
    )
}

@OptIn(ExperimentalWearFoundationApi::class)
@Composable
fun HomeScreen(
    uiState: WearHomeViewModel.UiState,
    onSurahClick: (Int) -> Unit,
    onNowPlayingClick: () -> Unit,
    onContinueClick: () -> Unit,
    onCycleMode: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberScalingLazyListState()
    val focusRequester = remember { FocusRequester() }

    Box(modifier.fillMaxSize()) {
        ScalingLazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .rotaryScrollable(RotaryScrollableDefaults.behavior(listState), focusRequester),
            state = listState,
            autoCentering = AutoCenteringParams(itemIndex = 0),
        ) {
            if (uiState.rows.isEmpty()) {
                item { LoadingRow() }
            } else {
                uiState.nowPlayingChip?.let { chip ->
                    item(key = "now-playing") {
                        NowPlayingChipRow(chip, onNowPlayingClick)
                    }
                } ?: uiState.continueChip?.let { chip ->
                    item(key = "continue") {
                        ContinueChipRow(chip, onContinueClick)
                    }
                }
                item(key = "mode") {
                    ModeChipRow(uiState.mode, onCycleMode)
                }
                items(uiState.rows.size, key = { uiState.rows[it].surah.number }) { index ->
                    SurahChipRow(uiState.rows[index], onSurahClick)
                }
            }
        }
        TimeText()
        PositionIndicator(scalingLazyListState = listState, modifier = Modifier.align(Alignment.CenterEnd))
    }

    // Rotary input only reaches the list when it holds focus.
    LaunchedEffect(Unit) { focusRequester.requestFocus() }
}

@Composable
private fun LoadingRow(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
private fun NowPlayingChipRow(
    chip: WearHomeViewModel.NowPlayingChip,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Chip(
        onClick = onClick,
        label = { Text(stringResource(R.string.now_playing_chip, chip.surahName, chip.position)) },
        icon = { Icon(Icons.Filled.PlayArrow, contentDescription = null) },
        modifier = modifier.fillMaxWidth(),
    )
}

@Composable
private fun ContinueChipRow(chip: WearHomeViewModel.ContinueChip, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Chip(
        onClick = onClick,
        label = { Text(stringResource(R.string.continue_chip, chip.position)) },
        icon = { Icon(Icons.Filled.Replay, contentDescription = null) },
        modifier = modifier.fillMaxWidth(),
    )
}

@Composable
private fun ModeChipRow(mode: RecitationMode, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Chip(
        onClick = onClick,
        label = { Text(mode.label) },
        secondaryLabel = { Text(stringResource(R.string.recitation_mode_hint)) },
        icon = { Icon(Icons.Filled.Audiotrack, contentDescription = null) },
        modifier = modifier.fillMaxWidth(),
    )
}

@Composable
private fun SurahChipRow(row: WearHomeViewModel.SurahRow, onSurahClick: (Int) -> Unit, modifier: Modifier = Modifier) {
    Chip(
        onClick = { onSurahClick(row.surah.number) },
        label = { Text(stringResource(R.string.surah_label, row.surah.number, row.surah.nameEnglish)) },
        secondaryLabel = {
            // Only downloads get a status; a plain surah (streams when played) shows its size alone.
            val status = downloadLabel(row.download)
            Text(
                if (status == null) {
                    stringResource(R.string.ayah_count, row.surah.ayahCount)
                } else {
                    stringResource(R.string.ayah_count_secondary, row.surah.ayahCount, status)
                },
            )
        },
        modifier = modifier.fillMaxWidth(),
    )
}

@Composable
private fun downloadLabel(download: SurahDownloadState): String? = when (download) {
    SurahDownloadState.NotDownloaded -> null

    is SurahDownloadState.Downloading ->
        stringResource(R.string.download_progress, (download.progress * 100).roundToInt())

    SurahDownloadState.Downloaded -> stringResource(R.string.download_state_downloaded)

    is SurahDownloadState.Failed -> stringResource(R.string.download_state_failed)
}
