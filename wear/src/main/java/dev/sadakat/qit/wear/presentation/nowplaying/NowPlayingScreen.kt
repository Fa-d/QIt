package dev.sadakat.qit.wear.presentation.nowplaying

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Tune
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.rotary.onRotaryScrollEvent
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.foundation.pager.HorizontalPager
import androidx.wear.compose.foundation.pager.PagerDefaults
import androidx.wear.compose.foundation.pager.rememberPagerState
import androidx.wear.compose.material3.AnimatedPage
import androidx.wear.compose.material3.CircularProgressIndicator
import androidx.wear.compose.material3.FilledIconButton
import androidx.wear.compose.material3.HorizontalPagerScaffold
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.IconButtonDefaults
import androidx.wear.compose.material3.LevelIndicator
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.PagerScaffoldDefaults
import androidx.wear.compose.material3.ProgressIndicatorDefaults
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.touchTargetAwareSize
import dev.sadakat.qit.core.designsystem.QItTheme
import dev.sadakat.qit.core.designsystem.component.WearTokens
import dev.sadakat.qit.core.domain.player.PlaybackError
import dev.sadakat.qit.wear.R
import kotlinx.coroutines.delay

private const val PAGES = 2
private const val CONTROLS_PAGE = 0

/** How long the volume level stays on screen after the crown stops turning. */
private const val VOLUME_HINT_MS = 2_000L

@Composable
fun NowPlayingRoute(
    onOpenOptions: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: NowPlayingViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    NowPlayingScreen(
        uiState = uiState,
        onPrevious = viewModel::previousAyah,
        onTogglePlayPause = viewModel::togglePlayPause,
        onNext = viewModel::nextAyah,
        onVolumeSteps = viewModel::adjustVolume,
        onOpenOptions = onOpenOptions,
        modifier = modifier,
    )
}

/**
 * Now playing in two pages: controls with the ayah-progress ring (the crown sets the volume), then
 * the current ayah's Arabic and translation (the crown scrolls).
 */
@Composable
fun NowPlayingScreen(
    uiState: WearNowPlayingUiState,
    onPrevious: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onNext: () -> Unit,
    onVolumeSteps: (Int) -> Unit,
    onOpenOptions: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val pagerState = rememberPagerState(pageCount = { PAGES })
    HorizontalPagerScaffold(pagerState = pagerState, modifier = modifier) {
        HorizontalPager(
            state = pagerState,
            flingBehavior = PagerDefaults.snapFlingBehavior(
                state = pagerState,
                snapPositionalThreshold = PagerScaffoldDefaults.HighSnapPositionalThreshold,
            ),
        ) { page ->
            AnimatedPage(pageIndex = page, pagerState = pagerState) {
                if (page == CONTROLS_PAGE) {
                    ControlsPage(
                        uiState = uiState,
                        onPrevious = onPrevious,
                        onTogglePlayPause = onTogglePlayPause,
                        onNext = onNext,
                        onVolumeSteps = onVolumeSteps,
                        onOpenOptions = onOpenOptions,
                    )
                } else {
                    TextPage(uiState = uiState)
                }
            }
        }
    }
}

