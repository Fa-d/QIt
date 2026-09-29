package dev.sadakat.qandeel.wear.presentation.home

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberOverscrollEffect
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.TransformingLazyColumnItemScope
import androidx.wear.compose.foundation.lazy.TransformingLazyColumnState
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.EdgeButton
import androidx.wear.compose.material3.FilledTonalButton
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.SurfaceTransformation
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.lazy.rememberTransformationSpec
import androidx.wear.compose.material3.lazy.transformedHeight
import dev.sadakat.qandeel.wear.R
import dev.sadakat.qandeel.wear.presentation.ScreenHeader

@Composable
fun HomeRoute(
    onSurahsClick: () -> Unit,
    onJuzClick: () -> Unit,
    onDownloadedClick: () -> Unit,
    onModeClick: () -> Unit,
    onNowPlayingClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: WearHomeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    HomeScreen(
        uiState = uiState,
        onSurahsClick = onSurahsClick,
        onJuzClick = onJuzClick,
        onDownloadedClick = onDownloadedClick,
        onModeClick = onModeClick,
        onNowPlayingClick = onNowPlayingClick,
        onContinueClick = {
            viewModel.continuePlaying()
            onNowPlayingClick()
        },
        modifier = modifier,
    )
}

/** The home hub: a few destinations and an edge button for what playback is doing. */
@Composable
fun HomeScreen(
    uiState: WearHomeUiState,
    onSurahsClick: () -> Unit,
    onJuzClick: () -> Unit,
    onDownloadedClick: () -> Unit,
    onModeClick: () -> Unit,
    onNowPlayingClick: () -> Unit,
    onContinueClick: () -> Unit,
    modifier: Modifier = Modifier,
    listState: TransformingLazyColumnState = rememberTransformingLazyColumnState(),
) {
    ScreenScaffold(
        scrollState = listState,
        modifier = modifier,
        edgeButton = {
            // The button chains crown input through to the list, as if the list went edge to edge.
            val scrollable = Modifier.scrollable(
                listState,
                orientation = Orientation.Vertical,
                reverseDirection = true,
                overscrollEffect = rememberOverscrollEffect(),
            )
            when {
                uiState.isQueued -> EdgeButton(onClick = onNowPlayingClick, modifier = scrollable) {
                    Text(stringResource(R.string.now_playing))
                }

                uiState.continuePosition != null -> EdgeButton(onClick = onContinueClick, modifier = scrollable) {
                    Text(stringResource(R.string.continue_chip, uiState.continuePosition))
                }
            }
        },
    ) { contentPadding ->
        TransformingLazyColumn(
            state = listState,
            contentPadding = contentPadding,
            modifier = Modifier.fillMaxSize(),
        ) {
            if (!uiState.loaded) {
                item(key = "loading") {
                    Text(stringResource(R.string.loading), modifier = Modifier.fillMaxWidth())
                }
            } else {
                item(key = "title") { ScreenHeader(stringResource(R.string.home_title)) }
                item(key = "surahs") {
                    HubRow(
                        label = stringResource(R.string.surahs),
                        icon = { Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = null) },
                        onClick = onSurahsClick,
                    )
                }
                item(key = "juz") {
                    HubRow(
                        label = stringResource(R.string.by_juz),
                        icon = { Icon(Icons.Filled.FormatListNumbered, contentDescription = null) },
                        onClick = onJuzClick,
                    )
                }
                // No downloads yet: the row would only repeat its empty destination.
                if (uiState.downloadedCount > 0) {
                    item(key = "downloaded") {
                        HubRow(
                            label = stringResource(R.string.downloaded_surahs, uiState.downloadedCount),
                            icon = { Icon(Icons.Filled.DownloadDone, contentDescription = null) },
                            onClick = onDownloadedClick,
                        )
                    }
                }
                item(key = "mode") {
                    HubRow(
                        label = stringResource(R.string.recitation, uiState.mode.label),
                        icon = { Icon(Icons.Filled.Audiotrack, contentDescription = null) },
                        onClick = onModeClick,
                    )
                }
            }
        }
    }
}

/** One hub row, with the edge-morphing treatment Wear Material 3 gives list buttons. */
@Composable
private fun TransformingLazyColumnItemScope.HubRow(
    label: String,
    icon: @Composable BoxScope.() -> Unit,
    onClick: () -> Unit,
) {
    val transformationSpec = rememberTransformationSpec()
    FilledTonalButton(
        onClick = onClick,
        modifier = Modifier
            .minimumVerticalContentPadding(ButtonDefaults.minimumVerticalListContentPadding)
            .fillMaxWidth()
            .transformedHeight(this, transformationSpec),
        transformation = SurfaceTransformation(transformationSpec),
        icon = icon,
    ) {
        Text(label)
    }
}
