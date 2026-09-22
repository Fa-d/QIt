package dev.sadakat.qit.wear.presentation.juz

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
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.SurfaceTransformation
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.lazy.rememberTransformationSpec
import androidx.wear.compose.material3.lazy.transformedHeight
import dev.sadakat.qit.core.domain.model.AyahRef
import dev.sadakat.qit.wear.R
import dev.sadakat.qit.wear.presentation.ScreenHeader

@Composable
fun JuzRoute(onJuzClick: (AyahRef) -> Unit, modifier: Modifier = Modifier, viewModel: JuzViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    JuzScreen(uiState = uiState, onJuzClick = onJuzClick, modifier = modifier)
}

/** The thirty juz; a tap opens the surah the juz starts in, at its first ayah. */
@Composable
fun JuzScreen(uiState: WearJuzUiState, onJuzClick: (AyahRef) -> Unit, modifier: Modifier = Modifier) {
    val listState = rememberTransformingLazyColumnState()
    ScreenScaffold(scrollState = listState, modifier = modifier) { contentPadding ->
        TransformingLazyColumn(
            state = listState,
            contentPadding = contentPadding,
            modifier = Modifier.fillMaxSize(),
        ) {
            item(key = "title") { ScreenHeader(stringResource(R.string.juz_title)) }
            items(uiState.rows, key = { it.juz }) { row ->
                JuzRow(row, onJuzClick)
            }
        }
    }
}

@Composable
private fun TransformingLazyColumnItemScope.JuzRow(row: JuzRowUiModel, onJuzClick: (AyahRef) -> Unit) {
    val transformationSpec = rememberTransformationSpec()
    FilledTonalButton(
        onClick = { onJuzClick(row.start) },
        modifier = Modifier
            .minimumVerticalContentPadding(ButtonDefaults.minimumVerticalListContentPadding)
            .fillMaxWidth()
            .transformedHeight(this, transformationSpec),
        transformation = SurfaceTransformation(transformationSpec),
        secondaryLabel = {
            val name = row.surahName
                ?: stringResource(R.string.surah_fallback_name, row.start.surah)
            Text(stringResource(R.string.juz_start, name, row.start.surah, row.start.ayah))
        },
    ) {
        Text(stringResource(R.string.juz_label, row.juz))
    }
}
