package dev.sadakat.qit.presentation.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FormatSize
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.sadakat.qit.R
import dev.sadakat.qit.core.designsystem.QItTheme

/** Connects [HomeScreen] to its [HomeViewModel]. */
@Composable
fun HomeRoute(
    onOpenReader: (surah: Int, ayah: Int) -> Unit,
    onOpenProgress: () -> Unit,
    onOpenReadingSettings: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    HomeScreen(
        state = state,
        onQueryChange = viewModel::onQueryChange,
        onBrowseChange = viewModel::onBrowseChange,
        onOpenReader = onOpenReader,
        onOpenProgress = onOpenProgress,
        onContinuePlayPause = viewModel::onContinuePlayPause,
        onOpenReadingSettings = onOpenReadingSettings,
        modifier = modifier,
    )
}

/**
 * Home: the continue card first (the most common thing to do), then search — by name, number or a
 * verse reference — and the surahs or the juz. The large title collapses as the list scrolls.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    state: HomeUiState,
    onQueryChange: (String) -> Unit,
    onBrowseChange: (BrowseMode) -> Unit,
    onOpenReader: (surah: Int, ayah: Int) -> Unit,
    onOpenProgress: () -> Unit,
    onContinuePlayPause: () -> Unit,
    onOpenReadingSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    Column(
        modifier = modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
    ) {
        LargeTopAppBar(
            title = { Text(stringResource(R.string.home_title)) },
            actions = {
                IconButton(onClick = onOpenProgress) {
                    Icon(
                        Icons.Rounded.Insights,
                        contentDescription = stringResource(R.string.home_cd_progress),
                    )
                }
                IconButton(onClick = onOpenReadingSettings) {
                    Icon(
                        Icons.Rounded.FormatSize,
                        contentDescription = stringResource(R.string.home_cd_reading_settings),
                    )
                }
            },
            // Inset paddings come from the app scaffold; don't apply them twice.
            windowInsets = WindowInsets(0, 0, 0, 0),
            scrollBehavior = scrollBehavior,
        )
        when {
            state.loadFailed -> Message(stringResource(R.string.load_error), isError = true)

            state.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }

            else -> LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("home_list"),
                contentPadding = PaddingValues(bottom = QItTheme.spacing.lg),
            ) {
                homeItems(state, onQueryChange, onBrowseChange, onOpenReader, onContinuePlayPause)
            }
        }
    }
}

private fun LazyListScope.homeItems(
    state: HomeUiState,
    onQueryChange: (String) -> Unit,
    onBrowseChange: (BrowseMode) -> Unit,
    onOpenReader: (surah: Int, ayah: Int) -> Unit,
    onContinuePlayPause: () -> Unit,
) {
    state.continueListening?.takeUnless { state.isSearching }?.let { card ->
        item(key = "continue") {
            ContinueListeningCard(
                item = card,
                onOpen = { onOpenReader(card.surah, card.ayah) },
                onPlayPause = onContinuePlayPause,
                modifier = Modifier.animateItem(),
            )
        }
    }
    item(key = "search") {
        HomeSearchField(query = state.query, onQueryChange = onQueryChange)
    }
    if (!state.isSearching) {
        item(key = "browse") {
            BrowseToggle(selected = state.browse, onSelect = onBrowseChange)
        }
    }
    state.jumpTarget?.let { jump ->
        item(key = "jump") {
            JumpRow(jump, onClick = { onOpenReader(jump.ref.surah, jump.ref.ayah) }, modifier = Modifier.animateItem())
        }
    }
    if (state.isSearching || state.browse == BrowseMode.SURAH) {
        items(state.surahs, key = { "surah_${it.surah.number}" }) { row ->
            SurahRow(row, onClick = { onOpenReader(row.surah.number, 0) }, modifier = Modifier.animateItem())
        }
    } else {
        items(state.juz, key = { "juz_${it.juz}" }) { row ->
            JuzRow(row, onClick = { onOpenReader(row.start.surah, row.start.ayah) }, modifier = Modifier.animateItem())
        }
    }
    if (state.isSearching && state.surahs.isEmpty() && state.jumpTarget == null) {
        item(key = "empty") { Message(stringResource(R.string.home_no_results, state.query.trim())) }
    }
}

@Composable
private fun BrowseToggle(selected: BrowseMode, onSelect: (BrowseMode) -> Unit, modifier: Modifier = Modifier) {
    val options = listOf(BrowseMode.SURAH to R.string.home_browse_surahs, BrowseMode.JUZ to R.string.home_browse_juz)
    SingleChoiceSegmentedButtonRow(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = QItTheme.spacing.screenGutter, vertical = QItTheme.spacing.sm),
    ) {
        options.forEachIndexed { index, (mode, label) ->
            SegmentedButton(
                selected = mode == selected,
                onClick = { onSelect(mode) },
                shape = SegmentedButtonDefaults.itemShape(index, options.size),
            ) {
                Text(stringResource(label))
            }
        }
    }
}

@Composable
private fun Message(text: String, modifier: Modifier = Modifier, isError: Boolean = false) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyLarge,
        color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.padding(QItTheme.spacing.xl),
    )
}
