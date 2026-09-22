package dev.sadakat.qit.presentation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import dev.sadakat.qit.core.designsystem.QItTheme
import dev.sadakat.qit.presentation.home.HomeRoute
import dev.sadakat.qit.presentation.navigation.HomeDestination
import dev.sadakat.qit.presentation.navigation.ReaderDestination
import dev.sadakat.qit.presentation.player.MiniPlayer
import dev.sadakat.qit.presentation.player.NowPlayingActions
import dev.sadakat.qit.presentation.player.NowPlayingSheet
import dev.sadakat.qit.presentation.player.PlayerViewModel
import dev.sadakat.qit.presentation.reader.SurahReaderRoute
import dev.sadakat.qit.presentation.settings.ReadingSettingsSheet

/**
 * Root of the phone UI: home and the reader, the mini player pinned under both whenever something
 * is queued, the full player sliding up from it, and the reading settings sheet.
 */
@Composable
fun QuranApp(modifier: Modifier = Modifier, playerViewModel: PlayerViewModel = hiltViewModel()) {
    val navController = rememberNavController()
    val playerState by playerViewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showNowPlaying by rememberSaveable { mutableStateOf(false) }
    var showReadingSettings by rememberSaveable { mutableStateOf(false) }
    val openReader: (Int, Int) -> Unit = { surah, ayah ->
        navController.navigate(ReaderDestination(surah, ayah)) { launchSingleTop = true }
    }

    // Playback errors (no network for a streaming surah, ...) surface once, then are consumed.
    LaunchedEffect(playerState.error) {
        playerState.error?.let {
            snackbarHostState.showSnackbar(it)
            playerViewModel.consumeError()
        }
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            AnimatedVisibility(
                visible = playerState.nowPlaying != null,
                enter = expandVertically(QItTheme.motion.enter()),
                exit = shrinkVertically(QItTheme.motion.exit()),
            ) {
                MiniPlayer(
                    state = playerState,
                    onExpand = { showNowPlaying = true },
                    onTogglePlayPause = playerViewModel::togglePlayPause,
                    onNext = playerViewModel::nextAyah,
                )
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = HomeDestination,
            modifier = Modifier.padding(padding),
        ) {
            composable<HomeDestination> {
                HomeRoute(onOpenReader = openReader, onOpenReadingSettings = { showReadingSettings = true })
            }
            composable<ReaderDestination> {
                SurahReaderRoute(
                    onBack = { navController.popBackStack() },
                    onOpenReadingSettings = { showReadingSettings = true },
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
                onSeekToAyah = playerViewModel::seekToAyah,
                onModeChange = playerViewModel::setMode,
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
        )
    }
    if (showReadingSettings) {
        ReadingSettingsSheet(onDismiss = { showReadingSettings = false })
    }
}
