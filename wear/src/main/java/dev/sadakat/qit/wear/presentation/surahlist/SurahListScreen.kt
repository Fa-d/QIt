package dev.sadakat.qit.wear.presentation.surahlist

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.TransformingLazyColumnItemScope
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.FilledTonalButton
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.SurfaceTransformation
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.lazy.rememberTransformationSpec
import androidx.wear.compose.material3.lazy.transformedHeight
import dev.sadakat.qit.core.domain.repository.SurahDownloadState
import dev.sadakat.qit.wear.R
import kotlin.math.roundToInt

@Composable
fun SurahListRoute(
    onSurahClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SurahListViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    SurahListScreen(uiState = uiState, onSurahClick = onSurahClick, modifier = modifier)
}

/** All 114 surahs, or only the downloaded ones, each with its offline state. */
@Composable
fun SurahListScreen(uiState: WearSurahListUiState, onSurahClick: (Int) -> Unit, modifier: Modifier = Modifier) {
    val listState = rememberTransformingLazyColumnState()
    ScreenScaffold(scrollState = listState, modifier = modifier) { contentPadding ->
        TransformingLazyColumn(
            state = listState,
            contentPadding = contentPadding,
            modifier = Modifier.fillMaxSize(),
        ) {
            if (uiState.rows.isEmpty()) {
                item(key = "empty") { EmptyRow(uiState.downloadedOnly) }
            } else {
                items(uiState.rows, key = { it.surah.number }) { row ->
                    SurahRow(row, onSurahClick)
                }
            }
        }
    }
}

@Composable
private fun TransformingLazyColumnItemScope.EmptyRow(downloadedOnly: Boolean) {
    // Only the offline filter can match zero rows; the whole list is text that always loads.
    if (downloadedOnly) {
        val transformationSpec = rememberTransformationSpec()
        ListHeader(
            modifier = Modifier
                .fillMaxWidth()
                .transformedHeight(this, transformationSpec),
            transformation = SurfaceTransformation(transformationSpec),
        ) {
            Text(stringResource(R.string.downloaded_list_empty))
        }
    }
}

@Composable
private fun TransformingLazyColumnItemScope.SurahRow(row: SurahRowUiModel, onSurahClick: (Int) -> Unit) {
    val transformationSpec = rememberTransformationSpec()
    FilledTonalButton(
        onClick = { onSurahClick(row.surah.number) },
        modifier = Modifier
            .minimumVerticalContentPadding(ButtonDefaults.minimumVerticalListContentPadding)
            .fillMaxWidth()
            .transformedHeight(this, transformationSpec),
        transformation = SurfaceTransformation(transformationSpec),
        secondaryLabel = {
            val status = downloadLabel(row.download)
            if (status == null) {
                Text(stringResource(R.string.ayah_count, row.surah.ayahCount))
            } else {
                Text(stringResource(R.string.ayah_count_secondary, row.surah.ayahCount, status))
            }
        },
    ) {
        Text(stringResource(R.string.surah_label, row.surah.number, row.surah.nameEnglish))
    }
}

/** Only downloads get a status; a surah that streams when played shows its length alone. */
@Composable
private fun downloadLabel(download: SurahDownloadState): String? = when (download) {
    SurahDownloadState.NotDownloaded -> null

    is SurahDownloadState.Downloading ->
        stringResource(R.string.download_progress, (download.progress * 100).roundToInt())

    SurahDownloadState.Downloaded -> stringResource(R.string.download_state_downloaded)

    is SurahDownloadState.Failed -> stringResource(R.string.download_state_failed)
}
