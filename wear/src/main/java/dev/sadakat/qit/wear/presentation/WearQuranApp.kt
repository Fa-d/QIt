package dev.sadakat.qit.wear.presentation

// qit:legacy-ui — predates the design tokens; its UX slice replaces it.

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.navigation.SwipeDismissableNavHost
import androidx.wear.compose.navigation.composable
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController
import dev.sadakat.qit.wear.presentation.home.HomeRoute
import dev.sadakat.qit.wear.presentation.nowplaying.NowPlayingRoute
import dev.sadakat.qit.wear.presentation.surah.SurahRoute

private const val ROUTE_HOME = "home"
private const val ROUTE_SURAH = "surah/{number}"
private const val ROUTE_NOW_PLAYING = "nowplaying"

private fun surahRoute(number: Int) = "surah/$number"

/** Root of the watch UI: the surah list, one screen per surah, and Now playing. */
@Composable
fun WearQuranApp(modifier: Modifier = Modifier) {
    MaterialTheme {
        val navController = rememberSwipeDismissableNavController()
        SwipeDismissableNavHost(
            navController = navController,
            startDestination = ROUTE_HOME,
            modifier = modifier,
        ) {
            composable(ROUTE_HOME) {
                HomeRoute(
                    onSurahClick = { surah -> navController.navigate(surahRoute(surah)) },
                    onNowPlayingClick = { navController.navigate(ROUTE_NOW_PLAYING) },
                )
            }
            composable(
                route = ROUTE_SURAH,
                arguments = listOf(navArgument("number") { type = NavType.IntType }),
            ) {
                SurahRoute(onPlayNow = { navController.navigate(ROUTE_NOW_PLAYING) })
            }
            composable(ROUTE_NOW_PLAYING) {
                NowPlayingRoute()
            }
        }
    }
}
