package dev.sadakat.qit.presentation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import dev.sadakat.qit.presentation.navigation.QuranDestinations
import dev.sadakat.qit.presentation.player.PlayerBar
import dev.sadakat.qit.presentation.player.PlayerViewModel
import dev.sadakat.qit.presentation.reader.SurahReaderRoute
import dev.sadakat.qit.presentation.surahlist.SurahListRoute

/**
 * Root of the phone UI: navigation between the surah list and the reader, with the player bar
 * pinned to the bottom whenever something is queued.
 */
@Composable
fun QuranApp(modifier: Modifier = Modifier, playerViewModel: PlayerViewModel = hiltViewModel()) {
    val navController = rememberNavController()
    val playerState by playerViewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

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
            if (playerState.nowPlaying != null) {
                PlayerBar(
                    state = playerState,
                    onOpenReader = { surah, ayah ->
                        navController.navigate(QuranDestinations.surahReader(surah, ayah)) {
                            launchSingleTop = true
                        }
                    },
                    onPrevious = playerViewModel::previousAyah,
                    onTogglePlayPause = playerViewModel::togglePlayPause,
                    onNext = playerViewModel::nextAyah,
                    onStop = playerViewModel::stop,
                )
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = QuranDestinations.SURAH_LIST,
            modifier = Modifier.padding(padding),
        ) {
            composable(QuranDestinations.SURAH_LIST) {
                SurahListRoute(
                    onSurahClick = { surah ->
                        navController.navigate(QuranDestinations.surahReader(surah))
                    },
                    onContinueListening = { surah, ayah ->
                        navController.navigate(QuranDestinations.surahReader(surah, ayah)) {
                            launchSingleTop = true
                        }
                    },
                )
            }
            composable(
                route = QuranDestinations.SURAH_READER_PATTERN,
                arguments = QuranDestinations.surahReaderArguments,
            ) {
                SurahReaderRoute(onBack = { navController.popBackStack() }, onOpenReadingSettings = {})
            }
        }
    }
}
