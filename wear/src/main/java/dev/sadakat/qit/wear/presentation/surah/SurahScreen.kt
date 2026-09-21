package dev.sadakat.qit.wear.presentation.surah

// qit:legacy-ui — predates the design tokens; its UX slice replaces it.

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.wear.compose.foundation.ExperimentalWearFoundationApi
import androidx.wear.compose.foundation.lazy.AutoCenteringParams
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.foundation.rotary.RotaryScrollableDefaults
import androidx.wear.compose.foundation.rotary.rotaryScrollable
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.PositionIndicator
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText
import dev.sadakat.qit.core.domain.repository.SurahDownloadState
import dev.sadakat.qit.wear.R
import kotlin.math.roundToInt

@Composable
fun SurahRoute(onPlayNow: () -> Unit, modifier: Modifier = Modifier, viewModel: WearSurahViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    SurahScreen(
        uiState = uiState,
        onPlay = {
            viewModel.play()
            onPlayNow()
        },
        onDownload = viewModel::download,
        onRemove = viewModel::remove,
        modifier = modifier,
    )
}

@OptIn(ExperimentalWearFoundationApi::class)
@Composable
fun SurahScreen(
    uiState: WearSurahViewModel.UiState,
    onPlay: () -> Unit,
    onDownload: () -> Unit,
    onRemove: () -> Unit,
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
            val surah = uiState.surah
            if (surah == null) {
                item { NotFoundRow() }
            } else {
                item(key = "title") {
                    Column {
                        Text(
                            text = stringResource(R.string.surah_label, surah.number, surah.nameEnglish),
                            style = MaterialTheme.typography.title3,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Text(
                            text = stringResource(R.string.ayah_count, surah.ayahCount),
                            style = MaterialTheme.typography.caption1,
                            color = MaterialTheme.colors.secondary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
                item(key = "mode") {
                    Text(
                        text = stringResource(R.string.mode_label, uiState.mode.label),
                        style = MaterialTheme.typography.caption1,
                        color = MaterialTheme.colors.secondary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                item(key = "play") {
                    Chip(
                        onClick = onPlay,
                        label = { Text(stringResource(R.string.play)) },
                        icon = { Icon(Icons.Filled.PlayArrow, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                item(key = "download") {
                    DownloadChipRow(uiState.download, onDownload)
                }
                if (uiState.download is SurahDownloadState.Downloaded) {
                    item(key = "remove") {
                        Chip(
                            onClick = onRemove,
                            label = { Text(stringResource(R.string.remove)) },
                            icon = { Icon(Icons.Filled.Delete, contentDescription = null) },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
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
private fun NotFoundRow(modifier: Modifier = Modifier) {
    Text(
        text = stringResource(R.string.surah_not_found),
        style = MaterialTheme.typography.body1,
        textAlign = TextAlign.Center,
        modifier = modifier.fillMaxWidth(),
    )
}

@Composable
private fun DownloadChipRow(download: SurahDownloadState, onDownload: () -> Unit, modifier: Modifier = Modifier) {
    when (download) {
        is SurahDownloadState.Downloading ->
            Chip(
                onClick = onDownload,
                enabled = false,
                label = {
                    Text(stringResource(R.string.downloading, progressLabel(download.progress)))
                },
                icon = { Icon(Icons.Filled.Download, contentDescription = null) },
                modifier = modifier.fillMaxWidth(),
            )

        SurahDownloadState.Downloaded ->
            Chip(
                onClick = {},
                enabled = false,
                label = { Text(stringResource(R.string.download_state_downloaded)) },
                icon = { Icon(Icons.Filled.DownloadDone, contentDescription = null) },
                modifier = modifier.fillMaxWidth(),
            )

        is SurahDownloadState.Failed ->
            Chip(
                onClick = onDownload,
                label = { Text(stringResource(R.string.retry_download)) },
                icon = { Icon(Icons.Filled.Download, contentDescription = null) },
                modifier = modifier.fillMaxWidth(),
            )

        SurahDownloadState.NotDownloaded ->
            Chip(
                onClick = onDownload,
                label = { Text(stringResource(R.string.download)) },
                icon = { Icon(Icons.Filled.Download, contentDescription = null) },
                modifier = modifier.fillMaxWidth(),
            )
    }
}

@Composable
private fun progressLabel(progress: Float): String =
    stringResource(R.string.download_progress, (progress * 100).roundToInt())
