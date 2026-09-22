package dev.sadakat.qit.wear.presentation.mode

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
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.RadioButton
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.SurfaceTransformation
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.lazy.rememberTransformationSpec
import androidx.wear.compose.material3.lazy.transformedHeight
import dev.sadakat.qit.core.domain.model.RecitationMode
import dev.sadakat.qit.wear.R

@Composable
fun ModeRoute(onDismiss: () -> Unit, modifier: Modifier = Modifier, viewModel: ModeViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    ModeScreen(
        uiState = uiState,
        onSelect = {
            viewModel.select(it)
            onDismiss()
        },
        modifier = modifier,
    )
}

/** One radio button per recitation mode; selecting persists it and returns. */
@Composable
fun ModeScreen(uiState: WearModeUiState, onSelect: (RecitationMode) -> Unit, modifier: Modifier = Modifier) {
    val listState = rememberTransformingLazyColumnState()
    ScreenScaffold(scrollState = listState, modifier = modifier) { contentPadding ->
        TransformingLazyColumn(
            state = listState,
            contentPadding = contentPadding,
            modifier = Modifier.fillMaxSize(),
        ) {
            item(key = "header") { HeaderRow() }
            items(RecitationMode.entries, key = { it }) { mode ->
                val transformationSpec = rememberTransformationSpec()
                RadioButton(
                    selected = mode == uiState.mode,
                    onSelect = { onSelect(mode) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .transformedHeight(this, transformationSpec),
                    transformation = SurfaceTransformation(transformationSpec),
                ) {
                    Text(mode.label)
                }
            }
        }
    }
}

@Composable
private fun TransformingLazyColumnItemScope.HeaderRow() {
    val transformationSpec = rememberTransformationSpec()
    ListHeader(
        modifier = Modifier
            .fillMaxWidth()
            .transformedHeight(this, transformationSpec),
        transformation = SurfaceTransformation(transformationSpec),
    ) {
        Text(stringResource(R.string.recitation_header))
    }
}
