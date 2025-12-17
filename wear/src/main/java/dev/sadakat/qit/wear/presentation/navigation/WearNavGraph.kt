package dev.sadakat.qit.wear.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.navigation.SwipeDismissableNavHost
import androidx.wear.compose.navigation.composable
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController
import dev.sadakat.qit.wear.presentation.screens.DownloadsScreen
import dev.sadakat.qit.wear.presentation.screens.MusicLibraryScreen
import dev.sadakat.qit.wear.presentation.screens.NowPlayingScreen
import dev.sadakat.qit.wear.presentation.viewmodel.PlaybackViewModel

/**
 * Simplified navigation for Wear OS
 * - Now Playing: Start screen (primary function)
 * - Music Library: Browse all songs in flat list
 * - Downloads: Manage offline songs
 */
sealed class WearScreen(val route: String) {
    object NowPlaying : WearScreen("now_playing")
    object MusicLibrary : WearScreen("music_library")
    object Downloads : WearScreen("downloads")
}

@Composable
fun WearNavGraph() {
    val navController = rememberSwipeDismissableNavController()

    MaterialTheme {
        SwipeDismissableNavHost(
            navController = navController,
            startDestination = WearScreen.NowPlaying.route  // Start with Now Playing
        ) {
            // Now Playing screen - Primary function
            composable(WearScreen.NowPlaying.route) {
                val playbackViewModel: PlaybackViewModel = hiltViewModel()

                NowPlayingScreen(
                    viewModel = playbackViewModel,
                    onBrowseMusicClick = {
                        navController.navigate(WearScreen.MusicLibrary.route)
                    },
                    onDownloadsClick = {
                        navController.navigate(WearScreen.Downloads.route)
                    }
                )
            }

            // Music Library - Browse all songs in flat list
            composable(WearScreen.MusicLibrary.route) {
                val playbackViewModel: PlaybackViewModel = hiltViewModel()

                MusicLibraryScreen(
                    onSongClick = { songId ->
                        // Play song and navigate back to Now Playing
                        playbackViewModel.playSong(songId.value)
                        navController.navigate(WearScreen.NowPlaying.route) {
                            // Clear back stack to avoid deep navigation
                            popUpTo(WearScreen.NowPlaying.route) { inclusive = true }
                        }
                    }
                )
            }

            // Downloads - Manage offline songs
            composable(WearScreen.Downloads.route) {
                DownloadsScreen(
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }
        }
    }
}
