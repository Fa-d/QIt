package dev.sadakat.qit.presentation.progress

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.sadakat.qit.R
import dev.sadakat.qit.core.designsystem.QItTheme
import dev.sadakat.qit.core.domain.model.ListeningOrder
import dev.sadakat.qit.core.domain.model.QuranMeta
import dev.sadakat.qit.core.ui.kit.QItAlertDialog
import dev.sadakat.qit.core.ui.kit.QItCard
import dev.sadakat.qit.core.ui.kit.QItMenu
import dev.sadakat.qit.core.ui.kit.QItScaffold
import dev.sadakat.qit.core.ui.kit.QItSegmentedToggle
import dev.sadakat.qit.core.ui.kit.QItTopBar
import java.util.Locale
import kotlin.math.roundToInt

/** Connects [ProgressScreen] to its [ProgressViewModel]. */
@Composable
fun ProgressRoute(
    onBack: () -> Unit,
    onOpenReader: (surah: Int, ayah: Int) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
    viewModel: ProgressViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    ProgressScreen(
        state = state,
        onBack = onBack,
        onOrderChange = viewModel::setOrder,
        onReset = viewModel::reset,
        onOpenReader = onOpenReader,
        modifier = modifier,
        contentPadding = contentPadding,
    )
}

/** The listening progress: how much of the Quran has been heard, then each surah heard and how often. */
@Composable
fun ProgressScreen(
    state: ProgressUiState,
    onBack: () -> Unit,
    onOrderChange: (ListeningOrder) -> Unit,
    onReset: () -> Unit,
    onOpenReader: (surah: Int, ayah: Int) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
) {
    var resetPending by remember { mutableStateOf(false) }
    if (resetPending) {
        ResetProgressDialog(
            onConfirm = {
                resetPending = false
                onReset()
            },
            onDismiss = { resetPending = false },
        )
    }

    QItScaffold(
        topBar = { ProgressTopBar(showMenu = !state.isEmpty, onBack = onBack, onResetClick = { resetPending = true }) },
        modifier = modifier,
        contentPadding = contentPadding,
    ) { padding ->
        when {
            state.isLoading -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }

            state.isEmpty -> ProgressEmpty(Modifier.fillMaxSize().padding(padding))

            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                // The list scrolls behind the top bar and the mini player; only its items keep clear.
                contentPadding = PaddingValues(
                    top = padding.calculateTopPadding(),
                    bottom = padding.calculateBottomPadding() + QItTheme.spacing.lg,
                ),
            ) {
                item(key = "summary") { ProgressSummary(state) }
                item(key = "order") { OrderToggle(selected = state.order, onSelect = onOrderChange) }
                items(state.rows, key = { "progress_surah_${it.surah}" }) { row ->
                    ProgressRow(row, onClick = { onOpenReader(row.surah, 0) })
                }
            }
        }
    }
}

@Composable
private fun ProgressTopBar(showMenu: Boolean, onBack: () -> Unit, onResetClick: () -> Unit) {
    QItTopBar(
        title = { Text(stringResource(R.string.progress_title)) },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = stringResource(R.string.cd_back),
                )
            }
        },
        actions = { if (showMenu) ProgressOverflowMenu(onResetClick = onResetClick) },
    )
}

/** Ayahs heard of the whole Quran, the coverage line and how long was listened in total. */
@Composable
private fun ProgressSummary(state: ProgressUiState, modifier: Modifier = Modifier) {
    QItCard(
        shape = RoundedCornerShape(QItTheme.radius.lg),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = QItTheme.spacing.screenGutter, vertical = QItTheme.spacing.sm),
    ) {
        Column(Modifier.padding(QItTheme.spacing.lg)) {
            Text(
                text = numberText(state.ayahsHeard),
                style = MaterialTheme.typography.displaySmall,
            )
            Text(
                text = stringResource(R.string.progress_of_ayahs_heard, numberText(QuranMeta.TOTAL_AYAHS)),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(QItTheme.spacing.sm))
            LinearProgressIndicator(
                progress = { state.coverage },
                color = MaterialTheme.colorScheme.primary,
                trackColor = QItTheme.colors.progressTrack,
                gapSize = QItTheme.spacing.none,
                drawStopIndicator = {},
                modifier = Modifier
                    .fillMaxWidth()
                    .height(QItTheme.sizes.strokeThin),
            )
            Spacer(Modifier.height(QItTheme.spacing.xs))
            Text(
                text = stringResource(
                    R.string.progress_coverage_line,
                    percent(state.coverage),
                    listenedTimeText(state.listenedMs),
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (state.rounds >= 1) {
                Spacer(Modifier.height(QItTheme.spacing.xs))
                Text(
                    text = pluralStringResource(R.plurals.progress_whole_quran_rounds, state.rounds, state.rounds),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** How the list is ordered: last heard first, most listens first, or by surah number. */
@Composable
private fun OrderToggle(selected: ListeningOrder, onSelect: (ListeningOrder) -> Unit, modifier: Modifier = Modifier) {
    val options = listOf(
        ListeningOrder.RECENT to R.string.progress_order_recent,
        ListeningOrder.MOST_HEARD to R.string.progress_order_most_heard,
        ListeningOrder.BY_NUMBER to R.string.progress_order_by_number,
    )
    QItSegmentedToggle(
        options = options.map { it.first },
        selected = selected,
        onSelect = onSelect,
        label = { order -> stringResource(options.first { it.first == order }.second) },
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = QItTheme.spacing.screenGutter, vertical = QItTheme.spacing.sm),
    )
}

/** Nothing has been heard yet; the screen stays empty until the first ayah is played. */
@Composable
private fun ProgressEmpty(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(QItTheme.spacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Rounded.Headphones,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(QItTheme.sizes.iconLarge),
        )
        Spacer(Modifier.height(QItTheme.spacing.sm))
        Text(
            text = stringResource(R.string.progress_empty_title),
            style = MaterialTheme.typography.titleMedium,
        )
        Spacer(Modifier.height(QItTheme.spacing.xs))
        Text(
            text = stringResource(R.string.progress_empty_text),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

/** The only action here, behind the overflow: forgetting everything, after asking. */
@Composable
private fun ProgressOverflowMenu(onResetClick: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(
                imageVector = Icons.Rounded.MoreVert,
                contentDescription = stringResource(R.string.cd_more_options),
            )
        }
        QItMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.progress_reset)) },
                onClick = {
                    expanded = false
                    onResetClick()
                },
            )
        }
    }
}

@Composable
private fun ResetProgressDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    QItAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.progress_reset_title)) },
        text = { Text(stringResource(R.string.progress_reset_text)) },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text(stringResource(R.string.progress_reset_confirm)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
}

/** "1,204" — thousands grouped the way the reader's locale writes them. */
internal fun numberText(n: Int): String = String.format(Locale.getDefault(), "%,d", n)

/** "14 h 5 min", "35 min", or "Under a minute". */
@Composable
internal fun listenedTimeText(ms: Long): String {
    val hours = ms / MS_PER_HOUR
    val minutes = (ms % MS_PER_HOUR) / MS_PER_MINUTE
    return when {
        hours >= 1 -> stringResource(R.string.progress_time_hours_minutes, hours, minutes)
        minutes >= 1 -> stringResource(R.string.progress_time_minutes, minutes)
        else -> stringResource(R.string.progress_time_under_minute)
    }
}

internal fun percent(fraction: Float): Int = (fraction * PERCENT).roundToInt()

private const val PERCENT = 100
private const val MS_PER_HOUR = 3_600_000L
private const val MS_PER_MINUTE = 60_000L
