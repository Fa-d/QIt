// kit-migration: pending (still builds Material containers itself; move it onto the :core:ui kit)
@file:Suppress("MatchingDeclarationName") // FollowAlong.kt = the state, its scroller and its chip.

package dev.sadakat.qit.presentation.reader

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material3.ElevatedSuggestionChip
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.res.stringResource
import dev.sadakat.qit.R
import dev.sadakat.qit.core.designsystem.QItTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Follow-along: the reader's list mirrors the reciting ayah until the user scrolls somewhere else.
 * [rememberFollowAlongState] wires the scrolling (and its cancellation by a user drag);
 * [JumpToRecitingChip] offers the way back once the reciting ayah is off screen.
 */
@Stable
class FollowAlongState {

    /** True while the list should mirror the reciting ayah on its own. */
    var following: Boolean by mutableStateOf(true)
        private set

    /** Reading elsewhere pauses following; the chip or tapping an ayah resumes it. */
    fun pause() {
        following = false
    }

    fun resume() {
        following = true
    }

    /** Attach to the list; any user drag means the reader wants to read somewhere else. */
    val userDragObserver = object : NestedScrollConnection {
        override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
            if (source == NestedScrollSource.UserInput) pause()
            return Offset.Zero
        }
    }
}

/** List index of the reciting [ayah]; the basmala (ayah 0) is the header itself. */
internal fun recitingAyahIndex(headerCount: Int, ayah: Int): Int = if (ayah < 1) 0 else headerCount + ayah - 1

/**
 * The follow-along state of one reader list: while following (and the reading pref allows it), each
 * new reciting ayah is scrolled into view; a user drag cancels that scroll and pauses following.
 */
@Composable
fun rememberFollowAlongState(
    listState: LazyListState,
    headerCount: Int,
    playingAyah: Int?,
    enabled: Boolean,
): FollowAlongState {
    val state = remember { FollowAlongState() }
    val currentEnabled by rememberUpdatedState(enabled)

    LaunchedEffect(playingAyah, headerCount) {
        val ayah = playingAyah ?: return@LaunchedEffect
        if (!currentEnabled || !state.following) return@LaunchedEffect
        val scroll = launch { listState.animateScrollToItem(recitingAyahIndex(headerCount, ayah)) }
        // A drag mid-flight must win over the auto-scroll, not fight it.
        launch {
            snapshotFlow { state.following }.first { !it }
            scroll.cancel()
        }
    }
    return state
}

/** Which way the reciting ayah lies off screen. */
private enum class RecitingDirection { Above, Below }

/**
 * The bottom-centre pill shown while the reciting ayah is off screen and the list is not mirroring
 * it. Tapping it scrolls back and resumes following.
 */
@Composable
fun JumpToRecitingChip(
    follow: FollowAlongState,
    listState: LazyListState,
    playingAyah: Int?,
    headerCount: Int,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val motion = QItTheme.motion
    val direction: RecitingDirection? by remember(playingAyah, headerCount, enabled) {
        derivedStateOf {
            val ayah = playingAyah ?: return@derivedStateOf null
            // Nothing to offer while the list is (or is about to be) mirroring the recitation.
            if (enabled && follow.following) return@derivedStateOf null
            val index = recitingAyahIndex(headerCount, ayah)
            val visible = listState.layoutInfo.visibleItemsInfo
            val first = visible.firstOrNull()?.index
            val last = visible.lastOrNull()?.index
            when {
                first != null && index < first -> RecitingDirection.Above
                last != null && index > last -> RecitingDirection.Below
                else -> null
            }
        }
    }

    AnimatedVisibility(
        visible = direction != null,
        enter = fadeIn(motion.enter()) + slideInVertically(motion.enter()) { it / 2 },
        exit = fadeOut(motion.exit()) + slideOutVertically(motion.exit()) { it / 2 },
        modifier = modifier,
    ) {
        ElevatedSuggestionChip(
            onClick = {
                val ayah = playingAyah ?: return@ElevatedSuggestionChip
                scope.launch {
                    follow.resume()
                    listState.animateScrollToItem(recitingAyahIndex(headerCount, ayah))
                }
            },
            label = {
                Text(text = stringResource(R.string.jump_to_reciting_ayah), maxLines = 1)
            },
            icon = {
                Icon(
                    imageVector = if (direction == RecitingDirection.Below) {
                        Icons.Rounded.ArrowDownward
                    } else {
                        Icons.Rounded.ArrowUpward
                    },
                    contentDescription = null,
                )
            },
            modifier = Modifier.padding(bottom = QItTheme.spacing.md),
        )
    }
}
