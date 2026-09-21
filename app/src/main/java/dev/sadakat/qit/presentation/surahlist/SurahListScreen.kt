package dev.sadakat.qit.presentation.surahlist

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.sadakat.qit.R
import dev.sadakat.qit.core.domain.model.Revelation
import dev.sadakat.qit.core.domain.model.Surah
import dev.sadakat.qit.core.domain.repository.SurahDownloadState
import dev.sadakat.qit.presentation.components.NumberBadge
import dev.sadakat.qit.presentation.components.ayahTitleText
import dev.sadakat.qit.ui.theme.AmiriQuran

/** Connects [SurahListScreen] to its [SurahListViewModel]. */
@Composable
fun SurahListRoute(
    onSurahClick: (Int) -> Unit,
    onContinueListening: (surah: Int, ayah: Int) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SurahListViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    SurahListScreen(
        state = state,
        onSearchQueryChange = viewModel::onSearchQueryChange,
        onSurahClick = onSurahClick,
        onContinueListening = {
            viewModel.continueListening()
            onContinueListening(it.surah, it.ayah)
        },
        modifier = modifier,
    )
}

/** The Quran's table of contents: search, continue-listening card and the 114 surahs. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SurahListScreen(
    state: SurahListUiState,
    onSearchQueryChange: (String) -> Unit,
    onSurahClick: (Int) -> Unit,
    onContinueListening: (ContinueListening) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text(stringResource(R.string.surah_list_title)) },
            // Inset paddings come from the app scaffold; don't apply them twice.
            windowInsets = WindowInsets(0, 0, 0, 0),
        )
        OutlinedTextField(
            value = state.query,
            onValueChange = onSearchQueryChange,
            placeholder = { Text(stringResource(R.string.surah_search_hint)) },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .testTag("surah_search"),
        )
        when {
            state.loadFailed -> LoadError()

            state.surahs.isEmpty() && state.query.isBlank() -> Loading()

            else -> {
                LazyColumn {
                    state.continueListening?.let { card ->
                        item(key = "continue") {
                            ContinueListeningCard(item = card, onClick = { onContinueListening(card) })
                        }
                    }
                    items(state.surahs, key = { it.number }) { surah ->
                        SurahRow(
                            surah = surah,
                            downloadState = state.downloadStates[surah.number]
                                ?: SurahDownloadState.NotDownloaded,
                            onClick = { onSurahClick(surah.number) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Loading() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
private fun LoadError() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = stringResource(R.string.load_error),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Composable
private fun ContinueListeningCard(item: ContinueListening, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .testTag("continue_listening"),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.continue_listening),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = ayahTitleText(item.surahName, item.surah, item.ayah),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun SurahRow(surah: Surah, downloadState: SurahDownloadState, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        NumberBadge(number = surah.number)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = surah.nameEnglish,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            val ayahsText = pluralStringResource(R.plurals.ayah_count, surah.ayahCount, surah.ayahCount)
            val revelationText = stringResource(
                if (surah.revelation == Revelation.MECCAN) R.string.revelation_meccan else R.string.revelation_medinan,
            )
            Text(
                text = stringResource(R.string.surah_subtitle, surah.meaningEnglish, ayahsText, revelationText),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.width(8.dp))
        Text(
            text = surah.nameArabic,
            fontFamily = AmiriQuran,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.width(8.dp))
        DownloadIndicator(state = downloadState)
    }
}

/** Compact download state indicator for surah rows. */
@Composable
fun DownloadIndicator(state: SurahDownloadState, modifier: Modifier = Modifier) {
    val downloadingText = stringResource(R.string.cd_downloading)
    when (state) {
        is SurahDownloadState.Downloaded -> Icon(
            imageVector = Icons.Rounded.CheckCircle,
            contentDescription = stringResource(R.string.cd_downloaded),
            tint = MaterialTheme.colorScheme.primary,
            modifier = modifier.size(20.dp),
        )

        is SurahDownloadState.Downloading -> CircularProgressIndicator(
            progress = { state.progress },
            strokeWidth = 2.dp,
            modifier = modifier
                .size(20.dp)
                .semantics { contentDescription = downloadingText },
        )

        is SurahDownloadState.Failed -> Icon(
            imageVector = Icons.Rounded.ErrorOutline,
            contentDescription = stringResource(R.string.cd_download_failed),
            tint = MaterialTheme.colorScheme.error,
            modifier = modifier.size(20.dp),
        )

        is SurahDownloadState.NotDownloaded -> Unit
    }
}
