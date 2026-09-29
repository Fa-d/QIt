package dev.sadakat.qandeel.wear.presentation.surah

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberOverscrollEffect
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.TransformingLazyColumnItemScope
import androidx.wear.compose.foundation.lazy.TransformingLazyColumnState
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.AlertDialog
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.EdgeButton
import androidx.wear.compose.material3.FilledTonalButton
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.SurfaceTransformation
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.lazy.TransformationSpec
import androidx.wear.compose.material3.lazy.rememberTransformationSpec
import androidx.wear.compose.material3.lazy.transformedHeight
import dev.sadakat.qandeel.core.designsystem.QandeelTheme
import dev.sadakat.qandeel.core.domain.model.Surah
import dev.sadakat.qandeel.core.domain.repository.SurahDownloadState
import dev.sadakat.qandeel.wear.R
import kotlin.math.roundToInt

@Composable
fun SurahRoute(
    onPlayNow: () -> Unit,
    onModeClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: WearSurahViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    SurahScreen(
        uiState = uiState,
        onPlay = {
            viewModel.play()
            onPlayNow()
        },
        onDownload = viewModel::download,
        onRemove = viewModel::remove,
        onModeClick = onModeClick,
        modifier = modifier,
    )
}

/** One surah: its name, the download controls, the recitation mode, and Play on the edge. */
@Composable
fun SurahScreen(
    uiState: WearSurahUiState,
    onPlay: () -> Unit,
    onDownload: () -> Unit,
    onRemove: () -> Unit,
    onModeClick: () -> Unit,
    modifier: Modifier = Modifier,
    listState: TransformingLazyColumnState = rememberTransformingLazyColumnState(),
) {
    val surah = uiState.surah
    // Tapping "Downloaded" asks before deleting: removal is the one destructive action here.
    var confirmingRemove by remember { mutableStateOf(false) }

    ScreenScaffold(
        scrollState = listState,
        modifier = modifier,
        edgeButton = {
            if (surah != null) {
                EdgeButton(
                    onClick = onPlay,
                    // Chain crown input through the button into the list.
                    modifier = Modifier.scrollable(
                        listState,
                        orientation = Orientation.Vertical,
                        reverseDirection = true,
                        overscrollEffect = rememberOverscrollEffect(),
                    ),
                ) {
                    Text(stringResource(R.string.play))
                }
            }
        },
    ) { contentPadding ->
        TransformingLazyColumn(
            state = listState,
            contentPadding = contentPadding,
            modifier = Modifier.fillMaxSize(),
        ) {
            if (surah == null) {
                item(key = "not-found") { NotFoundRow() }
            } else {
                item(key = "title") { TitleRow(surah) }
                item(key = "download") {
                    DownloadRow(
                        download = uiState.download,
                        onPrimary = {
                            if (uiState.download is SurahDownloadState.Downloaded) {
                                confirmingRemove = true
                            } else {
                                onDownload()
                            }
                        },
                    )
                }
                item(key = "mode") { ModeRow(uiState.mode.label, onModeClick) }
            }
        }
    }

    if (confirmingRemove && surah != null) {
        AlertDialog(
            visible = true,
            onDismissRequest = { confirmingRemove = false },
            title = { Text(stringResource(R.string.remove_download_title)) },
            text = { Text(stringResource(R.string.remove_download_text, surah.nameEnglish)) },
            confirmButton = {
                Button(
                    onClick = {
                        confirmingRemove = false
                        onRemove()
                    },
                    icon = { Icon(Icons.Filled.DownloadDone, contentDescription = null) },
                ) {
                    Text(stringResource(R.string.remove))
                }
            },
        )
    }
}

@Composable
private fun TransformingLazyColumnItemScope.NotFoundRow() {
    Text(
        text = stringResource(R.string.surah_not_found),
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth(),
    )
}

/** The surah's own name leads, in the mushaf's hand: it is what a reader recognizes first. */
@Composable
private fun TransformingLazyColumnItemScope.TitleRow(surah: Surah) {
    val transformationSpec = rememberTransformationSpec()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .transformedHeight(this, transformationSpec),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = surah.nameArabicShort,
            style = QandeelTheme.arabic.watchTitle,
            color = QandeelTheme.colors.arabicText,
            textAlign = TextAlign.Center,
        )
        Text(
            text = stringResource(R.string.surah_label, surah.number, surah.nameEnglish),
            style = MaterialTheme.typography.titleSmall,
            textAlign = TextAlign.Center,
        )
        Text(
            text = stringResource(R.string.ayah_count, surah.ayahCount),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

/**
 * The download button's label follows the state; the enabled "Downloaded" variant is deliberate —
 * its tap offers removal.
 */
@Composable
private fun TransformingLazyColumnItemScope.DownloadRow(download: SurahDownloadState, onPrimary: () -> Unit) {
    val transformationSpec = rememberTransformationSpec()
    FilledTonalButton(
        onClick = onPrimary,
        modifier = rowModifier(ButtonDefaults.minimumVerticalListContentPadding, transformationSpec),
        transformation = SurfaceTransformation(transformationSpec),
        enabled = download !is SurahDownloadState.Downloading,
        icon = { Icon(downloadIcon(download), contentDescription = null) },
    ) {
        Text(downloadLabel(download))
    }
}

@Composable
private fun TransformingLazyColumnItemScope.ModeRow(modeLabel: String, onModeClick: () -> Unit) {
    val transformationSpec = rememberTransformationSpec()
    FilledTonalButton(
        onClick = onModeClick,
        modifier = rowModifier(ButtonDefaults.minimumVerticalListContentPadding, transformationSpec),
        transformation = SurfaceTransformation(transformationSpec),
        icon = { Icon(Icons.Filled.Audiotrack, contentDescription = null) },
    ) {
        Text(stringResource(R.string.recitation, modeLabel))
    }
}

private fun TransformingLazyColumnItemScope.rowModifier(
    listContentPadding: Dp,
    transformationSpec: TransformationSpec,
): Modifier = Modifier
    .minimumVerticalContentPadding(listContentPadding)
    .fillMaxWidth()
    .transformedHeight(this, transformationSpec)

private fun downloadIcon(download: SurahDownloadState) = when (download) {
    SurahDownloadState.Downloaded -> Icons.Filled.DownloadDone
    else -> Icons.Filled.Download
}

@Composable
private fun downloadLabel(download: SurahDownloadState): String = when (download) {
    SurahDownloadState.NotDownloaded -> stringResource(R.string.download)

    is SurahDownloadState.Downloading ->
        stringResource(R.string.downloading, progressLabel(download.progress))

    SurahDownloadState.Downloaded -> stringResource(R.string.download_state_downloaded)

    is SurahDownloadState.Failed -> stringResource(R.string.retry_download)
}

@Composable
private fun progressLabel(progress: Float): String =
    stringResource(R.string.download_progress, (progress * 100).roundToInt())
