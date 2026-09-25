package dev.sadakat.qit.presentation.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FormatSize
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import dev.sadakat.qit.core.ui.kit.QItScaffold
import dev.sadakat.qit.core.ui.kit.QItSearchField
import dev.sadakat.qit.core.ui.kit.QItSegmentedToggle
import dev.sadakat.qit.core.ui.kit.QItTopBar
import dev.sadakat.qit.core.ui.kit.QItTopBarScrollKind
import dev.sadakat.qit.core.ui.kit.QItTopBarSize
import dev.sadakat.qit.core.ui.kit.rememberQItTopBarScroll
import dev.sadakat.qit.presentation.components.LoadError

/** Connects [HomeScreen] to its [HomeViewModel]. */
@Composable
fun HomeRoute(
    onOpenReader: (surah: Int, ayah: Int) -> Unit,
    onOpenProgress: () -> Unit,
    onOpenReadingSettings: () -> Unit,
    onOpenAppearance: () -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
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
        onOpenAppearance = onOpenAppearance,
        onRetry = viewModel::retry,
        contentPadding = contentPadding,
        modifier = modifier,
    )
}

/**
 * Home: the continue card first (the most common thing to do), then search — by name, number or a
 * verse reference — and the surahs or the juz. The large title collapses as the list scrolls.
 */
@Composable
fun HomeScreen(
    state: HomeUiState,
    onQueryChange: (String) -> Unit,
    onBrowseChange: (BrowseMode) -> Unit,
    onOpenReader: (surah: Int, ayah: Int) -> Unit,
    onOpenProgress: () -> Unit,
    onContinuePlayPause: () -> Unit,
    onOpenReadingSettings: () -> Unit,
    onOpenAppearance: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
) {
    val scroll = rememberQItTopBarScroll(QItTopBarScrollKind.COLLAPSE_ON_SCROLL)
    QItScaffold(
        topBar = {
            QItTopBar(
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
                    IconButton(onClick = onOpenAppearance) {
                        Icon(
                            Icons.Rounded.Palette,
                            contentDescription = stringResource(R.string.home_cd_appearance),
                        )
                    }
                },
                size = QItTopBarSize.LARGE,
                scroll = scroll,
            )
        },
        modifier = modifier.nestedScroll(scroll.nestedScrollConnection),
        contentPadding = contentPadding,
    ) { padding ->
        when {
            state.loadFailed -> LoadError(
                message = stringResource(R.string.load_error),
                onRetry = onRetry,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = padding.calculateTopPadding()),
            )

            state.isLoading -> Box(
                Modifier
                    .fillMaxSize()
                    .padding(top = padding.calculateTopPadding()),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }

            else -> LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("home_list"),
                // The list scrolls behind the top bar and the mini player; only its items keep clear.
                contentPadding = PaddingValues(
                    top = padding.calculateTopPadding(),
                    bottom = padding.calculateBottomPadding() + QItTheme.spacing.lg,
                ),
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
        QItSearchField(
            query = state.query,
            onQueryChange = onQueryChange,
            placeholder = stringResource(R.string.home_search_hint),
            clearLabel = stringResource(R.string.home_cd_clear_search),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = QItTheme.spacing.screenGutter, vertical = QItTheme.spacing.sm)
                .testTag("home_search"),
        )
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
        item(key = "empty") { EmptySearch(query = state.query, onQueryChange = onQueryChange) }
    }
}

@Composable
private fun BrowseToggle(selected: BrowseMode, onSelect: (BrowseMode) -> Unit, modifier: Modifier = Modifier) {
    val labels = mapOf(
        BrowseMode.SURAH to R.string.home_browse_surahs,
        BrowseMode.JUZ to R.string.home_browse_juz,
    )
    QItSegmentedToggle(
        options = labels.keys.toList(),
        selected = selected,
        onSelect = onSelect,
        label = { mode -> stringResource(labels.getValue(mode)) },
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = QItTheme.spacing.screenGutter, vertical = QItTheme.spacing.sm),
    )
}

/** No match: teach the search syntax, with examples that fill the field when tapped. */
@Composable
private fun EmptySearch(query: String, onQueryChange: (String) -> Unit, modifier: Modifier = Modifier) {
    val examples = listOf(
        stringResource(R.string.home_search_example_name),
        stringResource(R.string.home_search_example_number),
        stringResource(R.string.home_search_example_verse),
    )
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = QItTheme.spacing.screenGutter, vertical = QItTheme.spacing.xl),
    ) {
        Text(
            text = stringResource(R.string.home_no_results, query.trim()),
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            text = stringResource(R.string.home_no_results_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = QItTheme.spacing.xs),
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(QItTheme.spacing.sm),
            modifier = Modifier.padding(top = QItTheme.spacing.md),
        ) {
            examples.forEach { example ->
                AssistChip(onClick = { onQueryChange(example) }, label = { Text(example) })
            }
        }
    }
}
