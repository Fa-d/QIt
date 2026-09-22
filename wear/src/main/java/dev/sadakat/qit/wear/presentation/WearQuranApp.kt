package dev.sadakat.qit.wear.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.navigation.SwipeDismissableNavHost
import androidx.wear.compose.navigation.composable
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController
import dev.sadakat.qit.wear.presentation.home.HomeRoute
import dev.sadakat.qit.wear.presentation.juz.JuzRoute
import dev.sadakat.qit.wear.presentation.mode.ModeRoute
import dev.sadakat.qit.wear.presentation.nowplaying.NowPlayingRoute
import dev.sadakat.qit.wear.presentation.options.OptionsRoute
import dev.sadakat.qit.wear.presentation.surah.SurahRoute
import dev.sadakat.qit.wear.presentation.surahlist.SurahListRoute
import dev.sadakat.qit.wear.presentation.theme.QItWearTheme

private const val ROUTE_HOME = "home"
private const val ROUTE_SURAHS = "surahs?downloaded={downloaded}"
private const val ROUTE_JUZ = "juz"
private const val ROUTE_SURAH = "surah/{number}?from={from}"
private const val ROUTE_NOW_PLAYING = "nowplaying"
private const val ROUTE_MODE = "mode"
private const val ROUTE_OPTIONS = "options"

private fun surahsRoute(downloaded: Boolean) = "surahs?downloaded=$downloaded"
private fun surahRoute(number: Int, from: Int? = null) = "surah/$number" + (from?.let { "?from=$it" } ?: "")

/**
 * Root of the watch UI: the app scaffold (time text) over a swipe-dismiss navigation graph from
 * the home hub to the surahs, the juz, Now playing and the pickers.
 */
@Composable
fun WearQuranApp(
    openNowPlayingRequest: Boolean = false,
    onClearNowPlayingRequest: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    QItWearTheme {
        AppScaffold {
            val navController = rememberSwipeDismissableNavController()
            val clearRequest by rememberUpdatedState(onClearNowPlayingRequest)

            // The tile (and anything else launching with the extra) asks for Now playing by name.
            LaunchedEffect(openNowPlayingRequest) {
                if (openNowPlayingRequest) {
                    navController.navigate(ROUTE_NOW_PLAYING) {
                        launchSingleTop = true
                        popUpTo(ROUTE_HOME)
                    }
                    clearRequest()
                }
            }

            SwipeDismissableNavHost(
                navController = navController,
                startDestination = ROUTE_HOME,
                modifier = modifier,
            ) {
                composable(ROUTE_HOME) {
                    HomeRoute(
                        onSurahsClick = { navController.navigate(surahsRoute(downloaded = false)) },
                        onJuzClick = { navController.navigate(ROUTE_JUZ) },
                        onDownloadedClick = { navController.navigate(surahsRoute(downloaded = true)) },
                        onModeClick = { navController.navigate(ROUTE_MODE) },
                        onNowPlayingClick = { navController.navigate(ROUTE_NOW_PLAYING) },
                    )
                }
                composable(
                    route = ROUTE_SURAHS,
                    arguments = listOf(navArgument("downloaded") { type = NavType.BoolType }),
                ) {
                    SurahListRoute(onSurahClick = { number -> navController.navigate(surahRoute(number)) })
                }
                composable(ROUTE_JUZ) {
                    JuzRoute(
                        onJuzClick = { start -> navController.navigate(surahRoute(start.surah, start.ayah)) },
                    )
                }
                composable(
                    route = ROUTE_SURAH,
                    arguments = listOf(
                        navArgument("number") { type = NavType.IntType },
                        navArgument("from") {
                            type = NavType.IntType
                            nullable = true
                            defaultValue = null
                        },
                    ),
                ) {
                    SurahRoute(
                        onPlayNow = { navController.navigate(ROUTE_NOW_PLAYING) },
                        onModeClick = { navController.navigate(ROUTE_MODE) },
                    )
                }
                composable(ROUTE_NOW_PLAYING) {
                    NowPlayingRoute(onOpenOptions = { navController.navigate(ROUTE_OPTIONS) })
                }
                composable(ROUTE_MODE) {
                    // A choice is a commit: pick, persist, and land back where the picker was opened.
                    ModeRoute(onDismiss = { navController.popBackStack() })
                }
                composable(ROUTE_OPTIONS) {
                    OptionsRoute()
                }
            }
        }
    }
}
