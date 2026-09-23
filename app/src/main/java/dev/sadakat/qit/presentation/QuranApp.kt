package dev.sadakat.qit.presentation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import dev.sadakat.qit.R
import dev.sadakat.qit.core.designsystem.QItTheme
import dev.sadakat.qit.core.ui.kit.QItAppShell
import dev.sadakat.qit.presentation.appearance.AppearanceRoute
import dev.sadakat.qit.presentation.home.HomeRoute
import dev.sadakat.qit.presentation.navigation.AppearanceDestination
import dev.sadakat.qit.presentation.navigation.HomeDestination
import dev.sadakat.qit.presentation.navigation.ProgressDestination
import dev.sadakat.qit.presentation.navigation.ReaderDestination
import dev.sadakat.qit.presentation.player.MiniPlayer
import dev.sadakat.qit.presentation.player.NowPlayingActions
import dev.sadakat.qit.presentation.player.NowPlayingSheet
import dev.sadakat.qit.presentation.player.PlayerViewModel
import dev.sadakat.qit.presentation.player.messageRes
import dev.sadakat.qit.presentation.progress.ProgressRoute
import dev.sadakat.qit.presentation.reader.SurahReaderRoute
import dev.sadakat.qit.presentation.settings.ReadingSettingsSheet

/**
 * Root of the phone UI: home, the reader and progress, the mini player pinned under them whenever
 * something is queued, the full player sliding up from it, and the reading settings sheet.
 */
@Composable
fun QuranApp(modifier: Modifier = Modifier, playerViewModel: PlayerViewModel = hiltViewModel()) {
    val navController = rememberNavController()
    val playerState by playerViewModel.uiState.collectAsStateWithLifecycle()
    val pointer by playerViewModel.pointer.collectAsStateWithLifecycle()
    // Read only inside lambdas, where it's drawn: the root doesn't recompose with every tick.
    val progress = playerViewModel.progress.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showNowPlaying by rememberSaveable { mutableStateOf(false) }
    var showReadingSettings by rememberSaveable { mutableStateOf(false) }
    val openAppearance = { navController.navigate(AppearanceDestination) { launchSingleTop = true } }
    val openReader: (Int, Int) -> Unit = { surah, ayah ->
        navController.navigate(ReaderDestination(surah, ayah)) { launchSingleTop = true }
    }

    // Playback errors (no network for a streaming surah, ...) surface once, then are consumed.
    val playbackError = playerState.error
    val playbackErrorMessage = playbackError?.let { stringResource(it.messageRes()) }
    val retryLabel = stringResource(R.string.playback_retry)
    LaunchedEffect(playbackError) {
        playbackError?.let {
            val result = snackbarHostState.showSnackbar(
                message = playbackErrorMessage.orEmpty(),
                actionLabel = retryLabel,
                withDismissAction = true,
            )
            if (result == SnackbarResult.ActionPerformed) playerViewModel.retry()
            playerViewModel.consumeError()
        }
    }

    QItAppShell(
        // Test tags double as resource ids, so the baseline profile journey can find the screens.
        modifier = modifier.semantics { testTagsAsResourceId = true },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            AnimatedVisibility(
                visible = playerState.nowPlaying != null,
                enter = expandVertically(QItTheme.motion.enter()),
                exit = shrinkVertically(QItTheme.motion.exit()),
            ) {
                MiniPlayer(
                    state = playerState,
                    progress = { progress.value },
                    onExpand = { showNowPlaying = true },
                    onTogglePlayPause = playerViewModel::togglePlayPause,
                    onNext = playerViewModel::nextAyah,
                )
            }
        },
    ) { contentPadding ->
        // Screens draw behind the mini player and keep their content clear of it with this padding.
        NavHost(navController = navController, startDestination = HomeDestination) {
            composable<HomeDestination> {
                HomeRoute(
                    onOpenReader = openReader,
                    onOpenProgress = {
                        navController.navigate(ProgressDestination) { launchSingleTop = true }
                    },
                    onOpenReadingSettings = { showReadingSettings = true },
                    onOpenAppearance = openAppearance,
                    contentPadding = contentPadding,
                )
            }
            composable<ReaderDestination> {
                SurahReaderRoute(
                    onBack = { navController.popBackStack() },
                    onOpenReadingSettings = { showReadingSettings = true },
                    contentPadding = contentPadding,
                )
            }
            composable<AppearanceDestination> {
                AppearanceRoute(onBack = { navController.popBackStack() }, contentPadding = contentPadding)
            }
            composable<ProgressDestination> {
                ProgressRoute(
                    onBack = { navController.popBackStack() },
                    onOpenReader = openReader,
                    contentPadding = contentPadding,
                )
            }
        }
    }

    if (showNowPlaying && playerState.nowPlaying != null) {
        NowPlayingSheet(
            state = playerState,
            actions = NowPlayingActions(
                onTogglePlayPause = playerViewModel::togglePlayPause,
                onPrevious = playerViewModel::previousAyah,
                onNext = playerViewModel::nextAyah,
                onSeek = playerViewModel::seekTo,
                onModeChange = playerViewModel::setMode,
                onVoiceChange = playerViewModel::setVoice,
                onRepeatChange = playerViewModel::setRepeat,
                onSpeedChange = playerViewModel::setSpeed,
                onSleepTimerChange = playerViewModel::setSleepTimer,
                onOpenReader = { surah, ayah ->
                    showNowPlaying = false
                    openReader(surah, ayah)
                },
                onStop = {
                    showNowPlaying = false
                    playerViewModel.stop()
                },
            ),
            onDismiss = { showNowPlaying = false },
            pointer = pointer,
            progress = { progress.value },
        )
    }
    if (showReadingSettings) {
        ReadingSettingsSheet(
            onDismiss = { showReadingSettings = false },
            onOpenAppearance = {
                showReadingSettings = false
                openAppearance()
            },
        )
    }
}