/** The transport page: the progress ring around the surah, position, controls and volume. */
@Composable
private fun ControlsPage(
    uiState: WearNowPlayingUiState,
    onPrevious: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onNext: () -> Unit,
    onVolumeSteps: (Int) -> Unit,
    onOpenOptions: () -> Unit,
) {
    ScreenScaffold {
        val focusRequester = remember { FocusRequester() }
        val crownTravel = remember { mutableFloatStateOf(0f) }
        var volumeSeen by remember { mutableStateOf(false) }
        var volumeHint by remember { mutableStateOf(false) }

        // The hint appears while the crown turns and fades once it has been idle a moment; the
        // first run just records the starting volume so opening the screen shows nothing.
        LaunchedEffect(uiState.volume) {
            if (!volumeSeen) {
                volumeSeen = true
                return@LaunchedEffect
            }
            volumeHint = true
            delay(VOLUME_HINT_MS)
            volumeHint = false
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .onRotaryScrollEvent { event ->
                    crownTravel.floatValue += event.verticalScrollPixels
                    val steps = (crownTravel.floatValue / WearTokens.VOLUME_CROWN_PIXELS_PER_STEP).toInt()
                    if (steps != 0) {
                        crownTravel.floatValue -= steps * WearTokens.VOLUME_CROWN_PIXELS_PER_STEP
                        onVolumeSteps(steps)
                    }
                    true
                }
                .focusRequester(focusRequester)
                .focusable(),
        ) {
            CircularProgressIndicator(
                progress = { uiState.progress },
                modifier = Modifier.fillMaxSize(),
                strokeWidth = WearTokens.ProgressRingStroke,
                colors = ProgressIndicatorDefaults.colors(trackColor = QItTheme.colors.progressTrack),
            )
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                val surahNumber = uiState.surahNumber
                if (surahNumber == null) {
                    Text(
                        text = stringResource(R.string.nothing_playing),
                        style = MaterialTheme.typography.titleSmall,
                        textAlign = TextAlign.Center,
                    )
                } else {
                    Text(
                        text = uiState.surahName.orEmpty(),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = if (uiState.ayah == 0) {
                            stringResource(R.string.bismillah)
                        } else {
                            stringResource(R.string.ayah_position, surahNumber, uiState.ayah)
                        },
                        // Compact enough that the column fits under the clock on small round screens.
                        style = MaterialTheme.typography.titleLarge,
                    )
                    if (uiState.isBuffering) {
                        CircularProgressIndicator(
                            strokeWidth = QItTheme.sizes.strokeThin,
                            modifier = Modifier.padding(vertical = QItTheme.spacing.xs),
                        )
                    }
                    uiState.error?.let { error ->
                        Text(
                            text = stringResource(
                                when (error) {
                                    PlaybackError.NETWORK -> R.string.playback_error_network
                                    PlaybackError.FAILED -> R.string.playback_error_failed
                                },
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = QItTheme.spacing.xl),
                        )
                    }
                    ControlsRow(
                        isPlaying = uiState.isPlaying,
                        onPrevious = onPrevious,
                        onTogglePlayPause = onTogglePlayPause,
                        onNext = onNext,
                    )
                    FilledIconButton(
                        onClick = onOpenOptions,
                        modifier = Modifier
                            .padding(top = QItTheme.spacing.xs)
                            .touchTargetAwareSize(IconButtonDefaults.SmallButtonSize),
                    ) {
                        Icon(Icons.Filled.Tune, contentDescription = stringResource(R.string.cd_options))
                    }
                }
            }
            if (volumeHint) {
                LevelIndicator(
                    value = { uiState.volume },
                    modifier = Modifier.align(Alignment.CenterStart),
                )
            }
        }

        // Rotary input reaches the volume only while this page holds focus.
        LaunchedEffect(Unit) { focusRequester.requestFocus() }
    }
}

@Composable
private fun ControlsRow(
    isPlaying: Boolean,
    onPrevious: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onNext: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(QItTheme.spacing.sm),
    ) {
        val sideSize = Modifier.touchTargetAwareSize(IconButtonDefaults.DefaultButtonSize)
        FilledIconButton(onClick = onPrevious, modifier = sideSize) {
            Icon(Icons.Filled.SkipPrevious, contentDescription = stringResource(R.string.previous_ayah))
        }
        FilledIconButton(
            onClick = onTogglePlayPause,
            modifier = Modifier.touchTargetAwareSize(IconButtonDefaults.LargeButtonSize),
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                contentDescription = stringResource(
                    if (isPlaying) R.string.pause else R.string.play_pause,
                ),
            )
        }
        FilledIconButton(onClick = onNext, modifier = sideSize) {
            Icon(Icons.Filled.SkipNext, contentDescription = stringResource(R.string.next_ayah))
        }
    }
}

/** The text page: the whole Arabic ayah and its translation, scrolled by the crown. */
@Composable
internal fun TextPage(uiState: WearNowPlayingUiState) {
    val listState = rememberTransformingLazyColumnState()
    ScreenScaffold(scrollState = listState) { contentPadding ->
        TransformingLazyColumn(
            state = listState,
            contentPadding = contentPadding,
            modifier = Modifier.fillMaxSize(),
        ) {
            if (uiState.surahNumber == null) {
                item(key = "nothing") {
                    Text(
                        text = stringResource(R.string.nothing_playing),
                        style = MaterialTheme.typography.titleSmall,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            } else {
                item(key = "arabic") {
                    // No maxLines here: the whole ayah, however long, reads in place.
                    Text(
                        text = uiState.ayahText ?: stringResource(R.string.bismillah_arabic),
                        style = QItTheme.arabic.watchBody,
                        color = QItTheme.colors.arabicText,
                        textAlign = TextAlign.Center,
                        // Inset from the round edge, where the first lines would otherwise clip.
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = QItTheme.spacing.xl),
                    )
                }
                uiState.translation?.let { translation ->
                    item(key = "translation") {
                        Text(
                            text = translation,
                            style = MaterialTheme.typography.bodySmall,
                            color = QItTheme.colors.translationText,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = QItTheme.spacing.xl, vertical = QItTheme.spacing.sm),
                        )
                    }
                }
            }
        }
    }
}
