package dev.sadakat.qit.wear.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.navigation.SwipeDismissableNavHost
import androidx.wear.compose.navigation.composable
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController
import dev.sadakat.qit.wear.presentation.screens.NowPlayingScreen
import dev.sadakat.qit.wear.presentation.screens.PlaylistListScreen
import dev.sadakat.qit.wear.presentation.screens.SongListScreen

sealed class WearScreen(val route: String) {
    object PlaylistList : WearScreen("playlist_list")
    object SongList : WearScreen("song_list/{playlistId}") {
        fun createRoute(playlistId: String) = "song_list/$playlistId"
    }
    object NowPlaying : WearScreen("now_playing")
}

@Composable
fun WearNavGraph() {
    val navController = rememberSwipeDismissableNavController()

    MaterialTheme {
        SwipeDismissableNavHost(
            navController = navController,
            startDestination = WearScreen.PlaylistList.route
        ) {
            composable(WearScreen.PlaylistList.route) {
                PlaylistListScreen(
                    onPlaylistClick = { playlistId ->
                        navController.navigate(WearScreen.SongList.createRoute(playlistId))
                    }
                )
            }

            composable(
                route = WearScreen.SongList.route,
                arguments = listOf(
                    navArgument("playlistId") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val playlistId = backStackEntry.arguments?.getString("playlistId") ?: return@composable
                SongListScreen(
                    playlistId = playlistId,
                    onSongClick = {
                        navController.navigate(WearScreen.NowPlaying.route)
                    }
                )
            }

            composable(WearScreen.NowPlaying.route) {
                NowPlayingScreen()
            }
        }
    }
}
