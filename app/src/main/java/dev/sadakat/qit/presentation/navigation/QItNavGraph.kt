package dev.sadakat.qit.presentation.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Watch
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import dev.sadakat.qit.presentation.components.CollapsiblePlayer
import dev.sadakat.qit.presentation.screens.MusicLibraryScreen
import dev.sadakat.qit.presentation.screens.PlaylistDetailScreen
import dev.sadakat.qit.presentation.screens.PlaylistListScreen
import dev.sadakat.qit.presentation.screens.WatchSyncScreen

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object MusicLibrary : Screen("music_library", "Library", Icons.Default.LibraryMusic)
    object Playlists : Screen("playlists", "Playlists", Icons.Default.PlaylistPlay)
    object PlaylistDetail : Screen("playlist_detail/{playlistId}", "Playlist", Icons.Default.PlaylistPlay) {
        fun createRoute(playlistId: String) = "playlist_detail/$playlistId"
    }
    object WatchSync : Screen("watch_sync", "Watch", Icons.Default.Watch)
}

@Composable
fun QItNavGraph() {
    val navController = rememberNavController()
    val screens = listOf(
        Screen.MusicLibrary,
        Screen.Playlists,
        Screen.WatchSync
    )

    Scaffold(
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination

                screens.forEach { screen ->
                    NavigationBarItem(
                        icon = { Icon(screen.icon, contentDescription = screen.title) },
                        label = { Text(screen.title) },
                        selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        Box {
            NavHost(
                navController = navController,
                startDestination = Screen.MusicLibrary.route,
                modifier = Modifier.padding(innerPadding)
            ) {
                composable(Screen.MusicLibrary.route) {
                    MusicLibraryScreen()
                }
                composable(Screen.Playlists.route) {
                    PlaylistListScreen(
                        onPlaylistClick = { playlistId ->
                            navController.navigate(Screen.PlaylistDetail.createRoute(playlistId))
                        }
                    )
                }
                composable(
                    route = Screen.PlaylistDetail.route,
                    arguments = listOf(
                        navArgument("playlistId") { type = NavType.StringType }
                    )
                ) { backStackEntry ->
                    val playlistId = backStackEntry.arguments?.getString("playlistId") ?: return@composable
                    PlaylistDetailScreen(
                        playlistId = playlistId,
                        onNavigateBack = { navController.popBackStack() }
                    )
                }
                composable(Screen.WatchSync.route) {
                    WatchSyncScreen()
                }
            }

            // Always-present collapsible player overlay
            CollapsiblePlayer(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = innerPadding.calculateBottomPadding())
            )
        }
    }
}
